package net.osmand.plus.gallery.controller

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.contract.IGalleryRowController
import net.osmand.plus.gallery.contract.IGalleryRowView
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.MediaLoadListener
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.MediaHolder

abstract class GalleryRowController(
	protected val app: OsmandApplication,
	protected val key: GalleryKey
) : IGalleryRowController {

	protected var view: IGalleryRowView? = null
	private var items: List<GalleryItem> = emptyList()
	private var loading = false

	protected val loadListener = object : MediaLoadListener {

		override fun onLoadingStarted(key: GalleryKey) {
			loading = true
			view?.onLoadingImage(true)
		}

		override fun onLoaded(key: GalleryKey, holder: MediaHolder) {
			loading = false
			view?.onLoadingImage(false)

			items = buildGalleryItems(holder).ifEmpty {
				emptyStateItems()
			}
			view?.render()
		}

		override fun onLoadFailed(key: GalleryKey) {
			loading = false
			view?.onLoadingImage(false)

			items = emptyStateItems()
			view?.render()
		}
	}

	override fun attach(view: IGalleryRowView) {
		this.view = view
	}

	override fun detach() {
		app.galleryHelper.mediaLoader.cancel(key, loadListener)
		loading = false
		view = null
	}

	override fun getGalleryKey() = key

	override fun onRowBuilt(collapsed: Boolean) {
		loadIfNeeded(collapsed)
	}

	override fun onCollapseExpandRow(collapsed: Boolean) {
		loadIfNeeded(collapsed)
	}

	private fun loadIfNeeded(collapsed: Boolean) {
		if (!collapsed && !loading && items.isEmpty()) {
			app.galleryHelper.mediaLoader.load(key, loadListener)
		}
	}

	override fun getGalleryItems(): List<GalleryItem> {
		if (requiresInternet() && !app.settings.isInternetConnectionAvailable && !hasMediaItems()) {
			return listOf(GalleryItem.NoInternet)
		}
		return items
	}

	override fun onReloadMediaItems() {
		if (requiresInternet() && !app.settings.isInternetConnectionAvailable) {
			app.showShortToastMessage(R.string.shared_string_no_internet_connection)
			return
		}
		app.galleryHelper.mediaLoader.reload(key, loadListener)
	}

	protected fun hasMediaItems(): Boolean {
		return items.any { it is GalleryItem.Media }
	}

	protected open fun requiresInternet(): Boolean = false

	protected open fun emptyStateItems(): List<GalleryItem> {
		return listOf(GalleryItem.NoMedia())
	}

	protected abstract fun buildGalleryItems(holder: MediaHolder): List<GalleryItem>
}