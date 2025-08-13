package net.osmand.shared.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

actual object DispatcherProvider {
	@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
	actual fun singleThread(): CoroutineDispatcher = newSingleThreadContext("SingleThread")
}