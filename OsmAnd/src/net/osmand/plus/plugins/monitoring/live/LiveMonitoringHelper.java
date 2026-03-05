package net.osmand.plus.plugins.monitoring.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.simulation.SimulationProvider;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.MapUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveMonitoringHelper {

	protected static final AtomicBoolean LOCKED_LIVE_SENDER = new AtomicBoolean();

	private final OsmandApplication app;

	private final ConcurrentLinkedQueue<LiveMonitoringData> queue = new ConcurrentLinkedQueue<>();

	private LatLon lastPoint;
	private long lastTimeUpdated;

	public LiveMonitoringHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isLiveMonitoringEnabled() {
		OsmandSettings settings = app.getSettings();
		return settings.LIVE_MONITORING.get()
				&& (settings.SAVE_TRACK_TO_GPX.get() || settings.SAVE_GLOBAL_TRACK_TO_GPX.get());
	}

	public void updateLocation(@Nullable net.osmand.Location location) {
		long locationTime = System.currentTimeMillis();

		if (location != null && shouldRecordLocation(location, locationTime)) {
			int battery = AndroidUtils.getBatteryLevel(app);
			LiveMonitoringData data = new LiveMonitoringData(location, locationTime, battery);
			setupLiveDataTimeAndDistance(data, location, locationTime);

			queue.add(data);
			lastPoint = new LatLon(location.getLatitude(), location.getLongitude());
			lastTimeUpdated = locationTime;
		}
		if (isLiveMonitoringEnabled() && !queue.isEmpty()) {
			OsmAndTaskManager.executeTask(new LiveSender(app, queue));
		}
	}

	private boolean shouldRecordLocation(@NonNull Location location, long locationTime) {
		boolean record = false;
		if (isLiveMonitoringEnabled() && SimulationProvider.isLocationForRecording(location)
				&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			OsmandSettings settings = app.getSettings();
			if (locationTime - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
				record = true;
			}
			float minDistance = settings.SAVE_TRACK_MIN_DISTANCE.get();
			if (minDistance > 0 && lastPoint != null
					&& MapUtils.getDistance(lastPoint, location.getLatitude(), location.getLongitude()) < minDistance) {
				record = false;
			}
			float precision = settings.SAVE_TRACK_PRECISION.get();
			if (precision > 0 && (!location.hasAccuracy() || location.getAccuracy() > precision)) {
				record = false;
			}
			float minSpeed = settings.SAVE_TRACK_MIN_SPEED.get();
			if (minSpeed > 0 && (!location.hasSpeed() || location.getSpeed() < minSpeed)) {
				record = false;
			}
		}
		return record;
	}

	private void setupLiveDataTimeAndDistance(@NonNull LiveMonitoringData data,
			@Nullable Location location, long locationTime) {
		long timeToArrival = 0, timeToIntermediateOrFinish = 0;
		int distanceToArrivalOrMarker = 0, distanceToIntermediateOrFinish = 0;
		RoutingHelper routingHelper = app.getRoutingHelper();

		if (routingHelper.isRouteCalculated()) {
			timeToArrival = (routingHelper.getLeftTime() * 1000L) + locationTime;
			distanceToArrivalOrMarker = routingHelper.getLeftDistance();
			int leftTimeNextIntermediate = routingHelper.getLeftTimeNextIntermediate();

			if (leftTimeNextIntermediate == 0) {
				timeToIntermediateOrFinish = timeToArrival;
			} else {
				timeToIntermediateOrFinish = (leftTimeNextIntermediate * 1000L) + locationTime;
			}

			distanceToIntermediateOrFinish = routingHelper.getLeftDistanceNextIntermediate();
			if (distanceToIntermediateOrFinish == 0) {
				distanceToIntermediateOrFinish = distanceToArrivalOrMarker;
			}
		} else {
			MapMarker firstMarker = app.getMapMarkersHelper().getFirstMapMarker();

			if (firstMarker != null && location != null) {
				distanceToArrivalOrMarker = (int) MapUtils.getDistance(firstMarker.getLatitude(), firstMarker.getLongitude(), location.getLatitude(), location.getLongitude());
			}
		}
		data.setTimesAndDistances(timeToArrival, timeToIntermediateOrFinish, distanceToArrivalOrMarker, distanceToIntermediateOrFinish);
	}
}