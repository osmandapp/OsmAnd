package net.osmand.plus.plugins.monitoring;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.simulation.SimulationProvider;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveMonitoringHelper {

	private static final Log log = PlatformUtil.getLog(LiveMonitoringHelper.class);

	private final OsmandApplication app;

	private final ConcurrentLinkedQueue<LiveMonitoringData> queue;

	private LatLon lastPoint;
	private long lastTimeUpdated;

	public LiveMonitoringHelper(@NonNull OsmandApplication app) {
		this.app = app;
		queue = new ConcurrentLinkedQueue<>();
	}

	public boolean isLiveMonitoringEnabled() {
		OsmandSettings settings = app.getSettings();
		return settings.LIVE_MONITORING.get() && (settings.SAVE_TRACK_TO_GPX.get() || settings.SAVE_GLOBAL_TRACK_TO_GPX.get());
	}

	public void updateLocation(@Nullable net.osmand.Location location) {
		long locationTime = System.currentTimeMillis();

		if (shouldRecordLocation(location, locationTime)) {
			LiveMonitoringData data = new LiveMonitoringData((float) location.getLatitude(), (float) location.getLongitude(),
					(float) location.getAltitude(), location.getSpeed(), location.getAccuracy(), location.getBearing(), locationTime);
			setupLiveDataTimeAndDistance(data, location, locationTime);
			queue.add(data);
			lastPoint = new LatLon(location.getLatitude(), location.getLongitude());
			lastTimeUpdated = locationTime;
		}
		if (isLiveMonitoringEnabled() && !queue.isEmpty())  {
			new LiveSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, queue);
		}
	}

	private boolean shouldRecordLocation(@Nullable Location location, long locationTime) {
		boolean record = false;
		if (location != null && isLiveMonitoringEnabled()
				&& SimulationProvider.isNotSimulatedLocation(location)
				&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			OsmandSettings settings = app.getSettings();
			if (locationTime - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
				record = true;
			}
			float minDistance = settings.SAVE_TRACK_MIN_DISTANCE.get();
			if (minDistance > 0 && lastPoint != null && MapUtils.getDistance(lastPoint, location.getLatitude(), location.getLongitude()) <
					minDistance) {
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

	private void setupLiveDataTimeAndDistance(@NonNull LiveMonitoringData data, @Nullable net.osmand.Location location, long locationTime) {
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


	private static class LiveMonitoringData {
		public static final int NUMBER_OF_LIVE_DATA_FIELDS = 11;    //change the value after each addition\deletion of data field

		private final float lat;
		private final float lon;
		private final float alt;
		private final float speed;
		private final float bearing;
		private final float hdop;
		private final long time;
		private long timeToArrival;
		private long timeToIntermediateOrFinish;
		private int distanceToArrivalOrMarker;
		private int distanceToIntermediateOrFinish;

		public void setTimesAndDistances(long timeToArrival, long timeToIntermediateOrFinish, int distanceToArrivalOrMarker, int distanceToIntermediateOrFinish) {
			this.timeToArrival = timeToArrival;
			this.timeToIntermediateOrFinish = timeToIntermediateOrFinish;
			this.distanceToArrivalOrMarker = distanceToArrivalOrMarker;
			this.distanceToIntermediateOrFinish = distanceToIntermediateOrFinish;
		}

		public LiveMonitoringData(float lat, float lon, float alt, float speed, float hdop, float bearing, long time) {
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
			this.speed = speed;
			this.hdop = hdop;
			this.time = time;
			this.bearing = bearing;
		}
	}

	private static AtomicBoolean LOCKED_LIVE_SENDER = new AtomicBoolean();
	private class LiveSender extends AsyncTask<ConcurrentLinkedQueue<LiveMonitoringData>, Void, Void> {


		@Override
		protected Void doInBackground(ConcurrentLinkedQueue<LiveMonitoringData>... concurrentLinkedQueues) {
			boolean lock = LOCKED_LIVE_SENDER.compareAndSet(false, true);
			if (!lock) {
				return null;
			}
			try {
				while (!queue.isEmpty()) {
					int maxSendInterval = app.getSettings().LIVE_MONITORING_MAX_INTERVAL_TO_SEND.get();
					LiveMonitoringData data = queue.peek();
					if (!(System.currentTimeMillis() - data.time > maxSendInterval)) {
						boolean retry = sendData(data);
						if (!retry) {
							break;
						}
					} else {
						queue.poll();
					}
				}
			} finally {
				LOCKED_LIVE_SENDER.set(false);
			}
			return null;
		}
	}

	public boolean sendData(@NonNull LiveMonitoringData data) {
		String baseUrl = app.getSettings().LIVE_MONITORING_URL.get();
		boolean retry = false;
		String urlStr;
		try {
			urlStr = getLiveUrl(baseUrl, data);
		} catch (IllegalArgumentException e) {
			log.error("Could not construct live url from base url: " + baseUrl, e);
			return retry;
		}
		try {
			// Parse the URL and let the URI constructor handle proper encoding of special characters such as spaces
			URL url = new URL(urlStr);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
					url.getPath(), url.getQuery(), url.getRef());
			urlConnection.setConnectTimeout(AndroidNetworkUtils.CONNECT_TIMEOUT);
			urlConnection.setReadTimeout(AndroidNetworkUtils.READ_TIMEOUT);
			log.info("Monitor " + uri);
			if (urlConnection.getResponseCode() / 100 != 2) {
				String msg = urlConnection.getResponseCode() + " : " + //$NON-NLS-1$//$NON-NLS-2$
						urlConnection.getResponseMessage();
				log.error("Error sending monitor request: " + msg);
			} else {
				retry = true; // move to next point
				queue.poll();
				InputStream is = urlConnection.getInputStream();
				StringBuilder responseBody = new StringBuilder();
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
					String s;
					while ((s = in.readLine()) != null) {
						responseBody.append(s);
						responseBody.append("\n"); //$NON-NLS-1$
					}
					is.close();
				}
				log.info("Monitor response (" + urlConnection.getHeaderField("Content-Type") + "): " + responseBody);
			}
			urlConnection.disconnect();
		} catch (Exception e) {
			retry = false;
			log.error("Failed connect to " + urlStr + ": " + e.getMessage(), e);
		}
		return retry;
	}

	private String getLiveUrl(@NonNull String baseUrl, @NonNull LiveMonitoringData data) {
		List<String> prm = new ArrayList<>();
		int maxLen = 0;
		for (int i = 0; i < LiveMonitoringData.NUMBER_OF_LIVE_DATA_FIELDS; i++) {
			boolean b = baseUrl.contains("{" + i + "}");
			if (b) {
				maxLen = i;
			}
		}
		for (int i = 0; i < maxLen + 1; i++) {
			switch (i) {
				case 0:
					prm.add(data.lat + "");
					break;
				case 1:
					prm.add(data.lon + "");
					break;
				case 2:
					prm.add(data.time + "");
					break;
				case 3:
					prm.add(data.hdop + "");
					break;
				case 4:
					prm.add(data.alt + "");
					break;
				case 5:
					prm.add(data.speed + "");
					break;
				case 6:
					prm.add(data.bearing + "");
					break;
				case 7:
					prm.add(data.timeToArrival + "");
					break;
				case 8:
					prm.add(data.timeToIntermediateOrFinish + "");
					break;
				case 9:
					prm.add(data.distanceToArrivalOrMarker + "");
					break;
				case 10:
					prm.add(data.distanceToIntermediateOrFinish + "");
					break;
				default:
					break;
			}
		}
		return MessageFormat.format(baseUrl, prm.toArray());
	}
}
