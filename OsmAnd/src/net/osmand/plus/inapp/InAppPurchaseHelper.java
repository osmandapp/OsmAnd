package net.osmand.plus.inapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.Period.PeriodUnit;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchaseExternalInApp;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchaseExternalSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.inapp.InAppPurchases.PurchaseInfo;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.utils.AndroidNetworkUtils.OnSendRequestsListener;
import net.osmand.plus.utils.AndroidNetworkUtils.Request;
import net.osmand.plus.utils.AndroidNetworkUtils.RequestResponse;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.*;

public abstract class InAppPurchaseHelper {

	// Debug tag, for logging
	protected static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(InAppPurchaseHelper.class);
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();

	public static final String SERVER_URL = "https://osmand.net";
	public static final String SUBSCRIPTION_REGISTER_URL = SERVER_URL + "/subscription/register";
	public static final String GET_ACTIVE_SUBSCRIPTIONS_SKU_URL = SERVER_URL + "/api/subscriptions/active";
	public static final String GET_SUBSCRIPTIONS_URL = SERVER_URL + "/api/subscriptions/get";
	public static final String GET_ALL_SUBSCRIPTIONS_URL = SERVER_URL + "/api/subscriptions/getAll";
	public static final String GET_INAPPS_URL = SERVER_URL + "/api/inapps/get";
	public static final String USER_VALIDATE_SUBSCRIPTION_URL = SERVER_URL + "/userdata/user-validate-sub";
	public static final String PURCHASE_COMPLETE_URL = SERVER_URL + "/api/purchase-complete";
	public static final String PURCHASED_URL = SERVER_URL + "/subscription/purchased";

	public static final String PURCHASE_TYPE_SUBSCRIPTION = "subscription";
	public static final String PURCHASE_TYPE_INAPP = "inapp";

	public static final String PLATFORM_GOOGLE = "google";
	public static final String PLATFORM_APPLE = "apple";
	public static final String PLATFORM_AMAZON = "amazon";
	public static final String PLATFORM_HUAWEI = "huawei";
	public static final String PLATFORM_FASTSPRING = "fastspring";

	protected static final String OSMAND_PLUS_APP_ORDER_ID = "OSMAND_PLUS_APP";

	private final boolean mDebugLog = true;

	protected InAppPurchases purchases;
	protected long lastValidationCheckTime;
	protected boolean inventoryRequested;
	protected boolean externalPurchasesRequested;
	protected Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();
	protected Map<String, InAppStateHolder> inAppStateMap = new HashMap<>();

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily

	protected boolean isDeveloperVersion;
	protected String token = "";
	protected InAppPurchaseTaskType activeTask;
	protected boolean processingTask;
	protected boolean inventoryRequestPending;

	protected OsmandApplication ctx;
	protected InAppPurchaseListener uiActivity;

	protected long lastPromoCheckTime;
	protected boolean promoRequested;

	public interface InAppPurchaseListener {

		default void onError(InAppPurchaseTaskType taskType, String error) {

		}

		default void onGetItems() {

		}

		default void onItemPurchased(String sku, boolean active) {

		}

		default void showProgress(InAppPurchaseTaskType taskType) {

		}

		default void dismissProgress(InAppPurchaseTaskType taskType) {

		}
	}

	public interface InAppPurchaseInitCallback {

		void onSuccess();

		void onFail();
	}

	public static class SubscriptionStateHolder {
		public String sku;
		public String name;
		public String icon;
		public SubscriptionState state = SubscriptionState.UNDEFINED;
		public long startTime;
		public long expireTime;
		public PeriodUnit periodUnit;
		public PurchaseOrigin origin;
		public InAppSubscription linkedSubscription;
	}

	public static class InAppStateHolder {
		public String sku;
		public String name;
		public String icon;
		public PurchaseOrigin origin;
		public String platform;
		public long purchaseTime;
		public long expireTime;
		public InAppPurchase linkedPurchase;
	}

	public enum InAppPurchaseTaskType {
		REQUEST_INVENTORY,
		PURCHASE_FULL_VERSION,
		PURCHASE_SUBSCRIPTION,
		PURCHASE_DEPTH_CONTOURS,
		PURCHASE_CONTOUR_LINES
	}

	public abstract static class InAppCommand {

		InAppCommandResultHandler resultHandler;

		// return true if done and false if async task started
		abstract void run(@NonNull InAppPurchaseHelper helper);

		protected boolean userRequested() {
			return false;
		}

		protected void commandDone() {
			InAppCommandResultHandler resultHandler = this.resultHandler;
			if (resultHandler != null) {
				resultHandler.onCommandDone(this);
			}
		}
	}

	public interface InAppCommandResultHandler {
		void onCommandDone(@NonNull InAppCommand command);
	}

	public String getToken() {
		return token;
	}

	public InAppPurchaseTaskType getActiveTask() {
		return activeTask;
	}

	public InAppPurchases getInAppPurchases() {
		return purchases;
	}

	public InAppSubscriptionList getSubscriptions() {
		return purchases.getSubscriptions();
	}

	@Nullable
	public InAppPurchase getFullVersion() {
		return purchases.getFullVersion();
	}

	@Nullable
	public InAppPurchase getDepthContours() {
		return purchases.getDepthContours();
	}

	@Nullable
	public InAppPurchase getContourLines() {
		return purchases.getContourLines();
	}

	public InAppSubscription getMonthlySubscription() {
		return purchases.getMonthlySubscription();
	}

	@Nullable
	public InAppSubscription getPurchasedMonthlySubscription() {
		return purchases.getPurchasedMonthlySubscription();
	}

