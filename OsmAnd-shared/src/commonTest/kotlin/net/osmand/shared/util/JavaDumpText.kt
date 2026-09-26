package net.osmand.shared.util

import net.osmand.shared.routing.testEnvironment
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

/**
 * Each char as four hex digits after an 'x', the way the compat tests of OsmAnd-java write a string
 * into the dumps the tests here compare the copy to.
 */
internal fun dumpHex(s: String): String {
	val sb = StringBuilder(s.length * 4 + 1).append('x')
	for (c in s) {
		sb.append(c.code.toString(16).padStart(4, '0'))
	}
	return sb.toString()
}

internal fun dumpUnhex(h: String): String {
	val sb = StringBuilder((h.length - 1) / 4)
	var i = 1
	while (i < h.length) {
		sb.append(h.substring(i, i + 4).toInt(16).toChar())
		i += 4
	}
	return sb.toString()
}

/** java's `Long.toHexString(Double.doubleToRawLongBits(v))`. */
internal fun dumpBits(v: Double): String = v.toRawBits().toULong().toString(16)

/**
 * The lines of the dump a compat test of OsmAnd-java writes into its build directory, or where the
 * variable [env] points; null, after saying so, when it is not there.
 */
internal fun readJavaDump(test: String, env: String, file: String, compatTest: String): List<String>? {
	val path = javaDumpPath(test, env, file, compatTest) ?: return null
	return FileSystem.SYSTEM.read(path) { readUtf8() }.split("\n").filter { it.isNotEmpty() }
}

/**
 * [readJavaDump], line by line: for a dump too large to hold as one string. The caller closes it.
 */
internal fun openJavaDump(test: String, env: String, file: String, compatTest: String): BufferedSource? {
	val path = javaDumpPath(test, env, file, compatTest) ?: return null
	return FileSystem.SYSTEM.source(path).buffer()
}

private fun javaDumpPath(test: String, env: String, file: String, compatTest: String): Path? {
	val path = (testEnvironment(env) ?: "../OsmAnd-java/build/$file").toPath()
	if (!FileSystem.SYSTEM.exists(path)) {
		println("$test: no java dump at $path, run $compatTest in OsmAnd-java first")
		return null
	}
	return path
}
