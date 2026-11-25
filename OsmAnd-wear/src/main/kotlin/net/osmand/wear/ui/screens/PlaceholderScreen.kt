package net.osmand.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/** Stands in for the screens delivered by later stages, so the nav graph is walkable now. */
@Composable
fun PlaceholderScreen(route: String) {
	ScreenScaffold {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(text = route, textAlign = TextAlign.Center)
		}
	}
}
