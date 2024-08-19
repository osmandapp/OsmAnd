package net.osmand.shared.util

/**
 * Common interface that could be used by background operations.
 * Implementation of it depends on the chosen UI platform.
 */
interface IProgress {

	/**
	 * @param taskName
	 * @param work - -1 means that the task is indeterminate,
	 * otherwise a specific number could be specified.
	 */
	fun startTask(taskName: String, work: Int)

	fun startWork(work: Int)

	fun progress(deltaWork: Int)

	fun remaining(remainingWork: Int)

	fun finishTask()

	fun isIndeterminate(): Boolean

	fun isInterrupted(): Boolean

	fun setGeneralProgress(genProgress: String)

	companion object {
		val EMPTY_PROGRESS: IProgress = object : IProgress {
			override fun startTask(taskName: String, work: Int) {}

			override fun startWork(work: Int) {}

			override fun progress(deltaWork: Int) {}

			override fun remaining(remainingWork: Int) {}

			override fun finishTask() {}

			override fun isIndeterminate(): Boolean = true

			override fun isInterrupted(): Boolean = false

			override fun setGeneralProgress(genProgress: String) {}
		}
	}
}
