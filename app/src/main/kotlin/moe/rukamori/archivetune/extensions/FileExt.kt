/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.extensions

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

operator fun File.div(child: String): File = File(this, child)

fun File.directorySizeBytes(): Long {
    val stack = ArrayDeque<File>()
    if (!runCatching { exists() }.getOrDefault(false)) return 0L
    stack.add(this)
    var totalBytes = 0L
    while (stack.isNotEmpty()) {
        val file = stack.removeLast()
        if (runCatching { file.isFile }.getOrDefault(false)) {
            totalBytes += runCatching { file.length() }.getOrDefault(0L)
            continue
        }
        val children = runCatching { file.listFiles() }.getOrNull() ?: continue
        children.forEach { child -> stack.add(child) }
    }
    return totalBytes
}

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)
