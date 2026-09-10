package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.Buffer
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// iOS-only behaviour of the pure-Kotlin parser: encoding auto-detection (QXmlStreamReader did it,
// so losing it was a regression once already) and the streaming window, whose refills must be
// invisible no matter where a token happens to land. The cross-platform event contract is
// asserted in commonTest/XmlPullParserContractTest.
class XmlPullParserIosTest {

	private companion object {
		// Mirrors CHAR_BUFFER_SIZE / BYTE_CHUNK_SIZE in XmlPullParser: tokens are built around
		// this boundary on purpose.
		const val WINDOW = 8 * 1024
	}

	private fun bytes(vararg parts: Any): ByteArray {
		val out = ArrayList<Byte>()
		for (part in parts) {
			when (part) {
				is String -> for (b in part.encodeToByteArray()) out.add(b)
				is Int -> out.add(part.toByte())
				else -> throw IllegalArgumentException("unsupported part: $part")
			}
		}
		return out.toByteArray()
	}

	private fun utf16(text: String, bigEndian: Boolean, withBom: Boolean): ByteArray {
		val out = ArrayList<Byte>()
		if (withBom) {
			if (bigEndian) {
				out.add(0xFE.toByte()); out.add(0xFF.toByte())
			} else {
				out.add(0xFF.toByte()); out.add(0xFE.toByte())
			}
		}
		for (c in text) {
			val code = c.code
			if (bigEndian) {
				out.add((code shr 8).toByte()); out.add((code and 0xFF).toByte())
			} else {
				out.add((code and 0xFF).toByte()); out.add((code shr 8).toByte())
			}
		}
		return out.toByteArray()
	}

	private fun parser(input: ByteArray): XmlPullParser {
		val parser = XmlPullParser()
		parser.setInput(Buffer().write(input), null)
		return parser
	}

	private fun textOf(input: ByteArray): String {
		val parser = parser(input)
		val text = StringBuilder()
		while (true) {
			val type = parser.next()
			if (type == XmlPullParser.TEXT) text.append(parser.getText())
			if (type == XmlPullParser.END_DOCUMENT) break
		}
		parser.close()
		return text.toString()
	}

	private fun textOf(xml: String): String = textOf(xml.encodeToByteArray())

	private fun events(input: ByteArray): List<String> {
		val parser = parser(input)
		val out = mutableListOf<String>()
		while (true) {
			val type = parser.next()
			out.add(
				when (type) {
					XmlPullParser.START_TAG -> "START ${parser.getName()}"
					XmlPullParser.END_TAG -> "END ${parser.getName()}"
					XmlPullParser.TEXT -> "TEXT ${parser.getText()}"
					XmlPullParser.END_DOCUMENT -> "EOF"
					else -> "OTHER $type"
				}
			)
			if (type == XmlPullParser.END_DOCUMENT) break
		}
		parser.close()
		return out
	}

	private fun encodingOf(input: ByteArray): String? {
		val parser = parser(input)
		val encoding = parser.getInputEncoding()
		parser.close()
		return encoding
	}

	// --- encoding auto-detection ---------------------------------------------------------

	@Test
	fun detectsUtf8WithoutDeclaration() {
		val input = "<a>Привет 日本語 𝄞</a>".encodeToByteArray()
		assertEquals("Привет 日本語 𝄞", textOf(input))
		assertEquals("UTF-8", encodingOf(input))
	}

	@Test
	fun detectsUtf8ByteOrderMark() {
		val input = bytes(0xEF, 0xBB, 0xBF, "<a>Привет</a>")
		assertEquals("Привет", textOf(input))
		assertEquals("UTF-8", encodingOf(input))
		assertEquals(listOf("START a", "TEXT Привет", "END a", "EOF"), events(input))
	}

	@Test
	fun detectsUtf16ByteOrderMarks() {
		val le = utf16("<a t=\"Привет\">Мир и труд</a>", bigEndian = false, withBom = true)
		val be = utf16("<a t=\"Привет\">Мир и труд</a>", bigEndian = true, withBom = true)
		assertEquals("Мир и труд", textOf(le))
		assertEquals("UTF-16LE", encodingOf(le))
		assertEquals("Мир и труд", textOf(be))
		assertEquals("UTF-16BE", encodingOf(be))
	}

