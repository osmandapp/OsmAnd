package net.osmand.aiconnector.assistant

import org.json.JSONArray
import org.json.JSONObject

enum class Role { USER, ASSISTANT, TOOL }

class ChatMessage(val role: Role, val text: String)

class ToolCall(val name: String, val args: JSONObject)

/**
 * Writes the conversation as one text prompt for a model without a chat template of its own (Gemini Nano)
 * and reads its tool calls back: the model answers with plain JSON {"tool": ..., "arguments": ...}.
 */
class ChatPrompt(private val instructions: String, private val tools: JSONArray) {

	/** Instructions and tools: identical in every call of a chat, the model keeps them cached. */
	val prefix: String = instructions + "\n\nTools (JSON schema):\n" + tools.toString() +
			"\n\nTo call a tool reply ONLY with JSON: {\"tool\": \"<name>\", \"arguments\": {...}}. " +
			"If no tool is needed reply with plain text.\n\n"

	val stops: List<String> = listOf("\nUser:")

	fun render(history: List<ChatMessage>): String {
		val sb = StringBuilder(prefix)
		for (m in history) {
			when (m.role) {
				Role.USER -> sb.append("User: ${m.text}\n")
				Role.ASSISTANT -> sb.append("Assistant: ${m.text}\n")
				Role.TOOL -> sb.append("Tool result: ${m.text}\n$AFTER_TOOL\n")
			}
		}
		sb.append("Assistant:")
		return sb.toString()
	}

	/** How an assistant turn with a tool call is written back into the history. */
	fun toolCallText(call: ToolCall): String = JSONObject().put("tool", call.name).put("arguments", call.args).toString()

	/** The tool call in a model answer, or null when the answer is text for the user. */
	fun parseToolCall(answer: String): ToolCall? {
		val start = answer.indexOf('{')
		if (start < 0) return null
		val json = balancedObject(answer, start) ?: return null
		val o = try {
			JSONObject(json)
		} catch (e: Exception) {
			return null
		}
		val name = o.optString("name").ifEmpty { o.optString("tool") }
		if (name.isEmpty()) return null
		val args = o.optJSONObject("arguments") ?: o.optJSONObject("parameters") ?: JSONObject()
		return ToolCall(name, args)
	}

	fun cleanAnswer(answer: String): String = answer.trim()

	/** Without it, models that call tools with plain JSON repeat the same call after its result. */
	private val AFTER_TOOL = "The tool call is done. Now answer the user in plain text, or call another tool " +
			"only if the request needs one more step."

	private fun balancedObject(s: String, start: Int): String? {
		var depth = 0
		var inString = false
		var i = start
		while (i < s.length) {
			val c = s[i]
			if (inString) {
				if (c == '\\') i++ else if (c == '"') inString = false
			} else when (c) {
				'"' -> inString = true
				'{' -> depth++
				'}' -> if (--depth == 0) return s.substring(start, i + 1)
			}
			i++
		}
		return null
	}
}
