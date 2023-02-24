package net.osmand.plus.track;

import static net.osmand.router.RouteColorize.LIGHT_GREY;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.track.helpers.SelectedGpxFile;
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
	private boolean useJoinSegments;

	private final Map<GradientScaleType, List<TrkSegment>> nonSimplifiedSegmentsCache = new HashMap<>();
	private final Map<String, List<TrkSegment>> simplifiedSegmentsCache = new HashMap<>();
	private Set<String> availableColoringTypes;

	private final Map<Integer, List<RouteSegmentResult>> routeCache = new HashMap<>();

	private long prevModifiedTime = -1;

	public CachedTrack(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
		this.useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;
	}

	public List<RouteSegmentResult> getCachedRouteSegments(int nonEmptySegmentIdx) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		if (isCachedTrackChanged()) {
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

	@NonNull
	public List<TrkSegment> getAllNonSimplifiedCachedTrackSegments() {
		List<TrkSegment> result = new ArrayList<>();
		for (List<TrkSegment> segments : nonSimplifiedSegmentsCache.values()) {
			result.addAll(segments);
		}
		return result;
	}

	@NonNull
	public List<TrkSegment> getTrackSegments(@NonNull GradientScaleType scaleType) {
		if (isCachedTrackChanged()) {
			clearCaches();
		}

		List<TrkSegment> segments = nonSimplifiedSegmentsCache.get(scaleType);
		if (segments == null) {
			RouteColorize gpxColorization = createGpxColorization(scaleType);
			List<RouteColorizationPoint> colorsOfPoints = gpxColorization.getResult();
			segments = createColoredSegments(colorsOfPoints, scaleType);
			nonSimplifiedSegmentsCache.put(scaleType, segments);
		}

		return segments;
	}

	@NonNull
	public List<TrkSegment> getSimplifiedTrackSegments(int zoom, @NonNull GradientScaleType scaleType) {
		if (isCachedTrackChanged()) {
			clearCaches();
		}

		String trackId = zoom + "_" + scaleType;
		List<TrkSegment> segments = simplifiedSegmentsCache.get(trackId);
		if (segments == null) {
			RouteColorize gpxColorization = createGpxColorization(scaleType);
			List<RouteColorizationPoint> colorsOfPoints = gpxColorization.getSimplifiedResult(zoom);
			segments = createColoredSegments(colorsOfPoints, scaleType);
			simplifiedSegmentsCache.put(trackId, segments);
		}

		return segments;
	}

	private boolean isCachedTrackChanged() {
		boolean trackChanged = false;
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;
		boolean useJoinSegments = selectedGpxFile.isJoinSegments();
		if (useFilteredGpx != this.useFilteredGpx
				|| useJoinSegments != this.useJoinSegments
				|| prevModifiedTime != gpxFile.modifiedTime) {
			this.useFilteredGpx = useFilteredGpx;
			this.useJoinSegments = useJoinSegments;
			prevModifiedTime = gpxFile.modifiedTime;
			trackChanged = true;
		}
		return trackChanged;
	}

	@NonNull
	private RouteColorize createGpxColorization(@NonNull GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		GPXTrackAnalysis trackAnalysis = selectedGpxFile.getTrackAnalysisToDisplay(app);
		ColorizationType colorizationType = scaleType.toColorizationType();
		float maxSpeed = app.getSettings().getApplicationMode().getMaxSpeed();
		return new RouteColorize(gpxFile, trackAnalysis, colorizationType, maxSpeed);
	}

	@NonNull
	private List<TrkSegment> createColoredSegments(@NonNull List<RouteColorizationPoint> colorizationPoints,
	                                               @NonNull GradientScaleType scaleType) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean joinSegments = selectedGpxFile.isJoinSegments();

		List<TrkSegment> simplifiedSegments = new ArrayList<>();
		ColorizationType colorizationType = scaleType.toColorizationType();
		int id = 0;
		int colorPointIdx = 0;

		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		for (int i = 0; i < segments.size(); i++) {
			TrkSegment segment = segments.get(i);

			// Such segments are not processed by colorization
			if (segment.points.size() < 2) {
				continue;
			}

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
			if (joinSegments) {
				if (i + 1 < segments.size()) {
					simplifiedSegments.add(createStraightSegment(colorizationType, segments, i));
				}
			}
		}
		return simplifiedSegments;
	}

	@NonNull
	private TrkSegment createStraightSegment(ColorizationType colorizationType, List<TrkSegment> segments, int segIdx) {
		TrkSegment straightSegment = new TrkSegment();
		WptPt currentSegmentLastPoint = segments.get(segIdx).points.get(segments.get(segIdx).points.size() - 1);
		WptPt nextSegmentFirstPoint = segments.get(segIdx + 1).points.get(0);
		WptPt firstPoint = new WptPt(currentSegmentLastPoint);
		WptPt lastPoint = new WptPt(nextSegmentFirstPoint);
		firstPoint.setColor(colorizationType, LIGHT_GREY);
		lastPoint.setColor(colorizationType, LIGHT_GREY);
		straightSegment.points.add(firstPoint);
		straightSegment.points.add(lastPoint);
		return straightSegment;
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
		nonSimplifiedSegmentsCache.clear();
		simplifiedSegmentsCache.clear();
		routeCache.clear();
	}
}