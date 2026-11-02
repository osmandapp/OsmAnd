package net.osmand.shared.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.osmand.shared.io.DispatcherProvider

/**
 * Runs the tasks it is given one after another, each to its end, on a thread of its own started
 * with the first task: java's `ThreadPoolExecutor` of one thread, which the search keeps for its
 * work in the background. What a task throws is dropped, as the future of java's executor keeps
 * it where nobody reads it.
 */
internal class SerialExecutor {

	private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)
	private val worker = lazy {
		val queue = tasks
		CoroutineScope(DispatcherProvider.singleThread()).launch {
			for (task in queue) {
				try {
					task()
				} catch (e: Throwable) {
					// dropped
				}
			}
		}
	}

	fun submit(task: suspend () -> Unit) {
		worker.value
		tasks.trySend(task)
	}
}
