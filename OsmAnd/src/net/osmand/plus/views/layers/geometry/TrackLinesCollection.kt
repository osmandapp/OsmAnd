package net.osmand.plus.views.layers.geometry

import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.VectorLinesCollection

/**
 * One vector lines collection for all segments of a track. A collection per segment made the renderer look its
 * providers up among thousands of others on every frame. Each segment keeps its own range of line ids here.
 */
class TrackLinesCollection {

	var collection: VectorLinesCollection? = null
		private set

	private var nextLineId = 1

	fun allocateLineIds(): Int {
		val first = nextLineId
		nextLineId += LINE_IDS_PER_SEGMENT
		return first
	}

	fun obtain(mapRenderer: MapRendererView, volumetric: Boolean, priority: Long): VectorLinesCollection {
		val current = collection
		if (current != null && current.hasVolumetricSymbols == volumetric
			&& mapRenderer.hasSymbolsProvider(current)) {
			return current
		}
		if (current != null && mapRenderer.hasSymbolsProvider(current)) {
			mapRenderer.removeSymbolsProvider(current)
		}
		val created = VectorLinesCollection(volumetric)
		created.setPriority(priority)
		mapRenderer.addSymbolsProvider(created)
		collection = created
		return created
	}

	fun reset(mapRenderer: MapRendererView?) {
		val current = collection
		if (current != null && mapRenderer != null && mapRenderer.hasSymbolsProvider(current)) {
			mapRenderer.removeSymbolsProvider(current)
		}
		collection = null
	}

	companion object {
		const val LINE_IDS_PER_SEGMENT = 1024
	}
}
