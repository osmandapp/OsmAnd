package net.osmand.plus.gallery.contract

import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem

interface IGalleryRowController : IGalleryListener {

	var view: IGalleryRowView?

	fun onGalleryRowBuilt(updateOnly: Boolean, collapsed: Boolean)

	fun attachView(view: IGalleryRowView) {
		this.view = view
	}

	fun detachView(view: IGalleryRowView) {
		this.view = null
	}

	fun getGalleryItems(): List<GalleryItem>

	fun collectActionButtons(): List<GalleryActionButton>

	fun handleActionButtonClick(actionButton: GalleryActionButton)

	fun onCollapseExpandRow(collapsed: Boolean)

	fun onClose()
}