package net.osmand.shared.io

import okio.FileSystem
import okio.SYSTEM
import okio.Sink

// Okio's Apple/POSIX implementation uses fopen("wx"), unlike its JVM implementation.
internal actual fun KFile.exclusiveSink(): Sink = FileSystem.SYSTEM.sink(path, mustCreate = true)
