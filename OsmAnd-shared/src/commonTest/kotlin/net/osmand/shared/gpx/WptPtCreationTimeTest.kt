package net.osmand.shared.gpx

import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.primitives.WptPt
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins down how a waypoint's creation time moves between the GPX `<time>` element and the
 * `osmand:created_on` extension.
 *
 * A point created by the app starts with only `/gpx/wpt/time` (the moment it was created). The first
 * update that carries a timestamp copies that `<time>` value into `/wpt/extensions/osmand:created_on`
 * and sets `<time>` to the moment of the update (thus `<time>` becomes the modification time).
 * Later updates keep the extension and update only `<time>`.
 */
class WptPtCreationTimeTest {

	private val t0 = 1_704_067_200_000L // 2024-01-01T00:00:00Z
	private val edited1 = 1_700_000_000_000L // 2023-11-14T22:13:20Z
	private val edited2 = 1_690_000_000_000L // 2023-07-22T04:26:40Z

	@Test
	fun newPointHasOnlyTimeAndNoCreatedOnExtension() {
		val point = newPoint(t0)

		assertEquals(t0, point.time)
		assertEquals(0L, point.getCreatedOn())
		assertNull(point.getExtension(PointAttributes.POINT_CREATED_ON))
		// Without the extension the GPX time is what is shown as the creation time.
		assertEquals(t0, point.getCreationTimeForDisplay())
	}

	@Test
	fun createAdjustedPointSetsTimeButNoCreatedOnExtension() {
		val before = currentTimeMillis()
		val point = WptPt.createAdjustedPoint(
			10.0, 20.0, null, "name", null, 0, null, null, null, null
		)
		val after = currentTimeMillis()

		assertTrue(point.time in before..after)
		assertEquals(0L, point.getCreatedOn())
		assertEquals(point.time, point.getCreationTimeForDisplay())
	}

	@Test
	fun updateWithoutTimestampKeepsTimeAndAddsNoCreatedOnExtension() {
		val existing = newPoint(t0)
		val newWpt = WptPt(10.0, 20.0)
		newWpt.desc = "description"

		existing.updatePoint(newWpt, false)

		assertEquals(t0, existing.time)
		assertEquals(0L, existing.getCreatedOn())
		assertEquals("description", existing.desc)
		assertEquals(t0, existing.getCreationTimeForDisplay())
	}

	@Test
	fun updateWithTimestampMovesTimeIntoCreatedOnExtension() {
		val existing = newPoint(t0)
		val newWpt = WptPt(10.0, 20.0)
		newWpt.desc = "description"

		val before = currentTimeMillis()
		existing.updatePoint(newWpt, true)
		val after = currentTimeMillis()

		assertEquals(seconds(t0), existing.getCreatedOn())
		assertTrue(existing.time in before..after)
		assertNotEquals(t0, existing.time)
		// The extension now takes precedence over the updated <time>.
		assertEquals(existing.getCreatedOn(), existing.getCreationTimeForDisplay())
	}

	@Test
	fun createdOnExtensionSurvivesGpxRoundTrip() {
		val existing = newPoint(t0)
		val newWpt = WptPt(10.0, 20.0)
		newWpt.desc = "description"
		existing.updatePoint(newWpt, true)

		val gpxFile = GpxFile("test")
		gpxFile.addPoint(existing)
		val reloaded = loadGpx(writeGpxToString(gpxFile))

		assertNull(reloaded.error)
		val point = reloaded.getPointsList().single()
		assertEquals(seconds(t0), point.getCreatedOn())
		assertEquals(seconds(existing.time), point.time)
		assertEquals(point.getCreatedOn(), point.getCreationTimeForDisplay())
	}

	@Test
	fun incomingCreatedOnWinsOverExistingTime() {
		val existing = newPoint(t0)
		val newWpt = WptPt(10.0, 20.0)
		newWpt.setCreatedOn(edited1)

		existing.updatePoint(newWpt, true)

		assertEquals(seconds(edited1), existing.getCreatedOn())
		assertNotEquals(seconds(t0), existing.getCreatedOn())
	}

	@Test
	fun editingCreationTimeOnPointWithoutExtensionStoresEditedValue() {
		val existing = newPoint(t0)
		val gpxFile = GpxFile("test")
		gpxFile.addPoint(existing)

		// Mirrors WptPtEditorFragment.doUpdateWpt: update the point, then apply the picked value.
		val before = currentTimeMillis()
		gpxFile.updateWptPt(existing, editedPoint("description"), true)
		existing.setCreatedOn(edited1)
		val after = currentTimeMillis()

		assertTrue(existing.time in before..after)
		// Current behaviour: the edited value replaces the pre-edit <time> in the extension.
		assertEquals(seconds(edited1), existing.getCreatedOn())
		assertNotEquals(seconds(t0), existing.getCreatedOn())
		assertEquals(existing.getCreatedOn(), existing.getCreationTimeForDisplay())
	}

	@Test
	fun secondCreationTimeEditUpdatesBothTimeAndExtension() {
		val existing = newPoint(t0)
		val gpxFile = GpxFile("test")
		gpxFile.addPoint(existing)

		gpxFile.updateWptPt(existing, editedPoint("first"), true)
		existing.setCreatedOn(edited1)
		val timeAfterFirstEdit = existing.time

		val before = currentTimeMillis()
		gpxFile.updateWptPt(existing, editedPoint("second"), true)
		existing.setCreatedOn(edited2)
		val after = currentTimeMillis()

		assertTrue(existing.time in before..after)
		assertTrue(existing.time >= timeAfterFirstEdit)
		assertEquals(seconds(edited2), existing.getCreatedOn())
		assertNotEquals(seconds(edited1), existing.getCreatedOn())
		assertEquals(existing.getCreatedOn(), existing.getCreationTimeForDisplay())
	}

	private fun newPoint(time: Long) = WptPt(10.0, 20.0, time, Double.NaN, 0f, Float.NaN)

	private fun editedPoint(description: String) =
		WptPt(10.0, 20.0, description, "name", null, null, null, null)

	/** The extension stores whole seconds, so compare against the formatted value. */
	private fun seconds(millis: Long) = GpxUtilities.parseTime(GpxUtilities.formatTime(millis))

	private fun loadGpx(xml: String) =
		GpxUtilities.loadGpxFile(null, Buffer().writeUtf8(xml), null, false)

	private fun writeGpxToString(gpxFile: GpxFile): String {
		val buffer = Buffer()
		assertNull(GpxUtilities.writeGpx(null, buffer, gpxFile, null))
		return buffer.readUtf8()
	}
}
