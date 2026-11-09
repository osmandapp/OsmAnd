package net.osmand.plus.views.layers.geometry

import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.VectorLine
import net.osmand.core.jni.VectorLineArrowsProvider
import net.osmand.core.jni.VectorLinesCollection

/**
 * One collection of vector lines, with one arrows provider, for the geometry ways of all segments of a track.
 * A collection per segment gave the map renderer thousands of symbols providers for a track with thousands of
 * segments, and it looks providers up on every frame. Each way keeps its own line ids; the lines get ids unique
 * in the collection. The collection is removed from the renderer when the last way leaves it.
 */
class SharedVectorLines {

	var collection: VectorLinesCollection? = null
		private set
	private var priority = Long.MIN_VALUE
	private var arrowsProvider: VectorLineArrowsProvider? = null
	private var arrowsLinesChanged = false

	// Lines of each way in the collection by the line ids of the way; the owner of the lines is the drawer of the way
	private val linesOfOwners = HashMap<Any, MutableMap<Int, VectorLine>>()
	private var nextLineId = 1

	fun obtainCollection(mapRenderer: MapRendererView, volumetric: Boolean, priority: Long,
	                     owner: Any): VectorLinesCollection {
		var collection = collection
		if (collection == null || collection.hasVolumetricSymbols != volumetric
			|| !mapRenderer.hasSymbolsProvider(collection)) {
			release(mapRenderer)
			collection = VectorLinesCollection(volumetric)
			collection.setPriority(priority)
			mapRenderer.addSymbolsProvider(collection)
			this.collection = collection
			this.priority = priority
		}
		linesOfOwners.getOrPut(owner) { HashMap() }
		return collection
	}

	fun findLine(owner: Any, lineId: Int): VectorLine? = linesOfOwners[owner]?.get(lineId)

	fun newLineId(): Int = nextLineId++

	fun addLine(owner: Any, lineId: Int, line: VectorLine) {
		linesOfOwners.getOrPut(owner) { HashMap() }[lineId] = line
		arrowsLinesChanged = true
	}

	fun removeLines(mapRenderer: MapRendererView, owner: Any) {
		val lines = linesOfOwners.remove(owner) ?: return
		val collection = collection
		if (collection != null && lines.isNotEmpty()) {
			for (line in lines.values) {
				collection.removeLine(line)
			}
			arrowsLinesChanged = true
		}
		if (linesOfOwners.isEmpty()) {
			release(mapRenderer)
		}
	}

	/** The arrows provider sees only the lines it was created with, so it is created again when they change. */
	fun updateArrows(mapRenderer: MapRendererView, showArrows: Boolean) {
		val collection = collection
		val provider = arrowsProvider
		if (collection == null || !showArrows) {
			if (provider != null) {
				mapRenderer.removeSymbolsProvider(provider)
				arrowsProvider = null
			}
		} else if (provider == null || arrowsLinesChanged) {
			if (provider != null) {
				mapRenderer.removeSymbolsProvider(provider)
			}
			val newProvider = VectorLineArrowsProvider(collection)
			newProvider.setPriority(priority)
			mapRenderer.addSymbolsProvider(newProvider)
			arrowsProvider = newProvider
			arrowsLinesChanged = false
		}
	}

	fun release(mapRenderer: MapRendererView?) {
		val provider = arrowsProvider
		if (provider != null) {
			mapRenderer?.removeSymbolsProvider(provider)
			arrowsProvider = null
		}
		val collection = collection
		if (collection != null) {
			mapRenderer?.removeSymbolsProvider(collection)
			this.collection = null
		}
		linesOfOwners.clear()
		nextLineId = 1
		arrowsLinesChanged = false
	}
}
