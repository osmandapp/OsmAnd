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
	private val bridgeScript: String?
) : NanoHTTPD(host, port) {

	companion object {
		const val BRIDGE_FILE = "osmand_mcp_bridge.py"
		private const val TAG = "OsmAndAiConnector"
		private val SUPPORTED_VERSIONS = listOf("2025-06-18", "2025-03-26", "2024-11-05")
		private const val INSTRUCTIONS = "Controls the OsmAnd map app on an Android phone through OsmAnd's AIDL API. " +
				"Start with osmand_status. Settings are changed with osmand_set_preference; most settings are per profile. " +
				"For pictures: show a track, point the camera with osmand_set_camera, then osmand_screenshot; " +
				"track numbers come from osmand_track_stats and osmand_track_points."
	}

	override fun serve(session: IHTTPSession): Response {
		// the computer downloads the stdio bridge once; it holds no key, so no authorization
		if (session.uri == "/$BRIDGE_FILE" && session.method == Method.GET && bridgeScript != null) {
			return newFixedLengthResponse(Response.Status.OK, "text/x-python", bridgeScript)
		}
		if (session.uri.trimEnd('/') != "/mcp") return text(Response.Status.NOT_FOUND, "Not found")
		val origin = session.headers["origin"]
		if (origin != null && !origin.matches(Regex("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?"))) {
			return text(Response.Status.FORBIDDEN, "Forbidden origin")
		}
		if (session.headers["authorization"] != "Bearer $token") {
			return text(Response.Status.UNAUTHORIZED, "Unauthorized")
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
					rpcResult(id, tools.call(name, params.optJSONObject("arguments") ?: JSONObject()))
				}
				else -> rpcError(id, -32601, "Method not found: $method")
			}
		} catch (e: Exception) {
			rpcError(id, -32602, e.message ?: e.toString())
		}
		return json(response)
	}

	private fun rpcResult(id: Any?, result: JSONObject) =
		JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)

	private fun rpcError(id: Any?, code: Int, message: String) =
		JSONObject().put("jsonrpc", "2.0").put("id", id ?: JSONObject.NULL)
			.put("error", JSONObject().put("code", code).put("message", message))

	private fun json(o: JSONObject) = newFixedLengthResponse(Response.Status.OK, "application/json", o.toString())

	private fun text(status: Response.Status, msg: String) = newFixedLengthResponse(status, MIME_PLAINTEXT, msg)
}

