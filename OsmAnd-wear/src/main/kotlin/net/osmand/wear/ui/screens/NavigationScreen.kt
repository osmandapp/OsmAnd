package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.ManeuverInfo
import net.osmand.wear.api.NavigationState

/**
 * Active route across two pages.
 *
 * The first carries the next manoeuvre alone, at the size a glance from the handlebars can take.
 * Everything else - how far is left, what comes after, how to stop - is a page away, because in
 * motion a list answers a question nobody is asking yet.
 *
 * Everything shown is already formatted by the phone, so this screen holds no unit or locale
 * logic of its own.
 */
@Composable
fun NavigationScreen(
	navigation: NavigationState?,
	icons: Map<String, ImageBitmap>,
	onStop: () -> Unit
) {
	if (navigation == null) {
		EmptyState(stringResource(R.string.wear_not_navigating))
		return
	}

	val pagerState = rememberPagerState { PAGE_COUNT }

	// The scaffold is what lets a pager live inside SwipeDismissableNavHost: it reports the
	// pager's scroll position upwards, so a horizontal drag pages instead of being swallowed by
	// the host's swipe-to-dismiss, and it supplies the page indicator.
	HorizontalPagerScaffold(pagerState = pagerState) {
		HorizontalPager(state = pagerState) { page ->
			when (page) {
				0 -> NextManeuver(navigation, icons)
				else -> RouteDetails(navigation, icons, onStop)
			}
		}
	}
}

/**
 * The nearest manoeuvre, and nothing else. The distance is the one figure worth reading while
 * moving, so it is the largest thing on the screen; the phone's own turn arrow sits above it.
 */
@Composable
private fun NextManeuver(navigation: NavigationState, icons: Map<String, ImageBitmap>) {
	val maneuver = navigation.maneuvers.firstOrNull()
	if (maneuver == null) {
		EmptyState(stringResource(R.string.wear_not_navigating))
		return
	}

	ScreenScaffold {
		Column(
			modifier = Modifier
				.fillMaxSize()
				// Wider margins than a square screen would need: the corners of a round one eat
				// into the top and bottom rows, and the watch's clock occupies the first of them.
				.padding(
					horizontal = 12.percentOfWidth(),
					vertical = 15.percentOfHeight()
				),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			maneuver.iconKey?.let { icons[it] }?.let { arrow ->
				// The phone draws every glyph white so that colour is decided here, by the
				// watch's own palette. The arrow takes the colour of the figure it belongs to.
				Image(
					bitmap = arrow,
					contentDescription = null,
					colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
					modifier = Modifier.size(48.dp)
				)
			}
			Text(
				text = maneuver.distanceText,
				style = MaterialTheme.typography.displayMedium,
				color = MaterialTheme.colorScheme.tertiary,
				maxLines = 1,
				modifier = Modifier.padding(top = 4.dp)
			)
			maneuver.streetName?.let { street ->
				Text(
					text = street,
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
					// Two lines is what is left once the arrow and the distance have taken
					// theirs. The whole phrase is a page away, where there is room to scroll.
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.padding(top = 6.dp)
				)
			}
			if (navigation.paused) {
				Text(
					text = stringResource(R.string.wear_paused),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.tertiary,
					modifier = Modifier.padding(top = 6.dp)
				)
			}
		}
	}
}

@Composable
private fun RouteDetails(
	navigation: NavigationState,
	icons: Map<String, ImageBitmap>,
	onStop: () -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			// Tighter than the other lists, and chosen by measurement: at this value the
			// remaining distance starts on the same row as the arrow on the page before it,
			// so paging across does not shift the eye.
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 18.dp)
		) {
			item { TripSummary(navigation) }

			items(navigation.maneuvers.size) { index ->
				val maneuver = navigation.maneuvers[index]
				Maneuver(maneuver, maneuver.iconKey?.let { icons[it] })
			}

			item {
				Button(
					onClick = onStop,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					colors = ButtonDefaults.filledTonalButtonColors(),
					icon = {
						Icon(
							painter = painterResource(R.drawable.ic_action_rec_stop),
							contentDescription = null,
							modifier = Modifier.size(ButtonDefaults.IconSize)
						)
					},
					label = { Text(stringResource(R.string.wear_stop)) }
				)
			}
		}
	}
}

@Composable
private fun TripSummary(navigation: NavigationState) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 10.percentOfWidth()),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = navigation.leftDistanceText,
			style = MaterialTheme.typography.displaySmall,
			maxLines = 1
		)
		Text(
			text = listOfNotNull(navigation.leftTimeText, navigation.etaText)
				.filter { it.isNotEmpty() }
				.joinToString("  ·  "),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
		if (navigation.paused) {
			Text(
				text = stringResource(R.string.wear_paused),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.tertiary
			)
		}
	}
}

@Composable
private fun Maneuver(maneuver: ManeuverInfo, icon: ImageBitmap?) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			// The street name comes from the phone at whatever length OSM gives it, and a
			// centred line without this margin runs off both sides of a round display.
			.padding(horizontal = 10.percentOfWidth(), vertical = 6.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			// Drawn on the phone by OsmAnd's own turn drawable — the watch only places and
			// colours it, here to match the distance beside it rather than the page's accent.
			icon?.let {
				Image(
					bitmap = it,
					contentDescription = null,
					colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
					modifier = Modifier.size(30.dp)
				)
			}
			Text(
				text = maneuver.distanceText,
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(start = 8.dp)
			)
		}
		maneuver.streetName?.let { street ->
			Text(
				text = street,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
				// Two lines hold the long "Turn right and go <street>" phrases the phone
				// sends; beyond that a single manoeuvre would push the next one off screen.
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(top = 2.dp)
			)
		}
	}
}

private const val PAGE_COUNT = 2

@Composable
private fun EmptyState(message: String) {
	ScreenScaffold {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 12.percentOfWidth()),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = message,
				textAlign = TextAlign.Center,
				style = MaterialTheme.typography.titleMedium
			)
		}
	}
}
