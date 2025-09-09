package net.osmand.shared.util

actual typealias KLock = Any

actual inline fun <R> synchronized(lock: KLock, block: () -> R): R =
	kotlin.synchronized(lock) { block() }
