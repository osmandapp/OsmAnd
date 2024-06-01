package net.osmand.shared.extensions

import net.osmand.shared.io.CommonFile
import java.io.File

fun File.cFile(): CommonFile = CommonFile(this.absolutePath)
