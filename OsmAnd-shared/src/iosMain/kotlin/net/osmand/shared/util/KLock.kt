package net.osmand.shared.util

import platform.Foundation.NSRecursiveLock

/**
 * Recursive, because it stands in for a java monitor. The code copied from the app locks in a
 * method that goes on to call another locking method of the same object - `searchTravelGpx` by
 * route types calls `searchTravelGpx` by route ids - and java lets a thread re-enter a monitor it
 * already holds. A plain `NSLock` deadlocks there instead, on the main thread, with no error.
 */
actual class KLock {
	private val mutex = NSRecursiveLock()
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
