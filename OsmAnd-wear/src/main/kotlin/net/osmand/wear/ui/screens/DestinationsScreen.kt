package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.DestinationGroup
import net.osmand.wear.api.DestinationInfo

/**
 * Places the phone already knows about, in one list.
 *
 * The groups are kept in the order the phone sent them rather than sorted here: home and work
 * first because they are two taps that answer most journeys, then where you were last taken,
 * then the favourites nearest to you, then what you last looked for. Only the phone can judge
 * "nearest" and "last", so only the phone does.
 */
@Composable
fun DestinationsScreen(
	destinations: List<DestinationInfo>,
	onSelect: (DestinationInfo) -> Unit
) {
	if (destinations.isEmpty()) {
		MessageScreen(
			title = stringResource(R.string.wear_destinations_empty),
			hint = stringResource(R.string.wear_destinations_empty_hint)
		)
		return
	}

	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			var shown: DestinationGroup? = null
			for (destination in destinations) {
				if (destination.group != shown) {
					shown = destination.group
					val title = destination.group.title()
					item { ListHeader { Text(stringResource(title)) } }
				}
				item { Destination(destination, onSelect) }
			}
		}
	}
}

@Composable
private fun Destination(destination: DestinationInfo, onSelect: (DestinationInfo) -> Unit) {
	OutlinedButton(
		onClick = { onSelect(destination) },
		icon = {
			Icon(
				painter = painterResource(destination.group.icon()),
				contentDescription = null,
				modifier = Modifier.size(20.dp)
			)
		},
		label = {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = destination.name,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis
				)
				if (destination.distanceText.isNotEmpty()) {
					Text(
						text = destination.distanceText,
						style = MaterialTheme.typography.bodyExtraSmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		},
		modifier = Modifier.fillMaxWidth()
	)
}

private fun DestinationGroup.title(): Int = when (this) {
	DestinationGroup.HOME -> R.string.wear_home
	DestinationGroup.WORK -> R.string.wear_work
	DestinationGroup.FAVOURITE -> R.string.wear_favourites
	DestinationGroup.NAVIGATION_HISTORY -> R.string.wear_navigation_history
	DestinationGroup.SEARCH_HISTORY -> R.string.wear_search_history
}

private fun DestinationGroup.icon(): Int = when (this) {
	DestinationGroup.HOME -> R.drawable.ic_action_home_dark
	DestinationGroup.WORK -> R.drawable.ic_action_work
	DestinationGroup.FAVOURITE -> R.drawable.ic_action_favorite
	DestinationGroup.NAVIGATION_HISTORY, DestinationGroup.SEARCH_HISTORY ->
		R.drawable.ic_action_history
}
