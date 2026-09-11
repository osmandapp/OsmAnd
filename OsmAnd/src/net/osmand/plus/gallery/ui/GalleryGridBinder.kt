package net.osmand.plus.gallery.ui

import android.content.Context
import android.view.ScaleGestureDetector
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.motion.GalleryLayoutMorph
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.gallery.ui.motion.GridPinchController
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils

class GalleryGridBinder(
	private val recyclerView: GalleryGridRecyclerView,
	private val adapter: GalleryGridAdapter,
	private val controller: GalleryGridController,
	private val sectionCards: Boolean = false,
	nightMode: Boolean = false,
	private val resizableViewWidth: Int? = null
) {
	private interface ScrollLockable {
		var scrollLocked: Boolean
	}

	private class GalleryListLayoutManager(context: Context) : LinearLayoutManager(context), ScrollLockable {
		override var scrollLocked = false

		override fun canScrollVertically(): Boolean = !scrollLocked && super.canScrollVertically()
	}

	private class GalleryGridLayoutManager(context: Context, span: Int) : GridLayoutManager(context, span), ScrollLockable {
		var extraLayoutSpacePx = 0
		override var scrollLocked = false

		override fun canScrollVertically(): Boolean = !scrollLocked && super.canScrollVertically()

		override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
			if (extraLayoutSpacePx > 0) {
				extraLayoutSpace[0] = extraLayoutSpacePx
				extraLayoutSpace[1] = extraLayoutSpacePx
			} else {
				super.calculateExtraLayoutSpace(state, extraLayoutSpace)
			}
		}
	}

	private val app = recyclerView.context.applicationContext as OsmandApplication
	private val animationsEnabled = GalleryMotion.animationsEnabled(app)
	val cardDecoration: GallerySectionCardDecoration? = if (sectionCards) GallerySectionCardDecoration(app, nightMode) else null
	private val itemDecorator: GalleryGridItemDecorator? = if (sectionCards) null else GalleryGridItemDecorator(app)
	private var morph: GalleryLayoutMorph? = null
	private var pinch: GridPinchController? = null
	private var pendingItems: List<GalleryItem>? = null

	val isMorphing: Boolean get() = morph != null

	private val isPinching: Boolean get() = pinch?.isActive == true

	fun bind() {
		recyclerView.adapter = adapter
		cardDecoration?.let { recyclerView.addItemDecoration(it) }
		itemDecorator?.let { recyclerView.addItemDecoration(it) }
		recyclerView.itemAnimator = GalleryItemAnimator(recyclerView, adapter, cardDecoration, animationsEnabled)
		val pinchController = GridPinchController(app, recyclerView, adapter, controller, cardDecoration, itemDecorator, resizableViewWidth,
			object : GridPinchController.Listener {
				override fun isPortrait(): Boolean = AndroidUiHelper.isOrientationPortrait(recyclerView.context)

				override fun setScrollLocked(locked: Boolean) {
					(recyclerView.layoutManager as? ScrollLockable)?.scrollLocked = locked
				}

				override fun setExtraLayoutSpace(px: Int) {
					(recyclerView.layoutManager as? GalleryGridLayoutManager)?.let { manager ->
						if (manager.extraLayoutSpacePx != px) {
							manager.extraLayoutSpacePx = px
							manager.requestLayout()
						}
					}
				}

				override fun commitSpan(span: Int, anchorPosition: Int, anchorOffset: Int, onLaidOut: () -> Unit) {
					val manager = recyclerView.layoutManager as? GalleryGridLayoutManager ?: return
					manager.spanCount = span
					adapter.setItems(controller.getGalleryItems())
					manager.scrollToPositionWithOffset(anchorPosition, anchorOffset)
					recyclerView.doOnPreDraw {
						onLaidOut()
						pendingItems?.let { pending ->
							pendingItems = null
							setItems(pending, animated = true)
						}
					}
				}

				override fun switchDisplayMode(mode: GalleryDisplayMode) {
					controller.setDisplayMode(mode)
				}
			})
		pinch = pinchController
		recyclerView.setScaleDetector(ScaleGestureDetector(recyclerView.context, object : ScaleGestureDetector.OnScaleGestureListener {
			override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = pinchController.onScaleBegin(detector)
			override fun onScale(detector: ScaleGestureDetector): Boolean = pinchController.onScale(detector)
			override fun onScaleEnd(detector: ScaleGestureDetector) = pinchController.onScaleEnd()
		}))
		recyclerView.setGestureFinishedListener { pinchController.onGestureFinished() }
		applyLayout()
	}

	fun applyLayout() {
		val isList = controller.getDisplayMode() == GalleryDisplayMode.LIST
		val locked = (recyclerView.layoutManager as? ScrollLockable)?.scrollLocked == true
		recyclerView.layoutManager = if (isList) GalleryListLayoutManager(recyclerView.context).apply { scrollLocked = locked }
		else GalleryGridLayoutManager(recyclerView.context, controller.getSpanCount(AndroidUiHelper.isOrientationPortrait(recyclerView.context))).apply {
			scrollLocked = locked
			spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
				override fun getSpanSize(position: Int) = if (adapter.getItem(position) is GalleryItem.Media) 1 else spanCount
			}
		}
		if (!sectionCards) {
			val sidePadding = if (isList) 0 else AndroidUtils.dpToPx(recyclerView.context, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
			recyclerView.setPadding(sidePadding, 0, sidePadding, recyclerView.resources.getDimensionPixelSize(R.dimen.content_padding_large))
		}
	}

	fun setItems(items: List<GalleryItem>, animated: Boolean) {
		if (isMorphing || isPinching) {
			pendingItems = items
			return
		}
		val manager = recyclerView.layoutManager as? LinearLayoutManager
		val keepTop = animated && manager != null && adapter.itemCount > 0 && manager.findFirstCompletelyVisibleItemPosition() == 0
		adapter.setItems(items, animated)
		if (keepTop) manager?.scrollToPositionWithOffset(0, 0)
	}

	fun morphLayout(items: List<GalleryItem>, onComplete: (() -> Unit)? = null) {
		morph?.cancel()
		val crossMode = adapter.displayMode != controller.getDisplayMode()
		val apply = {
			adapter.displayMode = controller.getDisplayMode()
			applyLayout()
			adapter.setItems(items)
		}
		if (!animationsEnabled || recyclerView.childCount == 0) {
			recyclerView.itemAnimator?.endAnimations()
			apply()
			onComplete?.invoke()
			return
		}
		recyclerView.itemAnimator?.endAnimations()
		val running = GalleryLayoutMorph(recyclerView, adapter, cardDecoration)
		morph = running
		running.run(items, crossMode, apply) {
			if (morph === running) morph = null
			pendingItems?.let { pending ->
				pendingItems = null
				setItems(pending, animated = true)
			}
			onComplete?.invoke()
		}
	}

	fun release() {
		morph?.cancel()
		morph = null
		pendingItems = null
		recyclerView.itemAnimator?.endAnimations()
	}
}
