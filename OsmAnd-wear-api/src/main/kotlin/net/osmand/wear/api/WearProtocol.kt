package net.osmand.wear.api

/**
 * Constants shared by both ends of the phone <-> watch link.
 *
 * Paths are Data Layer addresses and are part of the contract: renaming one breaks
 * every already installed counterpart, so treat them as frozen once released.
 */
object WearProtocol {

	/**
	 * Bumped whenever the meaning of an existing field changes or a field becomes required.
	 * v2 replaced NavigationState's single-turn fields with a list of upcoming manoeuvres.
	 * v3 split recording figures into value and unit and added the profile list.
	 * v4 made MarkerInfo.bearingDegrees a bearing from true north instead of an angle already
	 * turned for the phone's heading, so the watch can turn it by its own compass.
	 * Purely additive changes (a new nullable field) do not need a bump, because both ends
	 * decode with `ignoreUnknownKeys`.
	 */
	const val VERSION = 9

	/** Advertised by the phone app, looked up by the watch. */
	const val CAPABILITY_PHONE_APP = "osmand_phone_app"

	/** Advertised by the watch app, looked up by the phone. */
	const val CAPABILITY_WEAR_APP = "osmand_wear_app"

	/** DataClient item holding the latest [PhoneState]. */
	const val PATH_STATE = "/osmand/state"

	/** Key of the serialized [PhoneState] inside the data item. */
	const val KEY_STATE = "state"

	/** Asset key holding the arrow for the manoeuvre at [index] in the published snapshot. */
	fun turnIconKey(index: Int): String = "turn_$index"

	/** Asset key holding the glyph of the profile identified by [appModeKey]. */
	fun profileIconKey(appModeKey: String): String = "profile_$appModeKey"

	/** MessageClient path for [WearCommand]s travelling watch -> phone. */
	const val PATH_COMMAND = "/osmand/command"

	/**
	 * ChannelClient path for the map frame stream.
	 *
	 * A channel rather than a data item: frames are a stream, not state. Each one is written as
	 * a four byte big-endian length, a four byte big-endian request number, and then that many
	 * bytes of WebP, so the reader knows where a frame ends without waiting for the stream to
	 * close.
	 *
	 * The request number is the sequence of the last gesture the phone had applied when it drew
	 * the frame. The watch shows its own preview of a gesture until the phone catches up, and
	 * comparing this number against the last one it asked for is how it tells a frame that
	 * answers the gesture from one that was already in flight when the gesture was made. Without
	 * it the watch can only guess from timing, and a frame arriving between two quick drags takes
	 * the preview away before the map has moved.
	 */
	const val PATH_MAP_STREAM = "/osmand/map"

	/**
	 * True when a payload produced by [theirVersion] can be rendered by this build.
	 * Newer minor payloads stay readable; a major bump means the counterpart must update.
	 */
	fun isCompatible(theirVersion: Int): Boolean = theirVersion == VERSION
}
