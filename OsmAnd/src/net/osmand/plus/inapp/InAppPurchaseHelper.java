package net.osmand.plus.inapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchaseLiveUpdatesOldSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.inapp.util.IabHelper;
import net.osmand.plus.inapp.util.IabHelper.OnIabPurchaseFinishedListener;
import net.osmand.plus.inapp.util.IabHelper.QueryInventoryFinishedListener;
import net.osmand.plus.inapp.util.IabResult;
import net.osmand.plus.inapp.util.Inventory;
import net.osmand.plus.inapp.util.Purchase;
import net.osmand.plus.inapp.util.SkuDetails;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
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

import static net.osmand.plus.inapp.util.IabHelper.IABHELPER_USER_CANCELLED;
import static net.osmand.plus.inapp.util.IabHelper.ITEM_TYPE_SUBS;

public class InAppPurchaseHelper {
	// Debug tag, for logging
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();
	private boolean mDebugLog = true;

	public static final long SUBSCRIPTION_HOLDING_TIME_MSEC = 1000 * 60 * 60 * 24 * 3; // 3 days

	private InAppPurchases purchases;
	private long lastValidationCheckTime;

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily
	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	// The helper object
	private IabHelper mHelper;
	private boolean isDeveloperVersion;
	private String token = "";
	private InAppPurchaseTaskType activeTask;
	private boolean processingTask = false;
	private boolean inventoryRequestPending = false;

	private OsmandApplication ctx;
	private InAppPurchaseListener uiActivity = null;

	/* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
	 * (that you got from the Google Play developer console). This is not your
	 * developer public key, it's the *app-specific* public key.
	 *
	 * Instead of just storing the entire literal string here embedded in the
	 * program,  construct the key at runtime from pieces or
	 * use bit manipulation (for example, XOR with some other string) to hide
	 * the actual key.  The key itself is not secret information, but we don't
	 * want to make it easy for an attacker to replace the public key with one
	 * of their own and then fake messages from the server.
	 */
	private static final String BASE64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgk8cEx" +
			"UO4mfEwWFLkQnX1Tkzehr4SnXLXcm2Osxs5FTJPEgyTckTh0POKVMrxeGLn0KoTY2NTgp1U/inp" +
			"wccWisPhVPEmw9bAVvWsOkzlyg1kv03fJdnAXRBSqDDPV6X8Z3MtkPVqZkupBsxyIllEILKHK06" +
			"OCw49JLTsMR3oTRifGzma79I71X0spw0fM+cIRlkS2tsXN8GPbdkJwHofZKPOXS51pgC1zU8uWX" +
			"I+ftJO46a1XkNh1dO2anUiQ8P/H4yOTqnMsXF7biyYuiwjXPOcy0OMhEHi54Dq6Mr3u5ZALOAkc" +
			"YTjh1H/ZgqIHy5ZluahINuDE76qdLYMXrDMQIDAQAB";

	public interface InAppPurchaseListener {
		void onError(InAppPurchaseTaskType taskType, String error);

		void onGetItems();

		void onItemPurchased(String sku, boolean active);

		void showProgress(InAppPurchaseTaskType taskType);

		void dismissProgress(InAppPurchaseTaskType taskType);
	}

	public enum InAppPurchaseTaskType {
		REQUEST_INVENTORY,
		PURCHASE_FULL_VERSION,
		PURCHASE_LIVE_UPDATES,
		PURCHASE_DEPTH_CONTOURS
	}

	public interface InAppRunnable {
		// return true if done and false if async task started
		boolean run(InAppPurchaseHelper helper);
	}

	public String getToken() {
		return token;
	}

	public InAppPurchaseTaskType getActiveTask() {
		return activeTask;
	}

