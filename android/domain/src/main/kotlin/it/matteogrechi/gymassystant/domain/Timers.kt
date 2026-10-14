package it.matteogrechi.gymassystant.domain

// GYM-012/024: wall-clock changes cannot change elapsed time, and reboot cannot fabricate time.
object Timers {
    fun settle(timer: TimerState, clock: ClockReading, limitMs: Long? = null): TimerState {
        require(clock.monotonicMs >= 0 && (limitMs == null || limitMs >= 0))
        require(timer.elapsedMs >= 0 && (limitMs == null || timer.elapsedMs <= limitMs))
        val anchor = timer.anchorMonotonicMs ?: return timer
        if (timer.bootId == null || clock.bootId == null || timer.bootId != clock.bootId || clock.monotonicMs < anchor) {
            return timer.copy(anchorMonotonicMs = null, bootId = null, interrupted = true)
        }
        val delta = clock.monotonicMs - anchor
        val elapsed = if (delta > Long.MAX_VALUE - timer.elapsedMs) Long.MAX_VALUE else timer.elapsedMs + delta
        val clamped = if (limitMs == null) elapsed else elapsed.coerceAtMost(limitMs)
        return timer.copy(elapsedMs = clamped, anchorMonotonicMs = if (limitMs != null && clamped == limitMs) null else clock.monotonicMs)
    }

    fun readingMs(timer: TimerState, mode: TimerMode, recoverySeconds: Int, clock: ClockReading): Long {
        require(recoverySeconds in 1..3600)
        val limit = if (mode == TimerMode.RECOVERY) recoverySeconds * 1000L else null
        val elapsed = settle(timer, clock, limit).elapsedMs
        return if (limit == null) elapsed else limit - elapsed
    }

    internal fun change(timer: TimerState, action: TimerAction, clock: ClockReading, limitMs: Long?): TimerState {
        val settled = settle(timer, clock, limitMs)
        return when (action) {
            TimerAction.RESET -> TimerState()
            TimerAction.PAUSE -> settled.copy(anchorMonotonicMs = null, bootId = null)
            TimerAction.START -> {
                require(clock.bootId != null) { "Boot identity unavailable" }
                settled.copy(elapsedMs = if (limitMs != null && settled.elapsedMs == limitMs) 0 else settled.elapsedMs,
                    anchorMonotonicMs = clock.monotonicMs, bootId = clock.bootId, interrupted = false, capturedRunning = false)
            }
        }
    }

    internal fun pause(timer: TimerState, clock: ClockReading, limitMs: Long? = null): TimerState =
        settle(timer, clock, limitMs).copy(anchorMonotonicMs = null, bootId = null)
}
