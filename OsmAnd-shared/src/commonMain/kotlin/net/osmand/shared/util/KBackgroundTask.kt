package net.osmand.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class KBackgroundTask<T> {
	private var job = Job()
	private var scope = CoroutineScope(Dispatchers.IO + job)

	fun execute() {
		scope.launch {
			val result = doInBackground()
			postExecute(result)
			scope.cancel()
		}
	}

	abstract fun doInBackground(): T

	protected suspend fun progress() {
		withContext(Dispatchers.Main) {
			onProgress()
		}
	}

	private suspend fun postExecute(result: T) {
		withContext(Dispatchers.Main) {
			onPostExecute(result)
		}
	}

	open fun onPostExecute(result: T) {

	}

	open fun onProgress() {

	}

}