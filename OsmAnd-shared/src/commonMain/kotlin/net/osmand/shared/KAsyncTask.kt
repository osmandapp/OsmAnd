package net.osmand.shared

import co.touchlab.stately.concurrency.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class KAsyncTask<Params, Progress, Result>(
	ioTask: Boolean = false,
	private val callback: AsyncTaskCallback? = null,
	context: CoroutineDispatcher? = null
) {
	private val mainScope = CoroutineScope(Dispatchers.Main)
	private val backgroundContext: CoroutineDispatcher = context ?: if (ioTask) Dispatchers.IO else Dispatchers.Default

	private var cancelled = AtomicBoolean(false)
	private var running = AtomicBoolean(false)

	open fun onPreExecute() {}

	abstract suspend fun doInBackground(vararg params: Params): Result

	open fun onPostExecute(result: Result) {}

	open fun onProgressUpdate(vararg values: Progress) {}

	open fun onCancelled() {}

	protected fun publishProgress(vararg values: Progress) {
		if (isCancelled()) return
		mainScope.launch {
			onProgressUpdate(*values)
		}
	}

	fun execute(vararg params: Params) {
		start()
		mainScope.launch {
			if (!isCancelled()) {
				onPreExecute()
			}
			var result: Result? = null
			if (!isCancelled()) {
				result = withContext(backgroundContext) {
					doInBackground(*params)
				}
			}
			if (!isCancelled()) {
				result?.let { onPostExecute(result) }
			} else {
				onCancelled()
			}
			finish()
		}
	}

	fun isCancelled(): Boolean {
		return cancelled.value || callback?.isCancelled() == true
	}

	fun isRunning() = running.value

	fun cancel() {
		cancelled.value = true
	}

	private fun start() {
		running.value = true
	}

	private fun finish() {
		running.value = false
	}

	interface AsyncTaskCallback {
		fun isCancelled(): Boolean
	}
}