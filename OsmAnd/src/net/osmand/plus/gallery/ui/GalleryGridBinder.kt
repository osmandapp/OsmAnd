package net.osmand.plus.gallery.ui

import android.content.Context
import android.os.Parcelable
import android.view.ScaleGestureDetector
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.FragmentActivity
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
	private val controller: GalleryGridController,
	private val activity: FragmentActivity,
	private val nightMode: Boolean,
	private val sectionCards: Boolean
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
	private var adapter: GalleryGridAdapter? = null
	private var morph: GalleryLayoutMorph? = null
	private var pinch: GridPinchController? = null
	private var pendingItems: List<GalleryItem>? = null
	private var released = false

	var items: List<GalleryItem> = emptyList()
		private set

	var pendingLayoutState: Parcelable? = null

	val isMorphing: Boolean get() = morph != null

	private val isPinching: Boolean get() = pinch?.isActive == true

	init {
		recyclerView.doOnLayout {
			recyclerView.post { if (!released && adapter == null) bind() }
		}
	}

	private fun bind() {
		val contentWidth = if (sectionCards) recyclerView.width - recyclerView.paddingLeft - recyclerView.paddingRight else recyclerView.width
		val adapter = controller.createAdapter(activity, contentWidth, nightMode)
		this.adapter = adapter
		adapter.displayMode = controller.getDisplayMode()
		adapter.selectionMode = controller.isSelectionMode()
		adapter.sectionCardRadius = cardDecoration?.radius ?: 0f
		recyclerView.adapter = adapter
		cardDecoration?.let { recyclerView.addItemDecoration(it) }
		itemDecorator?.let { recyclerView.addItemDecoration(it) }
		recyclerView.itemAnimator = GalleryItemAnimator(recyclerView, adapter, cardDecoration, animationsEnabled)
		val pinchController = GridPinchController(app, recyclerView, adapter, controller, cardDecoration, itemDecorator, contentWidth,
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
						applyPendingItems()
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
		applyLayout(adapter)
		items = controller.getGalleryItems()
		adapter.setItems(items)
		restoreLayoutState()
	}

	private fun applyLayout(adapter: GalleryGridAdapter) {
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

	fun updateDisplayMode() {
		val adapter = adapter ?: return
		morph?.cancel()
		val items = controller.getGalleryItems()
		this.items = items
		val crossMode = adapter.displayMode != controller.getDisplayMode()
		val apply = {
			adapter.displayMode = controller.getDisplayMode()
			applyLayout(adapter)
			adapter.setItems(items)
		}
		if (!animationsEnabled || recyclerView.childCount == 0) {
			recyclerView.itemAnimator?.endAnimations()
			apply()
			return
		}
		recyclerView.itemAnimator?.endAnimations()
		val running = GalleryLayoutMorph(recyclerView, adapter, cardDecoration)
		morph = running
		running.run(items, crossMode, apply) {
			if (morph === running) morph = null
			applyPendingItems()
		}
	}

	fun updateItems() {
		items = controller.getGalleryItems()
		val adapter = adapter ?: return
		adapter.selectionMode = controller.isSelectionMode()
		setItems(adapter, items)
		restoreLayoutState()
		if (cardDecoration != null) recyclerView.invalidateItemDecorations()
	}

	fun updateSelection() {
		val adapter = adapter ?: return
		adapter.selectionMode = controller.isSelectionMode()
		adapter.notifySelectionChanged()
	}

	fun saveLayoutState(): Parcelable? = recyclerView.layoutManager?.onSaveInstanceState() ?: pendingLayoutState

	private fun setItems(adapter: GalleryGridAdapter, items: List<GalleryItem>) {
		if (isMorphing || isPinching) {
			pendingItems = items
			return
		}
		val manager = recyclerView.layoutManager as? LinearLayoutManager
		val keepTop = manager != null && adapter.itemCount > 0 && manager.findFirstCompletelyVisibleItemPosition() == 0
		adapter.setItems(items, animated = true)
		if (keepTop) manager?.scrollToPositionWithOffset(0, 0)
	}

	private fun applyPendingItems() {
		val pending = pendingItems ?: return
		pendingItems = null
		adapter?.let { setItems(it, pending) }
	}

	private fun restoreLayoutState() {
		val state = pendingLayoutState ?: return
		if (items.isEmpty()) return
		pendingLayoutState = null
		recyclerView.layoutManager?.onRestoreInstanceState(state)
	}

	fun release() {
		released = true
		morph?.cancel()
		morph = null
		pendingItems = null
		recyclerView.itemAnimator?.endAnimations()
		recyclerView.adapter = null
		adapter = null
	}
}
