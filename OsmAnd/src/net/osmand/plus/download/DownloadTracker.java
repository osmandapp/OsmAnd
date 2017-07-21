package net.osmand.plus.download;

import android.os.Build;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class DownloadTracker {
	private static final Log log = PlatformUtil.getLog(DownloadTracker.class);

	private Map<String, String> getCustomVars(OsmandApplication ctx) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("App", Version.getFullVersion(ctx));
		map.put("Device", Build.DEVICE);
		map.put("Brand", Build.BRAND);
		map.put("Model", Build.MODEL);
		map.put("Package", ctx.getPackageName());

		map.put("Version name", ctx.getVersionName());
		map.put("Version code", ctx.getVersionCode()+"");
		return map;
	}

	private String randomNumber() {
		return (new Random(System.currentTimeMillis()).nextInt(100000000) + 100000000) + "";
	}

	static final String analyticsVersion = "4.3"; // Analytics version - AnalyticsVersion

	public void trackEvent(OsmandApplication a,
			String category, String action, String label, int value, String trackingAcount) {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		try {
			Map<String, String> customVariables = getCustomVars(a);
			parameters.put("AnalyticsVersion", analyticsVersion);
			parameters.put("utmn", randomNumber());
			parameters.put("utmhn", "https://app.osmand.net");
			parameters.put("utmni", "1");
			parameters.put("utmt", "event");

			StringBuilder customVars = new StringBuilder();
			Iterator<Entry<String, String>> customs = customVariables.entrySet().iterator();
			for (int i = 0; i < customVariables.size(); i++) {
				Entry<String, String> n = customs.next();
				if (i > 0) {
					customVars.append("*");
				}
				// "'" => "'0", ')' => "'1", '*' => "'2", '!' => "'3",
				customVars.append((i + 1) + "!").append((n.getKey() + n.getValue()));
			}

			parameters.put("utmcs", "UTF-8");
			parameters.put("utmul", "en");
			parameters.put("utmhid", (System.currentTimeMillis() / 1000) + "");
			parameters.put("utmac", trackingAcount);
			String domainHash = "app.osmand.net".hashCode() + "";

			String utma = domainHash + ".";
			File fl = a.getAppPath(".nomedia");
			if (fl.exists()) {
				utma += (fl.lastModified()) + ".";
			} else {
				utma += (randomNumber()) + ".";
			}
			utma += ((System.currentTimeMillis() / 1000) + ".");
			utma += ((System.currentTimeMillis() / 1000) + ".");
			utma += ((System.currentTimeMillis() / 1000) + ".");
			utma += "1";
			parameters.put("utmcc", "__utma=" + utma + ";");
			parameters.put("utme", MessageFormat.format("5({0}*{1}*{2})({3})", category, action, label == null ? "" : label, value)
					+ customVars);

			String scheme = "";
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
				scheme = "https";
			else
				scheme = "http";
			String beaconUrl = scheme + "://www.google-analytics.com/__utm.gif";
			StringBuilder urlString = new StringBuilder(beaconUrl + "?");
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				urlString.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), "UTF-8"));
				if (it.hasNext()) {
					urlString.append("&");
				}
			}

			log.debug(urlString);
			HttpURLConnection conn = NetworkUtils.getHttpURLConnection(urlString.toString());
			conn.setConnectTimeout(5000);
			conn.setDoInput(false);
			conn.setDoOutput(false);
			conn.connect();
			log.info("Response analytics is " + conn.getResponseCode() + " " + conn.getResponseMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

}
