package net.osmand.plus.gallery.contract

import net.osmand.plus.gallery.model.GalleryAction

fun interface IGalleryActionListener {
	fun handleGalleryAction(action: GalleryAction)
}