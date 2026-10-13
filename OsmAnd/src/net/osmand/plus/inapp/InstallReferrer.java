package net.osmand.plus.inapp;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.util.Map;

/**
 * Campaign of the Google Play install (utm_source, utm_medium, utm_campaign).
 * Ad click ids are not kept, only the fact that the install came from an ad click.
 */
public class InstallReferrer {

	private static final String[] CAMPAIGN_KEYS = {"utm_source", "utm_medium", "utm_campaign"};
	private static final String[] AD_CLICK_KEYS = {"gclid", "gbraid", "wbraid"};
	private static final String AD_CLICK = "ad_click";
	private static final int MAX_VALUE_LENGTH = 100;

	@NonNull
	public static String filter(@Nullable String referrer) {
		if (Algorithms.isEmpty(referrer)) {
			return "";
		}
		Uri uri = Uri.parse("?" + referrer);
		Uri.Builder builder = new Uri.Builder();
		for (String key : CAMPAIGN_KEYS) {
			String value = uri.getQueryParameter(key);
			if (!Algorithms.isEmpty(value)) {
				builder.appendQueryParameter(key, value.length() > MAX_VALUE_LENGTH
						? value.substring(0, MAX_VALUE_LENGTH) : value);
			}
		}
		for (String key : AD_CLICK_KEYS) {
			if (!Algorithms.isEmpty(uri.getQueryParameter(key))) {
				builder.appendQueryParameter(AD_CLICK, "1");
				break;
			}
		}
		String query = builder.build().getEncodedQuery();
		return query != null ? query : "";
	}

	public static void addParams(@NonNull OsmandApplication app, @NonNull Map<String, String> params) {
		String referrer = app.getSettings().INSTALL_REFERRER.get();
		if (Algorithms.isEmpty(referrer)) {
			return;
		}
		Uri uri = Uri.parse("?" + referrer);
		for (String key : uri.getQueryParameterNames()) {
			String value = uri.getQueryParameter(key);
			if (!Algorithms.isEmpty(value)) {
				params.put(key, value);
			}
		}
	}
}
