package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.DiscountBottomSheet;
import net.osmand.plus.chooseplan.MapsPlusPlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.chooseplan.button.SubscriptionButton;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class DiscountHelper {

	private static final String TAG = "DiscountHelper";
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(DiscountHelper.class);

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
	private static final String SELECTED_CHOOSE_PLAN_BUTTON_KEY = "selected_choose_plan_btn";
	private static final String CHOOSE_PLAN_TYPE_FREE = "free-version";
	private static final String CHOOSE_PLAN_TYPE_SEA_DEPTH = "sea-depth";
	private static final String CHOOSE_PLAN_TYPE_HILLSHADE = "hillshade";
	private static final String CHOOSE_PLAN_TYPE_WIKIPEDIA = "wikipedia";
	private static final String WEATHER = "weather";
	private static final String CHOOSE_PLAN_TYPE_WIKIVOYAGE = "wikivoyage";
	private static final String CHOOSE_PLAN_TYPE_OSMAND_CLOUD = "osmand-cloud";
	private static final String CHOOSE_PLAN_TYPE_ADVANCED_WIDGETS = "advanced-widgets";
	private static final String CHOOSE_PLAN_TYPE_HOURLY_MAP_UPDATES = "hourly-map-updates";
	private static final String CHOOSE_PLAN_TYPE_MONTHLY_MAP_UPDATES = "monthly-map-updates";
	private static final String CHOOSE_PLAN_TYPE_UNLIMITED_MAP_DOWNLOADS = "unlimited-map-downloads";
	private static final String CHOOSE_PLAN_TYPE_COMBINED_WIKI = "combined-wiki";
	private static final String CHOOSE_PLAN_TYPE_EXTERNAL_SENSORS_SUPPORT = "external-sensors-support";
	private static final String CHOOSE_PLAN_TYPE_PRO = "osmand-pro";
	private static final String CHOOSE_PLAN_TYPE_MAPS_PLUS = "osmand-maps-plus";

	private static final String FEATURE_PRO = "pro";
	private static final String FEATURE_MAPS = "maps";
	private static final String FEATURE_LIVE = "live_maps";
	private static final String FEATURE_CONTOURS = "contours";
	private static final String FEATURE_NAUTICAL = "nautical";

	public static void checkAndDisplay(MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		OsmandSettings settings = app.getSettings();
		boolean forceShowDiscountBottomSheet = settings.SHOULD_SHOW_DISCOUNT_BOTTOM_SHEET.get();
		if (!settings.INAPPS_READ.get() && !(Version.isDeveloperVersion(app) && forceShowDiscountBottomSheet)) {
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
		Map<String, String> pms = new LinkedHashMap<>();
		pms.put("version", Version.getFullVersion(app));
		pms.put("nd", String.valueOf(app.getAppInitializer().getFirstInstalledDays()));
		pms.put("ns", String.valueOf(app.getAppInitializer().getNumberOfStarts()));
		pms.put("lang", app.getLanguage() + "");
		List<String> features = getFeatures(app);
		if (!features.isEmpty()) {
			pms.put("features", TextUtils.join(",", features));
		}
		try {
			if (app.isUserAndroidIdAllowed()) {
				pms.put("aid", app.getUserAndroidId());
			}
		} catch (Exception ignore) {
		}
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					return AndroidNetworkUtils.sendRequest(mapActivity.getApp(),
							URL, pms, "Requesting discount info...", false, false);
				} catch (Exception e) {
					logError("Requesting discount info error: ", e);
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				try {
					JSONObject obj = null;
					if (settings.SHOULD_SHOW_DISCOUNT_BOTTOM_SHEET.get()) {
						obj = new JSONObject(app.getString(R.string.test_discount_response));
					} else if (!Algorithms.isEmpty(response)) {
						obj = new JSONObject(response);
					}
					if (obj != null && obj.length() > 0) {
						processDiscountResponse(obj, mapActivity);
					}
				} catch (JSONException e) {
					logError("JSON parsing error: ", e);
				}
			}
		});
	}

	@SuppressLint("SimpleDateFormat")
	private static void processDiscountResponse(@NonNull JSONObject obj, MapActivity mapActivity) {
		try {
			OsmandApplication app = mapActivity.getApp();
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

			if (app.getSettings().SHOULD_SHOW_DISCOUNT_BOTTOM_SHEET.get()) {
				InAppPurchaseHelper purchaseHelper = mapActivity.getPurchaseHelper();
				if (purchaseHelper != null) {
					purchaseHelper.requestInventory(false);
				}
				showDiscountBanner(mapActivity, data);
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
							mapActivity.getFragmentsHelper().showXMasDialog();
						} else {
							InAppPurchaseHelper purchaseHelper = mapActivity.getPurchaseHelper();
							if (purchaseHelper != null) {
								purchaseHelper.requestInventory(false);
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

	private static List<String> getFeatures(@NonNull OsmandApplication app) {
		List<String> res = new ArrayList<>();
		if (InAppPurchaseUtils.isOsmAndProPurchased(app) || InAppPurchaseUtils.isPromoSubscribed(app)) {
			res.add(FEATURE_PRO);
		}
		if (InAppPurchaseUtils.isMapsPlusPurchased(app) || InAppPurchaseUtils.isFullVersionPurchased(app)) {
			res.add(FEATURE_MAPS);
		}
		if (InAppPurchaseUtils.isLiveUpdatesPurchased(app)) {
			res.add(FEATURE_LIVE);
		}
		if (InAppPurchaseUtils.isContourLinesPurchased(app)) {
			res.add(FEATURE_CONTOURS);
		}
		if (InAppPurchaseUtils.isDepthContoursPurchased(app)) {
			res.add(FEATURE_NAUTICAL);
		}
		return res;
	}

	public static boolean validateUrl(OsmandApplication app, String url) {
		if (url.startsWith(INAPP_PREFIX) && url.length() > INAPP_PREFIX.length()) {
			String inAppSku = url.substring(INAPP_PREFIX.length());
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			return purchaseHelper == null || !purchaseHelper.isPurchased(inAppSku);
		}
		return true;
	}

	@Nullable
	private static String getChoosePlanType(@NonNull String url) {
		if (url.startsWith(SHOW_CHOOSE_PLAN_PREFIX) && url.length() > SHOW_CHOOSE_PLAN_PREFIX.length()) {
			return url.substring(SHOW_CHOOSE_PLAN_PREFIX.length());
		}
		return null;
	}

	@Nullable
	private static String getCurrentSalePlanType() {
		return mData == null ? null : mData.choosePlanType;
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

	private static void showDiscountBanner(@NonNull MapActivity mapActivity, ControllerData data) {
		mData = data;
		mBannerVisible = DiscountBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), data);
		mapActivity.getMapActions().updateDrawerMenu();
	}

	private static void showPoiFilter(MapActivity mapActivity, PoiUIFilter poiFilter) {
		QuickSearchHelper.showPoiFilterOnMap(mapActivity, poiFilter, () -> mFilterVisible = false);
		mFilter = poiFilter;
		mFilterVisible = true;
	}

	public static void onDiscountBottomSheetDismissed() {
		mBannerVisible = false;
	}

	@Nullable
	public static String getCurrentSaleTitle() {
		return mData != null ? mData.message : null;
	}

	@Nullable
	public static String getCurrentSaleDiscount(@NonNull OsmandApplication app, boolean nightMode, boolean includeMapsPlan) {
		if (mData == null) {
			return null;
		}
		if (!Algorithms.isEmpty(mData.discount)) {
			return mData.discount;
		}
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper == null) {
			return null;
		}
		List<InAppSubscription> subscriptions = getCurrentSaleSubscriptions(app, purchaseHelper, includeMapsPlan);
		List<SubscriptionButton> buttons = PurchasingUtils.collectSubscriptionButtons(app, purchaseHelper, subscriptions, nightMode);
		for (SubscriptionButton button : buttons) {
			InAppSubscription subscription = button.getPurchaseItem();
			if (button.isDiscountApplied()
					&& subscription.getIntroductoryInfo() != null
					&& !Algorithms.isEmpty(button.getDiscount())) {
				return button.getDiscount();
			}
		}
		return null;
	}

	@NonNull
	private static List<InAppSubscription> getCurrentSaleSubscriptions(@NonNull OsmandApplication app,
	                                                                   @NonNull InAppPurchaseHelper purchaseHelper, boolean includeMapsPlan) {
		if (mData != null && !Algorithms.isEmpty(mData.choosePlanType)) {
			String planType = mData.choosePlanType;
			if (CHOOSE_PLAN_TYPE_PRO.equals(planType)) {
				return PurchasingUtils.getVisibleProSubscriptions(app, purchaseHelper);
			} else if (CHOOSE_PLAN_TYPE_MAPS_PLUS.equals(planType) && includeMapsPlan) {
				return PurchasingUtils.getVisibleMapsSubscriptions(app, purchaseHelper);
			}
			OsmAndFeature feature = mData.getChoosePlanFeature();
			if (feature != null && (!feature.isAvailableInMapsPlus() || includeMapsPlan)) {
				return getFeatureSubscriptions(app, purchaseHelper, feature);
			}
		}
		return Collections.emptyList();
	}

	@NonNull
	private static List<InAppSubscription> getFeatureSubscriptions(@NonNull OsmandApplication app,
	                                                               @NonNull InAppPurchaseHelper purchaseHelper,
	                                                               @NonNull OsmAndFeature feature) {
		List<InAppSubscription> result = new ArrayList<>();
		if (feature.isAvailableInMapsPlus()) {
			result.addAll(PurchasingUtils.getVisibleMapsSubscriptions(app, purchaseHelper));
		}
		result.addAll(PurchasingUtils.getVisibleProSubscriptions(app, purchaseHelper));
		return result;
	}

	public static boolean hasCurrentSale() {
		return mData != null && !Algorithms.isEmpty(mData.message);
	}

	public static boolean shouldShowCurrentSaleInDrawer(@NonNull OsmandApplication app) {
		return hasCurrentSale() && !Version.isPaidVersion(app);
	}

	@DrawableRes
	public static int getCurrentSaleDrawerIconId(boolean nightMode) {
		return isCurrentSaleProPurchase()
				? (nightMode ? R.drawable.ic_action_osmand_pro_logo_colored_night : R.drawable.ic_action_osmand_pro_logo_colored)
				: R.drawable.ic_action_osmand_maps_plus;
	}

	private static boolean isCurrentSaleProPurchase() {
		String planType = getCurrentSalePlanType();
		return CHOOSE_PLAN_TYPE_PRO.equals(planType);
	}

	public static void openCurrentSale(@NonNull MapActivity mapActivity) {
		if (mData != null && !Algorithms.isEmpty(mData.url)) {
			openUrl(mapActivity, mData.url);
		} else {
			ChoosePlanFragment.showDefaultInstance(mapActivity);
		}
	}

	public static void onDiscountBottomSheetClicked(@NonNull MapActivity mapActivity, @NonNull String url) {
		mapActivity.getApp().logEvent("motd_click");
		mBannerVisible = false;
		openUrl(mapActivity, url);
	}

	public static void openUrl(MapActivity mapActivity, String url) {
		if (url.startsWith(INAPP_PREFIX)) {
			OsmandApplication app = mapActivity.getApp();
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				InAppPurchase fullVersion = purchaseHelper.getFullVersion();
				if (fullVersion != null && url.contains(fullVersion.getSku())) {
					app.logEvent("in_app_purchase_redirect");
					try {
						purchaseHelper.purchaseFullVersion(mapActivity);
					} catch (UnsupportedOperationException e) {
						LOG.error("purchaseFullVersion is not supported", e);
					}
				} else {
					InAppPurchases purchases = purchaseHelper.getInAppPurchases();
					for (InAppPurchase p : purchaseHelper.getSubscriptions().getAllSubscriptions()) {
						if (url.contains(p.getSku())) {
							if (purchases.isMaps(p)) {
								MapsPlusPlanFragment.showInstance(mapActivity, p.getSku());
							} else if (purchases.isOsmAndPro(p)) {
								OsmAndProPlanFragment.showInstance(mapActivity, p.getSku());
							} else {
								ChoosePlanFragment.showDefaultInstance(mapActivity);
							}
							break;
						}
					}
				}
			}
		} else if (url.startsWith(SEARCH_QUERY_PREFIX)) {
			String query = url.substring(SEARCH_QUERY_PREFIX.length());
			if (!query.isEmpty()) {
				mapActivity.getFragmentsHelper().showQuickSearch(query);
			}
		} else if (url.startsWith(SHOW_POI_PREFIX)) {
			String names = url.substring(SHOW_POI_PREFIX.length());
			if (!names.isEmpty()) {
				OsmandApplication app = mapActivity.getApp();
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
			if (mData != null && mData.activityJson != null) {
				openActivity(mapActivity, mData.activityJson);
			}
		} else if (url.startsWith(SHOW_CHOOSE_PLAN_PREFIX)) {
			String planType = url.substring(SHOW_CHOOSE_PLAN_PREFIX.length()).trim();
			showDialogForPlanType(mapActivity, planType);
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			AndroidUtils.startActivityIfSafe(mapActivity, intent);
		}
	}

	private static void showDialogForPlanType(@NonNull MapActivity mapActivity, @NonNull String planType) {
		String selectedButtonId = null;
		if (mData != null && mData.urlParams != null) {
			selectedButtonId = mData.urlParams.optString(SELECTED_CHOOSE_PLAN_BUTTON_KEY);
		}
		switch (planType) {
			case CHOOSE_PLAN_TYPE_FREE:
			case CHOOSE_PLAN_TYPE_MAPS_PLUS:
				if (Algorithms.isEmpty(selectedButtonId)) {
					MapsPlusPlanFragment.showInstance(mapActivity);
				} else {
					MapsPlusPlanFragment.showInstance(mapActivity, selectedButtonId);
				}
				break;
			case CHOOSE_PLAN_TYPE_PRO:
				if (Algorithms.isEmpty(selectedButtonId)) {
					OsmAndProPlanFragment.showInstance(mapActivity);
				} else {
					OsmAndProPlanFragment.showInstance(mapActivity, selectedButtonId);
				}
				break;
			case CHOOSE_PLAN_TYPE_SEA_DEPTH:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.NAUTICAL);
				break;
			case CHOOSE_PLAN_TYPE_HILLSHADE:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.TERRAIN);
				break;
			case CHOOSE_PLAN_TYPE_WIKIPEDIA:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.WIKIPEDIA);
				break;
			case CHOOSE_PLAN_TYPE_WIKIVOYAGE:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.WIKIVOYAGE);
				break;
			case CHOOSE_PLAN_TYPE_OSMAND_CLOUD:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.OSMAND_CLOUD);
				break;
			case CHOOSE_PLAN_TYPE_ADVANCED_WIDGETS:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.ADVANCED_WIDGETS);
				break;
			case CHOOSE_PLAN_TYPE_HOURLY_MAP_UPDATES:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.HOURLY_MAP_UPDATES);
				break;
			case CHOOSE_PLAN_TYPE_MONTHLY_MAP_UPDATES:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.MONTHLY_MAP_UPDATES);
				break;
			case CHOOSE_PLAN_TYPE_UNLIMITED_MAP_DOWNLOADS:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.UNLIMITED_MAP_DOWNLOADS);
				break;
			case CHOOSE_PLAN_TYPE_COMBINED_WIKI:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.COMBINED_WIKI);
				break;
			case CHOOSE_PLAN_TYPE_EXTERNAL_SENSORS_SUPPORT:
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.EXTERNAL_SENSORS_SUPPORT);
				break;
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
			AndroidUtils.startActivityIfSafe(context, intent);
		}
	}

	public static class ControllerData {

		private String choosePlanType;
		private String message;
		private String description;
		private String iconId;
		private String url;
		private String textBtnTitle;
		private String discount;

		@ColorInt
		private int iconColor = -1;
		@ColorInt
		private int bgColor = -1;
		@ColorInt
		private int titleColor = -1;
		@ColorInt
		private int descrColor = -1;
		@ColorInt
		private int statusBarColor = -1;
		@ColorInt
		private int textBtnTitleColor = -1;

		private JSONObject urlParams;
		private JSONObject activityJson;
		private JSONArray oneOfConditions;

		@Nullable
		public String getChoosePlanType() {
			return choosePlanType;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		@Nullable
		public String getIconId() {
			return iconId;
		}

		@Nullable
		public String getUrl() {
			return url;
		}

		@Nullable
		public String getTextBtnTitle() {
			return textBtnTitle;
		}

		@Nullable
		public String getDiscount() {
			return discount;
		}

		@Nullable
		public String getSelectedChoosePlanButtonId() {
			return urlParams == null ? null : urlParams.optString(SELECTED_CHOOSE_PLAN_BUTTON_KEY);
		}

		@ColorInt
		public int getIconColor() {
			return iconColor;
		}

		@ColorInt
		public int getTitleColor() {
			return titleColor;
		}

		@ColorInt
		public int getDescriptionColor() {
			return descrColor;
		}

		@ColorInt
		public int getTextBtnTitleColor() {
			return textBtnTitleColor;
		}

		@Nullable
		public OsmAndFeature getChoosePlanFeature() {
			String planType = choosePlanType;
			if (Algorithms.isEmpty(planType)) {
				return null;
			}
			switch (planType) {
				case WEATHER:
					return OsmAndFeature.WEATHER;
				case CHOOSE_PLAN_TYPE_SEA_DEPTH:
					return OsmAndFeature.NAUTICAL;
				case CHOOSE_PLAN_TYPE_HILLSHADE:
					return OsmAndFeature.TERRAIN;
				case CHOOSE_PLAN_TYPE_WIKIPEDIA:
					return OsmAndFeature.WIKIPEDIA;
				case CHOOSE_PLAN_TYPE_WIKIVOYAGE:
					return OsmAndFeature.WIKIVOYAGE;
				case CHOOSE_PLAN_TYPE_OSMAND_CLOUD:
					return OsmAndFeature.OSMAND_CLOUD;
				case CHOOSE_PLAN_TYPE_ADVANCED_WIDGETS:
					return OsmAndFeature.ADVANCED_WIDGETS;
				case CHOOSE_PLAN_TYPE_HOURLY_MAP_UPDATES:
					return OsmAndFeature.HOURLY_MAP_UPDATES;
				case CHOOSE_PLAN_TYPE_MONTHLY_MAP_UPDATES:
					return OsmAndFeature.MONTHLY_MAP_UPDATES;
				case CHOOSE_PLAN_TYPE_UNLIMITED_MAP_DOWNLOADS:
					return OsmAndFeature.UNLIMITED_MAP_DOWNLOADS;
				case CHOOSE_PLAN_TYPE_COMBINED_WIKI:
					return OsmAndFeature.COMBINED_WIKI;
				case CHOOSE_PLAN_TYPE_EXTERNAL_SENSORS_SUPPORT:
					return OsmAndFeature.EXTERNAL_SENSORS_SUPPORT;
				default:
					return null;
			}
		}

		static ControllerData parse(OsmandApplication app, JSONObject obj) throws JSONException {
			ControllerData res = new ControllerData();
			res.message = obj.getString("message");
			res.description = obj.getString("description");
			res.iconId = obj.getString("icon");
			res.url = parseUrl(app, obj.getString("url"));
			res.choosePlanType = DiscountHelper.getChoosePlanType(res.url);
			res.textBtnTitle = obj.optString("button_title");
			res.discount = obj.optString("discount");
			res.iconColor = parseColor("icon_color_default_light", obj);
			res.bgColor = parseColor("bg_color", obj);
			res.titleColor = parseColor("title_color", obj);
			res.descrColor = parseColor("description_color", obj);
			res.statusBarColor = parseColor("status_bar_color", obj);
			res.textBtnTitleColor = parseColor("button_title_color", obj);
			res.urlParams = obj.optJSONObject("url_params");
			res.activityJson = obj.optJSONObject("activity");
			res.oneOfConditions = obj.optJSONArray("oneOfConditions");
			return res;
		}

		private static int parseColor(String key, JSONObject obj) {
			String color = obj.optString(key);
			if (!color.isEmpty()) {
				return Algorithms.parseColor(color);
			}
			return -1;
		}
	}

	private static void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

	private abstract static class Condition {

		protected OsmandApplication app;

		Condition(OsmandApplication app) {
			this.app = app;
		}

		abstract String getId();

		abstract boolean matches(String value);

	}

	private static class NotPurchasedFeatureCondition extends Condition {

		NotPurchasedFeatureCondition(OsmandApplication app) {
			super(app);
		}

		@Override
		String getId() {
			return "not_purchased_feature";
		}

		@Override
		boolean matches(@NonNull String value) {
			return switch (value) {
				case FEATURE_PRO ->
						!InAppPurchaseUtils.isOsmAndProPurchased(app) && !InAppPurchaseUtils.isPromoSubscribed(app);
				case FEATURE_MAPS ->
						!InAppPurchaseUtils.isMapsPlusPurchased(app) && !InAppPurchaseUtils.isFullVersionPurchased(app);
				case FEATURE_LIVE -> !InAppPurchaseUtils.isLiveUpdatesPurchased(app);
				case FEATURE_CONTOURS -> !InAppPurchaseUtils.isContourLinesPurchased(app);
				case FEATURE_NAUTICAL -> !InAppPurchaseUtils.isDepthContoursPurchased(app);
				default -> false;
			};
		}
	}

	private abstract static class InAppPurchaseCondition extends Condition {

		InAppPurchases inAppPurchases;

		InAppPurchaseCondition(OsmandApplication app) {
			super(app);
			inAppPurchases = app.getInAppPurchaseHelper().getInAppPurchases();
		}
	}

	private abstract static class SubscriptionCondition extends Condition {

		InAppSubscriptionList liveUpdates;

		SubscriptionCondition(OsmandApplication app) {
			super(app);
			liveUpdates = app.getInAppPurchaseHelper().getSubscriptions();
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
			OsmandPlugin plugin = PluginsHelper.getPlugin(value);
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
			OsmandPlugin plugin = PluginsHelper.getPlugin(value);
			return plugin != null && !plugin.needsInstallation();
		}
	}

	private static class Conditions {

		protected OsmandApplication app;
		private final Condition[] conditions;

		Conditions(OsmandApplication app) {
			this.app = app;
			conditions = new Condition[] {
					new NotPurchasedFeatureCondition(app),
					new NotPurchasedSubscriptionCondition(app),
					new PurchasedSubscriptionCondition(app),
					new NotPurchasedInAppPurchaseCondition(app),
					new PurchasedInAppPurchaseCondition(app),
					new NotPurchasedPluginCondition(app),
					new PurchasedPluginCondition(app)};
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
