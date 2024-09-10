package net.osmand.shared.extensions

import net.osmand.shared.io.KFile
import java.io.File

fun File.kFile(): KFile = KFile(this.absolutePath)
fun KFile.jFile(): File = File(this.absolutePath())
