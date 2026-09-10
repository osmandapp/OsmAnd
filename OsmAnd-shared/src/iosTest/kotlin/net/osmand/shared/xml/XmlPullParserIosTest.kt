package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.Buffer
import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Exercised against the libxml2-backed actual. Everything here is contract behaviour that
// GpxUtilities.loadGpxFile and ImportGpx.parseKmlStreaming depend on.
private const val NS_FEATURE = "http://xmlpull.org/v1/doc/features.html#process-namespaces"

class XmlPullParserIosTest {

	private var counter = 0

	private fun tempFile(bytes: ByteArray): KFile {
		val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "xpp-test-${counter++}.xml"
		FileSystem.SYSTEM.write(path) { write(bytes) }
		return KFile(path.toString())
	}

	private fun parserFor(xml: String, namespaces: Boolean = true): XmlPullParser =
		XmlPullParser().apply {
			setFeature(NS_FEATURE, namespaces)
			setInput(tempFile(xml.encodeToByteArray()), "UTF-8")
		}

	private fun parserForBytes(bytes: ByteArray): XmlPullParser =
		XmlPullParser().apply { setInput(tempFile(bytes), "UTF-8") }

	/** Drains the parser into "TYPE:name" / "TEXT:value" strings. */
	private fun drain(parser: XmlPullParser): List<String> {
		val events = mutableListOf<String>()
		while (true) {
			when (val type = parser.next()) {
				XmlPullParser.START_TAG -> events.add("START:${parser.getName()}")
				XmlPullParser.END_TAG -> events.add("END:${parser.getName()}")
				XmlPullParser.TEXT -> events.add("TEXT:${parser.getText()}")
				XmlPullParser.END_DOCUMENT -> return events
				else -> events.add("OTHER:$type")
			}
		}
	}

	@Test
	fun walksElementsTextAndEndTags() {
		val parser = parserFor("<gpx><trk><name>Hike</name></trk></gpx>")
		assertEquals(
			listOf("START:gpx", "START:trk", "START:name", "TEXT:Hike", "END:name", "END:trk", "END:gpx"),
			drain(parser)
		)
		parser.close()
	}

