package net.osmand.plus.plugins.odb

import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.obd.OBDCommand
import net.osmand.util.Algorithms

class VehicleTrackPointsAnalyser : GpxTrackAnalysis.TrackPointsAnalyser {

    override fun onAnalysePoint(
        analysis: GpxTrackAnalysis,
        point: WptPt,
        attribute: PointAttributes
    ) {
        OBDCommand.entries
            .mapNotNull { it.gpxTag }
            .forEach { tag ->
                val value = getPointAttribute(point, tag)
                attribute.setAttributeValue(tag, value)

                if (!analysis.hasData(tag) && attribute.hasValidValue(tag)) {
                    analysis.setHasData(tag, true)
                }
            }
    }

    private fun getPointAttribute(wptPt: WptPt, key: String): Float {
        var value = wptPt.getDeferredExtensionsToRead()[key]
        if (Algorithms.isEmpty(value)) {
            value = wptPt.getExtensionsToRead()[key]
        }
        return Algorithms.parseFloatSilently(value, 0f)
    }
}