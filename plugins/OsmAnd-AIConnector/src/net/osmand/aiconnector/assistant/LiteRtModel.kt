package net.osmand.aiconnector.assistant

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** A model answer in a conversation: tool calls to run, or text for the user. */
class ModelReply(val text: String, val calls: List<ToolCall>, val stats: String?)

/**
 * A model that keeps the conversation itself (its chat template, tool format and KV cache) and is told
 * only what is new: the user's message or a tool result.
 */
interface ConversationModel {

	/** @param earlier the chat so far, when it is opened again from the list */
	fun start(instructions: String, tools: JSONArray, earlier: List<ChatMessage> = emptyList())

	fun send(userText: String): ModelReply

	/** @param image a picture the tool made (a screenshot), for models that see pictures */
	fun sendToolResult(name: String, result: String, image: ByteArray? = null): ModelReply

	fun close()
}

/**
 * Google's LiteRT-LM runtime (.litertlm files): on the GPU (OpenCL) or the CPU, on some chips the NPU.
 * The file carries the model's chat template and tool format; the runtime parses the tool calls.
 */
@OptIn(ExperimentalApi::class)
class LiteRtModel(file: File, cacheDir: File, gpu: Boolean, vision: Boolean = false, contextTokens: Int = 4096) :
	ConversationModel {

	private val engine: Engine
	private var conversation: Conversation? = null

	init {
		ExperimentalFlags.enableBenchmark = true
		val backend = if (gpu) Backend.GPU() else Backend.CPU()
		// the vision encoder of multimodal models (Gemma) runs on the GPU too
		val visionBackend = if (vision) Backend.GPU() else null
		engine = Engine(EngineConfig(file.absolutePath, backend, visionBackend, null, contextTokens,
			if (vision) 1 else null, cacheDir.absolutePath))
		// compiles the GPU kernels on the first run (cached in cacheDir afterwards)
		engine.initialize()
	}

	override fun start(instructions: String, tools: JSONArray, earlier: List<ChatMessage>) {
		conversation?.close()
		val providers = (0 until tools.length()).map { i ->
			val declaration = tools.getJSONObject(i).toString()
			tool(object : OpenApiTool {
				override fun getToolDescriptionJsonString() = declaration

				// the assistant runs the tools itself (automatic tool calling is off)
				override fun execute(paramsJsonString: String) = "{}"
			})
		}
		// greedy, no automatic tool calls, no reasoning (Qwen3 thinks by default)
		conversation = engine.createConversation(ConversationConfig(
			Contents.of(instructions),
			earlier.map { if (it.role == Role.USER) Message.user(it.text) else Message.model(it.text) }, providers, SamplerConfig(1, 1.0, 0.0, 0), false,
			null, mapOf("enable_thinking" to false), null, false, 512, ThinkingConfig(false)))
	}

	override fun send(userText: String) = reply(conv().sendMessage(Message.user(userText)))

	override fun sendToolResult(name: String, result: String, image: ByteArray?): ModelReply {
		val response: Any = try {
			JSONObject(result).toString()
		} catch (e: Exception) {
			result
		}
		if (image != null) {
			// the chat template has no place for a picture in a tool turn ("more images than expected"):
			// the screenshot comes as the user's message
			return reply(conv().sendMessage(Message.user(Contents.of(Content.ImageBytes(image), Content.Text(result)))))
		}
		return reply(conv().sendMessage(Message.tool(Contents.of(Content.ToolResponse(name, response)))))
	}

	private fun reply(m: Message): ModelReply {
		val text = m.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
		val calls = m.toolCalls.map { ToolCall(it.name, json(it.arguments) as JSONObject) }
		return ModelReply(text.trim(), calls, stats())
	}

	/**
	 * Gson numbers (LazilyParsedNumber) are not types org.json knows: JSONObject(map) turns them into null,
	 * which lost every coordinate. Convert the values by hand.
	 */
	private fun json(v: Any?): Any = when (v) {
		null -> JSONObject.NULL
		is Map<*, *> -> JSONObject().also { o -> v.forEach { (k, x) -> o.put(k.toString(), json(x)) } }
		is List<*> -> JSONArray().also { a -> v.forEach { a.put(json(it)) } }
		is Number -> v.toDouble().let { d -> if (d == Math.rint(d) && Math.abs(d) < 1e15) d.toLong() else d }
		is Boolean, is String -> v
		else -> v.toString()
	}

	private fun stats(): String? = try {
		val b = conv().getBenchmarkInfo()
		String.format(Locale.US, "prompt %d tok %.0f tok/s · answer %d tok %.1f tok/s",
			b.lastPrefillTokenCount, b.lastPrefillTokensPerSecond, b.lastDecodeTokenCount, b.lastDecodeTokensPerSecond)
	} catch (e: Exception) {
		null
	}

	private fun conv() = conversation ?: throw IllegalStateException("No conversation")

	override fun close() {
		conversation?.close()
		conversation = null
		engine.close()
	}
}
