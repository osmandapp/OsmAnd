package net.osmand.shared.extensions

import net.osmand.shared.io.KFile
import java.io.File

fun File.kFile(): KFile = KFile(this.path)
fun KFile.jFile(): File = File(if (this.isPathEmpty()) "" else this.path())
