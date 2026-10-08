package net.osmand.aiconnector

import androidx.annotation.StringRes

/**
 * How to add the connector to an AI assistant. Each client gets where to put it and the exact text to copy.
 * With the bridge (Wi-Fi) the assistant starts a stdio command that finds the phone by name, so the settings
 * hold no address and survive a new one from the router. Otherwise they hold the URL.
 */
enum class AssistantClient(@StringRes val titleId: Int, @StringRes val whereId: Int) {

	CLAUDE_CODE(R.string.client_claude_code, R.string.client_claude_code_where) {
		override fun httpConfig(url: String, token: String) =
			"claude mcp add --transport http osmand $url --header \"Authorization: Bearer $token\""

		override fun bridgeConfig(token: String) =
			"claude mcp add osmand -- python3 ${ConnectorSettings.BRIDGE_PATH} --token $token"
	},
	CODEX(R.string.client_codex, R.string.client_codex_where) {
		override fun httpConfig(url: String, token: String) = """
			[mcp_servers.osmand]
			url = "$url"

			[mcp_servers.osmand.http_headers]
			Authorization = "Bearer $token"
			""".trimIndent()

		override fun bridgeConfig(token: String) = """
			[mcp_servers.osmand]
			command = "sh"
			args = ["-c", "${bridgeCommand(token)}"]
			""".trimIndent()
	},
	CURSOR(R.string.client_cursor, R.string.client_cursor_where) {
		override fun httpConfig(url: String, token: String) = """
			{
			  "mcpServers": {
			    "osmand": {
			      "url": "$url",
			      "headers": {
			        "Authorization":
			          "Bearer $token"
			      }
			    }
			  }
			}
			""".trimIndent()

		override fun bridgeConfig(token: String) = """
			{
			  "mcpServers": {
			    "osmand": {
			      "command": "sh",
			      "args": ["-c",
			        "${bridgeCommand(token)}"]
			    }
			  }
			}
			""".trimIndent()
	},
	VS_CODE(R.string.client_vs_code, R.string.client_vs_code_where) {
		override fun httpConfig(url: String, token: String) = """
			{
			  "servers": {
			    "osmand": {
			      "type": "http",
			      "url": "$url",
			      "headers": {
			        "Authorization":
			          "Bearer $token"
			      }
			    }
			  }
			}
			""".trimIndent()

		override fun bridgeConfig(token: String) = """
			{
			  "servers": {
			    "osmand": {
			      "type": "stdio",
			      "command": "sh",
			      "args": ["-c",
			        "${bridgeCommand(token)}"]
			    }
			  }
			}
			""".trimIndent()
	},
	OTHER(R.string.client_other, R.string.client_other_where) {
		override fun httpConfig(url: String, token: String) = """
			URL: $url
			Transport: Streamable HTTP
			Header: Authorization: Bearer $token

			Apps that only start local commands (e.g. Claude Desktop):
			npx mcp-remote $url --header "Authorization: Bearer $token"
			""".trimIndent()

		override fun bridgeConfig(token: String) = """
			Command (stdio):
			${bridgeCommand(token)}

			It finds the phone on this Wi-Fi by name (dns-sd on macOS, avahi-browse on Linux).
			""".trimIndent()
	};

	// sh -c expands ~ in the bridge path; the clients start the command without a shell
	protected fun bridgeCommand(token: String) = "python3 ${ConnectorSettings.BRIDGE_PATH} --token $token"

	protected abstract fun httpConfig(url: String, token: String): String

	protected abstract fun bridgeConfig(token: String): String

	fun config(bridge: Boolean, url: String, token: String) =
		if (bridge) bridgeConfig(token) else httpConfig(url, token)
}
