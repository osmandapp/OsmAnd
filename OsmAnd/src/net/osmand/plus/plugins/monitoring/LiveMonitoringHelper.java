package net.osmand.plus.plugins.monitoring;

import android.os.AsyncTask;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.SimulationProvider;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
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

public class LiveMonitoringHelper {

	private static final Log log = PlatformUtil.getLog(LiveMonitoringHelper.class);

	private final OsmandApplication app;

	private final ConcurrentLinkedQueue<LiveMonitoringData> queue;

	private LatLon lastPoint;
	private long lastTimeUpdated;
	private boolean started;

	public LiveMonitoringHelper(OsmandApplication app) {
		this.app = app;
		queue = new ConcurrentLinkedQueue<>();
	}

	public boolean isLiveMonitoringEnabled() {
		OsmandSettings settings = app.getSettings();
		return settings.LIVE_MONITORING.get() && (settings.SAVE_TRACK_TO_GPX.get() || settings.SAVE_GLOBAL_TRACK_TO_GPX.get());
	}

	public void updateLocation(net.osmand.Location location) {
		boolean record = false;
		long locationTime = System.currentTimeMillis();
		if (location != null && isLiveMonitoringEnabled()
				&& SimulationProvider.isNotSimulatedLocation(location)
				&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			OsmandSettings settings = app.getSettings();
			if (locationTime - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
				record = true;
			}
			float minDistance = settings.SAVE_TRACK_MIN_DISTANCE.get();
			if(minDistance > 0 && lastPoint != null && MapUtils.getDistance(lastPoint, location.getLatitude(), location.getLongitude()) < 
					minDistance) {
				record = false;
			}
			float precision = settings.SAVE_TRACK_PRECISION.get();
			if(precision > 0 && (!location.hasAccuracy() || location.getAccuracy() > precision)) {
				record = false;
			}
			float minSpeed = settings.SAVE_TRACK_MIN_SPEED.get();
			if(minSpeed > 0 && (!location.hasSpeed() || location.getSpeed() < minSpeed)) {
				record = false;
			}
		}
		if (isLiveMonitoringEnabled()) {
			if (!started) {
				new LiveSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, queue);
				started = true;
			}
		} else {
			started = false;
		}
		long eta = 0, etfa = 0;
		int eda = 0, edfa = 0;
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isRouteCalculated()) {
			eta = (routingHelper.getLeftTime() * 1000L) + locationTime;
			eda = routingHelper.getLeftDistance();

			int leftTimeNextIntermediate = routingHelper.getLeftTimeNextIntermediate();
			if (leftTimeNextIntermediate == 0) {
				etfa = eta;
			} else {
				etfa = (leftTimeNextIntermediate * 1000L) + locationTime;
			}
			edfa = routingHelper.getLeftDistanceNextIntermediate();
			if (edfa == 0) {
				edfa = eda;
			}
		} else {
			MapMarker firstMarker = app.getMapMarkersHelper().getFirstMapMarker();

			if (firstMarker != null && location != null) {
				float[] distInfo = new float[2];
				Location.distanceBetween(firstMarker.getLatitude(), firstMarker.getLongitude(),
						location.getLatitude(), location.getLongitude(), distInfo);
				eda = (int) distInfo[0];
			}
		}

		if(record) {
			LiveMonitoringData data = new LiveMonitoringData((float)location.getLatitude(), (float)location.getLongitude(),
					(float)location.getAltitude(), location.getSpeed(), location.getAccuracy(), location.getBearing(), locationTime,
					eta, etfa, eda, edfa);
			queue.add(data);
			lastPoint = new LatLon(location.getLatitude(), location.getLongitude());
			lastTimeUpdated = locationTime;
		}
	}


	private static class LiveMonitoringData {

		private final float lat;
		private final float lon;
		private final float alt;
		private final float speed;
		private final float bearing;
		private final long eta;
		private final long etfa;
		private final int eda;
		private final int edfa;
		private final float hdop;
		private final long time;

		public LiveMonitoringData(float lat, float lon, float alt, float speed, float hdop, float bearing, long time, long eta, long etfa, int eda, int edfa) {
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
			this.speed = speed;
			this.hdop = hdop;
			this.time = time;
			this.bearing = bearing;
			this.eta = eta;
			this.etfa = etfa;
			this.eda = eda;
			this.edfa = edfa;
		}

	}

	private class LiveSender extends AsyncTask<ConcurrentLinkedQueue<LiveMonitoringData>, Void, Void> {

		@Override
		protected Void doInBackground(ConcurrentLinkedQueue<LiveMonitoringData>... concurrentLinkedQueues) {
			while (isLiveMonitoringEnabled()) {
				int maxSendInterval = app.getSettings().LIVE_MONITORING_MAX_INTERVAL_TO_SEND.get();
				for (ConcurrentLinkedQueue queue : concurrentLinkedQueues) {
					if (!queue.isEmpty()) {
						LiveMonitoringData data = (LiveMonitoringData) queue.peek();
						if (!(System.currentTimeMillis() - data.time > maxSendInterval)) {
							sendData(data);
						} else {
							queue.poll();
						}
					}
				}
			}
			return null;
		}
	}

	public void sendData(LiveMonitoringData data) {
		String baseUrl = app.getSettings().LIVE_MONITORING_URL.get();
		String urlStr;
		try {
			urlStr = getLiveUrl(baseUrl, data);
		} catch (IllegalArgumentException e) {
			log.error("Could not construct live url from base url: " + baseUrl, e);
			return;
		}
		try {
			// Parse the URL and let the URI constructor handle proper encoding of special characters such as spaces
			URL url = new URL(urlStr);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
					url.getPath(), url.getQuery(), url.getRef());

			urlConnection.setConnectTimeout(15000);
			urlConnection.setReadTimeout(15000);

			log.info("Monitor " + uri);

			if (urlConnection.getResponseCode() / 100 != 2) {

				String msg = urlConnection.getResponseCode() + " : " + //$NON-NLS-1$//$NON-NLS-2$
						urlConnection.getResponseMessage();
				log.error("Error sending monitor request: " + msg);
			} else {
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
			log.error("Failed connect to " + urlStr + ": " + e.getMessage(), e);
		}
	}

	private String getLiveUrl(String baseUrl, LiveMonitoringData data) {
		List<String> prm = new ArrayList<String>();
		int maxLen = 0;
		for (int i = 0; i < 11; i++) {
			boolean b = baseUrl.contains("{"+i+"}");
			if(b) {
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
				prm.add(data.eta + "");
				break;
			case 8:
				prm.add(data.etfa + "");
				break;
			case 9:
				prm.add(data.eda + "");
				break;
			case 10:
				prm.add(data.edfa + "");
				break;
			default:
				break;
			}
		}
		return MessageFormat.format(baseUrl, prm.toArray());
	}
}
