package com.squalor.consecutor

import java.time.Duration
import java.time.ZonedDateTime

fun millisUntilNextMidnight(now: ZonedDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis()
