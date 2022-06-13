package net.osmand.plus.plugins.mapillary;

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

class GetMapillaryUserAsyncTask extends AsyncTask<String, Void, Pair<String, String>> {

	private static final String TAG = GetMapillaryUserAsyncTask.class.getSimpleName();
	private static final String DOWNLOAD_PATH = "https://a.mapillary.com/v3/users?usernames=%s&client_id=%s";
	private static final String CLIENT_ID = "LXJVNHlDOGdMSVgxZG5mVzlHQ3ZqQTo0NjE5OWRiN2EzNTFkNDg4";

	@Override
	protected Pair<String, String> doInBackground(String... params) {
		try {
			URL url = new URL(String.format(DOWNLOAD_PATH, params[0], CLIENT_ID));
			URLConnection conn = NetworkUtils.getHttpURLConnection(url);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder json = new StringBuilder(1024);
			String tmp;

			while ((tmp = reader.readLine()) != null) {
				json.append(tmp).append("\n");
			}
			reader.close();

			JSONArray data = new JSONArray(json.toString());

			if (data.length() > 0) {
				JSONObject user = data.getJSONObject(0);
				String name = user.getString("username");
				String key = user.getString("key");
				return new Pair<>(key, name);
			}
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
