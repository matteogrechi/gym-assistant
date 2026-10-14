package it.matteogrechi.gymassystant.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import it.matteogrechi.gymassystant.domain.ClockReading
import it.matteogrechi.gymassystant.domain.ObservationTime
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

// GYM-012/020: monotonic timing and durable civil-date attendance use separate clocks.
class AndroidClock(private val context: Context) {
    private val processIdentity = "process:${UUID.randomUUID()}"
    fun read(): ClockReading {
        val instant = Instant.now()
        val zone = ZoneId.systemDefault()
        val boot = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT).toString()
        }.getOrNull() ?: processIdentity
        return ClockReading(
            ObservationTime(instant.toString(), instant.atZone(zone).toLocalDate().toString(), zone.id),
            SystemClock.elapsedRealtime(), boot,
        )
    }
}
