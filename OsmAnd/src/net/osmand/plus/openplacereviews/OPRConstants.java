package net.osmand.plus.openplacereviews;


import android.content.Context;

import net.osmand.plus.R;
import net.osmand.plus.osmedit.opr.OpenDBAPI;

public class OPRConstants {
	public static final String OPR_OAUTH_PREFIX = "opr-oauth";
	private static final String PURPOSE = OpenDBAPI.PURPOSE;
	private static final String CALLBACK_URL = OPR_OAUTH_PREFIX + "://osmand_opr_auth";

	public static String getBaseUrl(Context ctx) {
		return ctx.getString(R.string.opr_base_url);
	}


	public static String getLoginUrl(Context ctx) {
		return getBaseUrl(ctx) + "login" + getQueryString(ctx);
	}

	public static String getRegisterUrl(Context ctx) {
		return getBaseUrl(ctx) + "signup" + getQueryString(ctx);
	}

	public static String getQueryString(Context ctx) {
		return "?" + getPurposeParam(ctx) + "&" + getCallbackParam(ctx);
	}

	public static String getPurposeParam(Context ctx) {
		return "purpose=" + PURPOSE;
	}

	public static String getCallbackParam(Context ctx) {
		return "callback=" + CALLBACK_URL;
	}
}