package net.osmand.plus.gallery.contract

import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem

interface IGalleryRowController : IGalleryListener, IGalleryActionListener {
	fun attach(view: IGalleryRowView)
	fun detach()
	fun getGalleryKey(): GalleryKey
	fun getGalleryItems(): List<GalleryItem>
	fun collectActionButtons(): List<GalleryActionButton>
	fun onRowBuilt(collapsed: Boolean)
	fun onCollapseExpandRow(collapsed: Boolean)
	fun matches(key: GalleryKey) = getGalleryKey() == key
}