	@Nullable
	public InAppSubscription getAnyPurchasedOsmAndProSubscription() {
		return purchases.getAnyPurchasedOsmAndProSubscription();
	}

	public abstract boolean isPurchasedLocalFullVersion();

	public abstract boolean isPurchasedLocalDeepContours();

	public abstract boolean isSubscribedToLocalLiveUpdates();

	public abstract boolean isSubscribedToLocalOsmAndPro();

	public abstract boolean isSubscribedToLocalMaps();

	public InAppPurchaseHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		isDeveloperVersion = Version.isDeveloperVersion(ctx);
	}

	@NonNull
	public List<InAppSubscription> getEverMadeSubscriptions() {
		List<InAppSubscription> subscriptions = new ArrayList<>();
		for (InAppSubscription subscription : getSubscriptions().getVisibleSubscriptions()) {
			if (subscription.isPurchased() || subscription.getState() != SubscriptionState.UNDEFINED) {
				subscriptions.add(subscription);
			}
		}
		return subscriptions;
	}

	@Nullable
	public InAppPurchase getEverMadePurchaseBySku(@NonNull String sku) {
		for (InAppPurchase purchase : getEverMadeMainPurchases()) {
			if (Algorithms.objectEquals(purchase.getSku(), sku)) {
				return purchase;
			}
		}
		return null;
	}

	@NonNull
	public List<InAppPurchase> getEverMadeMainPurchases() {
		List<InAppPurchase> purchases = new ArrayList<>(getEverMadeSubscriptions());
		// Add full version if it is purchased or available by default
		InAppPurchase fullVersion = getFullVersion();
		if (fullVersion != null) {
			boolean isFullVersionByDefault = Version.isFullVersion(ctx) && !Version.isDeveloperBuild(ctx);
			if (fullVersion.isPurchased() || isFullVersionByDefault) {
				purchases.add(fullVersion);
			}
		}
		return purchases;
	}

	@NonNull
	public abstract String getPlatform();

	@NonNull
	public List<SubscriptionStateHolder> getExternalSubscriptions() {
		List<SubscriptionStateHolder> res = new ArrayList<>();
		PurchaseOrigin origin = getPlatformOrigin();
		for (SubscriptionStateHolder holder : subscriptionStateMap.values()) {
			if (holder.linkedSubscription != null && holder.origin != origin) {
				res.add(holder);
			}
		}
		return res;
	}

	@NonNull
	public List<InAppStateHolder> getExternalInApps() {
		List<InAppStateHolder> res = new ArrayList<>();
		String platform = getPlatform();
		for (InAppStateHolder holder : inAppStateMap.values()) {
			if (holder.linkedPurchase != null && !platform.equals(holder.platform)) {
				res.add(holder);
			}
		}
		return res;
	}

	public static void subscribe(@NonNull Activity activity, @NonNull InAppPurchaseHelper purchaseHelper, @NonNull String sku) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		OsmandSettings settings = app.getSettings();
		if (settings.isInternetConnectionAvailable(true)) {
			purchaseHelper.purchaseSubscription(activity, sku);
		} else {
			app.showToastMessage(R.string.internet_not_available);
		}
	}

	public abstract void isInAppPurchaseSupported(@NonNull Activity activity, @Nullable InAppPurchaseInitCallback callback);

	public boolean hasInventory() {
		return lastValidationCheckTime != 0;
	}

	public boolean isPurchased(String sku) {
		List<InAppPurchase> allPurchases = purchases.getAllInAppPurchases(true);
		for (InAppPurchase p : allPurchases) {
			if (p.getSku().equals(sku) && p.isPurchased()) {
				return true;
			}
		}
		InAppPurchase purchase = purchases.getInAppPurchaseBySku(sku);
		if (purchase == null) {
			purchase = purchases.getInAppSubscriptionBySku(sku);
		}
		if (purchase != null) {
			if (purchases.isFullVersion(purchase) && InAppPurchaseUtils.isFullVersionAvailable(ctx)) {
				return true;
			} else if (purchases.isDepthContours(purchase) && InAppPurchaseUtils.isDepthContoursAvailable(ctx)) {
				return true;
			}
			int featureId = purchase.getFeatureId();
			for (InAppPurchase p : allPurchases) {
				if (p.hasFeatureInScope(featureId)) {
					if (p.isPurchased()) {
						return true;
					} else {
						if (purchases.isLiveUpdates(p) && InAppPurchaseUtils.isLiveUpdatesAvailable(ctx)) {
							return true;
						} else if (purchases.isOsmAndPro(p) && InAppPurchaseUtils.isOsmAndProAvailable(ctx)) {
							return true;
						} else if (purchases.isMaps(p) && InAppPurchaseUtils.isMapsPlusAvailable(ctx)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected void exec(@NonNull InAppPurchaseTaskType taskType, @NonNull InAppCommand command) {
		if (isDeveloperVersion || (!Version.isGooglePlayEnabled() && !Version.isHuawei() && !Version.isAmazon())) {
			notifyDismissProgress(taskType);
			stop(true);
			return;
		}

		if (processingTask) {
			if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
				inventoryRequestPending = true;
			}
			logError("Already processing task: " + activeTask + ". Exit.");
			return;
		}

		// Create the helper, passing it our context and the public key to verify signatures with
		logDebug("Creating InAppPurchaseHelper.");

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		logDebug("Starting setup.");
		try {
			processingTask = true;
			activeTask = taskType;
			command.resultHandler = new InAppCommandResultHandler() {
				@Override
				public void onCommandDone(@NonNull InAppCommand command) {
					processingTask = false;
				}
			};
			execImpl(taskType, command);
		} catch (Exception e) {
			logError("exec Error", e);
			stop(true);
		}
	}

	protected abstract void execImpl(@NonNull InAppPurchaseTaskType taskType, @NonNull InAppCommand command);

	public boolean needRequestInventory() {
		return !inventoryRequested && ((InAppPurchaseUtils.isSubscribedToAny(ctx) && Algorithms.isEmpty(ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get()))
				|| System.currentTimeMillis() - lastValidationCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC);
	}

	public boolean needRequestPromo() {
		return !promoRequested || System.currentTimeMillis() - lastPromoCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC;
	}

	public void requestInventory(boolean userRequested) {
		notifyShowProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
		OsmAndTaskManager.executeTask(new RequestInventoryTask(userRequested));
	}

	public abstract void purchaseFullVersion(@NonNull Activity activity) throws UnsupportedOperationException;

	public void purchaseSubscription(@NonNull Activity activity, String sku) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
		OsmAndTaskManager.executeTask(new SubscriptionPurchaseTask(activity, sku));
	}

	public abstract void purchaseDepthContours(@NonNull Activity activity) throws UnsupportedOperationException;

	public abstract void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException;

	public abstract void manageSubscription(@NonNull Context ctx, @Nullable String sku, @Nullable PurchaseOrigin origin);

	protected boolean isUserInfoSupported() {
		return true;
	}

	@SuppressLint("StaticFieldLeak")
	private class SubscriptionPurchaseTask extends AsyncTask<Void, Void, String> {

		private final WeakReference<Activity> activity;

		private final String sku;
		private String userId;

		SubscriptionPurchaseTask(Activity activity, String sku) {
			this.activity = new WeakReference<>(activity);
			this.sku = sku;
		}

		@Override
		protected void onPreExecute() {
			userId = ctx.getSettings().BILLING_USER_ID.get();
		}

		@Override
		protected String doInBackground(Void... params) {
			if (isUserInfoSupported() && (Algorithms.isEmpty(userId) || Algorithms.isEmpty(token))) {
				try {
					Map<String, String> parameters = new HashMap<>();
					addUserInfo(parameters);
					return AndroidNetworkUtils.sendRequest(ctx, SUBSCRIPTION_REGISTER_URL,
							parameters, "Requesting userId...", true, true);

				} catch (Exception e) {
					logError("sendRequest Error", e);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String response) {
			if (!isUserInfoSupported()) {
				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
				logDebug("Launching purchase flow for " + sku + " subscription");
				exec(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, getPurchaseSubscriptionCommand(activity, sku, null));
				return;
			}
			logDebug("Response=" + response);
			if (response == null) {
				if (!Algorithms.isEmpty(userId)) {
					if (Algorithms.isEmpty(token)) {
						complain("User token is empty.");
						notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
						notifyError(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, "User token is empty.");
						stop(true);
						return;
					}
				} else {
					complain("Cannot retrieve userId from server.");
					notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
					notifyError(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, "Cannot retrieve userId from server.");
					stop(true);
					return;
				}
			} else {
				try {
					JSONObject obj = new JSONObject(response);
					userId = obj.getString("userid");
					ctx.getSettings().BILLING_USER_ID.set(userId);
					token = obj.getString("token");
					ctx.getSettings().BILLING_USER_TOKEN.set(token);
					logDebug("UserId=" + userId);
				} catch (JSONException e) {
					String message = "JSON parsing error: "
							+ (e.getMessage() == null ? "unknown" : e.getMessage());
					complain(message);
					notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
					notifyError(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, message);
					stop(true);
				}
			}

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
			if (!Algorithms.isEmpty(userId) && !Algorithms.isEmpty(token)) {
				logDebug("Launching purchase flow for " + sku + " subscription for userId=" + userId);
				String userInfo = userId + " " + token;
				exec(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, getPurchaseSubscriptionCommand(activity, sku, userInfo));
			} else {
				notifyError(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, "Empty userId");
				stop(true);
			}
		}
	}

	protected abstract InAppCommand getPurchaseSubscriptionCommand(WeakReference<Activity> activity,
	                                                               String sku, String userInfo) throws UnsupportedOperationException;

	@SuppressLint("StaticFieldLeak")
	private class RequestInventoryTask extends AsyncTask<Void, Void, String[]> {

		private final boolean userRequested;

		RequestInventoryTask(boolean userRequested) {
			this.userRequested = userRequested;
		}

		@Override
		protected String[] doInBackground(Void... params) {
			String activeSubscriptionsIds = null;
			String subscriptionsState = null;
			String inappsState = null;
			try {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("androidPackage", ctx.getPackageName());
				addUserInfo(parameters);
				activeSubscriptionsIds = AndroidNetworkUtils.sendRequest(ctx,
						GET_ACTIVE_SUBSCRIPTIONS_SKU_URL,
						parameters, "Requesting active subscriptions...", false, false);

				boolean hasToken = false;
				String userId = ctx.getSettings().BILLING_USER_ID.get();
				String userToken = ctx.getSettings().BILLING_USER_TOKEN.get();
				if (!Algorithms.isEmpty(userId) && !Algorithms.isEmpty(userToken)) {
					parameters.put("userId", userId);
					parameters.put("userToken", userToken);
					hasToken = true;
				}
				String deviceId = ctx.getSettings().BACKUP_DEVICE_ID.get();
				String accessToken = ctx.getSettings().BACKUP_ACCESS_TOKEN.get();
				if (!Algorithms.isEmpty(deviceId) && !Algorithms.isEmpty(accessToken)) {
					parameters.put("deviceId", deviceId);
					parameters.put("accessToken", accessToken);
					hasToken = true;
				}
				if (hasToken) {
					subscriptionsState = AndroidNetworkUtils.sendRequest(ctx, GET_ALL_SUBSCRIPTIONS_URL,
							parameters, "Requesting subscriptions state...", false, false);
					inappsState = AndroidNetworkUtils.sendRequest(ctx, GET_INAPPS_URL,
							parameters, "Requesting inapps state...", false, false);
				}
			} catch (Exception e) {
				logError("sendRequest Error", e);
			}
			if (userRequested) {
				ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.set("");
			}
			return new String[] {activeSubscriptionsIds, subscriptionsState, inappsState};
		}

		@Override
		protected void onPostExecute(String[] response) {
			logDebug("Response=" + Arrays.toString(response));
			String activeSubscriptionsIdsJson = response[0];
			String subscriptionsStateJson = response[1];
			String inappsStateJson = response[2];
			if (activeSubscriptionsIdsJson != null) {
				inventoryRequested = true;
				try {
					JSONObject obj = new JSONObject(activeSubscriptionsIdsJson);
					JSONArray names = obj.names();
					if (names != null) {
						for (int i = 0; i < names.length(); i++) {
							String skuType = names.getString(i);
							JSONObject subObj = obj.getJSONObject(skuType);
							String sku = subObj.getString("sku");
							if (!Algorithms.isEmpty(sku)) {
								getSubscriptions().upgradeSubscription(sku);
							}
						}
					}
				} catch (JSONException e) {
					logError("Json parsing error", e);
				}
			}
			if (subscriptionsStateJson != null) {
				inventoryRequested = true;
				subscriptionStateMap = parseSubscriptionStates(subscriptionsStateJson);
			}
			if (inappsStateJson != null) {
				inventoryRequested = true;
				inAppStateMap = parseInAppStates(inappsStateJson);
			}
			externalPurchasesRequested = subscriptionsStateJson != null && inappsStateJson != null;
			exec(InAppPurchaseTaskType.REQUEST_INVENTORY, getRequestInventoryCommand(userRequested));
		}
	}

	@NonNull
	Map<String, SubscriptionStateHolder> parseSubscriptionStates(@NonNull String subscriptionsStateJson) {
		Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();
		try {
			JSONArray subArrJson = new JSONArray(subscriptionsStateJson);
			for (int i = 0; i < subArrJson.length(); i++) {
				JSONObject subObj = subArrJson.getJSONObject(i);
				boolean valid = subObj.getBoolean("valid");
				if (!valid) {
					continue;
				}
				String sku = subObj.getString("sku");
				String state = subObj.getString("state");
				String name = subObj.optString("name", null);
				String icon = subObj.optString("icon", null);

				if (!Algorithms.isEmpty(sku) && !Algorithms.isEmpty(state)) {
					SubscriptionStateHolder stateHolder = new SubscriptionStateHolder();
					stateHolder.sku = sku;
					stateHolder.name = name;
					stateHolder.icon = icon;
					stateHolder.state = SubscriptionState.getByStateStr(state);
					stateHolder.startTime = subObj.optLong("start_time");
					stateHolder.expireTime = subObj.optLong("expire_time");
					stateHolder.origin = getPurchaseOriginBySku(sku);
					try {
						stateHolder.linkedSubscription = InAppPurchaseExternalSubscription.buildFromJson(ctx, subObj);
					} catch (Exception e) {
						LOG.error("Subscription state json parsing error = " + subObj, e);
					}

					PeriodUnit periodUnit = null;
					if (stateHolder.origin == PurchaseOrigin.PROMO || sku.contains("annual")) {
						periodUnit = PeriodUnit.YEAR;
					} else if (sku.contains("monthly")) {
						periodUnit = PeriodUnit.MONTH;
					}
					stateHolder.periodUnit = periodUnit;

					subscriptionStateMap.put(sku, stateHolder);
				}
			}
		} catch (JSONException e) {
			logError("Subscription state json parsing error", e);
		}
		return subscriptionStateMap;
	}

	@NonNull
	Map<String, InAppStateHolder> parseInAppStates(@NonNull String inAppsStateJson) {
		Map<String, InAppStateHolder> inappStateMap = new HashMap<>();
		try {
			JSONArray subArrJson = new JSONArray(inAppsStateJson);
			for (int i = 0; i < subArrJson.length(); i++) {
				JSONObject subObj = subArrJson.getJSONObject(i);
				String sku = subObj.getString("sku");
				String name = subObj.optString("name", null);
				String icon = subObj.optString("icon", null);
				String platform = subObj.optString("platform", null);
				long purchaseTime = subObj.optLong("purchaseTime", 0);
				long expireTime = subObj.optLong("expireTime", 0);
				if (!Algorithms.isEmpty(sku)) {
					InAppStateHolder stateHolder = new InAppStateHolder();
					stateHolder.sku = sku;
					stateHolder.name = name;
					stateHolder.icon = icon;
					stateHolder.origin = getPurchaseOriginBySku(sku);
					stateHolder.platform = platform;
					stateHolder.purchaseTime = purchaseTime;
					stateHolder.expireTime = expireTime;
					try {
						stateHolder.linkedPurchase = InAppPurchaseExternalInApp.buildFromJson(ctx, subObj);
					} catch (Exception e) {
						LOG.error("InApp state json parsing error = " + subObj, e);
					}
					inappStateMap.put(sku, stateHolder);
				}
			}
		} catch (JSONException e) {
			logError("Inapp state json parsing error", e);
		}
		return inappStateMap;
	}

	protected void applyPurchases() {
		boolean externalPurchasesHandled = !ctx.getBackupHelper().isRegistered()
				|| this.externalPurchasesRequested;

		boolean purchasedFullVersion = isPurchasedLocalFullVersion() || isPurchasedExternalFullVersion();
		boolean depthContoursPurchased = isPurchasedLocalDeepContours();
		if (purchasedFullVersion) {
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
		}
		if (depthContoursPurchased) {
			ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
		}

		boolean subscribedToLiveUpdates = isSubscribedToLocalLiveUpdates();
		boolean subscribedToMaps = isSubscribedToLocalMaps() || isSubscribedToExternalMaps();
		boolean subscribedToOsmAndPro = isSubscribedToLocalOsmAndPro() || isSubscribedToExternalOsmAndPro() || isPurchasedExternalOsmAndPro();
		if (!subscribedToLiveUpdates && ctx.getSettings().LIVE_UPDATES_PURCHASED.get() && externalPurchasesHandled) {
			ctx.getSettings().LIVE_UPDATES_PURCHASED.set(false);
		} else if (subscribedToLiveUpdates) {
			ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
		}
		if (!subscribedToOsmAndPro && ctx.getSettings().OSMAND_PRO_PURCHASED.get() && externalPurchasesHandled) {
			ctx.getSettings().OSMAND_PRO_PURCHASED.set(false);
		} else if (subscribedToOsmAndPro) {
			ctx.getSettings().OSMAND_PRO_PURCHASED.set(true);
		}
		if (!subscribedToMaps && ctx.getSettings().OSMAND_MAPS_PURCHASED.get() && externalPurchasesHandled) {
			ctx.getSettings().OSMAND_MAPS_PURCHASED.set(false);
		} else if (subscribedToMaps) {
			ctx.getSettings().OSMAND_MAPS_PURCHASED.set(true);
		}
		if (!subscribedToLiveUpdates && !subscribedToOsmAndPro && !subscribedToMaps && externalPurchasesHandled) {
			if (!InAppPurchaseUtils.isDepthContoursAvailable(ctx)) {
				ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(false);
			}
		}

		logDebug("User " + (purchasedFullVersion ? "HAS" : "DOES NOT HAVE") + " Full Version purchased.");
		logDebug("User " + (subscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE") + " Live Updates purchased.");
		logDebug("User " + (subscribedToOsmAndPro ? "HAS" : "DOES NOT HAVE") + " OsmAnd Pro purchased.");
		logDebug("User " + (subscribedToMaps ? "HAS" : "DOES NOT HAVE") + " Maps purchased.");
	}

	protected boolean isPurchasedExternalFullVersion() {
		for (InAppStateHolder holder : inAppStateMap.values()) {
			if (holder.linkedPurchase != null && holder.linkedPurchase.isFullVersion()) {
				return true;
			}
		}
		return false;
	}

	protected boolean isPurchasedExternalOsmAndPro() {
		for (InAppStateHolder holder : inAppStateMap.values()) {
			if (holder.linkedPurchase != null && holder.linkedPurchase.isOsmAndPro()) {
				return true;
			}
		}
		return false;
	}

	protected boolean isSubscribedToExternalOsmAndPro() {
		for (SubscriptionStateHolder holder : subscriptionStateMap.values()) {
			if (holder.linkedSubscription != null && holder.linkedSubscription.isOsmAndPro()) {
				return true;
			}
		}
		return false;
	}

	protected boolean isSubscribedToExternalMaps() {
		for (SubscriptionStateHolder holder : subscriptionStateMap.values()) {
			if (holder.linkedSubscription != null && holder.linkedSubscription.isMaps()) {
				return true;
			}
		}
		return false;
	}

	public void checkPromoAsync(@Nullable CallbackWithObject<Boolean> listener) {
		OsmAndTaskManager.executeTask(new CheckBackupSubscriptionTask(listener));
	}

	@SuppressLint("StaticFieldLeak")
	private class CheckBackupSubscriptionTask extends AsyncTask<Void, Void, Boolean> {

		private final CallbackWithObject<Boolean> listener;

		public CheckBackupSubscriptionTask(@Nullable CallbackWithObject<Boolean> listener) {
			this.listener = listener;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			boolean subscriptionActive = false;
			try {
				String promocode = ctx.getSettings().BACKUP_PROMOCODE.get();
				if (!Algorithms.isEmpty(promocode)) {
					subscriptionActive = checkBackupSubscription(promocode);
				}
				if (!subscriptionActive) {
					// Get only PRO subscriptions
					String orderId = getOrderIdByDeviceIdAndToken();
					if (!Algorithms.isEmpty(orderId)) {
						subscriptionActive = checkBackupSubscription(orderId);
					}
				}
			} catch (Exception e) {
				logError("checkPromoAsync Error", e);
			}
			return subscriptionActive;
		}

		private boolean checkBackupSubscription(@NonNull String orderId) {
			Map<String, SubscriptionStateHolder> subscriptionStates = getSubscriptionStatesByOrderId(orderId);
			if (!Algorithms.isEmpty(subscriptionStates)) {
				SubscriptionStateHolder stateHolder = subscriptionStates.entrySet().iterator().next().getValue();
				OsmandSettings settings = ctx.getSettings();
				settings.BACKUP_PURCHASE_STATE.set(stateHolder.state);
				settings.BACKUP_PURCHASE_START_TIME.set(stateHolder.startTime);
				settings.BACKUP_PURCHASE_EXPIRE_TIME.set(stateHolder.expireTime);
				settings.BACKUP_PURCHASE_PERIOD.set(stateHolder.periodUnit);
				settings.BACKUP_SUBSCRIPTION_ORIGIN.set(stateHolder.origin);
				settings.BACKUP_SUBSCRIPTION_SKU.set(stateHolder.sku);
				return stateHolder.state.isActive();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean active) {
			promoRequested = true;
			lastPromoCheckTime = System.currentTimeMillis();
			ctx.getSettings().BACKUP_PURCHASE_ACTIVE.set(active);
			notifyGetItems();
			if (listener != null) {
				listener.processResult(active);
			}
		}
	}

	public boolean checkBackupSubscriptions() {
		boolean subscriptionActive = false;
		String promocode = ctx.getSettings().BACKUP_PROMOCODE.get();
		if (!Algorithms.isEmpty(promocode)) {
			subscriptionActive = checkSubscriptionByOrderId(promocode);
		}
		if (!subscriptionActive) {
			String orderId = getOrderIdByDeviceIdAndToken();
			if (!Algorithms.isEmpty(orderId)) {
				subscriptionActive = checkSubscriptionByOrderId(orderId);
			}
		}
		return subscriptionActive;
	}

	private boolean checkSubscriptionByOrderId(@NonNull String orderId) {
		boolean active = false;
		Map<String, SubscriptionStateHolder> subscriptionStates = getSubscriptionStatesByOrderId(orderId);
		if (!Algorithms.isEmpty(subscriptionStates)) {
			for (SubscriptionStateHolder stateHolder : subscriptionStates.values()) {
				active |= stateHolder.state.isActive();
			}
		}
		return active;
	}

	private String getOrderIdByDeviceIdAndToken() {
		String[] orderId = new String[1];
		String deviceId = ctx.getSettings().BACKUP_DEVICE_ID.get();
		String accessToken = ctx.getSettings().BACKUP_ACCESS_TOKEN.get();
		if (!Algorithms.isEmpty(deviceId) && !Algorithms.isEmpty(accessToken)) {
			Map<String, String> params = new HashMap<>();
			params.put("deviceid", deviceId);
			params.put("accessToken", accessToken);
			AndroidNetworkUtils.sendRequest(ctx, USER_VALIDATE_SUBSCRIPTION_URL,
					params, "Validate user subscription", false, false, (result, error, resultCode) -> {
						if (Algorithms.isEmpty(error)) {
							if (result != null) {
								try {
									JSONObject obj = new JSONObject(result);
									orderId[0] = obj.optString("orderid");
								} catch (JSONException e) {
									logError("Json parsing error", e);
								}
							}
						} else {
							logError(error);
						}
					});
		}
		return orderId[0];
	}

	private Map<String, SubscriptionStateHolder> getSubscriptionStatesByOrderId(@NonNull String orderId) {
		Map<String, String> params = new HashMap<>();
		params.put("orderId", orderId);
		String subscriptionsState = AndroidNetworkUtils.sendRequest(ctx, GET_SUBSCRIPTIONS_URL,
				params, "Requesting promo subscription state", false, false);

		if (subscriptionsState != null) {
			return parseSubscriptionStates(subscriptionsState);
		}
		return null;
	}

	protected abstract InAppCommand getRequestInventoryCommand(boolean userRequested) throws UnsupportedOperationException;

	protected void onProductDetailsResponseDone(@NonNull List<PurchaseInfo> purchaseInfoList, boolean userRequested) {
		OnRequestResultListener listener = (result, error, resultCode) -> {
			notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
			notifyGetItems();
			stop(true);
			logDebug("Initial inapp query finished");
			if (userRequested) {
				showToast(ctx.getString(R.string.purchases_restored));
			}
		};

		if (purchaseInfoList.size() > 0) {
			sendTokens(purchaseInfoList, listener);
		} else {
			listener.onResult("OK", null, null);
		}
	}

	protected void addUserInfo(Map<String, String> parameters) {
		parameters.put("version", Version.getFullVersion(ctx));
		parameters.put("lang", ctx.getLanguage());
		parameters.put("nd", String.valueOf(ctx.getAppInitializer().getFirstInstalledDays()));
		parameters.put("ns", String.valueOf(ctx.getAppInitializer().getNumberOfStarts()));
		if (ctx.isUserAndroidIdAllowed()) {
			parameters.put("aid", ctx.getUserAndroidId());
		}
	}

	protected void onPurchaseDone(PurchaseInfo info) {
		logDebug("Purchase successful.");

		InAppSubscription subscription = getSubscriptions().getSubscriptionBySku(info.getSku().get(0));
		InAppPurchase fullVersion = getFullVersion();
		InAppPurchase depthContours = getDepthContours();
		InAppPurchase contourLines = getContourLines();
		if (subscription != null) {
			boolean maps = purchases.isMaps(subscription);
			boolean liveUpdates = purchases.isLiveUpdates(subscription);
			boolean pro = purchases.isOsmAndPro(subscription);
			// bought live updates
			if (maps) {
				logDebug("Maps subscription purchased.");
			} else if (liveUpdates) {
				logDebug("Live updates subscription purchased.");
			} else if (pro) {
				logDebug("OsmAnd Pro subscription purchased.");
			}
			String sku = subscription.getSku();
			subscription.setPurchaseState(PurchaseState.PURCHASED);
			subscription.setPurchaseInfo(ctx, info);
			subscription.setState(ctx, SubscriptionState.UNDEFINED);
			logDebug("Sending tokens...");
			sendTokens(Collections.singletonList(info), new OnRequestResultListener() {
				@Override
				public void onResult(@Nullable String result, @Nullable String error, @Nullable Integer resultCode) {
					logDebug("Tokens sent");
					boolean active = false;
					if (liveUpdates || pro) {
						active = ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
						if (pro) {
							ctx.getSettings().OSMAND_PRO_PURCHASED.set(true);
						}
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);
						ctx.getSettings().LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME.set(0L);
						ctx.getSettings().LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME.set(0L);
					} else if (maps) {
						active = ctx.getSettings().OSMAND_MAPS_PURCHASED.get();
						ctx.getSettings().OSMAND_MAPS_PURCHASED.set(true);
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);
					}
					notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
					notifyItemPurchased(sku, active);
					refreshAndroidAuto();
					stop(true);
				}
			});

			sendPurchaseComplete(info);

		} else if (fullVersion != null && info.getSku().contains(fullVersion.getSku())) {
			// bought full version
			fullVersion.setPurchaseState(PurchaseState.PURCHASED);
			fullVersion.setPurchaseInfo(ctx, info);
			logDebug("Full version purchased.");
			showToast(ctx.getString(R.string.full_version_thanks));
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

			sendPurchaseComplete(info);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
			notifyItemPurchased(fullVersion.getSku(), false);
			refreshAndroidAuto();
			stop(true);

		} else if (depthContours != null && info.getSku().contains(depthContours.getSku())) {
			// bought sea depth contours
			depthContours.setPurchaseState(PurchaseState.PURCHASED);
			depthContours.setPurchaseInfo(ctx, info);
			logDebug("Sea depth contours purchased.");
			showToast(ctx.getString(R.string.sea_depth_thanks));
			ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

			sendPurchaseComplete(info);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
			notifyItemPurchased(depthContours.getSku(), false);
			stop(true);

		} else if (contourLines != null && info.getSku().contains(contourLines.getSku())) {
			// bought contour lines
			contourLines.setPurchaseState(PurchaseState.PURCHASED);
			contourLines.setPurchaseInfo(ctx, info);
			logDebug("Contours lines purchased.");
			showToast(ctx.getString(R.string.contour_lines_thanks));
			ctx.getSettings().CONTOUR_LINES_PURCHASED.set(true);

			sendPurchaseComplete(info);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_CONTOUR_LINES);
			notifyItemPurchased(contourLines.getSku(), false);
			stop(true);

		} else {
			notifyDismissProgress(activeTask);
			stop(true);
		}
	}

	private void refreshAndroidAuto() {
		NavigationSession carNavigationSession = ctx.getCarNavigationSession();
		if (carNavigationSession != null) {
			logDebug("Call Android Auto");
			carNavigationSession.onPurchaseDone();
		}
	}

	// Do not forget call stop() when helper is not needed anymore
	public void stop() {
		stop(false);
	}

	protected abstract boolean isBillingManagerExists();

	protected abstract void destroyBillingManager();

	protected void stop(boolean taskDone) {
		logDebug("Destroying helper.");
		InAppPurchaseTaskType task = activeTask;
		if (isBillingManagerExists()) {
			if (taskDone) {
				processingTask = false;
			}
			if (!processingTask) {
				activeTask = null;
				destroyBillingManager();
			}
		} else {
			processingTask = false;
			activeTask = null;
		}
		if (inventoryRequestPending) {
			inventoryRequestPending = false;
			requestInventory(false);
		} else {
			if (task == InAppPurchaseTaskType.REQUEST_INVENTORY) {
				applyPurchases();
			}
		}
	}

	private void sendPurchaseComplete(@NonNull PurchaseInfo info) {
		try {
            Map<String, String> params = new HashMap<>();
			params.put("purchaseId", info.getSku().get(0));
			params.put("orderId", info.getOrderId());
			addUserInfo(params);
			AndroidNetworkUtils.sendRequestAsync(ctx, PURCHASE_COMPLETE_URL, params, "Sending purchase complete...", false, false, null);
		} catch (Exception e) {
			logError("SendPurchaseComplete Error", e);
		}
	}

	public void resetPurchases() {
		subscriptionStateMap = new HashMap<>();
		inAppStateMap = new HashMap<>();
	}

	protected void sendTokens(@NonNull List<PurchaseInfo> purchaseInfoList, @Nullable OnRequestResultListener listener) {
		String userId = ctx.getSettings().BILLING_USER_ID.get();
		String token = ctx.getSettings().BILLING_USER_TOKEN.get();
		String email = ctx.getSettings().BILLING_USER_EMAIL.get();
		String deviceId = ctx.getSettings().BACKUP_DEVICE_ID.get();
		String accessToken = ctx.getSettings().BACKUP_ACCESS_TOKEN.get();
		try {
            String userOperation = "Sending purchase info...";
			List<Request> requests = new ArrayList<>();
			for (PurchaseInfo info : purchaseInfoList) {
				String sku = info.getSku().get(0);
				Map<String, String> parameters = new HashMap<>();
				parameters.put("purchaseType", purchases.getInAppPurchaseBySku(sku) != null
						? PURCHASE_TYPE_INAPP : PURCHASE_TYPE_SUBSCRIPTION);
				parameters.put("userid", userId);
				parameters.put("sku", sku);
				parameters.put("platform", getPlatform());
				parameters.put("orderId", info.getOrderId());
				parameters.put("purchaseToken", info.getPurchaseToken());
				parameters.put("email", email);
				parameters.put("token", token);
				if (!Algorithms.isEmpty(deviceId) && !Algorithms.isEmpty(accessToken)) {
					parameters.put("deviceid", deviceId);
					parameters.put("accessToken", accessToken);
				}
				addUserInfo(parameters);
				requests.add(new Request(PURCHASED_URL, parameters, userOperation, true, true));
			}
			AndroidNetworkUtils.sendRequestsAsync(ctx, requests, new OnSendRequestsListener() {
				@Override
				public void onRequestSending(@NonNull Request request) {
				}

				@Override
				public void onRequestSent(@NonNull RequestResponse response) {
				}

				@Override
				public void onRequestsSent(@NonNull List<RequestResponse> results) {
					for (RequestResponse rr : results) {
						String sku = rr.getRequest().getParameters().get("sku");
						PurchaseInfo info = getPurchaseInfo(sku);
						if (info != null) {
							updateSentTokens(info);
							String result = rr.getResponse();
							if (result != null) {
								try {
									JSONObject obj = new JSONObject(result);
									if (obj.has("error")) {
										complain("SendToken Error: "
												+ obj.getString("error")
												+ " (response=" + result + " google=" + info + ")");
									}
								} catch (JSONException e) {
									logError("SendToken", e);
									complain("SendToken Error: "
											+ (e.getMessage() != null ? e.getMessage() : "JSONException")
											+ " (response=" + result + " google=" + info + ")");
								}
							}
						}
					}
					if (listener != null) {
						listener.onResult("OK", null, null);
					}
				}

				private void updateSentTokens(@NonNull PurchaseInfo info) {
					String tokensSentStr = ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get();
					Set<String> tokensSent = new HashSet<>(Arrays.asList(tokensSentStr.split(";")));
					tokensSent.add(info.getSku().get(0));
					ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.set(TextUtils.join(";", tokensSent));
				}

				@Nullable
				private PurchaseInfo getPurchaseInfo(String sku) {
					for (PurchaseInfo info : purchaseInfoList) {
						if (info.getSku().contains(sku)) {
							return info;
						}
					}
					return null;
				}
			});
		} catch (Exception e) {
			logError("SendToken Error", e);
			if (listener != null) {
				listener.onResult("Error", null, null);
			}
		}
	}

	public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
		return false;
	}

	protected void notifyError(InAppPurchaseTaskType taskType, String message) {
		ctx.runInUIThread(() -> {
			if (uiActivity != null) {
				uiActivity.onError(taskType, message);
			}
		});
	}

	protected void notifyGetItems() {
		ctx.runInUIThread(() -> {
			if (uiActivity != null) {
				uiActivity.onGetItems();
			}
		});
	}

	protected void notifyItemPurchased(String sku, boolean active) {
		ctx.runInUIThread(() -> {
			if (uiActivity != null) {
				uiActivity.onItemPurchased(sku, active);
			}
		});
	}

	protected void notifyShowProgress(InAppPurchaseTaskType taskType) {
		ctx.runInUIThread(() -> {
			if (uiActivity != null) {
				uiActivity.showProgress(taskType);
			}
		});
	}

	protected void notifyDismissProgress(InAppPurchaseTaskType taskType) {
		ctx.runInUIThread(() -> {
			if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
				OsmAndTaskManager.executeTask(new CheckBackupSubscriptionTask(null));
			}
			if (uiActivity != null) {
				uiActivity.dismissProgress(taskType);
			}
		});
	}

	/// UI notifications methods
	public void setUiActivity(InAppPurchaseListener uiActivity) {
		this.uiActivity = uiActivity;
	}

	public void resetUiActivity(InAppPurchaseListener uiActivity) {
		if (this.uiActivity == uiActivity) {
			this.uiActivity = null;
		}
	}

	protected void complain(String message) {
		logError("**** InAppPurchaseHelper Error: " + message);
		showToast(message);
	}

	protected void showToast(String message) {
		ctx.showToastMessage(message);
	}

	protected void logDebug(String msg) {
		if (mDebugLog) {
			Log.d(TAG, msg);
		}
	}

	protected void logError(String msg) {
		Log.e(TAG, msg);
	}

	protected void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

	@NonNull
	public PurchaseOrigin getPurchaseOriginBySku(@NonNull String sku) {
		if (sku.startsWith("promo_")) {
			return PurchaseOrigin.PROMO;
		}
		if (sku.startsWith("net.osmand.maps.")) {
			return PurchaseOrigin.IOS;
		}
		if (sku.contains(".huawei.")) {
			return PurchaseOrigin.HUAWEI;
		}
		if (sku.contains(".amazon.")) {
			return PurchaseOrigin.AMAZON;
		}
		if (sku.contains(".fastspring.")) {
			return PurchaseOrigin.FASTSPRING;
		}
		return PurchaseOrigin.GOOGLE;
	}

	@NonNull
	public PurchaseOrigin getPlatformOrigin() {
		return getPurchaseOriginByPlatform(getPlatform());
	}

	@NonNull
	public PurchaseOrigin getPurchaseOriginByPlatform(@NonNull String platform) {
		return switch (platform) {
			case PLATFORM_APPLE -> PurchaseOrigin.IOS;
			case PLATFORM_AMAZON -> PurchaseOrigin.AMAZON;
			case PLATFORM_HUAWEI -> PurchaseOrigin.HUAWEI;
			case PLATFORM_FASTSPRING -> PurchaseOrigin.FASTSPRING;
			default -> PurchaseOrigin.GOOGLE;
		};
	}
}
