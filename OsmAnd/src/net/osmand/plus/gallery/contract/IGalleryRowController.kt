package net.osmand.plus.gallery.contract

import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem

interface IGalleryRowController : IGalleryListener, IGalleryActionListener {
	fun attach(view: IGalleryRowView)
	fun detach()
	fun getGalleryItems(): List<GalleryItem>
	fun collectActionButtons(): List<GalleryActionButton>
	fun onRowBuilt(collapsed: Boolean)
	fun onCollapseExpandRow(collapsed: Boolean)
}