package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

abstract class OBDDevicesBaseFragment : BaseFullScreenFragment() {

	protected val vehicleMetricsPlugin =
		PluginsHelper.requirePlugin(VehicleMetricsPlugin::class.java)

	protected val externalDevicesPlugin =
		PluginsHelper.requirePlugin(ExternalSensorsPlugin::class.java)

	@get:LayoutRes
	protected abstract val layoutId: Int

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		updateNightMode()
		val view = themedInflater.inflate(layoutId, container, false)
		setupToolbar(view)
		setupUI(view)
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)
		return view
	}

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.status_bar_main_light
	}

	override fun isUsedOnMap(): Boolean {
		return true
	}

	protected open fun setupUI(view: View) {}
	protected open fun setupToolbar(view: View) {
		val appbar = view.findViewById<View>(R.id.appbar)
		ViewCompat.setElevation(appbar, elevation)
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode))
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { v: View? -> requireActivity().onBackPressed() }
	}

	protected val elevation: Float
		get() = 5.0f

	companion object {
		val TAG: String = OBDDevicesBaseFragment::class.java.simpleName
	}
}