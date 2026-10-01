package net.osmand.wear.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.wear.compose.material3.ScreenScaffold

import android.os.SystemClock
import android.util.Log

import kotlinx.coroutines.delay

import kotlin.math.pow

import net.osmand.wear.R
import net.osmand.wear.data.MapFrames

/**
 * The phone's map, rendered at this watch's size and streamed here.
 *
 * Nothing is drawn locally: the watch sends gestures and shows what comes back, which is what
 * keeps the map identical to the phone's without carrying any of OsmAnd's rendering here.
 */
@Composable
fun MapScreen(
	carConnected: Boolean,
	onStart: (width: Int, height: Int, density: Float) -> Unit,
	onStop: () -> Unit,
	onPause: (paused: Boolean) -> Unit,
	onZoom: (factor: Float, seq: Int) -> Unit,
	onPan: (dx: Float, dy: Float, seq: Int) -> Unit,
	onRecenter: (seq: Int) -> Unit
) {
	// One screen in a car is the one the driver should be looking at, and it is not this one.
	if (carConnected) {
		MessageScreen(
			title = stringResource(R.string.wear_map_car),
			hint = stringResource(R.string.wear_map_car_hint)
		)
		return
	}

	val configuration = LocalConfiguration.current
	val density = LocalDensity.current.density
	val width = with(LocalDensity.current) { configuration.screenWidthDp.dp.roundToPx() }
	val height = with(LocalDensity.current) { configuration.screenHeightDp.dp.roundToPx() }

	// The renderer on the phone lives exactly as long as this screen does: it is the expensive
	// half of the feature, around 87 MB there, and nothing on the watch needs it once the map
	// is off screen.
	DisposableEffect(width, height, density) {
		onStart(width, height, density)
		onDispose { onStop() }
	}

	var shown by remember { mutableStateOf(Shown()) }
	var pendingZoom by remember { mutableFloatStateOf(1f) }
	var gesturing by remember { mutableStateOf(false) }
	// Numbers every gesture asked of the phone. Frames carry the last one the phone had
	// applied, which is what distinguishes an answer from a frame that was already on its way.
	var asked by remember { mutableIntStateOf(0) }
	// Temporary, alongside WearMapStreamer's frame budget: how long a gesture waits for the
	// frame that answers it, measured from this side so it covers the whole round trip.
	var askedAt by remember { mutableLongStateOf(0L) }
	val focus = remember { FocusRequester() }

	// The watch screen going dark stops the frames too, but leaves the renderer standing:
	// rebuilding it costs the tile load again, and its memory is spent either way while this
	// screen is open.
	LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onPause(true) }
	LifecycleEventEffect(Lifecycle.Event.ON_START) { onPause(false) }

	// Held for as long as the hand is on the watch: the phone has nothing new to show until it
	// hears the gesture, and every frame that arrives meanwhile costs a webp decode and a
	// texture upload on this UI thread.
	LaunchedEffect(gesturing) { onPause(gesturing) }

	// Frame and gesture are one value, replaced in one write, so a composition can never catch
	// the new frame still carrying the finished gesture's offset. Collected here rather than
	// read as state and reset in an effect, because an effect runs a composition too late and
	// that one stale composition is a visible jump.
	//
	// A frame drawn before the phone heard the last gesture is dropped: taking the preview away
	// for it would put the map back where it was until the real answer arrived.
	LaunchedEffect(Unit) {
		MapFrames.frame.collect { arrived ->
			if (arrived == null) {
				shown = Shown()
			} else if (!gesturing && arrived.seq >= asked) {
				if (askedAt != 0L) {
					Log.i(LATENCY_TAG, "gesture $asked answered in "
							+ (SystemClock.elapsedRealtime() - askedAt) + " ms")
					askedAt = 0L
				}
				shown = Shown(arrived.image)
			}
		}
	}

	// Detents are counted up and asked for together: a flick of the bezel is a dozen of them,
	// and a message with a re-render behind each one lands well after the hand has stopped.
	LaunchedEffect(pendingZoom) {
		if (pendingZoom == 1f) {
			return@LaunchedEffect
		}
		delay(BEZEL_SETTLE_MS)
		asked++
		askedAt = SystemClock.elapsedRealtime()
		onZoom(pendingZoom, asked)
		pendingZoom = 1f
		gesturing = false
	}

	ScreenScaffold {
		Box(
			modifier = Modifier.fillMaxSize().clipToBounds(),
			contentAlignment = Alignment.Center
		) {
			val frame = shown.frame
			if (frame != null) {
				// requiredSize, not size: the frame is deliberately wider than the watch, and
				// size() coerces it back into the parent's constraints, which throws the
				// surplus away. Laid out larger and centred, the surplus waits off screen for
				// a drag to bring it in.
				Image(
					bitmap = frame,
					contentDescription = null,
					contentScale = ContentScale.None,
					modifier = Modifier
						.requiredSize(
							with(LocalDensity.current) { frame.width.toDp() },
							with(LocalDensity.current) { frame.height.toDp() }
						)
						.offset { IntOffset(shown.drag.x.toInt(), shown.drag.y.toInt()) }
						.graphicsLayer(scaleX = shown.scale, scaleY = shown.scale)
				)
			} else {
				MessageScreen(title = stringResource(R.string.wear_map_loading))
			}

			// The gesture surface is inset from the left rather than covering the screen: a
			// handler there would swallow the edge swipe that leaves this screen, and not
			// consuming the drag is not enough to give it back.
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(start = LEFT_EDGE_FRACTION.times(configuration.screenWidthDp).dp)
					.onRotaryScrollEvent { event ->
						val detent = if (event.verticalScrollPixels > 0) 1f / BEZEL_STEP else BEZEL_STEP
						gesturing = true
						pendingZoom *= detent
						shown = shown.scaledBy(detent, width, height)
						true
					}
					.pointerInput(Unit) {
						// One finger drags, two pinch, and the same loop handles both so a
						// second finger landing mid-drag does not end the gesture. A tap is
						// recognised here too, rather than by a detector of its own: two
						// fingers arriving for a pinch read as two taps to one of those, and
						// the double tap it then reported recentred the map mid-gesture.
						var lastTap = 0L
						awaitEachGesture {
							awaitFirstDown(requireUnconsumed = false)
							gesturing = true
							// Where the shown frame already stood: a gesture that starts before
							// the previous one's frame has arrived inherits its offset, and only
							// what this gesture adds may be asked for again. Sending the whole
							// offset moved the map twice for one movement of the hand.
							val base = shown.drag
							var pinch = 1f
							var moved = false
							var fingers = 1
							do {
								val event = awaitPointerEvent()
								if (event.changes.any { it.isConsumed }) {
									break
								}
								fingers = maxOf(fingers, event.changes.count { it.pressed })
								val zoomed = event.calculateZoom()
								val panned = event.calculatePan()
								if (zoomed != 1f || panned != Offset.Zero) {
									pinch *= zoomed
									shown = shown.movedBy(panned, zoomed, width, height)
									moved = true
									event.changes.forEach {
										if (it.positionChanged()) {
											it.consume()
										}
									}
								}
							} while (event.changes.any { it.pressed })

							if (moved) {
								askedAt = SystemClock.elapsedRealtime()
								if (pinch != 1f) {
									asked++
									onZoom(pinch, asked)
								}
								asked++
								onPan(shown.drag.x - base.x, shown.drag.y - base.y, asked)
							} else if (fingers == 1) {
								val now = System.currentTimeMillis()
								if (now - lastTap < DOUBLE_TAP_MS) {
									asked++
									onRecenter(asked)
									lastTap = 0L
								} else {
									lastTap = now
								}
							}
							gesturing = false
						}
					}
					.focusRequester(focus)
					.focusable()
			)
		}
	}

	LaunchedEffect(Unit) { focus.requestFocus() }
}

