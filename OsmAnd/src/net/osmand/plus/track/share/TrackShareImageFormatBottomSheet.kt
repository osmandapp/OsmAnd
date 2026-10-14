package net.osmand.plus.track.share

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.MenuBottomSheetDialogFragment
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem
import net.osmand.plus.track.share.TrackShareImage.Format
import net.osmand.plus.utils.AndroidUtils

/** The shape of the picture "Share as image" makes: a 4:5 card, a square or a 9:16 phone screen. */
class TrackShareImageFormatBottomSheet : MenuBottomSheetDialogFragment() {

	fun interface FormatListener {
		fun onFormatSelected(format: Format)
	}

	override fun createMenuItems(savedInstanceState: Bundle?) {
		items.add(TitleItem(getString(R.string.share_as_image)))
		addItem(Format.CARD, R.string.share_image_card, R.string.share_image_card_descr, R.drawable.ic_action_photo_dark)
		addItem(Format.SQUARE, R.string.share_image_square, R.string.share_image_square_descr, R.drawable.ic_action_picture_size)
		addItem(Format.SCREENSHOT, R.string.share_image_screenshot, R.string.share_image_screenshot_descr, R.drawable.ic_action_phone)
	}

	private fun addItem(format: Format, titleId: Int, descriptionId: Int, iconId: Int) {
		items.add(BottomSheetItemWithDescription.Builder()
			.setDescription(getString(descriptionId))
			.setTitle(getString(titleId))
			.setIcon(getActiveIcon(iconId))
			.setLayoutId(R.layout.bottom_sheet_item_with_descr_active)
			.setOnClickListener {
				(targetFragment as? FormatListener)?.onFormatSelected(format)
				dismiss()
			}
			.create())
	}

	override fun getDismissButtonTextId(): Int = R.string.shared_string_cancel

	companion object {
		private val TAG: String = TrackShareImageFormatBottomSheet::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager, target: Fragment) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = TrackShareImageFormatBottomSheet()
				fragment.setTargetFragment(target, 0)
				fragment.show(manager, TAG)
			}
		}
	}
}
