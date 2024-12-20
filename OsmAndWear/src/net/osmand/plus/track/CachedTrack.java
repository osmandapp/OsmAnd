package net.osmand.plus.track;

import static net.osmand.ColorPalette.LIGHT_GREY;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableForDrawingTrack;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.ColorPalette;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.routing.RouteColorize;
import net.osmand.shared.routing.RouteColorize.ColorizationType;
import net.osmand.shared.routing.RouteColorize.RouteColorizationPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CachedTrack {

	private final OsmandApplication app;

	private final SelectedGpxFile selectedGpxFile;

	private final Map<Integer, List<RouteSegmentResult>> routeCache = new ConcurrentHashMap<>();
	private final Map<String, List<TrkSegment>> simplifiedSegmentsCache = new HashMap<>();
	private final Map<String, List<TrkSegment>> nonSimplifiedSegmentsCache = new HashMap<>();
	private Set<String> availableColoringTypes;

	private CachedTrackParams params;

	public CachedTrack(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
		this.app = app;
		this.selectedGpxFile = selectedGpxFile;
		this.params = new CachedTrackParams(-1, selectedGpxFile.getFilteredSelectedGpxFile() != null, false);
	}

	@NonNull
	public SelectedGpxFile getSelectedGpxFile() {
		return selectedGpxFile;
	}

	@NonNull
	public CachedTrackParams getCachedTrackParams() {
		return params;
	}

	@Nullable
	public List<RouteSegmentResult> getCachedRouteSegments(int nonEmptySegmentIdx) {
		if (isCachedTrackChanged()) {
			clearCaches();
		}
		return routeCache.get(nonEmptySegmentIdx);
	}

	public void setCachedRouteSegments(@NonNull List<RouteSegmentResult> routeSegments, int nonEmptySegmentIdx) {
		routeCache.put(nonEmptySegmentIdx, routeSegments);
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
	public List<TrkSegment> getTrackSegments(@Nullable GradientScaleType scaleType,
	                                         @Nullable GradientScaleType outlineScaleType,
	                                         @NonNull String palette) {
		if (isCachedTrackChanged()) {
			clearCaches();
		}

		String trackId = scaleType + "_" + palette + "_" + outlineScaleType;
		List<TrkSegment> segments = nonSimplifiedSegmentsCache.get(trackId);
		if (segments == null) {
			RouteColorize colorization = scaleType != null ? createGpxColorization(scaleType, palette) : null;
			RouteColorize outlineColorization = outlineScaleType != null ? createGpxColorization(outlineScaleType, palette) : null;

			Pair<GradientScaleType, List<RouteColorizationPoint>> lineColors = null;
			Pair<GradientScaleType, List<RouteColorizationPoint>> outlineColors = null;
			if (colorization != null) {
				lineColors = new Pair<>(scaleType, colorization.getResult());
			}
			if (outlineColorization != null) {
				outlineColors = new Pair<>(outlineScaleType, outlineColorization.getResult());
			}
			segments = createColoredSegments(lineColors, outlineColors);
			nonSimplifiedSegmentsCache.put(trackId, segments);
		}

		return segments;
	}

	@NonNull
	public List<TrkSegment> getSimplifiedTrackSegments(int zoom, @NonNull GradientScaleType scaleType, @NonNull String palette) {
		if (isCachedTrackChanged()) {
			clearCaches();
		}

		String trackId = zoom + "_" + scaleType + "_" + palette;
		List<TrkSegment> segments = simplifiedSegmentsCache.get(trackId);
		if (segments == null) {
			RouteColorize colorization = createGpxColorization(scaleType, palette);
			List<RouteColorizationPoint> colorsOfPoints = colorization.getSimplifiedResult(zoom);
			segments = createColoredSegments(Pair.create(scaleType, colorsOfPoints), null);
			simplifiedSegmentsCache.put(trackId, segments);
		}

		return segments;
	}

	private boolean isCachedTrackChanged() {
		GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean useJoinSegments = selectedGpxFile.isJoinSegments();
		boolean useFilteredGpx = selectedGpxFile.getFilteredSelectedGpxFile() != null;
		if (useFilteredGpx != params.useFilteredGpx
				|| useJoinSegments != params.useJoinSegments
				|| gpxFile.getModifiedTime() != params.prevModifiedTime) {
			params = new CachedTrackParams(gpxFile.getModifiedTime(), useFilteredGpx, useJoinSegments);
			return true;
		}
		return false;
	}

	@NonNull
	private RouteColorize createGpxColorization(@NonNull GradientScaleType scaleType, @NonNull String gradientPalette) {
		GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		GpxTrackAnalysis trackAnalysis = selectedGpxFile.getTrackAnalysisToDisplay(app);
		ColorizationType colorizationType = scaleType.toColorizationType();
		float maxSpeed = app.getSettings().getApplicationMode().getMaxSpeed();
		ColorPalette colorPalette = app.getColorPaletteHelper().getGradientColorPaletteSync(colorizationType, gradientPalette);

		return new RouteColorize(gpxFile, trackAnalysis, colorizationType, colorPalette, maxSpeed);
	}

	@NonNull
	private List<TrkSegment> createColoredSegments(@Nullable Pair<GradientScaleType, List<RouteColorizationPoint>> lineColors,
	                                               @Nullable Pair<GradientScaleType, List<RouteColorizationPoint>> outlineColors) {
		GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		boolean joinSegments = selectedGpxFile.isJoinSegments();
		ColorizationType colorizationType = lineColors != null ? lineColors.first.toColorizationType() : null;
		ColorizationType outlineColorizationType = outlineColors != null ? outlineColors.first.toColorizationType() : null;

		List<TrkSegment> simplifiedSegments = new ArrayList<>();
		int id = 0;
		int colorPointIdx = 0;
		int size = Math.max(lineColors != null ? lineColors.second.size() : 0, outlineColors != null ? outlineColors.second.size() : 0);

		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		for (int i = 0; i < segments.size(); i++) {
			TrkSegment segment = segments.get(i);

			// Such segments are not processed by colorization
			if (segment.getPoints().size() < 2) {
				continue;
			}

			TrkSegment simplifiedSegment = new TrkSegment();
			simplifiedSegments.add(simplifiedSegment);
			for (WptPt pt : segment.getPoints()) {
				if (colorPointIdx >= size) {
					return simplifiedSegments;
				}
				RouteColorizationPoint point = lineColors != null && lineColors.second.size() > colorPointIdx ? lineColors.second.get(colorPointIdx) : null;
				RouteColorizationPoint outlinePoint = outlineColors != null && outlineColors.second.size() > colorPointIdx ? outlineColors.second.get(colorPointIdx) : null;
				if (point != null && point.getId() == id || outlinePoint != null && outlinePoint.getId() == id) {
					simplifiedSegment.getPoints().add(pt);
					if (point != null) {
						pt.setColor(colorizationType, point.getPrimaryColor());
					}
					if (outlinePoint != null) {
						pt.setColor(outlineColorizationType, outlinePoint.getPrimaryColor());
					}
					colorPointIdx++;
				}
				id++;
			}
			if (joinSegments) {
				if (i + 1 < segments.size()) {
					simplifiedSegments.add(createStraightSegment(colorizationType, outlineColorizationType, segments, i));
				}
			}
		}
		return simplifiedSegments;
	}

	@NonNull
	private TrkSegment createStraightSegment(@Nullable ColorizationType colorizationType,
	                                         @Nullable ColorizationType outlineColorizationType,
	                                         @NonNull List<TrkSegment> segments, int segIdx) {
		TrkSegment straightSegment = new TrkSegment();
		WptPt currentSegmentLastPoint = segments.get(segIdx).getPoints().get(segments.get(segIdx).getPoints().size() - 1);
		WptPt nextSegmentFirstPoint = segments.get(segIdx + 1).getPoints().get(0);
		WptPt firstPoint = new WptPt(currentSegmentLastPoint);
		WptPt lastPoint = new WptPt(nextSegmentFirstPoint);
		firstPoint.setColor(colorizationType, ColorPalette.Companion.getLIGHT_GREY());
		lastPoint.setColor(colorizationType, ColorPalette.Companion.getLIGHT_GREY());
		firstPoint.setColor(outlineColorizationType, LIGHT_GREY);
		lastPoint.setColor(outlineColorizationType, LIGHT_GREY);
		firstPoint.setColor(outlineColorizationType, LIGHT_GREY);
		lastPoint.setColor(outlineColorizationType, LIGHT_GREY);
		straightSegment.getPoints().add(firstPoint);
		straightSegment.getPoints().add(lastPoint);
		return straightSegment;
	}

	public boolean isColoringTypeAvailable(@NonNull ColoringType coloringType, @Nullable String routeInfoAttribute) {
		if (params.prevModifiedTime != selectedGpxFile.getGpxFileToDisplay().getModifiedTime() || availableColoringTypes == null) {
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
		for (ColoringType coloringType : ColoringType.Companion.valuesOf(ColoringPurpose.TRACK)) {
			if (!coloringType.isRouteInfoAttribute()
					&& isAvailableForDrawingTrack(app, new ColoringStyle(coloringType), selectedGpxFile)) {
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
			ColoringStyle coloringStyle = new ColoringStyle(ColoringType.ALTITUDE, routeInfoAttribute);
			if (isAvailableForDrawingTrack(app, coloringStyle, selectedGpxFile)) {
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