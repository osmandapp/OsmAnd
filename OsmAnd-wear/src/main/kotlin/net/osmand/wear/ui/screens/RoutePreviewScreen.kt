package net.osmand.wear.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import kotlinx.coroutines.delay

import net.osmand.wear.R
import net.osmand.wear.api.RoutePreviewState
import net.osmand.wear.data.MapFrames

/**
 * The route the phone has worked out, before setting off. The map is its own drawing and takes
 * no gestures: the ones that would explore it are the ones that leave the screen.
 */
@Composable
fun RoutePreviewScreen(
	preview: RoutePreviewState?,
	onStart: () -> Unit,
	onStartStream: (width: Int, height: Int, density: Float) -> Unit,
	onStopStream: () -> Unit,
	onStillWatching: () -> Unit
) {
	val configuration = LocalConfiguration.current
	val density = LocalDensity.current.density
	val width = with(LocalDensity.current) { configuration.screenWidthDp.dp.roundToPx() }
	val height = with(LocalDensity.current) { configuration.screenHeightDp.dp.roundToPx() }

	DisposableEffect(width, height, density) {
		// The last frame is held for the whole app: without this the preview opens on whatever
		// the map screen was showing.
		MapFrames.clear()
		onStartStream(width, height, density)
		onDispose { onStopStream() }
	}

	// The phone drops the renderer after a minute of quiet, and a preview is looked at longer.
	LaunchedEffect(Unit) {
		while (true) {
			delay(STREAM_PING_MS)
			onStillWatching()
		}
	}

	val frame by MapFrames.frame.collectAsStateWithLifecycle()

	ScreenScaffold {
		Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
			frame?.let {
				Image(
					bitmap = it.image,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize()
				)
			}

			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = 10.percentOfWidth(), vertical = 12.percentOfHeight()),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.SpaceBetween
			) {
				Summary(preview)
				if (preview?.calculating == false) {
					Button(
						onClick = onStart,
						// The label slot is a row starting at the left.
						label = {
							Text(
								text = stringResource(R.string.wear_route_start),
								textAlign = TextAlign.Center,
								modifier = Modifier.fillMaxWidth()
							)
						},
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Composable
private fun Summary(preview: RoutePreviewState?) {
	// On its own ground: a label legible over a park is not legible over a motorway.
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.clip(RoundedCornerShape(SCRIM_CORNER))
			.background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = SCRIM_ALPHA))
			.padding(horizontal = 10.dp, vertical = 4.dp)
	) {
		Text(
			text = preview?.name.orEmpty(),
			style = MaterialTheme.typography.titleSmall,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis
		)
		Text(
			text = when {
				preview == null -> stringResource(R.string.wear_route_preview_failed)
				preview.calculating -> stringResource(R.string.wear_route_calculating)
				else -> listOf(preview.distanceText, preview.timeText)
					.filter { it.isNotEmpty() }
					.joinToString("  ·  ")
			},
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

private val SCRIM_CORNER = 12.dp

private const val SCRIM_ALPHA = 0.88f

/** Comfortably inside WearMapStreamer's silence timeout, which is a minute. */
private const val STREAM_PING_MS = 20_000L
