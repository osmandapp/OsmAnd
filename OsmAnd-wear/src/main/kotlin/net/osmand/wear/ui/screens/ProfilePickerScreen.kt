package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
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
import net.osmand.wear.api.ProfileInfo
import net.osmand.wear.ui.theme.OsmAndWearColors

/**
 * Profile picker. The order is the phone's own profile order; the mockups ask for most recently
 * used first, which OsmAnd does not record, so that ordering waits for a source of that data.
 */
@Composable
fun ProfilePickerScreen(
	profiles: List<ProfileInfo>,
	icons: Map<String, ImageBitmap>,
	onSelect: (String) -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			item {
				ListHeader { Text(stringResource(R.string.wear_profile)) }
			}
			items(profiles.size) { index ->
				val profile = profiles[index]
				Button(
					onClick = { onSelect(profile.key) },
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.buttonColors(
						containerColor = OsmAndWearColors.AltChipContainer,
						contentColor = OsmAndWearColors.ChipContent,
						iconColor = OsmAndWearColors.AltAccent
					),
					icon = {
						ProfileGlyph(profile.iconKey?.let { icons[it] }, OsmAndWearColors.AltAccent)
					},
					label = { Text(profile.title) },
					secondaryLabel = if (profile.selected) {
						{ Text(stringResource(R.string.wear_profile_selected)) }
					} else {
						null
					}
				)
			}
		}
	}
}
