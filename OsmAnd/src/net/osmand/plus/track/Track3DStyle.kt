package net.osmand.plus.track

import net.osmand.util.Algorithms

class Track3DStyle(
	var visualizationType: Gpx3DVisualizationType,
	var wallColorType: Gpx3DWallColorType,
	var linePositionType: Gpx3DLinePositionType,
	var additionalExaggeration: Float,
	var elevationMeters: Float
) {

	override fun equals(other: Any?): Boolean {
		return super.equals(other) && other is Track3DStyle
				&& visualizationType == other.visualizationType
				&& wallColorType == other.wallColorType
				&& linePositionType == other.linePositionType
				&& additionalExaggeration == other.additionalExaggeration
				&& elevationMeters == other.elevationMeters
	}

	override fun hashCode(): Int {
		return Algorithms.hash(
			visualizationType,
			wallColorType,
			linePositionType,
			additionalExaggeration,
			elevationMeters
		)
	}
}