	// A declaration that names the endianness wins; a plain "UTF-16" without a BOM is resolved
	// from the first two bytes, so a big-endian document is not read as little-endian mojibake.
	@Test
	fun detectsDeclaredUtf16WithoutByteOrderMark() {
		for (label in listOf("UTF-16", "utf16", "unicode")) {
			val declared = "<?xml version=\"1.0\" encoding=\"$label\"?><a>Привет</a>"
			assertEquals(
				"Привет",
				textOf(utf16(declared, bigEndian = false, withBom = false)),
				"little-endian document declared as $label"
			)
			assertEquals(
				"Привет",
				textOf(utf16(declared, bigEndian = true, withBom = false)),
				"big-endian document declared as $label"
			)
		}
		assertEquals(
			"Привет",
			textOf(utf16("<?xml version=\"1.0\" encoding=\"UTF-16BE\"?><a>Привет</a>", bigEndian = true, withBom = false))
		)
		assertEquals(
			"Привет",
			textOf(utf16("<?xml version=\"1.0\" encoding=\"UTF-16LE\"?><a>Привет</a>", bigEndian = false, withBom = false))
		)
	}

	@Test
	fun detectsDeclaredWindows1251() {
		// "Привет" / "Мир и труд" as raw cp1251 bytes.
		val input = bytes(
			"<?xml version=\"1.0\" encoding=\"windows-1251\"?><a t=\"",
			0xCF, 0xF0, 0xE8, 0xE2, 0xE5, 0xF2,
			"\">",
			0xCC, 0xE8, 0xF0, 0x20, 0xE8, 0x20, 0xF2, 0xF0, 0xF3, 0xE4,
			"</a>"
		)
		assertEquals("Мир и труд", textOf(input))
		assertEquals("windows-1251", encodingOf(input))
		val parser = parser(input)
		parser.next()
		assertEquals("Привет", parser.getAttributeValue("", "t"))
		parser.close()
	}

	@Test
	fun detectsDeclaredWindows1252() {
		val input = bytes(
			"<?xml version=\"1.0\" encoding=\"windows-1252\"?><a>",
			0x80, "uro Caf", 0xE9, // "€uro Café"
			"</a>"
		)
		assertEquals("€uro Café", textOf(input))
		assertEquals("windows-1252", encodingOf(input))
	}

	@Test
	fun detectsDeclaredLatin1() {
		val input = bytes(
			"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a>",
			"Caf", 0xE9, " ", 0xDC, "ber", // "Café Über"
			"</a>"
		)
		assertEquals("Café Über", textOf(input))
		assertEquals("iso-8859-1", encodingOf(input))
	}

	@Test
	fun fallsBackToUtf8ForUnknownDeclaredEncoding() {
		val input = "<?xml version=\"1.0\" encoding=\"x-made-up\"?><a>Привет</a>".encodeToByteArray()
		assertEquals("Привет", textOf(input))
	}

	@Test
	fun keepsByteOrderMarkOutOfTheFirstTextEvent() {
		// UTF-16 stream whose BOM survives decoding as U+FEFF.
		val input = utf16("﻿<a>text</a>", bigEndian = false, withBom = false)
		assertEquals(listOf("START a", "TEXT text", "END a", "EOF"), events(input))
	}

	// --- streaming window ----------------------------------------------------------------

	@Test
	fun readsTextLongerThanTheWindow() {
		val long = "привет мир ".repeat(WINDOW / 4)
		assertEquals(long, textOf("<a><b>$long</b></a>"))
	}

	@Test
	fun readsAttributeValueLongerThanTheWindow() {
		val long = "значение-".repeat(WINDOW / 4)
		val parser = parser("<a v=\"$long\" w=\"tail\"/>".encodeToByteArray())
		parser.next()
		assertEquals(long, parser.getAttributeValue("", "v"))
		assertEquals("tail", parser.getAttributeValue("", "w"))
		parser.close()
	}

	@Test
	fun readsMultibyteCharactersSplitAcrossChunks() {
		for (pad in WINDOW - 6..WINDOW + 2) {
			val text = "x".repeat(pad) + "Ж日𝄞"
			assertEquals(text, textOf("<a>$text</a>"), "multibyte character at offset $pad")
		}
	}

