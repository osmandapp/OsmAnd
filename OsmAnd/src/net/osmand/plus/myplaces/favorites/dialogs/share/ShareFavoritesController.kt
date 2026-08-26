package net.osmand.plus.myplaces.favorites.dialogs.share

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
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import java.io.File
import java.util.Locale

class ShareFavoritesController(
	app: OsmandApplication,
	private val groups: List<FavoriteGroup>,
	private val folderPath: String
) : BaseDialogController(app) {

	enum class DialogState {
		READY,
		PREPARING
	}

	private var preparationTask: PrepareShareDataTask? = null
	private var gpxFile: File? = null
	private var pointsDescription: Spanned? = null
	private var exportItems: List<SettingsItem> = emptyList()
	private var hasMissingMedia = false
	private val fileSession = ShareFavoritesFileSession(app)
	private val dataPreparer = ShareFavoritesDataPreparer(app)
	private var preparationRunning = false
	private var osfExportRunning = false
	private var dialogResumed = false
	private var abandoned = false
	private var shareRequested = false
	private var pendingShare: PendingShare = PendingShare.None
	private var actionInProgress = false

	var state: DialogState = DialogState.PREPARING
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
			val folderName = FavoriteFolderFormatter.getDisplayName(app, folderPath)
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
		if (state != DialogState.READY || abandoned || actionInProgress) {
			return
		}
		val file = gpxFile ?: return
		val description = pointsDescription ?: return
		actionInProgress = true
		pendingShare = PendingShare.Gpx(file, description)
		tryLaunchPendingShare()
	}

	fun onPointsAndMediaClicked() {
		if (state != DialogState.READY || abandoned || actionInProgress || exportItems.isEmpty()) {
			return
		}
		actionInProgress = true
		if (hasMissingMedia) {
			app.showToastMessage(R.string.share_favorites_missing_media_warning)
		}
		state = DialogState.PREPARING
		notifyUiChanged()

		try {
			val directory = fileSession.getOrCreateOsfDirectory()
			val fileName = gpxFile?.nameWithoutExtension
			if (fileName == null) {
				returnToReady()
				return
			}
			osfExportRunning = true
			app.fileSettingsHelper.exportSettings(directory, fileName,
				createExportListener(), exportItems, true
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

	private fun cleanupSessionIfPossible() {
		if (!shareRequested && !preparationRunning && !osfExportRunning) {
			cleanupSession()
		}
	}

	private fun cleanupSession() {
		fileSession.cleanup()
	}

	private fun cleanupOsfSession() {
		fileSession.cleanupOsf()
	}

	private fun returnToReady() {
		actionInProgress = false
		state = DialogState.READY
		notifyUiChanged()
	}

	private fun notifyUiChanged() {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID)
	}

	private inner class PrepareShareDataTask : AsyncTask<Void, Void, ShareFavoritesDataPreparer.PreparationResult>() {

		override fun doInBackground(vararg params: Void?): ShareFavoritesDataPreparer.PreparationResult {
			fileSession.cleanupExpiredGpxSessions()
			return dataPreparer.prepare(groups, folderPath, fileSession) { isCancelled }
		}

		override fun onCancelled(result: ShareFavoritesDataPreparer.PreparationResult?) {
			preparationTask = null
			preparationRunning = false
			cleanupSessionIfPossible()
		}

		override fun onPostExecute(result: ShareFavoritesDataPreparer.PreparationResult) {
			preparationTask = null
			preparationRunning = false
			handlePreparationResult(result)
		}
	}

	private fun handlePreparationResult(result: ShareFavoritesDataPreparer.PreparationResult) {
		if (abandoned) {
			cleanupSessionIfPossible()
			return
		}
		when (result) {
			is ShareFavoritesDataPreparer.PreparationResult.Ready -> applyReadyResult(result)
			is ShareFavoritesDataPreparer.PreparationResult.GpxOnly -> applyGpxOnlyResult(result)
			is ShareFavoritesDataPreparer.PreparationResult.Error -> {
				result.cause?.let { LOG.error("Failed to prepare Favorites share data", it) }
				cleanupSession()
				app.showToastMessage(R.string.share_favorites_preparation_failed)
				dialogManager.askDismissDialog(PROCESS_ID)
			}
			ShareFavoritesDataPreparer.PreparationResult.Cancelled -> cleanupSessionIfPossible()
		}
	}

	private fun applyGpxOnlyResult(result: ShareFavoritesDataPreparer.PreparationResult.GpxOnly) {
		applyGpxResult(result.gpxFile, result.pointsDescription)
		exportItems = emptyList()
		hasMissingMedia = false
		state = DialogState.READY
		notifyUiChanged()
		if (result.mediaPreparationFailed) {
			app.showToastMessage(R.string.share_favorites_media_preparation_failed)
		}
		pendingShare = PendingShare.Gpx(result.gpxFile, result.pointsDescription)
		tryLaunchPendingShare()
	}

	private fun applyReadyResult(result: ShareFavoritesDataPreparer.PreparationResult.Ready) {
		applyGpxResult(result.gpxFile, result.pointsDescription)
		exportItems = result.exportItems
		hasMissingMedia = result.missingMedia

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
		state = DialogState.READY
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

	private sealed interface PendingShare {
		data object None : PendingShare

		data class Gpx(
			val file: File,
			val description: Spanned
		) : PendingShare

		data class Osf(val file: File) : PendingShare
	}

	enum class ShareHandlingResult {
		GPX_FALLBACK_REQUIRED,
		MEDIA_FLOW_STARTED,
		SHARE_FLOW_ALREADY_ACTIVE,
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
			groups: List<FavoriteGroup>,
			folderPath: String
		): ShareHandlingResult {
			val app = activity.application as OsmandApplication
			if (AttachedMediaDataHelper(app).collectMediaLinks(groups).isEmpty()) {
				return ShareHandlingResult.GPX_FALLBACK_REQUIRED
			}
			val manager = activity.supportFragmentManager
			if (getExistedInstance(app) != null
					|| manager.findFragmentByTag(ShareFavoritesBottomSheet::class.java.simpleName) != null) {
				return ShareHandlingResult.SHARE_FLOW_ALREADY_ACTIVE
			}
			if (!ShareFavoritesBottomSheet.canBeAdded(manager)) {
				return ShareHandlingResult.CANNOT_SHOW
			}
			val controller = ShareFavoritesController(app, groups, folderPath)
			app.dialogManager.register(PROCESS_ID, controller)
			ShareFavoritesBottomSheet.showInstance(manager)
			controller.startPreparing()
			return ShareHandlingResult.MEDIA_FLOW_STARTED
		}
	}
}
