package net.osmand.plus.myplaces.favorites.dialogs.share

import android.text.Spanned
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.myplaces.favorites.FavoriteGroup
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask
import net.osmand.plus.settings.backend.backup.exporttype.AttachedMediaExportType
import net.osmand.plus.settings.backend.backup.items.AttachedMediaSettingsItem
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.util.Algorithms
import java.io.File

class ShareFavoritesDataPreparer(private val app: OsmandApplication) {

	fun prepare(
		groups: List<FavoriteGroup>,
		folderPath: String,
		fileSession: ShareFavoritesFileSession,
		isCancelled: () -> Boolean
	): PreparationResult {
		var destination: File? = null
		return try {
			val originalDestination = ShareFavoritesAsyncTask.createDestinationFile(app, groups, folderPath)
			val gpxFile = fileSession.createGpxDestination(originalDestination.name)
			destination = gpxFile
			val error = app.favoritesHelper.fileHelper.saveFile(groups, gpxFile)
			if (error != null || !gpxFile.isFile) {
				gpxFile.delete()
				PreparationResult.Error(error)
			} else if (isCancelled()) {
				PreparationResult.Cancelled
			} else {
				val description = ShareFavoritesAsyncTask.buildPointsDescription(app, groups)
				if (isCancelled()) {
					PreparationResult.Cancelled
				} else {
					prepareMediaShareData(groups, gpxFile, description, isCancelled)
				}
			}
		} catch (e: Exception) {
			destination?.delete()
			PreparationResult.Error(e)
		}
	}

	private fun prepareMediaShareData(
		groups: List<FavoriteGroup>,
		destination: File,
		description: Spanned,
		isCancelled: () -> Boolean
	): PreparationResult {
		try {
			val dataHelper = AttachedMediaDataHelper(app)
			val links = dataHelper.collectMediaLinks(groups)
			val mediaItems = AttachedMediaExportType.collectSettingsItems(app, groups)
			if (isCancelled()) {
				return PreparationResult.Cancelled
			}
			if (mediaItems.isEmpty()) {
				return PreparationResult.GpxOnly(destination, description)
			}

			val data = ArrayList<Any>(mediaItems.size + groups.size)
			data.addAll(groups)
			data.addAll(mediaItems)
			val items = app.fileSettingsHelper.prepareSettingsItems(data, emptyList(), true)
			if (isCancelled()) {
				return PreparationResult.Cancelled
			}

			val packedMedia = items.filterIsInstance<AttachedMediaSettingsItem>()
			if (packedMedia.isEmpty()) {
				return PreparationResult.GpxOnly(destination, description)
			}
			val packedHrefs = packedMedia.flatMapTo(hashSetOf()) { item -> item.hrefKeys }
			val hasMissingMedia = links.any { link ->
				val href = link.href?.trim()
				!href.isNullOrEmpty() && !Algorithms.isUrl(href) && href !in packedHrefs
			}
			val totalSize = destination.length() + packedMedia.sumOf { item -> item.size }
			if (isCancelled()) {
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
			if (isCancelled()) {
				return PreparationResult.Cancelled
			}
			LOG.error(
				"Failed to prepare attached media for Favorites; falling back to GPX: ${destination.absolutePath}",
				e
			)
			return PreparationResult.GpxOnly(destination, description,
				mediaPreparationFailed = true
			)
		}
	}

	sealed interface PreparationResult {
		data class GpxOnly(
			val gpxFile: File,
			val pointsDescription: Spanned,
			val mediaPreparationFailed: Boolean = false
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

	companion object {
		private val LOG = PlatformUtil.getLog(ShareFavoritesDataPreparer::class.java)
	}
}
