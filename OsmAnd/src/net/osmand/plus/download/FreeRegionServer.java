package net.osmand.plus.download;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InstallReferrer;
import net.osmand.plus.utils.AndroidNetworkUtils;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The server decides whether the free region may be replaced. A GET request, so that the
 * parameters - package, version, nd, ns, aid, utm* - land in the server access log.
 * <p>
 * PROTOTYPE: until /api/replace-download-map is deployed the answer is simulated on the device -
 * the first replace is allowed, the next ones are not; the "answer" preference
 * ("allow" / "deny") forces it. The real email request is the existing
 * https://osmand.net/subscription/register_email - not called here, a test address must not land
 * in the mailing list.
 */
public class FreeRegionServer {

	private static final Log LOG = PlatformUtil.getLog(FreeRegionServer.class);

	private static final String REPLACE_URL = "https://osmand.net/api/replace-download-map";
	private static final long FAKE_NETWORK_DELAY_MS = 1200;

	private static final String PROTO_REPLACES_USED = "proto_replaces_used";
	private static final String PROTO_ANSWER = "proto_answer";

	public interface ReplaceCallback {
		void onReplaceChecked(boolean allowed);
	}

	private final OsmandApplication app;
	private final SharedPreferences preferences;
	private final Handler handler = new Handler(Looper.getMainLooper());

	public FreeRegionServer(@NonNull OsmandApplication app) {
		this.app = app;
		this.preferences = FreeRegionHelper.getPreferences(app);
	}

	public void requestReplace(@NonNull WorldRegion from, @NonNull WorldRegion to, @NonNull ReplaceCallback callback) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("package", app.getPackageName());
		params.put("version", Version.getFullVersion(app));
		params.put("lang", app.getLanguage());
		params.put("nd", String.valueOf(app.getAppInitializer().getFirstInstalledDays()));
		params.put("ns", String.valueOf(app.getAppInitializer().getNumberOfStarts()));
		if (app.isUserAndroidIdAllowed()) {
			params.put("aid", app.getUserAndroidId());
		}
		InstallReferrer.addParams(app, params);
		params.put("from", from.getRegionId());
		params.put("to", to.getRegionId());

		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				return AndroidNetworkUtils.sendRequest(app, REPLACE_URL, params,
						"Requesting free region replace...", false, false);
			}

			@Override
			protected void onPostExecute(String response) {
				Boolean allowed = parseAllowed(response);
				if (allowed != null && !hasForcedAnswer()) {
					callback.onReplaceChecked(allowed);
				} else {
					simulateReplace(callback);
				}
			}
		});
	}

	@Nullable
	private Boolean parseAllowed(@Nullable String response) {
		if (response == null) {
			return null;
		}
		try {
			return "ok".equalsIgnoreCase(new JSONObject(response).optString("status"));
		} catch (JSONException e) {
			LOG.error("Replace response: " + response, e);
			return null;
		}
	}

	private boolean hasForcedAnswer() {
		String forced = preferences.getString(PROTO_ANSWER, "");
		return "allow".equals(forced) || "deny".equals(forced);
	}

	private void simulateReplace(@NonNull ReplaceCallback callback) {
		handler.postDelayed(() -> {
			int used = preferences.getInt(PROTO_REPLACES_USED, 0);
			String forced = preferences.getString(PROTO_ANSWER, "");
			boolean allowed = "allow".equals(forced) || (!"deny".equals(forced) && used < 1);
			if (allowed) {
				preferences.edit().putInt(PROTO_REPLACES_USED, used + 1).apply();
			}
			callback.onReplaceChecked(allowed);
		}, FAKE_NETWORK_DELAY_MS);
	}

	public void registerEmail(@NonNull String email, @NonNull Runnable onRegistered) {
		handler.postDelayed(() -> {
			app.getSettings().EMAIL_SUBSCRIBED.set(true);
			onRegistered.run();
		}, FAKE_NETWORK_DELAY_MS);
	}
}
