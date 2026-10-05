package net.osmand.aiconnector

import androidx.annotation.StringRes

/**
 * How to add the connector to an AI assistant. Each client gets where to put it and the exact text to copy.
 */
enum class AssistantClient(@StringRes val titleId: Int, @StringRes val whereId: Int) {

	CLAUDE_CODE(R.string.client_claude_code, R.string.client_claude_code_where) {
		override fun config(url: String, token: String) =
			"claude mcp add --transport http osmand $url --header \"Authorization: Bearer $token\""
	},
	CODEX(R.string.client_codex, R.string.client_codex_where) {
		override fun config(url: String, token: String) = """
			[mcp_servers.osmand]
			url = "$url"

			[mcp_servers.osmand.http_headers]
			Authorization = "Bearer $token"
			""".trimIndent()
	},
	CURSOR(R.string.client_cursor, R.string.client_cursor_where) {
		override fun config(url: String, token: String) = """
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
	},
	VS_CODE(R.string.client_vs_code, R.string.client_vs_code_where) {
		override fun config(url: String, token: String) = """
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
	},
	OTHER(R.string.client_other, R.string.client_other_where) {
		override fun config(url: String, token: String) = """
			URL: $url
			Transport: Streamable HTTP
			Header: Authorization: Bearer $token

			Apps that only start local commands (e.g. Claude Desktop):
			npx mcp-remote $url --header "Authorization: Bearer $token"
			""".trimIndent()
	};

	abstract fun config(url: String, token: String): String
}
