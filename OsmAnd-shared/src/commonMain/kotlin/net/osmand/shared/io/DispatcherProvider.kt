package net.osmand.shared.io

import kotlinx.coroutines.CoroutineDispatcher

expect object DispatcherProvider {
	fun singleThread(): CoroutineDispatcher
}