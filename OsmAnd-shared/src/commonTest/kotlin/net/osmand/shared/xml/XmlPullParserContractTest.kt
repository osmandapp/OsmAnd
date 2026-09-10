package net.osmand.shared.xml

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The contract every actual of XmlPullParser must honour: same event stream on Android, JVM and
// iOS. iOS-only concerns (encoding auto-detection, the streaming buffer boundaries) live in
// iosTest/XmlPullParserIosTest.
class XmlPullParserContractTest {

	// "UTF-8" is what GpxUtilities.loadGpxFile passes; the JVM actual decodes as Latin-1 without it.
	private fun parser(xml: String): XmlPullParser {
		val parser = XmlPullParser()
		parser.setInput(Buffer().write(xml.encodeToByteArray()), "UTF-8")
		return parser
	}

	private fun events(xml: String): List<String> {
		val parser = parser(xml)
		val out = mutableListOf<String>()
		while (true) {
			val type = parser.next()
			out.add(describe(parser, type))
			if (type == XmlPullParser.END_DOCUMENT) break
		}
		parser.close()
		return out
	}

	private fun describe(parser: XmlPullParser, type: Int): String = when (type) {
		XmlPullParser.START_TAG -> {
			val attrs = (0 until parser.getAttributeCount())
				.joinToString(",") { "${parser.getAttributeName(it)}=${parser.getAttributeValue(it)}" }
			if (attrs.isEmpty()) "START ${parser.getName()}" else "START ${parser.getName()} $attrs"
		}
		XmlPullParser.END_TAG -> "END ${parser.getName()}"
		XmlPullParser.TEXT -> "TEXT ${parser.getText()}"
		XmlPullParser.END_DOCUMENT -> "EOF"
		else -> "OTHER $type"
	}

	private fun textOf(xml: String): String {
		val parser = parser(xml)
		val text = StringBuilder()
		while (true) {
			val type = parser.next()
			if (type == XmlPullParser.TEXT) text.append(parser.getText())
			if (type == XmlPullParser.END_DOCUMENT) break
		}
		parser.close()
		return text.toString()
	}

	@Test
	fun reportsElementsTextAndAttributes() {
		assertEquals(
			listOf(
				"START gpx creator=OsmAnd,version=1.1",
				"START wpt lat=50.1,lon=14.4",
				"START name",
				"TEXT Point",
				"END name",
				"END wpt",
				"END gpx",
				"EOF"
			),
			events("<gpx creator=\"OsmAnd\" version=\"1.1\">" +
				"<wpt lat=\"50.1\" lon=\"14.4\"><name>Point</name></wpt></gpx>")
		)
	}

	// The self-closing element must not consume its parent from the open-tag stack: that bug
	// broke multi-track GPX import with "Mismatched closing tag".
	@Test
	fun selfClosingElementsKeepTheTagStackBalanced() {
		assertEquals(
			listOf(
				"START a",
				"START b", "END b",
				"START c",
				"START d", "END d",
				"START e",
				"START f", "END f",
				"END e",
				"END c",
				"START g", "END g",
				"END a",
				"EOF"
			),
			events("<a><b/><c><d/><e><f/></e></c><g/></a>")
		)
	}

	@Test
	fun reportsTwoTracksWithSelfClosingChildren() {
		assertEquals(
			listOf(
				"START gpx",
				"START trk", "START trkseg",
				"START trkpt lat=1,lon=2", "END trkpt",
				"START trkpt lat=3,lon=4", "END trkpt",
				"END trkseg", "END trk",
				"START trk", "START trkseg",
				"START trkpt lat=5,lon=6", "END trkpt",
				"END trkseg", "END trk",
				"END gpx",
				"EOF"
			),
			events(
				"<gpx><trk><trkseg><trkpt lat=\"1\" lon=\"2\"/><trkpt lat=\"3\" lon=\"4\"/></trkseg></trk>" +
					"<trk><trkseg><trkpt lat=\"5\" lon=\"6\"/></trkseg></trk></gpx>"
			)
		)
	}

