package net.osmand.plus.myplaces.favorites.dialogs

import android.graphics.Typeface
import android.os.AsyncTask
import android.text.Spanned
import androidx.fragment.app.FragmentActivity
import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.mapcontextmenu.other.ShareMenu.NativeShareDialogBuilder
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener
import net.osmand.plus.settings.backend.backup.exporttype.AttachedMediaExportType
import net.osmand.plus.settings.backend.backup.items.AttachedMediaSettingsItem
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.FileUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.UUID

class ShareFavoritesController(
	app: OsmandApplication,
	private val group: FavoriteGroup
) : BaseDialogController(app) {

	enum class State {
		READY,
		PREPARING
	}

	private var preparationTask: PrepareShareDataTask? = null
	private var gpxFile: File? = null
	private var pointsDescription: Spanned? = null
	private var exportItems: List<SettingsItem> = emptyList()
	private var missingMedia = false
	private val shareSessionId = UUID.randomUUID().toString()
	private val gpxSessionDirectory = File(File(app.cacheDir, "share"), shareSessionId)
	private val osfSessionDirectory = File(File(FileUtils.getTempDir(app), "share_favorites"), shareSessionId)
	private var preparationRunning = false
	private var osfExportRunning = false
	private var dialogResumed = false
	private var abandoned = false
	private var shareRequested = false
	private var pendingShare: PendingShare = PendingShare.None
	private var actionInProgress = false

	var state: State = State.PREPARING
		private set

	var pointsOnlyDetails: String = ""
		private set

	var pointsAndMediaDetails: String = ""
		private set

	val canSharePointsAndMedia: Boolean
		get() = exportItems.isNotEmpty()

	override fun getProcessId(): String = PROCESS_ID

	val title: String
		get() = getString(R.string.shared_string_share)

	val description: CharSequence
		get() {
			val folderName = group.getDisplayName(app)
			val text = getString(R.string.share_favorites_with_media_description, folderName)
			return UiUtilities.createSpannableString(text, Typeface.BOLD, folderName)
		}

	fun startPreparing() {
		if (preparationTask != null || abandoned) {
			return
		}
		val task = PrepareShareDataTask()
		preparationTask = task
		preparationRunning = true
		try {
			OsmAndTaskManager.executeTask(task)
		} catch (e: RuntimeException) {
			preparationTask = null
			preparationRunning = false
			cleanupSessionIfPossible()
			LOG.error("Failed to start Favorites share preparation", e)
			app.showToastMessage(R.string.share_favorites_preparation_failed)
			dialogManager.askDismissDialog(PROCESS_ID)
		}
	}

	fun onPointsOnlyClicked() {
		if (state != State.READY || abandoned || actionInProgress) {
			return
		}
		val file = gpxFile ?: return
		val description = pointsDescription ?: return
		actionInProgress = true
		pendingShare = PendingShare.Gpx(file, description)
		tryLaunchPendingShare()
	}

	fun onPointsAndMediaClicked() {
		if (state != State.READY || abandoned || actionInProgress || exportItems.isEmpty()) {
			return
		}
		actionInProgress = true
		if (missingMedia) {
			app.showToastMessage(R.string.share_favorites_missing_media_warning)
		}
		state = State.PREPARING
		notifyUiChanged()

		try {
			val directory = getOrCreateDirectory(osfSessionDirectory)
			val fileName = gpxFile?.nameWithoutExtension
			if (fileName == null) {
				returnToReady()
				return
			}
			osfExportRunning = true
			app.fileSettingsHelper.exportSettings(
				directory,
				fileName,
				createExportListener(),
				exportItems,
				true
			)
		} catch (e: RuntimeException) {
			osfExportRunning = false
			cleanupOsfSession()
			LOG.error("Failed to start Favorites OSF preparation", e)
			app.showToastMessage(R.string.share_favorites_preparation_failed)
			returnToReady()
		}
	}

	fun onDialogResumed() {
		dialogResumed = true
		tryLaunchPendingShare()
	}

	fun onDialogPaused() {
		dialogResumed = false
	}

	fun onDismissRequested() {
		abandon()
	}

	override fun finishProcessIfNeeded(activity: FragmentActivity?): Boolean {
		val finished = super.finishProcessIfNeeded(activity)
		if (finished) {
			abandon()
		}
		return finished
	}

	private fun abandon() {
		if (abandoned) {
			return
		}
		abandoned = true
		dialogResumed = false
		pendingShare = PendingShare.None
		preparationTask?.cancel(true)
		cleanupSessionIfPossible()
	}

	private fun createExportListener(): SettingsExportListener {
		return object : SettingsExportListener {
			override fun onSettingsExportFinished(file: File, succeed: Boolean) {
				osfExportRunning = false
				if (abandoned) {
					cleanupSessionIfPossible()
					return
				}
				if (succeed && file.isFile) {
					pendingShare = PendingShare.Osf(file)
					tryLaunchPendingShare()
				} else {
					cleanupOsfSession()
					LOG.error("Failed to prepare Favorites OSF: ${file.absolutePath}")
					app.showToastMessage(R.string.share_favorites_preparation_failed)
					returnToReady()
				}
			}
		}
	}

	private fun tryLaunchPendingShare() {
		if (!dialogResumed || abandoned || shareRequested) {
			return
		}
		val pendingShare = pendingShare
		val activity = activity ?: return
		val file = when (pendingShare) {
			is PendingShare.Gpx -> pendingShare.file
			is PendingShare.Osf -> pendingShare.file
			PendingShare.None -> return
		}
		if (!file.isFile) {
			handleShareFailure(pendingShare, null)
			return
		}
		try {
			when (pendingShare) {
				is PendingShare.Gpx -> ShareFavoritesAsyncTask.shareFavorites(
					app,
					activity,
					file,
					pendingShare.description
				)
				is PendingShare.Osf -> NativeShareDialogBuilder()
					.addFileWithSaveAction(file, app, activity, false)
					.setChooserTitle(getString(R.string.shared_string_share))
					.setExtraStream(AndroidUtils.getUriForFile(app, file))
					.setExtraSubject(file.name)
					.build(app)
				PendingShare.None -> Unit
			}
			shareRequested = true
			this.pendingShare = PendingShare.None
			dialogManager.askDismissDialog(PROCESS_ID)
		} catch (e: RuntimeException) {
			handleShareFailure(pendingShare, e)
		}
	}

	private fun handleShareFailure(pendingShare: PendingShare, cause: RuntimeException?) {
		this.pendingShare = PendingShare.None
		val file = when (pendingShare) {
			is PendingShare.Gpx -> pendingShare.file
			is PendingShare.Osf -> pendingShare.file
			PendingShare.None -> return
		}
		if (cause != null) {
			LOG.error("Failed to share Favorites file: ${file.absolutePath}", cause)
		} else {
			LOG.error("Favorites file is missing before sharing: ${file.absolutePath}")
		}
		app.showToastMessage(R.string.share_favorites_share_failed)
		when (pendingShare) {
			is PendingShare.Osf -> cleanupOsfSession()
			is PendingShare.Gpx -> if (cause == null) {
				cleanupSession()
			}
			PendingShare.None -> Unit
		}
		returnToReady()
	}

	private fun getOrCreateDirectory(directory: File): File {
		if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
			throw IllegalStateException("Could not create share directory: ${directory.absolutePath}")
		}
		return directory
	}

	private fun cleanupSessionIfPossible() {
		if (!shareRequested && !preparationRunning && !osfExportRunning) {
			cleanupSession()
		}
	}

	private fun cleanupSession() {
		Algorithms.removeAllFiles(gpxSessionDirectory)
		Algorithms.removeAllFiles(osfSessionDirectory)
	}

	private fun cleanupOsfSession() {
		Algorithms.removeAllFiles(osfSessionDirectory)
	}

	private fun returnToReady() {
		actionInProgress = false
		state = State.READY
		notifyUiChanged()
	}

	private fun notifyUiChanged() {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID)
	}

	private inner class PrepareShareDataTask : AsyncTask<Void, Void, PreparationResult>() {

		private var temporaryGpxFile: File? = null

		override fun doInBackground(vararg params: Void?): PreparationResult {
			return try {
				prepareShareData()
			} catch (e: Exception) {
				temporaryGpxFile?.delete()
				PreparationResult.Error(e)
			}
		}

		private fun prepareShareData(): PreparationResult {
			val groups = Collections.singletonList(group)
			val originalDestination = ShareFavoritesAsyncTask.createDestinationFile(app, groups)
			val destination = File(
				getOrCreateDirectory(gpxSessionDirectory),
				originalDestination.name
			)
			temporaryGpxFile = destination
			val error = app.favoritesHelper.fileHelper.saveFile(groups, destination)
			if (error != null || !destination.isFile) {
				destination.delete()
				return PreparationResult.Error(error)
			}
			if (isCancelled) {
				return PreparationResult.Cancelled
			}
			val description = ShareFavoritesAsyncTask.buildPointsDescription(app, groups)
			if (isCancelled) {
				return PreparationResult.Cancelled
			}
			return prepareMediaShareData(groups, destination, description)
		}

		private fun prepareMediaShareData(
			groups: List<FavoriteGroup>,
			destination: File,
			description: Spanned
		): PreparationResult {
			try {
				val dataHelper = AttachedMediaDataHelper(app)
				val links = dataHelper.collectMediaLinks(groups)
				val mediaItems = AttachedMediaExportType.collectSettingsItems(app, groups)
				if (isCancelled) {
					return PreparationResult.Cancelled
				}
				if (mediaItems.isEmpty()) {
					return PreparationResult.GpxOnly(destination, description)
				}

				val data = ArrayList<Any>(mediaItems.size + 1)
				data.add(group)
				data.addAll(mediaItems)
				val items = app.fileSettingsHelper.prepareSettingsItems(data, emptyList(), true)
				AttachedMediaExportType.processSettingsItems(app, groups, items)
				if (isCancelled) {
					return PreparationResult.Cancelled
				}

				val packedMedia = items.filterIsInstance<AttachedMediaSettingsItem>()
				if (packedMedia.isEmpty()) {
					return PreparationResult.GpxOnly(destination, description)
				}
				val packedHrefs = packedMedia.flatMapTo(hashSetOf()) { item -> item.hrefKeys }
				val hasMissingMedia = links.any { link ->
					val href = link.href?.trim()
					!href.isNullOrEmpty() && !isRemoteHref(href) && href !in packedHrefs
				}
				val totalSize = destination.length() + packedMedia.sumOf { item -> item.size }
				if (isCancelled) {
					return PreparationResult.Cancelled
				}
				return PreparationResult.Ready(
					destination,
					description,
					items,
					packedMedia.size + 1,
					totalSize,
					hasMissingMedia
				)
			} catch (e: Exception) {
				if (isCancelled) {
					return PreparationResult.Cancelled
				}
				LOG.error(
					"Failed to prepare attached media for Favorites; falling back to GPX: ${destination.absolutePath}",
					e
				)
				return PreparationResult.GpxOnly(destination, description)
			}
		}

		override fun onCancelled(result: PreparationResult?) {
			temporaryGpxFile?.delete()
			preparationTask = null
			preparationRunning = false
			cleanupSessionIfPossible()
		}

		override fun onPostExecute(result: PreparationResult) {
			preparationTask = null
			preparationRunning = false
			if (abandoned) {
				cleanupSessionIfPossible()
				return
			}
			when (result) {
				is PreparationResult.Ready -> applyReadyResult(result)
				is PreparationResult.GpxOnly -> applyGpxOnlyResult(result)
				is PreparationResult.Error -> {
					result.cause?.let { LOG.error("Failed to prepare Favorites share data", it) }
					cleanupSession()
					app.showToastMessage(R.string.share_favorites_preparation_failed)
					dialogManager.askDismissDialog(PROCESS_ID)
				}
				PreparationResult.Cancelled -> cleanupSessionIfPossible()
			}
		}
	}

	private fun applyGpxOnlyResult(result: PreparationResult.GpxOnly) {
		applyGpxResult(result.gpxFile, result.pointsDescription)
		exportItems = emptyList()
		missingMedia = false
		state = State.READY
		notifyUiChanged()
		pendingShare = PendingShare.Gpx(result.gpxFile, result.pointsDescription)
		tryLaunchPendingShare()
	}

	private fun applyReadyResult(result: PreparationResult.Ready) {
		applyGpxResult(result.gpxFile, result.pointsDescription)
		exportItems = result.exportItems
		missingMedia = result.missingMedia

		val filesCount = app.resources.getQuantityString(
			R.plurals.files_count,
			result.fileCount,
			result.fileCount
		)
		pointsAndMediaDetails = getString(
			R.string.ltr_or_rtl_triple_combine_via_bold_point,
			getString(R.string.shared_string_archive),
			filesCount,
			AndroidUtils.formatSize(app, result.totalSourceSize)
		)
		state = State.READY
		notifyUiChanged()
	}

	private fun applyGpxResult(gpxFile: File, pointsDescription: Spanned) {
		this.gpxFile = gpxFile
		this.pointsDescription = pointsDescription
		val extension = gpxFile.extension
			.ifEmpty { IndexConstants.GPX_FILE_EXT.removePrefix(".") }
			.uppercase(Locale.ROOT)
		pointsOnlyDetails = getString(
			R.string.ltr_or_rtl_combine_via_bold_point,
			extension,
			AndroidUtils.formatSize(app, gpxFile.length())
		)
	}

	private sealed interface PreparationResult {
		data class GpxOnly(
			val gpxFile: File,
			val pointsDescription: Spanned
		) : PreparationResult

		data class Ready(
			val gpxFile: File,
			val pointsDescription: Spanned,
			val exportItems: List<SettingsItem>,
			val fileCount: Int,
			val totalSourceSize: Long,
			val missingMedia: Boolean
		) : PreparationResult

		data class Error(val cause: Exception?) : PreparationResult
		data object Cancelled : PreparationResult
	}

	private sealed interface PendingShare {
		data object None : PendingShare

		data class Gpx(
			val file: File,
			val description: Spanned
		) : PendingShare

		data class Osf(val file: File) : PendingShare
	}

	enum class ShareHandlingResult {
		FALLBACK_TO_GPX,
		MEDIA_FLOW_STARTED,
		ALREADY_IN_PROGRESS,
		CANNOT_SHOW
	}

	companion object {
		const val PROCESS_ID = "share_favorites_with_media"
		private val LOG = PlatformUtil.getLog(ShareFavoritesController::class.java)

		@JvmStatic
		fun getExistedInstance(app: OsmandApplication): ShareFavoritesController? {
			return app.dialogManager.findController(PROCESS_ID) as? ShareFavoritesController
		}

		@JvmStatic
		fun handleShareRequest(
			activity: FragmentActivity,
			group: FavoriteGroup
		): ShareHandlingResult {
			val app = activity.application as OsmandApplication
			if (AttachedMediaDataHelper(app).collectMediaLinks(listOf(group)).isEmpty()) {
				return ShareHandlingResult.FALLBACK_TO_GPX
			}
			val manager = activity.supportFragmentManager
			if (getExistedInstance(app) != null
					|| manager.findFragmentByTag(ShareFavoritesBottomSheet::class.java.simpleName) != null) {
				return ShareHandlingResult.ALREADY_IN_PROGRESS
			}
			if (!ShareFavoritesBottomSheet.canBeAdded(manager)) {
				return ShareHandlingResult.CANNOT_SHOW
			}
			val controller = ShareFavoritesController(app, group)
			app.dialogManager.register(PROCESS_ID, controller)
			ShareFavoritesBottomSheet.showInstance(manager)
			controller.startPreparing()
			return ShareHandlingResult.MEDIA_FLOW_STARTED
		}

		private fun isRemoteHref(href: String): Boolean {
			return href.startsWith("http://", ignoreCase = true)
					|| href.startsWith("https://", ignoreCase = true)
		}
	}
}
