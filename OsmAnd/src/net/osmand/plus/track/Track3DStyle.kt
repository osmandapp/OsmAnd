package net.osmand.plus.track

import net.osmand.util.Algorithms

class Track3DStyle(
	var visualizationType: Gpx3DVisualizationType,
	var wallColorType: Gpx3DWallColorType,
	var linePositionType: Gpx3DLinePositionType,
	var exaggeration: Float,
	var elevation: Float
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