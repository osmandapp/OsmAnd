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
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class InAppPurchaseHelper {
	// Debug tag, for logging
	protected static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(InAppPurchaseHelper.class);
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();
	private final boolean mDebugLog = true;

	protected InAppPurchases purchases;
	protected long lastValidationCheckTime;
	protected boolean inventoryRequested;
	protected Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily

	private static final String ANDROID_AUTO_START_DATE = "01.01.2022";
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

	protected boolean isDeveloperVersion;
	protected String token = "";
	protected InAppPurchaseTaskType activeTask;
	protected boolean processingTask = false;
	protected boolean inventoryRequestPending = false;

	protected OsmandApplication ctx;
	protected InAppPurchaseListener uiActivity = null;

	protected long lastPromoCheckTime;
	protected boolean promoRequested;

	public interface InAppPurchaseListener {

		void onError(InAppPurchaseTaskType taskType, String error);

		void onGetItems();

		void onItemPurchased(String sku, boolean active);

		void showProgress(InAppPurchaseTaskType taskType);

		void dismissProgress(InAppPurchaseTaskType taskType);
	}

	public interface InAppPurchaseInitCallback {

		void onSuccess();

		void onFail();
	}

	static class SubscriptionStateHolder {
		SubscriptionState state = SubscriptionState.UNDEFINED;
		long startTime = 0;
		long expireTime = 0;
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
		abstract void run(InAppPurchaseHelper helper);

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

	public static boolean isSubscribedToAny(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| isSubscribedToMaps(ctx)
				|| isOsmAndProAvailable(ctx)
				|| isSubscribedToMapperUpdates(ctx)
				|| ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
	}

	public static boolean isSubscribedToMaps(@NonNull OsmandApplication app) {
		return isSubscribedToMaps(app, true);
	}

	public static boolean isSubscribedToMaps(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return checkDevBuild && Version.isDeveloperBuild(app)
				|| app.getSettings().OSMAND_MAPS_PURCHASED.get();
	}

	public static boolean isSubscribedToLiveUpdates(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| ctx.getSettings().LIVE_UPDATES_PURCHASED.get()
				|| isSubscribedToMapperUpdates(ctx)
				|| isOsmAndProAvailable(ctx);
	}

	private static boolean isSubscribedToOsmAndPro(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| ctx.getSettings().OSMAND_PRO_PURCHASED.get();
	}

	private static boolean isSubscribedToMapperUpdates(@NonNull OsmandApplication ctx) {
		return ctx.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.get() > System.currentTimeMillis();
	}

	public static boolean isSubscribedToPromo(@NonNull OsmandApplication ctx) {
		return ctx.getSettings().BACKUP_PROMOCODE_ACTIVE.get();
	}

	public static boolean isOsmAndProAvailable(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| isSubscribedToPromo(ctx)
				|| isSubscribedToOsmAndPro(ctx);
	}

	public static boolean isAndroidAutoAvailable(@NonNull OsmandApplication ctx) {
		long time = System.currentTimeMillis();
		long calTime = getStartAndroidAutoTime();
		if (time >= calTime) {
			return Version.isDeveloperBuild(ctx) || Version.isPaidVersion(ctx);
		}
		return true;
	}

	private static long getStartAndroidAutoTime() {
		try {
			Date date = SIMPLE_DATE_FORMAT.parse(ANDROID_AUTO_START_DATE);
			if (date != null) {
				return date.getTime();
			}
		} catch (ParseException e) {
			LOG.error(e);
		}
		return 0;
	}

	public static boolean isFullVersionPurchased(@NonNull OsmandApplication app) {
		return isFullVersionPurchased(app, true);
	}

	public static boolean isFullVersionPurchased(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return checkDevBuild && Version.isDeveloperBuild(app) || app.getSettings().FULL_VERSION_PURCHASED.get();
	}

	public static boolean isDepthContoursPurchased(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| Version.isPaidVersion(ctx)
				|| ctx.getSettings().DEPTH_CONTOURS_PURCHASED.get();
	}

	public static boolean isContourLinesPurchased(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| Version.isPaidVersion(ctx)
				|| ctx.getSettings().CONTOUR_LINES_PURCHASED.get();
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

	@NonNull
	public List<InAppPurchase> getEverMadeMainPurchases() {
		List<InAppPurchase> purchases = new ArrayList<>(getEverMadeSubscriptions());
		InAppPurchase fullVersion = getFullVersion();
		if (fullVersion != null && fullVersion.isPurchased()) {
			purchases.add(fullVersion);
		}
		return purchases;
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

	public abstract void isInAppPurchaseSupported(@NonNull final Activity activity, @Nullable final InAppPurchaseInitCallback callback);

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
			if (purchases.isFullVersion(purchase) && isFullVersionPurchased(ctx)) {
				return true;
			} else if (purchases.isDepthContours(purchase) && isDepthContoursPurchased(ctx)) {
				return true;
			}
			int featureId = purchase.getFeatureId();
			for (InAppPurchase p : allPurchases) {
				if (p.hasFeatureInScope(featureId)) {
					if (p.isPurchased()) {
						return true;
					} else {
						if (purchases.isLiveUpdatesSubscription(p) && isSubscribedToLiveUpdates(ctx)) {
							return true;
						} else if (purchases.isOsmAndProSubscription(p) && isSubscribedToOsmAndPro(ctx)) {
							return true;
						} else if (purchases.isMapsSubscription(p) && isSubscribedToMaps(ctx)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected void exec(final @NonNull InAppPurchaseTaskType taskType, final @NonNull InAppCommand command) {
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

	protected abstract void execImpl(@NonNull final InAppPurchaseTaskType taskType, @NonNull final InAppCommand command);

	public boolean needRequestInventory() {
		return !inventoryRequested && ((isSubscribedToAny(ctx) && Algorithms.isEmpty(ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get()))
				|| System.currentTimeMillis() - lastValidationCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC);
	}

	public boolean needRequestPromo() {
		return !promoRequested || System.currentTimeMillis() - lastPromoCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC;
	}

	public void requestInventory(boolean userRequested) {
		notifyShowProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
		new RequestInventoryTask(userRequested).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
		new CheckPromoTask(null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public abstract void purchaseFullVersion(@NonNull final Activity activity) throws UnsupportedOperationException;

	public void purchaseSubscription(@NonNull Activity activity, String sku) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
		new SubscriptionPurchaseTask(activity, sku).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public abstract void purchaseDepthContours(@NonNull final Activity activity) throws UnsupportedOperationException;

	public abstract void purchaseContourLines(@NonNull final Activity activity) throws UnsupportedOperationException;

	public abstract void manageSubscription(@NonNull Context ctx, @Nullable String sku);

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
					return AndroidNetworkUtils.sendRequest(ctx,
							"https://osmand.net/subscription/register",
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
				final String userInfo = userId + " " + token;
				exec(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, getPurchaseSubscriptionCommand(activity, sku, userInfo));
			} else {
				notifyError(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION, "Empty userId");
				stop(true);
			}
		}
	}

	protected abstract InAppCommand getPurchaseSubscriptionCommand(final WeakReference<Activity> activity,
																   final String sku, final String userInfo) throws UnsupportedOperationException;

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
			try {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("androidPackage", ctx.getPackageName());
				addUserInfo(parameters);
				activeSubscriptionsIds = AndroidNetworkUtils.sendRequest(ctx,
						"https://osmand.net/api/subscriptions/active",
						parameters, "Requesting active subscriptions...", false, false);

				String userId = ctx.getSettings().BILLING_USER_ID.get();
				String userToken = ctx.getSettings().BILLING_USER_TOKEN.get();
				if (!Algorithms.isEmpty(userId) && !Algorithms.isEmpty(userToken)) {
					parameters.put("userId", userId);
					parameters.put("userToken", userToken);
					subscriptionsState = AndroidNetworkUtils.sendRequest(ctx,
							"https://osmand.net/api/subscriptions/get",
							parameters, "Requesting subscriptions state...", false, false);
				}
			} catch (Exception e) {
				logError("sendRequest Error", e);
			}
			return new String[] {activeSubscriptionsIds, subscriptionsState};
		}

		@Override
		protected void onPostExecute(String[] response) {
			logDebug("Response=" + Arrays.toString(response));
			String activeSubscriptionsIdsJson = response[0];
			String subscriptionsStateJson = response[1];
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
			exec(InAppPurchaseTaskType.REQUEST_INVENTORY, getRequestInventoryCommand(userRequested));
		}
	}

	@NonNull
	public Map<String, SubscriptionStateHolder> parseSubscriptionStates(@NonNull String subscriptionsStateJson) {
		Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();
		try {
			JSONArray subArrJson = new JSONArray(subscriptionsStateJson);
			for (int i = 0; i < subArrJson.length(); i++) {
				JSONObject subObj = subArrJson.getJSONObject(i);
				String sku = subObj.getString("sku");
				String state = subObj.getString("state");

				if (!Algorithms.isEmpty(sku) && !Algorithms.isEmpty(state)) {
					SubscriptionStateHolder stateHolder = new SubscriptionStateHolder();
					stateHolder.state = SubscriptionState.getByStateStr(state);
					stateHolder.startTime = subObj.optLong("start_time");
					stateHolder.expireTime = subObj.optLong("expire_time");
					subscriptionStateMap.put(sku, stateHolder);
				}
			}
		} catch (JSONException e) {
			logError("Json parsing error", e);
		}
		return subscriptionStateMap;
	}

	public void checkPromoAsync(@Nullable CallbackWithObject<Boolean> listener) {
		CheckPromoTask task = new CheckPromoTask(listener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	@SuppressLint("StaticFieldLeak")
	private class CheckPromoTask extends AsyncTask<Void, Void, Boolean> {

		private final CallbackWithObject<Boolean> listener;

		public CheckPromoTask(@Nullable CallbackWithObject<Boolean> listener) {
			this.listener = listener;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			boolean promoActive = false;
			try {
				String promocode = ctx.getSettings().BACKUP_PROMOCODE.get();
				if (!Algorithms.isEmpty(promocode)) {
					promoActive = checkPromoSubscription(promocode);
				}
				if (!promoActive) {
					String orderId = getOrderIdByDeviceIdAndToken();
					if (!Algorithms.isEmpty(orderId)) {
						promoActive = checkPromoSubscription(orderId);
					}
				}
			} catch (Exception e) {
				logError("checkPromoAsync Error", e);
			}
			return promoActive;
		}

		private boolean checkPromoSubscription(@NonNull String orderId) {
			Entry<String, SubscriptionStateHolder> entry = getSubscriptionStateByOrderId(orderId);
			if (entry != null) {
				SubscriptionStateHolder stateHolder = entry.getValue();
				if ("promo_website".equals(entry.getKey())) {
					ctx.getSettings().BACKUP_PROMOCODE_STATE.set(stateHolder.state);
					ctx.getSettings().BACKUP_PROMOCODE_START_TIME.set(stateHolder.startTime);
					ctx.getSettings().BACKUP_PROMOCODE_EXPIRE_TIME.set(stateHolder.expireTime);
					return stateHolder.state.isActive();
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean active) {
			promoRequested = true;
			lastPromoCheckTime = System.currentTimeMillis();
			ctx.getSettings().BACKUP_PROMOCODE_ACTIVE.set(active);

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
		Entry<String, SubscriptionStateHolder> entry = getSubscriptionStateByOrderId(orderId);
		if (entry != null) {
			SubscriptionStateHolder stateHolder = entry.getValue();
			return stateHolder.state.isActive();
		}
		return false;
	}

	private String getOrderIdByDeviceIdAndToken() {
		String[] orderId = new String[1];
		String deviceId = ctx.getSettings().BACKUP_DEVICE_ID.get();
		String accessToken = ctx.getSettings().BACKUP_ACCESS_TOKEN.get();
		if (!Algorithms.isEmpty(deviceId) && !Algorithms.isEmpty(accessToken)) {
			Map<String, String> params = new HashMap<>();
			params.put("deviceid", deviceId);
			params.put("accessToken", accessToken);
			AndroidNetworkUtils.sendRequest(ctx, "https://osmand.net/userdata/user-validate-sub",
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

	private Entry<String, SubscriptionStateHolder> getSubscriptionStateByOrderId(@NonNull String orderId) {
		Map<String, String> params = new HashMap<>();
		params.put("orderId", orderId);
		String subscriptionsState = AndroidNetworkUtils.sendRequest(ctx, "https://osmand.net/api/subscriptions/get",
				params, "Requesting promo subscription state", false, false);

		if (subscriptionsState != null) {
			Set<Entry<String, SubscriptionStateHolder>> stateHolders = parseSubscriptionStates(subscriptionsState).entrySet();
			if (!Algorithms.isEmpty(stateHolders)) {
				return stateHolders.iterator().next();
			}
		}
		return null;
	}

	protected abstract InAppCommand getRequestInventoryCommand(boolean userRequested) throws UnsupportedOperationException;

	protected void onSkuDetailsResponseDone(@NonNull List<PurchaseInfo> purchaseInfoList, boolean userRequested) {
		final OnRequestResultListener listener = new OnRequestResultListener() {
			@Override
			public void onResult(@Nullable String result, @Nullable String error, @Nullable Integer resultCode) {
				notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
				notifyGetItems();
				stop(true);
				logDebug("Initial inapp query finished");
				if (userRequested) {
					showToast(ctx.getString(R.string.purchases_restored));
				}
			}
		};

		if (purchaseInfoList.size() > 0) {
			sendTokens(purchaseInfoList, listener);
		} else {
			listener.onResult("OK", null, null);
		}
	}

	@SuppressLint("HardwareIds")
	protected void addUserInfo(Map<String, String> parameters) {
		parameters.put("version", Version.getFullVersion(ctx));
		parameters.put("lang", ctx.getLanguage() + "");
		parameters.put("nd", ctx.getAppInitializer().getFirstInstalledDays() + "");
		parameters.put("ns", ctx.getAppInitializer().getNumberOfStarts() + "");
		parameters.put("aid", ctx.getUserAndroidId());
	}

	protected void onPurchaseDone(PurchaseInfo info) {
		logDebug("Purchase successful.");

		InAppSubscription subscription = getSubscriptions().getSubscriptionBySku(info.getSku());
		InAppPurchase fullVersion = getFullVersion();
		InAppPurchase depthContours = getDepthContours();
		InAppPurchase contourLines = getContourLines();
		if (subscription != null) {
			final boolean maps = purchases.isMapsSubscription(subscription);
			final boolean liveUpdates = purchases.isLiveUpdatesSubscription(subscription);
			final boolean pro = purchases.isOsmAndProSubscription(subscription);
			// bought live updates
			if (maps) {
				logDebug("Maps subscription purchased.");
			} else if (liveUpdates) {
				logDebug("Live updates subscription purchased.");
			} else if (pro) {
				logDebug("OsmAnd Pro subscription purchased.");
			}
			final String sku = subscription.getSku();
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

		} else if (fullVersion != null && info.getSku().equals(fullVersion.getSku())) {
			// bought full version
			fullVersion.setPurchaseState(PurchaseState.PURCHASED);
			fullVersion.setPurchaseInfo(ctx, info);
			logDebug("Full version purchased.");
			showToast(ctx.getString(R.string.full_version_thanks));
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
			notifyItemPurchased(fullVersion.getSku(), false);
			refreshAndroidAuto();
			stop(true);

		} else if (depthContours != null && info.getSku().equals(depthContours.getSku())) {
			// bought sea depth contours
			depthContours.setPurchaseState(PurchaseState.PURCHASED);
			depthContours.setPurchaseInfo(ctx, info);
			logDebug("Sea depth contours purchased.");
			showToast(ctx.getString(R.string.sea_depth_thanks));
			ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
			notifyItemPurchased(depthContours.getSku(), false);
			stop(true);

		} else if (contourLines != null && info.getSku().equals(contourLines.getSku())) {
			// bought contour lines
			contourLines.setPurchaseState(PurchaseState.PURCHASED);
			contourLines.setPurchaseInfo(ctx, info);
			logDebug("Contours lines purchased.");
			showToast(ctx.getString(R.string.contour_lines_thanks));
			ctx.getSettings().CONTOUR_LINES_PURCHASED.set(true);

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
		}
	}

	protected void sendTokens(@NonNull final List<PurchaseInfo> purchaseInfoList, final OnRequestResultListener listener) {
		final String userId = ctx.getSettings().BILLING_USER_ID.get();
		final String token = ctx.getSettings().BILLING_USER_TOKEN.get();
		final String email = ctx.getSettings().BILLING_USER_EMAIL.get();
		try {
			String url = "https://osmand.net/subscription/purchased";
			String userOperation = "Sending purchase info...";
			final List<Request> requests = new ArrayList<>();
			for (PurchaseInfo info : purchaseInfoList) {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("userid", userId);
				parameters.put("sku", info.getSku());
				parameters.put("orderId", info.getOrderId());
				parameters.put("purchaseToken", info.getPurchaseToken());
				parameters.put("email", email);
				parameters.put("token", token);
				addUserInfo(parameters);
				requests.add(new Request(url, parameters, userOperation, true, true));
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
												+ " (response=" + result + " google=" + info.toString() + ")");
									}
								} catch (JSONException e) {
									logError("SendToken", e);
									complain("SendToken Error: "
											+ (e.getMessage() != null ? e.getMessage() : "JSONException")
											+ " (response=" + result + " google=" + info.toString() + ")");
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
					tokensSent.add(info.getSku());
					ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.set(TextUtils.join(";", tokensSent));
				}

				@Nullable
				private PurchaseInfo getPurchaseInfo(String sku) {
					for (PurchaseInfo info : purchaseInfoList) {
						if (info.getSku().equals(sku)) {
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
		if (uiActivity != null) {
			uiActivity.onError(taskType, message);
		}
	}

	protected void notifyGetItems() {
		if (uiActivity != null) {
			uiActivity.onGetItems();
		}
	}

	protected void notifyItemPurchased(String sku, boolean active) {
		if (uiActivity != null) {
			uiActivity.onItemPurchased(sku, active);
		}
	}

	protected void notifyShowProgress(InAppPurchaseTaskType taskType) {
		if (uiActivity != null) {
			uiActivity.showProgress(taskType);
		}
	}

	protected void notifyDismissProgress(InAppPurchaseTaskType taskType) {
		if (uiActivity != null) {
			uiActivity.dismissProgress(taskType);
		}
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

	protected void showToast(final String message) {
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
}
