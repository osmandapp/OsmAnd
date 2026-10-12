package net.osmand.aiconnector

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.DataInputStream

/** Minimal MCP Streamable HTTP server: POST /mcp with JSON-RPC, plain JSON responses, no sessions. */
class McpHttpServer(
	host: String,
	port: Int,
	private val token: String,
	private val tools: OsmAndTools,
	private val bridgeScript: String?,
	private val onActivity: () -> Unit = {}
) : NanoHTTPD(host, port) {

	companion object {
		const val BRIDGE_FILE = "osmand_mcp_bridge.py"
		private const val TAG = "OsmAndAiConnector"
		private val SUPPORTED_VERSIONS = listOf("2025-06-18", "2025-03-26", "2024-11-05")
		private const val INSTRUCTIONS = "Controls the OsmAnd map app on an Android phone through OsmAnd's AIDL API. " +
				"Start with osmand_status. Settings are changed with osmand_set_preference; most settings are per profile. " +
				"For pictures: show a track, point the camera with osmand_set_camera, then osmand_screenshot; " +
				"track numbers come from osmand_track_stats and osmand_track_points. " +
				"Before bigger tasks read the guide: osmand_guide topic flyover before a flyover video."
	}

	override fun serve(session: IHTTPSession): Response {
		// the computer downloads the stdio bridge once; it holds no key, so no authorization
		if (session.uri == "/$BRIDGE_FILE" && session.method == Method.GET && bridgeScript != null) {
			return newFixedLengthResponse(Response.Status.OK, "text/x-python", bridgeScript)
		}
		val files = session.uri.startsWith(FLYOVER_FILES_PATH)
		if (session.uri.trimEnd('/') != "/mcp" && !files) return text(Response.Status.NOT_FOUND, "Not found")
		val origin = session.headers["origin"]
		if (origin != null && !origin.matches(Regex("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?"))) {
			return text(Response.Status.FORBIDDEN, "Forbidden origin")
		}
		if (session.headers["authorization"] != "Bearer $token") {
			return text(Response.Status.UNAUTHORIZED, "Unauthorized")
		}
		if (files) {
			return if (session.method == Method.GET) flyoverFile(session) else text(Response.Status.METHOD_NOT_ALLOWED, "Use GET")
		}
		return when (session.method) {
			Method.POST -> handlePost(session)
			Method.DELETE -> text(Response.Status.OK, "")
			else -> text(Response.Status.METHOD_NOT_ALLOWED, "Use POST")
		}
	}

	private fun handlePost(session: IHTTPSession): Response {
		val length = session.headers["content-length"]?.toIntOrNull() ?: 0
		val body = ByteArray(length)
		DataInputStream(session.inputStream).readFully(body)
		val req = try {
			JSONObject(String(body, Charsets.UTF_8))
		} catch (e: Exception) {
			return json(rpcError(null, -32700, "Parse error"))
		}
		val id = req.opt("id")
		val method = req.optString("method")
		if (id == null) {
			// notification, e.g. notifications/initialized
			return newFixedLengthResponse(Response.Status.ACCEPTED, MIME_PLAINTEXT, "")
		}
		val params = req.optJSONObject("params") ?: JSONObject()
		val response = try {
			when (method) {
				"initialize" -> {
					val asked = params.optString("protocolVersion")
					rpcResult(
						id, JSONObject()
							.put("protocolVersion", if (asked in SUPPORTED_VERSIONS) asked else SUPPORTED_VERSIONS[0])
							.put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
							.put("serverInfo", JSONObject().put("name", "osmand-ai-connector").put("version", BuildConfig.VERSION_NAME))
							.put("instructions", INSTRUCTIONS)
					)
				}
				"ping" -> rpcResult(id, JSONObject())
				"tools/list" -> rpcResult(id, JSONObject().put("tools", tools.list()))
				"tools/call" -> {
					val name = params.getString("name")
					Log.i(TAG, "tools/call $name")
					onActivity()
					try {
						rpcResult(id, tools.call(name, params.optJSONObject("arguments") ?: JSONObject()))
					} finally {
						// a long call (a flyover) counts from its end
						onActivity()
					}
				}
				else -> rpcError(id, -32601, "Method not found: $method")
			}
		} catch (e: Exception) {
			rpcError(id, -32602, e.message ?: e.toString())
		}
		return json(response)
	}

	/** A recorded flyover video, streamed; "Range: bytes=N-" continues a download that broke off. */
	private fun flyoverFile(session: IHTTPSession): Response {
		val name = session.uri.removePrefix(FLYOVER_FILES_PATH)
		onActivity()
		val opened = try {
			tools.openFlyover(name)
		} catch (e: Exception) {
			return text(Response.Status.INTERNAL_ERROR, e.message ?: e.toString())
		}
		val (input, size) = opened ?: return text(Response.Status.NOT_FOUND, "No finished flyover video '$name' " +
				"(an older OsmAnd cannot hand videos out, or the app lacks the Screenshots group)")
		val range = Regex("bytes=(\\d+)-(\\d*)").matchEntire(session.headers["range"] ?: "")
		val from = range?.groupValues?.get(1)?.toLongOrNull() ?: 0L
		if (from >= size && size > 0) {
			input.close()
			return text(Response.Status.RANGE_NOT_SATISFIABLE, "The video has $size bytes")
		}
		val to = range?.groupValues?.get(2)?.toLongOrNull()?.coerceAtMost(size - 1) ?: (size - 1)
		var skipped = 0L
		while (skipped < from) {
			val n = input.skip(from - skipped)
			if (n <= 0) break
			skipped += n
		}
		val status = if (range != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
		return newFixedLengthResponse(status, "video/mp4", input, to - from + 1).apply {
			addHeader("Accept-Ranges", "bytes")
			addHeader("Content-Disposition", "attachment; filename=\"${name.replace("\"", "")}\"")
			if (range != null) addHeader("Content-Range", "bytes $from-$to/$size")
		}
	}

	private fun rpcResult(id: Any?, result: JSONObject) =
		JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)

	private fun rpcError(id: Any?, code: Int, message: String) =
		JSONObject().put("jsonrpc", "2.0").put("id", id ?: JSONObject.NULL)
			.put("error", JSONObject().put("code", code).put("message", message))

	private fun json(o: JSONObject) = newFixedLengthResponse(Response.Status.OK, "application/json", o.toString())

	private fun text(status: Response.Status, msg: String) = newFixedLengthResponse(status, MIME_PLAINTEXT, msg)
}

