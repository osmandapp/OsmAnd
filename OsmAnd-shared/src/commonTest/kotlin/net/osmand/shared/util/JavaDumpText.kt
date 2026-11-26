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

/** Strings as the compat tests of OsmAnd-java write a list of them: [dumpHex] each, joined by commas, "-" for none. */
internal fun dumpHexList(list: List<String>): String = if (list.isEmpty()) "-" else list.joinToString(",") { dumpHex(it) }

internal fun dumpUnhexList(h: String): List<String> = if (h == "-") emptyList() else h.split(",").map { dumpUnhex(it) }

/** The FNV-1a hash of the chars of [s], `NameIndexCompatTest.fnv` of OsmAnd-java: a long answer in a dump. */
internal fun dumpFnv(s: String): Long {
	var h = 0xcbf29ce484222325uL.toLong()
	for (c in s) {
		h = h xor c.code.toLong()
		h *= 0x100000001b3L
	}
	return h
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
 * Reads a Java compatibility dump line by line. The caller closes it.
 *
 * To reproduce the iOS index and spatial comparisons, run the ordinary Java tests with ICU4J
 * 77.1 and its locale SPI on the JDK 17 boot class path. Set ICU_JARS to the two Maven Central
 * artifacts com.ibm.icu:icu4j and com.ibm.icu:icu4j-localespi, joined by the path separator.
 * From the Android project, with OSMAND_MAPS_DIR pointing to the regional OBF test maps:
 * ```sh
 * JAVA_TOOL_OPTIONS="-Xbootclasspath/a:$ICU_JARS -Djava.locale.providers=SPI,CLDR -Duser.language=en -Duser.country=US" \
 * OSMAND_SEARCH_COMPAT=1 OSMAND_TEST_HEAP=3g ./gradlew :OsmAnd-java:test --rerun \
 *   --tests '*NameIndexCompatTest' --tests '*SpatialSearchTokenCompatTest' \
 *   --tests '*SpatialTextSearchCompatTest' --tests '*SpatialTextSearchAPICompatTest'
 * ```
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
