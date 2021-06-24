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

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidNetworkUtils.OnSendRequestsListener;
import net.osmand.AndroidNetworkUtils.Request;
import net.osmand.AndroidNetworkUtils.RequestResponse;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.inapp.InAppPurchases.PurchaseInfo;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class InAppPurchaseHelper {
	// Debug tag, for logging
	protected static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(InAppPurchaseHelper.class);
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();
	private boolean mDebugLog = false;

	protected InAppPurchases purchases;
	protected long lastValidationCheckTime;
	protected boolean inventoryRequested;
	protected Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily

	protected boolean isDeveloperVersion;
	protected String token = "";
	protected InAppPurchaseTaskType activeTask;
	protected boolean processingTask = false;
	protected boolean inventoryRequestPending = false;

	protected OsmandApplication ctx;
	protected InAppPurchaseListener uiActivity = null;

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
		long expireTime = 0;
	}

	public enum InAppPurchaseTaskType {
		REQUEST_INVENTORY,
		PURCHASE_FULL_VERSION,
		PURCHASE_SUBSCRIPTION,
		PURCHASE_DEPTH_CONTOURS,
		PURCHASE_CONTOUR_LINES
	}

	public abstract class InAppCommand {

		InAppCommandResultHandler resultHandler;

		// return true if done and false if async task started
		abstract void run(InAppPurchaseHelper helper);

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
				|| ctx.getSettings().OSMAND_MAPS_PURCHASED.get()
				|| ctx.getSettings().LIVE_UPDATES_PURCHASED.get()
				|| ctx.getSettings().OSMAND_PRO_PURCHASED.get();
	}

	public static boolean isSubscribedToMaps(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| ctx.getSettings().OSMAND_MAPS_PURCHASED.get();
	}

	public static boolean isSubscribedToLiveUpdates(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| ctx.getSettings().LIVE_UPDATES_PURCHASED.get()
				|| ctx.getSettings().OSMAND_PRO_PURCHASED.get();
	}

	public static boolean isSubscribedToOsmAndPro(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx)
				|| ctx.getSettings().OSMAND_PRO_PURCHASED.get();
	}

	public static boolean isFullVersionPurchased(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx) || ctx.getSettings().FULL_VERSION_PURCHASED.get();
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

	public InAppPurchase getFullVersion() {
		return purchases.getFullVersion();
	}

	public InAppPurchase getDepthContours() {
		return purchases.getDepthContours();
	}

	public InAppPurchase getContourLines() {
		return purchases.getContourLines();
	}

	public InAppSubscription getMonthlyLiveUpdates() {
		return purchases.getMonthlyLiveUpdates();
	}

	@Nullable
	public InAppSubscription getPurchasedMonthlyLiveUpdates() {
		return purchases.getPurchasedMonthlyLiveUpdates();
	}

	@Nullable
	public InAppSubscription getAnyPurchasedSubscription() {
		return purchases.getAnyPurchasedSubscription();
	}

	public InAppPurchaseHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		isDeveloperVersion = Version.isDeveloperVersion(ctx);
	}

	@NonNull
	public List<InAppSubscription> getEverMadeSubscriptions() {
		List<InAppSubscription> subscriptions = new ArrayList<>();
		for (InAppSubscription subscription : getSubscriptions().getVisibleSubscriptions()) {
			if (subscription.isPurchased() ||  subscription.getState() != SubscriptionState.UNDEFINED) {
				subscriptions.add(subscription);
			}
		}
		return subscriptions;
	}

	public static void subscribe(@NonNull Activity activity, @NonNull InAppPurchaseHelper purchaseHelper, @NonNull String sku) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		OsmandSettings settings = app.getSettings();
		if (settings.isInternetConnectionAvailable(true)) {
			purchaseHelper.purchaseSubscription(activity, sku,
					settings.BILLING_USER_EMAIL.get(),
					settings.BILLING_USER_NAME.get(),
					settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get(),
					settings.BILLING_HIDE_USER_NAME.get());
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
		if (isDeveloperVersion || (!Version.isGooglePlayEnabled() && !Version.isHuawei())) {
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

	public void requestInventory() {
		notifyShowProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
		new RequestInventoryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public abstract void purchaseFullVersion(@NonNull final Activity activity) throws UnsupportedOperationException;

	public void purchaseSubscription(@NonNull Activity activity, String sku, String email, String userName,
									 String countryDownloadName, boolean hideUserName) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_SUBSCRIPTION);
		new SubscriptionPurchaseTask(activity, sku, email, userName, countryDownloadName, hideUserName)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public abstract void purchaseDepthContours(@NonNull final Activity activity) throws UnsupportedOperationException;

	public abstract void purchaseContourLines(@NonNull final Activity activity) throws UnsupportedOperationException;

	public abstract void manageSubscription(@NonNull Context ctx, @Nullable String sku);

	@SuppressLint("StaticFieldLeak")
	private class SubscriptionPurchaseTask extends AsyncTask<Void, Void, String> {

		private final WeakReference<Activity> activity;

		private final String sku;
		private final String email;
		private final String userName;
		private final String countryDownloadName;
		private final boolean hideUserName;

		private String userId;

		SubscriptionPurchaseTask(Activity activity, String sku, String email, String userName,
								 String countryDownloadName, boolean hideUserName) {
			this.activity = new WeakReference<>(activity);

			this.sku = sku;
			this.email = email;
			this.userName = userName;
			this.countryDownloadName = countryDownloadName;
			this.hideUserName = hideUserName;
		}

		@Override
		protected String doInBackground(Void... params) {
			userId = ctx.getSettings().BILLING_USER_ID.get();
			if (Algorithms.isEmpty(userId) || Algorithms.isEmpty(token)) {
				try {
					Map<String, String> parameters = new HashMap<>();
					parameters.put("visibleName", hideUserName ? "" : userName);
					parameters.put("preferredCountry", countryDownloadName);
					parameters.put("email", email);
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

		RequestInventoryTask() {
		}

		@Override
		protected String[] doInBackground(Void... params) {
			try {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("androidPackage", ctx.getPackageName());
				addUserInfo(parameters);
				String activeSubscriptionsIds = AndroidNetworkUtils.sendRequest(ctx,
						"https://osmand.net/api/subscriptions/active",
						parameters, "Requesting active subscriptions...", false, false);

				String subscriptionsState = null;
				String userId = ctx.getSettings().BILLING_USER_ID.get();
				String userToken = ctx.getSettings().BILLING_USER_TOKEN.get();
				if (!Algorithms.isEmpty(userId) && !Algorithms.isEmpty(userToken)) {
					parameters.put("userId", userId);
					parameters.put("userToken", userToken);
					subscriptionsState = AndroidNetworkUtils.sendRequest(ctx,
							"https://osmand.net/api/subscriptions/get",
							parameters, "Requesting subscriptions state...", false, false);
				}

				return new String[] { activeSubscriptionsIds, subscriptionsState };
			} catch (Exception e) {
				logError("sendRequest Error", e);
			}
			return null;
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
				Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();
				try {
					JSONArray subArrJson = new JSONArray(subscriptionsStateJson);
					for (int i = 0; i < subArrJson.length(); i++) {
						JSONObject subObj = subArrJson.getJSONObject(i);
						String sku = subObj.getString("sku");
						String state = subObj.getString("state");
						long expireTime = 0;
						if (subObj.has("expire_time")) {
							expireTime = subObj.getLong("expire_time");
						}
						if (!Algorithms.isEmpty(sku) && !Algorithms.isEmpty(state)) {
							SubscriptionStateHolder stateHolder = new SubscriptionStateHolder();
							stateHolder.state = SubscriptionState.getByStateStr(state);
							stateHolder.expireTime = expireTime;
							subscriptionStateMap.put(sku, stateHolder);
						}
					}
				} catch (JSONException e) {
					logError("Json parsing error", e);
				}
				InAppPurchaseHelper.this.subscriptionStateMap = subscriptionStateMap;
			}
			exec(InAppPurchaseTaskType.REQUEST_INVENTORY, getRequestInventoryCommand());
		}
	}

	protected abstract InAppCommand getRequestInventoryCommand() throws UnsupportedOperationException;

	protected void onSkuDetailsResponseDone(List<PurchaseInfo> purchaseInfoList) {
		final OnRequestResultListener listener = new OnRequestResultListener() {
			@Override
			public void onResult(@Nullable String result, @Nullable String error) {
				notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
				notifyGetItems();
				stop(true);
				logDebug("Initial inapp query finished");
			}
		};

		if (purchaseInfoList.size() > 0) {
			sendTokens(purchaseInfoList, listener);
		} else {
			listener.onResult("OK", null);
		}
	}

	@SuppressLint("HardwareIds")
	private void addUserInfo(Map<String, String> parameters) {
		parameters.put("version", Version.getFullVersion(ctx));
		parameters.put("lang", ctx.getLanguage() + "");
		parameters.put("nd", ctx.getAppInitializer().getFirstInstalledDays() + "");
		parameters.put("ns", ctx.getAppInitializer().getNumberOfStarts() + "");
		parameters.put("aid", ctx.getUserAndroidId());
	}

	protected void onPurchaseDone(PurchaseInfo info) {
		logDebug("Purchase successful.");

		InAppSubscription subscription = getSubscriptions().getSubscriptionBySku(info.getSku());
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
			sendTokens(Collections.singletonList(info), new OnRequestResultListener() {
				@Override
				public void onResult(@Nullable String result, @Nullable String error) {
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
					stop(true);
				}
			});

		} else if (info.getSku().equals(getFullVersion().getSku())) {
			// bought full version
			getFullVersion().setPurchaseState(PurchaseState.PURCHASED);
			getFullVersion().setPurchaseInfo(ctx, info);
			logDebug("Full version purchased.");
			showToast(ctx.getString(R.string.full_version_thanks));
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
			notifyItemPurchased(getFullVersion().getSku(), false);
			stop(true);

		} else if (info.getSku().equals(getDepthContours().getSku())) {
			// bought sea depth contours
			getDepthContours().setPurchaseState(PurchaseState.PURCHASED);
			getDepthContours().setPurchaseInfo(ctx, info);
			logDebug("Sea depth contours purchased.");
			showToast(ctx.getString(R.string.sea_depth_thanks));
			ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
			notifyItemPurchased(getDepthContours().getSku(), false);
			stop(true);

		} else if (info.getSku().equals(getContourLines().getSku())) {
			// bought contour lines
			getContourLines().setPurchaseState(PurchaseState.PURCHASED);
			getContourLines().setPurchaseInfo(ctx, info);
			logDebug("Contours lines purchased.");
			showToast(ctx.getString(R.string.contour_lines_thanks));
			ctx.getSettings().CONTOUR_LINES_PURCHASED.set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_CONTOUR_LINES);
			notifyItemPurchased(getContourLines().getSku(), false);
			stop(true);

		} else {
			notifyDismissProgress(activeTask);
			stop(true);
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
			requestInventory();
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
									if (!obj.has("error")) {
										processPurchasedJson(obj);
									} else {
										complain("SendToken Error: "
												+ obj.getString("error")
												+ " (userId=" + userId + " token=" + token + " response=" + result + " google=" + info.toString() + ")");
									}
								} catch (JSONException e) {
									logError("SendToken", e);
									complain("SendToken Error: "
											+ (e.getMessage() != null ? e.getMessage() : "JSONException")
											+ " (userId=" + userId + " token=" + token + " response=" + result + " google=" + info.toString() + ")");
								}
							}
						}
					}
					if (listener != null) {
						listener.onResult("OK", null);
					}
				}

				private void updateSentTokens(@NonNull PurchaseInfo info) {
					String tokensSentStr = ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get();
					Set<String> tokensSent = new HashSet<>(Arrays.asList(tokensSentStr.split(";")));
					tokensSent.add(info.getSku());
					ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.set(TextUtils.join(";", tokensSent));
				}

				private void processPurchasedJson(JSONObject obj) throws JSONException {
					if (obj.has("visibleName") && !Algorithms.isEmpty(obj.getString("visibleName"))) {
						ctx.getSettings().BILLING_USER_NAME.set(obj.getString("visibleName"));
						ctx.getSettings().BILLING_HIDE_USER_NAME.set(false);
					} else {
						ctx.getSettings().BILLING_HIDE_USER_NAME.set(true);
					}
					if (obj.has("preferredCountry")) {
						String prefferedCountry = obj.getString("preferredCountry");
						if (!ctx.getSettings().BILLING_USER_COUNTRY_DOWNLOAD_NAME.get().equals(prefferedCountry)) {
							ctx.getSettings().BILLING_USER_COUNTRY_DOWNLOAD_NAME.set(prefferedCountry);
							CountrySelectionFragment countrySelectionFragment = new CountrySelectionFragment();
							countrySelectionFragment.initCountries(ctx);
							CountryItem countryItem = null;
							if (Algorithms.isEmpty(prefferedCountry)) {
								countryItem = countrySelectionFragment.getCountryItems().get(0);
							} else if (!prefferedCountry.equals(OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER)) {
								countryItem = countrySelectionFragment.getCountryItem(prefferedCountry);
							}
							if (countryItem != null) {
								ctx.getSettings().BILLING_USER_COUNTRY.set(countryItem.getLocalName());
							}
						}
					}
					if (obj.has("email")) {
						ctx.getSettings().BILLING_USER_EMAIL.set(obj.getString("email"));
					}
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
				listener.onResult("Error", null);
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
