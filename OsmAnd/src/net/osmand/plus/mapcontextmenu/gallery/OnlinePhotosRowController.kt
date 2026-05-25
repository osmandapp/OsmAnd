package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.GallerySession
import net.osmand.plus.gallery.contract.IGalleryRowController
import net.osmand.plus.gallery.contract.IGalleryRowView
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryItem.NoInternet
import net.osmand.plus.gallery.online.OnlinePhotoParamsProvider
import net.osmand.plus.gallery.online.OnlinePhotosHolder
import net.osmand.plus.gallery.ui.GalleryGridFragment
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import net.osmand.util.Algorithms

private val SHOW_ALL_ACTION = GalleryAction("show_all")

open class OnlinePhotosRowController(
	val app: OsmandApplication,
	private val paramsProvider: OnlinePhotoParamsProvider
) : IGalleryRowController, OnlinePhotosFlowListener {

	val mediaProvider: MediaProvider = MediaProvider(app)

	// TODO: flow should be one object for all controllers
	private val onlinePhotosFlow = OnlinePhotosFlow(app, this)

	private val galleryItems = mutableListOf<GalleryItem>()

	override var view: IGalleryRowView? = null

	protected val mapActivity: MapActivity?
		get() = view?.mapActivity

	override fun askUpdate(updateOnly: Boolean, collapsed: Boolean) {
		val photoItems = onlinePhotosFlow?.currentGalleryItems
		if (updateOnly && photoItems != null) {
			setItems(photoItems)
		} else {
			loadImagesIfNeeded(collapsed)
		}
	}

	protected fun setItems(vararg items: GalleryItem) {
		setItems(items.asList())
	}

	protected fun setItems(items: Collection<GalleryItem>) {
		galleryItems.clear()
		galleryItems.addAll(items)
		view?.render()
	}

	override fun getGalleryItems(): List<GalleryItem> {
		val items = mutableListOf<GalleryItem>()
		if (!app.settings.isInternetConnectionAvailable) {
			items.add(NoInternet)
		} else {
			items.addAll(galleryItems)
		}
		return items
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val position = GallerySession.getPhotoItemIndexById(mediaItem.id)
		mapActivity?.let { GalleryPhotoPagerFragment.showInstance(it, position) }
	}

	override fun onReloadMediaItems() {
		if (!app.settings.isInternetConnectionAvailable) {
			app.showShortToastMessage(R.string.shared_string_no_internet_connection)
		} else {
			startLoadingImages()
		}
	}

	private fun loadImagesIfNeeded(collapsed: Boolean) {
		if (!collapsed && onlinePhotosFlow?.hasCurrentGalleryItems() == false && !onlinePhotosFlow.isLoading) {
			startLoadingImages()
		}
	}

	private fun startLoadingImages() {
		onlinePhotosFlow?.startLoadingImages(
			paramsProvider.getLatLon(), paramsProvider.getAdditionalImageParams())
	}

	override fun collectActionButtons(): List<GalleryActionButton> {
		val showAllButton = GalleryActionButton(
			titleId = R.string.shared_string_show_all,
			action = SHOW_ALL_ACTION
		)
		return if (hasMediaItems()) listOf(showAllButton) else emptyList()
	}

	override fun handleActionButtonClick(actionButton: GalleryActionButton) {
		if (actionButton.action == SHOW_ALL_ACTION) {
			mapActivity?.let { GalleryGridFragment.showInstance(it) }
		}
	}

	override fun onCollapseExpandRow(collapsed: Boolean) {
		loadImagesIfNeeded(collapsed)
	}

	override fun onClose() {
		onlinePhotosFlow?.clear()
	}

	protected fun hasMediaItems(): Boolean {
		if (Algorithms.isEmpty(galleryItems)) {
			return false
		}
		for (item in galleryItems) {
			if (item is GalleryItem.Media) {
				return true
			}
		}
		return false
	}

	override fun onPhotosLoadStarted() {
		// TODO check is hidden
		view?.onLoadingImage(true)
	}

	override fun onPhotosLoadFinished(holder: OnlinePhotosHolder) {
		// TODO check is hidden
		view?.onLoadingImage(false)
		setItems(holder.getOrderedGalleryItems())
	}
}