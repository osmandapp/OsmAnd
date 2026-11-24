package net.osmand.aiconnector.assistant

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import net.osmand.aiconnector.MainActivity
import net.osmand.aiconnector.ConnectorSettings
import net.osmand.aiconnector.OsmAndBridge
import net.osmand.aiconnector.OsmAndTools
import net.osmand.aidlapi.info.AppInfoParams
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

/** One line of the chat as the screen shows it. */
sealed class ChatEvent {
	class User(val text: String) : ChatEvent()
	class Answer(val text: String, val stats: String?) : ChatEvent()
	class Tool(val name: String, val args: String, var result: String? = null, var failed: Boolean = false) : ChatEvent()
	class Info(val text: String) : ChatEvent()
	class Error(val text: String) : ChatEvent()
}

/**
 * The chat with a model on the phone: keeps the conversation, runs the model off the main thread and the
 * tools it calls on OsmAnd. Lives in [net.osmand.aiconnector.AssistantApplication], so a loaded model
 * survives screen rotation and leaving the screen.
 */
class LocalAssistant(private val ctx: Context) {

	val models = LocalModels(ctx)
	val events = mutableListOf<ChatEvent>()
	var listener: (() -> Unit)? = null
	var busy = false
		private set

	/** Debug runs: LiteRT models on the CPU instead of the GPU. */
	var forceCpu = false

	/**
	 * Debug runs from adb: tools that change OsmAnd are not run. Set for one request only (see [send]):
	 * kept for the whole app it silently turned the user's own requests into dry runs.
	 */
	private var dryRun = false

	/**
	 * Stop was tapped: no more tool calls for this request. LiteRT-LM 0.19 cannot interrupt a generation,
	 * so the model finishes the answer it is writing, but nothing more is done in OsmAnd.
	 */
	@Volatile
	private var cancelled = false

	/** Repeat the user's request after each tool result: small models forget the rest of a request with several steps. */
	var remind = true

	private val worker = Executors.newSingleThreadExecutor()
	private val main = Handler(Looper.getMainLooper())
	private val history = mutableListOf<ChatMessage>()
	private var bridge: OsmAndBridge? = null
	private var tools: LocalTools? = null
	private var model: ChatModel? = null
	private var conversationModel: ConversationModel? = null
	private var loadedId: String? = null
	private var chatPrompt: ChatPrompt? = null

	/** The saved chats; the open one is [chatId]. */
	val store = ChatStore(ctx)
	var chatId: String = store.newId()
		private set

	/** Chats started by debug runs are not saved. */
	private var saveChat = true

	/** Whether the loaded model has been given this chat (after a switch it gets the earlier turns again). */
	private var started = false

	init {
		// the chat from last time stays open
		store.list().firstOrNull()?.let { restore(it.id) }
	}

	/** @param dryRun debug runs only: do not change OsmAnd, just tell the model it was done */
	fun send(text: String, dryRun: Boolean = false) {
		if (busy) return
		this.dryRun = dryRun
		val info = models.selected
		if (info == null) {
			post(ChatEvent.Error("No model chosen"))
			return
		}
		busy = true
		cancelled = false
		post(ChatEvent.User(text))
		worker.execute {
			try {
				if (info.engine == ModelEngine.LITERT) {
					runConversation(info, text)
					return@execute
				}
				val m = loadModel(info)
				val t = tools()
				// instructions and tools never change, so the model reads them once per session (KV cache);
				// what changes (location, profile) goes into the user's message
				val prompt = chatPrompt ?: ChatPrompt(INSTRUCTIONS, t.declarations()).also { chatPrompt = it }
				val context = context(t)
				Log.i(TAG, "CONTEXT $context")
				history.add(ChatMessage(Role.USER, if (context.isEmpty()) text else "$context\n\n$text"))
				runTurn(m, t, prompt)
			} catch (e: Throwable) {
				post(ChatEvent.Error(e.message ?: e.toString()))
			} finally {
				main.post {
					done()
					busy = false
					persist()
					listener?.invoke()
				}
			}
		}
	}

