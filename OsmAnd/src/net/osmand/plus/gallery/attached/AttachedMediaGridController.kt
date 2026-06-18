package net.osmand.plus.gallery.attached

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.attached.helpers.AttachedMediaUiHelper
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.controller.GalleryPresentationMapper
import net.osmand.plus.gallery.data.Cancellable
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.gallery.data.MediaLoadListener
import net.osmand.plus.gallery.data.MediaMetadataListener
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.gallery.model.GalleryToolbarAction
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.ui.GalleryGridFragment
import net.osmand.plus.gallery.ui.holders.SortBarHolder
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.media.domain.MediaItem

class AttachedMediaGridController(
	app: OsmandApplication,
	override val key: GalleryKey,
	private val target: Linkable?,
	private val latLon: LatLon?
) : GalleryGridController(app, key) {

	private var sortMode = GallerySortMode.NAME_A_Z

	private val metadataRepository = app.galleryHelper.metadataRepository
	private val mapper = GalleryPresentationMapper(app, metadataRepository)

	private val handler = Handler(Looper.getMainLooper())
	private var metadataRequest: Cancellable? = null
	private var hasPendingMetadataUpdate = false
	private val mediaChangeListener: (Set<GalleryKey>) -> Unit = { keys ->
		if (key in keys) {
			onMediaChanged()
		}
	}

	private val reloadListener = object : MediaLoadListener {
		override fun onLoadingStarted(key: GalleryKey) {}

		override fun onLoaded(key: GalleryKey, holder: MediaHolder) {
			view?.updateItems()
			view?.updateToolbar()
			requestMetadata()
		}

		override fun onLoadFailed(key: GalleryKey) {
			view?.updateItems()
			view?.updateToolbar()
		}
	}

	private val metadataListener = object : MediaMetadataListener {
		override fun onMetadataLoaded(item: MediaItem, metadata: GalleryMediaMetadata) {
			scheduleMetadataUpdate()
		}

		override fun onBatchFinished() {
			scheduleMetadataUpdate()
		}
	}

	private val applyMetadataUpdate = Runnable {
		if (!hasPendingMetadataUpdate) return@Runnable
		if (isSelectionMode()) return@Runnable
		hasPendingMetadataUpdate = false
		view?.updateItems()
	}

	init {
		app.galleryHelper.addAttachedMediaChangeListener(mediaChangeListener)
	}

	override fun attach(view: IGalleryGridView) {
		super.attach(view)
		requestMetadata()
	}

	override fun onScreenDestroyed(activity: FragmentActivity?) {
		app.galleryHelper.removeAttachedMediaChangeListener(mediaChangeListener)
		metadataRequest?.cancel()
		metadataRequest = null
		handler.removeCallbacks(applyMetadataUpdate)
		super.onScreenDestroyed(activity)
	}

	override fun exitSelectionMode() {
		super.exitSelectionMode()
		if (hasPendingMetadataUpdate) {
			hasPendingMetadataUpdate = false
			view?.updateItems()
		}
	}

	private fun requestMetadata() {
		val items = getMediaItems()
		if (items.isEmpty()) return
		metadataRequest?.cancel()
		metadataRequest = metadataRepository.request(items, metadataListener)
	}

	private fun scheduleMetadataUpdate() {
		hasPendingMetadataUpdate = true
		handler.removeCallbacks(applyMetadataUpdate)
		handler.postDelayed(applyMetadataUpdate, METADATA_UPDATE_DEBOUNCE_MS)
	}

	override fun getProcessId(): String = processId(key)

	override fun getScreenTitle(): String = app.getString(R.string.shared_string_media)

	override fun isListModeSupported(): Boolean = true

	override fun isSelectionModeSupported(): Boolean = true

	override fun getGalleryItems(): List<GalleryItem> {
		val media = sortMedia(getMediaItems())
		val items = mutableListOf<GalleryItem>()
		items.add(GalleryItem.SortBar(sortMode))
		if (media.isEmpty()) {
			return items
		}
		media.forEach {
			items.add(
				GalleryItem.Media(it, showLoadingProgress = false, presentation = mapper.presentation(it))
			)
		}
		items.add(GalleryItem.MediaStats(mapper.statsText(media)))
		return items
	}

	private fun sortMedia(media: List<MediaItem>): List<MediaItem> {
		val byName = compareBy<MediaItem> { it.title.lowercase() }
		return when (sortMode) {
			GallerySortMode.NAME_A_Z -> media.sortedWith(byName)
			GallerySortMode.NAME_Z_A -> media.sortedWith(byName.reversed())
			GallerySortMode.LAST_MODIFIED,
			GallerySortMode.NEWEST_FIRST ->
				media.sortedByDescending { metadataOf(it)?.dateMillis ?: Long.MIN_VALUE }
			GallerySortMode.OLDEST_FIRST ->
				media.sortedBy { metadataOf(it)?.dateMillis ?: Long.MAX_VALUE }
			GallerySortMode.DURATION_LONG_SHORT ->
				media.sortedByDescending { metadataOf(it)?.durationMs ?: Long.MIN_VALUE }
			GallerySortMode.DURATION_SHORT_LONG ->
				media.sortedBy { metadataOf(it)?.durationMs ?: Long.MAX_VALUE }
			// TODO #7125: Nearest needs per-item coordinates
			GallerySortMode.NEAREST -> media
		}
	}

	private fun metadataOf(item: MediaItem): GalleryMediaMetadata? =
		metadataRepository.getCached(item)

	override fun getToolbarActions(): List<GalleryToolbarAction> =
		if (isSelectionMode()) {
			listOf(
				GalleryToolbarAction(
					SELECT_ALL_ACTION,
					if (isAllSelected()) R.drawable.ic_action_deselect_all else R.drawable.ic_action_select_all,
					R.string.shared_string_select_all
				),
				GalleryToolbarAction(
					ACTIONS_MENU_ACTION,
					R.drawable.ic_overflow_menu_white,
					R.string.shared_string_actions
				)
			)
		} else {
			listOf(
				GalleryToolbarAction(ADD_ACTION, R.drawable.ic_action_add_no_bg, R.string.shared_string_add),
				GalleryToolbarAction(EDIT_ACTION, R.drawable.ic_action_edit_outlined, R.string.shared_string_edit)
			)
		}

	override fun handleGalleryAction(v: View, action: GalleryAction) {
		val activity = view?.getMapActivity() ?: return
		when (action) {
			ADD_ACTION -> {
				val target = target ?: return
				AttachedMediaUiHelper(activity).showAddMenu(v, target, latLon) { onMediaChanged() }
			}

			EDIT_ACTION -> enterSelectionMode(null)
			SELECT_ALL_ACTION -> toggleSelectAll()
			ACTIONS_MENU_ACTION -> showSelectionActionsMenu(v)
			SortBarHolder.SORT_ACTION -> showSortMenu(v)

			// TODO #7125: implement export\delete of the selected media items.
			EXPORT_ACTION -> {}
			DELETE_ACTION -> {}
		}
	}

	private fun showSelectionActionsMenu(anchor: View) {
		val nightMode = view?.isNightMode() ?: false
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val items = listOf(
			PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_export)
				.setIcon(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_export, iconColor))
				.setOnClickListener { handleGalleryAction(anchor, EXPORT_ACTION) }
				.create(),
			PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_delete_outlined, iconColor))
				.setOnClickListener { handleGalleryAction(anchor, DELETE_ACTION) }
				.create()
		)
		val data = PopUpMenuDisplayData()
		data.anchorView = anchor
		data.menuItems = items
		data.nightMode = nightMode
		data.widthMode = PopUpMenuWidthMode.STANDARD
		PopUpMenu.show(data)
	}

	private fun showSortMenu(anchor: View) {
		val nightMode = view?.isNightMode() ?: false
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val items = GallerySortMode.entries.map { mode ->
			PopUpMenuItem.Builder(app)
				.setTitleId(mode.titleId)
				.setIcon(app.uiUtilities.getPaintedIcon(mode.iconId, iconColor))
				.setSelected(mode == sortMode)
				.setOnClickListener { setSortMode(mode) }
				.create()
		}
		val data = PopUpMenuDisplayData()
		data.anchorView = anchor
		data.menuItems = items
		data.nightMode = nightMode
		data.widthMode = PopUpMenuWidthMode.STANDARD
		PopUpMenu.show(data)
	}

	private fun setSortMode(mode: GallerySortMode) {
		if (sortMode != mode) {
			sortMode = mode
			view?.updateItems()
		}
	}

	private fun onMediaChanged() {
		app.galleryHelper.mediaLoader.reload(key, reloadListener)
	}

	companion object {
		private const val PROCESS_ID = "gallery_grid_attached"
		private const val METADATA_UPDATE_DEBOUNCE_MS = 250L

		private val ADD_ACTION = GalleryAction("attached_add")
		private val EDIT_ACTION = GalleryAction("attached_edit")
		private val SELECT_ALL_ACTION = GalleryAction("attached_select_all")
		private val ACTIONS_MENU_ACTION = GalleryAction("attached_actions_menu")
		private val EXPORT_ACTION = GalleryAction("attached_export")
		private val DELETE_ACTION = GalleryAction("attached_delete")

		fun processId(key: GalleryKey): String = "${PROCESS_ID}_${key}"

		@JvmStatic
		@JvmOverloads
		fun show(
			activity: FragmentActivity,
			key: GalleryKey,
			target: Linkable? = null,
			latLon: LatLon? = null
		) {
			val app = activity.application as OsmandApplication
			val controller = AttachedMediaGridController(app, key, target, latLon)
			app.dialogManager.register(controller.processId, controller)
			GalleryGridFragment.showInstance(activity, controller.processId)
		}
	}
}