package net.osmand.plus.plugins.astronomy.views.contextmenu

import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.download.DownloadActivityType
import net.osmand.plus.download.IndexItem
import net.osmand.plus.helpers.FileNameTranslationHelper
import net.osmand.plus.inapp.InAppPurchaseUtils

class AstroKnowledgeBaseController(
	private val app: OsmandApplication
) {

	companion object {
		private const val KNOWLEDGE_BASE_FILE_NAME =
			FileNameTranslationHelper.STARS_ARTICLES + IndexConstants.STAR_MAP_INDEX_EXT
	}

	var requestedIndexesReload = false
		private set

	fun buildCardItem(): AstroKnowledgeCardItem? {
		if (isDownloaded()) {
			return null
		}
		val state = currentState() ?: return null
		val downloadItem = findDownloadItem()
		return AstroKnowledgeCardItem(
			state = state,
			buttonTitle = if (state == AstroKnowledgeCardState.DOWNLOAD) {
				buildButtonTitle(downloadItem)
			} else {
				app.getString(R.string.shared_string_get)
			},
			actionEnabled = state != AstroKnowledgeCardState.DOWNLOAD || findActiveDownload() == null
		)
	}

	fun currentState(): AstroKnowledgeCardState? {
		if (isDownloaded()) {
			return null
		}
		return if (hasAccess()) {
			AstroKnowledgeCardState.DOWNLOAD
		} else {
			AstroKnowledgeCardState.UPSELL
		}
	}

	fun hasAccess(): Boolean = InAppPurchaseUtils.isAstronomyAvailable(app)

	fun isDownloaded(): Boolean {
		return app.getAppPath(IndexConstants.ASTRO_DIR)
			.resolve(KNOWLEDGE_BASE_FILE_NAME)
			.exists()
	}

	fun findDownloadItem(): IndexItem? {
		return app.downloadThread.indexes
			.getIndexItems(listOf(DownloadActivityType.STAR_MAP_FILE))
			.firstOrNull(::isKnowledgeBaseIndexItem)
	}

	fun findActiveDownload(): IndexItem? {
		return app.downloadThread.currentDownloadingItems.firstOrNull(::isKnowledgeBaseIndexItem)
	}

	fun ensureIndexesLoaded() {
		val downloadThread = app.downloadThread
		val indexes = downloadThread.indexes
		if (requestedIndexesReload || indexes.isDownloadedFromInternet || indexes.downloadFromInternetFailed) {
			return
		}
		requestedIndexesReload = true
		downloadThread.runReloadIndexFilesSilent()
	}

	fun resetIndexesReloadFlag() {
		requestedIndexesReload = false
	}

	fun shouldRefreshAfterDownload(actionWasDisabled: Boolean): Boolean {
		return isDownloaded() || findActiveDownload() != null || actionWasDisabled
	}

	private fun buildButtonTitle(indexItem: IndexItem?): String {
		val downloadThread = app.downloadThread
		val activeDownloadItem = findActiveDownload()
		if (activeDownloadItem != null && downloadThread.currentDownloadingItem == activeDownloadItem && !activeDownloadItem.isDownloaded) {
			val progress = downloadThread.currentDownloadProgress.toInt()
			val mb = activeDownloadItem.archiveSizeMB
			return if (progress != -1) {
				app.getString(R.string.value_downloaded_of_mb, mb * progress / 100, mb)
			} else {
				app.getString(
					R.string.shared_string_downloading_formatted,
					activeDownloadItem.getSizeDescription(app)
				)
			}
		}
		if (activeDownloadItem != null) {
			return app.getString(
				R.string.shared_string_downloading_formatted,
				activeDownloadItem.getSizeDescription(app)
			)
		}
		return if (indexItem != null) {
			app.getString(
				R.string.ltr_or_rtl_combine_via_space,
				app.getString(R.string.shared_string_download),
				indexItem.getSizeDescription(app)
			)
		} else {
			app.getString(R.string.shared_string_download)
		}
	}

	private fun isKnowledgeBaseIndexItem(item: IndexItem?): Boolean {
		return item?.basename?.endsWith(FileNameTranslationHelper.STARS_ARTICLES, ignoreCase = true) == true
	}
}