	private fun runTurn(m: ChatModel, t: LocalTools, prompt: ChatPrompt) {
		var previous: String? = null
		repeat(maxSteps) {
			if (stopped()) return
			val c = m.complete(prompt.prefix, prompt.render(history), maxAnswerTokens, prompt.stops)
			val call = prompt.parseToolCall(c.text)
			if (call == null) {
				val answer = prompt.cleanAnswer(c.text)
				history.add(ChatMessage(Role.ASSISTANT, answer))
				post(ChatEvent.Answer(answer, stats(c)))
				return
			}
			// a small model may repeat the call it just made instead of answering
			val key = call.name + call.args
			if (key == previous) {
				history.add(ChatMessage(Role.ASSISTANT, "Done."))
				post(ChatEvent.Answer("Done.", stats(c)))
				return
			}
			previous = key
			history.add(ChatMessage(Role.ASSISTANT, prompt.toolCallText(call)))
			val event = ChatEvent.Tool(call.name.removePrefix("osmand_"), call.args.toString())
			post(event)
			val (result, failed) = t.call(call, dryRun).let { it.text to it.failed }
			history.add(ChatMessage(Role.TOOL, result))
			main.post {
				event.result = result
				event.failed = failed
				Log.i(TAG, "RESULT ${if (failed) "failed" else "ok"} ${result.take(300).replace('\n', ' ')}")
				listener?.invoke()
			}
			post(ChatEvent.Info(stats(c)))
		}
		post(ChatEvent.Error("Stopped after $maxSteps tool calls"))
	}

	/** A model that keeps the conversation itself: it is sent only the new message or tool result. */
	private fun runConversation(info: ModelInfo, text: String) {
		val m = loadConversationModel(info)
		val t = tools()
		// the tools may have been made again (another OsmAnd app chosen): they follow the loaded model
		t.vision = info.vision
		if (!started) {
			// a chat opened from the list: the model gets its earlier questions and answers
			m.start(INSTRUCTIONS, t.declarations(), lastTurns())
			started = true
		}
		val context = context(t)
		Log.i(TAG, "CONTEXT $context")
		val message = if (context.isEmpty()) text else "$context\n\n$text"
		history.add(ChatMessage(Role.USER, message))
		var reply = withRoom(m, t, { m.send(message) }) { m.send(message) }
		var previous: String? = null
		var repeated = 0
		repeat(maxSteps) {
			if (stopped()) return
			val call = reply.calls.firstOrNull()
			if (call == null) {
				history.add(ChatMessage(Role.ASSISTANT, reply.text))
				post(ChatEvent.Answer(reply.text.ifEmpty { "Done." }, reply.stats))
				return
			}
			val key = call.name + call.args
			if (key == previous) {
				// the same call again: point the model at the result it has, once, before giving up
				if (repeated++ > 0) {
					post(ChatEvent.Answer("Done.", reply.stats))
					return
				}
				reply = m.sendToolResult(call.name, "You made this call already, its result is above. " +
						"Answer the user with it now: \"$text\".")
				return@repeat
			}
			previous = key
			val event = ChatEvent.Tool(call.name.removePrefix("osmand_"), call.args.toString())
			post(event)
			reply.stats?.let { post(ChatEvent.Info(it)) }
			val r = t.call(call, dryRun)
			main.post {
				event.result = r.text
				event.failed = r.failed
				Log.i(TAG, "RESULT ${if (r.failed) "failed" else "ok"} ${r.text.take(300).replace('\n', ' ')}" +
						(r.image?.let { " + image ${it.size} bytes" } ?: ""))
				listener?.invoke()
			}
			history.add(ChatMessage(Role.TOOL, r.text))
			val forModel = if (remind) "${r.text}\n\n(The user asked: \"$text\". If a part of it is not done yet, " +
					"call the tool for it now. If everything is done, answer the user.)" else r.text
			// on a full context the tool is not run again: the new conversation gets the request and its result
			reply = withRoom(m, t, { m.sendToolResult(call.name, forModel, r.image) }) {
				m.send("$message\n\nResult of ${call.name}:\n$forModel")
			}
		}
		post(ChatEvent.Error("Stopped after $maxSteps tool calls"))
	}

	/**
	 * Sends to the model. When its context is full, the conversation starts again with the
	 * last questions and answers before this request, and [again] sends what is needed to go on.
	 */
	private fun withRoom(m: ConversationModel, t: LocalTools, send: () -> ModelReply, again: () -> ModelReply): ModelReply =
		try {
			send()
		} catch (e: Exception) {
			if (e.message?.contains("RESOURCE_EXHAUSTED") != true) throw e
			Log.w(TAG, "The context is full, starting the conversation again: ${e.message}")
			// the last entry of the history is this request: it goes into again()
			m.start(INSTRUCTIONS, t.declarations(), lastTurns().dropLast(1))
			again()
		}

	/** The questions and answers a model gets again: the last few, so they fit into its context. */
	private fun lastTurns() = history.filter { it.role != Role.TOOL }.takeLast(maxReplayed)

