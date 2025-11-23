package net.osmand.plus.plugins.astro

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class StarMapFragment : BaseFullScreenDialogFragment() {
	companion object {
		val TAG: String = StarMapFragment::class.java.simpleName

		fun showInstance(mapActivity: MapActivity) {
			val manager: FragmentManager = mapActivity.supportFragmentManager
			manager.findFragmentByTag(TAG)?.let { foundFragment ->
				(foundFragment as StarMapFragment).dialog?.dismiss()
			}
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = StarMapFragment()
				fragment.show(manager, TAG)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

	override fun getThemeId(): Int {
		return if (nightMode) R.style.OsmandDarkTheme else R.style.OsmandLightTheme_LightStatusBar
	}

	override fun getStatusBarColorId(): Int {
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode)
	}

	override fun createDialog(savedInstanceState: Bundle?): Dialog {
		return object : Dialog(requireContext(), themeId) {
			override fun onBackPressed() {
				dismiss()
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = themedInflater.inflate(R.layout.fragment_star_map, container, false)
		view.setBackgroundColor(
			ContextCompat.getColor(
				app,
				if (nightMode) R.color.activity_background_color_dark else R.color.list_background_color_light))
		val backButton: ImageView = view.findViewById(R.id.back_button)
		backButton.setOnClickListener { dismiss() }
		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		updateUI()
	}

	private fun getMyLocation() = app.locationProvider.lastKnownLocation

	private fun updateUI() {

	}
}