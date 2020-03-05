package net.osmand.telegram.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.ui.views.BottomSheetDialog

class ChooseOsmAndBottomSheet : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val settings get() = app.settings
	private val uiUtils get() = app.uiUtils

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_choose_osmand, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		val itemsCont = mainView.findViewById<ViewGroup>(R.id.items_container)
		for (appConn in TelegramSettings.AppConnect.getInstalledApps(requireContext())) {
			inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getIcon(appConn.iconId))
				findViewById<TextView>(R.id.title).text = appConn.title
				findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
				setOnClickListener {
					settings.updateAppToConnect(appConn.appPackage)
					targetFragment?.also { target ->
						target.onActivityResult(targetRequestCode, OSMAND_CHOSEN_REQUEST_CODE, null)
					}
					dismiss()
				}
				itemsCont.addView(this)
			}
		}

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		return mainView
	}

	companion object {

		const val OSMAND_CHOSEN_REQUEST_CODE = 0

		private const val TAG = "ChooseOsmAndBottomSheet"

		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment): Boolean {
			return try {
				ChooseOsmAndBottomSheet().apply {
					setTargetFragment(target, OSMAND_CHOSEN_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
