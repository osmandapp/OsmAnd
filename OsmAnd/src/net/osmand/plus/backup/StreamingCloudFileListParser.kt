package net.osmand.plus.backup

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.MalformedJsonException
import net.osmand.plus.utils.AndroidUtils
import net.osmand.util.Algorithms
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.EOFException
import java.io.FilterReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.ArrayList

/**
 * Streaming parser for the prepare/checking `/userdata/list-files` response.
 *
 * It retains at most one temporary file-entry JSONObject and skips `uniqueFiles`
 * and unknown root fields without materializing them.
 */
object StreamingCloudFileListParser {
	private const val JSON_ERROR_MESSAGE = "Download file list error: json parsing"
	private const val EMPTY_ERROR_MESSAGE = "Download file list error: empty response"

	@JvmStatic
	@Throws(IOException::class)
	fun parse(inputStream: InputStream, context: Context): CloudFileListParseResult {
		val trackingReader = TrackingReader(
			InputStreamReader(inputStream, StandardCharsets.UTF_8)
		)
		val reader = JsonReader(trackingReader)
		reader.isLenient = true
		try {
			val firstToken = reader.peek()
			if (firstToken == JsonToken.END_DOCUMENT) {
				return if (trackingReader.hasContentAfterLineRemoval()) {
					jsonError(null, emptyList())
				} else {
					emptyResponse()
				}
			}
			if (firstToken != JsonToken.BEGIN_OBJECT) {
				reader.skipValue()
				requireEndOfDocument(reader)
				return jsonError(null, emptyList())
			}
			val rootState = readRoot(reader)
			requireEndOfDocument(reader)
			return evaluateRoot(rootState, context)
		} catch (e: InputSourceIOException) {
			throw e.sourceException
		} catch (_: EOFException) {
			return if (trackingReader.hasContentAfterLineRemoval()) {
				jsonError(null, emptyList())
			} else {
				emptyResponse()
			}
		} catch (_: MalformedJsonException) {
			// Legacy JSONObject construction fails before exposing any allFiles records.
			return jsonError(null, emptyList())
		} catch (_: JSONException) {
			// Legacy JSONObject construction fails before exposing any allFiles records.
			return jsonError(null, emptyList())
		} catch (_: IllegalStateException) {
			// Legacy JSONObject construction fails before exposing any allFiles records.
			return jsonError(null, emptyList())
		} catch (e: IOException) {
			throw e
		} finally {
			try {
				reader.close()
			} catch (_: Exception) {
				// Matches the legacy body reader, which ignores close failures.
			}
		}
	}

	private fun readRoot(reader: JsonReader): RootState {
		val rootState = RootState()
		reader.beginObject()
		while (reader.hasNext()) {
			when (reader.nextName()) {
				"totalZipSize" -> rootState.totalZipSize.set(readJsonValue(reader))
				"totalFiles" -> rootState.totalFiles.set(readJsonValue(reader))
				"totalFileVersions" -> rootState.totalFileVersions.set(readJsonValue(reader))
				"maximumAccountSize" -> rootState.maximumAccountSize.set(readJsonValue(reader))
				"allFiles" -> {
					if (reader.peek() == JsonToken.BEGIN_ARRAY) {
						rootState.allFilesOutcome = readAllFiles(reader)
					} else {
						rootState.allFilesOutcome = null
						reader.skipValue()
					}
				}
				"uniqueFiles" -> reader.skipValue()
				else -> reader.skipValue()
			}
		}
		reader.endObject()
		return rootState
	}

	private fun readAllFiles(reader: JsonReader): AllFilesOutcome {
		val outcome = AllFilesOutcome()
		reader.beginArray()
		while (reader.hasNext()) {
			if (outcome.failureCategory != null) {
				reader.skipValue()
			} else if (reader.peek() != JsonToken.BEGIN_OBJECT) {
				reader.skipValue()
				outcome.failWithJson()
			} else {
				val fileJson = readJsonObject(reader)
				try {
					outcome.remoteFiles.add(RemoteFile(fileJson))
				} catch (_: JSONException) {
					outcome.failWithJson()
				} catch (e: RuntimeException) {
					outcome.failWithRuntime(e)
				}
			}
		}
		reader.endArray()
		return outcome
	}

	private fun evaluateRoot(
		rootState: RootState,
		context: Context
	): CloudFileListParseResult {
		val totalZipSize: String
		val totalFiles: String
		val totalFileVersions: String
		var maximumAccountSize: Long? = null

		val outcome: AllFilesOutcome
		try {
			totalZipSize = rootState.totalZipSize.getRequiredString()
			totalFiles = rootState.totalFiles.getRequiredString()
			totalFileVersions = rootState.totalFileVersions.getRequiredString()
			maximumAccountSize = Algorithms.parseLongSilently(
				rootState.maximumAccountSize.getRequiredString(),
				0
			)
			outcome = rootState.allFilesOutcome
				?: throw JSONException("Value allFiles is not a JSONArray")
		} catch (_: JSONException) {
			return jsonError(maximumAccountSize, emptyList())
		}

		if (outcome.failureCategory == AllFilesFailure.JSON) {
			return jsonError(
				maximumAccountSize, outcome.remoteFiles
			)
		}
		if (outcome.failureCategory == AllFilesFailure.RUNTIME) {
			return runtimeError(
				outcome.runtimeException,
				maximumAccountSize, outcome.remoteFiles
			)
		}

		try {
			val message = "Total files: " + totalFiles + " " +
					"Total zip size: " + AndroidUtils.formatSize(
				context,
				java.lang.Long.parseLong(totalZipSize)
			) + " " +
					"Total file versions: " + totalFileVersions
			return CloudFileListParseResult(
				BackupHelper.STATUS_SUCCESS, message,
				maximumAccountSize, outcome.remoteFiles
			)
		} catch (e: RuntimeException) {
			return runtimeError(
				e,
				maximumAccountSize, outcome.remoteFiles
			)
		}
	}

