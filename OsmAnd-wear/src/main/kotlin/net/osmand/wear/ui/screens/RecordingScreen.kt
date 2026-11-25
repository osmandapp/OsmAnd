package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import net.osmand.wear.R
import net.osmand.wear.api.ProfileInfo
import net.osmand.wear.api.RecordingState

/**
 * Trip recording before a session exists: which profile the track will be attributed to, and the
 * button that starts it. Recording settings are not offered — the watch has none of its own yet,
 * and the mockups only call for that row when there is something behind it.
 */
@Composable
fun RecordingStartScreen(
	profile: ProfileInfo?,
	profileIcon: ImageBitmap?,
	onPickProfile: () -> Unit,
	onStart: () -> Unit
) {
	val listState = rememberScalingLazyListState()

	ScreenScaffold(
		scrollState = listState,
		// Start lives in the scaffold's own slot rather than as a list item: an EdgeButton hugs
		// the bottom of the round display and the scaffold hands back the padding the list needs
		// to clear it.
		edgeButton = {
			EdgeButton(
				onClick = onStart,
				buttonSize = EdgeButtonSize.Medium
			) {
				Icon(
					painter = painterResource(R.drawable.ic_action_trip_rec_start),
					contentDescription = null,
					modifier = Modifier.size(ButtonDefaults.IconSize)
				)
				Text(
					text = stringResource(R.string.wear_start),
					modifier = Modifier.padding(start = 6.dp)
				)
			}
		}
	) { contentPadding ->
		ScalingLazyColumn(
			state = listState,
			contentPadding = contentPadding
		) {
			item {
				ListHeader { Text(stringResource(R.string.wear_trip_recording)) }
			}
			item {
				Button(
					onClick = onPickProfile,
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.filledTonalButtonColors(),
					icon = {
						ProfileGlyph(profileIcon, MaterialTheme.colorScheme.primary)
					},
					label = { Text(profile?.title ?: stringResource(R.string.wear_profile)) },
					secondaryLabel = { Text(stringResource(R.string.wear_profile)) }
				)
			}
		}
	}
}

/** Renders the phone-drawn profile glyph, falling back to nothing rather than a wrong icon. */
@Composable
fun ProfileGlyph(icon: ImageBitmap?, tint: androidx.compose.ui.graphics.Color) {
	icon?.let {
		androidx.compose.foundation.Image(
			bitmap = it,
			contentDescription = null,
			colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
			modifier = Modifier.size(ButtonDefaults.IconSize)
		)
	}
}

/** True when the watch should show the live pager rather than the start screen. */
fun RecordingState?.isSessionOpen(): Boolean = this != null && active
