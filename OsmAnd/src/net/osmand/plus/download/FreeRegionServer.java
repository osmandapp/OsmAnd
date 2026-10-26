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
 * The server decides whether the free region may be replaced. The app sends how many replaces it
 * has already made and whether the user has left an email. A GET request, so that the
 * parameters - package, version, nd, ns, aid, utm* - land in the server access log.
 * <p>
 * PROTOTYPE: the "proto_answer" preference ("ok" / "request_email" / "no") overrides the server
 * answer for testing; "local" answers by the server rule on the device until the server is deployed. The real email request is the existing
 * https://osmand.net/subscription/register_email - not called here, a test address must not land
 * in the mailing list.
 */
public class FreeRegionServer {

	private static final Log LOG = PlatformUtil.getLog(FreeRegionServer.class);

	private static final String REPLACE_URL = "https://osmand.net/api/replace-download-map";
	private static final long FAKE_NETWORK_DELAY_MS = 1200;

	private static final String PROTO_ANSWER = "proto_answer";
	private static final String PROTO_LOCAL = "local";

	public enum ReplaceAnswer {
		/** Replace now. */
		OK("ok"),
		/** One more replace for an email. */
		REQUEST_EMAIL("request_email"),
		/** No more replaces - only Maps+. */
		NO("no");

		private final String status;

		ReplaceAnswer(@NonNull String status) {
			this.status = status;
		}

		@Nullable
		static ReplaceAnswer fromStatus(@Nullable String status) {
			for (ReplaceAnswer answer : values()) {
				if (answer.status.equalsIgnoreCase(status)) {
					return answer;
				}
			}
			return null;
		}
	}

	public interface ReplaceCallback {
		/** @param answer null when the server could not be reached */
		void onReplaceChecked(@Nullable ReplaceAnswer answer);
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
		params.put("replaces", String.valueOf(new FreeRegionHelper(app).getReplacesCount()));
		params.put("email", String.valueOf(app.getSettings().EMAIL_SUBSCRIBED.get()));

		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				return AndroidNetworkUtils.sendRequest(app, REPLACE_URL, params,
						"Requesting free region replace...", false, false);
			}

			@Override
			protected void onPostExecute(String response) {
				String forced = preferences.getString(PROTO_ANSWER, null);
				if (PROTO_LOCAL.equals(forced)) {
					callback.onReplaceChecked(answerLocally(params));
				} else if (ReplaceAnswer.fromStatus(forced) != null) {
					callback.onReplaceChecked(ReplaceAnswer.fromStatus(forced));
				} else {
					callback.onReplaceChecked(parseAnswer(response));
				}
			}
		});
	}

	@Nullable
	private ReplaceAnswer parseAnswer(@Nullable String response) {
		if (response == null) {
			return null;
		}
		try {
			ReplaceAnswer answer = ReplaceAnswer.fromStatus(new JSONObject(response).optString("status"));
			// an unknown answer of a newer server - no replace
			return answer != null ? answer : ReplaceAnswer.NO;
		} catch (JSONException e) {
			LOG.error("Replace response: " + response, e);
			return null;
		}
	}

	/** The rule of the server: 5 replaces, then one more for an email, at most 1000. */
	@NonNull
	private static ReplaceAnswer answerLocally(@NonNull Map<String, String> params) {
		int replaces = Integer.parseInt(params.get("replaces"));
		boolean email = Boolean.parseBoolean(params.get("email"));
		if (replaces >= 1000) {
			return ReplaceAnswer.NO;
		}
		return replaces >= 5 && !email ? ReplaceAnswer.REQUEST_EMAIL : ReplaceAnswer.OK;
	}

	public void registerEmail(@NonNull String email, @NonNull Runnable onRegistered) {
		handler.postDelayed(() -> {
			app.getSettings().EMAIL_SUBSCRIBED.set(true);
			onRegistered.run();
		}, FAKE_NETWORK_DELAY_MS);
	}
}
