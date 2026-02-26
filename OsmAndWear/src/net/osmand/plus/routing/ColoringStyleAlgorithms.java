package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.render.MapRenderRepositories;

import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteExporter;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;
import net.osmand.shared.routing.ColoringType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColoringStyleAlgorithms {

	public static boolean isAvailableForDrawingRoute(@NonNull OsmandApplication app,
	                                                 @NonNull ColoringStyle coloringStyle,
	                                                 @NonNull RouteCalculationResult route) {
		ColoringType coloringType = coloringStyle.getType();
		String attributeName = coloringStyle.getRouteInfoAttribute();
		if (coloringType.isGradient()) {
			List<Location> locations = route.getImmutableAllLocations();
			for (Location location : locations) {
				if (location.hasAltitude()) {
					return true;
				}
			}
			return false;
		}
		if (coloringType.isRouteInfoAttribute()) {
			return !Algorithms.isEmpty(route.getOriginalRoute())
					&& isAttributeAvailableForDrawing(app, route.getOriginalRoute(), attributeName);
		}
		return true;
	}

	public static boolean isAvailableForDrawingTrack(@NonNull OsmandApplication app,
	                                                 @NonNull ColoringStyle coloringStyle,
	                                                 @NonNull SelectedGpxFile selectedGpxFile) {
		ColoringType coloringType = coloringStyle.getType();
		String attributeName = coloringStyle.getRouteInfoAttribute();
		if (coloringType.isGradient()) {
			GradientScaleType scaleType = coloringType.toGradientScaleType();
			GpxTrackAnalysis analysis = selectedGpxFile.getTrackAnalysisToDisplay(app);
			if (analysis != null && scaleType != null) {
				return analysis.isColorizationTypeAvailable(scaleType.toColorizationType());
			}
		}
		if (coloringType.isRouteInfoAttribute()) {
			List<RouteSegmentResult> routeSegments = getRouteSegmentsInTrack(selectedGpxFile.getGpxFile());
			if (Algorithms.isEmpty(routeSegments)) {
				return false;
			}
			return isAttributeAvailableForDrawing(app, routeSegments, attributeName);
		}
		return true;
	}

	public static boolean isAvailableInSubscription(@NonNull OsmandApplication app,
	                                                @NonNull ColoringStyle coloringStyle) {
		return isAvailableInSubscription(app, coloringStyle, false);
	}

	public static boolean isAvailableInSubscription(@NonNull OsmandApplication app,
	                                                @NonNull ColoringStyle coloringStyle,
	                                                boolean useForRoute) {
		ColoringType coloringType = coloringStyle.getType();
		if ((coloringType.isRouteInfoAttribute() && useForRoute) || coloringType == ColoringType.SLOPE) {
			return InAppPurchaseUtils.isColoringTypeAvailable(app);
		}
		return true;
	}

	public static boolean isAttributeAvailableForDrawing(@NonNull OsmandApplication app,
	                                                     @NonNull List<RouteSegmentResult> routeSegments,
	                                                     @Nullable String attributeName) {
		if (Algorithms.isEmpty(routeSegments) || Algorithms.isEmpty(attributeName)) {
			return false;
		}

		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		List<String> rendererAttrs = RouteStatisticsHelper
				.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);
		if (Algorithms.isEmpty(rendererAttrs) || !rendererAttrs.contains(attributeName)) {
			return false;
		}

		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, night);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRenderer, night);

		List<RouteStatistics> routeStatisticsList =
				RouteStatisticsHelper.calculateRouteStatistic(routeSegments,
						Collections.singletonList(attributeName), currentRenderer,
						defaultRenderer, currentSearchRequest, defaultSearchRequest);
		return !Algorithms.isEmpty(routeStatisticsList);
	}

	@Nullable
	public static List<RouteSegmentResult> getRouteSegmentsInTrack(@NonNull GpxFile gpxFile) {
		if (!RouteExporter.OSMAND_ROUTER_V2.equals(gpxFile.getAuthor())) {
			return null;
		}
		List<RouteSegmentResult> routeSegments = new ArrayList<>();
		for (int i = 0; i < gpxFile.getNonEmptyTrkSegments(false).size(); i++) {
			TrkSegment segment = gpxFile.getNonEmptyTrkSegments(false).get(i);
			if (segment.hasRoute()) {
				routeSegments.addAll(RouteProvider.parseOsmAndGPXRoute(new ArrayList<>(), gpxFile, new ArrayList<>(), i));
			}
		}
		return routeSegments;
	}
}