	@Test
	fun marksEmptyElementTags() {
		val parser = parser("<a><b/><c>t</c></a>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertTrue(!parser.isEmptyElementTag(), "<a> is not an empty element")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("b", parser.getName())
		assertTrue(parser.isEmptyElementTag(), "<b/> is an empty element")
		assertEquals(XmlPullParser.END_TAG, parser.next())
		assertEquals("b", parser.getName())
		parser.close()
	}

	// org.xmlpull.v1.XmlPullParser: the depth is incremented at START_TAG and decremented only
	// after END_TAG, so an END_TAG reports the depth of the element it closes. An empty element
	// is a START_TAG and an END_TAG at the same depth.
	@Test
	fun reportsDepthOfTheCurrentElement() {
		val parser = parser("<root>text<foobar><empty/></foobar></root>")
		val depths = mutableListOf<String>()
		while (true) {
			val type = parser.next()
			depths.add("${describe(parser, type)} @${parser.getDepth()}")
			if (type == XmlPullParser.END_DOCUMENT) break
		}
		parser.close()
		assertEquals(
			listOf(
				"START root @1",
				"TEXT text @1",
				"START foobar @2",
				"START empty @3",
				"END empty @3",
				"END foobar @2",
				"END root @1",
				"EOF @0"
			),
			depths
		)
	}

	// Garmin writes its extensions under arbitrary prefixes (ns2:, ns3:) bound to the well-known
	// URIs, and GpxUtilities.getQualifiedExtensionTagName() tells known from foreign by the URI.
	@Test
	fun resolvesNamespaceUriOfPrefixedElements() {
		val parser = parser(
			"<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"" +
				" xmlns:ns3=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"" +
				" xmlns:test=\"https://example.com/gpx/test\">" +
				"<ns3:hr>145</ns3:hr><test:hr>1</test:hr><plain/></gpx>"
		)
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("gpx", parser.getName())
		assertEquals("http://www.topografix.com/GPX/1/1", parser.getNamespace())

		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("hr", parser.getName())
		assertEquals("ns3", parser.getPrefix())
		assertEquals("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", parser.getNamespace())
		assertEquals("145", parser.nextText())

		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("hr", parser.getName())
		assertEquals("https://example.com/gpx/test", parser.getNamespace())
		assertEquals("1", parser.nextText())

		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("plain", parser.getName())
		assertEquals(null, parser.getPrefix())
		assertEquals("http://www.topografix.com/GPX/1/1", parser.getNamespace())
		parser.close()
	}

	@Test
	fun resolvesNamespacesPerElementScope() {
		val parser = parser(
			"<root xmlns:p=\"urn:outer\"><a xmlns:p=\"urn:inner\"><p:x/></a><p:y/></root>"
		)
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <root>
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <a>
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <p:x/>
		assertEquals("urn:inner", parser.getNamespace())
		assertEquals(XmlPullParser.END_TAG, parser.next()) // </p:x>
		assertEquals("urn:inner", parser.getNamespace())
		assertEquals(XmlPullParser.END_TAG, parser.next()) // </a>
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <p:y/>
		assertEquals("urn:outer", parser.getNamespace())
		parser.close()
	}

	@Test
	fun reportsNamespaceOfAttributes() {
		val parser = parser("<a xmlns:x=\"urn:x\" x:v=\"prefixed\" v=\"plain\"/>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals(2, parser.getAttributeCount())
		val prefixed = (0 until 2).single { parser.getAttributePrefix(it) == "x" }
		assertEquals("urn:x", parser.getAttributeNamespace(prefixed))
		assertEquals("", parser.getAttributeNamespace(1 - prefixed))
		// An unprefixed attribute is in no namespace, so "" must not match the prefixed one.
		assertEquals("plain", parser.getAttributeValue("", "v"))
		assertEquals("prefixed", parser.getAttributeValue("urn:x", "v"))
		parser.close()
	}

	@Test
	fun coalescesCdataWithSurroundingText() {
		assertEquals(
			listOf("START a", "TEXT before<raw> & tags after", "END a", "EOF"),
			events("<a>before<![CDATA[<raw> & tags ]]>after</a>")
		)
		assertEquals(listOf("START a", "TEXT ", "END a", "EOF"), events("<a><![CDATA[]]></a>"))
	}

	@Test
	fun expandsPredefinedAndNumericEntitiesInText() {
		assertEquals("&<>\"'ABП—", textOf("<a>&amp;&lt;&gt;&quot;&apos;&#65;&#x42;&#1055;&#x2014;</a>"))
	}

	@Test
	fun expandsPredefinedAndNumericEntitiesInAttributeValues() {
		val parser = parser("<a v=\"&amp;&lt;&gt;&quot;&#65;&#x42;&#1055;\" w=\"plain\"/>")
		assertEquals(XmlPullParser.START_TAG, parser.next())
		assertEquals("&<>\"ABП", parser.getAttributeValue(0))
		assertEquals("&<>\"ABП", parser.getAttributeValue("", "v"))
		assertEquals("plain", parser.getAttributeValue("", "w"))
		assertEquals(null, parser.getAttributeValue("", "missing"))
		parser.close()
	}

	@Test
	fun skipsCommentsAndProcessingInstructions() {
		assertEquals(
			listOf("START a", "TEXT tu", "END a", "EOF"),
			events("<?xml version=\"1.0\"?><!-- lead --><a><?pi data?>t<!-- inner -->u</a>")
		)
	}

	@Test
	fun skipsDoctypeWithInternalSubset() {
		assertEquals(
			listOf("START gpx", "TEXT body", "END gpx", "EOF"),
			events("<!DOCTYPE gpx [ <!ELEMENT gpx (#PCDATA)> ]><gpx>body</gpx>")
		)
	}

	@Test
	fun nextTextReadsElementContent() {
		val parser = parser("<a><b>text</b><c></c><d/></a>")
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <a>
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <b>
		assertEquals("text", parser.nextText())
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <c>
		assertEquals("", parser.nextText())
		assertEquals(XmlPullParser.START_TAG, parser.next()) // <d/>
		assertEquals("", parser.nextText())
		parser.close()
	}

	@Test
	fun keepsMultibyteAndSurrogateText() {
		assertEquals("Привет 日本語 𝄞", textOf("<a>Привет 日本語 𝄞</a>"))
	}

	@Test
	fun rejectsMismatchedClosingTag() {
		assertFailsWith<XmlParserException> { events("<a><b>text</c></a>") }
	}

	@Test
	fun rejectsAttributeWithoutValue() {
		assertFailsWith<XmlParserException> { events("<a flag></a>") }
	}

	@Test
	fun rejectsUnquotedAttributeValue() {
		assertFailsWith<XmlParserException> { events("<a v=1></a>") }
	}

	@Test
	fun rejectsUnterminatedAttributeValue() {
		assertFailsWith<XmlParserException> { events("<a v=\"1></a>") }
	}
}
