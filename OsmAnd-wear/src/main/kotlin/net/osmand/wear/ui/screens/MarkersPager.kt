package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.MarkerInfo

/**
 * Map markers: the active one under an arrow on the first page, the rest as a list on the second.
 */
@Composable
fun MarkersPager(
	markers: List<MarkerInfo>,
	onPassed: (String) -> Unit,
	onMoveToTop: (String) -> Unit,
	onAddHere: () -> Unit
) {
	// The empty state carries the add button too, otherwise the only way to get a first marker
	// would be to reach for the phone.
	if (markers.isEmpty()) {
		NoMarkers(onAddHere)
		return
	}

	val pagerState = rememberPagerState { MARKER_PAGE_COUNT }

	HorizontalPagerScaffold(pagerState = pagerState) {
		HorizontalPager(state = pagerState) { page ->
			when (page) {
				0 -> ActiveMarker(markers.first(), onPassed)
				else -> MarkerList(markers, onMoveToTop, onAddHere)
			}
		}
	}
}

@Composable
private fun NoMarkers(onAddHere: () -> Unit) {
	ScreenScaffold {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 12.percentOfWidth()),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = stringResource(R.string.wear_no_markers),
				textAlign = TextAlign.Center,
				style = MaterialTheme.typography.titleMedium
			)
			AddHereButton(onAddHere, Modifier.padding(top = 12.dp))
		}
	}
}

@Composable
private fun ActiveMarker(marker: MarkerInfo, onPassed: (String) -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 10.percentOfWidth(), vertical = 10.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Bearing(marker, size = 64.dp)
		Text(
			text = marker.distanceText,
			style = MaterialTheme.typography.displaySmall,
			maxLines = 1
		)
		Text(
			text = marker.name,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
		)
		Button(
			onClick = { onPassed(marker.id) },
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.filledTonalButtonColors(),
			icon = {
				Icon(
					painter = painterResource(R.drawable.ic_action_done),
					contentDescription = null,
					modifier = Modifier.size(ButtonDefaults.IconSize)
				)
			},
			label = { Text(stringResource(R.string.wear_marker_passed)) }
		)
	}
}

@Composable
private fun MarkerList(
	markers: List<MarkerInfo>,
	onMoveToTop: (String) -> Unit,
	onAddHere: () -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			item { ListHeader { Text(stringResource(R.string.wear_markers)) } }
			items(markers.size) { index ->
				val marker = markers[index]
				Button(
					onClick = { onMoveToTop(marker.id) },
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.filledTonalButtonColors(),
					icon = { Bearing(marker, size = ButtonDefaults.IconSize) },
					label = {
						Text(text = marker.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
					},
					secondaryLabel = { Text(marker.distanceText) }
				)
			}
			item { AddHereButton(onAddHere) }
		}
	}
}

@Composable
private fun AddHereButton(onAddHere: () -> Unit, modifier: Modifier = Modifier) {
	Button(
		onClick = onAddHere,
		modifier = modifier.fillMaxWidth(),
		colors = ButtonDefaults.filledTonalButtonColors(),
		icon = {
			Icon(
				painter = painterResource(R.drawable.ic_action_add),
				contentDescription = null,
				modifier = Modifier.size(ButtonDefaults.IconSize)
			)
		},
		label = { Text(stringResource(R.string.wear_marker_add_here)) }
	)
}

@Composable
private fun Bearing(marker: MarkerInfo, size: androidx.compose.ui.unit.Dp) {
	Icon(
		painter = painterResource(R.drawable.ic_action_start_navigation),
		contentDescription = null,
		tint = if (marker.colorArgb == 0) MaterialTheme.colorScheme.primary else Color(marker.colorArgb),
		modifier = Modifier
			.size(size)
			.rotate(marker.bearingDegrees)
	)
}

private const val MARKER_PAGE_COUNT = 2
