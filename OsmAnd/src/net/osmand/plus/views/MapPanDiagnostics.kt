package net.osmand.plus.views

import android.os.SystemClock
import android.view.MotionEvent
import net.osmand.PlatformUtil
import net.osmand.core.android.MapRendererView
import kotlin.math.hypot

/**
 * Temporary diagnostics for #25736. No event interception, camera writes or location coordinates.
 * MOVE events only update bounded counters; native camera snapshots are sampled at most every 100 ms.
 * Keep enabled in this diagnostic branch: the reporter also tests self-built release variants.
 */
class MapPanDiagnostics @JvmOverloads constructor(
	private val log: (String) -> Unit = { LOG.info(it) },
	private val uptime: () -> Long = { SystemClock.uptimeMillis() }
) {
	private var gesture: Gesture? = null
	private var resumedAt = -1L
	private var recenteredAt = -1L

	@Synchronized
	fun onResume() {
		finish("resume")
		resumedAt = uptime()
		log("PAN25736 resume uptimeMs=$resumedAt")
	}

	@Synchronized
	fun onPause() {
		finish("pause")
		log("PAN25736 pause uptimeMs=${uptime()}")
	}

	@Synchronized
	fun onRecenter() {
		recenteredAt = uptime()
		log("PAN25736 recenter uptimeMs=$recenteredAt")
	}

	@Synchronized
	fun onActivityTouch(event: MotionEvent, screenLocked: Boolean) {
		val current = getGesture(event)
		current.activityEvents++
		current.screenLocked = current.screenLocked || screenLocked
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> current.activityDown = true
			MotionEvent.ACTION_MOVE -> current.activityMoves++
		}
		current.maxPointers = maxOf(current.maxPointers, event.pointerCount)
		current.maxQueueMs = maxOf(current.maxQueueMs, uptime() - event.eventTime)
		current.travelPx = maxOf(current.travelPx, hypot(event.rawX - current.x, event.rawY - current.y))
	}

	@Synchronized
	fun onActivityTouchFinished(event: MotionEvent, startedAt: Long, handled: Boolean) {
		val current = gesture ?: return
		if (current.id != event.downTime) return
		current.maxDispatchMs = maxOf(current.maxDispatchMs, uptime() - startedAt)
		if (!handled) current.unhandledEvents++
		if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
			finish(MotionEvent.actionToString(event.actionMasked))
		}
	}

	@Synchronized
	fun onMapTouch(event: MotionEvent, view: OsmandMapTileView) {
		val current = getGesture(event)
		current.mapEvents++
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				current.mapDown = true
				val settings = view.settings
				val tracking = view.application.mapViewTrackingUtilities
				log("PAN25736 mapDown id=${current.id} linked=${tracking.isMapLinkedToLocation}"
						+ " returning=${tracking.isMovingToMyLocation} userInteraction=${view.isUserMapInteractionActive}"
						+ " removeAnimations=${settings.DO_NOT_USE_ANIMATIONS.get()}"
						+ " positionAnimation=${settings.ANIMATE_MY_LOCATION.get()} renderer=${view.hasMapRenderer()}"
						+ " locationSource=${settings.LOCATION_SOURCE.get()} rotateMode=${settings.ROTATE_MAP.get()}")
			}
			MotionEvent.ACTION_MOVE -> current.mapMoves++
			MotionEvent.ACTION_CANCEL -> current.mapCancels++
		}
		// getTarget() is the fixed gesture anchor, not the camera center. Sample target31 instead.
		val renderer = view.mapRenderer ?: return
		val now = uptime()
		if (current.cameraSamples == 0 || now - current.sampledAt >= 100
				|| event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
			sampleCamera(current, renderer, now)
		}
	}

	@Synchronized
	fun onDetectorInput(event: MotionEvent, present: Boolean, zoom: Boolean, doubleTap: Boolean,
			tilt: Boolean, rotation: Boolean) {
		val current = gesture ?: return
		if (current.id != event.downTime) return
		if (!present) current.detectorMissing++
		if (zoom) current.zoomBlocked++
		if (doubleTap) current.doubleTapBlocked++
		if (tilt) current.tiltEvents++
		if (rotation) current.rotationEvents++
		if (present && !zoom && !doubleTap) {
			if (event.actionMasked == MotionEvent.ACTION_DOWN) current.detectorDown = true
			if (event.actionMasked == MotionEvent.ACTION_MOVE) current.detectorMoves++
		}
	}

	@Synchronized
	fun onCallback(event: MotionEvent?, name: String) {
		val current = gesture ?: return
		if (event == null || event.downTime != current.id) current.otherDownTimeCallbacks++
		current.callbacks[name] = (current.callbacks[name] ?: 0) + 1
	}

	@Synchronized
	fun onNativePanResult(accepted: Boolean) {
		val current = gesture ?: return
		current.nativeCalls++
		if (accepted) current.nativeAccepted++
	}

	@Synchronized
	fun onAnchorResolved(found: Boolean) {
		val current = gesture ?: return
		current.anchorLookups++
		if (!found) current.anchorMisses++
	}

	@Synchronized
	fun onLocationCameraUpdate() {
		val current = gesture ?: return
		current.locationCameraUpdates++
		if (current.callbacks["scroll"] == null) current.locationUpdatesBeforeScroll++
	}

	private fun getGesture(event: MotionEvent): Gesture {
		gesture?.let { if (it.id == event.downTime) return it }
		finish("nextDownTime")
		val now = uptime()
		return Gesture(event.downTime, event.rawX, event.rawY).also {
			gesture = it
			log("PAN25736 begin id=${it.id} afterResumeMs=${if (resumedAt < 0) -1 else now - resumedAt}"
					+ " afterRecenterMs=${if (recenteredAt < 0) -1 else now - recenteredAt}")
		}
	}

	private fun sampleCamera(current: Gesture, renderer: MapRendererView, now: Long) {
		val target = renderer.state.target31
		val frame = renderer.frameId
		if (current.cameraSamples > 0) {
			if (target.x != current.targetX || target.y != current.targetY) current.cameraChanges++
			if (frame != current.frame) current.frameAdvances++
		}
		current.cameraSamples++
		current.sampledAt = now
		current.targetX = target.x
		current.targetY = target.y
		current.frame = frame
	}

	private fun finish(reason: String) {
		val current = gesture ?: return
		gesture = null
		with(current) {
			log("PAN25736 end id=$id reason=$reason durationMs=${uptime() - id}"
					+ " activityDown=$activityDown mapDown=$mapDown detectorDown=$detectorDown"
					+ " activityEvents=$activityEvents mapEvents=$mapEvents"
					+ " activityMoves=$activityMoves mapMoves=$mapMoves detectorMoves=$detectorMoves"
					+ " mapCancels=$mapCancels maxPointers=$maxPointers travelPx=${travelPx.toInt()}"
					+ " maxQueueMs=$maxQueueMs maxDispatchMs=$maxDispatchMs unhandled=$unhandledEvents"
					+ " screenLocked=$screenLocked detectorMissing=$detectorMissing"
					+ " zoomBlocked=$zoomBlocked doubleTapBlocked=$doubleTapBlocked"
					+ " tiltEvents=$tiltEvents rotationEvents=$rotationEvents callbacks=$callbacks"
					+ " otherDownTimeCallbacks=$otherDownTimeCallbacks nativeCalls=$nativeCalls nativeAccepted=$nativeAccepted"
					+ " anchorLookups=$anchorLookups anchorMisses=$anchorMisses"
					+ " cameraSamples=$cameraSamples cameraChanges=$cameraChanges frameAdvances=$frameAdvances"
					+ " locationCameraUpdates=$locationCameraUpdates locationUpdatesBeforeScroll=$locationUpdatesBeforeScroll")
		}
	}

	private class Gesture(val id: Long, val x: Float, val y: Float) {
		var activityDown = false
		var mapDown = false
		var detectorDown = false
		var activityEvents = 0
		var mapEvents = 0
		var activityMoves = 0
		var mapMoves = 0
		var detectorMoves = 0
		var mapCancels = 0
		var maxPointers = 0
		var travelPx = 0f
		var maxQueueMs = 0L
		var maxDispatchMs = 0L
		var unhandledEvents = 0
		var screenLocked = false
		var detectorMissing = 0
		var zoomBlocked = 0
		var doubleTapBlocked = 0
		var tiltEvents = 0
		var rotationEvents = 0
		val callbacks = linkedMapOf<String, Int>()
		var otherDownTimeCallbacks = 0
		var nativeCalls = 0
		var nativeAccepted = 0
		var anchorLookups = 0
		var anchorMisses = 0
		var cameraSamples = 0
		var cameraChanges = 0
		var frameAdvances = 0
		var sampledAt = 0L
		var targetX = 0
		var targetY = 0
		var frame = 0
		var locationCameraUpdates = 0
		var locationUpdatesBeforeScroll = 0
	}

	companion object {
		private val LOG = PlatformUtil.getLog(MapPanDiagnostics::class.java)
	}
}
