package net.osmand.shared.binary

import com.sun.management.UnixOperatingSystemMXBean
import net.osmand.shared.routing.RoutingTestFixtures
import org.junit.Assume.assumeTrue
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A reader that fails to walk a file's structure closes the file: nobody gets the reader to close it,
 * and the app opens a damaged file again on every search. The jvm counts the files a process has
 * open, which Kotlin/Native does not, so this runs on the jvm only.
 */
class BinaryMapIndexReaderOpenFilesJvmTest {

	@Test
	fun aFileThatCannotBeReadIsNotLeftOpen() {
		val os = ManagementFactory.getOperatingSystemMXBean() as? UnixOperatingSystemMXBean
		assumeTrue("this jvm does not count the files a process has open", os != null)
		val damaged = File.createTempFile("damaged", ".obf")
		try {
			val bytes = File(RoutingTestFixtures.resource("Turn_lanes_test.obf")).readBytes()
			damaged.writeBytes(bytes.copyOf(bytes.size / 2))
			val before = os!!.openFileDescriptorCount
			repeat(OPENS) {
				runCatching { BinaryMapIndexReader(damaged.path) }
			}
			val opened = os.openFileDescriptorCount - before
			assertTrue(opened < OPENS / 10, "$opened files left open by $OPENS readers that failed")
		} finally {
			damaged.delete()
		}
	}

	companion object {
		private const val OPENS = 200
	}
}
