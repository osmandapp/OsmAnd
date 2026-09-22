package net.osmand.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

import kotlinx.coroutines.launch

import net.osmand.wear.api.WearCommand
import net.osmand.wear.data.PhoneConnector
import net.osmand.wear.data.PhoneLink
import net.osmand.wear.data.PhoneStateRepository
import net.osmand.wear.ui.screens.ConnectionScreen
import net.osmand.wear.ui.screens.HomeScreen
import net.osmand.wear.ui.screens.NavigationScreen
import net.osmand.wear.ui.screens.PlaceholderScreen
import net.osmand.wear.ui.theme.OsmAndWearTheme

object Routes {
	const val HOME = "home"
	const val NAVIGATION = "navigation"
	const val RECORDING = "recording"
	const val SETTINGS = "settings"
}

@Composable
fun WearApp(connector: PhoneConnector) {
	OsmAndWearTheme {
		val navController = rememberSwipeDismissableNavController()
		val scope = rememberCoroutineScope()
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

					// Everything below the root assumes a live link, so the root itself is the
					// only place that has to cope with its absence.
					when (val current = link) {
						is PhoneLink.Connected -> HomeScreen(
							state = current.snapshot.state,
							onOpen = { route -> navController.navigate(route) }
						)

						else -> ConnectionScreen(link = current, connector = connector)
					}
				}
				composable(Routes.NAVIGATION) {
					val link by PhoneStateRepository.link.collectAsStateWithLifecycle()
					val snapshot = (link as? PhoneLink.Connected)?.snapshot
					NavigationScreen(
						navigation = snapshot?.state?.navigation,
						icons = snapshot?.icons.orEmpty(),
						onStop = {
							// Fire and forget: the phone answers with a fresh snapshot, and that
							// echo — not this call — is what updates the screen.
							scope.launch { connector.sendCommand(WearCommand.StopNavigation) }
							navController.popBackStack()
						}
					)
				}
				composable(Routes.RECORDING) { PlaceholderScreen(Routes.RECORDING) }
				composable(Routes.SETTINGS) { PlaceholderScreen(Routes.SETTINGS) }
			}
		}
	}
}
