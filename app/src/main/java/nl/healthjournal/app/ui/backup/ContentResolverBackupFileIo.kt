package nl.healthjournal.app.ui.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Reads and writes the picked document through the system file picker's grant; no storage permission needed. */
class ContentResolverBackupFileIo(private val resolver: ContentResolver) : BackupFileIo {

    override fun read(uri: Uri): ByteArray {
        val input = resolver.openInputStream(uri) ?: throw IOException("cannot open")
        input.use {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val n = it.read(buffer)
                if (n < 0) break
                total += n
                // A backup of a personal journal is far smaller; refuse before holding something huge in memory.
                if (total > MAX_BYTES) throw BackupFileTooLarge()
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }

    override fun write(uri: Uri, bytes: ByteArray) {
        // "wt" truncates, so a longer file that was there before cannot leave trailing bytes behind.
        val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("cannot open")
        output.use { it.write(bytes) }
    }

    private companion object {
        const val MAX_BYTES = 64L * 1024 * 1024
    }
}
