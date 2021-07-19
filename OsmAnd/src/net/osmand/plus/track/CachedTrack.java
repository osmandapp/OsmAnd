package net.osmand.plus.track;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.router.RouteColorize.RouteColorizationPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

public class CachedTrack {

	private final OsmandApplication app;

	private final SelectedGpxFile selectedGpxFile;
	private final Map<String, List<TrkSegment>> cache = new HashMap<>();
	private Set<GradientScaleType> availableScaleTypes = null;

	private long prevModifiedTime = -1;

	public CachedTrack(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
	}

	public List<TrkSegment> getCachedSegments(int zoom, @NonNull GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		String trackId = zoom + "_" + scaleType.toString();
		if (prevModifiedTime == gpxFile.modifiedTime) {
			List<TrkSegment> segments = cache.get(trackId);
			if (segments == null) {
				segments = calculateGradientTrack(selectedGpxFile, zoom, scaleType);
				cache.put(trackId, segments);
			}
			return segments;
		} else {
			cache.clear();
			prevModifiedTime = gpxFile.modifiedTime;
			List<TrkSegment> segments = calculateGradientTrack(selectedGpxFile, zoom, scaleType);
			cache.put(trackId, segments);
			return segments;
		}
	}

	private List<TrkSegment> calculateGradientTrack(SelectedGpxFile selectedGpxFile, int zoom,
													GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		RouteColorize colorize = new RouteColorize(zoom, gpxFile, selectedGpxFile.getTrackAnalysis(app),
				scaleType.toColorizationType(), app.getSettings().getApplicationMode().getMaxSpeed());
		List<RouteColorizationPoint> colorsOfPoints = colorize.getResult(true);
		return createSimplifiedSegments(selectedGpxFile.getGpxFile(), colorsOfPoints, scaleType);
	}

	private List<TrkSegment> createSimplifiedSegments(GPXFile gpxFile,
													  List<RouteColorizationPoint> colorizationPoints,
													  GradientScaleType scaleType) {
		List<TrkSegment> simplifiedSegments = new ArrayList<>();
		ColorizationType colorizationType = scaleType.toColorizationType();
		int id = 0;
		int colorPointIdx = 0;

		for (TrkSegment segment : gpxFile.getNonEmptyTrkSegments(false)) {
			TrkSegment simplifiedSegment = new TrkSegment();
			simplifiedSegments.add(simplifiedSegment);
			for (WptPt pt : segment.points) {
				if (colorPointIdx >= colorizationPoints.size()) {
					return simplifiedSegments;
				}
				RouteColorizationPoint colorPoint = colorizationPoints.get(colorPointIdx);
				if (colorPoint.id == id) {
					simplifiedSegment.points.add(pt);
					pt.setColor(colorizationType, colorPoint.color);
					colorPointIdx++;
				}
				id++;
			}
		}

		return simplifiedSegments;
	}

	public boolean isScaleTypeAvailable(@NonNull GradientScaleType scaleType) {
		if (prevModifiedTime != selectedGpxFile.getGpxFile().modifiedTime || availableScaleTypes == null) {
			defineAvailableScaleTypes();
		}
		return availableScaleTypes.contains(scaleType);
	}

	private void defineAvailableScaleTypes() {
		GPXTrackAnalysis analysis = selectedGpxFile.getTrackAnalysis(app);
		availableScaleTypes = new HashSet<>();
		if (analysis.isColorizationTypeAvailable(GradientScaleType.SPEED.toColorizationType())) {
			availableScaleTypes.add(GradientScaleType.SPEED);
		}
		if (analysis.isColorizationTypeAvailable(GradientScaleType.ALTITUDE.toColorizationType())) {
			availableScaleTypes.add(GradientScaleType.ALTITUDE);
			availableScaleTypes.add(GradientScaleType.SLOPE);
		}
	}
}