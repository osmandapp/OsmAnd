package net.osmand.shared.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.TimeSource

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
fun millisToLocalDateTime(milliseconds: Long): LocalDateTime =
	Instant.fromEpochMilliseconds(milliseconds).toLocalDateTime(TimeZone.currentSystemDefault())

private val monotonicOrigin = TimeSource.Monotonic.markNow()

/** Nanoseconds from an arbitrary origin, for measuring how long something took; `System.nanoTime` on the jvm. */
fun nanoTime(): Long = monotonicOrigin.elapsedNow().inWholeNanoseconds
