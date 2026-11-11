package net.osmand.shared.gpx

import android.system.Os
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.shared.io.KFile
import okio.FileSystem
import okio.IOException
import java.io.File
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GpxFileCopyAndroidTest {
	private val fs = FileSystem.SYSTEM
	private val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
		"gpx-copy-${System.nanoTime()}").apply { mkdirs() }
	private val source = KFile(File(directory, "original.gpx").path)
	private val destination = KFile(File(directory, "copy.gpx").path)

	@AfterTest
	fun cleanup() {
		directory.deleteRecursively()
	}

	@Test
	fun preservesXmlAndClosesDestinationDescriptors() {
		val xml = "\uFEFF<gpx xmlns='http://www.topografix.com/GPX/1/1' xmlns:x='urn:x'>\r\n" +
			"<metadata><name>Old<!--keep--></name></metadata><trk><name>Old</name><type>hiking</type>" +
			"<extensions><x:data><![CDATA[<name>Old</name>]]></x:data></extensions>" +
			"<trkseg><trkpt lat='1.123456789123' lon='2.000000000'/></trkseg></trk></gpx>"
		fs.write(source.path) { writeUtf8(xml) }
		repeat(32) {
			GpxFileCopy().copyWithUpdatedNames(source, destination, "New & 🏔")
			assertEquals(xml.replace("<name>Old<!--keep-->", "<name>New &amp; 🏔<!--keep-->")
				.replace("<trk><name>Old</name>", "<trk><name>New &amp; 🏔</name>"),
				fs.read(destination.path) { readUtf8() })
			assertEquals(xml, fs.read(source.path) { readUtf8() })
			fs.delete(destination.path)
		}
		val leaked = File("/proc/self/fd").listFiles().orEmpty().mapNotNull {
			runCatching { Os.readlink(it.path) }.getOrNull()
		}.filter { it.startsWith(directory.canonicalPath) }
		assertEquals(emptyList(), leaked)
	}

	@Test
	fun refusesExistingFilesAndSymlinks() {
		fs.write(source.path) { writeUtf8("<gpx/>") }
		fs.write(destination.path) { writeUtf8("existing") }
		assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, destination, "New") }
		assertEquals("existing", fs.read(destination.path) { readUtf8() })
		fs.delete(destination.path)
		Os.symlink(source.path(), destination.path())
		assertFailsWith<IOException> { GpxFileCopy().copyWithUpdatedNames(source, destination, "New") }
		assertEquals(source.path(), Os.readlink(destination.path()))
		assertEquals("<gpx/>", fs.read(source.path) { readUtf8() })
	}

	@Test
	fun concurrentCopiesHaveExactlyOneWinner() {
		fs.write(source.path) { writeUtf8("<gpx><metadata><name>Old</name></metadata></gpx>") }
		val executor = Executors.newFixedThreadPool(8)
		val barrier = CyclicBarrier(8)
		try {
			val calls = (0 until 8).map { index ->
				executor.submit<Boolean> {
					barrier.await(10, TimeUnit.SECONDS)
					try {
						GpxFileCopy().copyWithUpdatedNames(source, destination, "Copy $index")
						true
					} catch (_: IOException) {
						false
					}
				}
			}
			val results = calls.map { it.get(15, TimeUnit.SECONDS) }
			assertEquals(1, results.count { it })
			assertEquals("<gpx><metadata><name>Copy ${results.indexOf(true)}</name></metadata></gpx>",
				fs.read(destination.path) { readUtf8() })
			assertEquals("<gpx><metadata><name>Old</name></metadata></gpx>", fs.read(source.path) { readUtf8() })
		} finally {
			executor.shutdownNow()
			assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS))
		}
	}
}
