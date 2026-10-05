package net.osmand.plus.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import net.osmand.plus.R
import net.osmand.plus.activities.BuildsManager.DownloadState
import net.osmand.plus.activities.BuildsManager.ListState
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow
import java.io.File
import java.util.Date

/**
 * OsmAnd builds of the Development plugin. All state lives in [BuildsManager], so the screen
 * only shows it: a rotation rebuilds the views and nothing else.
 */
class BuildsFragment : BaseMaterialFragment(), BuildsManager.Listener {

	private lateinit var manager: BuildsManager

	private lateinit var scrollView: View
	private lateinit var stateContainer: View
	private lateinit var stateProgress: View
	private lateinit var stateText: TextView
	private lateinit var stateButton: View
	private lateinit var downloadSection: View
	private lateinit var downloadTitle: TextView
	private lateinit var downloadSubtitle: TextView
	private lateinit var downloadProgress: LinearProgressIndicator
	private lateinit var mainAction: MaterialButton
	private lateinit var secondaryAction: MaterialButton
	private lateinit var latestGroup: ViewGroup
	private lateinit var archiveGroup: View
	private lateinit var archiveRow: SettingRow

	/* the installer is opened only for a download this screen has watched until its end */
	private var watchedRunning = false

	private val installLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			manager.onInstallResult(result.resultCode == Activity.RESULT_OK)
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		manager = BuildsManager.getInstance(osmandApp)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_builds, container, false)
		setupToolbar(view)

		scrollView = view.findViewById(R.id.scroll_view)
		stateContainer = view.findViewById(R.id.state_container)
		stateProgress = view.findViewById(R.id.state_progress)
		stateText = view.findViewById(R.id.state_text)
		stateButton = view.findViewById(R.id.state_button)
		stateButton.setOnClickListener { manager.loadBuilds(true) }

		downloadSection = view.findViewById(R.id.download_section)
		val downloadRow: View = view.findViewById(R.id.download_row)
		downloadTitle = downloadRow.findViewById(R.id.title)
		downloadSubtitle = downloadRow.findViewById(R.id.subtitle)
		downloadProgress = downloadRow.findViewById(R.id.progress)
		mainAction = downloadRow.findViewById(R.id.main_action)
		secondaryAction = downloadRow.findViewById(R.id.secondary_action)
		SegmentedList.apply(view.findViewById(R.id.download_group))

		latestGroup = view.findViewById(R.id.latest_group)
		archiveGroup = view.findViewById(R.id.archive_group)
		archiveRow = SettingRow(view.findViewById(R.id.archive_row))
		archiveRow.setIcon(R.drawable.ic_action_history,
			AndroidUtils.getColorFromAttr(view.context, R.attr.colorOnSurfaceVariant))
		archiveRow.setTitle(R.string.shared_string_archive)
		archiveRow.hideSwitch()
		archiveRow.setOnClickListener { showArchive() }
		SegmentedList.apply(archiveGroup as ViewGroup)
		return view
	}

	private fun setupToolbar(view: View) {
		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.setTitle(R.string.version_settings)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
		toolbar.menu.add(R.string.shared_string_refresh).apply {
			val icon = AppCompatResources.getDrawable(view.context, R.drawable.ic_action_update)?.mutate()
			icon?.setTint(AndroidUtils.getColorFromAttr(view.context, R.attr.colorOnSurfaceVariant))
			setIcon(icon)
			setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
			setOnMenuItemClickListener {
				manager.loadBuilds(true)
				true
			}
		}
	}

	override fun onResume() {
		super.onResume()
		manager.addListener(this)
		watchedRunning = manager.downloadState is DownloadState.Running
		manager.loadBuilds(false)
		manager.listState?.let { onListStateChanged(it) }
		onDownloadStateChanged(manager.downloadState)
	}

	override fun onPause() {
		super.onPause()
		manager.removeListener(this)
	}

	override fun onListStateChanged(state: ListState) {
		val loaded = state is ListState.Loaded
		scrollView.visibility = if (loaded) View.VISIBLE else View.GONE
		stateContainer.visibility = if (loaded) View.GONE else View.VISIBLE
		when (state) {
			is ListState.Loading -> {
				stateProgress.visibility = View.VISIBLE
				stateButton.visibility = View.GONE
				stateText.setText(R.string.loading_builds)
			}
			is ListState.Failed -> {
				stateProgress.visibility = View.GONE
				stateButton.visibility = View.VISIBLE
				stateText.text = state.message?.let {
					getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.loading_builds_failed), it)
				} ?: getString(R.string.loading_builds_failed)
			}
			is ListState.Loaded -> updateBuilds(state.builds)
		}
	}

	private fun updateBuilds(builds: List<OsmAndBuild>) {
		val latest = builds.filter { it.path.startsWith(BuildsManager.LATEST_BUILDS_FOLDER) }
		val archive = builds.filter { !it.path.startsWith(BuildsManager.LATEST_BUILDS_FOLDER) }
		val installedDate = manager.getInstalledDate()

		latestGroup.removeAllViews()
		val inflater = LayoutInflater.from(latestGroup.context)
		for (build in latest) {
			val rowView = inflater.inflate(R.layout.item_ui_setting_row, latestGroup, false)
			bindBuildRow(SettingRow(rowView), build, installedDate) {
				BuildDownloadDialog.show(parentFragmentManager, it)
			}
			latestGroup.addView(rowView)
		}
		SegmentedList.apply(latestGroup)
		val latestVisibility = if (latest.isEmpty()) View.GONE else View.VISIBLE
		latestGroup.visibility = latestVisibility
		requireView().findViewById<View>(R.id.latest_header).visibility = latestVisibility

		archiveGroup.visibility = if (archive.isEmpty()) View.GONE else View.VISIBLE
		val dates = archive.mapNotNull { it.date?.time }
		if (dates.isNotEmpty()) {
			archiveRow.setSubtitle(getString(R.string.ltr_or_rtl_combine_via_dash,
				AndroidUtils.formatDate(osmandApp, dates.min()),
				AndroidUtils.formatDate(osmandApp, dates.max())))
		}
	}

	override fun onDownloadStateChanged(state: DownloadState) {
		downloadSection.visibility = if (state is DownloadState.Idle) View.GONE else View.VISIBLE
		when (state) {
			is DownloadState.Idle -> watchedRunning = false
			is DownloadState.Running -> {
				watchedRunning = true
				bindDownload(state.build, state.downloaded, state.total, null)
				mainAction.setText(R.string.shared_string_pause)
				mainAction.setOnClickListener { manager.stopDownload() }
				secondaryAction.setText(R.string.shared_string_cancel)
				secondaryAction.setOnClickListener { manager.discardDownload() }
			}
			is DownloadState.Stopped -> {
				watchedRunning = false
				val status = state.error ?: getString(R.string.shared_string_paused)
				bindDownload(state.build, state.downloaded, state.total, status)
				mainAction.setText(R.string.shared_string_resume)
				mainAction.setOnClickListener { manager.startDownload(state.build) }
				secondaryAction.setText(R.string.shared_string_cancel)
				secondaryAction.setOnClickListener { manager.discardDownload() }
			}
			is DownloadState.Completed -> {
				downloadTitle.text = state.build.tag
				downloadSubtitle.text = getString(R.string.ltr_or_rtl_combine_via_bold_point,
					getString(R.string.shared_string_download_successful),
					AndroidUtils.formatSize(osmandApp, state.file.length()))
				downloadProgress.visibility = View.GONE
				mainAction.setText(R.string.shared_string_install)
				mainAction.setOnClickListener { install(state.build, state.file) }
				secondaryAction.setText(R.string.shared_string_delete)
				secondaryAction.setOnClickListener { manager.discardDownload() }
				if (watchedRunning) {
					watchedRunning = false
					install(state.build, state.file)
				}
			}
		}
	}

	private fun bindDownload(build: OsmAndBuild, downloaded: Long, total: Long, status: String?) {
		downloadTitle.text = build.tag
		val size = if (total > 0) {
			getString(R.string.ltr_or_rtl_combine_via_slash,
				AndroidUtils.formatSize(osmandApp, downloaded), AndroidUtils.formatSize(osmandApp, total))
		} else {
			AndroidUtils.formatSize(osmandApp, downloaded)
		}
		val progress = if (total > 0) {
			getString(R.string.ltr_or_rtl_combine_via_bold_point, size, "${downloaded * 100 / total}%")
		} else {
			size
		}
		downloadSubtitle.text = status?.let {
			getString(R.string.ltr_or_rtl_combine_via_bold_point, it, progress)
		} ?: progress
		downloadProgress.visibility = View.VISIBLE
		if (total > 0) {
			downloadProgress.isIndeterminate = false
			downloadProgress.setProgressCompat((downloaded * downloadProgress.max / total).toInt(), true)
		} else {
			downloadProgress.isIndeterminate = status == null
		}
	}

	private fun install(build: OsmAndBuild, file: File) {
		val context = requireContext()
		val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
		val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
			.setData(uri)
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			.putExtra(Intent.EXTRA_RETURN_RESULT, true)
		manager.onInstallStarted(build, file)
		installLauncher.launch(intent)
	}

	private fun showArchive() {
		val manager = parentFragmentManager
		val tag = BuildsArchiveFragment::class.java.name
		if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
			manager.beginTransaction()
				.replace(R.id.fragmentContainer, BuildsArchiveFragment(), tag)
				.addToBackStack(tag)
				.commitAllowingStateLoss()
		}
	}

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	companion object {

		/** One row of a build list: tag, date and size; newer than the installed build in primary. */
		@JvmStatic
		fun bindBuildRow(row: SettingRow, build: OsmAndBuild, installedDate: Date?,
		                 onClick: (OsmAndBuild) -> Unit) {
			val context = row.view.context
			row.setIcon(R.drawable.ic_action_apk,
				AndroidUtils.getColorFromAttr(context, R.attr.colorOnSurfaceVariant))
			row.setTitle(build.tag)
			row.hideSwitch()
			val date = build.date?.let { AndroidUtils.formatDateTime(context, it.time) }
			val size = build.size?.toFloatOrNull()?.let { context.getString(R.string.file_size_in_mb, it) }
			row.setSubtitle(if (date != null && size != null) {
				context.getString(R.string.ltr_or_rtl_combine_via_bold_point, date, size)
			} else {
				date ?: size
			})
			val newer = installedDate != null && build.date?.after(installedDate) == true
			row.setSubtitleColor(AndroidUtils.getColorFromAttr(context,
				if (newer) R.attr.colorPrimary else R.attr.colorOnSurfaceVariant))
			row.setOnClickListener { onClick(build) }
		}
	}
}
