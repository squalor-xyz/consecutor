package com.squalor.consecutor

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.ZonedDateTime

fun millisUntilNextMidnight(now: ZonedDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis()

/**
 * Suspends until just after the next local midnight, or until [clockChanges] has a signal, whichever
 * comes first. The wait is a monotonic delay, so a wall-clock or time-zone change does not shorten it;
 * callers signal the change so that the wait is computed again.
 */
suspend fun awaitDayBoundary(now: () -> ZonedDateTime, clockChanges: ReceiveChannel<Unit>) {
    withTimeoutOrNull(millisUntilNextMidnight(now()) + 1_000) { clockChanges.receive() }
}

/** Calls [refresh] after every day boundary and every clock change signal, forever. */
suspend fun refreshAtDayBoundaries(
    now: () -> ZonedDateTime,
    clockChanges: ReceiveChannel<Unit>,
    refresh: () -> Unit
): Nothing {
    while (true) {
        awaitDayBoundary(now, clockChanges)
        refresh()
    }
}
