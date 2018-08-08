package net.osmand.telegram.ui

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.osmand.telegram.R

class SettingsDialogFragment : DialogFragment() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(android.support.v4.app.DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragement_settings_dialog, container)
	}

	companion object {

		private const val TAG = "SettingsDialogFragment"

		fun showInstance(fm: FragmentManager): Boolean {
			return try {
				SettingsDialogFragment().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
