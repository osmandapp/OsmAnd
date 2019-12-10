package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import net.osmand.AndroidNetworkUtils;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class DiscountHelper {

	private static final String TAG = "DiscountHelper";
	//private static final String DISCOUNT_JSON = "discount.json";

	private static long mLastCheckTime;
	private static ControllerData mData;
	private static boolean mBannerVisible;
	private static PoiUIFilter mFilter;
	private static boolean mFilterVisible;
	private static final String URL = "https://osmand.net/api/motd";
	private static final String INAPP_PREFIX = "osmand-in-app:";
	private static final String SEARCH_QUERY_PREFIX = "osmand-search-query:";
	private static final String SHOW_POI_PREFIX = "osmand-show-poi:";
	private static final String OPEN_ACTIVITY = "open_activity";

	private static final String SHOW_CHOOSE_PLAN_PREFIX = "show-choose-plan:";
	private static final String CHOOSE_PLAN_TYPE_FREE = "free-version";
	private static final String CHOOSE_PLAN_TYPE_LIVE = "osmand-live";
	private static final String CHOOSE_PLAN_TYPE_SEA_DEPTH = "sea-depth";
	private static final String CHOOSE_PLAN_TYPE_HILLSHADE = "hillshade";
	private static final String CHOOSE_PLAN_TYPE_WIKIPEDIA = "wikipedia";
	private static final String CHOOSE_PLAN_TYPE_WIKIVOYAGE= "wikivoyage";

	@SuppressLint("HardwareIds")
	public static void checkAndDisplay(final MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (settings.DO_NOT_SHOW_STARTUP_MESSAGES.get() || !settings.INAPPS_READ.get()) {
			return;
		}
		if (mBannerVisible) {
			showDiscountBanner(mapActivity, mData);
		} else if (mFilterVisible) {
			showPoiFilter(mapActivity, mFilter);
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
			boolean showChristmasDialog = obj.optBoolean("show_christmas_dialog", false);

			if (!validateUrl(app, data.url)) {
				return;
			}

			if (data.oneOfConditions != null) {
				boolean oneOfConditionsMatch = false;
				try {
					Conditions conditions = new Conditions(app);
					JSONArray conditionsArr = data.oneOfConditions;
					for (int i = 0; i < conditionsArr.length(); i++) {
						JSONObject conditionObj = conditionsArr.getJSONObject(i);
						JSONArray conditionArr = conditionObj.getJSONArray("condition");
						if (conditionArr.length() > 0) {
							boolean conditionMatch = true;
							for (int k = 0; k < conditionArr.length(); k++) {
								JSONObject o = conditionArr.getJSONObject(k);
								conditionMatch = conditions.matchesCondition(o);
								if (!conditionMatch) {
									break;
								}
							}
							oneOfConditionsMatch |= conditionMatch;
						}
					}
				} catch (JSONException e) {
					// ignore
				}
				if (!oneOfConditionsMatch) {
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
						if (showChristmasDialog) {
							mapActivity.showXMasDialog();
						} else {
							InAppPurchaseHelper purchaseHelper = mapActivity.getPurchaseHelper();
							if (purchaseHelper != null) {
								purchaseHelper.requestInventory();
							}
							showDiscountBanner(mapActivity, data);
						}
					}
				}
			}
		} catch (Exception e) {
			logError("JSON parsing error: ", e);
		}
	}

	public static boolean validateUrl(OsmandApplication app, String url) {
		if (url.startsWith(INAPP_PREFIX) && url.length() > INAPP_PREFIX.length()) {
			String inAppSku = url.substring(INAPP_PREFIX.length());
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null
					&& purchaseHelper.isPurchased(inAppSku) || InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
				return false;
			}
		}
		return true;
	}

	public static String parseUrl(OsmandApplication app, String url) {
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
		if (data.bgColor != -1) {
			LayerDrawable bgLand = (LayerDrawable) ContextCompat.getDrawable(mapActivity, R.drawable.discount_bar_bg_land);
			if (bgLand != null) {
				((GradientDrawable) bgLand.findDrawableByLayerId(R.id.color_bg)).setColor(data.bgColor);
			}
			ColorDrawable bg = new ColorDrawable(data.bgColor);
			toolbarController.setBgs(bg, bg, bgLand, bgLand);
		}
		toolbarController.setTitle(data.message);
		toolbarController.setTitleTextClrs(data.titleColor, data.titleColor);
		toolbarController.setDescription(data.description);
		toolbarController.setDescrTextClrs(data.descrColor, data.descrColor);
		toolbarController.setBackBtnIconIds(iconId, iconId);
		toolbarController.setBackBtnIconClrs(data.iconColor, data.iconColor);
		toolbarController.setStatusBarColor(data.statusBarColor);
		if (!TextUtils.isEmpty(data.textBtnTitle)) {
			toolbarController.setTextBtnVisible(true);
			toolbarController.setTextBtnTitle(data.textBtnTitle);
			toolbarController.setTextBtnTitleClrs(data.textBtnTitleColor, data.textBtnTitleColor);
		}
		if (!Algorithms.isEmpty(data.url)) {
			View.OnClickListener clickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().logEvent("motd_click");
					mBannerVisible = false;
					mapActivity.hideTopToolbar(toolbarController);
					openUrl(mapActivity, data.url);
				}
			};
			toolbarController.setOnBackButtonClickListener(clickListener);
			toolbarController.setOnTitleClickListener(clickListener);
			toolbarController.setOnTextBtnClickListener(clickListener);
		}
		toolbarController.setOnCloseButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMyApplication().logEvent("motd_close");
				mBannerVisible = false;
				mapActivity.hideTopToolbar(toolbarController);
			}
		});

		mData = data;
		mBannerVisible = true;

		mapActivity.showTopToolbar(toolbarController);
	}

	private static void showPoiFilter(final MapActivity mapActivity, final PoiUIFilter poiFilter) {
		QuickSearchHelper.showPoiFilterOnMap(mapActivity, poiFilter, new Runnable() {
			@Override
			public void run() {
				mFilterVisible = false;
			}
		});
		mFilter = poiFilter;
		mFilterVisible = true;
	}

	public static void openUrl(final MapActivity mapActivity, String url) {
		if (url.startsWith(INAPP_PREFIX)) {
			OsmandApplication app = mapActivity.getMyApplication();
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				if (url.contains(purchaseHelper.getFullVersion().getSku())) {
					app.logEvent("in_app_purchase_redirect");
					purchaseHelper.purchaseFullVersion(mapActivity);
				} else {
					for (InAppPurchase p : purchaseHelper.getLiveUpdates().getAllSubscriptions()) {
						if (url.contains(p.getSku())) {
							ChoosePlanDialogFragment.showOsmLiveInstance(mapActivity.getSupportFragmentManager());
							break;
						}
					}
				}
			}
		} else if (url.startsWith(SEARCH_QUERY_PREFIX)) {
			String query = url.substring(SEARCH_QUERY_PREFIX.length());
			if (!query.isEmpty()) {
				mapActivity.showQuickSearch(query);
			}
		} else if (url.startsWith(SHOW_POI_PREFIX)) {
			String names = url.substring(SHOW_POI_PREFIX.length());
			if (!names.isEmpty()) {
				OsmandApplication app = mapActivity.getMyApplication();
				MapPoiTypes poiTypes = app.getPoiTypes();
				Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<>();
				for (String name : names.split(",")) {
					AbstractPoiType abstractType = poiTypes.getAnyPoiTypeByKey(name);
					if (abstractType instanceof PoiCategory) {
						acceptedTypes.put((PoiCategory) abstractType, null);
					} else if (abstractType instanceof PoiType) {
						PoiType type = (PoiType) abstractType;
						PoiCategory category = type.getCategory();
						LinkedHashSet<String> set = acceptedTypes.get(category);
						if (set == null) {
							set = new LinkedHashSet<>();
							acceptedTypes.put(category, set);
						}
						set.add(type.getKeyName());
					}
				}
				if (!acceptedTypes.isEmpty()) {
					PoiUIFilter filter = new PoiUIFilter("", null, acceptedTypes, app);
					filter.setName(filter.getTypesName());
					showPoiFilter(mapActivity, filter);
				}
			}
		} else if (url.equals(OPEN_ACTIVITY)) {
			if (mData.activityJson != null) {
				openActivity(mapActivity, mData.activityJson);
			}
		} else if (url.startsWith(SHOW_CHOOSE_PLAN_PREFIX)) {
			String planType = url.substring(SHOW_CHOOSE_PLAN_PREFIX.length()).trim();
			if (CHOOSE_PLAN_TYPE_FREE.equals(planType)) {
				ChoosePlanDialogFragment.showFreeVersionInstance(mapActivity.getSupportFragmentManager());
			} else if (CHOOSE_PLAN_TYPE_LIVE.equals(planType)) {
				ChoosePlanDialogFragment.showOsmLiveInstance(mapActivity.getSupportFragmentManager());
			} else if (CHOOSE_PLAN_TYPE_SEA_DEPTH.equals(planType)) {
				ChoosePlanDialogFragment.showSeaDepthMapsInstance(mapActivity.getSupportFragmentManager());
			} else if (CHOOSE_PLAN_TYPE_HILLSHADE.equals(planType)) {
				ChoosePlanDialogFragment.showHillshadeSrtmPluginInstance(mapActivity.getSupportFragmentManager());
			} else if (CHOOSE_PLAN_TYPE_WIKIPEDIA.equals(planType)) {
				ChoosePlanDialogFragment.showWikipediaInstance(mapActivity.getSupportFragmentManager());
			} else if (CHOOSE_PLAN_TYPE_WIKIVOYAGE.equals(planType)) {
				ChoosePlanDialogFragment.showWikivoyageInstance(mapActivity.getSupportFragmentManager());
			}
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			mapActivity.startActivity(intent);
		}
	}

	private static void openActivity(Context context, JSONObject activityObject) {
		boolean successful = false;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		try {
			for (Iterator<String> it = activityObject.keys(); it.hasNext(); ) {
				String key = it.next();
				if (key.equals("activity_name")) {
					intent.setClassName(context, activityObject.getString(key));
					successful = true;
					continue;
				}
				Object obj = activityObject.get(key);
				if (obj instanceof Integer) {
					intent.putExtra(key, (Integer) obj);
				} else if (obj instanceof Long) {
					intent.putExtra(key, (Long) obj);
				} else if (obj instanceof Boolean) {
					intent.putExtra(key, (Boolean) obj);
				} else if (obj instanceof Float) {
					intent.putExtra(key, (Float) obj);
				} else if (obj instanceof Double) {
					intent.putExtra(key, (Double) obj);
				} else if (obj instanceof String) {
					intent.putExtra(key, (String) obj);
				}
			}
		} catch (JSONException e) {
			successful = false;
		}
		if (successful) {
			try {
				context.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				// ignore
			}
		}
	}

	private static class ControllerData {

		String message;
		String description;
		String iconId;
		String url;
		String textBtnTitle;

		@ColorInt
		int iconColor = -1;
		@ColorInt
		int bgColor = -1;
		@ColorInt
		int titleColor = -1;
		@ColorInt
		int descrColor = -1;
		@ColorInt
		int statusBarColor = -1;
		@ColorInt
		int textBtnTitleColor = -1;

		JSONObject activityJson;
		JSONArray oneOfConditions;

		static ControllerData parse(OsmandApplication app, JSONObject obj) throws JSONException {
			ControllerData res = new ControllerData();
			res.message = obj.getString("message");
			res.description = obj.getString("description");
			res.iconId = obj.getString("icon");
			res.url = parseUrl(app, obj.getString("url"));
			res.textBtnTitle = obj.optString("button_title");
			res.iconColor = parseColor("icon_color_default_light", obj);
			res.bgColor = parseColor("bg_color", obj);
			res.titleColor = parseColor("title_color", obj);
			res.descrColor = parseColor("description_color", obj);
			res.statusBarColor = parseColor("status_bar_color", obj);
			res.textBtnTitleColor = parseColor("button_title_color", obj);
			res.activityJson = obj.optJSONObject("activity");
			res.oneOfConditions = obj.optJSONArray("oneOfConditions");
			return res;
		}

		private static int parseColor(String key, JSONObject obj) {
			String color = obj.optString(key);
			if (!color.isEmpty()) {
				return Color.parseColor(color);
			}
			return -1;
		}
	}

	public static class DiscountBarController extends TopToolbarController {

		private int statusBarColor = NO_COLOR;

		DiscountBarController() {
			super(TopToolbarControllerType.DISCOUNT);
			setSingleLineTitle(false);
			setBackBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setBgIds(R.color.discount_bar_bg, R.color.discount_bar_bg,
					R.drawable.discount_bar_bg_land, R.drawable.discount_bar_bg_land);
		}

		@Override
		public int getStatusBarColor(Context context, boolean night) {
			return statusBarColor;
		}

		void setStatusBarColor(int statusBarColor) {
			this.statusBarColor = statusBarColor;
		}
	}

	private static void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

	private static abstract class Condition {

		protected OsmandApplication app;

		Condition(OsmandApplication app) {
			this.app = app;
		}

		abstract String getId();

		abstract boolean matches(String value);

	}

	private static abstract class InAppPurchaseCondition extends Condition {

		InAppPurchases inAppPurchases;

		InAppPurchaseCondition(OsmandApplication app) {
			super(app);
			inAppPurchases = app.getInAppPurchaseHelper().getInAppPurchases();
		}
	}

	private static abstract class SubscriptionCondition extends Condition {

		InAppSubscriptionList liveUpdates;

		SubscriptionCondition(OsmandApplication app) {
			super(app);
			liveUpdates = app.getInAppPurchaseHelper().getLiveUpdates();
		}
	}

	private static class NotPurchasedSubscriptionCondition extends SubscriptionCondition {

		NotPurchasedSubscriptionCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "not_purchased_subscription";
		}

		@Override
		boolean matches(@NonNull String value) {
			InAppSubscription subscription = liveUpdates.getSubscriptionBySku(value);
			return subscription == null || !subscription.isPurchased();
		}
	}

	private static class PurchasedSubscriptionCondition extends SubscriptionCondition {

		PurchasedSubscriptionCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "purchased_subscription";
		}

		@Override
		boolean matches(@NonNull String value) {
			InAppSubscription subscription = liveUpdates.getSubscriptionBySku(value);
			return subscription != null && subscription.isPurchased();
		}
	}

	private static class NotPurchasedInAppPurchaseCondition extends InAppPurchaseCondition {

		NotPurchasedInAppPurchaseCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "not_purchased_inapp";
		}

		@Override
		boolean matches(@NonNull String value) {
			InAppPurchase purchase = inAppPurchases.getInAppPurchaseBySku(value);
			return purchase == null || !purchase.isPurchased();
		}
	}

	private static class PurchasedInAppPurchaseCondition extends InAppPurchaseCondition {

		PurchasedInAppPurchaseCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "purchased_inapp";
		}

		@Override
		boolean matches(@NonNull String value) {
			InAppPurchase purchase = inAppPurchases.getInAppPurchaseBySku(value);
			return purchase != null && purchase.isPurchased();
		}
	}

	private static class NotPurchasedPluginCondition extends Condition {

		NotPurchasedPluginCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "not_purchased_plugin";
		}

		@Override
		boolean matches(@NonNull String value) {
			OsmandPlugin plugin = OsmandPlugin.getPlugin(value);
			return plugin == null || plugin.needsInstallation();
		}
	}

	private static class PurchasedPluginCondition extends Condition {

		PurchasedPluginCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "purchased_plugin";
		}

		@Override
		boolean matches(@NonNull String value) {
			OsmandPlugin plugin = OsmandPlugin.getPlugin(value);
			return plugin != null && !plugin.needsInstallation();
		}
	}

	private static class Conditions {

		protected OsmandApplication app;
		private Condition[] conditions;

		Conditions(OsmandApplication app) {
			this.app = app;
			conditions = new Condition[] {
					new NotPurchasedSubscriptionCondition(app),
					new PurchasedSubscriptionCondition(app),
					new NotPurchasedInAppPurchaseCondition(app),
					new PurchasedInAppPurchaseCondition(app),
					new NotPurchasedPluginCondition(app),
					new PurchasedPluginCondition(app) };
		}

		boolean matchesCondition(JSONObject o) {
			for (Condition condition : conditions) {
				String value = o.optString(condition.getId());
				if (!TextUtils.isEmpty(value)) {
					return condition.matches(value);
				}
			}
			return false;
		}
	}
}
