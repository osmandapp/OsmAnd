package net.osmand.telegram.ui

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog
import net.osmand.telegram.utils.OsmandFormatter

class SharingStatusBottomSheet : DialogFragment() {
	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication
	private val settings get() = app.settings
	private val uiUtils get() = app.uiUtils

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_sharing_status, container, false)
		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }
		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						targetFragment?.also { target ->
							target.onActivityResult(targetRequestCode, SHARING_STATUS_REQUEST_CODE, null)
						}
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		val itemsCont = mainView.findViewById<ViewGroup>(R.id.items_container)
		settings.sharingStatusChanges.reversed().forEach { sharingStatus ->
			inflater.inflate(R.layout.item_with_three_text_lines, itemsCont, false).apply {
				val sharingStatusType = sharingStatus.statusType
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getIcon(sharingStatusType.iconId, sharingStatusType.iconColorRes))
				findViewById<TextView>(R.id.title).text = sharingStatus.getDescription(app)
				findViewById<TextView>(R.id.status_change_time).text =
						OsmandFormatter.getFormattedTime(sharingStatus.statusChangeTime, false)
				val time = sharingStatus.locationTime
				findViewById<TextView>(R.id.last_location_line).text = getString(sharingStatusType.descriptionId)
				if (time > 0) {
					val sentTime = OsmandFormatter.getFormattedTime(time, false)
					findViewById<TextView>(R.id.last_location_line_time).text = sentTime
				} else {
					findViewById<TextView>(R.id.last_location_line_time).text = "-"
				}
				if (sharingStatusType.canResendLocation) {
					findViewById<TextView>(R.id.re_send_location).apply {
						setOnClickListener {
							app.forceUpdateMyLocation()
							dismiss()
						}
					}
				} else {
					findViewById<TextView>(R.id.re_send_location).visibility = View.GONE
				}
				itemsCont.addView(this)
			}
		}
		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_close)
			setOnClickListener {
				targetFragment?.also { target ->
					target.onActivityResult(targetRequestCode, SHARING_STATUS_REQUEST_CODE, null)
				}
				dismiss()
			}
		}
		return mainView
	}

	companion object {
		const val SHARING_STATUS_REQUEST_CODE = 5
		private const val TAG = "SharingStatusBottomSheet"
		fun showInstance(fm: FragmentManager, target: Fragment): Boolean {
			return try {
				SharingStatusBottomSheet().apply {
					setTargetFragment(target, SHARING_STATUS_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}