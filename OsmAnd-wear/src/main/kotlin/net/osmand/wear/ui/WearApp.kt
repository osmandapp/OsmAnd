package net.osmand.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

import net.osmand.wear.data.PhoneConnector
import net.osmand.wear.data.PhoneLink
import net.osmand.wear.data.PhoneStateRepository
import net.osmand.wear.ui.screens.ConnectionScreen
import net.osmand.wear.ui.screens.HomeScreen
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
		val link by PhoneStateRepository.link.collectAsStateWithLifecycle()

		AppScaffold {
			SwipeDismissableNavHost(
				navController = navController,
				startDestination = Routes.HOME
			) {
				composable(Routes.HOME) {
					// Everything below the root assumes a live link, so the root itself is the
					// only place that has to cope with its absence.
					when (val current = link) {
						is PhoneLink.Connected -> HomeScreen(
							state = current.state,
							onOpen = { route -> navController.navigate(route) }
						)

						else -> ConnectionScreen(
							link = current,
							connector = connector
						)
					}
				}
				composable(Routes.NAVIGATION) { PlaceholderScreen(Routes.NAVIGATION) }
				composable(Routes.RECORDING) { PlaceholderScreen(Routes.RECORDING) }
				composable(Routes.SETTINGS) { PlaceholderScreen(Routes.SETTINGS) }
			}
		}
	}
}