	@Test
	fun readsEntitiesSplitAcrossChunks() {
		for (pad in WINDOW - 8..WINDOW + 2) {
			val prefix = "x".repeat(pad)
			assertEquals("$prefix&<A", textOf("<a>$prefix&amp;&lt;&#65;</a>"), "entity at offset $pad")
		}
	}

	@Test
	fun readsCdataAndMarkupSplitAcrossChunks() {
		for (pad in WINDOW - 12..WINDOW + 2) {
			val prefix = "x".repeat(pad)
			assertEquals(
				"$prefix<raw> & ]] tail",
				textOf("<a>$prefix<![CDATA[<raw> & ]] tail]]></a>"),
				"cdata end marker at offset $pad"
			)
			assertEquals(
				"${prefix}tail",
				textOf("<a>$prefix<!-- comment -->tail</a>"),
				"comment at offset $pad"
			)
		}
	}

	@Test
	fun readsTagsAndAttributesSplitAcrossChunks() {
		for (pad in WINDOW - 20..WINDOW + 2) {
			val prefix = "x".repeat(pad)
			val parser = parser("<a>$prefix<point lat=\"50.1\" lon=\"14.4\"/></a>".encodeToByteArray())
			assertEquals(XmlPullParser.START_TAG, parser.next())
			assertEquals(XmlPullParser.TEXT, parser.next())
			assertEquals(XmlPullParser.START_TAG, parser.next())
			assertEquals("point", parser.getName(), "tag at offset $pad")
			assertEquals("50.1", parser.getAttributeValue("", "lat"), "attribute at offset $pad")
			assertEquals("14.4", parser.getAttributeValue("", "lon"), "attribute at offset $pad")
			assertEquals(XmlPullParser.END_TAG, parser.next())
			assertEquals(XmlPullParser.END_TAG, parser.next())
			assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
			parser.close()
		}
	}

	@Test
	fun readsCommentLongerThanTheWindow() {
		val comment = "-c-".repeat(WINDOW)
		assertEquals("tail", textOf("<a><!--$comment-->tail</a>"))
	}

	// --- text and attribute normalisation --------------------------------------------------

	@Test
	fun normalisesLineEndings() {
		assertEquals("t1\nt2\nt3\nt4", textOf("<a>t1\r\nt2\rt3\nt4</a>"))
	}

	@Test
	fun normalisesWhitespaceInAttributeValues() {
		val parser = parser("<a v=\"line1\nline2\ttab\r\nlast\" w=\"&#10;&#9;\"/>".encodeToByteArray())
		parser.next()
		assertEquals("line1 line2 tab last", parser.getAttributeValue("", "v"))
		// Character references keep their literal value, unlike the raw characters.
		assertEquals("\n\t", parser.getAttributeValue("", "w"))
		parser.close()
	}

	@Test
	fun keepsUnknownEntitiesAsLiteralText() {
		assertEquals("&unknown; &; & &#xZZ;", textOf("<a>&unknown; &; & &#xZZ;</a>"))
	}

	@Test
	fun ignoresWhitespaceOutsideTheRootElement() {
		assertEquals(
			listOf("START a", "TEXT t", "END a", "EOF"),
			events("<?xml version=\"1.0\"?>\n\n<a>t</a>\n\n".encodeToByteArray())
		)
	}

	@Test
	fun skipsDoctypeWithInternalSubset() {
		assertEquals(
			"body",
			textOf("<!DOCTYPE a [ <!ENTITY x \"y\"> <!ELEMENT a (#PCDATA)> ]><a>body</a>")
		)
		assertEquals("body", textOf("<!DOCTYPE a><a>body</a>"))
		assertEquals("body", textOf("<!doctype a [ <!ELEMENT a (#PCDATA)> ]><a>body</a>"))
	}

	// A '>' inside a quoted literal or inside a comment of the internal subset must not end the
	// DOCTYPE early. kxml2 rejects entity declarations outright, so this cannot be asserted
	// against the JVM actual in commonTest.
	@Test
	fun skipsDoctypeWithAngleBracketsInQuotesAndComments() {
		assertEquals(
			"body",
			textOf("<!DOCTYPE a [ <!ENTITY x \"a > b\"> <!ENTITY y 'c > d'> ]><a>body</a>")
		)
		assertEquals(
			"body",
			textOf("<!DOCTYPE a [ <!-- > ]> --> <!ELEMENT a (#PCDATA)> ]><a>body</a>")
		)
		assertEquals(
			"body",
			textOf("<!DOCTYPE a SYSTEM \"http://example.org/a>b.dtd\"><a>body</a>")
		)
		assertEquals(
			"body",
			textOf("<!DOCTYPE a PUBLIC \"-//x//DTD a>b//EN\" \"a>b.dtd\" [ <!ENTITY z \"]>\"> ]><a>body</a>")
		)
	}

