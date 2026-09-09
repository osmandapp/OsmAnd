package net.osmand.shared.util

import net.sf.junidecode.Junidecode
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Writes `KTransliterationTables.kt` by asking junidecode for every code unit of the basic plane.
 *
 * Not a test: it is here because this is the one source set that has junidecode on its classpath,
 * and because a generated file with no way to regenerate it is a file nobody can touch.
 * `KTransliterationTablesTest` is what checks the result, and it runs normally.
 *
 * To regenerate, from the repository root:
 * ```
 * ./gradlew :OsmAnd-shared:jvmTest --tests "*KTransliterationTablesGenerator" -i
 * ```
 * with the `@Ignore` below removed, then put it back.
 */
@Ignore
class KTransliterationTablesGenerator {

	/** Defined here rather than read from the file this rewrites, which may not exist yet. */
	private val separator = '\u00B7'

	@Test
	fun generate() {
		val blocks = arrayOfNulls<Array<String>>(256)
		for (high in 0..0xff) {
			val entries = Array(256) { low -> Junidecode.unidecode(((high shl 8) or low).toChar().toString()) }
			blocks[high] = if (entries.any { it.isNotEmpty() }) entries else null
		}

		val out = StringBuilder()
		out.append("package net.osmand.shared.util\n\n")
		out.append("/**\n")
		out.append(" * Junidecode's tables, generated from com.moparisthebest:junidecode:0.1.1 by asking it for every\n")
		out.append(" * code unit of the basic plane, so that every platform romanises a name the same way.\n")
		out.append(" *\n")
		out.append(" * One string per 256 code unit block, entries separated by a middle dot, which no mapping\n")
		out.append(" * contains: their alphabet is U+000A to U+007E. A block whose mappings are all empty is null,\n")
		out.append(" * and its characters romanise to nothing, which is what junidecode does with them.\n")
		out.append(" *\n")
		out.append(" * Generated, do not edit: run KTransliterationTablesGenerator to rebuild it.\n")
		out.append(" */\n")
		out.append("internal object KTransliterationTables {\n\n")
		out.append("\tconst val SEPARATOR = '$separator'\n\n")
		for (high in 0..0xff) {
			val entries = blocks[high] ?: continue
			out.append("\tprivate const val B${hex(high)} = \"")
			out.append(escape(entries.joinToString(separator.toString())))
			out.append("\"\n")
		}
		out.append("\n\t/** The joined block for a high byte, or null when nothing in it romanises. */\n")
		out.append("\tfun block(high: Int): String? = when (high) {\n")
		for (high in 0..0xff) {
			if (blocks[high] != null) {
				out.append("\t\t0x${hex(high)} -> B${hex(high)}\n")
			}
		}
		out.append("\t\telse -> null\n\t}\n}\n")

		val target = File("src/commonMain/kotlin/net/osmand/shared/util/KTransliterationTables.kt")
		target.writeText(out.toString())
		println("### wrote ${target.absolutePath}, ${blocks.count { it != null }} blocks, ${target.length()} bytes")
	}

	private fun hex(value: Int): String = value.toString(16).uppercase().padStart(2, '0')

	private fun escape(text: String): String {
		val b = StringBuilder(text.length + 64)
		for (c in text) {
			when {
				c == '"' -> b.append("\\\"")
				c == '\\' -> b.append("\\\\")
				c == '$' -> b.append("\\$")
				// the first code points romanise to themselves, control characters included
				c.code < 0x20 || c.code == 0x7f -> b.append("\\u%04X".format(c.code))
				else -> b.append(c)
			}
		}
		return b.toString()
	}
}
