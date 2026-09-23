package net.osmand.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

import kotlinx.coroutines.launch

import net.osmand.wear.R
import net.osmand.wear.api.WearCommand
import net.osmand.wear.data.PhoneConnector
import net.osmand.wear.data.PhoneLink
import net.osmand.wear.data.PhoneStateRepository
import net.osmand.wear.data.Snapshot
import net.osmand.wear.ui.screens.ConnectionScreen
import net.osmand.wear.ui.screens.FinishRecordingDialog
import net.osmand.wear.ui.screens.HomeScreen
import net.osmand.wear.ui.screens.MessageScreen
import net.osmand.wear.ui.screens.NavigationScreen
import net.osmand.wear.ui.screens.PlaceholderScreen
import net.osmand.wear.ui.screens.ProfilePickerScreen
import net.osmand.wear.ui.screens.RecordingPager
import net.osmand.wear.ui.screens.RecordingStartScreen
import net.osmand.wear.ui.screens.isSessionOpen
import net.osmand.wear.ui.theme.OsmAndWearTheme

object Routes {
	const val HOME = "home"
	const val NAVIGATION = "navigation"
	const val RECORDING = "recording"
	const val PROFILES = "profiles"
	const val SETTINGS = "settings"
}

@Composable
fun WearApp(connector: PhoneConnector) {
	OsmAndWearTheme {
		val navController = rememberSwipeDismissableNavController()
		val scope = rememberCoroutineScope()

		// Fire and forget: the phone answers every command with a fresh snapshot, and that echo —
		// not the call itself — is what moves the screen.
		val send: (WearCommand) -> Unit = { command -> scope.launch { connector.sendCommand(command) } }

		AppScaffold {
			SwipeDismissableNavHost(
				navController = navController,
				startDestination = Routes.HOME
			) {
				composable(Routes.HOME) {
					// Collected inside the destination, not in the enclosing scope: the nav graph
					// builder runs once, so anything derived outside stays frozen at its first
					// value while the screen keeps recomposing around it.
					val link by PhoneStateRepository.link.collectAsStateWithLifecycle()

					when (val current = link) {
						is PhoneLink.Connected -> HomeScreen(
							state = current.snapshot.state,
							onOpen = { route -> navController.navigate(route) }
						)

						else -> ConnectionScreen(link = current, connector = connector)
					}
				}
				composable(Routes.NAVIGATION) {
					val snapshot = currentSnapshot()
					NavigationScreen(
						navigation = snapshot?.state?.navigation,
						icons = snapshot?.icons.orEmpty(),
						onStop = {
							send(WearCommand.StopNavigation)
							navController.popBackStack()
						}
					)
				}
				composable(Routes.RECORDING) {
					val snapshot = currentSnapshot()
					val recording = snapshot?.state?.recording
					var confirmFinish by remember { mutableStateOf(false) }

					if (recording == null) {
						// A null recording state means the monitoring plugin is off on the phone,
						// which is not something the watch can switch on for the user.
						MessageScreen(
							title = stringResource(R.string.wear_recording_off),
							hint = stringResource(R.string.wear_recording_off_hint)
						)
					} else if (recording.isSessionOpen()) {
						RecordingPager(
							recording = recording,
							onPause = { send(WearCommand.PauseRecording) },
							onResume = { send(WearCommand.ResumeRecording) },
							onFinish = { confirmFinish = true },
							onSaveAndContinue = { send(WearCommand.SaveAndContinueRecording) }
						)
						FinishRecordingDialog(
							visible = confirmFinish,
							onDismiss = { confirmFinish = false },
							onConfirm = {
								confirmFinish = false
								send(WearCommand.FinishRecording)
							}
						)
					} else {
						val profiles = snapshot?.state?.profiles.orEmpty()
						val selected = profiles.firstOrNull { it.selected }
						RecordingStartScreen(
							profile = selected,
							profileIcon = selected?.iconKey?.let { snapshot?.icons?.get(it) },
							onPickProfile = { navController.navigate(Routes.PROFILES) },
							onStart = { send(WearCommand.StartRecording) }
						)
					}
				}
				composable(Routes.PROFILES) {
					val snapshot = currentSnapshot()
					ProfilePickerScreen(
						profiles = snapshot?.state?.profiles.orEmpty(),
						icons = snapshot?.icons.orEmpty(),
						onSelect = { key ->
							send(WearCommand.SelectProfile(key))
							navController.popBackStack()
						}
					)
				}
				composable(Routes.SETTINGS) { PlaceholderScreen(Routes.SETTINGS) }
			}
		}
	}
}

@Composable
private fun currentSnapshot(): Snapshot? {
	val link by PhoneStateRepository.link.collectAsStateWithLifecycle()
	return (link as? PhoneLink.Connected)?.snapshot
}
