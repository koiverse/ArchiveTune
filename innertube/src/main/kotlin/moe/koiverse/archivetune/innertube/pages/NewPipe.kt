package moe.koiverse.archivetune.innertube

import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.mozilla.javascript.Context
import java.io.IOException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: NewPipeRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)

        var hasUserAgent = false
        headers.forEach { (headerName, headerValueList) ->
            if (headerName.equals("User-Agent", ignoreCase = true) && headerValueList.isNotEmpty()) {
                hasUserAgent = true
            }
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

object NewPipeUtils {

    init {
        NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> =
        runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }.recoverCatching { e ->
            if (e.isSignaturePatternMissing()) {
                SignatureDecipherFallback.getSignatureTimestamp(videoId)
            } else {
                throw e
            }
        }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            val url = format.url ?: run {
                val cipherString = format.signatureCipher ?: format.cipher
                if (cipherString == null) throw ParsingException("Could not find format url")

                val params = parseQueryString(cipherString)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")

                val decipheredSignature =
                    try {
                        YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
                    } catch (e: Throwable) {
                        if (e.isSignaturePatternMissing()) {
                            SignatureDecipherFallback.deobfuscateSignature(videoId, obfuscatedSignature)
                        } else {
                            throw e
                        }
                    }
                url.parameters[signatureParam] =
                    decipheredSignature
                url.toString()
            }

            runCatching {
                retryWithBackoff(
                    maxAttempts = 3,
                    initialDelayMs = 250L,
                    maxDelayMs = 2_000L
                ) {
                    YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
                }
            }.getOrElse { url }
        }

    private inline fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        block: () -> T
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                val isRetryable =
                    e is SocketTimeoutException ||
                        e is IOException ||
                        e.cause is SocketTimeoutException ||
                        e.cause is IOException
                if (!isRetryable || attempt == maxAttempts - 1) throw e
                lastError = e
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
                delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
                attempt++
            }
        }
        throw lastError ?: IllegalStateException("Retry attempts exhausted")
    }

}

private fun Throwable.isSignaturePatternMissing(): Boolean {
    val text =
        buildString {
            var current: Throwable? = this@isSignaturePatternMissing
            var depth = 0
            while (current != null && depth < 6) {
                append(current::class.java.name)
                append(':')
                append(current.message.orEmpty())
                append('\n')
                current = current.cause
                depth++
            }
        }

    return text.contains("Could not parse deobfuscation function", ignoreCase = true) ||
        text.contains("Could not find deobfuscation function", ignoreCase = true) ||
        text.contains("Failed to find pattern", ignoreCase = true)
}

private object SignatureDecipherFallback {
    private const val cacheTtlMs: Long = 6 * 60 * 60 * 1000L
    private val lock = Any()

    private var cachedPlayerJsUrl: String? = null
    private var cachedScriptPrefix: String? = null
    private var cachedSignatureTimestamp: Int? = null
    private var cachedAtMs: Long = 0L

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()

    fun deobfuscateSignature(videoId: String, obfuscatedSignature: String): String {
        val scriptPrefix = getOrBuildScriptPrefix(videoId)
        val escaped = escapeJsString(obfuscatedSignature)
        val jsToEval = scriptPrefix + "\"" + escaped + "\");"

        val ctx = Context.enter()
        ctx.optimizationLevel = -1
        return try {
            val scope = ctx.initStandardObjects()
            val result = ctx.evaluateString(scope, jsToEval, "archivetune_decipher", 1, null)
            Context.toString(result)
        } finally {
            Context.exit()
        }
    }

    fun getSignatureTimestamp(videoId: String): Int {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val cached = cachedSignatureTimestamp
            if (cached != null && (now - cachedAtMs) <= cacheTtlMs) return cached
        }

        val (playerJsUrl, signatureTimestamp) = refreshFromWatch(videoId)
        if (signatureTimestamp != null) return signatureTimestamp

        val jsCode = fetchText(playerJsUrl)
        val fromPlayer =
            extractSignatureTimestamp(jsCode)
                ?: throw ParsingException("Could not locate signature timestamp")

        synchronized(lock) {
            cachedSignatureTimestamp = fromPlayer
            cachedAtMs = System.currentTimeMillis()
        }

