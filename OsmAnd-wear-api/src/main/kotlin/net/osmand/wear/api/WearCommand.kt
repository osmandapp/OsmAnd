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

	/** Moves a marker to history, which is what OsmAnd calls marking it visited. */
	@Serializable
	@SerialName("mark_marker_passed")
	data class MarkMarkerPassed(val id: String) : WearCommand

	/** Makes a marker the active one, the one the phone's widget and the arrow screen track. */
	@Serializable
	@SerialName("move_marker_to_top")
	data class MoveMarkerToTop(val id: String) : WearCommand

	/** Drops a marker at the phone's current position; the watch has no fix of its own yet. */
	@Serializable
	@SerialName("add_marker_here")
	data object AddMarkerHere : WearCommand

	/**
	 * Asks the phone to stand a renderer up at the watch's own size and start sending frames.
	 * The watch sends its dimensions because they differ across models, and its density because
	 * a renderer context is built around one.
	 */
	@Serializable
	@SerialName("start_map_stream")
	data class StartMapStream(val width: Int, val height: Int, val density: Float) : WearCommand

	/** Tears the renderer down. Sent when the map screen closes, not merely when it is hidden. */
	@Serializable
	@SerialName("stop_map_stream")
	data object StopMapStream : WearCommand

	/**
	 * Stops the frames without tearing the renderer down, for when the watch screen goes dark.
	 * Rebuilding it would cost the tile load again, which is seconds; holding it costs memory
	 * that is already spent while the screen is open.
	 */
	@Serializable
	@SerialName("pause_map_stream")
	data class PauseMapStream(val paused: Boolean) : WearCommand

	/**
	 * How much bigger the map should get, as a plain scale factor: 2 shows half as much ground.
	 * A factor rather than zoom levels because the watch shows its own preview of the gesture
	 * while it waits for a frame, and only a factor lets that preview be exact — what a level
	 * means in pixels is the renderer's business, and the phone converts it there.
	 */
	@Serializable
	@SerialName("zoom_map")
	data class ZoomMap(val factor: Float, val seq: Int = 0) : WearCommand

	/** Drag, in watch pixels; the phone turns them into a move of its own viewport. */
	@Serializable
	@SerialName("pan_map")
	data class PanMap(val dx: Float, val dy: Float, val seq: Int = 0) : WearCommand

	/**
	 * Asks the phone to work out a route to this point and hold it for a look, without setting
	 * off. The watch shows the phone's own drawing of it and decides from there.
	 */
	@Serializable
	@SerialName("preview_route")
	data class PreviewRoute(
		val latitude: Double,
		val longitude: Double,
		val name: String
	) : WearCommand

	/** Sets off along the route being previewed. */
	@Serializable
	@SerialName("start_navigation")
	data object StartNavigation : WearCommand

	/** Drops the previewed route without setting off. */
	@Serializable
	@SerialName("cancel_route_preview")
	data object CancelRoutePreview : WearCommand

	/** Picks the renderer that draws the watch's map: the legacy one, or OpenGL. */
	@Serializable
	@SerialName("set_map_renderer")
	data class SetMapRenderer(val legacy: Boolean) : WearCommand

	/** Puts the map back on the current position. */
	@Serializable
	@SerialName("recenter_map")
	data class RecenterMap(val seq: Int = 0) : WearCommand

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
