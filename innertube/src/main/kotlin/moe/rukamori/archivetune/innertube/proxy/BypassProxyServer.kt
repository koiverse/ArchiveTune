package moe.rukamori.archivetune.innertube.proxy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class BypassProxyServer {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val _port = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dnsClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val dohResolvers = listOf(
        "https://dns.google/dns-query",
        "https://cloudflare-dns.com/dns-query",
        "https://dns.adguard.com/dns-query",
    )

    private val dohClients = dohResolvers.map { url ->
        DnsOverHttps.Builder()
            .client(dnsClient)
            .url(url.toHttpUrl())
            .build()
    }

    val proxy: Proxy
        get() = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", _port.get()))

    val port: Int
        get() = _port.get()

    val isRunning: Boolean
        get() = serverSocket?.isBound == true && !serverSocket!!.isClosed

    fun start() {
        if (isRunning) return
        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        _port.set(socket.localPort)

        serverJob = scope.launch {
            while (isActive && socket.isBound && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    launch {
                        try {
                            handleConnection(client)
                        } catch (_: Exception) {
                        } finally {
                            try { client.close() } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {
                    if (!socket.isClosed) break
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        _port.set(0)
    }

    private fun handleConnection(client: Socket) {
        client.soTimeout = 15000
        val input = client.getInputStream()
        val output = client.getOutputStream()

        val request = readHttpRequest(input) ?: return

        if (request.startsWith("CONNECT ")) {
            handleConnect(client, input, output, request)
        } else {
            handleHttp(client, input, output, request)
        }
    }

    private fun handleConnect(
        client: Socket,
        input: InputStream,
        output: OutputStream,
        request: String,
    ) {
        val parts = request.split(" ")
        if (parts.size < 2) return
        val hostPort = parts[1]
        val colonIndex = hostPort.lastIndexOf(':')
        if (colonIndex < 0) return
        val host = hostPort.substring(0, colonIndex)
        val port = hostPort.substring(colonIndex + 1).toIntOrNull() ?: return

        val resolvedHost = resolveHost(host) ?: return

        val remote = Socket()
        try {
            remote.connect(InetSocketAddress(resolvedHost, port), 15000)
            remote.soTimeout = 30000

            output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            output.flush()

            writeBypassedTls(input, remote.getOutputStream())

            relay(input, remote.getOutputStream(), remote.getInputStream(), output)
        } finally {
            try { remote.close() } catch (_: Exception) {}
        }
    }

    private fun handleHttp(
        client: Socket,
        input: InputStream,
        output: OutputStream,
        request: String,
    ) {
        val lines = request.lines()
        if (lines.isEmpty()) return
        val requestLine = lines[0]
        val parts = requestLine.split(" ")
        if (parts.size < 3) return

        val method = parts[0]
        val url = parts[1]
        val version = parts[2]

        val parsedUrl = try {
            java.net.URL(url)
        } catch (_: Exception) { return }

        val host = parsedUrl.host
        val port = parsedUrl.port.let { if (it == -1) 80 else it }
        val path = parsedUrl.file.ifEmpty { "/" }

        val resolvedHost = resolveHost(host) ?: return

        val remote = Socket()
        try {
            remote.connect(InetSocketAddress(resolvedHost, port), 15000)
            remote.soTimeout = 30000
            val remoteOutput = remote.getOutputStream()

            val modifiedRequest = applyHttpDesync(request, host, method, path, version)
            remoteOutput.write(modifiedRequest.toByteArray())
            remoteOutput.flush()

            val remoteInput = remote.getInputStream()
            relayHttpResponse(remoteInput, output)
        } finally {
            try { remote.close() } catch (_: Exception) {}
        }
    }

    private fun writeBypassedTls(clientInput: InputStream, remoteOutput: OutputStream) {
        val helloBytes = readTlsClientHello(clientInput) ?: return

        val sniOffset = findSniOffset(helloBytes)

        val splitPoint = if (sniOffset > 0 && sniOffset < helloBytes.size) {
            sniOffset
        } else {
            (helloBytes.size * 0.3).toInt().coerceIn(1, helloBytes.size - 4)
        }

        remoteOutput.write(helloBytes, 0, splitPoint)
        remoteOutput.flush()
        Thread.sleep(Random.nextLong(30, 80))

        remoteOutput.write(helloBytes, splitPoint, helloBytes.size - splitPoint)
        remoteOutput.flush()
        Thread.sleep(Random.nextLong(10, 30))
    }

    private fun readTlsClientHello(input: InputStream): ByteArray? {
        val header = ByteArray(5)
        var offset = 0
        while (offset < 5) {
            val read = input.read(header, offset, 5 - offset)
            if (read < 0) return null
            offset += read
        }

        if (header[0].toInt() != 0x16) return null

        val recordLength = ((header[3].toInt() and 0xFF) shl 8) or (header[4].toInt() and 0xFF)
        if (recordLength < 4 || recordLength > 32768) return null

        val body = ByteArray(recordLength)
        offset = 0
        while (offset < recordLength) {
            val read = input.read(body, offset, recordLength - offset)
            if (read < 0) return null
            offset += read
        }

        return header + body
    }

    private fun findSniOffset(data: ByteArray): Int {
        if (data.size < 43) return -1

        var pos = 43

        if (pos + 1 > data.size) return -1
        val sessionIdLen = data[pos].toInt() and 0xFF
        pos += 1 + sessionIdLen

        if (pos + 2 > data.size) return -1
        val cipherSuitesLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
        pos += 2 + cipherSuitesLen

        if (pos + 1 > data.size) return -1
        val compressionLen = data[pos].toInt() and 0xFF
        pos += 1 + compressionLen

        if (pos + 2 > data.size) return -1
        val extensionsLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
        pos += 2
        val extensionsEnd = pos + extensionsLen

        while (pos + 4 <= extensionsEnd && pos + 4 <= data.size) {
            val extType = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            val extLen = ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)
            pos += 4
            if (extType == 0x0000) {
                return (pos - 4).coerceAtLeast(5)
            }
            pos += extLen
        }

        return -1
    }

    private fun applyHttpDesync(
        request: String,
        host: String,
        method: String,
        path: String,
        version: String,
    ): String {
        val sb = StringBuilder()
        sb.append("$method $path $version\r\n")

        val lines = request.lines().drop(1)
        var hasHost = false
        for (line in lines) {
            if (line.isBlank()) {
                sb.append("\r\n")
                break
            }
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val headerName = line.substring(0, colonIndex)
                if (headerName.equals("Host", ignoreCase = true)) {
                    sb.append("Host: $host\r\n")
                    hasHost = true
                } else if (!headerName.equals("Proxy-Connection", ignoreCase = true) &&
                    !headerName.equals("Proxy-Authorization", ignoreCase = true)
                ) {
                    val mixedCase = headerName.mapIndexed { i, c ->
                        if (i % 2 == 0) c.uppercaseChar() else c.lowercaseChar()
                    }.joinToString("")
                    sb.append("$mixedCase: ${line.substring(colonIndex + 1).trimStart()}\r\n")
                }
            }
        }
        if (!hasHost) {
            sb.append("Host: $host\r\n")
        }
        sb.append("\r\n")
        return sb.toString()
    }

    private fun relay(
        from1: InputStream,
        to1: OutputStream,
        from2: InputStream,
        to2: OutputStream,
    ) {
        val t1 = Thread {
            try {
                val buf = ByteArray(8192)
                while (true) {
                    val read = from1.read(buf)
                    if (read < 0) break
                    to1.write(buf, 0, read)
                    to1.flush()
                }
            } catch (_: Exception) {}
        }
        val t2 = Thread {
            try {
                val buf = ByteArray(8192)
                while (true) {
                    val read = from2.read(buf)
                    if (read < 0) break
                    to2.write(buf, 0, read)
                    to2.flush()
                }
            } catch (_: Exception) {}
        }
        t1.start()
        t2.start()
        try { t1.join(30000) } catch (_: Exception) {}
        try { t2.join(30000) } catch (_: Exception) {}
    }

    private fun relayHttpResponse(from: InputStream, to: OutputStream) {
        val buf = ByteArray(8192)
        try {
            while (true) {
                val read = from.read(buf)
                if (read < 0) break
                to.write(buf, 0, read)
                to.flush()
            }
        } catch (_: Exception) {}
    }

    private fun readHttpRequest(input: InputStream): String? {
        val buf = ByteArray(8192)
        var total = 0
        while (total < buf.size) {
            val read = input.read(buf, total, buf.size - total)
            if (read < 0) return null
            total += read
            val data = String(buf, 0, total)
            if (data.contains("\r\n\r\n") || data.contains("\n\n")) {
                val headerEnd = if (data.contains("\r\n\r\n")) {
                    data.indexOf("\r\n\r\n") + 4
                } else {
                    data.indexOf("\n\n") + 2
                }
                return data.substring(0, headerEnd)
            }
            if (total >= 32768) return null
        }
        return null
    }

    private fun resolveHost(host: String): String? {
        for (doh in dohClients) {
            try {
                val addresses = doh.lookup(host)
                if (addresses.isNotEmpty()) {
                    return addresses.first().hostAddress
                }
            } catch (_: Exception) {}
        }
        return try {
            InetAddress.getByName(host).hostAddress
        } catch (_: Exception) { null }
    }
}
