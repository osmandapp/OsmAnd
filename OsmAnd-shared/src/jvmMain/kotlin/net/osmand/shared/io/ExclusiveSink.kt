package net.osmand.shared.io

import okio.Sink
import okio.sink
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE

internal actual fun KFile.exclusiveSink(): Sink =
	Files.newOutputStream(path.toNioPath(), CREATE_NEW, WRITE).sink()
