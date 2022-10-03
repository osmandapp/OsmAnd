package net.osmand.plus.plugins.openplacereviews;


import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;

public class OPRConstants {
	public static final String OPR_OAUTH_PREFIX = "opr-oauth";
	private static final String PURPOSE = OpenDBAPI.PURPOSE;
	private static final String CALLBACK_URL = OPR_OAUTH_PREFIX + "://osmand_opr_auth";

	public static String getBaseUrl(@NonNull OsmandApplication app) {
		OpenPlaceReviewsPlugin plugin = PluginsHelper.getPlugin(OpenPlaceReviewsPlugin.class);
		boolean useDevUrl = plugin != null && plugin.OPR_USE_DEV_URL.get();
		return app.getString(useDevUrl ? R.string.dev_opr_base_url : R.string.opr_base_url);
	}

	public static String getLoginUrl(@NonNull OsmandApplication app) {
		return getBaseUrl(app) + "login" + getQueryString();
	}

	public static String getRegisterUrl(@NonNull OsmandApplication app) {
		return getBaseUrl(app) + "signup" + getQueryString();
	}

	public static String getQueryString() {
		return "?" + getPurposeParam() + "&" + getCallbackParam();
	}

	public static String getPurposeParam() {
		return "purpose=" + PURPOSE;
	}

	public static String getCallbackParam() {
		return "callback=" + CALLBACK_URL;
	}
}