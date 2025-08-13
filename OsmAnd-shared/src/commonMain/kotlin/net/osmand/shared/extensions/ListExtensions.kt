package net.osmand.shared.extensions

import kotlinx.coroutines.*

suspend fun <T, R> List<T>.parallelMap(transform: suspend (T) -> R): List<R> = coroutineScope {
	map { async { transform(it) } }.awaitAll()
}