        return fromPlayer
    }

    private fun getOrBuildScriptPrefix(videoId: String): String {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val cachedUrl = cachedPlayerJsUrl
            val cachedPrefix = cachedScriptPrefix
            if (cachedUrl != null && cachedPrefix != null && (now - cachedAtMs) <= cacheTtlMs) {
                return cachedPrefix
            }

            val (playerJsUrl, _) = refreshFromWatch(videoId)
            val jsCode = fetchText(playerJsUrl)
            val scriptPrefix = buildDecipherScriptPrefix(jsCode)

            cachedPlayerJsUrl = playerJsUrl
            cachedScriptPrefix = scriptPrefix
            cachedAtMs = now

            return scriptPrefix
        }
    }

    private fun refreshFromWatch(videoId: String): Pair<String, Int?> {
        val watchHtml =
            fetchText(
                "https://www.youtube.com/watch?v=$videoId&bpctr=9999999999&has_verified=1"
            )
        val playerJsUrl = extractPlayerJsUrl(watchHtml)
        val signatureTimestamp = extractSignatureTimestamp(watchHtml)

        synchronized(lock) {
            cachedPlayerJsUrl = playerJsUrl
            cachedSignatureTimestamp = signatureTimestamp
            cachedAtMs = System.currentTimeMillis()
        }

        return playerJsUrl to signatureTimestamp
    }

    private fun fetchText(url: String): String {
        val request =
            OkHttpRequest.Builder()
                .get()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept", "*/*")
                .header("Referer", "https://www.youtube.com/")
                .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body.string()
                if (response.code == 429 || body.contains("captcha", ignoreCase = true)) {
                    throw ReCaptchaException("reCaptcha Challenge requested", url)
                }
                throw IOException("HTTP ${response.code} for $url")
            }
            return response.body.string()
        }
    }

    private fun extractPlayerJsUrl(watchHtml: String): String {
        val patterns =
            listOf(
                Regex(""""jsUrl"\s*:\s*"([^"]+)""""),
                Regex(""""PLAYER_JS_URL"\s*:\s*"([^"]+)""""),
                Regex("""["'](\/s\/player\/[^"']+?\/base\.js[^"']*)["']"""),
                Regex("""<script[^>]+src="([^"]*\/s\/player\/[^"]+?\/base\.js[^"]*)"[^>]*>"""),
                Regex("""<link[^>]+href="([^"]*\/s\/player\/[^"]+?\/base\.js[^"]*)"[^>]*>"""),
            )

        val raw =
            patterns.asSequence()
                .mapNotNull { it.find(watchHtml)?.groupValues?.getOrNull(1) }
                .firstOrNull()
                ?: throw ParsingException("Could not find player JS URL in watch HTML")

        val unescaped =
            raw.replace("\\u0026", "&")
                .replace("\\/", "/")

        return if (unescaped.startsWith("http")) unescaped else "https://www.youtube.com$unescaped"
    }

    private fun buildDecipherScriptPrefix(playerJs: String): String {
        val functionMatch =
            findDecipherFunction(playerJs)
                ?: throw ParsingException("Could not locate signature decipher function in player JS")

        val functionName = functionMatch.first
        val functionDef = functionMatch.second

        val objName =
            Regex("""(?:^|[;,{])([$\w]{2,})\.[\w$]{2,}\(""")
                .find(functionDef)
                ?.groupValues
                ?.getOrNull(1)

        val objDef =
            if (objName != null) {
                extractObjectDefinition(playerJs, objName)
                    ?: throw ParsingException("Could not locate signature decipher helper object in player JS")
            } else {
                ""
            }

        return buildString {
            if (objDef.isNotBlank()) {
                append(objDef)
                append('\n')
            }
            append(functionDef)
            append('\n')
            append("function __archivetune_decipher(s){return ")
            append(functionName)
            append("(s);}__archivetune_decipher(")
        }
    }

    private fun findDecipherFunction(playerJs: String): Pair<String, String>? {
        val headerRegexes =
            listOf(
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*([$\w]{2,})\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*var\s+([$\w]{2,})\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*let\s+([$\w]{2,})\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*const\s+([$\w]{2,})\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)function\s+([$\w]{2,})\s*\([^)]*\)\s*\{"""),
            )

        data class Candidate(val name: String, val def: String, val score: Int)

        val candidates = mutableListOf<Candidate>()
        val callsiteNames = findDecipherNamesFromCallsites(playerJs)
        for (name in callsiteNames) {
            val def = extractFunctionDefinitionByName(playerJs, name) ?: continue
            if (!def.containsSplitJoin()) continue
            val score = scoreDecipherCandidate(def)
            if (score <= 0) continue
            candidates.add(Candidate(name, def, score + 5))
        }

        for (regex in headerRegexes) {
            regex.findAll(playerJs).forEach { match ->
                val name = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@forEach
                val braceIndex = playerJs.indexOf('{', match.range.last)
                if (braceIndex < 0) return@forEach
                val endIndex = findMatchingBraceIndex(playerJs, braceIndex) ?: return@forEach
                val def = playerJs.substring(match.range.first, endIndex + 1).trimStart(';', '\n')
                if (!def.containsSplitJoin()) return@forEach
                val score = scoreDecipherCandidate(def)
                if (score <= 0) return@forEach
                candidates.add(Candidate(name, def, score))
            }
        }

        return candidates.maxByOrNull { it.score }?.let { it.name to it.def }
    }

    private fun String.containsSplitJoin(): Boolean {
        val split = Regex("""\.split\(\s*["']{2}\s*\)""").containsMatchIn(this)
        val join = Regex("""\.join\(\s*["']{2}\s*\)""").containsMatchIn(this)
        return split && join
    }

    private fun scoreDecipherCandidate(def: String): Int {
        var score = 0
        if (def.contains(".reverse(")) score += 3
        if (def.contains(".splice(")) score += 3
        if (def.contains(".slice(")) score += 2
        if (Regex("""[;,{][$\w]{2,}\.[\w$]{2,}\(""").containsMatchIn(def)) score += 3
        if (def.length in 120..3000) score += 1
        return score
    }

    private fun extractObjectDefinition(playerJs: String, objName: String): String? {
        val escaped = Regex.escape(objName)
        val starts =
            sequenceOf(
                Regex("""(?m)(?:^|[;\n])\s*var\s+$escaped\s*=\s*\{"""),
                Regex("""(?m)(?:^|[;\n])\s*let\s+$escaped\s*=\s*\{"""),
                Regex("""(?m)(?:^|[;\n])\s*const\s+$escaped\s*=\s*\{"""),
                Regex("""(?m)(?:^|[;\n])\s*$escaped\s*=\s*\{"""),
            )
                .mapNotNull { it.find(playerJs) }
                .firstOrNull()
                ?: return null

        val braceIndex = playerJs.indexOf('{', starts.range.last)
        if (braceIndex < 0) return null
        val endIndex = findMatchingBraceIndex(playerJs, braceIndex) ?: return null
        var end = endIndex + 1
        while (end < playerJs.length && playerJs[end].isWhitespace()) end++
        if (end < playerJs.length && playerJs[end] == ';') end++
        return playerJs.substring(starts.range.first, end).trimStart(';', '\n')
    }

    private fun findDecipherNamesFromCallsites(playerJs: String): Set<String> {
        val patterns =
            listOf(
                Regex("""\.set\(\s*["']signature["']\s*,\s*([$\w]{2,})\("""),
                Regex("""\.set\(\s*["']sig["']\s*,\s*([$\w]{2,})\("""),
            )

        return patterns.asSequence()
            .flatMap { regex -> regex.findAll(playerJs).map { it.groupValues.getOrNull(1).orEmpty() } }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun extractFunctionDefinitionByName(playerJs: String, name: String): String? {
        val escaped = Regex.escape(name)
        val patterns =
            listOf(
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*$escaped\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*var\s+$escaped\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*let\s+$escaped\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)(?:^|[;,{]\s*|\n)\s*const\s+$escaped\s*=\s*function\([^)]*\)\s*\{"""),
                Regex("""(?m)function\s+$escaped\s*\([^)]*\)\s*\{"""),
            )

        for (pattern in patterns) {
            val match = pattern.find(playerJs) ?: continue
            val braceIndex = playerJs.indexOf('{', match.range.last)
            if (braceIndex < 0) continue
            val endIndex = findMatchingBraceIndex(playerJs, braceIndex) ?: continue
            return playerJs.substring(match.range.first, endIndex + 1).trimStart(';', '\n')
        }
        return null
    }

    private fun findMatchingBraceIndex(text: String, startBraceIndex: Int): Int? {
        if (startBraceIndex !in text.indices || text[startBraceIndex] != '{') return null
        var depth = 0
        var i = startBraceIndex
        var inSingle = false
        var inDouble = false
        var inTemplate = false
        while (i < text.length) {
            val c = text[i]
            if (inSingle) {
                if (c == '\\') i += 2 else if (c == '\'') {
                    inSingle = false
                    i++
                } else i++
                continue
            }
            if (inDouble) {
                if (c == '\\') i += 2 else if (c == '"') {
                    inDouble = false
                    i++
                } else i++
                continue
            }
            if (inTemplate) {
                if (c == '\\') i += 2 else if (c == '`') {
                    inTemplate = false
                    i++
                } else i++
                continue
            }

            when (c) {
                '\'' -> {
                    inSingle = true
                    i++
                }
                '"' -> {
                    inDouble = true
                    i++
                }
                '`' -> {
                    inTemplate = true
                    i++
                }
                '{' -> {
                    depth++
                    i++
                }
                '}' -> {
                    depth--
                    if (depth == 0) return i
                    i++
                }
                else -> i++
            }
        }
        return null
    }

    private fun extractSignatureTimestamp(text: String): Int? {
        val patterns =
            listOf(
                Regex(""""signatureTimestamp"\s*:\s*(\d+)"""),
                Regex(""""sts"\s*:\s*(\d+)"""),
                Regex("""\bsts\s*:\s*(\d+)"""),
            )
        return patterns.asSequence()
            .mapNotNull { it.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .firstOrNull()
    }

    private fun escapeJsString(value: String): String {
        val sb = StringBuilder(value.length + 16)
        for (c in value) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
