package net.osmand.plus.plugins.audionotes.library

import androidx.fragment.app.FragmentActivity
import android.view.View
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.controller.GalleryPresentationMapper
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.AttachmentLine
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.holders.MediaLibraryListViewHolder
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.PointImageUtils
import net.osmand.shared.data.KLatLon
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.library.MediaLibraryGrouping
import net.osmand.shared.media.library.MediaLibrarySortMode
import net.osmand.shared.media.library.MediaLibrarySorter
import net.osmand.shared.media.library.MediaLibraryStats
import net.osmand.shared.util.KMapUtils
import kotlin.math.roundToInt

class MediaLibraryController(app: OsmandApplication, private val plugin: AudioVideoNotesPlugin) :
	GalleryGridController(app, GalleryKey.MediaLibrary) {
	private val repository = app.galleryHelper.mediaLibraryRepository
	private val mapper = GalleryPresentationMapper(app, app.galleryHelper.metadataRepository)
	private var entries: List<MediaLibraryEntry> = emptyList()
	private var pendingEntries: List<MediaLibraryEntry>? = null
	private var restoredSelection: Set<String>? = null
	private val collapsedGroups = mutableSetOf<MediaType>()
	private val repositoryListener: (List<MediaLibraryEntry>) -> Unit = {
		if (isSelectionMode()) {
			pendingEntries = it
		} else {
			entries = it
			restoredSelection?.let { ids ->
				restoredSelection = null
				enterSelectionMode(null)
				selectionHelper.onItemsSelected(getMediaItems().filter { item -> item.id in ids }, true)
			}
			view?.updateItems()
			view?.updateSelection()
			view?.updateToolbar()
		}
	}
	val sortMode: GallerySortMode get() = plugin.MEDIA_LIBRARY_SORT_MODE.get()

	init { super.setDisplayMode(plugin.MEDIA_LIBRARY_DISPLAY_MODE.get()) }

	override fun getProcessId() = PROCESS_ID
	override fun getScreenTitle(): String = getString(R.string.shared_string_media)
	override fun isListModeSupported() = true
	override fun isSelectionModeSupported() = true
	override fun isGroupingSupported() = true
	override fun isGrouped(): Boolean = plugin.MEDIA_LIBRARY_GROUPED.get()
	override fun getMediaItems() = entries.map { it.mediaItem }

	fun selectedIds(): List<String> = restoredSelection?.toList() ?: getSelectedItems().map { it.id }
	fun restoreSelection(ids: List<String>) { restoredSelection = ids.toSet() }

	override fun exitSelectionMode() {
		super.exitSelectionMode()
		pendingEntries?.let {
			entries = it
			pendingEntries = null
			view?.updateItems()
		}
	}

	override fun onMediaItemMenuClicked(item: net.osmand.shared.media.domain.MediaItem, anchor: View) {
		if (isSelectionMode()) return
		val activity = view?.getActivity() ?: return
		val entry = entries.firstOrNull { it.id == item.id } ?: return
		MediaItemMenu.show(activity, entry, anchor, view?.isNightMode() == true, true, orderedIds())
	}

	private fun orderedIds() = getGalleryItems().filterIsInstance<GalleryItem.Media>().map { it.mediaItem.id }

	fun showSelectionMenu(anchor: View) {
		val activity = view?.getActivity() ?: return
		val ids = selectedIds().toSet()
		val selected = entries.filter { it.id in ids }
		MediaItemMenu.showSelection(activity, selected, anchor, view?.isNightMode() == true, ::exitSelectionMode)
	}

	override fun attach(view: IGalleryGridView) {
		super.attach(view)
		repository.subscribe(repositoryListener)
	}

	override fun detach() {
		repository.unsubscribe(repositoryListener)
		super.detach()
	}

	override fun setDisplayMode(mode: GalleryDisplayMode) {
		plugin.MEDIA_LIBRARY_DISPLAY_MODE.set(mode)
		super.setDisplayMode(mode)
	}

	fun toggleGrouping() {
		plugin.MEDIA_LIBRARY_GROUPED.set(!isGrouped())
		view?.updateSections()
	}

	override fun onGroupHeaderClicked(type: MediaType) {
		if (!collapsedGroups.add(type)) collapsedGroups.remove(type)
		view?.updateItems()
	}

	fun getCollapsedGroups(): List<String> = collapsedGroups.map { it.name }

	fun restoreCollapsedGroups(names: List<String>) {
		collapsedGroups.addAll(MediaType.entries.filter { it.name in names })
	}

	override fun onSortModeSelected(sortMode: GallerySortMode) {
		plugin.MEDIA_LIBRARY_SORT_MODE.set(sortMode)
		view?.updateItems()
	}

	override fun getSpanCount(isPortrait: Boolean): Int = if (isPortrait)
		plugin.MEDIA_LIBRARY_SPAN_COUNT.get().coerceIn(2, 5)
	else plugin.MEDIA_LIBRARY_SPAN_COUNT_LANDSCAPE.get().coerceIn(4, 8)

	override fun setSpanCount(isPortrait: Boolean, count: Int) {
		(if (isPortrait) plugin.MEDIA_LIBRARY_SPAN_COUNT else plugin.MEDIA_LIBRARY_SPAN_COUNT_LANDSCAPE).set(count)
	}

	override fun getSpanBounds(isPortrait: Boolean) = if (isPortrait) 2..5 else 4..8

	override fun resolveSpanResizableSize(viewWidth: Int?, spanCount: Int): Int {
		val padding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
		val gap = AndroidUtils.dpToPxF(app, 8f).roundToInt()
		val width = viewWidth ?: return super.resolveSpanResizableSize(null, spanCount)
		return ((width - 2 * padding - (spanCount - 1) * gap) / spanCount).coerceAtLeast(1)
	}

	private fun reference(): KLatLon {
		val location = app.locationProvider.lastKnownLocation
		val mapLocation = app.settings.lastKnownMapLocation
		return location?.let { KLatLon(it.latitude, it.longitude) }
			?: KLatLon(mapLocation.latitude, mapLocation.longitude)
	}

	override fun getGalleryItems(): List<GalleryItem> {
		if (!repository.hasSnapshot) return emptyList()
		if (entries.isEmpty()) return listOf(GalleryItem.NoMedia(
			titleResId = R.string.media_library_empty_title,
			descriptionResId = R.string.media_library_empty_descr,
			iconResId = R.drawable.ic_action_photo_album))
		val reference = reference()
		val sorted = entries.sortedWith(MediaLibrarySorter.comparator(MediaLibrarySortMode.valueOf(sortMode.name), reference))
		val nightMode = view?.isNightMode() ?: false
		fun toItem(entry: MediaLibraryEntry): GalleryItem.Media {
			val distance = entry.lat?.let { lat -> entry.lon?.let { lon ->
				OsmAndFormatter.getFormattedDistance(KMapUtils.getDistance(reference.latitude, reference.longitude, lat, lon).toFloat(), app)
			} }
			val attachment = entry.attachments.lastOrNull()?.let {
				val favorite = it.target as? FavouritePoint
				val icon = if (favorite != null) PointImageUtils.getFromPoint(app,
					app.favoritesHelper.getColorWithCategory(favorite, ColorUtilities.getColor(app, R.color.color_favorite)), false, favorite)
				else app.uiUtilities.getPaintedIcon(R.drawable.ic_action_polygom_dark, ColorUtilities.getDefaultIconColor(app, nightMode))
				AttachmentLine(icon, MediaLibraryGrouping.lastAttachedName(entry.attachments) { attachment -> attachment.name }.orEmpty(),
					entry.attachments.size - 1, if (favorite != null) AttachmentLine.Kind.FAVORITE else AttachmentLine.Kind.TRACK_POINT)
			}
			return GalleryItem.Media(entry.mediaItem, presentation = mapper.presentation(entry.mediaItem, sortMode, distance).copy(attachment = attachment))
		}
		return buildList {
			if (isGrouped()) {
				for (group in MediaLibraryGrouping.group(sorted)) {
					val collapsed = group.type in collapsedGroups
					add(GalleryItem.GroupHeader(group.type, group.count, collapsed))
					if (!collapsed) addAll(group.items.map(::toItem))
				}
			} else addAll(sorted.map(::toItem))
			add(GalleryItem.Spacer)
			add(GalleryItem.MediaStats(mapper.statsText(MediaLibraryStats.compute(entries)), sectionFooter = true))
		}
	}

	override fun createAdapter(mapActivity: FragmentActivity, viewWidth: Int?, nightMode: Boolean): GalleryGridAdapter =
		super.createAdapter(mapActivity, viewWidth, nightMode).apply {
			val inflater = UiUtilities.getInflater(mapActivity, nightMode)
			listRowFactory = { parent -> MediaLibraryListViewHolder(app,
				inflater.inflate(R.layout.media_library_list_item, parent, false), MediaProvider(app),
				::onMediaItemClicked, ::onMediaItemLongClicked, ::toggleSelection,
				app.galleryHelper.posterLoader, ::onMediaItemMenuClicked) }
			emptyRowFactory = { parent -> MediaLibraryEmptyHolder(inflater.inflate(R.layout.track_folder_empty_state, parent, false), app) }
		}

	companion object { const val PROCESS_ID = "media_library" }
}
