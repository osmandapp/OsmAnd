package net.osmand.shared.util

import platform.Foundation.NSLock

actual class KLock {
	private val mutex = NSLock()
	fun lock() = mutex.lock()
	fun unlock() = mutex.unlock()
}

actual inline fun <R> synchronized(lock: KLock, block: () -> R): R {
	lock.lock()
	try {
		return block()
	} finally {
		lock.unlock()
	}
}
