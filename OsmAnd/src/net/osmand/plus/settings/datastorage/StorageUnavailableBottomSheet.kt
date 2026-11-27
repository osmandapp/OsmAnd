package net.osmand.plus.settings.datastorage

import android.content.DialogInterface
import android.os.Bundle
import android.text.BidiFormatter
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.activities.RestartActivity
import net.osmand.plus.base.MenuBottomSheetDialogFragment
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem
import net.osmand.plus.utils.AndroidUtils

class StorageUnavailableBottomSheet : MenuBottomSheetDialogFragment() {

	override fun createMenuItems(savedInstanceState: Bundle?) {
		val directory = app.unavailableStorageDirectoryAtStartup ?: return
		items.add(TitleItem(getString(R.string.storage_unavailable_title)))
		val path = BidiFormatter.getInstance().unicodeWrap(directory.absolutePath)
		items.add(LongDescriptionItem(getString(R.string.storage_unavailable_description, path)))
	}

	override fun onStart() {
		super.onStart()
		if (app.unavailableStorageDirectoryAtStartup == null) {
			dismissAllowingStateLoss()
		}
	}

	override fun getDismissButtonTextId() = R.string.shared_string_continue

	override fun onDismissButtonClickAction() {
		acknowledgeForSession()
	}

	override fun onCancel(dialog: DialogInterface) {
		acknowledgeForSession()
		super.onCancel(dialog)
	}

	override fun getRightBottomButtonTextId() = R.string.restart_now

	override fun onRightBottomButtonClick() {
		callActivity(RestartActivity::doRestartSilent)
	}

	companion object {
		private val TAG = StorageUnavailableBottomSheet::class.java.simpleName
		private var acknowledgedForSession = false

		@JvmStatic
		fun acknowledgeForSession() {
			acknowledgedForSession = true
		}

		@JvmStatic
		fun showIfNeeded(manager: FragmentManager) {
			if (!acknowledgedForSession && AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
				StorageUnavailableBottomSheet().show(manager, TAG)
			}
		}
	}
}
