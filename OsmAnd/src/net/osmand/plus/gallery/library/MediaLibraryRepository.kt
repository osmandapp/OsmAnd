package net.osmand.plus.gallery.library

import android.os.Handler
import android.os.Looper
import net.osmand.PlatformUtil
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.data.Cancellable
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.gallery.data.MediaMetadataListener
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.myplaces.favorites.FavoritesListener
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.shared.media.domain.MediaItem

class MediaLibraryRepository(private val app: OsmandApplication) {
	private val handler = Handler(Looper.getMainLooper())
	private val metadataRepository get() = app.galleryHelper.metadataRepository
	private val plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin::class.java)
	private val scanner = MediaLibraryScanner(app, plugin)
	private val listeners = linkedSetOf<(List<MediaLibraryEntry>) -> Unit>()
	private var scanning = false
	private var scanAgain = false
	private val refreshCallbacks = mutableListOf<() -> Unit>()
	private var metadataRequest: Cancellable? = null
	private var entries: List<MediaLibraryEntry> = emptyList()
	var hasSnapshot = false
		private set

	private val favoritesListener = object : FavoritesListener {
		override fun onFavoritesLoaded() = refresh()
		override fun onFavoriteDataUpdated(point: FavouritePoint) = refresh()
		override fun onSavingFavoritesFinished(success: Boolean) { if (success) refresh() }
	}
	private val recordingsListener = AudioVideoNotesPlugin.RecordingsListener { refresh(); false }
	private val attachmentsListener: (Set<GalleryKey>) -> Unit = { refresh() }
	private var metadataUpdatePending = false
	private val metadataUpdate = Runnable { metadataUpdatePending = false; publish() }

	fun subscribe(listener: (List<MediaLibraryEntry>) -> Unit) {
		val first = listeners.isEmpty()
		listeners.add(listener)
		if (first) {
			app.favoritesHelper.addListener(favoritesListener)
			plugin?.addRecordingsListener(recordingsListener)
			app.galleryHelper.addAttachedMediaChangeListener(attachmentsListener)
			refresh()
		}
		if (hasSnapshot) listener(snapshot())
	}

	fun unsubscribe(listener: (List<MediaLibraryEntry>) -> Unit) {
		if (!listeners.remove(listener) || listeners.isNotEmpty()) return
		app.favoritesHelper.removeListener(favoritesListener)
		plugin?.removeRecordingsListener(recordingsListener)
		app.galleryHelper.removeAttachedMediaChangeListener(attachmentsListener)
		cancelMetadataRequest()
	}

	fun getEntry(id: String): MediaLibraryEntry? = snapshot().firstOrNull { it.id == id }

	@JvmOverloads
	fun refresh(onRefreshed: (() -> Unit)? = null) {
		handler.post {
			onRefreshed?.let(refreshCallbacks::add)
			if (scanning) {
				scanAgain = true
			} else {
				scanning = true
				val callbacks = refreshCallbacks.toList()
				refreshCallbacks.clear()
				app.taskManager.runInBackground(object : OsmAndTaskRunnable<Void, Void, Result<List<MediaLibraryEntry>>>() {
					override fun doInBackground(vararg params: Void?) = runCatching { scanner.scan() }

					override fun onPostExecute(result: Result<List<MediaLibraryEntry>>) {
						scanning = false
						result.onSuccess {
							entries = it.toList()
							hasSnapshot = true
							publish(updateGallery = true)
							callbacks.forEach { callback -> callback() }
							requestMetadata()
						}.onFailure { LOG.warn("Unable to scan media library", it) }
						if (scanAgain) { scanAgain = false; refresh() }
					}
				})
			}
		}
	}

	private fun requestMetadata() {
		cancelMetadataRequest()
		if (listeners.isEmpty()) return
		metadataRequest = metadataRepository.request(entries.map { entry -> entry.mediaItem }, object : MediaMetadataListener {
			override fun onMetadataLoaded(item: MediaItem, metadata: GalleryMediaMetadata) {
				if (!metadataUpdatePending) {
					metadataUpdatePending = true
					handler.postDelayed(metadataUpdate, METADATA_UPDATE_DEBOUNCE_MS)
				}
			}
			override fun onBatchFinished() {
				handler.removeCallbacks(metadataUpdate)
				metadataUpdatePending = false
				publish()
			}
		})
	}

	private fun cancelMetadataRequest() {
		metadataRequest?.cancel()
		metadataRequest = null
		handler.removeCallbacks(metadataUpdate)
		metadataUpdatePending = false
	}

	private fun snapshot() = entries.map { it.copy(metadata = metadataRepository.getCached(it.mediaItem)) }

	private fun publish(updateGallery: Boolean = false) {
		val snapshot = snapshot()
		val media = snapshot.map { it.mediaItem }
		if (updateGallery) {
			app.galleryHelper.repository.put(GalleryKey.MediaLibrary, object : MediaHolder {
				override fun getItems() = media
			})
		}
		listeners.toList().forEach { it(snapshot) }
	}

	companion object {
		private val LOG = PlatformUtil.getLog(MediaLibraryRepository::class.java)
		private const val METADATA_UPDATE_DEBOUNCE_MS = 250L
	}
}
