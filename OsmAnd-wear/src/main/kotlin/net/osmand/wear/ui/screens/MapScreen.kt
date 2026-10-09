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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
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
import androidx.wear.compose.material3.MaterialTheme
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
	openglMissing: Boolean,
	onStart: (width: Int, height: Int, density: Float) -> Unit,
	onStop: () -> Unit,
	onPause: (paused: Boolean) -> Unit,
	onZoom: (factor: Float, seq: Int) -> Unit,
	onPan: (dx: Float, dy: Float, seq: Int) -> Unit,
	onRecenter: (seq: Int) -> Unit
) {
		// A car has one screen the driver should be looking at, and it is not this one.
	if (carConnected) {
		MessageScreen(
			title = stringResource(R.string.wear_map_car),
			hint = stringResource(R.string.wear_map_car_hint)
		)
		return
	}

		// Without it no frame ever comes, and the screen would sit on "loading".
	if (openglMissing) {
		MessageScreen(
			title = stringResource(R.string.wear_map_opengl_off),
			hint = stringResource(R.string.wear_map_opengl_off_hint)
		)
		return
	}

	val configuration = LocalConfiguration.current
	val density = LocalDensity.current.density
	val width = with(LocalDensity.current) { configuration.screenWidthDp.dp.roundToPx() }
	val height = with(LocalDensity.current) { configuration.screenHeightDp.dp.roundToPx() }

	// The renderer on the phone, around 87 MB there, lives exactly as long as this screen.
	DisposableEffect(width, height, density) {
		onStart(width, height, density)
		onDispose { onStop() }
	}

	var shown by remember { mutableStateOf(Shown()) }
	var pendingZoom by remember { mutableFloatStateOf(1f) }
	var gesturing by remember { mutableStateOf(false) }
	// Frames carry the last gesture the phone applied, which is what tells an answer from a
	// frame that was already on its way.
	var asked by remember { mutableIntStateOf(0) }
	// Temporary, alongside WearMapStreamer's frame budget.
	var askedAt by remember { mutableLongStateOf(0L) }
	var lastSeq by remember { mutableIntStateOf(0) }
	var everShown by remember { mutableStateOf(false) }
	val focus = remember { FocusRequester() }

	// A dark screen stops the frames but leaves the renderer standing: rebuilding it costs
	// the tile load again, and its memory is spent either way while this screen is open.
	LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onPause(true) }
	LifecycleEventEffect(Lifecycle.Event.ON_START) { onPause(false) }

	// Held while the hand is on the watch: the phone has nothing new to show until it hears
	// the gesture, and each frame meanwhile costs a decode and a texture upload here.
	LaunchedEffect(gesturing) { onPause(gesturing) }

	// Frame and gesture are one value, replaced in one write: an effect resetting them runs a
	// composition too late, and that one stale composition is a visible jump.
	//
	// A frame older than the last gesture is not an answer to it and is held back - but only
	// for a while. Gestures are sent and forgotten and the phone restarts its count with the
	// stream, so an absolute rule turns one lost message into "never show a frame". Both ways
	// out resynchronise rather than let a single frame through, so the stream recovers.
	LaunchedEffect(Unit) {
		MapFrames.frame.collect { arrived ->
			if (arrived == null) {
				shown = Shown()
				lastSeq = 0
				return@collect
			}
			if (gesturing) {
				return@collect
			}
			val answers = arrived.seq >= asked
			val restarted = arrived.seq < lastSeq
			val waitedLongEnough = askedAt != 0L &&
					SystemClock.elapsedRealtime() - askedAt > ANSWER_DEADLINE_MS
			lastSeq = arrived.seq
			everShown = true
			if (!answers && !restarted && !waitedLongEnough) {
				return@collect
			}
			if (askedAt != 0L) {
				Log.i(LATENCY_TAG, "gesture $asked " + (if (answers) "answered" else "gave up")
						+ " in " + (SystemClock.elapsedRealtime() - askedAt) + " ms")
				askedAt = 0L
			}
			if (!answers) {
				asked = arrived.seq
			}
			shown = Shown(arrived.image)
		}
	}

	// Detents are counted up and asked for together: a flick of the bezel is a dozen of them.
	LaunchedEffect(pendingZoom) {
		if (pendingZoom == 1f) {
			return@LaunchedEffect
		}
		try {
			delay(BEZEL_SETTLE_MS)
			asked++
			askedAt = SystemClock.elapsedRealtime()
			onZoom(pendingZoom, asked)
			pendingZoom = 1f
		} finally {
			// Cleared however this ends: a flag left standing keeps the phone's stream paused.
			gesturing = false
		}
	}

	// The phone drops the renderer after a minute of silence, and only gestures are sent, so
	// simply watching the map counted as silence. The same beat reopens a stream that ended:
	// the request that starts it is made once, when the screen opens.
	LaunchedEffect(width, height, density) {
		while (true) {
			delay(STREAM_PING_MS)
			if (gesturing) {
				continue
			}
			if (everShown && shown.frame == null) {
				onStart(width, height, density)
			} else {
				onPause(false)
			}
		}
	}

	// The deadline runs on its own clock: the phone may have nothing new to show, so waiting
	// for a frame to arrive would be waiting for the one event that has stopped happening.
	LaunchedEffect(asked) {
		if (askedAt == 0L) {
			return@LaunchedEffect
		}
		delay(ANSWER_DEADLINE_MS)
		if (askedAt != 0L) {
			Log.i(LATENCY_TAG, "gesture $asked unanswered after $ANSWER_DEADLINE_MS ms")
			askedAt = 0L
			asked = lastSeq
			onPause(false)
		}
	}

	// Beyond the drawn frame the map is not missing but not drawn yet, which a chequerboard
	// says and flat black does not. Only beyond it: with no frame there is nothing to be
	// beyond, and it is noise behind the waiting message.
	val board = MaterialTheme.colorScheme.surfaceContainer
	// From the foreground, not a second container shade: in a dark scheme those sit so close
	// together that the squares vanish.
	val boardAlternate = MaterialTheme.colorScheme.onSurface.copy(alpha = CHEQUER_CONTRAST)

	ScreenScaffold {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.clipToBounds()
				.drawBehind {
					if (shown.frame != null) {
						chequerboard(board, boardAlternate)
					}
				},
			contentAlignment = Alignment.Center
		) {
			val frame = shown.frame
			if (frame != null) {
					// requiredSize, not size: size() coerces the frame back into the parent's
					// constraints and throws the surplus away.
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

				// Inset from the left: a handler there swallows the edge swipe that leaves this
				// screen, and not consuming the drag is not enough to give it back.
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(start = LEFT_EDGE_FRACTION.times(configuration.screenWidthDp).dp)
					.onRotaryScrollEvent { event ->
						val detent = if (event.verticalScrollPixels > 0) 1f / BEZEL_STEP else BEZEL_STEP
						gesturing = true
						pendingZoom *= detent
						shown = shown.scaledBy(detent)
						true
					}
					.pointerInput(Unit) {
						// One finger drags, two pinch, in one loop so a second finger landing
						// mid-drag does not end the gesture. A tap is recognised here too: a
						// separate detector read a pinch as a double tap and recentred the map.
						var lastTap = 0L
						awaitEachGesture {
							awaitFirstDown(requireUnconsumed = false)
							gesturing = true
							try {
							// Only what this gesture adds may be asked for again: a gesture
							// starting before the previous frame arrived inherits its offset.
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
										shown = shown.movedBy(panned, zoomed)
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
							} finally {
								gesturing = false
							}
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
 * A frame together with the gesture shown on top of it while the phone catches up.
 *
 * The gesture is not stopped at the edge of what the phone drew: that reads as a broken map,
 * where background says plainly it is not drawn yet. What the phone is asked for stays what
 * the eye was shown, so nothing jumps once the finger lifts.
 */
private data class Shown(
	val frame: ImageBitmap? = null,
	val drag: Offset = Offset.Zero,
	val scale: Float = 1f
) {

	fun movedBy(pan: Offset, zoom: Float): Shown = copy(drag = drag + pan, scale = scale * zoom)

	fun scaledBy(zoom: Float): Shown = copy(scale = scale * zoom)
}

/** Fixed to the screen rather than to the map: it stands for nothing having been drawn here. */
private fun DrawScope.chequerboard(light: Color, dark: Color) {
	val step = CHEQUER_SIZE_PX
	drawRect(light)
	var y = 0f
	var row = 0
	while (y < size.height) {
		var x = if (row % 2 == 0) 0f else step
		while (x < size.width) {
			drawRect(dark, topLeft = Offset(x, y), size = Size(step, step))
			x += step * 2
		}
		y += step
		row++
	}
}

private const val CHEQUER_SIZE_PX = 24f

private const val CHEQUER_CONTRAST = 0.12f

/** Narrower than the system's 15% dismiss zone: a map is dragged across its whole width. */
private const val LEFT_EDGE_FRACTION = 0.08f

/** A tenth of a zoom level per detent: a whole one per click overshoots far past the eye. */
private val BEZEL_STEP = 2f.pow(0.1f)

private const val BEZEL_SETTLE_MS = 180L

private const val DOUBLE_TAP_MS = 300L

/**
 * How long a gesture may hold the map still while waiting for the frame that answers it.
 * Measured round trips sit at 1.3-1.7 s, so this is a link in trouble rather than a slow one.
 */
private const val ANSWER_DEADLINE_MS = 3000L

/** Comfortably inside WearMapStreamer's silence timeout, which is a minute. */
private const val STREAM_PING_MS = 20_000L

private const val LATENCY_TAG = "OsmAndWearMap"
