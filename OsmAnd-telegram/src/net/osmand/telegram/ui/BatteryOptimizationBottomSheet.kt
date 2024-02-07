package net.osmand.telegram.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog

class BatteryOptimizationBottomSheet : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val log = PlatformUtil.getLog(BatteryOptimizationBottomSheet::class.java)

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext())

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_battery_optimization, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_later)
			setOnClickListener { dismiss() }
		}

		mainView.findViewById<TextView>(R.id.primary_btn).apply {
			setText(R.string.go_to_settings)
			setOnClickListener {
				if (Build.VERSION.SDK_INT >= 26) {
					val pkg = app.packageName
					val pm = app.getSystemService(PowerManager::class.java)
					if (pm != null) {
						val intent = if (!pm.isIgnoringBatteryOptimizations(pkg)) {
							Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$pkg"))
						} else {
							Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
						}
						if (intent.resolveActivity(app.packageManager) != null) {
							startActivity(intent)
						} else {
							log.error("No Intent available to handle action")
						}
					}
				}
				dismiss()
			}
		}

		return mainView
	}

	companion object {

		private const val TAG = "BatteryOptimizationBottomSheet"

		fun showInstance(fm: androidx.fragment.app.FragmentManager): Boolean {
			return try {
				BatteryOptimizationBottomSheet().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}