/**
 * A frame together with the gesture being shown on top of it while the phone catches up.
 *
 * Sliding further than the frame's surplus reaches, or shrinking past it, would show what the
 * phone never drew, so both stop at its edge. The finger may carry on past that, but what is
 * asked of the phone is what was shown — otherwise the map jumps the rest of the way on release.
 */
private data class Shown(
	val frame: ImageBitmap? = null,
	val drag: Offset = Offset.Zero,
	val scale: Float = 1f
) {

	fun movedBy(pan: Offset, zoom: Float, width: Int, height: Int): Shown =
		withTransform(drag + pan, scale * zoom, width, height)

	fun scaledBy(zoom: Float, width: Int, height: Int): Shown =
		withTransform(drag, scale * zoom, width, height)

	private fun withTransform(wanted: Offset, zoom: Float, width: Int, height: Int): Shown {
		val held = frame ?: return this
		val shown = zoom.coerceAtLeast(width / held.width.toFloat())
		val slackX = ((held.width * shown - width) / 2f).coerceAtLeast(0f)
		val slackY = ((held.height * shown - height) / 2f).coerceAtLeast(0f)
		return copy(
			drag = Offset(
				wanted.x.coerceIn(-slackX, slackX),
				wanted.y.coerceIn(-slackY, slackY)
			),
			scale = shown
		)
	}
}

/** Matches PagerDefaults.LeftEdgeZoneFraction, so both screens feel the same at the edge. */
private const val LEFT_EDGE_FRACTION = 0.15f

/** A tenth of a zoom level per detent: a whole one per click overshoots far past the eye. */
private val BEZEL_STEP = 2f.pow(0.1f)

private const val BEZEL_SETTLE_MS = 180L

private const val DOUBLE_TAP_MS = 300L

private const val LATENCY_TAG = "OsmAndWearMap"
