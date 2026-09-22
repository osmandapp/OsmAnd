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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.ManeuverInfo
import net.osmand.wear.api.NavigationState
import net.osmand.wear.ui.theme.OsmAndWearColors

/**
 * Active route, laid out after the third mockup in OsmAnd-Issues#2821: the remaining trip on
 * top, the nearest manoeuvres below it separated by rules, and Stop at the bottom.
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

	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
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
					colors = ButtonDefaults.buttonColors(
						containerColor = OsmAndWearColors.AltChipContainer,
						contentColor = OsmAndWearColors.ChipContent,
						iconColor = OsmAndWearColors.AltAccent
					),
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
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = navigation.leftDistanceText,
			style = MaterialTheme.typography.displaySmall
		)
		Text(
			text = listOfNotNull(navigation.leftTimeText, navigation.etaText)
				.filter { it.isNotEmpty() }
				.joinToString("  ·  "),
			style = MaterialTheme.typography.bodySmall,
			color = OsmAndWearColors.HeaderContent
		)
		if (navigation.paused) {
			Text(
				text = stringResource(R.string.wear_paused),
				style = MaterialTheme.typography.labelSmall,
				color = OsmAndWearColors.Accent
			)
		}
	}
}

@Composable
private fun Maneuver(maneuver: ManeuverInfo, icon: ImageBitmap?) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 6.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			// Drawn on the phone by OsmAnd's own turn drawable — the watch only places it.
			icon?.let {
				Image(
					bitmap = it,
					contentDescription = null,
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
				color = OsmAndWearColors.HeaderContent,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(top = 2.dp)
			)
		}
	}
}

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
