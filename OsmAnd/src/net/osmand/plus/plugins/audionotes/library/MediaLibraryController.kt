package net.osmand.plus.plugins.audionotes.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.gallery.controller.GalleryPresentationMapper
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.library.MediaLibraryEntry
import net.osmand.plus.gallery.model.AttachmentLine
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.SectionGridGeometry
import net.osmand.plus.gallery.ui.holders.MediaLibraryListViewHolder
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.PointImageUtils
import net.osmand.shared.data.KLatLon
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.library.MediaLibraryGrouping
import net.osmand.shared.media.library.MediaLibrarySorter
import net.osmand.shared.media.library.MediaLibraryStats
import net.osmand.shared.util.KMapUtils

class MediaLibraryController(app: OsmandApplication, private val plugin: AudioVideoNotesPlugin) :
	GalleryGridController(app, GalleryKey.MediaLibrary) {
	private val repository = app.galleryHelper.mediaLibraryRepository
	private val mapper = GalleryPresentationMapper(app, app.galleryHelper.metadataRepository)
	private var entries: List<MediaLibraryEntry> = emptyList()
	private var entriesDeferredDuringSelection: List<MediaLibraryEntry>? = null
	private var selectionToRestore: Set<String>? = null
	private val collapsedGroups = mutableSetOf<MediaType>()
	private val emptyItem = GalleryItem.NoMedia(
		titleResId = R.string.media_library_empty_title,
		descriptionResId = R.string.media_library_empty_descr,
		iconResId = R.drawable.ic_action_photo_album)
	private val repositoryListener: (List<MediaLibraryEntry>) -> Unit = {
		if (isSelectionMode()) {
			entriesDeferredDuringSelection = it
		} else {
			entries = it
			selectionToRestore?.let { ids ->
				selectionToRestore = null
				enterSelectionMode(null)
				selectionHelper.onItemsSelected(getMediaItems().filter { item -> item.id in ids }, true)
			}
			view?.updateItems()
			view?.updateSelection()
			view?.updateToolbar()
		}
	}
	val sortMode: GallerySortMode get() = plugin.MEDIA_LIBRARY_SORT_MODE.get()

	init { applyDisplayMode(plugin.MEDIA_LIBRARY_DISPLAY_MODE.get()) }

	override fun getProcessId() = PROCESS_ID
	override fun getScreenTitle(): String = getString(R.string.shared_string_media)
	override fun isListModeSupported() = true
	override fun isSelectionModeSupported() = true
	override fun isGrouped(): Boolean = plugin.MEDIA_LIBRARY_GROUPED.get()
	override fun getMediaItems() = entries.map { it.mediaItem }

	fun selectedIds(): List<String> = selectionToRestore?.toList() ?: getSelectedItems().map { it.id }
	fun restoreSelection(ids: List<String>) { selectionToRestore = ids.toSet() }

	override fun exitSelectionMode() {
		super.exitSelectionMode()
		entriesDeferredDuringSelection?.let {
			entries = it
			entriesDeferredDuringSelection = null
			view?.updateItems()
		}
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val activity = view?.getActivity() ?: return
		GalleryPagerController.show(activity, key, mediaItem.id, orderedIds(), autoPlay = true)
	}

	override fun onMediaItemMenuClicked(item: MediaItem, anchor: View) {
		if (isSelectionMode()) return
		val activity = view?.getActivity() ?: return
		val entry = entries.firstOrNull { it.id == item.id } ?: return
		MediaItemMenu.show(activity, entry, anchor, view?.isNightMode() == true, fromViewer = false, orderedIds = orderedIds())
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
		applyDisplayMode(mode)
	}

	fun toggleGrouping() {
		plugin.MEDIA_LIBRARY_GROUPED.set(!isGrouped())
		view?.updateItems()
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

	private fun spanPreference(isPortrait: Boolean) =
		if (isPortrait) plugin.MEDIA_LIBRARY_SPAN_COUNT else plugin.MEDIA_LIBRARY_SPAN_COUNT_LANDSCAPE

	override fun getSpanCount(isPortrait: Boolean): Int = spanPreference(isPortrait).get().coerceIn(getSpanBounds(isPortrait))

	override fun setSpanCount(isPortrait: Boolean, count: Int) {
		spanPreference(isPortrait).set(count)
	}

	override fun resolveSpanResizableSize(viewWidth: Int?, spanCount: Int): Int {
		val width = viewWidth ?: return super.resolveSpanResizableSize(null, spanCount)
		return SectionGridGeometry.of(app, spanCount, width).cellSize
	}

	private fun referenceLocation(): KLatLon {
		val location = app.locationProvider.lastKnownLocation
		val mapLocation = app.settings.lastKnownMapLocation
		return location?.let { KLatLon(it.latitude, it.longitude) }
			?: KLatLon(mapLocation.latitude, mapLocation.longitude)
	}

	override fun getGalleryItems(): List<GalleryItem> {
		if (!repository.hasSnapshot) return emptyList()
		if (entries.isEmpty()) return listOf(emptyItem)
		val reference = referenceLocation()
		val sorted = entries.sortedWith(MediaLibrarySorter.comparator(sortMode.shared, reference))
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
					entry.attachments.size - 1)
			}
			return GalleryItem.Media(entry.mediaItem, presentation = mapper.presentation(entry.mediaItem, sortMode, distance).copy(attachment = attachment))
		}
		return buildList {
			if (isGrouped()) {
				for (group in MediaLibraryGrouping.group(sorted)) {
					val collapsed = group.type in collapsedGroups
					add(GalleryItem.GroupHeader(group.type, collapsed))
					if (!collapsed) addAll(group.items.map(::toItem))
				}
			} else addAll(sorted.map(::toItem))
			add(GalleryItem.Spacer)
			add(GalleryItem.MediaStats(mapper.statsText(MediaLibraryStats.compute(entries)), sectionFooter = true))
		}
	}

	override fun createAdapter(activity: FragmentActivity, viewWidth: Int?, nightMode: Boolean): GalleryGridAdapter =
		super.createAdapter(activity, viewWidth, nightMode).apply {
			val inflater = UiUtilities.getInflater(activity, nightMode)
			listRowFactory = { parent -> MediaLibraryListViewHolder(app,
				inflater.inflate(R.layout.media_library_list_item, parent, false), MediaProvider(app),
				::onMediaItemClicked, ::onMediaItemLongClicked, ::toggleSelection,
				app.galleryHelper.posterLoader, ::onMediaItemMenuClicked) }
			emptyRowFactory = { parent -> emptyRow(inflater, parent, nightMode) }
		}

	private fun emptyRow(inflater: LayoutInflater, parent: ViewGroup, nightMode: Boolean): RecyclerView.ViewHolder {
		val view = inflater.inflate(R.layout.gallery_empty_state_item, parent, false)
		view.findViewById<ImageView>(R.id.icon).setImageDrawable(
			app.uiUtilities.getPaintedIcon(emptyItem.iconResId, ColorUtilities.getDefaultIconColor(app, nightMode)))
		view.findViewById<TextView>(R.id.title).setText(emptyItem.titleResId)
		view.findViewById<TextView>(R.id.description).setText(emptyItem.descriptionResId)
		return object : RecyclerView.ViewHolder(view) {}
	}

	companion object { const val PROCESS_ID = "media_library" }
}