	public static boolean isSubscribedToLiveUpdates(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx) || ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
	}

	public static boolean isFullVersionPurchased(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx) || ctx.getSettings().FULL_VERSION_PURCHASED.get();
	}

	public static boolean isDepthContoursPurchased(@NonNull OsmandApplication ctx) {
		return Version.isDeveloperBuild(ctx) || ctx.getSettings().DEPTH_CONTOURS_PURCHASED.get();
	}

	public InAppPurchases getInAppPurchases() {
		return purchases;
	}

	public InAppSubscriptionList getLiveUpdates() {
		return purchases.getLiveUpdates();
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

	public InAppPurchaseHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		isDeveloperVersion = Version.isDeveloperVersion(ctx);
		purchases = new InAppPurchases(ctx);
	}

	public boolean hasInventory() {
		return lastValidationCheckTime != 0;
	}

	public boolean isPurchased(String inAppSku) {
		if (purchases.isFullVersion(inAppSku)) {
			return isFullVersionPurchased(ctx);
		} else if (purchases.isLiveUpdates(inAppSku)) {
			return isSubscribedToLiveUpdates(ctx);
		} else if (purchases.isDepthContours(inAppSku)) {
			return isDepthContoursPurchased(ctx);
		}
		return false;
	}

	private void exec(final @NonNull InAppPurchaseTaskType taskType, final @NonNull InAppRunnable runnable) {
		if (isDeveloperVersion || !Version.isGooglePlayEnabled(ctx)) {
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
		mHelper = new IabHelper(ctx, BASE64_ENCODED_PUBLIC_KEY);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(false);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		logDebug("Starting setup.");
		try {
			processingTask = true;
			activeTask = taskType;
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					logDebug("Setup finished.");

					if (!result.isSuccess()) {
						// Oh noes, there was a problem.
						//complain("Problem setting up in-app billing: " + result);
						notifyError(taskType, result.getMessage());
						stop(true);
						return;
					}

					// Have we been disposed of in the meantime? If so, quit.
					if (mHelper == null) {
						stop(true);
						return;
					}

					processingTask = !runnable.run(InAppPurchaseHelper.this);
				}
			});
		} catch (Exception e) {
			logError("exec Error", e);
			stop(true);
		}
	}

	public boolean needRequestInventory() {
		return (isSubscribedToLiveUpdates(ctx) && Algorithms.isEmpty(ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get()))
				|| System.currentTimeMillis() - lastValidationCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC;
	}

	public void requestInventory() {
		notifyShowProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
		new RequestInventoryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public void purchaseFullVersion(final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
		exec(InAppPurchaseTaskType.PURCHASE_FULL_VERSION, new InAppRunnable() {
			@Override
			public boolean run(InAppPurchaseHelper helper) {
				try {
					mHelper.launchPurchaseFlow(activity,
							getFullVersion().getSku(), RC_REQUEST, mPurchaseFinishedListener);
					return false;
				} catch (Exception e) {
					complain("Cannot launch full version purchase!");
					logError("purchaseFullVersion Error", e);
					stop(true);
				}
				return true;
			}
		});
	}

	public void purchaseLiveUpdates(Activity activity, String sku, String email, String userName,
									String countryDownloadName, boolean hideUserName) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
		new LiveUpdatesPurchaseTask(activity, sku, email, userName, countryDownloadName, hideUserName)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public void purchaseDepthContours(final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
		exec(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS, new InAppRunnable() {
			@Override
			public boolean run(InAppPurchaseHelper helper) {
				try {
					mHelper.launchPurchaseFlow(activity,
							getDepthContours().getSku(), RC_REQUEST, mPurchaseFinishedListener);
					return false;
				} catch (Exception e) {
					complain("Cannot launch depth contours purchase!");
					logError("purchaseDepthContours Error", e);
					stop(true);
				}
				return true;
			}
		});
	}

	// Listener that's called when we finish querying the items and subscriptions we own
	private QueryInventoryFinishedListener mGotInventoryListener = new QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			logDebug("Query inventory finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null) {
				stop(true);
				return;
			}

			// Is it a failure?
			if (result.isFailure()) {
				logError("Failed to query inventory: " + result);
				notifyError(InAppPurchaseTaskType.REQUEST_INVENTORY, result.getMessage());
				stop(true);
				return;
			}

			logDebug("Query inventory was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! See
			 * verifyDeveloperPayload().
			 */

			List<String> allOwnedSubscriptionSkus = inventory.getAllOwnedSkus(ITEM_TYPE_SUBS);
			for (InAppPurchase p : getLiveUpdates().getAllSubscriptions()) {
				if (inventory.hasDetails(p.getSku())) {
					Purchase purchase = inventory.getPurchase(p.getSku());
					SkuDetails liveUpdatesDetails = inventory.getSkuDetails(p.getSku());
					fetchInAppPurchase(p, liveUpdatesDetails, purchase);
					allOwnedSubscriptionSkus.remove(p.getSku());
				}
			}
			for (String sku : allOwnedSubscriptionSkus) {
				Purchase purchase = inventory.getPurchase(sku);
				SkuDetails liveUpdatesDetails = inventory.getSkuDetails(sku);
				InAppSubscription s = getLiveUpdates().upgradeSubscription(sku);
				if (s == null) {
					s = new InAppPurchaseLiveUpdatesOldSubscription(liveUpdatesDetails);
				}
				fetchInAppPurchase(s, liveUpdatesDetails, purchase);
			}

			InAppPurchase fullVersion = getFullVersion();
			if (inventory.hasDetails(fullVersion.getSku())) {
				Purchase purchase = inventory.getPurchase(fullVersion.getSku());
				SkuDetails fullPriceDetails = inventory.getSkuDetails(fullVersion.getSku());
				fetchInAppPurchase(fullVersion, fullPriceDetails, purchase);
			}

			InAppPurchase depthContours = getDepthContours();
			if (inventory.hasDetails(depthContours.getSku())) {
				Purchase purchase = inventory.getPurchase(depthContours.getSku());
				SkuDetails depthContoursDetails = inventory.getSkuDetails(depthContours.getSku());
				fetchInAppPurchase(depthContours, depthContoursDetails, purchase);
			}

			InAppPurchase contourLines = getContourLines();
			if (inventory.hasDetails(contourLines.getSku())) {
				Purchase purchase = inventory.getPurchase(contourLines.getSku());
				SkuDetails contourLinesDetails = inventory.getSkuDetails(contourLines.getSku());
				fetchInAppPurchase(contourLines, contourLinesDetails, purchase);
			}

			Purchase fullVersionPurchase = inventory.getPurchase(fullVersion.getSku());
			boolean fullVersionPurchased = (fullVersionPurchase != null && fullVersionPurchase.getPurchaseState() == 0);
			if (fullVersionPurchased) {
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
			}

			Purchase depthContoursPurchase = inventory.getPurchase(depthContours.getSku());
			boolean depthContoursPurchased = (depthContoursPurchase != null && depthContoursPurchase.getPurchaseState() == 0);
			if (depthContoursPurchased) {
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			}

			// Do we have the live updates?
			boolean subscribedToLiveUpdates = false;
			List<Purchase> liveUpdatesPurchases = new ArrayList<>();
			for (InAppPurchase p : getLiveUpdates().getAllSubscriptions()) {
				Purchase purchase = inventory.getPurchase(p.getSku());
				if (purchase != null) {
					liveUpdatesPurchases.add(purchase);
					if (!subscribedToLiveUpdates && purchase.getPurchaseState() == 0) {
						subscribedToLiveUpdates = true;
					}
				}
			}
			OsmandPreference<Long> subscriptionCancelledTime = ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_TIME;
			if (!subscribedToLiveUpdates && ctx.getSettings().LIVE_UPDATES_PURCHASED.get()) {
				if (subscriptionCancelledTime.get() == 0) {
					subscriptionCancelledTime.set(System.currentTimeMillis());
					ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_FIRST_DLG_SHOWN.set(false);
					ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_SECOND_DLG_SHOWN.set(false);
				} else if (System.currentTimeMillis() - subscriptionCancelledTime.get() > SUBSCRIPTION_HOLDING_TIME_MSEC) {
					ctx.getSettings().LIVE_UPDATES_PURCHASED.set(false);
					if (!isDepthContoursPurchased(ctx)) {
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(false);
					}
				}
			} else if (subscribedToLiveUpdates) {
				subscriptionCancelledTime.set(0L);
				ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
			}

			lastValidationCheckTime = System.currentTimeMillis();
			logDebug("User " + (subscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE")
					+ " live updates purchased.");

			OsmandSettings settings = ctx.getSettings();
			settings.INAPPS_READ.set(true);

			List<Purchase> tokensToSend = new ArrayList<>();
			if (liveUpdatesPurchases.size() > 0) {
				List<String> tokensSent = Arrays.asList(settings.BILLING_PURCHASE_TOKENS_SENT.get().split(";"));
				for (Purchase purchase : liveUpdatesPurchases) {
					if ((Algorithms.isEmpty(settings.BILLING_USER_ID.get()) || Algorithms.isEmpty(settings.BILLING_USER_TOKEN.get()))
							&& !Algorithms.isEmpty(purchase.getDeveloperPayload())) {
						String payload = purchase.getDeveloperPayload();
						if (!Algorithms.isEmpty(payload)) {
							String[] arr = payload.split(" ");
							if (arr.length > 0) {
								settings.BILLING_USER_ID.set(arr[0]);
							}
							if (arr.length > 1) {
								token = arr[1];
								settings.BILLING_USER_TOKEN.set(token);
							}
						}
					}
					if (!tokensSent.contains(purchase.getSku())) {
						tokensToSend.add(purchase);
					}
				}
			}

			final OnRequestResultListener listener = new OnRequestResultListener() {
				@Override
				public void onResult(String result) {
					notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
					notifyGetItems();
					stop(true);
					logDebug("Initial inapp query finished");
				}
			};

			if (tokensToSend.size() > 0) {
				sendTokens(tokensToSend, listener);
			} else {
				listener.onResult("OK");
			}
		}
	};

	private void fetchInAppPurchase(@NonNull InAppPurchase inAppPurchase, @NonNull SkuDetails skuDetails, @Nullable Purchase purchase) {
		if (purchase != null) {
			inAppPurchase.setPurchaseState(purchase.getPurchaseState() == 0
					? PurchaseState.PURCHASED : PurchaseState.NOT_PURCHASED);
			inAppPurchase.setPurchaseTime(purchase.getPurchaseTime());
		} else {
			inAppPurchase.setPurchaseState(PurchaseState.NOT_PURCHASED);
		}
		inAppPurchase.setPrice(skuDetails.getPrice());
		inAppPurchase.setPriceCurrencyCode(skuDetails.getPriceCurrencyCode());
		if (skuDetails.getPriceAmountMicros() > 0) {
			inAppPurchase.setPriceValue(skuDetails.getPriceAmountMicros() / 1000000d);
		}
		String subscriptionPeriod = skuDetails.getSubscriptionPeriod();
		if (!Algorithms.isEmpty(subscriptionPeriod)) {
			if (inAppPurchase instanceof InAppSubscription) {
				((InAppSubscription) inAppPurchase).setSubscriptionPeriod(subscriptionPeriod);
			}
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class LiveUpdatesPurchaseTask extends AsyncTask<Void, Void, String> {

		private WeakReference<Activity> activity;

		private String sku;
		private String email;
		private String userName;
		private String countryDownloadName;
		private boolean hideUserName;

		private String userId;

		LiveUpdatesPurchaseTask(Activity activity, String sku, String email, String userName,
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
						notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
						notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, "User token is empty.");
						stop(true);
						return;
					}
				} else {
					complain("Cannot retrieve userId from server.");
					notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
					notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, "Cannot retrieve userId from server.");
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
					notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
					notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, message);
					stop(true);
				}
			}

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
			if (!Algorithms.isEmpty(userId) && !Algorithms.isEmpty(token)) {
				logDebug("Launching purchase flow for live updates subscription for userId=" + userId);
				final String payload = userId + " " + token;
				exec(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, new InAppRunnable() {
					@Override
					public boolean run(InAppPurchaseHelper helper) {
						try {
							Activity a = activity.get();
							if (a != null) {
								mHelper.launchPurchaseFlow(a,
										sku, ITEM_TYPE_SUBS,
										RC_REQUEST, mPurchaseFinishedListener, payload);
								return false;
							} else {
								stop(true);
							}
						} catch (Exception e) {
							logError("launchPurchaseFlow Error", e);
							stop(true);
						}
						return true;
					}
				});
			} else {
				notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, "Empty userId");
				stop(true);
			}
		}
	}

	public boolean onActivityResultHandled(int requestCode, int resultCode, Intent data) {
		logDebug("onActivityResult(" + requestCode + "," + resultCode + "," + data);
		if (mHelper == null) return false;

		try {
			// Pass on the activity result to the helper for handling
			if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
				// not handled, so handle it ourselves (here's where you'd
				// perform any handling of activity results not related to in-app
				// billing...
				//super.onActivityResult(requestCode, resultCode, data);
				return false;
			} else {
				logDebug("onActivityResult handled by IABUtil.");
				return true;
			}
		} catch (Exception e) {
			logError("onActivityResultHandled", e);
			return false;
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class RequestInventoryTask extends AsyncTask<Void, Void, String> {

		RequestInventoryTask() {
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("androidPackage", ctx.getPackageName());
				addUserInfo(parameters);
				return AndroidNetworkUtils.sendRequest(ctx,
						"https://osmand.net/api/subscriptions/active",
						parameters, "Requesting active subscriptions...", false, false);

			} catch (Exception e) {
				logError("sendRequest Error", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String response) {
			logDebug("Response=" + response);
			if (response != null) {
				try {
					JSONObject obj = new JSONObject(response);
					JSONArray names = obj.names();
					for (int i = 0; i < names.length(); i++) {
						String skuType = names.getString(i);
						JSONObject subObj = obj.getJSONObject(skuType);
						String sku = subObj.getString("sku");
						if (!Algorithms.isEmpty(sku)) {
							getLiveUpdates().upgradeSubscription(sku);
						}
					}
				} catch (JSONException e) {
					logError("Json parsing error", e);
				}
			}
			exec(InAppPurchaseTaskType.REQUEST_INVENTORY, new InAppRunnable() {
				@Override
				public boolean run(InAppPurchaseHelper helper) {
					logDebug("Setup successful. Querying inventory.");
					Set<String> skus = new HashSet<>();
					for (InAppPurchase purchase : purchases.getAllInAppPurchases()) {
						skus.add(purchase.getSku());
					}
					try {
						mHelper.queryInventoryAsync(true, new ArrayList<>(skus), mGotInventoryListener);
						return false;
					} catch (Exception e) {
						logError("queryInventoryAsync Error", e);
						notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
						stop(true);
					}
					return true;
				}
			});
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

	// Callback for when a purchase is finished
	private OnIabPurchaseFinishedListener mPurchaseFinishedListener = new OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			logDebug("Purchase finished: " + result + ", purchase: " + purchase);

			// if we were disposed of in the meantime, quit.
			if (mHelper == null) {
				stop(true);
				return;
			}

			if (result.isFailure()) {
				if (result.getResponse() != IABHELPER_USER_CANCELLED) {
					complain("Error purchasing: " + result);
				}
				notifyDismissProgress(activeTask);
				notifyError(activeTask, "Error purchasing: " + result);
				stop(true);
				return;
			}

			logDebug("Purchase successful.");

			InAppPurchase liveUpdatesPurchase = getLiveUpdates().getSubscriptionBySku(purchase.getSku());
			if (liveUpdatesPurchase != null) {
				// bought live updates
				logDebug("Live updates subscription purchased.");
				final String sku = liveUpdatesPurchase.getSku();
				liveUpdatesPurchase.setPurchaseState(purchase.getPurchaseState() == 0 ? PurchaseState.PURCHASED : PurchaseState.NOT_PURCHASED);
				sendTokens(Collections.singletonList(purchase), new OnRequestResultListener() {
					@Override
					public void onResult(String result) {
						boolean active = ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_TIME.set(0L);
						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_FIRST_DLG_SHOWN.set(false);
						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_SECOND_DLG_SHOWN.set(false);

						notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
						notifyItemPurchased(sku, active);
						stop(true);
					}
				});

			} else if (purchase.getSku().equals(getFullVersion().getSku())) {
				// bought full version
				getFullVersion().setPurchaseState(purchase.getPurchaseState() == 0 ? PurchaseState.PURCHASED : PurchaseState.NOT_PURCHASED);
				logDebug("Full version purchased.");
				showToast(ctx.getString(R.string.full_version_thanks));
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
				notifyItemPurchased(getFullVersion().getSku(), false);
				stop(true);

			} else if (purchase.getSku().equals(getDepthContours().getSku())) {
				// bought sea depth contours
				getDepthContours().setPurchaseState(purchase.getPurchaseState() == 0 ? PurchaseState.PURCHASED : PurchaseState.NOT_PURCHASED);
				logDebug("Sea depth contours purchased.");
				showToast(ctx.getString(R.string.sea_depth_thanks));
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
				ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
				notifyItemPurchased(getDepthContours().getSku(), false);
				stop(true);

			} else {
				notifyDismissProgress(activeTask);
				stop(true);
			}
		}
	};

	// Do not forget call stop() when helper is not needed anymore
	public void stop() {
		stop(false);
	}

	private void stop(boolean taskDone) {
		logDebug("Destroying helper.");
		if (mHelper != null) {
			if (taskDone) {
				processingTask = false;
			}
			if (!processingTask) {
				activeTask = null;
				mHelper.dispose();
				mHelper = null;
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

	private void sendTokens(@NonNull final List<Purchase> purchases, final OnRequestResultListener listener) {
		final String userId = ctx.getSettings().BILLING_USER_ID.get();
		final String token = ctx.getSettings().BILLING_USER_TOKEN.get();
		final String email = ctx.getSettings().BILLING_USER_EMAIL.get();
		try {
			String url = "https://osmand.net/subscription/purchased";
			String userOperation = "Sending purchase info...";
			final List<AndroidNetworkUtils.Request> requests = new ArrayList<>();
			for (Purchase purchase : purchases) {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("userid", userId);
				parameters.put("sku", purchase.getSku());
				parameters.put("purchaseToken", purchase.getToken());
				parameters.put("email", email);
				parameters.put("token", token);
				addUserInfo(parameters);
				requests.add(new AndroidNetworkUtils.Request(url, parameters, userOperation, true, true));
			}
			AndroidNetworkUtils.sendRequestsAsync(ctx, requests, new OnRequestResultListener() {
				@Override
				public void onResult(String result) {
					if (result != null) {
						try {
							JSONObject obj = new JSONObject(result);
							if (!obj.has("error")) {
								String tokensSentStr = ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get();
								Set<String> tokensSent = new HashSet<>(Arrays.asList(tokensSentStr.split(";")));
								for (Purchase purchase : purchases) {
									tokensSent.add(purchase.getSku());
								}
								ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.set(TextUtils.join(";", tokensSent));

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
							} else {
								complain("SendToken Error: "
										+ obj.getString("error")
										+ " (userId=" + userId + " token=" + token + " response=" + result + ")");
							}
						} catch (JSONException e) {
							logError("SendToken", e);
							complain("SendToken Error: "
									+ (e.getMessage() != null ? e.getMessage() : "JSONException")
									+ " (userId=" + userId + " token=" + token + " response=" + result + ")");
						}
					}
					if (listener != null) {
						listener.onResult("OK");
					}
				}
			});
		} catch (Exception e) {
			logError("SendToken Error", e);
			if (listener != null) {
				listener.onResult("Error");
			}
		}
	}

	private void notifyError(InAppPurchaseTaskType taskType, String message) {
		if (uiActivity != null) {
			uiActivity.onError(taskType, message);
		}
	}

	private void notifyGetItems() {
		if (uiActivity != null) {
			uiActivity.onGetItems();
		}
	}

	private void notifyItemPurchased(String sku, boolean active) {
		if (uiActivity != null) {
			uiActivity.onItemPurchased(sku, active);
		}
	}

	private void notifyShowProgress(InAppPurchaseTaskType taskType) {
		if (uiActivity != null) {
			uiActivity.showProgress(taskType);
		}
	}

	private void notifyDismissProgress(InAppPurchaseTaskType taskType) {
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

	private void complain(String message) {
		logError("**** InAppPurchaseHelper Error: " + message);
		showToast(message);
	}

	private void showToast(final String message) {
		ctx.showToastMessage(message);
	}

	private void logDebug(String msg) {
		if (mDebugLog) {
			Log.d(TAG, msg);
		}
	}

	private void logError(String msg) {
		Log.e(TAG, msg);
	}

	private void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

}
