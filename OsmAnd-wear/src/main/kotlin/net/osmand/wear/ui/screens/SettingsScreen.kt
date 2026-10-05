package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/**
 * Watch settings.
 *
 * The renderer choice is here rather than on the phone because it is the watch's map it decides,
 * and it is worth comparing the two with the map in front of you. Which one is in force comes
 * from the phone, where the renderer lives, so this shows what is really set.
 */
@Composable
fun SettingsScreen(
	legacyMapRenderer: Boolean,
	onSelectMapRenderer: (legacy: Boolean) -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(scrollState = listState) {
		ScalingLazyColumn(
			state = listState,
			contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp)
		) {
			item {
				ListHeader { Text(stringResource(R.string.wear_map_renderer)) }
			}
			item {
				RendererChoice(
					title = stringResource(R.string.wear_map_renderer_legacy),
					hint = stringResource(R.string.wear_map_renderer_legacy_descr),
					selected = legacyMapRenderer,
					onClick = { onSelectMapRenderer(true) }
				)
			}
			item {
				RendererChoice(
					title = stringResource(R.string.wear_map_renderer_opengl),
					hint = stringResource(R.string.wear_map_renderer_opengl_descr),
					selected = !legacyMapRenderer,
					onClick = { onSelectMapRenderer(false) }
				)
			}
		}
	}
}

@Composable
private fun RendererChoice(
	title: String,
	hint: String,
	selected: Boolean,
	onClick: () -> Unit
) {
	val label: @Composable RowScope.() -> Unit = {
		Column(modifier = Modifier.weight(1f)) {
			Text(text = title)
			Text(text = hint, style = MaterialTheme.typography.bodyExtraSmall)
		}
		if (selected) {
			Icon(
				painter = painterResource(R.drawable.ic_action_done),
				contentDescription = stringResource(R.string.wear_profile_selected),
				modifier = Modifier.size(20.dp)
			)
		}
	}
	OutlinedButton(
		onClick = onClick,
		label = label,
		modifier = Modifier.fillMaxWidth()
	)
}
