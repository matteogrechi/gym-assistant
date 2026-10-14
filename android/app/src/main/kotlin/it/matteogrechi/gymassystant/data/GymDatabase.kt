package it.matteogrechi.gymassystant.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

// GYM-023/024/027: one atomic, recoverable dataset and a durable backup outbox.
@Entity(tableName = "dataset")
data class StoredDataset(
    @PrimaryKey val id: Int = 1,
    val revision: Long,
    val json: String,
    val backupRevision: Long = -1,
)

@Dao
interface DatasetDao {
    @Query("SELECT * FROM dataset WHERE id = 1")
    suspend fun read(): StoredDataset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(dataset: StoredDataset)

    @Query("UPDATE dataset SET backupRevision = :revision WHERE id = 1 AND revision >= :revision")
    suspend fun acknowledgeBackup(revision: Long)
}

@Database(entities = [StoredDataset::class], version = 1, exportSchema = true)
abstract class GymDatabase : RoomDatabase() {
    abstract fun datasets(): DatasetDao
}
