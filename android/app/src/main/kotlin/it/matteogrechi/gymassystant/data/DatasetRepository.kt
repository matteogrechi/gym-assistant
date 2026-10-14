package it.matteogrechi.gymassystant.data

import android.content.Context
import androidx.room.withTransaction
import it.matteogrechi.gymassystant.domain.Dataset
import it.matteogrechi.gymassystant.domain.decodeDataset
import it.matteogrechi.gymassystant.domain.encodeDataset
import it.matteogrechi.gymassystant.domain.initialDataset
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// GYM-023/027: serialize edits against the latest committed state; never reset corrupt data.
class DatasetRepository(
    private val context: Context,
    private val database: GymDatabase,
    private val clock: AndroidClock,
    private val appearance: LaunchAppearance,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<Dataset?>(null)
    val state = mutableState.asStateFlow()

    suspend fun load(): Dataset = mutex.withLock {
        mutableState.value ?: run {
            val row = database.datasets().read()
            val dataset = row?.let { decodeDataset(it.json) } ?: initialDataset(clock.read())
            if (row == null) database.datasets().write(StoredDataset(revision = dataset.revision, json = encodeDataset(dataset)))
            appearance.cache(dataset.settings.theme)
            withContext(Dispatchers.Main) {
                mutableState.value = dataset
                appearance.apply(dataset.settings.theme)
            }
            dataset
        }
    }

    suspend fun update(change: (Dataset) -> Dataset): Dataset {
        load()
        return mutex.withLock {
            val previous = requireNotNull(mutableState.value)
            val next = change(previous)
            if (next == previous) return@withLock previous
            val committed = next.copy(revision = Math.addExact(previous.revision, 1))
            val json = encodeDataset(committed)
            // A committed edit must reach memory and the outbox even if its screen closes.
            withContext(NonCancellable) {
                database.withTransaction {
                    val row = requireNotNull(database.datasets().read())
                    database.datasets().write(row.copy(revision = committed.revision, json = json))
                }
                appearance.cache(committed.settings.theme)
                withContext(Dispatchers.Main) {
                    mutableState.value = committed
                    if (previous.settings.theme != committed.settings.theme) appearance.apply(committed.settings.theme)
                }
                BackupWorker.enqueue(context)
                committed
            }
        }
    }

    suspend fun stored(): StoredDataset {
        load()
        return requireNotNull(database.datasets().read())
    }

    suspend fun acknowledgeBackup(revision: Long) = mutex.withLock {
        val row = requireNotNull(database.datasets().read())
        if (revision > row.backupRevision) database.datasets().acknowledgeBackup(revision)
    }
}
