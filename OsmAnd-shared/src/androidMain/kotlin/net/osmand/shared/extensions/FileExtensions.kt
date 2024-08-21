package net.osmand.shared.extensions

import net.osmand.shared.io.KFile
import java.io.File

fun File.cFile(): KFile = KFile(this.absolutePath)