	@Test
	fun reportsSelfClosingElementAsStartAndSyntheticEnd() {
		val parser = parserFor("<trkseg><trkpt lat=\"1\" lon=\"2\"/></trkseg>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("trkseg", parser.getName())
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("trkpt", parser.getName())
		assertTrue(parser.isEmptyElementTag())
		assertEquals("1", parser.getAttributeValue(null, "lat"))
		assertEquals("2", parser.getAttributeValue(null, "lon"))
		assertEquals(XmlPullParser.END_TAG, parser.next())
		assertEquals("trkpt", parser.getName())
		assertEquals(XmlPullParser.END_TAG, parser.next())
		assertEquals("trkseg", parser.getName())
		assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
		parser.close()
	}

	/** The regression that broke multi-track import: a synthetic END_TAG must not unwind the parent. */
	@Test
	fun selfClosingChildrenDoNotUnwindTheParent() {
		val parser = parserFor("<trkseg><trkpt/><trkpt/><trkpt/></trkseg>")
		assertEquals(
			listOf("START:trkseg", "START:trkpt", "END:trkpt", "START:trkpt", "END:trkpt", "START:trkpt", "END:trkpt", "END:trkseg"),
			drain(parser)
		)
		parser.close()
	}

	@Test
	fun reportsDepthLikeXmlPull() {
		val parser = parserFor("<a><b><c/></b></a>")
		assertEquals(0, parser.getDepth()) // START_DOCUMENT
		parser.next(); assertEquals(1, parser.getDepth())
		parser.next(); assertEquals(2, parser.getDepth())
		parser.next(); assertEquals(3, parser.getDepth()) // <c/> START
		parser.next(); assertEquals(3, parser.getDepth()) // <c/> synthetic END
		parser.next(); assertEquals(2, parser.getDepth())
		parser.next(); assertEquals(1, parser.getDepth())
		parser.close()
	}

	@Test
	fun coalescesTextSplitByAComment() {
		val parser = parserFor("<name>Hi<!-- pause -->There</name>")
		assertEquals(listOf("START:name", "TEXT:HiThere", "END:name"), drain(parser))
		parser.close()
	}

	@Test
	fun mergesCdataIntoText() {
		val parser = parserFor("<desc>a<![CDATA[<b & c>]]>d</desc>")
		assertEquals(listOf("START:desc", "TEXT:a<b & c>d", "END:desc"), drain(parser))
		parser.close()
	}

	@Test
	fun expandsPredefinedAndNumericEntities() {
		val parser = parserFor("<n>&amp;&lt;&gt;&quot;&apos;&#65;&#x42;</n>")
		assertEquals(listOf("START:n", "TEXT:&<>\"'AB", "END:n"), drain(parser))
		parser.close()
	}

	@Test
	fun decodesEntitiesInAttributeValues() {
		val parser = parserFor("<n v=\"a&amp;b&#67;\"/>")
		parser.next()
		assertEquals("a&bC", parser.getAttributeValue(null, "v"))
		parser.close()
	}

	@Test
	fun hidesNamespaceDeclarationsWhenNamespacesAreProcessed() {
		val parser = parserFor(
			"<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxx=\"http://x/\" creator=\"OsmAnd\"/>"
		)
		parser.next()
		assertEquals(1, parser.getAttributeCount())
		assertEquals("creator", parser.getAttributeName(0))
		assertEquals("OsmAnd", parser.getAttributeValue(0))
		parser.close()
	}

	@Test
	fun reportsNamespaceDeclarationsWhenNamespacesAreOff() {
		val parser = parserFor(
			"<gpx xmlns:gpxx=\"http://x/\" creator=\"OsmAnd\"/>",
			namespaces = false
		)
		parser.next()
		assertEquals(2, parser.getAttributeCount())
		parser.close()
	}

	@Test
	fun returnsLocalNameAndNamespaceUriWhenNamespacesAreProcessed() {
		val parser = parserFor("<gpx xmlns:gpxx=\"http://garmin.com/xmlschemas/\"><gpxx:trk/></gpx>")
		parser.next()
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("trk", parser.getName())
		assertEquals("gpxx", parser.getPrefix())
		assertEquals("http://garmin.com/xmlschemas/", parser.getNamespace())
		parser.close()
	}

	@Test
	fun returnsQualifiedNameWhenNamespacesAreOff() {
		val parser = parserFor(
			"<kml xmlns:gx=\"http://x/\"><gx:Track/></kml>",
			namespaces = false
		)
		parser.next()
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("gx:Track", parser.getName())
		parser.close()
	}

	@Test
	fun nextTextReadsElementContentAndEmptyElements() {
		val parser = parserFor("<r><a>text</a><b/></r>")
		parser.next()
		parser.next()
		assertEquals("text", parser.nextText())
		assertEquals(XmlPullParser.END_TAG, parser.getEventType())
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("", parser.nextText())
		parser.close()
	}

	@Test
	fun isWhitespaceMarksBlankTextRuns() {
		val parser = parserFor("<r>\n\t<a>x</a>\n</r>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals(XmlPullParser.TEXT, parser.next())
		assertTrue(parser.isWhitespace())
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals(XmlPullParser.TEXT, parser.next())
		assertTrue(!parser.isWhitespace())
		parser.close()
	}

	@Test
	fun readsFromAnOkioSource() {
		val parser = XmlPullParser().apply {
			setInput(Buffer().writeUtf8("<gpx><trk>Route</trk></gpx>"), null)
		}
		assertEquals(listOf("START:gpx", "START:trk", "TEXT:Route", "END:trk", "END:gpx"), drain(parser))
		parser.close()
	}

	@Test
	fun detectsUtf8ByteOrderMark() {
		val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
				"<n>café</n>".encodeToByteArray()
		val parser = parserForBytes(bytes)
		assertEquals(listOf("START:n", "TEXT:café", "END:n"), drain(parser))
		parser.close()
	}

	@Test
	fun detectsUtf16ByteOrderMarks() {
		val xml = "<n>Привет</n>"
		val le = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + utf16(xml, bigEndian = false)
		val be = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + utf16(xml, bigEndian = true)
		for (bytes in listOf(le, be)) {
			val parser = parserForBytes(bytes)
			assertEquals(listOf("START:n", "TEXT:Привет", "END:n"), drain(parser))
			parser.close()
		}
	}

	@Test
	fun honoursDeclaredWindows1251() {
		// "Привет" in windows-1251: 0xCF 0xF0 0xE8 0xE2 0xE5 0xF2
		val body = byteArrayOf(0xCF.toByte(), 0xF0.toByte(), 0xE8.toByte(), 0xE2.toByte(), 0xE5.toByte(), 0xF2.toByte())
		val bytes = "<?xml version=\"1.0\" encoding=\"windows-1251\"?><n>".encodeToByteArray() +
				body + "</n>".encodeToByteArray()
		val parser = parserForBytes(bytes)
		assertEquals(listOf("START:n", "TEXT:Привет", "END:n"), drain(parser))
		parser.close()
	}

	@Test
	fun honoursDeclaredLatin1() {
		val bytes = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><n>".encodeToByteArray() +
				byteArrayOf(0xE9.toByte()) + "</n>".encodeToByteArray()
		val parser = parserForBytes(bytes)
		assertEquals(listOf("START:n", "TEXT:é", "END:n"), drain(parser))
		parser.close()
	}

	@Test
	fun reportsTheDetectedEncoding() {
		val bytes = "<?xml version=\"1.0\" encoding=\"windows-1251\"?><n/>".encodeToByteArray()
		val parser = parserForBytes(bytes)
		parser.next()
		assertEquals("windows-1251", parser.getInputEncoding()?.lowercase())
		parser.close()
	}

	@Test
	fun rejectsMismatchedTags() {
		val parser = parserFor("<a><b></a></b>")
		assertFailsWith<XmlParserException> { drain(parser) }
		parser.close()
	}

	@Test
	fun rejectsTruncatedDocuments() {
		val parser = parserFor("<a><b>text")
		assertFailsWith<XmlParserException> { drain(parser) }
		parser.close()
	}

	@Test
	fun errorMessageCarriesTheParserDetail() {
		val parser = parserFor("<a><b></a></b>")
		val failure = assertFailsWith<XmlParserException> { drain(parser) }
		assertTrue(failure.message!!.isNotEmpty(), "expected a libxml2 message, got: ${failure.message}")
		parser.close()
	}

	/** Text and attribute values far larger than any internal buffer must survive intact. */
	@Test
	fun readsValuesLargerThanTheReaderBuffers() {
		val long = "x".repeat(200_000)
		val parser = parserFor("<n v=\"$long\">$long</n>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals(long, parser.getAttributeValue(null, "v"))
		assertEquals(XmlPullParser.TEXT, parser.next())
		assertEquals(long, parser.getText())
		parser.close()
	}

	/** A document much larger than the IO buffer, read through the streaming Source path. */
	@Test
	fun streamsALargeDocument() {
		val builder = StringBuilder("<trkseg>")
		repeat(20_000) { builder.append("<trkpt lat=\"1.0\" lon=\"2.0\"><ele>$it</ele></trkpt>") }
		builder.append("</trkseg>")
		val parser = XmlPullParser().apply { setInput(Buffer().writeUtf8(builder.toString()), null) }
		var points = 0
		var lastEle: String? = null
		while (true) {
			when (parser.next()) {
				XmlPullParser.START_TAG -> if (parser.getName() == "trkpt") points++
					else if (parser.getName() == "ele") lastEle = parser.nextText()
				XmlPullParser.END_DOCUMENT -> {
					assertEquals(20_000, points)
					assertEquals("19999", lastEle)
					parser.close()
					return
				}
			}
		}
	}

	@Test
	fun reusesTheParserAcrossDocuments() {
		val parser = XmlPullParser()
		parser.setInput(tempFile("<a>1</a>".encodeToByteArray()), "UTF-8")
		assertEquals(listOf("START:a", "TEXT:1", "END:a"), drain(parser))
		parser.setInput(tempFile("<b>2</b>".encodeToByteArray()), "UTF-8")
		assertEquals(listOf("START:b", "TEXT:2", "END:b"), drain(parser))
		parser.close()
	}

	@Test
	fun returnsNoNameForTextEvents() {
		val parser = parserFor("<a>text</a>")
		parser.next()
		assertEquals(XmlPullParser.TEXT, parser.next())
		assertNull(parser.getName())
		assertEquals(0, parser.getAttributeCount())
		parser.close()
	}

	private fun utf16(value: String, bigEndian: Boolean): ByteArray {
		val out = ByteArray(value.length * 2)
		for (i in value.indices) {
			val unit = value[i].code
			if (bigEndian) {
				out[i * 2] = (unit shr 8).toByte()
				out[i * 2 + 1] = (unit and 0xFF).toByte()
			} else {
				out[i * 2] = (unit and 0xFF).toByte()
				out[i * 2 + 1] = (unit shr 8).toByte()
			}
		}
		return out
	}
}
