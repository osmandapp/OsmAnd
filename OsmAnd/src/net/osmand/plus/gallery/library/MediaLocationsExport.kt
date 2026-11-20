package net.osmand.plus.gallery.library

import androidx.fragment.app.FragmentActivity
import net.osmand.PlatformUtil
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.Version
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.gallery.data.MediaMetadataListener
import net.osmand.plus.shared.SharedUtil
import net.osmand.plus.track.helpers.GpxUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.library.MediaFormatting
import java.io.File
import java.lang.ref.WeakReference

/**
 * Exports the locations of Media tab files to a GPX file and shares it: one waypoint per file
 * that has coordinates, files without coordinates are skipped. Replaces the "Locations" item of
 * the old A/V notes Share mode.
 */
class MediaLocationsExport(private val app: OsmandApplication) {

	fun share(activity: FragmentActivity, entries: List<MediaLibraryEntry>) {
		if (entries.isEmpty()) return
		val activityRef = WeakReference(activity)
		val metadata = app.galleryHelper.metadataRepository
		// Coordinates of photos and videos come from metadata that the Media tab reads lazily
		metadata.request(entries.map { it.mediaItem }, object : MediaMetadataListener {
			override fun onMetadataLoaded(item: MediaItem, metadata: GalleryMediaMetadata) = Unit

			override fun onBatchFinished() {
				val loaded = entries.map { it.copy(metadata = metadata.getCached(it.mediaItem) ?: it.metadata) }
				write(activityRef, loaded)
			}
		})
	}

	private fun write(activityRef: WeakReference<FragmentActivity>, entries: List<MediaLibraryEntry>) {
		app.taskManager.runInBackground(object : OsmAndTaskRunnable<Void, Void, File?>() {
			override fun doInBackground(vararg params: Void?): File? {
				val points = entries.mapNotNull(::createPoint)
				if (points.isEmpty()) return null
				val gpxFile = GpxFile(Version.getFullVersion(app))
				gpxFile.addPoints(points)
				val file = File(app.cacheDir, "$SHARE_DIR/$FILE_NAME")
				file.parentFile?.mkdirs()
				val error = SharedUtil.writeGpxFile(file, gpxFile)
				if (error != null) {
					LOG.error("Unable to write media locations", error)
					return null
				}
				return file
			}

			override fun onPostExecute(file: File?) {
				val activity = activityRef.get()
				if (file == null) {
					app.showShortToastMessage(R.string.media_locations_export_empty)
				} else if (AndroidUtils.isActivityNotDestroyed(activity)) {
					GpxUiHelper.shareGpx(app, activity!!, file)
				}
			}
		})
	}

	private fun createPoint(entry: MediaLibraryEntry): WptPt? {
		val location = entry.location ?: return null
		if (!location.latitude.isFinite() || !location.longitude.isFinite()) return null
		val type = typeName(entry.type)
		return WptPt().apply {
			lat = location.latitude
			lon = location.longitude
			name = entry.title
			link = Link(entry.href)
			(entry.dateMs ?: entry.lastModifiedMs)?.takeIf { it > 0 }?.let { time = it }
			category = type
			desc = entry.durationMs?.takeIf { entry.type != MediaType.PHOTO && it > 0 }
				?.let { "$type ${MediaFormatting.duration(it)}" } ?: type
		}
	}

	private fun typeName(type: MediaType) = app.getString(when (type) {
		MediaType.PHOTO -> R.string.shared_string_photo
		MediaType.VIDEO -> R.string.shared_string_video
		MediaType.AUDIO -> R.string.shared_string_audio
		else -> R.string.shared_string_media
	})

	companion object {
		private val LOG = PlatformUtil.getLog(MediaLocationsExport::class.java)
		private const val SHARE_DIR = "share"
		private const val FILE_NAME = "media_locations.gpx"
	}
}
