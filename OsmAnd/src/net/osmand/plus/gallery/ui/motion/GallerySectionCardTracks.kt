package net.osmand.plus.gallery.ui.motion

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class GallerySectionCardTracks private constructor(
	private val tracks: List<Track>,
	private val radius: Float,
	private val viewportTop: Float,
	private val viewportBottom: Float
) {

	class Contribution(val start: RectF, val end: RectF, val delay: Long, val duration: Long)

	private class Track(
		val startSection: String?,
		val endSection: String?,
		val contributions: List<Contribution>,
		val startBox: RectF?,
		val endBox: RectF?,
		val openStart: OpenEdges,
		val openEnd: OpenEdges
	) {
		val minDelay = contributions.minOfOrNull { it.delay } ?: 0L
		var startTopRound = true
		var startBottomRound = true
		var endTopRound = true
		var endBottomRound = true
		val rect = RectF()
		val drawRect = RectF()
		var alpha = 1f
		var topRadius = 0f
		var bottomRadius = 0f
	}

	private val path = Path()
	private val corners = FloatArray(8)
	private val scratch = RectF()

	class Card(val startSection: String?, val endSection: String?, val rect: RectF, val alpha: Float)

	fun update(elapsed: Long) {
		for (track in tracks) {
			track.alpha = rectAt(track, elapsed, track.rect)
			if (track.startSection != null && track.endSection != null) {
				val p = GalleryMotion.progress(elapsed, track.minDelay, GalleryMotion.MOVE_DURATION_MS)
				track.topRadius = lerp(if (track.startTopRound) radius else 0f, if (track.endTopRound) radius else 0f, p)
				track.bottomRadius = lerp(if (track.startBottomRound) radius else 0f, if (track.endBottomRound) radius else 0f, p)
			} else {
				track.topRadius = radius
				track.bottomRadius = radius
			}
		}
	}

	fun currentCards(): List<Card> = tracks.map { Card(it.startSection, it.endSection, RectF(it.rect), it.alpha) }

	fun cardsAt(elapsed: Long): List<Card> = tracks.map { track ->
		val rect = RectF()
		val alpha = rectAt(track, elapsed, rect)
		Card(track.startSection, track.endSection, rect, alpha)
	}

	private fun rectAt(track: Track, elapsed: Long, out: RectF): Float {
		when {
			track.startSection == null -> {
				val box = track.endBox ?: return 0f
				val p = GalleryMotion.progress(elapsed, track.minDelay, GalleryMotion.FADE_DURATION_MS)
				scaled(box, GalleryMotion.APPEAR_SCALE, scratch)
				lerp(scratch, box, p, out)
				return p
			}
			track.endSection == null -> {
				val box = track.startBox ?: return 0f
				val p = GalleryMotion.progress(elapsed, track.minDelay, GalleryMotion.FADE_DURATION_MS)
				scaled(box, GalleryMotion.APPEAR_SCALE, scratch)
				lerp(box, scratch, p, out)
				return 1f - p
			}
			else -> {
				var first = true
				for (contribution in track.contributions) {
					val p = GalleryMotion.progress(elapsed, contribution.delay, contribution.duration)
					lerp(contribution.start, contribution.end, p, scratch)
					if (first) {
						out.set(scratch)
						first = false
					} else {
						out.union(scratch)
					}
				}
				extendOpenEdges(track, elapsed, out)
				return 1f
			}
		}
	}

	private fun extendOpenEdges(track: Track, elapsed: Long, out: RectF) {
		val startBox = track.startBox ?: return
		val endBox = track.endBox ?: return
		val p = GalleryMotion.progress(elapsed, track.minDelay, GalleryMotion.MOVE_DURATION_MS)
		if (track.openStart.top || track.openEnd.top) {
			val from = if (track.openStart.top) viewportTop - radius else startBox.top
			val to = if (track.openEnd.top) viewportTop - radius else endBox.top
			out.top = min(out.top, lerp(from, to, p))
		}
		if (track.openStart.bottom || track.openEnd.bottom) {
			val from = if (track.openStart.bottom) viewportBottom + radius else startBox.bottom
			val to = if (track.openEnd.bottom) viewportBottom + radius else endBox.bottom
			out.bottom = max(out.bottom, lerp(from, to, p))
		}
	}

	fun draw(canvas: Canvas, paint: Paint) {
		bridgeSeams()
		for (track in tracks) {
			if (track.drawRect.isEmpty || track.alpha <= 0f) continue
			paint.alpha = (track.alpha * 255f).roundToInt().coerceIn(0, 255)
			corners.fill(track.topRadius, 0, 4)
			corners.fill(track.bottomRadius, 4, 8)
			path.rewind()
			path.addRoundRect(track.drawRect, corners, Path.Direction.CW)
			canvas.drawPath(path, paint)
		}
		paint.alpha = 255
	}

	private fun bridgeSeams() {
		for (track in tracks) track.drawRect.set(track.rect)
		for (track in tracks) {
			for (other in tracks) {
				if (other === track) continue
				if (other.rect.top > track.rect.bottom - radius - TOUCH_PX && other.rect.top <= track.rect.bottom + TOUCH_PX) {
					track.drawRect.bottom = max(track.drawRect.bottom, other.rect.top + radius)
				}
				if (other.rect.bottom >= track.rect.top - TOUCH_PX && other.rect.bottom < track.rect.top + radius + TOUCH_PX) {
					track.drawRect.top = min(track.drawRect.top, other.rect.bottom - radius)
				}
			}
		}
	}

	class Handle {
		var start: RectF? = null
			internal set
		var end: RectF? = null
			internal set
	}

	class Builder(private val radius: Float, private val viewportTop: Float, private val viewportBottom: Float) {

		private class Element(
			val startSection: String?,
			val endSection: String?,
			val start: RectF?,
			val end: RectF?,
			val delay: Long,
			val duration: Long,
			val handle: Handle
		)

		private val elements = mutableListOf<Element>()
		private val openBefore = mutableMapOf<String, OpenEdges>()
		private val openAfter = mutableMapOf<String, OpenEdges>()

		fun element(startSection: String?, start: RectF?, endSection: String?, end: RectF?, delay: Long, duration: Long): Handle {
			val handle = Handle()
			val startSide = startSection?.takeIf { start != null }
			val endSide = endSection?.takeIf { end != null }
			if (startSide == null && endSide == null) return handle
			elements += Element(startSide, endSide, start?.takeIf { startSide != null }?.let { RectF(it) },
				end?.takeIf { endSide != null }?.let { RectF(it) }, delay, duration, handle)
			return handle
		}

		fun openEdgesBefore(section: String, open: OpenEdges) {
			openBefore[section] = open
		}

		fun openEdgesAfter(section: String, open: OpenEdges) {
			openAfter[section] = open
		}

		fun build(): GallerySectionCardTracks? {
			if (elements.isEmpty()) return null
			val withStart = elements.filter { it.startSection != null }
			val withEnd = elements.filter { it.endSection != null }
			fun adoptEnd(startSection: String) = withEnd.filter { it.startSection == startSection }
				.groupingBy { it.endSection!! }.eachCount().maxByOrNull { it.value }?.key
			fun adoptStart(endSection: String) = withStart.filter { it.endSection == endSection }
				.groupingBy { it.startSection!! }.eachCount().maxByOrNull { it.value }?.key
			val resolved = elements.map { element ->
				when {
					element.startSection == null ->
						Element(adoptStart(element.endSection!!), element.endSection, null, element.end, element.delay, element.duration, element.handle)
					element.endSection == null ->
						Element(element.startSection, adoptEnd(element.startSection), element.start, null, element.delay, element.duration, element.handle)
					else -> element
				}
			}
			val tracks = resolved.groupBy { it.startSection to it.endSection }.mapNotNull { (key, members) ->
				val (startSection, endSection) = key
				val startBox = union(members.mapNotNull { it.start })
				val endBox = union(members.mapNotNull { it.end })
				if (startSection != null && endSection != null && startBox != null && endBox != null) {
					val contributions = members.map { member ->
						val start = member.start ?: project(member.end!!, startBox)
						val end = member.end ?: project(member.start!!, endBox)
						member.handle.start = RectF(start)
						member.handle.end = RectF(end)
						Contribution(start, end, member.delay, member.duration)
					}
					Track(startSection, endSection, contributions, startBox, endBox,
						openBefore[startSection] ?: OpenEdges.CLOSED, openAfter[endSection] ?: OpenEdges.CLOSED)
				} else if (startSection != null && startBox != null) {
					val open = openBefore[startSection] ?: OpenEdges.CLOSED
					Track(startSection, null, members.map { Contribution(it.start!!, it.start, it.delay, it.duration) },
						extended(startBox, open), null, open, OpenEdges.CLOSED)
				} else if (endSection != null && endBox != null) {
					val open = openAfter[endSection] ?: OpenEdges.CLOSED
					Track(null, endSection, members.map { Contribution(it.end!!, it.end, it.delay, it.duration) }, null,
						extended(endBox, open), OpenEdges.CLOSED, open)
				} else {
					null
				}
			}
			if (tracks.isEmpty()) return null
			for (track in tracks) {
				val startBox = track.startBox?.let { extended(it, track.openStart) } ?: continue
				val endBox = track.endBox?.let { extended(it, track.openEnd) } ?: continue
				val startSiblings = tracks.filter { it.startSection == track.startSection && it.startBox != null }
				val endSiblings = tracks.filter { it.endSection == track.endSection && it.endBox != null }
				track.startTopRound = startSiblings.none { extended(it.startBox!!, it.openStart).top < startBox.top - EDGE_PX }
				track.startBottomRound = startSiblings.none { extended(it.startBox!!, it.openStart).bottom > startBox.bottom + EDGE_PX }
				track.endTopRound = endSiblings.none { extended(it.endBox!!, it.openEnd).top < endBox.top - EDGE_PX }
				track.endBottomRound = endSiblings.none { extended(it.endBox!!, it.openEnd).bottom > endBox.bottom + EDGE_PX }
			}
			return GallerySectionCardTracks(tracks, radius, viewportTop, viewportBottom)
		}

		private fun extended(box: RectF, open: OpenEdges): RectF =
			RectF(box).also { GalleryMotion.extendOffscreen(it, open, viewportTop, viewportBottom, radius) }

		private fun union(rects: List<RectF>): RectF? {
			if (rects.isEmpty()) return null
			val result = RectF(rects[0])
			for (index in 1 until rects.size) result.union(rects[index])
			return result
		}

		private fun project(rect: RectF, box: RectF): RectF =
			RectF(box.left, rect.top.coerceIn(box.top, box.bottom), box.right, rect.bottom.coerceIn(box.top, box.bottom))
	}

	companion object {
		private const val TOUCH_PX = 1f
		private const val EDGE_PX = 0.5f

		private fun lerp(from: Float, to: Float, p: Float): Float = from + (to - from) * p

		private fun lerp(from: RectF, to: RectF, p: Float, out: RectF) {
			out.set(lerp(from.left, to.left, p), lerp(from.top, to.top, p), lerp(from.right, to.right, p), lerp(from.bottom, to.bottom, p))
		}

		private fun scaled(box: RectF, scale: Float, out: RectF) {
			val dx = box.width() * (1f - scale) / 2f
			val dy = box.height() * (1f - scale) / 2f
			out.set(box.left + dx, box.top + dy, box.right - dx, box.bottom - dy)
		}
	}
}