	private fun loadConversationModel(info: ModelInfo): ConversationModel {
		conversationModel?.takeIf { loadedId == info.id }?.let { return it }
		closeModels()
		post(ChatEvent.Info("Loading ${info.title} (${if (info.gpu) "GPU" else "CPU"})…"))
		val start = System.currentTimeMillis()
		val m = LiteRtModel(models.file(info), ctx.cacheDir, info.gpu && !forceCpu, info.vision, info.contextTokens)
		conversationModel = m
		loadedId = info.id
		post(ChatEvent.Info("${info.title} loaded in ${System.currentTimeMillis() - start} ms"))
		return m
	}

	private fun closeModels() {
		model?.close()
		model = null
		conversationModel?.close()
		conversationModel = null
		loadedId = null
		// the next model gets this chat again: switching models keeps the conversation
		started = false
		chatPrompt = null
	}

	/** The last step of a request is done: the probe and tests wait for this line. */
	private fun done() = Log.i(TAG, "DONE")

	/** Starts a new chat; the current one stays in the list. */
	fun newChat(save: Boolean = true) {
		if (busy) return
		chatId = store.newId()
		saveChat = save
		history.clear()
		events.clear()
		started = false
		listener?.invoke()
	}

	fun openChat(id: String) {
		if (busy || id == chatId) return
		restore(id)
		listener?.invoke()
	}

	fun deleteChat(id: String) {
		if (busy) return
		store.delete(id)
		if (id == chatId) newChat()
		listener?.invoke()
	}

	private fun restore(id: String) {
		chatId = id
		saveChat = true
		events.clear()
		events.addAll(store.load(id))
		history.clear()
		for (e in events) {
			when (e) {
				is ChatEvent.User -> history.add(ChatMessage(Role.USER, e.text))
				is ChatEvent.Answer -> history.add(ChatMessage(Role.ASSISTANT, e.text))
				else -> {}
			}
		}
		started = false
	}

	private fun persist() {
		if (saveChat) store.save(chatId, events)
	}

	fun cancel() {
		cancelled = true
		model?.cancel()
	}

	private fun stopped(): Boolean {
		if (cancelled) post(ChatEvent.Info("Stopped"))
		return cancelled
	}

	/** Another OsmAnd app was chosen: the next tool call connects to it. */
	fun osmandChanged() = worker.execute {
		bridge?.disconnect()
		bridge = null
		tools = null
	}

	/** Asks OsmAnd for its status: whether it answers and allows access. */
	fun checkOsmand(done: (ok: Boolean, text: String) -> Unit) = worker.execute {
		val (ok, text) = try {
			// the full answer: what the model reads is cut to its context
			val o = tools().read("osmand_status")
			when {
				o == null -> false to tools().callServer("osmand_status", JSONObject()).text
				// the app's name is shown above the status already
				o.optBoolean("enabled") -> true to o.optString("package")
				else -> false to ctx.getString(net.osmand.aiconnector.R.string.access_denied)
			}
		} catch (e: Exception) {
			false to (e.message ?: e.toString())
		}
		main.post { done(ok, text) }
	}

	/** Runs one tool directly (debug runs); the result goes to the log. */
	fun runTool(name: String, args: String) = worker.execute {
		val t = tools()
		// server tools go straight to OsmAnd, without the checks made for the model (a restore may write "")
		val r = if (name == "osmand_find_setting" || name == "osmand_look_at_screen") t.call(ToolCall(name, JSONObject(args)))
		else t.callServer(name, JSONObject(args))
		Log.i(TAG, "DIRECT $name ${if (r.failed) "failed" else "ok"} ${r.text.take(3500).replace('\n', ' ')}")
	}

	/** Frees the model; the next request loads it again (with the current thread settings). */
	fun unload() = worker.execute {
		conversationModel?.close()
		conversationModel = null
		model?.close()
		model = null
		loadedId = null
	}

	/** The model is loaded once and kept; choosing another model frees the old one. */
	private fun loadModel(info: ModelInfo): ChatModel {
		model?.takeIf { loadedId == info.id }?.let { return it }
		// frees a LiteRT engine too: two models do not fit in memory
		closeModels()
		post(ChatEvent.Info("Loading ${info.title}…"))
		val start = System.currentTimeMillis()
		val m = when (info.engine) {
			ModelEngine.GEMINI_NANO -> GeminiNanoModel()
			ModelEngine.LITERT -> throw IllegalStateException("LiteRT models keep their own conversation")
		}
		model = m
		loadedId = info.id
		post(ChatEvent.Info("${info.title} loaded in ${System.currentTimeMillis() - start} ms"))
		return m
	}

