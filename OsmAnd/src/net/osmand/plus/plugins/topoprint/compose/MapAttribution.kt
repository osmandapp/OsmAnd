package net.osmand.plus.plugins.topoprint.compose

import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin
import net.osmand.plus.plugins.srtm.SRTMPlugin

/**
 * Builds the printed map's data-source attribution. OsmAnd map content comes
 * from many sources (the authoritative list lives in the OsmAnd repository's
 * LICENSE file: OSM/ODbL, Geofabrik boundaries, GEBCO/NOAA depth models,
 * JAXA/MERIT/ArcticDEM/USGS/IGN elevation models, ...), so the print credits
 * what actually contributed to this render and defers everything else to
 * that list.
 *
 * The block is space-bounded by design: a hard cap on line count and line
 * length keeps it small on paper. Sources are grouped per category, never
 * enumerated exhaustively — the pointer line is always present, so nothing
 * is misattributed when more sources contributed than fit.
 */
object MapAttribution {

	/** The credit block shares the bottom margin with the scale bar and compass. */
	private const val MAX_LINES = 4
	private const val MAX_LINE_CHARS = 90

	private const val OSM_LINE =
		"Map data © OpenStreetMap contributors (ODbL) openstreetmap.org/copyright"
	private const val ELEVATION_LINE =
		"Elevation: ALOS, MERIT, ArcticDEM, Sonny, USGS 3DEP, IGN et al."
	private const val DEPTH_LINE =
		"Bathymetry: GEBCO, NOAA, BOEM, swisstopo et al."
	private const val POINTER_LINE =
		"Further sources: OsmAnd License — github.com/osmandapp/OsmAnd/blob/master/LICENSE"

	fun buildLines(app: OsmandApplication): List<String> {
		val lines = mutableListOf(OSM_LINE)
		val srtm = PluginsHelper.getPlugin(SRTMPlugin::class.java)
		if (srtm != null && srtm.isActive() && (SRTMPlugin.isContourLinesLayerEnabled(app)
					|| srtm.isTerrainLayerEnabled() || srtm.is3DMapsEnabled())) {
			// Contour lines, the hillshade/slope layer or 3D relief are on:
			// the render contains elevation-derived data.
			lines += ELEVATION_LINE
		}
		if (PluginsHelper.isActive(NauticalMapsPlugin::class.java)) {
			lines += DEPTH_LINE
		}
		lines += POINTER_LINE
		// Defensive caps — the hand-written lines above fit, but the block
		// must never grow past what the bottom margin can hold.
		return lines.take(MAX_LINES).map { it.take(MAX_LINE_CHARS) }
	}
}
