package net.osmand.shared.util

expect class KLock()

expect inline fun <R> synchronized(lock: KLock, block: () -> R): R