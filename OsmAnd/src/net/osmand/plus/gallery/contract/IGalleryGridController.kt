package net.osmand.plus.gallery.contract

import net.osmand.plus.gallery.model.GalleryItem

interface IGalleryGridController : IGalleryListener, IGalleryActionListener {
	fun attach(view: IGalleryGridView)
	fun detach()
	fun getGalleryItems(): List<GalleryItem>
}
