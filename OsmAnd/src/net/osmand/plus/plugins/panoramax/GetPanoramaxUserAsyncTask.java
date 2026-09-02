package net.osmand.plus.plugins.panoramax;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import net.osmand.osm.io.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Resolves a typed user name to the account id that tagged the pictures.
 *
 * The search endpoint needs no API key, unlike the Mapillary equivalent. It answers with up to
 * ten fuzzy matches; only the best one is used here so the filter behaves the way the Mapillary
 * filter does. Because Panoramax is federated the same name can exist on several instances, so
 * the returned label is "name@instance" and that is what gets shown back to the user.
 */
class GetPanoramaxUserAsyncTask extends AsyncTask<String, Void, Pair<String, String>> {

	private static final String TAG = GetPanoramaxUserAsyncTask.class.getSimpleName();

	@Override
	protected Pair<String, String> doInBackground(String... params) {
		try {
			String query = URLEncoder.encode(params[0], "UTF-8");
			URL url = new URL(String.format(PanoramaxConstants.USER_SEARCH_URL, query));
			URLConnection conn = NetworkUtils.getHttpURLConnection(url);

			StringBuilder json = new StringBuilder(1024);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String tmp;
				while ((tmp = reader.readLine()) != null) {
					json.append(tmp).append("\n");
				}
			}

			JSONArray features = new JSONObject(json.toString()).optJSONArray("features");
			if (features == null || features.length() == 0) {
				return null;
			}

			JSONObject best = features.getJSONObject(0);
			for (int i = 0; i < features.length(); i++) {
				JSONObject user = features.getJSONObject(i);
				if (params[0].equalsIgnoreCase(user.optString("name"))
						|| params[0].equalsIgnoreCase(user.optString("label"))) {
					best = user;
					break;
				}
			}

			String id = best.optString("id", null);
			if (id == null) {
				return null;
			}
			String label = best.optString("label", null);
			if (label == null || label.isEmpty()) {
				label = best.optString("name");
			}
			return new Pair<>(id, label);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Unable to create url", e);
		} catch (IOException e) {
			Log.e(TAG, "Unable to open connection", e);
		} catch (JSONException e) {
			Log.e(TAG, "Unable to create json", e);
		}
		return null;
	}
}
