package net.osmand.plus.track

import net.osmand.shared.routing.Gpx3DWallColorType
import net.osmand.util.Algorithms

class Track3DStyle(
	val visualizationType: Gpx3DVisualizationType,
	val wallColorType: Gpx3DWallColorType,
	val linePositionType: Gpx3DLinePositionType,
	val exaggeration: Float,
	val elevation: Float
) {

	override fun equals(other: Any?): Boolean {
		return other is Track3DStyle
				&& visualizationType == other.visualizationType
				&& wallColorType == other.wallColorType
				&& linePositionType == other.linePositionType
				&& exaggeration == other.exaggeration
				&& elevation == other.elevation
	}

	override fun hashCode(): Int {
		return Algorithms.hash(
			visualizationType,
			wallColorType,
			linePositionType,
			exaggeration,
			elevation
		)
	}

	override fun toString(): String {
		return "Track3DStyle { visualization $visualizationType wallColor $wallColorType linePosition " +
				"$linePositionType exaggeration $exaggeration elevation $elevation}"
	}
}