package net.osmand.shared.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
fun minLocalDateTime(): LocalDateTime = LocalDateTime(-999_999_999, 1, 1, 0, 0)