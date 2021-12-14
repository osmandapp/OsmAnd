package net.osmand.plus.track;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.router.RouteColorize.RouteColorizationPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CachedTrack {

	private final OsmandApplication app;

	private final SelectedGpxFile selectedGpxFile;
	private boolean useFilteredGpx;

	private final Map<String, List<TrkSegment>> segmentsCache = new HashMap<>();
	private Set<String> availableColoringTypes = null;

	private final Map<Integer, List<RouteSegmentResult>> routeCache = new HashMap<>();

	private long prevModifiedTime = -1;

	public CachedTrack(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
		this.useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;
	}

	public List<RouteSegmentResult> getCachedRouteSegments(int nonEmptySegmentIdx) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;

		if (useFilteredGpx != this.useFilteredGpx || prevModifiedTime != gpxFile.modifiedTime) {
			this.useFilteredGpx = useFilteredGpx;
			prevModifiedTime = gpxFile.modifiedTime;
			clearCaches();
			List<RouteSegmentResult> routeSegments = RouteProvider.parseOsmAndGPXRoute(new ArrayList<>(),
					gpxFile, new ArrayList<>(), nonEmptySegmentIdx);
			routeCache.put(nonEmptySegmentIdx, routeSegments);
			return routeSegments;
		} else {
			List<RouteSegmentResult> routeSegments = routeCache.get(nonEmptySegmentIdx);
			if (routeSegments == null) {
				routeSegments = RouteProvider.parseOsmAndGPXRoute(new ArrayList<>(), gpxFile,
						new ArrayList<>(), nonEmptySegmentIdx);
				routeCache.put(nonEmptySegmentIdx, routeSegments);
			}
			return routeSegments;
		}
	}

	public List<TrkSegment> getCachedTrackSegments(int zoom, @NonNull GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;
		String trackId = zoom + "_" + scaleType.toString();

		if (useFilteredGpx != this.useFilteredGpx || prevModifiedTime != gpxFile.modifiedTime) {
			this.useFilteredGpx = useFilteredGpx;
			prevModifiedTime = gpxFile.modifiedTime;
			clearCaches();
			List<TrkSegment> segments = calculateGradientTrack(zoom, scaleType);
			segmentsCache.put(trackId, segments);
			return segments;
		} else {
			List<TrkSegment> segments = segmentsCache.get(trackId);
			if (segments == null) {
				segments = calculateGradientTrack(zoom, scaleType);
				segmentsCache.put(trackId, segments);
			}
			return segments;
		}
	}

	@NonNull
	private List<TrkSegment> calculateGradientTrack(int zoom, @NonNull GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		GPXTrackAnalysis trackAnalysis = selectedGpxFile.getTrackAnalysisToDisplay(app);
		ColorizationType colorizationType = scaleType.toColorizationType();
		float maxSpeed = app.getSettings().getApplicationMode().getMaxSpeed();
		RouteColorize colorize = new RouteColorize(zoom, gpxFile, trackAnalysis, colorizationType, maxSpeed);
		List<RouteColorizationPoint> colorsOfPoints = colorize.getResult(true);
		return createSimplifiedSegments(gpxFile, colorsOfPoints, scaleType);
	}

	@NonNull
	private List<TrkSegment> createSimplifiedSegments(@NonNull GPXFile gpxFile,
	                                                  @NonNull List<RouteColorizationPoint> colorizationPoints,
	                                                  @NonNull GradientScaleType scaleType) {
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

	public boolean isColoringTypeAvailable(@NonNull ColoringType coloringType, @Nullable String routeInfoAttribute) {
		if (prevModifiedTime != selectedGpxFile.getGpxFileToDisplay().modifiedTime || availableColoringTypes == null) {
			availableColoringTypes = listAvailableColoringTypes();
		}
		return availableColoringTypes.contains(coloringType.getName(routeInfoAttribute));
	}

	@NonNull
	private Set<String> listAvailableColoringTypes() {
		Set<String> availableColoringTypes = new HashSet<>();
		availableColoringTypes.addAll(listAvailableStaticColoringTypes());
		availableColoringTypes.addAll(listAvailableRouteInfoAttributes());
		return availableColoringTypes;
	}

	@NonNull
	private Set<String> listAvailableStaticColoringTypes() {
		Set<String> availableStaticTypes = new HashSet<>();
		for (ColoringType coloringType : ColoringType.getTrackColoringTypes()) {
			if (!coloringType.isRouteInfoAttribute()
					&& coloringType.isAvailableForDrawingTrack(app, selectedGpxFile, null)) {
				availableStaticTypes.add(coloringType.getName(null));
			}
		}
		return availableStaticTypes;
	}

	@NonNull
	private Set<String> listAvailableRouteInfoAttributes() {
		Set<String> availableRouteInfoAttributes = new HashSet<>();
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		List<String> rendererRouteInfoAttribute =
				RouteStatisticsHelper.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);

		for (String routeInfoAttribute : rendererRouteInfoAttribute) {
			if (ColoringType.ALTITUDE.isAvailableForDrawingTrack(app, selectedGpxFile, routeInfoAttribute)) {
				availableRouteInfoAttributes.add(routeInfoAttribute);
			}
		}

		return availableRouteInfoAttributes;
	}

	private void clearCaches() {
		segmentsCache.clear();
		routeCache.clear();
	}
}