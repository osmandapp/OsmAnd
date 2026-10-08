package net.osmand.plus.activities

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import java.text.MessageFormat
import java.util.Date

/** Asks before a build is downloaded. A dialog fragment, so a rotation keeps it open. */
class BuildDownloadDialog : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val app = requireActivity().application as OsmandApplication
		val build = readBuild(requireArguments())
		val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.APP)
		val context = ContextThemeWrapper(requireContext(),
			if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme)
		val date = build.date?.let { AndroidUtils.formatDateTime(app, it.time) } ?: ""
		return MaterialAlertDialogBuilder(context)
			.setMessage(MessageFormat.format(getString(R.string.install_selected_build),
				build.tag, date, build.size))
			.setPositiveButton(R.string.shared_string_download) { _, _ ->
				BuildsManager.getInstance(app).startDownload(build)
			}
			.setNegativeButton(R.string.shared_string_cancel, null)
			.create()
	}

	companion object {

		private val TAG = BuildDownloadDialog::class.java.name
		private const val KEY_PATH = "path"
		private const val KEY_TAG = "tag"
		private const val KEY_SIZE = "size"
		private const val KEY_DATE = "date"

		@JvmStatic
		fun show(manager: FragmentManager, build: OsmAndBuild) {
			if (!AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				return
			}
			val dialog = BuildDownloadDialog()
			dialog.arguments = Bundle().apply {
				putString(KEY_PATH, build.path)
				putString(KEY_TAG, build.tag)
				putString(KEY_SIZE, build.size)
				build.date?.let { putLong(KEY_DATE, it.time) }
			}
			dialog.show(manager, TAG)
		}

		private fun readBuild(args: Bundle): OsmAndBuild {
			val date = if (args.containsKey(KEY_DATE)) Date(args.getLong(KEY_DATE)) else null
			return OsmAndBuild(args.getString(KEY_PATH), args.getString(KEY_SIZE), date,
				args.getString(KEY_TAG))
		}
	}
}
