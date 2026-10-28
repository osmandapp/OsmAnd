package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.Metric
import net.osmand.wear.api.RecordingState
import net.osmand.wear.ui.theme.OsmAndWearColors

/**
 * A live recording session: figures on the first page, controls on the second, as in the mockups.
 */
@Composable
fun RecordingPager(
	recording: RecordingState,
	onPause: () -> Unit,
	onResume: () -> Unit,
	onFinish: () -> Unit,
	onSaveAndContinue: () -> Unit
) {
	val pagerState = rememberPagerState { PAGE_COUNT }

	// The scaffold is what lets a pager live inside SwipeDismissableNavHost: it reports the
	// pager's scroll position upwards, so a horizontal drag pages instead of being swallowed by
	// the host's swipe-to-dismiss, and it supplies the page indicator.
	HorizontalPagerScaffold(pagerState = pagerState) {
		HorizontalPager(state = pagerState) { page ->
			when (page) {
				0 -> RecordingFigures(recording)
				else -> RecordingControls(recording, onPause, onResume, onFinish, onSaveAndContinue)
			}
		}
	}
}

@Composable
private fun RecordingFigures(recording: RecordingState) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 6.percentOfWidth(), vertical = 10.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		StatusLine(recording)

		FieldLabel(stringResource(R.string.wear_distance), recording.distance.unit)
		Text(
			text = recording.distance.value,
			fontSize = 40.sp,
			maxLines = 1
		)

		Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
			Field(
				modifier = Modifier.weight(1f),
				label = stringResource(R.string.wear_time_span),
				unit = "",
				value = recording.timeSpan
			)
			Field(
				modifier = Modifier.weight(1f),
				label = stringResource(R.string.wear_speed),
				unit = recording.speed.unit,
				// The mockups show a dash rather than a speed frozen at the moment of pausing.
				value = recording.speed.value.ifEmpty { "—" }
			)
		}
		Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
			Field(
				modifier = Modifier.weight(1f),
				label = stringResource(R.string.wear_uphill),
				unit = recording.uphill.unit,
				value = recording.uphill.value.ifEmpty { "—" }
			)
			Field(
				modifier = Modifier.weight(1f),
				label = stringResource(R.string.wear_downhill),
				unit = recording.downhill.unit,
				value = recording.downhill.value.ifEmpty { "—" }
			)
		}
	}
}

@Composable
private fun RecordingControls(
	recording: RecordingState,
	onPause: () -> Unit,
	onResume: () -> Unit,
	onFinish: () -> Unit,
	onSaveAndContinue: () -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			item {
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text(
						text = stringResource(R.string.wear_trip_recording),
						style = MaterialTheme.typography.titleMedium
					)
					StatusLine(recording, withTime = true)
				}
			}
			item {
				ControlButton(
					label = if (recording.paused) R.string.wear_resume else R.string.wear_pause,
					icon = if (recording.paused) R.drawable.ic_action_trip_rec_start
					else R.drawable.ic_action_trip_rec_pause,
					primary = true,
					onClick = if (recording.paused) onResume else onPause
				)
			}
			item {
				ControlButton(
					label = R.string.wear_finish,
					icon = R.drawable.ic_action_trip_rec_finish,
					primary = false,
					onClick = onFinish
				)
			}
			item {
				ControlButton(
					label = R.string.wear_save_and_continue,
					icon = R.drawable.ic_action_trip_rec_save,
					primary = false,
					onClick = onSaveAndContinue
				)
			}
		}
	}
}

@Composable
private fun StatusLine(recording: RecordingState, withTime: Boolean = false) {
	val paused = recording.paused
	val text = buildString {
		append(if (paused) stringResource(R.string.wear_paused) else stringResource(R.string.wear_rec))
		if (withTime && recording.timeSpan.isNotEmpty()) {
			append(' ')
			append(recording.timeSpan)
		}
	}
	Text(
		text = text,
		fontSize = 12.sp,
		maxLines = 1,
		color = if (paused) OsmAndWearColors.RecPaused else OsmAndWearColors.RecActive
	)
}

@Composable
private fun FieldLabel(label: String, unit: String) {
	Text(
		text = if (unit.isEmpty()) label.uppercase() else "${label.uppercase()} ${unit.uppercase()}",
		fontSize = 10.sp,
		lineHeight = 12.sp,
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
		textAlign = TextAlign.Center
	)
}

@Composable
private fun Field(modifier: Modifier, label: String, unit: String, value: String) {
	Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
		FieldLabel(label, unit)
		Text(text = value, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
	}
}

@Composable
private fun ControlButton(label: Int, icon: Int, primary: Boolean, onClick: () -> Unit) {
	Button(
		onClick = onClick,
		modifier = Modifier.fillMaxWidth(),
		colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
		icon = {
			Icon(
				painter = painterResource(icon),
				contentDescription = null,
				modifier = Modifier.size(ButtonDefaults.IconSize)
			)
		},
		label = { Text(stringResource(label)) }
	)
}

private const val PAGE_COUNT = 2
