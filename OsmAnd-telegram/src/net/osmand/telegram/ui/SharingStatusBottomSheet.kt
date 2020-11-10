package net.osmand.telegram.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
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
		val items = settings.sharingStatusChanges.toArray().reversed()
		for (i in items.indices) {
			val sharingStatus = items[i] as TelegramSettings.SharingStatus
			inflater.inflate(R.layout.item_with_three_text_lines, itemsCont, false).apply {
				val sharingStatusType = sharingStatus.statusType
				val time = sharingStatus.locationTime * 1000

				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getIcon(sharingStatusType.iconId, sharingStatusType.iconColorRes))
				findViewById<TextView>(R.id.title).text = sharingStatus.getTitle(app)
				findViewById<TextView>(R.id.status_change_time).text = OsmandFormatter.getFormattedTime(sharingStatus.statusChangeTime, false)
				findViewById<TextView>(R.id.last_location_line).text = sharingStatus.description

				if (sharingStatusType != TelegramSettings.SharingStatusType.INITIALIZING) {
					if ((sharingStatusType == TelegramSettings.SharingStatusType.SENDING && time <= 0)) {
						findViewById<TextView>(R.id.last_location_line_time).visibility = View.GONE
					} else {
						val descriptionTime = when {
							time > 0 -> OsmandFormatter.getFormattedTime(time, false)
							sharingStatusType == TelegramSettings.SharingStatusType.NO_GPS -> getString(R.string.not_found_yet)
							else -> getString(R.string.not_sent_yet)
						}
						findViewById<TextView>(R.id.last_location_line_time).text = descriptionTime
					}
				} else {
					findViewById<TextView>(R.id.last_location_line_time).visibility = View.GONE
				}

				findViewById<TextView>(R.id.re_send_location).apply {
					if (sharingStatusType.canResendLocation) {
						if (i == 0) {
							setOnClickListener {
								app.shareLocationHelper.updateNetworkType()
								app.settings.prepareForSharingNewMessages(sharingStatus.chatsIds)
								app.shareLocationHelper.checkAndSendBufferMessages()
								app.forceUpdateMyLocation()
								dismiss()
							}
						} else {
							setTextColor(ContextCompat.getColor(context!!, R.color.secondary_text_light))
						}
					} else {
						visibility = View.GONE
					}
				}
				if (i == items.size - 1) {
					findViewById<View>(R.id.bottom_divider).visibility = View.GONE
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
		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment): Boolean {
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