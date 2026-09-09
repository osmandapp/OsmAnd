package net.osmand.plus.gallery.ui

import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.R

class GalleryGridBinder(
	private val recyclerView: GalleryGridRecyclerView,
	private val adapter: GalleryGridAdapter,
	private val controller: GalleryGridController,
	private val sectionCards: Boolean = false
) {
	fun bind() {
		recyclerView.adapter = adapter
		recyclerView.setScaleDetector(ScaleGestureDetector(recyclerView.context, object : ScaleGestureDetector.OnScaleGestureListener {
			override fun onScaleBegin(detector: ScaleGestureDetector): Boolean { controller.onScaleBegin(); return true }
			override fun onScale(detector: ScaleGestureDetector): Boolean { controller.onScaleChanged(detector.scaleFactor); return true }
			override fun onScaleEnd(detector: ScaleGestureDetector) = controller.onScaleEnd()
		}))
		recyclerView.setGestureFinishedListener { controller.onPinchGestureFinished() }
		applyLayout()
	}

	fun applyLayout() {
		val isList = controller.getDisplayMode() == GalleryDisplayMode.LIST
		recyclerView.layoutManager = if (isList) LinearLayoutManager(recyclerView.context)
		else GridLayoutManager(recyclerView.context, controller.getSpanCount(AndroidUiHelper.isOrientationPortrait(recyclerView.context))).apply {
			spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
				override fun getSpanSize(position: Int) = if (adapter.getItem(position) is GalleryItem.Media) 1 else spanCount
			}
		}
		if (!sectionCards) {
			val sidePadding = if (isList) 0 else AndroidUtils.dpToPx(recyclerView.context, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
			recyclerView.setPadding(sidePadding, 0, sidePadding, recyclerView.resources.getDimensionPixelSize(R.dimen.content_padding_large))
		}
	}
}
