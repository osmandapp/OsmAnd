package net.osmand.wear.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.PhoneState
import net.osmand.wear.ui.Routes
import net.osmand.wear.ui.theme.OsmAndWearColors

/**
 * Root menu, laid out after the start screen mockup in OsmAnd-Issues#2821: the app name as a
 * header, then Trip recording / Navigation / Settings as orange-glyphed chips in that order.
 */
@Composable
fun HomeScreen(state: PhoneState, onOpen: (String) -> Unit) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(state = listState) {
			item {
				ListHeader {
					Text(stringResource(R.string.app_name))
				}
			}
			item {
				MenuButton(R.string.wear_trip_recording, R.drawable.ic_trip_recording) {
					onOpen(Routes.RECORDING)
				}
			}
			item {
				MenuButton(R.string.wear_navigation, R.drawable.ic_navigation) {
					onOpen(Routes.NAVIGATION)
				}
			}
			item {
				MenuButton(R.string.wear_settings, R.drawable.ic_settings) {
					onOpen(Routes.SETTINGS)
				}
			}
		}
	}
}

@Composable
private fun MenuButton(
	@StringRes titleRes: Int,
	@DrawableRes iconRes: Int,
	onClick: () -> Unit
) {
	Button(
		onClick = onClick,
		modifier = Modifier.fillMaxWidth(),
		colors = ButtonDefaults.buttonColors(
			containerColor = OsmAndWearColors.ChipContainer,
			contentColor = OsmAndWearColors.ChipContent,
			iconColor = OsmAndWearColors.Accent
		),
		icon = {
			Icon(
				painter = painterResource(iconRes),
				contentDescription = null,
				modifier = Modifier.size(ButtonDefaults.IconSize)
			)
		},
		label = { Text(stringResource(titleRes)) }
	)
}
