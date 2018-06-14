package net.osmand.telegram.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CancellableAsyncTask(val taskId: String, val executeTimeout: Long = 0) {

	companion object {
		private const val SLEEP_TIME = 50L
		private val requestNumbersMap = ConcurrentHashMap<String, AtomicInteger>()
		private val singleThreadExecutorsMap = ConcurrentHashMap<String, ExecutorService>()

		fun run(taskId: String, executeTimeout: Long = 0, action: (() -> Unit)) {
			CancellableAsyncTask(taskId, executeTimeout).run(action)
		}

		fun clearResources(taskId: String) {
			requestNumbersMap.remove(taskId)
			singleThreadExecutorsMap.remove(taskId)
		}
	}

	private val singleThreadExecutor: ExecutorService
	private var requestNumber: AtomicInteger

	var isCancelled: Boolean = false

	init {
		val requestNumber = requestNumbersMap[taskId]
		if (requestNumber == null) {
			this.requestNumber = AtomicInteger()
			requestNumbersMap[taskId] = this.requestNumber
		} else {
			this.requestNumber = requestNumber
		}

		val singleThreadExecutor = singleThreadExecutorsMap[taskId]
		if (singleThreadExecutor == null) {
			this.singleThreadExecutor = Executors.newSingleThreadExecutor()
			singleThreadExecutorsMap[taskId] = this.singleThreadExecutor
		} else {
			this.singleThreadExecutor = singleThreadExecutor
		}
	}

	fun run(action: (() -> Unit)) {
		val req = requestNumber.incrementAndGet()

		singleThreadExecutor.submit(object : Runnable {

			private val isCancelled: Boolean
				get() = requestNumber.get() != req || this@CancellableAsyncTask.isCancelled

			override fun run() {
				try {
					if (executeTimeout > 0) {
						val startTime = System.currentTimeMillis()
						while (System.currentTimeMillis() - startTime <= executeTimeout) {
							if (isCancelled) {
								return
							}
							Thread.sleep(SLEEP_TIME)
						}
					}
					if (!isCancelled) {
						action.invoke()
					}
				} catch (e: InterruptedException) {
					// ignore
				}
			}
		})
	}
}
