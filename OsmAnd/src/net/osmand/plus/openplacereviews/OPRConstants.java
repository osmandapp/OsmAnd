package net.osmand.plus.openplacereviews;


import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.osmedit.opr.OpenDBAPI;

public class OPRConstants {
	public static final String OPR_OAUTH_PREFIX = "opr-oauth";
	private static final String PURPOSE = OpenDBAPI.PURPOSE;
	private static final String CALLBACK_URL = OPR_OAUTH_PREFIX + "://osmand_opr_auth";

	public static String getBaseUrl(@NonNull OsmandApplication app) {
		return app.getSettings().getOprUrl();
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