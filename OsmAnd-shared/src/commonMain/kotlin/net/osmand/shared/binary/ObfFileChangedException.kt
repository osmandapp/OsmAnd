package net.osmand.shared.binary

import okio.IOException

/** The file at a path is no longer the one a reader walked: an update has written another file there. */
internal class ObfFileChangedException(message: String) : IOException(message)
