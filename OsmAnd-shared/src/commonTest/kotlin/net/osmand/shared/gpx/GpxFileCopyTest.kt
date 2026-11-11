package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import net.osmand.shared.io.exclusiveSink
import okio.Buffer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.ForwardingSink
import okio.IOException
import okio.Path
import okio.SYSTEM
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GpxFileCopyTest {

	private val fs = FileSystem.SYSTEM
	private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "gpx-copy-${Random.nextLong().toULong()}"
	private val source = KFile(directory / "original.gpx")
	private val destination = KFile(directory / "copy.gpx")

	@AfterTest
	fun cleanup() {
		fs.deleteRecursively(directory)
	}

	@Test
	fun preservesEveryOtherByteIncludingUnknownXmlAndFormatting() {
		val xml = "\uFEFF" + """
			<?xml version='1.0' encoding='UTF-8'?>
			<?custom keep="yes"?>
			<gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:x="urn:unknown" version='1.1' creator='OsmAnd~' x:attr="a > b">
			  <!-- a comment with <trk><name>Old</name></trk> -->
			  <metadata x:attr='1'><name x:attr='keep'>Old</name><x:name>Old</x:name></metadata>
			  <wpt lat='1.0000000' lon='2.0000000'><name>Old</name></wpt>
			  <trk x:attr='keep'>
			    <name>Old</name><type>hiking</type><desc>Old &amp; &#x41;</desc>
			    <extensions><x:data x:attr='a'><x:data>Old</x:data><![CDATA[<name>Old</name>]]></x:data></extensions>
			    <trkseg><trkpt lat='1.123456789123' lon='2.000000000000'><ele>001.2300</ele></trkpt></trkseg>
			    <trkseg><trkpt lat='3' lon='4'/></trkseg>
			  </trk>
			</gpx>
			<!-- tail -->
		""".trimIndent().replace("\n", "\r\n") + "\r\n"
		val expected = xml.replace("<name x:attr='keep'>Old</name>", "<name x:attr='keep'>New</name>")
			.replace("    <name>Old</name><type>", "    <name>New</name><type>")
		assertCopy(xml, "New", expected)
	}

	@Test
	fun changesOnlyMetadataForMultipleTracks() {
		val xml = "<gpx><metadata><name>Old</name></metadata>" +
			"<trk><name>First</name></trk><trk><name>Second</name></trk></gpx>"
		assertCopy(xml, "New", xml.replace(">Old<", ">New<"))
	}

	@Test
	fun updatesTrackEvenWhenMetadataAlreadyMatches() {
		val xml = "<gpx><metadata><name>N&#101;w</name></metadata><trk><name>Old</name></trk></gpx>"
		assertCopy(xml, "New", xml.replace(">Old<", ">New<"))
	}

	@Test
	fun leavesAlreadyMatchingNamesByteIdentical() {
		val xml = "<gpx><metadata><name>A &amp; B</name></metadata><trk><name><![CDATA[A & B]]></name></trk></gpx>"
		assertCopy(xml, "A & B", xml)
	}

	@Test
	fun preservesCommentsAndInstructionsInsideNames() {
		val xml = "<gpx><metadata><name a='keep'>O<!--keep-->l<?keep yes?><![CDATA[d]]></name></metadata>" +
			"<trk><name><![CDATA[Old]]><!--keep-->&amp;tail</name></trk></gpx>"
		val expected = "<gpx><metadata><name a='keep'>New<!--keep--><?keep yes?></name></metadata>" +
			"<trk><name>New<!--keep--></name></trk></gpx>"
		assertCopy(xml, "New", expected)
	}

	@Test
	fun escapesNewNameWithoutChangingItsDecodedValue() {
		val name = "Маршрут 🏔 & < > ]]> \" ' \r\n\t"
		val escaped = "Маршрут 🏔 &amp; &lt; &gt; ]]&gt; \" ' &#13;\n\t"
		val xml = "<gpx><metadata><name>Old</name></metadata><trk><name>Old</name></trk></gpx>"
		assertCopy(xml, name, xml.replace(">Old<", ">$escaped<"))
		val result = GpxUtilities.loadGpxFile(null, Buffer().write(fs.read(destination.path) { readByteArray() }), null, false)
		assertNull(result.error)
		assertEquals(name, result.metadata.name)
		assertEquals(name, result.tracks.single().name)
	}

	@Test
	fun respectsPrefixesAndReboundNamespaces() {
		val xml = "<g:gpx xmlns:g='http://www.topografix.com/GPX/1/1' xmlns:x='urn:x'>" +
			"<x:metadata><x:name>Old</x:name></x:metadata>" +
			"<g:metadata><g:name>Meta</g:name></g:metadata>" +
			"<g:trk><g:name>Track</g:name><g:extensions><g:trk xmlns:g='urn:x'><g:name>Old</g:name></g:trk></g:extensions></g:trk>" +
			"<x:trk><x:name>Old</x:name></x:trk></g:gpx>"
		assertCopy(xml, "New", xml.replace(">Meta<", ">New<").replace(">Track<", ">New<"))
	}

	@Test
	fun insertsMetadataUsingRootNamespace() {
		val xml = "<g:gpx xmlns:g='http://www.topografix.com/GPX/1/1' xmlns='urn:other'><g:trk><g:name>Old</g:name></g:trk></g:gpx>"
		val expected = xml.replace("<g:trk>", "<g:metadata><g:name>New</g:name></g:metadata><g:trk>").replace(">Old<", ">New<")
		assertCopy(xml, "New", expected)
	}

	@Test
	fun insertsMissingNameBeforeMetadataChildren() {
		val xml = "<gpx><metadata a='keep'>\n<!--keep--><desc>description</desc></metadata><trk/></gpx>"
		assertCopy(xml, "New", xml.replace("<metadata a='keep'>", "<metadata a='keep'><name>New</name>"))
	}

	@Test
	fun expandsEmptyMetadataAndNameWithoutDroppingAttributes() {
		for ((xml, expected) in listOf(
			"<gpx><metadata a='keep' /></gpx>" to "<gpx><metadata a='keep' ><name>New</name></metadata></gpx>",
			"<gpx><metadata><name a='keep'/></metadata></gpx>" to "<gpx><metadata><name a='keep'>New</name></metadata></gpx>",
			"<gpx />" to "<gpx ><metadata><name>New</name></metadata></gpx>"
		)) {
			assertCopy(xml, "New", expected)
			fs.delete(destination.path)
		}
	}

	@Test
	fun preservesUnnamedTracks() {
		for (track in listOf("<trk/>", "<trk><name/></trk>", "<trk><name></name></trk>", "<trk><name><!--keep--></name></trk>")) {
			assertCopy("<gpx>$track</gpx>", "New", "<gpx><metadata><name>New</name></metadata>$track</gpx>")
			fs.delete(destination.path)
		}
	}

	@Test
	fun updatesEmptyMetadataNameButKeepsItsComment() {
		assertCopy("<gpx><metadata><name><!--keep--></name></metadata></gpx>", "New",
			"<gpx><metadata><name>New<!--keep--></name></metadata></gpx>")
	}

	@Test
	fun rejectsMalformedAndAmbiguousDocumentsWithoutCreatingACopy() {
		for (xml in listOf(
			"<gpx><trk></gpx>", "<gpx/><gpx/>", "<other/>", "<gpx><!--unfinished",
			"<gpx><metadata/><metadata/></gpx>",
			"<gpx><metadata><name>A</name><name>B</name></metadata></gpx>",
			"<gpx><metadata><name>A<unknown/>B</name></metadata></gpx>",
			"<gpx><metadata><name><unknown/></name></metadata></gpx>",
			"<gpx><trk><name><unknown/></name></trk></gpx>", "<gpx version='1.0'/>",
			"<gpx xmlns='urn:other'/>", "<gpx xmlns='http://www.topografix.com/GPX/1/0'/>",
			"<gpx><metadata><name>&undefined;</name></metadata></gpx>"
		)) assertRejected(xml.encodeToByteArray())
	}

	@Test
	fun rejectsDtdBeforeEntityExpansion() {
		for (xml in listOf(
			"<!DOCTYPE gpx [<!ENTITY name 'Old'>]><gpx><metadata><name>&name;</name></metadata></gpx>",
			"<!DOCTYPE gpx SYSTEM 'file:///does-not-exist'><gpx/>",
			"<!DOCTYPE gpx [<!ENTITY name SYSTEM 'https://example.invalid/private'>]><gpx><metadata><name>&name;</name></metadata></gpx>"
		)) assertRejected(xml.encodeToByteArray())
	}

	@Test
	fun rejectsUnsupportedOrInvalidEncodings() {
		assertRejected("<?xml version='1.0' encoding='ISO-8859-1'?><gpx/>".encodeToByteArray())
		assertRejected("<?xml version='1.1'?><gpx/>".encodeToByteArray())
		assertRejected(byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0x3c, 0))
		assertRejected("<gpx>".encodeToByteArray() + byteArrayOf(0xc0.toByte(), 0xaf.toByte()) + "</gpx>".encodeToByteArray())
	}

	@Test
	fun rejectsInvalidNamesWithoutCreatingACopy() {
		writeSource("<gpx/>".encodeToByteArray())
		for (name in listOf("a\u0000b", "a\u0001b", "a\uFFFFb", "a\uD800b")) {
			assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, destination, name) }
			assertEquals(listOf(source.path), fs.list(directory))
		}
	}

	@Test
	fun refusesExistingDestinationAndSourceAlias() {
		val original = "<gpx/>".encodeToByteArray()
		writeSource(original)
		fs.write(destination.path) { writeUtf8("existing file") }
		assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, destination, "New") }
		assertEquals("existing file", fs.read(destination.path) { readUtf8() })
		assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, source, "New") }
		assertContentEquals(original, fs.read(source.path) { readByteArray() })
		assertEquals(setOf(source.path, destination.path), fs.list(directory).toSet())
	}

	@Test
	fun removesIncompleteCopyAfterWriteOrCloseFailure() {
		for (failWrite in listOf(true, false)) {
			writeSource("<gpx/>".encodeToByteArray())
			val copier = GpxFileCopy(fs) { file ->
				object : ForwardingSink(file.exclusiveSink()) {
					override fun write(source: Buffer, byteCount: Long) {
						if (failWrite) {
							super.write(source, minOf(3, byteCount))
							throw IOException("Injected write failure")
						}
						super.write(source, byteCount)
					}
					override fun close() {
						super.close()
						if (!failWrite) throw IOException("Injected close failure")
					}
				}
			}
			assertFailsWith<IOException> { copier.copyWithUpdatedNames(source, destination, "New") }
			assertEquals(listOf(source.path), fs.list(directory))
			assertEquals("<gpx/>", fs.read(source.path) { readUtf8() })
		}
	}

	@Test
	fun retainsOriginalFailureWhenCleanupAlsoFails() {
		writeSource("<gpx/>".encodeToByteArray())
		val failingFs = object : ForwardingFileSystem(fs) {
			override fun delete(path: Path, mustExist: Boolean) {
				throw IOException("Injected cleanup failure")
			}
		}
		val copier = GpxFileCopy(failingFs) { file ->
			object : ForwardingSink(file.exclusiveSink()) {
				override fun write(source: Buffer, byteCount: Long) {
					super.write(source, minOf(3, byteCount))
					throw IOException("Injected write failure")
				}
			}
		}
		val failure = assertFailsWith<IOException> { copier.copyWithUpdatedNames(source, destination, "New") }
		assertEquals("Injected write failure", failure.message)
		assertEquals("Injected cleanup failure", failure.suppressedExceptions.single().message)
		assertEquals("<gp", fs.read(destination.path) { readUtf8() })
		assertEquals("<gpx/>", fs.read(source.path) { readUtf8() })
	}

	@Test
	fun refusesDestinationCreatedWhilePreparingCopy() {
		writeSource("<gpx/>".encodeToByteArray())
		val copier = GpxFileCopy(fs) { file ->
			fs.write(file.path) { writeUtf8("other writer") }
			file.exclusiveSink()
		}
		assertFailsWith<IOException> { copier.copyWithUpdatedNames(source, destination, "New") }
		assertEquals("other writer", fs.read(destination.path) { readUtf8() })
		assertEquals(setOf(source.path, destination.path), fs.list(directory).toSet())
	}

	@Test
	fun preservesLargeTrackCoordinatesWithoutModelRounding() {
		val points = buildString {
			repeat(10_000) { append("<trkpt lat='1.123456789123' lon='2.000000000000'><unknown>$it</unknown></trkpt>\n") }
		}
		val xml = "<gpx><metadata><name>Old</name></metadata><trk><name>Old</name><trkseg>$points</trkseg></trk></gpx>"
		assertCopy(xml, "New", xml.replace(">Old<", ">New<"))
	}

	private fun assertCopy(xml: String, name: String, expected: String) {
		val original = xml.encodeToByteArray()
		writeSource(original)
		GpxFileCopy().copyWithUpdatedNames(source, destination, name)
		assertContentEquals(expected.encodeToByteArray(), fs.read(destination.path) { readByteArray() })
		assertContentEquals(original, fs.read(source.path) { readByteArray() })
		assertEquals(setOf(source.path, destination.path), fs.list(directory).toSet())
	}

	private fun assertRejected(bytes: ByteArray) {
		writeSource(bytes)
		assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, destination, "New") }
		assertFalse(fs.exists(destination.path))
		assertContentEquals(bytes, fs.read(source.path) { readByteArray() })
		assertEquals(listOf(source.path), fs.list(directory))
	}

	private fun writeSource(bytes: ByteArray) {
		fs.createDirectories(directory)
		fs.write(source.path) { write(bytes) }
	}
}
