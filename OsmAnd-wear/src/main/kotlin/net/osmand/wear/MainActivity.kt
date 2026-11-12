package net.osmand.wear

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch

import net.osmand.wear.data.PhoneConnector
import net.osmand.wear.ui.WearApp

class MainActivity : ComponentActivity() {

	private lateinit var connector: PhoneConnector

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		connector = PhoneConnector(applicationContext)
		setContent {
			WearApp(connector)
		}
	}

	override fun onResume() {
		super.onResume()
		// Re-run the handshake on every resume: the watch may have been out of range while
		// the app sat in the background, and the cached snapshot is then stale.
		lifecycleScope.launch { connector.refresh() }
	}
}
