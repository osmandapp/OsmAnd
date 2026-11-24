package net.osmand.aiconnector.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A saved chat as the list shows it. */
class ChatSummary(val id: String, val title: String, val updated: Long)

/** The chats on the phone: one JSON file per chat in the app's files, removed with the app. */
class ChatStore(ctx: Context) {

	private val dir = File(ctx.filesDir, "chats").apply { mkdirs() }

	fun newId() = System.currentTimeMillis().toString()

	/** Newest first. */
	fun list(): List<ChatSummary> = (dir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
		.mapNotNull { f ->
			try {
				val o = JSONObject(f.readText())
				ChatSummary(o.getString("id"), o.optString("title"), o.optLong("updated"))
			} catch (e: Exception) {
				null
			}
		}
		.sortedByDescending { it.updated }

	fun save(id: String, events: List<ChatEvent>) {
		val title = events.filterIsInstance<ChatEvent.User>().firstOrNull()?.text ?: return
		val o = JSONObject().put("id", id).put("title", title.take(80)).put("updated", System.currentTimeMillis())
			.put("events", JSONArray().apply { events.forEach { put(write(it)) } })
		File(dir, "$id.json").writeText(o.toString())
	}

	fun load(id: String): List<ChatEvent> = try {
		val a = JSONObject(File(dir, "$id.json").readText()).getJSONArray("events")
		(0 until a.length()).mapNotNull { read(a.getJSONObject(it)) }
	} catch (e: Exception) {
		emptyList()
	}

	fun delete(id: String) = File(dir, "$id.json").delete()

	private fun write(e: ChatEvent): JSONObject = when (e) {
		is ChatEvent.User -> JSONObject().put("type", "user").put("text", e.text)
		is ChatEvent.Answer -> JSONObject().put("type", "answer").put("text", e.text).put("stats", e.stats)
		is ChatEvent.Tool -> JSONObject().put("type", "tool").put("name", e.name).put("args", e.args)
			.put("result", e.result).put("failed", e.failed)
		is ChatEvent.Info -> JSONObject().put("type", "info").put("text", e.text)
		is ChatEvent.Error -> JSONObject().put("type", "error").put("text", e.text)
	}

	private fun read(o: JSONObject): ChatEvent? = when (o.optString("type")) {
		"user" -> ChatEvent.User(o.getString("text"))
		"answer" -> ChatEvent.Answer(o.getString("text"), o.optString("stats").ifEmpty { null })
		"tool" -> ChatEvent.Tool(o.getString("name"), o.getString("args"), o.optString("result").ifEmpty { null },
			o.optBoolean("failed"))
		"info" -> ChatEvent.Info(o.getString("text"))
		"error" -> ChatEvent.Error(o.getString("text"))
		else -> null
	}
}
