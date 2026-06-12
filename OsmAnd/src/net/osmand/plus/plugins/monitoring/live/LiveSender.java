package net.osmand.plus.plugins.monitoring.live;

import static net.osmand.plus.plugins.monitoring.live.LiveMonitoringHelper.LOCKED_LIVE_SENDER;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.shared.gpx.GpxFormatter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
		if (IntentHelper.isOsmAndHost(baseUrl)) {
			sendLiveTrack(baseUrl, data);
			return true;
		}
		String urlStr;
		try {
			urlStr = getLiveUrl(baseUrl, data);
		} catch (IllegalArgumentException e) {
			log.error("Could not construct live url from base url: " + baseUrl, e);
			return false;
		}
		boolean ok = sendToUrl(urlStr) / 100 == 2;
		if (ok) {
			queue.poll();
		}
		return ok;
	}

	private void sendLiveTrack(@NonNull String baseUrl, @NonNull LiveMonitoringData data) {
		List<String> translations = app.getSettings().LIVE_MONITORING_TRANSLATIONS.getStringsList();
		Map<String, Map<String, String>> paramsByTranslation = getTranslationParams(data, translations);
		if (paramsByTranslation.isEmpty()) {
			queue.poll(); // nothing to send (no translations / not registered)
			return;
		}
		String url = "https://" + baseUrl + "/userdata/translation/msg";
		for (Map.Entry<String, Map<String, String>> entry : paramsByTranslation.entrySet()) {
			int code = post(url, entry.getValue());
			if (code == HttpURLConnection.HTTP_GONE) {
				// Translation deleted on the server — stop broadcasting into it.
				app.getSettings().LIVE_MONITORING_TRANSLATIONS.removeValue(entry.getKey());
				log.info("Live track translation gone (410) — removed from broadcast set");
			}
		}
		queue.poll();
	}

	private int post(@NonNull String url, @NonNull Map<String, String> params) {
		int[] code = {-1};
		AndroidNetworkUtils.sendRequest(app, url, params, null, false, true,
				(result, error, resultCode) -> {
					if (resultCode != null) {
						code[0] = resultCode;
					}
				});
		return code[0];
	}

	private int sendToUrl(@NonNull String urlStr) {
		InputStream is = null;
		int code = -1;
		try {
			// Parse the URL and let the URI constructor handle proper encoding of special characters such as spaces
			URL url = new URL(urlStr);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
					url.getPath(), url.getQuery(), url.getRef());
			urlConnection.setConnectTimeout(AndroidNetworkUtils.CONNECT_TIMEOUT);
			urlConnection.setReadTimeout(AndroidNetworkUtils.READ_TIMEOUT);
			log.info("Monitor " + uri);
			code = urlConnection.getResponseCode();
			if (code / 100 != 2) {
				log.error("Error sending monitor request: " + code + " : " + urlConnection.getResponseMessage());
			} else {
				is = urlConnection.getInputStream();
				StringBuilder builder = new StringBuilder();
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, UTF_8));
					String s;
					while ((s = in.readLine()) != null) {
						builder.append(s);
						builder.append("\n");
					}
				}
				log.info("Monitor response (" + urlConnection.getHeaderField("Content-Type") + "): " + builder);
			}
			urlConnection.disconnect();
		} catch (Exception e) {
			log.error("Failed connect to " + urlStr + ": " + e.getMessage(), e);
		} finally {
			Algorithms.closeStream(is);
		}
		return code;
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
					prm.add(data.battery + "");
					break;
				case 12:
					// deviceid
					prm.add(app.getSettings().BACKUP_DEVICE_ID.get());
					break;
				case 13:
					// accessToken
					prm.add(app.getSettings().BACKUP_ACCESS_TOKEN.get());
					break;
				default:
					break;
			}
		}
		return MessageFormat.format(baseUrl, prm.toArray());
	}

	private Map<String, Map<String, String>> getTranslationParams(@NonNull LiveMonitoringData data,
			@Nullable List<String> translations) {
		Map<String, Map<String, String>> result = new LinkedHashMap<>();
		if (translations == null || translations.isEmpty()) {
			log.info("No live track translations set — skipping encrypted send");
			return result;
		}
		String deviceId = app.getSettings().BACKUP_DEVICE_ID.get();
		String accessToken = app.getSettings().BACKUP_ACCESS_TOKEN.get();
		if (Algorithms.isEmpty(deviceId) || Algorithms.isEmpty(accessToken)) {
			log.info("Live track device is not registered (deviceId/accessToken missing) — skipping encrypted send");
			return result;
		}
		for (String translation : translations) {
			int sep = translation.indexOf(':');
			if (sep <= 0) {
				continue;
			}
			String tid = translation.substring(0, sep);
			String keyHex = translation.substring(sep + 1);
			if (Algorithms.isEmpty(tid) || Algorithms.isEmpty(keyHex)) {
				continue;
			}
			String encryptedData = LiveTrackCrypto.encrypt(keyHex, data);
			if (encryptedData == null) {
				log.error("Failed to encrypt live track location");
				continue;
			}
			Map<String, String> params = new LinkedHashMap<>();
			params.put("deviceid", deviceId);
			params.put("accessToken", accessToken);
			params.put("encryptedData", encryptedData);
			params.put("translationId", tid);

			result.put(translation, params);
		}
		return result;
	}
}