	@Test
	fun stopsAtEndOfInputInsideUnterminatedDoctype() {
		assertEquals(listOf("EOF"), events("<!DOCTYPE a [ <!ENTITY x \"unterminated".encodeToByteArray()))
	}

	// --- namespaces ------------------------------------------------------------------------

	@Test
	fun stripsPrefixesAndNamespaceDeclarationsByDefault() {
		val parser = parser(
			("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:osmand=\"https://osmand.net\" " +
				"version=\"1.1\"><osmand:color>red</osmand:color></gpx>").encodeToByteArray()
		)
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("gpx", parser.getName())
		assertEquals(1, parser.getAttributeCount())
		assertEquals("version", parser.getAttributeName(0))
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("color", parser.getName())
		assertEquals("osmand", parser.getPrefix())
		parser.close()
	}

	@Test
	fun keepsPrefixesWhenNamespaceProcessingIsOff() {
		val parser = XmlPullParser()
		parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false)
		parser.setInput(
			Buffer().write("<gx:Track xmlns:gx=\"http://www.google.com/kml/ext/2.2\"><gx:coord>1 2</gx:coord></gx:Track>".encodeToByteArray()),
			null
		)
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("gx:Track", parser.getName())
		assertEquals(1, parser.getAttributeCount())
		assertEquals("xmlns:gx", parser.getAttributeName(0))
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("gx:coord", parser.getName())
		assertEquals("1 2", parser.nextText())
		parser.close()
	}

	// --- malformed input --------------------------------------------------------------------

	@Test
	fun rejectsUnclosedElement() {
		assertFailsWith<XmlParserException> { textOf("<a><b>text</b>") }
		assertFailsWith<XmlParserException> { textOf("<a><b>text") }
	}

	@Test
	fun rejectsStrayClosingTag() {
		assertFailsWith<XmlParserException> { textOf("<a></a></b>") }
	}

	@Test
	fun rejectsMalformedMarkup() {
		assertFailsWith<XmlParserException> { textOf("<a><b>text</c></a>") }
		assertFailsWith<XmlParserException> { textOf("<a v=\"1\" w></a>") }
		assertFailsWith<XmlParserException> { textOf("<a v=1></a>") }
		assertFailsWith<XmlParserException> { textOf("<a v=\"1></a>") }
		assertFailsWith<XmlParserException> { textOf("<a><1b/></a>") }
		assertFailsWith<XmlParserException> { textOf("<a><b lat=\"1\"") }
	}

	@Test
	fun reportsEndDocumentForEmptyInput() {
		assertEquals(listOf("EOF"), events(ByteArray(0)))
	}

	// --- file input --------------------------------------------------------------------------

	@Test
	fun parsesFileAndReleasesItOnClose() {
		val file = KFile(NSTemporaryDirectory() + "xml-pull-parser-test.gpx")
		try {
			file.writeText("<gpx><wpt lat=\"50.1\" lon=\"14.4\"><name>Точка</name></wpt></gpx>")
			val parser = XmlPullParser()
			parser.setInput(file, "UTF-8")
			val names = mutableListOf<String>()
			while (true) {
				val type = parser.next()
				if (type == XmlPullParser.START_TAG) names.add(parser.getName() ?: "")
				if (type == XmlPullParser.TEXT) names.add(parser.getText() ?: "")
				if (type == XmlPullParser.END_DOCUMENT) break
			}
			assertEquals(listOf("gpx", "wpt", "name", "Точка"), names)
			parser.close()
			parser.close() // closing twice must stay harmless

			// A parser instance is reusable after close(), and after a parse that was abandoned
			// half way through.
			parser.setInput(file, "UTF-8")
			assertEquals(XmlPullParser.START_TAG, parser.next())
			parser.setInput(file, "UTF-8")
			assertEquals(XmlPullParser.START_TAG, parser.next())
			assertEquals("gpx", parser.getName())
			parser.close()
			assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
		} finally {
			assertTrue(file.delete())
		}
	}
}
