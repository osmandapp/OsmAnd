package net.osmand.aiconnector.assistant

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import net.osmand.aiconnector.R
import java.io.File

enum class ModelEngine {
	/** Google LiteRT-LM: a .litertlm file run on the GPU, the runtime keeps the conversation */
	LITERT,

	/** Gemini Nano in Android AICore (ML Kit), nothing to download in this app */
	GEMINI_NANO
}

class ModelInfo(
	val id: String,
	val title: String,
	@StringRes val summary: Int,
	val engine: ModelEngine,
	val url: String? = null,
	val sizeBytes: Long = 0,
	/** LiteRT-LM: run on the GPU (OpenCL), otherwise the CPU */
	val gpu: Boolean = false,
	/** the model sees pictures: it gets screenshots of OsmAnd */
	val vision: Boolean = false,
	/** the best model in our tests: shown first and marked */
	val recommended: Boolean = false,
	/** the conversation's length in tokens: instructions, tools, questions, results and answers */
	val contextTokens: Int = 4096
) {
	val fileName: String? get() = url?.substringAfterLast('/')
}

sealed class ModelState {
	object NotDownloaded : ModelState()
	class Downloading(val done: Long, val total: Long) : ModelState()
	object Ready : ModelState()
	class Failed(val reason: String) : ModelState()
}

/** The models the assistant can run on the phone, their files and which one is chosen. */
class LocalModels(private val ctx: Context) {

	val all = listOf(
		ModelInfo("gemma-4-e2b-litert", "Gemma 4 E2B", R.string.model_gemma4_litert, ModelEngine.LITERT,
			"https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm",
			2_340_870_772, gpu = true, vision = true, recommended = true, contextTokens = 8192),
		ModelInfo("qwen3-1.7b-litert", "Qwen3 1.7B", R.string.model_qwen3_litert, ModelEngine.LITERT,
			"https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
			977_184_032, gpu = true),
		ModelInfo("gemini-nano", "Gemini Nano", R.string.model_gemini_nano, ModelEngine.GEMINI_NANO)
	)

	private val prefs = ctx.getSharedPreferences("assistant", Context.MODE_PRIVATE)
	private val downloads = ctx.getSystemService(DownloadManager::class.java)

	var selected: ModelInfo?
		get() = all.firstOrNull { it.id == prefs.getString("model", null) }
		set(value) = prefs.edit().putString("model", value?.id).apply()

	/** Model files live in the app's external files dir: no permission needed, removed with the app. */
	fun file(model: ModelInfo): File = File(ctx.getExternalFilesDir("models"), model.fileName!!)

	fun state(model: ModelInfo): ModelState {
		if (model.engine == ModelEngine.GEMINI_NANO) return ModelState.Ready
		val id = prefs.getLong(downloadKey(model), -1)
		if (id >= 0) {
			downloads.query(DownloadManager.Query().setFilterById(id))?.use { c ->
				if (c.moveToFirst()) {
					val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
					val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
					val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
					when (status) {
						DownloadManager.STATUS_SUCCESSFUL ->
							prefs.edit().remove(downloadKey(model)).putBoolean(completeKey(model), true).apply()
						DownloadManager.STATUS_FAILED -> {
							prefs.edit().remove(downloadKey(model)).apply()
							file(model).delete()
							val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
							return ModelState.Failed(reason.toString())
						}
						else -> return ModelState.Downloading(done, if (total > 0) total else model.sizeBytes)
					}
				} else {
					prefs.edit().remove(downloadKey(model)).apply()
				}
			}
		}
		// a file copied by hand (adb push) counts too, as long as it is complete
		val f = file(model)
		// the size in the code is only a check for files copied by hand: the server may have a newer file
		val complete = f.exists() && (f.length() == model.sizeBytes || prefs.getBoolean(completeKey(model), false))
		return if (complete) ModelState.Ready else ModelState.NotDownloaded
	}

	fun download(model: ModelInfo) {
		file(model).delete()
		prefs.edit().remove(completeKey(model)).apply()
		val request = DownloadManager.Request(Uri.parse(model.url))
			.setTitle(model.title)
			.setDescription(ctx.getString(R.string.app_name))
			.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
			.setDestinationInExternalFilesDir(ctx, "models", model.fileName)
		prefs.edit().putLong(downloadKey(model), downloads.enqueue(request)).apply()
	}

	fun delete(model: ModelInfo) {
		val id = prefs.getLong(downloadKey(model), -1)
		if (id >= 0) downloads.remove(id)
		prefs.edit().remove(downloadKey(model)).remove(completeKey(model)).apply()
		file(model).delete()
		if (selected?.id == model.id) selected = null
	}

	private fun downloadKey(model: ModelInfo) = "download_${model.id}"

	private fun completeKey(model: ModelInfo) = "complete_${model.id}"
}
