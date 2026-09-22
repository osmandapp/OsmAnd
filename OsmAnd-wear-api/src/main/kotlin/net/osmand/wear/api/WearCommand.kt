package net.osmand.wear.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Actions travelling watch -> phone.
 *
 * The watch never mutates state locally: it sends an intent, the phone applies it to the
 * real OsmAnd state and echoes the result back as a new [PhoneState]. That keeps a single
 * source of truth even when the user also touches the phone.
 */
@Serializable
sealed interface WearCommand {

	/** Sent on watch app start so the phone publishes a fresh snapshot immediately. */
	@Serializable
	@SerialName("request_state")
	data object RequestState : WearCommand

	@Serializable
	@SerialName("stop_navigation")
	data object StopNavigation : WearCommand

	@Serializable
	@SerialName("pause_navigation")
	data class PauseNavigation(val paused: Boolean) : WearCommand

	@Serializable
	@SerialName("toggle_recording")
	data class ToggleRecording(val active: Boolean) : WearCommand

	/**
	 * Mirrors the existing AIDL contract (see OsmAnd-api PreferenceParams): a preference is
	 * addressed by its registered id, scoped to a profile, with the value carried as a string.
	 * A null [appModeKey] means "the currently selected profile".
	 */
	@Serializable
	@SerialName("set_preference")
	data class SetPreference(
		val prefId: String,
		val appModeKey: String? = null,
		val value: String
	) : WearCommand
}
