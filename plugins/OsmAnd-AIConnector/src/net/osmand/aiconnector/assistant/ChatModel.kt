package net.osmand.aiconnector.assistant

/** What one model call cost: shown under the answer, so the speed on the phone is visible. */
class Completion(
	val text: String,
	val promptTokens: Int = 0,
	val cachedTokens: Int = 0,
	val generatedTokens: Int = 0,
	val prefillMs: Long = 0,
	val generateMs: Long = 0
)

/** A model running on the phone. Calls block; they run off the main thread. */
interface ChatModel {

	/**
	 * @param prefix the part of the prompt that is the same for every call (instructions and tools),
	 * a model may keep it cached
	 * @param prompt the whole prompt, starting with [prefix]
	 */
	fun complete(prefix: String, prompt: String, maxTokens: Int, stops: List<String>): Completion

	fun cancel()

	fun close()
}
