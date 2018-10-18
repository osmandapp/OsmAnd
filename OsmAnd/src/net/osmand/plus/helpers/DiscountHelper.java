package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;

import net.osmand.AndroidNetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscountHelper {

	private static final String TAG = "DiscountHelper";
	//private static final String DISCOUNT_JSON = "discount.json";

	private static long mLastCheckTime;
	private static ControllerData mData;
	private static boolean mBannerVisible;
	private static final String URL = "https://osmand.net/api/motd";
	private static final String INAPP_PREFIX = "osmand-in-app:";


	public static void checkAndDisplay(final MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (settings.DO_NOT_SHOW_STARTUP_MESSAGES.get() || !settings.INAPPS_READ.get()) {
			return;
		}
		if (mBannerVisible) {
			showDiscountBanner(mapActivity, mData);
		}
		if (System.currentTimeMillis() - mLastCheckTime < 1000 * 60 * 60 * 24
				|| !settings.isInternetConnectionAvailable()) {
			return;
		}
		mLastCheckTime = System.currentTimeMillis();
		final Map<String, String> pms = new LinkedHashMap<>();
		pms.put("version", Version.getFullVersion(app));
		pms.put("nd", app.getAppInitializer().getFirstInstalledDays() + "");
		pms.put("ns", app.getAppInitializer().getNumberOfStarts() + "");
		pms.put("lang", app.getLanguage() + "");
		try {
			pms.put("aid", Secure.getString(app.getContentResolver(), Secure.ANDROID_ID));
		} catch (Exception e) {
			e.printStackTrace();
		}
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					return AndroidNetworkUtils.sendRequest(mapActivity.getMyApplication(),
							URL, pms, "Requesting discount info...", false, false);
				} catch (Exception e) {
					logError("Requesting discount info error: ", e);
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (response != null) {
					processDiscountResponse(response, mapActivity);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("SimpleDateFormat")
	private static void processDiscountResponse(String response, MapActivity mapActivity) {
		try {
			OsmandApplication app = mapActivity.getMyApplication();

			JSONObject obj = new JSONObject(response);
			ControllerData data = ControllerData.parse(app, obj);
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			Date start = df.parse(obj.getString("start"));
			Date end = df.parse(obj.getString("end"));
			int showStartFrequency = obj.getInt("show_start_frequency");
			double showDayFrequency = obj.getDouble("show_day_frequency");
			int maxTotalShow = obj.getInt("max_total_show");
			JSONObject application = obj.getJSONObject("application");

			if (data.url.startsWith(INAPP_PREFIX) && data.url.length() > INAPP_PREFIX.length()) {
				String inAppSku = data.url.substring(INAPP_PREFIX.length());
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null
						&& (purchaseHelper.isPurchased(inAppSku) || InAppPurchaseHelper.isSubscribedToLiveUpdates(app))) {
					return;
				}
			}

			String appName = app.getPackageName();
			Date date = new Date();
			if (application.has(appName) && application.getBoolean(appName)
					&& date.after(start) && date.before(end)) {

				OsmandSettings settings = app.getSettings();
				int discountId = getDiscountId(data.message, start);
				boolean discountChanged = settings.DISCOUNT_ID.get() != discountId;
				if (discountChanged) {
					settings.DISCOUNT_TOTAL_SHOW.set(0);
				}
				// show after every N (getNumberOfStarts()) starts or show after every N (double show_day_frequency) frequency
				if (discountChanged
						|| (app.getAppInitializer().getNumberOfStarts() - settings.DISCOUNT_SHOW_NUMBER_OF_STARTS.get() >= showStartFrequency
						|| System.currentTimeMillis() - settings.DISCOUNT_SHOW_DATETIME_MS.get() > 1000L * 60 * 60 * 24 * showDayFrequency)) {
					if (settings.DISCOUNT_TOTAL_SHOW.get() < maxTotalShow) {
						settings.DISCOUNT_ID.set(discountId);
						settings.DISCOUNT_TOTAL_SHOW.set(settings.DISCOUNT_TOTAL_SHOW.get() + 1);
						settings.DISCOUNT_SHOW_NUMBER_OF_STARTS.set(app.getAppInitializer().getNumberOfStarts());
						settings.DISCOUNT_SHOW_DATETIME_MS.set(System.currentTimeMillis());
						showDiscountBanner(mapActivity, data);
					}
				}
			}
		} catch (Exception e) {
			logError("JSON parsing error: ", e);
		}
	}

	private static String parseUrl(OsmandApplication app, String url) {
		if (!Algorithms.isEmpty(url)) {
			int i = url.indexOf("osmand-market-app:");
			if (i != -1) {
				String appName = url.substring(i + 18);
				return Version.getUrlWithUtmRef(app, appName);
			}
		}
		return url;
	}

	private static int getDiscountId(String message, Date start) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	private static void showDiscountBanner(final MapActivity mapActivity, final ControllerData data) {
		int iconId = mapActivity.getResources().getIdentifier(data.iconId, "drawable", mapActivity.getMyApplication().getPackageName());
		final DiscountBarController toolbarController = new DiscountBarController();
		toolbarController.setTitle(data.message);
		toolbarController.setDescription(data.description);
		toolbarController.setBackBtnIconIds(iconId, iconId);
		if (!Algorithms.isEmpty(data.url)) {
			View.OnClickListener clickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().logEvent(mapActivity, "motd_click");
					mBannerVisible = false;
					mapActivity.hideTopToolbar(toolbarController);
					openUrl(mapActivity, data.url);
				}
			};
			toolbarController.setOnBackButtonClickListener(clickListener);
			toolbarController.setOnTitleClickListener(clickListener);
		}
		toolbarController.setOnCloseButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMyApplication().logEvent(mapActivity, "motd_close");
				mBannerVisible = false;
				mapActivity.hideTopToolbar(toolbarController);
			}
		});

		mData = data;
		mBannerVisible = true;

		mapActivity.showTopToolbar(toolbarController);
	}

	private static void openUrl(final MapActivity mapActivity, String url) {
		if (url.startsWith(INAPP_PREFIX)) {
			if (url.contains(InAppPurchaseHelper.SKU_FULL_VERSION_PRICE)) {
				OsmandApplication app = mapActivity.getMyApplication();
				app.logEvent(mapActivity, "in_app_purchase_redirect");
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					purchaseHelper.purchaseFullVersion(mapActivity);
				}
			} else if (url.contains(InAppPurchaseHelper.SKU_LIVE_UPDATES)) {
				ChoosePlanDialogFragment.showOsmLiveInstance(mapActivity.getSupportFragmentManager());
			}
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			mapActivity.startActivity(intent);
		}
	}

	private static class ControllerData {

		String message;
		String description;
		String iconId;
		String url;

		static ControllerData parse(OsmandApplication app, JSONObject obj) throws JSONException {
			ControllerData res = new ControllerData();
			res.message = obj.getString("message");
			res.description = obj.getString("description");
			res.iconId = obj.getString("icon");
			res.url = parseUrl(app, obj.getString("url"));
			return res;
		}
	}

	private static class DiscountBarController extends TopToolbarController {

		DiscountBarController() {
			super(TopToolbarControllerType.DISCOUNT);
			setSingleLineTitle(false);
			setBackBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.color.discount_bar_bg, R.color.discount_bar_bg,
					R.drawable.discount_bar_bg_land, R.drawable.discount_bar_bg_land);
		}
	}

	private static void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}
}
