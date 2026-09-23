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
	@SerialName("start_recording")
	data object StartRecording : WearCommand

	@Serializable
	@SerialName("pause_recording")
	data object PauseRecording : WearCommand

	@Serializable
	@SerialName("resume_recording")
	data object ResumeRecording : WearCommand

	/** Saves the track and ends the session. */
	@Serializable
	@SerialName("finish_recording")
	data object FinishRecording : WearCommand

	/** Writes what has been recorded so far and keeps going. */
	@Serializable
	@SerialName("save_and_continue")
	data object SaveAndContinueRecording : WearCommand

	/** Switches the active OsmAnd profile, which is what the recording will be attributed to. */
	@Serializable
	@SerialName("select_profile")
	data class SelectProfile(val appModeKey: String) : WearCommand

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
