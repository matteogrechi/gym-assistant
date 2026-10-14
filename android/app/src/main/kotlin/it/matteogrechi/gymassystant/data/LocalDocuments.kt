package it.matteogrechi.gymassystant.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import it.matteogrechi.gymassystant.domain.MediaMetadata
import java.io.File
import java.security.MessageDigest
import java.util.UUID

// GYM-002/023/027: picker requests alone do not exclude unverifiable remote providers.
object LocalDocuments {
    fun requireLocal(uri: Uri, tree: Boolean = false) {
        val allowed = if (tree) setOf("com.android.externalstorage.documents") else setOf(
            "com.android.externalstorage.documents", "com.android.providers.media.documents",
            "com.android.providers.downloads.documents",
        )
        require(uri.scheme == "content" && uri.authority in allowed) { "Select a local storage provider" }
    }

    fun tree(context: Context, uri: Uri): DocumentFile {
        requireLocal(uri, tree = true)
        return requireNotNull(DocumentFile.fromTreeUri(context, uri)).also {
            require(it.exists() && it.isDirectory && it.canWrite()) { "Folder is unavailable" }
        }
    }

    fun importVideo(context: Context, uri: Uri, exerciseId: String): MediaMetadata {
        requireLocal(uri)
        val resolver = context.contentResolver
        val mime = requireNotNull(resolver.getType(uri))
        require(mime.startsWith("video/")) { "Select a video file" }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "video"
        val directory = File(context.filesDir, "media").apply { mkdirs() }
        val temporary = File(directory, "${UUID.randomUUID()}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        try {
            requireNotNull(resolver.openInputStream(uri)).use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= 512L * 1024 * 1024) { "Video exceeds 512 MiB" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                }
            }
            require(size > 0) { "Video is empty" }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val file = File(directory, hash)
            if (file.exists()) temporary.delete() else check(temporary.renameTo(file))
            return MediaMetadata(UUID.randomUUID().toString(), exerciseId, name, mime, size, hash, "media/$hash")
        } finally {
            temporary.delete()
        }
    }

    fun mediaFile(context: Context, media: MediaMetadata): File {
        require(media.relativePath == "media/${media.sha256}" && media.sha256.matches(Regex("[0-9a-f]{64}")))
        return File(context.filesDir, media.relativePath).also { require(it.isFile) { "Video is unavailable" } }
    }

    // GYM-023/024: acknowledge only closed, reopened, byte-identical generations.
    fun writeVerified(context: Context, directory: DocumentFile, name: String, mime: String, bytes: ByteArray): DocumentFile {
        val file = directory.createFile(mime, name) ?: error("Cannot create file")
        try {
            requireNotNull(context.contentResolver.openOutputStream(file.uri, "w")).use { it.write(bytes) }
            requireNotNull(context.contentResolver.openInputStream(file.uri)).use { input ->
                val buffer = ByteArray(64 * 1024)
                var offset = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(offset + count <= bytes.size)
                    for (i in 0 until count) check(buffer[i] == bytes[offset + i])
                    offset += count
                }
                check(offset == bytes.size)
            }
            return file
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }
}
