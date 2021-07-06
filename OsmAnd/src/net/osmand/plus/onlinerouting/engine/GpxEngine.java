package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.LocationsHolder;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RoutingEnvironment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
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
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new GpxEngine(params);
	}

	@Override
	@Nullable
	public OnlineRoutingResponse parseResponse(@NonNull String content,
	                                           @NonNull OsmandApplication app,
	                                           boolean leftSideNavigation) {
		GPXFile gpxFile = parseGpx(content);
		return gpxFile != null ? prepareResponse(app, gpxFile) : null;
	}

	private OnlineRoutingResponse prepareResponse(@NonNull OsmandApplication app,
	                                              @NonNull GPXFile gpxFile) {
		if (shouldApproximateRoute()) {
			GPXFile approximated = approximateGpx(app, gpxFile);
			gpxFile = approximated != null ? approximated : gpxFile;
		}
		return new OnlineRoutingResponse(gpxFile);
	}

	private GPXFile approximateGpx(@NonNull OsmandApplication app,
	                               @NonNull GPXFile gpxFile) {
		try {
			RoutingHelper routingHelper = app.getRoutingHelper();
			ApplicationMode appMode = routingHelper.getAppMode();

			List<WptPt> points = gpxFile.getAllSegmentsPoints();
			LocationsHolder holder = new LocationsHolder(points);

			if (holder.getSize() > 1) {
				LatLon start = holder.getLatLon(0);
				LatLon end = holder.getLatLon(holder.getSize() - 1);
				RoutingEnvironment env = routingHelper.getRoutingEnvironment(app, appMode, start, end);

				GpxRouteApproximation gctx = new GpxRouteApproximation(env.getCtx());
				List<GpxPoint> gpxPoints = routingHelper.generateGpxPoints(env, gctx, holder);
				GpxRouteApproximation gpxApproximation = routingHelper.calculateGpxApproximation(env, gctx, gpxPoints, null);

				MeasurementEditingContext ctx = new MeasurementEditingContext();
				ctx.setApplication(app);
				ctx.setPoints(gpxApproximation, points, appMode);
				return ctx.exportGpx(ONLINE_ROUTING_GPX_FILE_NAME);
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
