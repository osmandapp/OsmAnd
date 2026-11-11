package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import net.osmand.shared.io.exclusiveSink
import okio.FileSystem
import okio.IOException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpxFileCopyConcurrencyTest {
	@Test
	fun concurrentInstancesCannotOverwriteEachOthersCopy() {
		val fs = FileSystem.SYSTEM
		val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "gpx-copy-race-${Random.nextLong().toULong()}"
		val source = KFile(directory / "source.gpx")
		val destination = KFile(directory / "copy.gpx")
		val workers = 8
		val barrier = CyclicBarrier(workers)
		val executor = Executors.newFixedThreadPool(workers)
		try {
			fs.createDirectories(directory)
			fs.write(source.path) { writeUtf8("<gpx><metadata><name>Old</name></metadata></gpx>") }
			val calls = (0 until workers).map { index ->
				executor.submit<Boolean> {
					val copier = GpxFileCopy(fs) { file ->
						barrier.await(10, TimeUnit.SECONDS)
						file.exclusiveSink()
					}
					try {
						copier.copyWithUpdatedNames(source, destination, "Copy $index")
						true
					} catch (_: IOException) {
						false
					}
				}
			}
			val results = calls.map { it.get(15, TimeUnit.SECONDS) }
			assertEquals(1, results.count { it })
			val winner = results.indexOf(true)
			assertEquals("<gpx><metadata><name>Copy $winner</name></metadata></gpx>", fs.read(destination.path) { readUtf8() })
			assertEquals("<gpx><metadata><name>Old</name></metadata></gpx>", fs.read(source.path) { readUtf8() })
			assertEquals(setOf(source.path, destination.path), fs.list(directory).toSet())
		} finally {
			executor.shutdownNow()
			assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS))
			fs.deleteRecursively(directory)
		}
	}
}
