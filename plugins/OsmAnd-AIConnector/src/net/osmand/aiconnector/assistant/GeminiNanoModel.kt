package net.osmand.aiconnector.assistant

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures
import java.util.concurrent.TimeUnit

/**
 * Gemini Nano through ML Kit GenAI (Android AICore). The phone keeps the model; the app neither downloads
 * nor stores it. Only on phones where AICore offers the Prompt API (Pixel 9 and newer, some others).
 */
class GeminiNanoModel : ChatModel {

	private val model = GenerativeModelFutures.from(Generation.getClient())

	/** FeatureStatus: UNAVAILABLE, DOWNLOADABLE, DOWNLOADING or AVAILABLE. */
	fun status(): Int = model.checkStatus().get(30, TimeUnit.SECONDS)

	fun download(onProgress: (Long) -> Unit, onDone: (String?) -> Unit) {
		model.download(object : DownloadCallback {
			override fun onDownloadStarted(bytesToDownload: Long) {}
			override fun onDownloadProgress(totalBytesDownloaded: Long) = onProgress(totalBytesDownloaded)
			override fun onDownloadCompleted() = onDone(null)
			override fun onDownloadFailed(e: GenAiException) = onDone(e.message ?: e.toString())
		})
	}

	override fun complete(prefix: String, prompt: String, maxTokens: Int, stops: List<String>): Completion {
		if (status() != FeatureStatus.AVAILABLE) {
			throw IllegalStateException("Gemini Nano is not ready on this phone")
		}
		val request = GenerateContentRequest.Builder(TextPart(prompt.removePrefix(prefix))).apply {
			// the instructions and tools are the same in every call: AICore can keep them cached
			promptPrefix = PromptPrefix(prefix)
			temperature = 0f
			topK = 1
			maxOutputTokens = maxTokens
		}.build()
		val start = System.currentTimeMillis()
		val text = model.generateContent(request).get(120, TimeUnit.SECONDS).candidates.firstOrNull()?.text ?: ""
		return Completion(text, generateMs = System.currentTimeMillis() - start)
	}

	override fun cancel() {}

	override fun close() {}
}
