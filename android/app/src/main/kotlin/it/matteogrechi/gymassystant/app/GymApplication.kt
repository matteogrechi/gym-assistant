package it.matteogrechi.gymassystant.app

import android.app.Application
import androidx.room.Room
import it.matteogrechi.gymassystant.data.AndroidClock
import it.matteogrechi.gymassystant.data.DatasetRepository
import it.matteogrechi.gymassystant.data.GymDatabase
import it.matteogrechi.gymassystant.data.LaunchAppearance

// GYM-023/027: explicit dependencies shared by UI and the durable local backup worker.
class GymApplication : Application() {
    val appearance by lazy { LaunchAppearance(this) }
    override fun onCreate() {
        super.onCreate()
        appearance.apply()
    }
    val clock by lazy { AndroidClock(this) }
    private val database by lazy {
        Room.databaseBuilder(this, GymDatabase::class.java, "gym-assystant.db").build()
    }
    val repository by lazy { DatasetRepository(this, database, clock, appearance) }
}
