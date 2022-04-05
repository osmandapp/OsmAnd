package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
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
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.onlinerouting.engine.EngineType.GPX_TYPE;

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
		sb.append("?");
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
		params.add(EngineParameter.APPROXIMATE_ROUTE);
		params.add(EngineParameter.USE_EXTERNAL_TIMESTAMPS);
		params.add(EngineParameter.USE_ROUTING_FALLBACK);
	}

	@Override
	public void updateRouteParameters(@NonNull RouteCalculationParams params, @Nullable RouteCalculationResult previousRoute) {
		super.updateRouteParameters(params, previousRoute);
		if ((previousRoute == null || previousRoute.isEmpty()) && shouldApproximateRoute()) {
			params.initialCalculation = true;
		}
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new GpxEngine(params);
	}

	@Override
	@Nullable
	public OnlineRoutingResponse parseResponse(@NonNull String content, @NonNull OsmandApplication app,
	                                           boolean leftSideNavigation, boolean initialCalculation) {
		GPXFile gpxFile = parseGpx(content);
		return gpxFile != null ? prepareResponse(app, gpxFile, initialCalculation) : null;
	}

	private OnlineRoutingResponse prepareResponse(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                              boolean initialCalculation) {
		boolean calculatedTimeSpeed = useExternalTimestamps();
		if (shouldApproximateRoute() && !initialCalculation) {
			MeasurementEditingContext ctx = prepareApproximationContext(app, gpxFile);
			if (ctx != null) {
				GPXFile approximated = ctx.exportGpx(ONLINE_ROUTING_GPX_FILE_NAME);
				if (approximated != null) {
					calculatedTimeSpeed = ctx.hasCalculatedTimeSpeed();
					gpxFile = approximated;
				}
			}
		}
		return new OnlineRoutingResponse(gpxFile, calculatedTimeSpeed);
	}

	@Nullable
	private MeasurementEditingContext prepareApproximationContext(@NonNull OsmandApplication app,
	                                                              @NonNull GPXFile gpxFile) {
		try {
			RoutingHelper routingHelper = app.getRoutingHelper();
			ApplicationMode appMode = routingHelper.getAppMode();
			String routingProfile = getApproximateRouteProfile();
			String oldRoutingProfile = appMode.getRoutingProfile();
			if (routingProfile != null) {
				appMode.setRoutingProfile(routingProfile);
			}
			List<WptPt> points = gpxFile.getAllSegmentsPoints();
			LocationsHolder holder = new LocationsHolder(points);
			if (holder.getSize() > 1) {
				LatLon start = holder.getLatLon(0);
				LatLon end = holder.getLatLon(holder.getSize() - 1);
				RoutingEnvironment env = routingHelper.getRoutingEnvironment(app, appMode, start, end);
				GpxRouteApproximation gctx = new GpxRouteApproximation(env.getCtx());
				List<GpxPoint> gpxPoints = routingHelper.generateGpxPoints(env, gctx, holder);
				GpxRouteApproximation gpxApproximation = routingHelper.calculateGpxApproximation(env, gctx, gpxPoints, null);
				MeasurementEditingContext ctx = new MeasurementEditingContext(app);
				ctx.setPoints(gpxApproximation, points, appMode, useExternalTimestamps());
				appMode.setRoutingProfile(oldRoutingProfile);
				return ctx;
			}
		} catch (IOException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isResultOk(@NonNull StringBuilder errorMessage,
	                          @NonNull String content) {
		return parseGpx(content) != null;
	}

	@Nullable
	private GPXFile parseGpx(@NonNull String content) {
		InputStream gpxStream;
		try {
			gpxStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			return GPXUtilities.loadGPXFile(gpxStream);
		} catch (UnsupportedEncodingException e) {
			LOG.debug("Error when parsing GPX from server response: " + e.getMessage());
		}
		return null;
	}
}
