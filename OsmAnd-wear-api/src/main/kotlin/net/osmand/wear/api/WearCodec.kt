package net.osmand.wear.api

import kotlinx.serialization.json.Json

/**
 * Single place where protocol payloads become bytes.
 *
 * Decoding never throws: a payload from a counterpart of a different version must degrade
 * into "nothing to show" rather than crash the app, since the two APKs update independently.
 */
object WearCodec {

	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
		classDiscriminator = "type"
	}

	fun encodeState(state: PhoneState): ByteArray =
		json.encodeToString(PhoneState.serializer(), state).toByteArray()

	fun decodeState(bytes: ByteArray): PhoneState? = runCatching {
		json.decodeFromString(PhoneState.serializer(), String(bytes))
	}.getOrNull()?.takeIf { WearProtocol.isCompatible(it.protocolVersion) }

	fun encodeCommand(command: WearCommand): ByteArray =
		json.encodeToString(WearCommand.serializer(), command).toByteArray()

	fun decodeCommand(bytes: ByteArray): WearCommand? = runCatching {
		json.decodeFromString(WearCommand.serializer(), String(bytes))
	}.getOrNull()
}
