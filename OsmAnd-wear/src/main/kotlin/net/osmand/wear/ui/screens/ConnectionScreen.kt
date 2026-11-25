package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

import kotlinx.coroutines.launch

import net.osmand.wear.R
import net.osmand.wear.data.PhoneConnector
import net.osmand.wear.data.PhoneLink

/**
 * Shown until the first readable snapshot arrives. Controls are never offered here — sending
 * a command with no reachable phone would silently do nothing.
 */
@Composable
fun ConnectionScreen(link: PhoneLink, connector: PhoneConnector) {
	val scope = rememberCoroutineScope()

	// Side margins in percent, not dp: a fixed inset clips on round displays.
	ScreenScaffold {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 12.percentOfWidth()),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			val titleRes = when (link) {
				PhoneLink.Connecting -> R.string.wear_connecting
				PhoneLink.ProtocolMismatch -> R.string.wear_protocol_mismatch
				else -> R.string.wear_phone_not_connected
			}
			Text(
				text = stringResource(titleRes),
				textAlign = TextAlign.Center,
				style = MaterialTheme.typography.titleMedium
			)
			if (link == PhoneLink.NotConnected) {
				Text(
					text = stringResource(R.string.wear_phone_not_connected_hint),
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodySmall
				)
				Button(
					onClick = { scope.launch { connector.refresh() } },
					modifier = Modifier.fillMaxWidth(),
					label = { Text(stringResource(R.string.wear_retry)) }
				)
			}
		}
	}
}