	private fun readJsonObject(reader: JsonReader): JSONObject {
		val jsonObject = JSONObject()
		reader.beginObject()
		while (reader.hasNext()) {
			val name = reader.nextName()
			jsonObject.put(name, readJsonValue(reader))
		}
		reader.endObject()
		return jsonObject
	}

	private fun readJsonArray(reader: JsonReader): JSONArray {
		val array = JSONArray()
		reader.beginArray()
		while (reader.hasNext()) {
			array.put(readJsonValue(reader))
		}
		reader.endArray()
		return array
	}

	private fun readJsonValue(reader: JsonReader): Any {
		return when (reader.peek()) {
			JsonToken.BEGIN_OBJECT -> readJsonObject(reader)
			JsonToken.BEGIN_ARRAY -> readJsonArray(reader)
			JsonToken.STRING -> reader.nextString()
			JsonToken.NUMBER -> JSONTokener(reader.nextString()).nextValue()
			JsonToken.BOOLEAN -> reader.nextBoolean()
			JsonToken.NULL -> {
				reader.nextNull()
				JSONObject.NULL
			}
			else -> throw JSONException("Unexpected JSON token " + reader.peek())
		}
	}

	private fun requireEndOfDocument(reader: JsonReader) {
		if (reader.peek() != JsonToken.END_DOCUMENT) {
			throw MalformedJsonException("Unexpected trailing JSON value")
		}
	}

	private fun emptyResponse(): CloudFileListParseResult {
		return CloudFileListParseResult(
			BackupHelper.STATUS_EMPTY_RESPONSE_ERROR, EMPTY_ERROR_MESSAGE,
			null, emptyList()
		)
	}

	private fun jsonError(
		maximumAccountSize: Long?,
		remoteFiles: List<RemoteFile>
	): CloudFileListParseResult {
		return CloudFileListParseResult(
			BackupHelper.STATUS_PARSE_JSON_ERROR, JSON_ERROR_MESSAGE,
			maximumAccountSize, remoteFiles
		)
	}

	private fun runtimeError(
		exception: RuntimeException?,
		maximumAccountSize: Long?,
		remoteFiles: List<RemoteFile>
	): CloudFileListParseResult {
		return CloudFileListParseResult(
			null, exception?.message,
			maximumAccountSize, remoteFiles, exception
		)
	}

	private class RootState {
		val totalZipSize = RootValue("totalZipSize")
		val totalFiles = RootValue("totalFiles")
		val totalFileVersions = RootValue("totalFileVersions")
		val maximumAccountSize = RootValue("maximumAccountSize")
		var allFilesOutcome: AllFilesOutcome? = null
	}

	private class RootValue(private val name: String) {
		private var present = false
		private var value: Any? = null

		fun set(value: Any) {
			present = true
			this.value = value
		}

		fun getRequiredString(): String {
			if (!present) {
				throw JSONException("No value for $name")
			}
			return java.lang.String.valueOf(value)
		}
	}

	private enum class AllFilesFailure {
		JSON,
		RUNTIME
	}

	private class AllFilesOutcome {
		val remoteFiles: MutableList<RemoteFile> = ArrayList()
		var failureCategory: AllFilesFailure? = null
			private set
		var runtimeException: RuntimeException? = null
			private set

		fun failWithJson() {
			failureCategory = AllFilesFailure.JSON
		}

		fun failWithRuntime(exception: RuntimeException) {
			failureCategory = AllFilesFailure.RUNTIME
			runtimeException = exception
		}
	}

	/** Tracks whether the legacy line reader would have produced a non-empty String. */
	private class TrackingReader(reader: Reader) : FilterReader(reader) {
		private var nonLineBreakContent = false
		private var logicalLineBreakCount = 0
		private var previousWasCarriageReturn = false

		@Throws(IOException::class)
		override fun read(): Int {
			try {
				val value = super.read()
				track(value)
				return value
			} catch (e: IOException) {
				throw InputSourceIOException(e)
			}
		}

		@Throws(IOException::class)
		override fun read(buffer: CharArray, offset: Int, length: Int): Int {
			try {
				val count = super.read(buffer, offset, length)
				for (index in 0 until count) {
					track(buffer[offset + index].code)
				}
				return count
			} catch (e: IOException) {
				throw InputSourceIOException(e)
			}
		}

		private fun track(value: Int) {
			if (value == '\r'.code) {
				logicalLineBreakCount++
				previousWasCarriageReturn = true
			} else if (value == '\n'.code) {
				if (!previousWasCarriageReturn) {
					logicalLineBreakCount++
				}
				previousWasCarriageReturn = false
			} else if (value >= 0) {
				nonLineBreakContent = true
				previousWasCarriageReturn = false
			}
		}

		fun hasContentAfterLineRemoval(): Boolean {
			return nonLineBreakContent || logicalLineBreakCount > 1
		}
	}

	private class InputSourceIOException(
		val sourceException: IOException
	) : IOException(sourceException)
}
