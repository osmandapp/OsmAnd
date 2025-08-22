package net.osmand.plus.onlinerouting.engine;

import static net.osmand.plus.onlinerouting.engine.EngineType.GPX_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.LocationsHolder;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingEnvironment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GpxEngine extends OnlineRoutingEngine {

	private static final String ONLINE_ROUTING_GPX_FILE_NAME = "online_routing_gpx";

	public GpxEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return GPX_TYPE;
	}

	@Override
	@NonNull
	public String getTitle() {
		return "GPX";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "GPX";
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing) {
		if (sb.indexOf("?") >= 0) {
			if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&') {
				sb.append('&');
			}
		} else {
			sb.append('?');
		}
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude());
			if (i < path.size() - 1) {
				sb.append('&');
			}
		}
		if (startBearing != null) {
			if (sb.charAt(sb.length() - 1) != '?') {
				sb.append('&');
			}
			sb.append("heading=").append(startBearing.intValue());
		}
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "";
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {

	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
		params.add(EngineParameter.APPROXIMATION_ROUTING_PROFILE);
		params.add(EngineParameter.APPROXIMATION_DERIVED_PROFILE);
		params.add(EngineParameter.USE_EXTERNAL_TIMESTAMPS);
		params.add(EngineParameter.USE_ROUTING_FALLBACK);
	}

	@Override
	public void updateRouteParameters(@NonNull RouteCalculationParams params, @Nullable RouteCalculationResult previousRoute) {
		super.updateRouteParameters(params, previousRoute);
		if ((previousRoute == null || previousRoute.isEmpty()) && shouldApproximateRoute()) {
			params.initialCalculation = true;
		}
		if (previousRoute != null && previousRoute.isInitialCalculation()) {
			params.gpxFile = previousRoute.getGpxFile(); // catch gpx from 1st phase
		}
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new GpxEngine(params);
	}

	@Override
	@Nullable
	public OnlineRoutingResponse responseByContent(@NonNull OsmandApplication app, @NonNull String content,
	                                               boolean leftSideNavigation, boolean initialCalculation,
	                                               @Nullable RouteCalculationProgress calculationProgress) {
		GpxFile gpxFile = parseGpx(content);
		return gpxFile != null ? responseByGpxFile(app, gpxFile, initialCalculation, calculationProgress) : null;
	}

	public OnlineRoutingResponse responseByGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                               boolean initialCalculation, @Nullable RouteCalculationProgress calculationProgress) {
		boolean[] calculatedTimeSpeed = new boolean[]{useExternalTimestamps()};
		if (shouldApproximateRoute() && !initialCalculation) {
			GpxFile approximated = approximateGpxFile(app, gpxFile, calculationProgress, calculatedTimeSpeed);
			if (approximated != null) {
				gpxFile = approximated;
			}
		}
		return new OnlineRoutingResponse(gpxFile, calculatedTimeSpeed[0]);
	}

	@Nullable
	private GpxFile approximateGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                   @Nullable RouteCalculationProgress calculationProgress,
	                                   boolean[] calculatedTimeSpeed) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		ApplicationMode appMode = routingHelper.getAppMode();
		String oldRoutingProfile = appMode.getRoutingProfile();
		String oldDerivedProfile = appMode.getDerivedProfile();
		try {
			String routingProfile = getApproximationRoutingProfile();
			if (routingProfile != null) {
				appMode.setRoutingProfile(routingProfile);
				appMode.setDerivedProfile(getApproximationDerivedProfile());
			}
			List<WptPt> points = gpxFile.getAllSegmentsPoints();
			LocationsHolder holder = new LocationsHolder(SharedUtil.jWptPtList(points));
			if (holder.getSize() > 1) {
				LatLon start = holder.getLatLon(0);
				LatLon end = holder.getLatLon(holder.getSize() - 1);
				RoutingEnvironment env = routingHelper.getRoutingEnvironment(app, appMode, start, end);
				GpxRouteApproximation gctx = new GpxRouteApproximation(env.getCtx());
				gctx.ctx.calculationProgress = calculationProgress;
				List<GpxPoint> gpxPoints = routingHelper.generateGpxPoints(env, gctx, holder);
				GpxRouteApproximation gpxApproximation = routingHelper.calculateGpxApproximation(env, gctx, gpxPoints, null, calculatedTimeSpeed[0]);
				MeasurementEditingContext ctx = new MeasurementEditingContext(app);
				ctx.setPoints(gpxApproximation, points, appMode, calculatedTimeSpeed[0]);
				calculatedTimeSpeed[0] = ctx.hasCalculatedTimeSpeed();
				return ctx.exportGpx(ONLINE_ROUTING_GPX_FILE_NAME);
			}
		} catch (IOException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			appMode.setRoutingProfile(oldRoutingProfile);
			appMode.setDerivedProfile(oldDerivedProfile);
		}
		return null;
	}


	@Override
	public boolean isResultOk(@NonNull StringBuilder errorMessage,
	                          @NonNull String content) {
		return parseGpx(content) != null;
	}

	@Nullable
	private GpxFile parseGpx(@NonNull String content) {
		InputStream gpxStream;
		try {
			gpxStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			return SharedUtil.loadGpxFile(gpxStream);
		} catch (UnsupportedEncodingException e) {
			LOG.debug("Error when parsing GPX from server response: " + e.getMessage());
		}
		return null;
	}
}
