package net.osmand.wear.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
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
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.ProfileInfo
import net.osmand.wear.ui.theme.OsmAndWearColors

/**
 * Profile picker. Ordering is decided on the phone — the active profile, which is also the last
 * used one, comes first, then the rest in OsmAnd's own order.
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
				val glyph: @Composable BoxScope.() -> Unit = {
					ProfileGlyph(profile.iconKey?.let { icons[it] }, OsmAndWearColors.AltAccent)
				}
				val label: @Composable RowScope.() -> Unit = {
					Text(text = profile.title, modifier = Modifier.weight(1f))
					if (profile.selected) {
						Icon(
							painter = painterResource(R.drawable.ic_action_done),
							contentDescription = stringResource(R.string.wear_profile_selected),
							modifier = Modifier.size(20.dp)
						)
					}
				}
				val colors = ButtonDefaults.buttonColors(
					containerColor = OsmAndWearColors.AltChipContainer,
					contentColor = OsmAndWearColors.ChipContent,
					iconColor = OsmAndWearColors.AltAccent
				)

				// The selected profile is outlined with a tick rather than labelled, as in the
				// mockups: the ring reads at a glance on a list of near-identical chips.
				if (profile.selected) {
					OutlinedButton(
						onClick = { onSelect(profile.key) },
						modifier = Modifier.fillMaxWidth(),
						colors = colors,
						border = BorderStroke(2.dp, OsmAndWearColors.AltAccent),
						icon = glyph,
						label = label
					)
				} else {
					Button(
						onClick = { onSelect(profile.key) },
						modifier = Modifier.fillMaxWidth(),
						colors = colors,
						icon = glyph,
						label = label
					)
				}
			}
		}
	}
}
