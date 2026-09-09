package net.osmand.plus.gallery.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.PathInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/** Bounds moves and quiet appearances; metadata payloads reuse their holder. */
class GalleryItemAnimator(private val animationsEnabled: Boolean = true) : SimpleItemAnimator() {
	private data class Change(val holder: RecyclerView.ViewHolder, val start: () -> Unit, val finish: () -> Unit)
	private val pending = linkedMapOf<RecyclerView.ViewHolder, Change>()
	private val running = linkedMapOf<RecyclerView.ViewHolder, Change>()
	private val curve = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

	init {
		addDuration = if (animationsEnabled) 150 else 0
		removeDuration = addDuration
		moveDuration = if (animationsEnabled) 300 else 0
		changeDuration = addDuration
	}
	override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true

	override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
		endAnimation(holder)
		holder.itemView.apply { alpha = 0f; scaleX = .92f; scaleY = .92f }
		pending[holder] = Change(holder, {
			dispatchAddStarting(holder)
			holder.itemView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(addDuration)
		}, { dispatchAddFinished(holder) })
		return true
	}

	override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
		endAnimation(holder)
		pending[holder] = Change(holder, {
			dispatchRemoveStarting(holder)
			holder.itemView.animate().alpha(0f).scaleX(.92f).scaleY(.92f).setDuration(removeDuration)
		}, { dispatchRemoveFinished(holder) })
		return true
	}

	override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
		val x = fromX + holder.itemView.translationX - toX
		val y = fromY + holder.itemView.translationY - toY
		endAnimation(holder)
		if (x == 0f && y == 0f) { dispatchMoveFinished(holder); return false }
		holder.itemView.translationX = x
		holder.itemView.translationY = y
		pending[holder] = Change(holder, {
			dispatchMoveStarting(holder)
			holder.itemView.animate().translationX(0f).translationY(0f).setDuration(moveDuration)
		}, { dispatchMoveFinished(holder) })
		return true
	}

	override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder?, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
		if (oldHolder === newHolder) return animateMove(oldHolder, fromX, fromY, toX, toY)
		endAnimation(oldHolder)
		pending[oldHolder] = Change(oldHolder, {
			dispatchChangeStarting(oldHolder, true)
			oldHolder.itemView.animate().alpha(0f).setDuration(changeDuration)
		}, { dispatchChangeFinished(oldHolder, true) })
		newHolder?.let { holder ->
			endAnimation(holder)
			holder.itemView.alpha = 0f
			pending[holder] = Change(holder, {
				dispatchChangeStarting(holder, false)
				holder.itemView.animate().alpha(1f).setDuration(changeDuration)
			}, { dispatchChangeFinished(holder, false) })
		}
		return true
	}

	override fun runPendingAnimations() {
		val changes = pending.values.toList()
		pending.clear()
		changes.forEachIndexed { index, change ->
			running[change.holder] = change
			change.start()
			change.holder.itemView.animate().setInterpolator(curve).setStartDelay(if (animationsEnabled) minOf(index * 15L, 120L) else 0)
				.setListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						if (running.remove(change.holder) != null) finish(change)
						if (!isRunning) dispatchAnimationsFinished()
					}
				}).start()
		}
	}

	private fun finish(change: Change) {
		change.holder.itemView.apply {
			animate().setListener(null).setStartDelay(0)
			alpha = 1f; scaleX = 1f; scaleY = 1f; translationX = 0f; translationY = 0f
		}
		change.finish()
	}

	override fun endAnimation(item: RecyclerView.ViewHolder) {
		pending.remove(item)?.let(::finish)
		item.itemView.animate().cancel()
		running.remove(item)?.let(::finish)
		if (!isRunning) dispatchAnimationsFinished()
	}

	override fun endAnimations() {
		pending.keys.toList().forEach(::endAnimation)
		running.keys.toList().forEach(::endAnimation)
	}

	override fun isRunning(): Boolean = pending.isNotEmpty() || running.isNotEmpty()
}
