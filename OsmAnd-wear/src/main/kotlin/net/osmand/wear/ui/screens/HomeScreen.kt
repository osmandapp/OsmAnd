package net.osmand.wear.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.PhoneState
import net.osmand.wear.ui.Routes

/**
 * Root menu, laid out after the start screen mockup in OsmAnd-Issues#2821: the app name as a
 * header, then Trip recording / Navigation / Settings as orange-glyphed chips in that order.
 */
@Composable
fun HomeScreen(state: PhoneState, onOpen: (String) -> Unit) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			// Extra head- and footroom so the first and last entries settle clear of TimeText
			// and the screen edge instead of resting right against them.
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			item {
				ListHeader {
					Text(stringResource(R.string.app_name))
				}
			}
			item {
				MenuButton(R.string.wear_trip_recording, R.drawable.ic_action_track_recordable) {
					onOpen(Routes.RECORDING)
				}
			}
			item {
				MenuButton(R.string.wear_navigation, R.drawable.ic_action_start_navigation) {
					onOpen(Routes.NAVIGATION)
				}
			}
			item {
				MenuButton(R.string.wear_settings, R.drawable.ic_action_settings) {
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
		// A tonal chip, but with OsmAnd's brand accent on the glyph rather than the scheme's
		// primary: orange is what the main menu is drawn in on the phone too.
		colors = ButtonDefaults.filledTonalButtonColors(
			iconColor = MaterialTheme.colorScheme.tertiary
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
