package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

import net.osmand.wear.R

/** Confirmation before a session is ended and written to the phone. */
@Composable
fun FinishRecordingDialog(visible: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
	AlertDialog(
		visible = visible,
		onDismissRequest = onDismiss,
		icon = {
			Icon(
				painter = painterResource(R.drawable.ic_action_trip_rec_finish),
				contentDescription = null,
				modifier = Modifier.size(32.dp)
			)
		},
		title = { Text(stringResource(R.string.wear_finish_recording)) },
		text = { Text(stringResource(R.string.wear_finish_recording_hint)) },
		confirmButton = {
			AlertDialogDefaults.ConfirmButton(
				onClick = onConfirm
			) {
				Icon(
					painter = painterResource(R.drawable.ic_action_done),
					// Both buttons are glyph-only, so the label a screen reader announces has to
					// come from here.
					contentDescription = stringResource(R.string.wear_finish),
					modifier = Modifier.size(24.dp)
				)
			}
		},
		dismissButton = {
			AlertDialogDefaults.DismissButton(
				onClick = onDismiss
			) {
				Icon(
					painter = painterResource(R.drawable.ic_action_close),
					contentDescription = stringResource(R.string.wear_cancel),
					modifier = Modifier.size(24.dp)
				)
			}
		}
	)
}
