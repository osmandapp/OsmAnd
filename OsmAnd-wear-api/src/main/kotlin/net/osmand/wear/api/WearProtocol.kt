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
	 * Purely additive changes (a new nullable field) do not need a bump, because both ends
	 * decode with `ignoreUnknownKeys`.
	 */
	const val VERSION = 2

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

	/** MessageClient path for [WearCommand]s travelling watch -> phone. */
	const val PATH_COMMAND = "/osmand/command"

	/**
	 * True when a payload produced by [theirVersion] can be rendered by this build.
	 * Newer minor payloads stay readable; a major bump means the counterpart must update.
	 */
	fun isCompatible(theirVersion: Int): Boolean = theirVersion == VERSION
}
