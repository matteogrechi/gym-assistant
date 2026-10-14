package it.matteogrechi.gymassystant.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import it.matteogrechi.gymassystant.app.GymApplication
import it.matteogrechi.gymassystant.domain.Dataset
import it.matteogrechi.gymassystant.domain.MediaMetadata
import it.matteogrechi.gymassystant.domain.decodeDataset
import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// GYM-023/024/027: Room is the durable outbox; shared-storage failures never edit training data.
class BackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as GymApplication
            app.repository.load()
            val row = app.repository.stored()
            val dataset = decodeDataset(row.json)
            check(dataset.revision == row.revision) { "Stored revision mismatch" }
            val uri = dataset.settings.backupTreeUri
                ?: return@withContext Result.success(workDataOf(STATUS to "not_configured"))
            if (row.backupRevision >= row.revision) return@withContext Result.success(workDataOf(STATUS to "up_to_date"))
            setProgress(workDataOf(STATUS to "saving", REVISION to row.revision))
            val directory = LocalDocuments.tree(applicationContext, Uri.parse(uri))
            copyMedia(directory, dataset)
            val name = PortableFiles.generationName(dataset)
            val clock = app.clock.read()
            val existing = directory.findFile(name)
            val generation = if (existing != null && verified {
                PortableFiles.matchesRevision(readVerified(existing), dataset, clock)
            }) {
                existing
            } else {
                @Suppress("DEPRECATION")
                val version = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName ?: "unknown"
                val json = PortableFiles.json(dataset, clock, version)
                val expected = PortableFiles.readJson(json)
                val file = LocalDocuments.writeVerified(applicationContext, directory, "gym-assystant-pending-${UUID.randomUUID()}.json",
                    "application/json", json.toByteArray(Charsets.UTF_8))
                require(readVerified(file, canonicalName = false) == expected) { "Staged generation verification failed" }
                publish(directory, file, name)
                require(readVerified(file) == expected) { "Published generation verification failed" }
                file
            }
            require(generation.name == name) { "Provider changed generation filename" }
            verifyMedia(directory, dataset)
            app.repository.acknowledgeBackup(row.revision)
            // Pruning is maintenance: a failure must not invalidate the newly verified backup.
            try { prune(directory, dataset) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
            Result.success(workDataOf(STATUS to "saved", REVISION to row.revision))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val data = workDataOf(STATUS to "external_unsaved", ERROR to (failure.message ?: failure.javaClass.simpleName).take(500),
                "attempt" to runAttemptCount + 1)
            setProgress(data)
            if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry() else Result.failure(data)
        }
    }

    private fun readVerified(file: DocumentFile, canonicalName: Boolean = true): Dataset {
        require(file.isFile)
        val first = (applicationContext.contentResolver.openInputStream(file.uri) ?: throw IOException("Cannot read generation")).use(PortableFiles::readText)
        val dataset = PortableFiles.readJson(first)
        if (canonicalName) require(file.name == PortableFiles.generationName(dataset)) { "Generation filename/metadata mismatch" }
        val second = (applicationContext.contentResolver.openInputStream(file.uri) ?: throw IOException("Cannot reopen generation")).use(PortableFiles::readText)
        require(first == second) { "Generation changed while verifying" }
        return dataset
    }

    private fun verified(check: () -> Boolean): Boolean = try {
        check()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (denied: SecurityException) {
        throw denied
    } catch (failure: IOException) {
        if (failure is CharacterCodingException) false else throw failure
    } catch (_: Exception) {
        false
    }

    // Never publish partial bytes, or delete the unverified file that occupied the canonical name.
    private fun publish(directory: DocumentFile, staged: DocumentFile, name: String) {
        directory.findFile(name)?.let { unverified ->
            val quarantine = "gym-assystant-unverified-${UUID.randomUUID()}-$name"
            check(unverified.renameTo(quarantine) && unverified.name == quarantine) { "Cannot preserve unverified backup file" }
        }
        check(directory.findFile(name) == null) { "Canonical backup name is still occupied" }
        check(staged.renameTo(name) && staged.name == name) { "Cannot publish verified backup file" }
    }

    private fun mediaDirectory(directory: DocumentFile): DocumentFile =
        (directory.findFile("media") ?: directory.createDirectory("media") ?: error("Cannot create media directory")).also {
            require(it.isDirectory && it.canWrite()) { "Media directory is unavailable" }
        }

    private fun copyMedia(directory: DocumentFile, dataset: Dataset) {
        PortableFiles.validateMedia(dataset.media)
        if (dataset.media.isEmpty()) return
        val target = mediaDirectory(directory)
        for (media in dataset.media.distinctBy { it.sha256 }) {
            val existing = target.findFile(media.sha256)
            if (existing != null && verified { verifyClip(existing, media); true }) {
                continue
            }
            val source = LocalDocuments.mediaFile(applicationContext, media)
            require(media.size in 1..PortableFiles.MAX_MEDIA_BYTES && source.length() == media.size)
            val file = target.createFile("application/octet-stream", "gym-assystant-pending-${UUID.randomUUID()}") ?: error("Cannot create media file")
            source.inputStream().use { input ->
                requireNotNull(applicationContext.contentResolver.openOutputStream(file.uri, "w")).use { output ->
                    PortableFiles.copyMedia(input, output, media)
                }
            }
            verifyClip(file, media, canonicalName = false)
            publish(target, file, media.sha256)
            verifyClip(file, media)
        }
    }

    private fun verifyClip(file: DocumentFile, media: MediaMetadata, canonicalName: Boolean = true) {
        require(file.isFile && (!canonicalName || file.name == media.sha256))
        (applicationContext.contentResolver.openInputStream(file.uri) ?: throw IOException("Cannot read backup clip")).use { PortableFiles.verifyMedia(it, media) }
    }

    private fun verifyMedia(directory: DocumentFile, dataset: Dataset) {
        if (dataset.media.isEmpty()) return
        val mediaDirectory = requireNotNull(directory.findFile("media")) { "Backup media is missing" }
        require(mediaDirectory.isDirectory)
        for (media in dataset.media.distinctBy { it.sha256 }) {
            verifyClip(requireNotNull(mediaDirectory.findFile(media.sha256)) { "Backup clip is missing" }, media)
        }
    }

    private fun prune(directory: DocumentFile, current: Dataset) {
        val pattern = Regex("gym-assystant-${Regex.escape(current.id)}-r(0|[1-9][0-9]*)\\.json")
        // shortcut: inspect at most 256 named generations per run; excess files are safely retained.
        val candidates = directory.listFiles().mapNotNull { file ->
            val revision = pattern.matchEntire(file.name.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
            if (revision == null || revision > current.revision) null else revision to file
        }.sortedByDescending { it.first }.take(256)
        var verified = 0
        for ((revision, file) in candidates) {
            try {
                val dataset = readVerified(file)
                require(dataset.id == current.id && dataset.revision == revision)
                if (verified < 2) {
                    verifyMedia(directory, dataset)
                    verified++
                } else {
                    // Reopen/parse immediately before removing only an identified older app generation.
                    file.delete()
                }
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
        }
    }

    companion object {
        const val UNIQUE_WORK = "gym-assystant-local-backup"
        const val STATUS = "backup_status"
        const val REVISION = "backup_revision"
        const val ERROR = "backup_error"
        const val MAX_ATTEMPTS = 5

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
