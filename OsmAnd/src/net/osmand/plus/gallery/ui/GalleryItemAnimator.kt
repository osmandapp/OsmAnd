package net.osmand.plus.gallery.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import net.osmand.plus.gallery.ui.holders.GroupHeaderHolder
import net.osmand.plus.gallery.ui.holders.MorphableMediaHolder
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.gallery.ui.motion.GallerySectionCardTracks

class GalleryItemAnimator(
	private val recyclerView: RecyclerView,
	private val adapter: GalleryGridAdapter,
	private val cards: GallerySectionCardDecoration?,
	private val animationsEnabled: Boolean
) : SimpleItemAnimator() {

	private class Snapshot(val card: RectF?, val boundary: GallerySectionBoundary?)

	private sealed class Change(val holder: RecyclerView.ViewHolder, val duration: Long) {
		var delay = 0L
		var handle: GallerySectionCardTracks.Handle? = null
		val view: View get() = holder.itemView

		class Move(holder: RecyclerView.ViewHolder, val dx: Float, val dy: Float) : Change(holder, GalleryMotion.MOVE_DURATION_MS)
		class Add(holder: RecyclerView.ViewHolder) : Change(holder, GalleryMotion.MOVE_DURATION_MS)
		class Remove(holder: RecyclerView.ViewHolder) : Change(holder, GalleryMotion.MOVE_DURATION_MS)
		class FadeOut(holder: RecyclerView.ViewHolder) : Change(holder, GalleryMotion.FADE_DURATION_MS)
		class FadeIn(holder: RecyclerView.ViewHolder) : Change(holder, GalleryMotion.FADE_DURATION_MS)

		val visualTop: Float get() = if (this is Move) view.top + dy else view.top.toFloat()
		val visualLeft: Float get() = if (this is Move) view.left + dx else view.left.toFloat()

		val breathes: Boolean get() = holder is MorphableMediaHolder || holder is GroupHeaderHolder
	}

	private val pre = LinkedHashMap<RecyclerView.ViewHolder, Snapshot>()
	private val post = LinkedHashMap<RecyclerView.ViewHolder, Snapshot>()
	private val pending = mutableListOf<Change>()
	private val running = mutableListOf<Change>()
	private var timeline: ValueAnimator? = null

	init {
		addDuration = if (animationsEnabled) GalleryMotion.MOVE_DURATION_MS else 0
		removeDuration = addDuration
		changeDuration = if (animationsEnabled) GalleryMotion.FADE_DURATION_MS else 0
		moveDuration = addDuration
	}

	override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true

	override fun recordPreLayoutInformation(
		state: RecyclerView.State, holder: RecyclerView.ViewHolder, changeFlags: Int, payloads: MutableList<Any>
	): ItemHolderInfo {
		if (post.isNotEmpty()) {
			pre.clear()
			post.clear()
		}
		pre[holder] = snapshot(holder, adapter.getBoundSectionBoundary(holder))
		return super.recordPreLayoutInformation(state, holder, changeFlags, payloads)
	}

	override fun recordPostLayoutInformation(state: RecyclerView.State, holder: RecyclerView.ViewHolder): ItemHolderInfo {
		val position = recyclerView.getChildAdapterPosition(holder.itemView)
		val boundary = (if (position in 0 until adapter.itemCount) adapter.getSectionBoundary(position) else null)
			?: adapter.getBoundSectionBoundary(holder)
		post[holder] = snapshot(holder, boundary)
		return super.recordPostLayoutInformation(state, holder)
	}

	private fun snapshot(holder: RecyclerView.ViewHolder, boundary: GallerySectionBoundary?): Snapshot {
		val card = if (cards != null && boundary != null && holder.itemView.parent === recyclerView) {
			RectF().also { cards.cardBounds(recyclerView, holder.itemView, boundary, it) }
		} else {
			null
		}
		return Snapshot(card, boundary)
	}

	override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
		endAnimation(holder)
		val change = Change.Add(holder)
		val scale = if (change.breathes) GalleryMotion.APPEAR_SCALE else 1f
		holder.itemView.apply { alpha = 0f; scaleX = scale; scaleY = scale }
		pending += change
		return true
	}

	override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
		endAnimation(holder)
		pending += Change.Remove(holder)
		return true
	}

	override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
		val x = fromX + holder.itemView.translationX - toX
		val y = fromY + holder.itemView.translationY - toY
		endAnimation(holder)
		if (x == 0f && y == 0f) {
			dispatchMoveFinished(holder)
			return false
		}
		holder.itemView.translationX = x
		holder.itemView.translationY = y
		pending += Change.Move(holder, x, y)
		return true
	}

	override fun animateChange(
		oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder?, fromX: Int, fromY: Int, toX: Int, toY: Int
	): Boolean {
		if (oldHolder === newHolder) return animateMove(oldHolder, fromX, fromY, toX, toY)
		endAnimation(oldHolder)
		pending += Change.FadeOut(oldHolder)
		newHolder?.let { holder ->
			endAnimation(holder)
			holder.itemView.alpha = 0f
			pending += Change.FadeIn(holder)
		}
		return true
	}

	override fun runPendingAnimations() {
		if (pending.isEmpty()) {
			pre.clear()
			post.clear()
			return
		}
		if (running.isNotEmpty()) finishAll()
		val changes = pending.toList()
		pending.clear()
		changes.sortedWith(compareBy({ it.visualTop }, { it.visualLeft }))
			.forEachIndexed { index, change -> change.delay = if (animationsEnabled) GalleryMotion.stagger(index) else 0L }
		buildTracks(changes)
		pre.clear()
		post.clear()
		running += changes
		changes.forEach(::dispatchStarting)
		if (!animationsEnabled) {
			finishAll()
			return
		}
		val total = changes.maxOf { it.delay + it.duration }
		tick(0L)
		timeline = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = total
			interpolator = LinearInterpolator()
			addUpdateListener { tick((it.animatedFraction * total).toLong()) }
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (timeline === animation) finishAll()
				}
			})
			start()
		}
	}

	private fun buildTracks(changes: List<Change>) {
		val cards = cards ?: return
		if (!animationsEnabled) return
		val byHolder = HashMap<RecyclerView.ViewHolder, Change>()
		for (change in changes) byHolder[change.holder] = change
		val builder = GallerySectionCardTracks.Builder(cards.radius, 0f, recyclerView.height.toFloat())
		var any = false
		for (holder in (pre.keys + post.keys).distinct()) {
			val before = pre[holder]
			val after = post[holder]
			val startSection = before?.boundary?.sectionId
			val endSection = after?.boundary?.sectionId
			if (startSection == null && endSection == null) continue
			any = true
			val change = byHolder[holder]
			val handle = builder.element(startSection, before?.card, endSection, after?.card,
				change?.delay ?: 0L, change?.duration ?: GalleryMotion.MOVE_DURATION_MS)
			if (change is Change.Add || change is Change.Remove) change.handle = handle
		}
		if (!any) return
		pre.values.mapNotNull { it.boundary }.groupBy { it.sectionId }.forEach { (section, boundaries) ->
			builder.openEdgesBefore(section, boundaries.none { it.isFirst }, boundaries.none { it.isLast })
		}
		post.values.mapNotNull { it.boundary }.distinctBy { it.sectionId }.forEach { boundary ->
			val (top, bottom) = cards.openEdges(recyclerView, boundary)
			builder.openEdgesAfter(boundary.sectionId, top, bottom)
		}
		cards.tracks = builder.build()
		recyclerView.invalidate()
	}

	private fun tick(elapsed: Long) {
		for (change in running) {
			val p = GalleryMotion.progress(elapsed, change.delay, change.duration)
			val view = change.view
			when (change) {
				is Change.Move -> {
					view.translationX = change.dx * (1f - p)
					view.translationY = change.dy * (1f - p)
				}
				is Change.Add -> {
					val handle = change.handle
					val real = handle?.end
					val other = handle?.start
					if (real != null && other != null && other != real) {
						view.alpha = GalleryMotion.progress(elapsed, change.delay + change.duration - GalleryMotion.FADE_DURATION_MS, GalleryMotion.FADE_DURATION_MS)
						travel(view, real, other, real, p, appearScale(change, p))
					} else {
						val fade = GalleryMotion.progress(elapsed, change.delay, GalleryMotion.FADE_DURATION_MS)
						view.alpha = fade
						breathe(view, appearScale(change, fade))
					}
				}
				is Change.Remove -> {
					val handle = change.handle
					val real = handle?.start
					val other = handle?.end
					val fade = GalleryMotion.progress(elapsed, change.delay, GalleryMotion.FADE_DURATION_MS)
					view.alpha = 1f - fade
					if (real != null && other != null && other != real) {
						travel(view, real, real, other, p, appearScale(change, 1f - p))
					} else {
						breathe(view, appearScale(change, 1f - fade))
					}
				}
				is Change.FadeOut -> view.alpha = 1f - p
				is Change.FadeIn -> view.alpha = p
			}
		}
		cards?.tracks?.update(elapsed)
		recyclerView.invalidate()
	}

	private fun appearScale(change: Change, visible: Float): Float =
		if (change.breathes) GalleryMotion.APPEAR_SCALE + (1f - GalleryMotion.APPEAR_SCALE) * visible else 1f

	private fun breathe(view: View, scale: Float) {
		view.pivotX = view.width / 2f
		view.pivotY = view.height / 2f
		view.scaleX = scale
		view.scaleY = scale
	}

	private fun travel(view: View, real: RectF, from: RectF, to: RectF, p: Float, breathing: Float) {
		val top = from.top + (to.top - from.top) * p
		val height = from.height() + (to.height() - from.height()) * p
		view.pivotX = view.width / 2f
		view.pivotY = real.top - view.top
		view.translationY = top - real.top
		view.scaleY = if (real.height() > 0f) height / real.height() * breathing else breathing
		view.scaleX = breathing
	}

	private fun dispatchStarting(change: Change) {
		when (change) {
			is Change.Move -> dispatchMoveStarting(change.holder)
			is Change.Add -> dispatchAddStarting(change.holder)
			is Change.Remove -> dispatchRemoveStarting(change.holder)
			is Change.FadeOut -> dispatchChangeStarting(change.holder, true)
			is Change.FadeIn -> dispatchChangeStarting(change.holder, false)
		}
	}

	private fun finish(change: Change) {
		change.view.apply {
			alpha = 1f
			scaleX = 1f
			scaleY = 1f
			translationX = 0f
			translationY = 0f
			pivotX = width / 2f
			pivotY = height / 2f
		}
		when (change) {
			is Change.Move -> dispatchMoveFinished(change.holder)
			is Change.Add -> dispatchAddFinished(change.holder)
			is Change.Remove -> dispatchRemoveFinished(change.holder)
			is Change.FadeOut -> dispatchChangeFinished(change.holder, true)
			is Change.FadeIn -> dispatchChangeFinished(change.holder, false)
		}
	}

	private fun finishAll() {
		timeline?.let { animator ->
			timeline = null
			animator.cancel()
		}
		val finished = running.toList()
		running.clear()
		finished.forEach(::finish)
		cards?.let {
			if (it.tracks != null) {
				it.tracks = null
				recyclerView.invalidate()
			}
		}
		if (!isRunning) dispatchAnimationsFinished()
	}

	override fun endAnimation(item: RecyclerView.ViewHolder) {
		pending.filter { it.holder === item }.forEach { pending.remove(it); finish(it) }
		running.filter { it.holder === item }.forEach { running.remove(it); finish(it) }
		if (running.isEmpty() && timeline != null) finishAll()
		if (!isRunning) dispatchAnimationsFinished()
	}

	override fun endAnimations() {
		val started = pending.toList()
		pending.clear()
		started.forEach(::finish)
		finishAll()
	}

	override fun isRunning(): Boolean = pending.isNotEmpty() || running.isNotEmpty()
}
