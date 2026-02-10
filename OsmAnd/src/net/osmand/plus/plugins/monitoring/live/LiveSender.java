package net.osmand.plus.plugins.monitoring.live;

import static net.osmand.plus.plugins.monitoring.live.LiveMonitoringHelper.LOCKED_LIVE_SENDER;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.shared.gpx.GpxFormatter;

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
import java.util.Queue;

class LiveSender extends AsyncTask<Void, Void, Void> {

	private static final Log log = PlatformUtil.getLog(LiveSender.class);

	private final OsmandApplication app;
	private final Queue<LiveMonitoringData> queue;

	public LiveSender(@NonNull OsmandApplication app, @NonNull Queue<LiveMonitoringData> queue) {
		this.app = app;
		this.queue = queue;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		boolean lock = LOCKED_LIVE_SENDER.compareAndSet(false, true);
		if (!lock) {
			return null;
		}
		try {
			while (!queue.isEmpty()) {
				int maxSendInterval = app.getSettings().LIVE_MONITORING_MAX_INTERVAL_TO_SEND.get();
				LiveMonitoringData data = queue.peek();
				if (data != null && !(System.currentTimeMillis() - data.time > maxSendInterval)) {
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

	public boolean sendData(@NonNull LiveMonitoringData data) {
		String baseUrl = app.getSettings().LIVE_MONITORING_URL.get();
		boolean retry = false;
		String urlStr;
		try {
			if (baseUrl.equals("test.osmand.net") || baseUrl.equals("osmand.net")) {
				// "https://example.com?lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}").makeProfile();
				baseUrl = "https://" + baseUrl + "/userdata/translation/msg?" +
						"lat={0}&lon={1}&lat={0}&timestamp={2}&" +
						"hdop={3}&altitude={4}&speed={5}&" +
						"bearing={6}&tta={7}&ttf={8}&dta={9}&dtf={10}&&" +
						"deviceid={11}&accessToken={12}";
			}
			urlStr = getLiveUrl(baseUrl, data);
		} catch (IllegalArgumentException e) {
			log.error("Could not construct live url from base url: " + baseUrl, e);
			return false;
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
				String msg = urlConnection.getResponseCode() + " : " + urlConnection.getResponseMessage();
				log.error("Error sending monitor request: " + msg);
			} else {
				retry = true; // move to next point
				queue.poll();
				InputStream is = urlConnection.getInputStream();
				StringBuilder responseBody = new StringBuilder();
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, UTF_8));
					String s;
					while ((s = in.readLine()) != null) {
						responseBody.append(s);
						responseBody.append("\n");
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
					prm.add(GpxFormatter.INSTANCE.formatLatLon(data.lat));
					break;
				case 1:
					prm.add(GpxFormatter.INSTANCE.formatLatLon(data.lon));
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
				case 11:
					// deviceid
					prm.add(app.getSettings().BACKUP_DEVICE_ID.get());
					break;
				case 12:
					// accessToken
					prm.add(app.getSettings().BACKUP_ACCESS_TOKEN.get());
					break;
				case 13:
					prm.add(data.battery + "");
					break;
				default:
					break;
			}
		}
		return MessageFormat.format(baseUrl, prm.toArray());
	}
}