	private fun tools(): LocalTools {
		tools?.let { return it }
		val b = OsmAndBridge(ctx, ConnectorSettings.osmandPackage(ctx))
		bridge = b
		return LocalTools(OsmAndTools(b, ctx.assets), SettingsCatalog(ctx, ConnectorSettings.osmandPackage(ctx))).also {
			it.showOsmAnd = {
				ctx.packageManager.getLaunchIntentForPackage(ConnectorSettings.osmandPackage(ctx))?.let { launch ->
					ctx.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
					// the map has to be drawn before OsmAnd can take its picture
					Thread.sleep(2500)
				}
			}
			it.showChat = {
				ctx.startActivity(Intent(ctx, MainActivity::class.java)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
			}
			tools = it
		}
	}

	/** Where the user and the map are and the profile: a small model must not guess coordinates. */
	private fun context(t: LocalTools): String {
		val info: AppInfoParams? = try {
			bridge?.get()?.appInfo
		} catch (e: Exception) {
			null
		}
		val lines = mutableListOf<String>()
		// the location comes from OsmAnd only (the assistant has no location permission of its own);
		// while OsmAnd does not know it, the map center stands in for "near me"
		info?.lastKnownLocation?.let {
			lines.add("My location: lat ${fmt(it.latitude)}, lon ${fmt(it.longitude)}.")
		}
		val map = info?.mapLocation?.let { it.latitude to it.longitude } ?: savedMapCenter(t)
		map?.let { lines.add("Map center: lat ${fmt(it.first)}, lon ${fmt(it.second)}.") }
		t.read("osmand_status")?.optString("current_profile")?.takeIf { it.isNotEmpty() }?.let {
			lines.add("Current profile: $it.")
		}
		return if (lines.isEmpty()) "" else "[${lines.joinToString(" ")}]"
	}

	/** Where the map was when OsmAnd closed (its settings keep it). */
	private fun savedMapCenter(t: LocalTools): Pair<Double, Double>? {
		val prefs = t.read("osmand_get_preference",
			JSONObject().put("pref_ids", org.json.JSONArray().put("last_known_map_lat").put("last_known_map_lon")))
			?: return null
		val lat = prefs.optString("last_known_map_lat").toDoubleOrNull() ?: return null
		val lon = prefs.optString("last_known_map_lon").toDoubleOrNull() ?: return null
		return lat to lon
	}

	private fun fmt(v: Double) = String.format(Locale.US, "%.4f", v)

	private fun stats(c: Completion): String {
		if (c.promptTokens == 0) return "${c.generateMs} ms"
		val fresh = c.promptTokens - c.cachedTokens
		val speed = if (c.generateMs > 0) c.generatedTokens * 1000 / c.generateMs else 0
		return "prompt ${c.promptTokens} tok (new $fresh) ${c.prefillMs} ms · answer ${c.generatedTokens} tok " +
				"${c.generateMs} ms, $speed tok/s"
	}

	private fun post(e: ChatEvent) = main.post {
		events.add(e)
		persist()
		Log.i(TAG, describe(e))
		listener?.invoke()
	}

	private fun describe(e: ChatEvent) = when (e) {
		is ChatEvent.User -> "USER ${e.text}"
		is ChatEvent.Answer -> "ANSWER ${e.text} | ${e.stats}"
		is ChatEvent.Tool -> "TOOL ${e.name} ${e.args}"
		is ChatEvent.Info -> "INFO ${e.text}"
		is ChatEvent.Error -> "ERROR ${e.text}"
	}

	private val TAG = "OsmAndAssistant"
	private val INSTRUCTIONS = "You are the OsmAnd map assistant on the user's phone. You control the OsmAnd app " +
			"with tools. Call a tool when the request needs one, otherwise answer briefly in the user's language. " +
			"A user message may start with [my location, map center, current profile] in brackets: use it for " +
			"\"near me\" and \"here\"; without my location use the map center. Never invent coordinates: find a place with osmand_search first and use the " +
			"coordinates it returns. To change a setting or the map style, find its pref_id with osmand_find_setting " +
			"first, then call osmand_set_preference with that pref_id and one of its values. Do every part of the request, one tool call at a time. " +
			"When all is done, tell the user in one short sentence what was done."
	private val maxSteps = 8
	private val maxReplayed = 6
	private val maxAnswerTokens = 256
}
