package net.osmand.shared.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
fun minLocalDateTime(): LocalDateTime = Instant.fromEpochMilliseconds(0).toLocalDateTime(TimeZone.currentSystemDefault())