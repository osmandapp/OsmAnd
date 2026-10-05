package net.osmand.plus.views

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapPanDiagnosticsTest {
	private val lines = mutableListOf<String>()
	private var now = 1_000L
	private val diagnostics = MapPanDiagnostics({ lines.add(it) }, { now })

	@Test
	fun activityOnlyGestureDoesNotClaimMapDelivery() {
		event(MotionEvent.ACTION_DOWN) { diagnostics.onActivityTouch(it, false) }
		event(MotionEvent.ACTION_MOVE, time = 1_100, x = 250f) { diagnostics.onActivityTouch(it, false) }
		event(MotionEvent.ACTION_UP, time = 1_150) {
			diagnostics.onActivityTouch(it, false)
			diagnostics.onActivityTouchFinished(it, now, true)
		}
		val summary = summary()
		assertTrue(summary.contains("activityDown=true mapDown=false detectorDown=false"))
		assertTrue(summary.contains("activityMoves=1 mapMoves=0 detectorMoves=0"))
		assertTrue(summary.contains("travelPx=150"))
	}

	@Test
	fun suppressionAndFrameworkDoubleTapAreReportedSeparately() {
		event(MotionEvent.ACTION_DOWN) {
			diagnostics.onActivityTouch(it, false)
			diagnostics.onDetectorInput(it, true, false, true, false, false)
		}
		event(MotionEvent.ACTION_MOVE, time = 1_050) {
			diagnostics.onDetectorInput(it, true, false, false, false, false)
			diagnostics.onCallback(it, "frameworkDoubleTap")
		}
		diagnostics.onPause()
		val summary = summary()
		assertTrue(summary.contains("detectorDown=false"))
		assertTrue(summary.contains("detectorMoves=1"))
		assertTrue(summary.contains("zoomBlocked=0 doubleTapBlocked=1"))
		assertTrue(summary.contains("callbacks={frameworkDoubleTap=1}"))
	}

	@Test
	fun nativeFalseIsNotReportedAsAnErrorOrAsCameraMovement() {
		event(MotionEvent.ACTION_DOWN) { diagnostics.onActivityTouch(it, false) }
		diagnostics.onNativePanResult(false)
		diagnostics.onNativePanResult(true)
		diagnostics.onPause()
		val summary = summary()
		assertTrue(summary.contains("nativeCalls=2 nativeAccepted=1"))
		assertTrue(summary.contains("cameraSamples=0 cameraChanges=0 frameAdvances=0"))
		assertFalse(summary.contains("error", ignoreCase = true))
	}

	@Test
	fun locationUpdatesBeforeAndAfterScrollAreCounted() {
		event(MotionEvent.ACTION_DOWN) { diagnostics.onActivityTouch(it, false) }
		diagnostics.onLocationCameraUpdate()
		event(MotionEvent.ACTION_MOVE, time = 1_050) { diagnostics.onCallback(it, "scroll") }
		diagnostics.onLocationCameraUpdate()
		diagnostics.onPause()
		assertTrue(summary().contains("locationCameraUpdates=2 locationUpdatesBeforeScroll=1"))
	}

	@Test
	fun cancellationAndNewDownTimeFinishOnlyTheirOwnGesture() {
		event(MotionEvent.ACTION_DOWN) { diagnostics.onActivityTouch(it, false) }
		event(MotionEvent.ACTION_CANCEL, time = 1_050) {
			diagnostics.onActivityTouch(it, false)
			diagnostics.onActivityTouchFinished(it, now, true)
		}
		event(MotionEvent.ACTION_DOWN, down = 2_000, time = 2_000) { diagnostics.onActivityTouch(it, false) }
		event(MotionEvent.ACTION_UP, time = 2_010) {
			// A late callback for the previous stream must not end the new stream.
			diagnostics.onActivityTouchFinished(it, now, true)
		}
		event(MotionEvent.ACTION_DOWN, down = 3_000, time = 3_000) { diagnostics.onActivityTouch(it, false) }
		diagnostics.onPause()
		val summaries = lines.filter { it.startsWith("PAN25736 end") }
		assertEquals(3, summaries.size)
		assertTrue(summaries[0].contains("id=1000 reason=ACTION_CANCEL"))
		assertTrue(summaries[1].contains("id=2000 reason=nextDownTime"))
		assertTrue(summaries[2].contains("id=3000 reason=pause"))
	}

	@Test
	fun movesDoNotProducePerEventLogsAndEventsAreNotModified() {
		event(MotionEvent.ACTION_DOWN) { diagnostics.onActivityTouch(it, false) }
		val initialLines = lines.size
		repeat(1_000) { index ->
			event(MotionEvent.ACTION_MOVE, time = 1_001L + index) {
				val before = it.toString()
				diagnostics.onActivityTouch(it, false)
				diagnostics.onDetectorInput(it, true, false, false, false, false)
				diagnostics.onCallback(it, "scroll")
				diagnostics.onNativePanResult(true)
				diagnostics.onActivityTouchFinished(it, now, true)
				assertEquals(before, it.toString())
			}
		}
		assertEquals(initialLines, lines.size)
		diagnostics.onPause()
		assertTrue(summary().contains("activityMoves=1000"))
		assertTrue(summary().contains("callbacks={scroll=1000}"))
	}

	private fun summary() = lines.single { it.startsWith("PAN25736 end") }

	private fun event(action: Int, down: Long = 1_000, time: Long = down, x: Float = 100f,
			block: (MotionEvent) -> Unit) {
		now = time
		val event = MotionEvent.obtain(down, time, action, x, 100f, 0)
		try {
			block(event)
		} finally {
			event.recycle()
		}
	}
}
