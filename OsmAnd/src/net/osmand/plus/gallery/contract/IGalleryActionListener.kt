package net.osmand.plus.gallery.contract

import android.view.View
import net.osmand.plus.gallery.model.GalleryAction

fun interface IGalleryActionListener {
	fun handleGalleryAction(v: View, action: GalleryAction)
}