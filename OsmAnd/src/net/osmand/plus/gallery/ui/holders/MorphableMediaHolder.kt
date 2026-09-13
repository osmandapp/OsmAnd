package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View

interface MorphableMediaHolder {
	val boundItemId: String?

	val previewView: View

	val morphSnapshotView: View?

	val morphPreviewBitmap: Bitmap?
	val morphCenterIcon: Drawable?
	val morphShowsScrim: Boolean
	val morphDurationLabel: String?
	val morphShowsDuration: Boolean
	val morphDurationTextColor: Int
	val morphBgColor: Int

	fun getFadeableContentViews(): List<View>

	fun getSelectionOverlayViews(): List<View>

	fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit)

	fun endMorph(revealed: Boolean)
}
