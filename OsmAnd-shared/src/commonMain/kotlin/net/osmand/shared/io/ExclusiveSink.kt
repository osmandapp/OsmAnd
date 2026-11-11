package net.osmand.shared.io

import okio.IOException
import okio.Sink

/** Opens a new file atomically; never follows or overwrites an existing directory entry. */
@Throws(IOException::class)
internal expect fun KFile.exclusiveSink(): Sink
