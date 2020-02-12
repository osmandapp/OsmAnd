package net.osmand.plus.inapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidNetworkUtils.OnRequestsResultListener;
import net.osmand.AndroidNetworkUtils.RequestResponse;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchaseLiveUpdatesOldSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionIntroductoryInfo;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionList;
import net.osmand.plus.inapp.util.BillingManager;
import net.osmand.plus.inapp.util.BillingManager.BillingUpdatesListener;
import net.osmand.plus.liveupdates.CountrySelectionFragment;
import net.osmand.plus.liveupdates.CountrySelectionFragment.CountryItem;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InAppPurchaseHelper {
	// Debug tag, for logging
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(InAppPurchaseHelper.class);
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();
	private boolean mDebugLog = false;

	public static final long SUBSCRIPTION_HOLDING_TIME_MSEC = 1000 * 60 * 60 * 24 * 3; // 3 days

	private InAppPurchases purchases;
	private long lastValidationCheckTime;

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily
	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	// The helper object
	private BillingManager billingManager;
	private List<SkuDetails> skuDetailsList;

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

	private BillingManager getBillingManager() {
		return billingManager;
	}

	private void exec(final @NonNull InAppPurchaseTaskType taskType, final @NonNull InAppRunnable runnable) {
		if (isDeveloperVersion || !Version.isGooglePlayEnabled(ctx)) {
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
			billingManager = new BillingManager(ctx, BASE64_ENCODED_PUBLIC_KEY, new BillingUpdatesListener() {

				@Override
				public void onBillingClientSetupFinished() {
					logDebug("Setup finished.");

					BillingManager billingManager = getBillingManager();
					// Have we been disposed of in the meantime? If so, quit.
					if (billingManager == null) {
						stop(true);
						return;
					}

					if (!billingManager.isServiceConnected()) {
						// Oh noes, there was a problem.
						//complain("Problem setting up in-app billing: " + result);
						notifyError(taskType, billingManager.getBillingClientResponseMessage());
						stop(true);
						return;
					}

					processingTask = !runnable.run(InAppPurchaseHelper.this);
				}

				@Override
				public void onConsumeFinished(String token, BillingResult billingResult) {
				}

				@Override
				public void onPurchasesUpdated(final List<Purchase> purchases) {

					BillingManager billingManager = getBillingManager();
					// Have we been disposed of in the meantime? If so, quit.
					if (billingManager == null) {
						stop(true);
						return;
					}

					if (activeTask == InAppPurchaseTaskType.REQUEST_INVENTORY) {
						List<String> skuInApps = new ArrayList<>();
						for (InAppPurchase purchase : getInAppPurchases().getAllInAppPurchases(false)) {
							skuInApps.add(purchase.getSku());
						}
						for (Purchase p : purchases) {
							skuInApps.add(p.getSku());
						}
						billingManager.querySkuDetailsAsync(SkuType.INAPP, skuInApps, new SkuDetailsResponseListener() {
							@Override
							public void onSkuDetailsResponse(BillingResult billingResult, final List<SkuDetails> skuDetailsListInApps) {
								// Is it a failure?
								if (billingResult.getResponseCode() != BillingResponseCode.OK) {
									logError("Failed to query inapps sku details: " + billingResult.getResponseCode());
									notifyError(InAppPurchaseTaskType.REQUEST_INVENTORY, billingResult.getDebugMessage());
									stop(true);
									return;
								}

								List<String> skuSubscriptions = new ArrayList<>();
								for (InAppSubscription subscription : getInAppPurchases().getAllInAppSubscriptions()) {
									skuSubscriptions.add(subscription.getSku());
								}
								for (Purchase p : purchases) {
									skuSubscriptions.add(p.getSku());
								}

								BillingManager billingManager = getBillingManager();
								// Have we been disposed of in the meantime? If so, quit.
								if (billingManager == null) {
									stop(true);
									return;
								}

								billingManager.querySkuDetailsAsync(SkuType.SUBS, skuSubscriptions, new SkuDetailsResponseListener() {
									@Override
									public void onSkuDetailsResponse(BillingResult billingResult, final List<SkuDetails> skuDetailsListSubscriptions) {
										// Is it a failure?
										if (billingResult.getResponseCode() != BillingResponseCode.OK) {
											logError("Failed to query subscriptipons sku details: " + billingResult.getResponseCode());
											notifyError(InAppPurchaseTaskType.REQUEST_INVENTORY, billingResult.getDebugMessage());
											stop(true);
											return;
										}

										List<SkuDetails> skuDetailsList = new ArrayList<>(skuDetailsListInApps);
										skuDetailsList.addAll(skuDetailsListSubscriptions);
										InAppPurchaseHelper.this.skuDetailsList = skuDetailsList;

										mSkuDetailsResponseListener.onSkuDetailsResponse(billingResult, skuDetailsList);
									}
								});
							}
						});
					}
					for (Purchase purchase : purchases) {
						if (!purchase.isAcknowledged()) {
							onPurchaseFinished(purchase);
						}
					}
				}

				@Override
				public void onPurchaseCanceled() {
					stop(true);
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
					SkuDetails skuDetails = getSkuDetails(getFullVersion().getSku());
					if (skuDetails == null) {
						throw new IllegalArgumentException("Cannot find sku details");
					}
					BillingManager billingManager = getBillingManager();
					if (billingManager != null) {
						billingManager.initiatePurchaseFlow(activity, skuDetails);
					} else {
						throw new IllegalStateException("BillingManager disposed");
					}
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
					SkuDetails skuDetails = getSkuDetails(getDepthContours().getSku());
					if (skuDetails == null) {
						throw new IllegalArgumentException("Cannot find sku details");
					}
					BillingManager billingManager = getBillingManager();
					if (billingManager != null) {
						billingManager.initiatePurchaseFlow(activity, skuDetails);
					} else {
						throw new IllegalStateException("BillingManager disposed");
					}
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

	@Nullable
	private SkuDetails getSkuDetails(@NonNull String sku) {
		List<SkuDetails> skuDetailsList = this.skuDetailsList;
		if (skuDetailsList != null) {
			for (SkuDetails details : skuDetailsList) {
				if (details.getSku().equals(sku)) {
					return details;
				}
			}
		}
		return null;
	}

	private boolean hasDetails(@NonNull String sku) {
		return getSkuDetails(sku) != null;
	}

	@Nullable
	private Purchase getPurchase(@NonNull String sku) {
		BillingManager billingManager = getBillingManager();
		if (billingManager != null) {
			List<Purchase> purchases = billingManager.getPurchases();
			if (purchases != null) {
				for (Purchase p : purchases) {
					if (p.getSku().equals(sku)) {
						return p;
					}
				}
			}
		}
		return null;
	}

	// Listener that's called when we finish querying the items and subscriptions we own
	private SkuDetailsResponseListener mSkuDetailsResponseListener = new SkuDetailsResponseListener() {

		@NonNull
		private List<String> getAllOwnedSubscriptionSkus() {
			List<String> result = new ArrayList<>();
			BillingManager billingManager = getBillingManager();
			if (billingManager != null) {
				for (Purchase p : billingManager.getPurchases()) {
					if (getInAppPurchases().getInAppSubscriptionBySku(p.getSku()) != null) {
						result.add(p.getSku());
					}
				}
			}
			return result;
		}

		@Override
		public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {

			logDebug("Query sku details finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (getBillingManager() == null) {
				stop(true);
				return;
			}

			// Is it a failure?
			if (billingResult.getResponseCode() != BillingResponseCode.OK) {
				logError("Failed to query inventory: " + billingResult.getResponseCode());
				notifyError(InAppPurchaseTaskType.REQUEST_INVENTORY, billingResult.getDebugMessage());
				stop(true);
				return;
			}

			logDebug("Query sku details was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! See
			 * verifyDeveloperPayload().
			 */

			List<String> allOwnedSubscriptionSkus = getAllOwnedSubscriptionSkus();
			for (InAppSubscription s : getLiveUpdates().getAllSubscriptions()) {
				if (hasDetails(s.getSku())) {
					Purchase purchase = getPurchase(s.getSku());
					SkuDetails liveUpdatesDetails = getSkuDetails(s.getSku());
					if (liveUpdatesDetails != null) {
						fetchInAppPurchase(s, liveUpdatesDetails, purchase);
					}
					allOwnedSubscriptionSkus.remove(s.getSku());
				}
			}
			for (String sku : allOwnedSubscriptionSkus) {
				Purchase purchase = getPurchase(sku);
				SkuDetails liveUpdatesDetails = getSkuDetails(sku);
				if (liveUpdatesDetails != null) {
					InAppSubscription s = getLiveUpdates().upgradeSubscription(sku);
					if (s == null) {
						s = new InAppPurchaseLiveUpdatesOldSubscription(liveUpdatesDetails);
					}
					fetchInAppPurchase(s, liveUpdatesDetails, purchase);
				}
			}

			InAppPurchase fullVersion = getFullVersion();
			if (hasDetails(fullVersion.getSku())) {
				Purchase purchase = getPurchase(fullVersion.getSku());
				SkuDetails fullPriceDetails = getSkuDetails(fullVersion.getSku());
				if (fullPriceDetails != null) {
					fetchInAppPurchase(fullVersion, fullPriceDetails, purchase);
				}
			}

			InAppPurchase depthContours = getDepthContours();
			if (hasDetails(depthContours.getSku())) {
				Purchase purchase = getPurchase(depthContours.getSku());
				SkuDetails depthContoursDetails = getSkuDetails(depthContours.getSku());
				if (depthContoursDetails != null) {
					fetchInAppPurchase(depthContours, depthContoursDetails, purchase);
				}
			}

			InAppPurchase contourLines = getContourLines();
			if (hasDetails(contourLines.getSku())) {
				Purchase purchase = getPurchase(contourLines.getSku());
				SkuDetails contourLinesDetails = getSkuDetails(contourLines.getSku());
				if (contourLinesDetails != null) {
					fetchInAppPurchase(contourLines, contourLinesDetails, purchase);
				}
			}

			Purchase fullVersionPurchase = getPurchase(fullVersion.getSku());
			boolean fullVersionPurchased = fullVersionPurchase != null;
			if (fullVersionPurchased) {
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
			}

			Purchase depthContoursPurchase = getPurchase(depthContours.getSku());
			boolean depthContoursPurchased = depthContoursPurchase != null;
			if (depthContoursPurchased) {
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			}

			// Do we have the live updates?
			boolean subscribedToLiveUpdates = false;
			List<Purchase> liveUpdatesPurchases = new ArrayList<>();
			for (InAppPurchase p : getLiveUpdates().getAllSubscriptions()) {
				Purchase purchase = getPurchase(p.getSku());
				if (purchase != null) {
					liveUpdatesPurchases.add(purchase);
					if (!subscribedToLiveUpdates) {
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
			inAppPurchase.setPurchaseState(PurchaseState.PURCHASED);
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
				try {
					((InAppSubscription) inAppPurchase).setSubscriptionPeriodString(subscriptionPeriod);
				} catch (ParseException e) {
					LOG.error(e);
				}
			}
		}
		if (inAppPurchase instanceof InAppSubscription) {
			String introductoryPrice = skuDetails.getIntroductoryPrice();
			String introductoryPricePeriod = skuDetails.getIntroductoryPricePeriod();
			String introductoryPriceCycles = skuDetails.getIntroductoryPriceCycles();
			long introductoryPriceAmountMicros = skuDetails.getIntroductoryPriceAmountMicros();
			if (!Algorithms.isEmpty(introductoryPrice)) {
				InAppSubscription s = (InAppSubscription) inAppPurchase;
				try {
					s.setIntroductoryInfo(new InAppSubscriptionIntroductoryInfo(s, introductoryPrice,
							introductoryPriceAmountMicros, introductoryPricePeriod, introductoryPriceCycles));
				} catch (ParseException e) {
					LOG.error(e);
				}
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
							SkuDetails skuDetails = getSkuDetails(sku);
							if (a != null && skuDetails != null) {
								BillingManager billingManager = getBillingManager();
								if (billingManager != null) {
									billingManager.setPayload(payload);
									billingManager.initiatePurchaseFlow(a, skuDetails);
								} else {
									throw new IllegalStateException("BillingManager disposed");
								}
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
					try {
						BillingManager billingManager = getBillingManager();
						if (billingManager != null) {
							billingManager.queryPurchases();
						} else {
							throw new IllegalStateException("BillingManager disposed");
						}
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

	// Call when a purchase is finished
	private void onPurchaseFinished(Purchase purchase) {
		logDebug("Purchase finished: " + purchase);

		// if we were disposed of in the meantime, quit.
		if (getBillingManager() == null) {
			stop(true);
			return;
		}

		logDebug("Purchase successful.");

		InAppPurchase liveUpdatesPurchase = getLiveUpdates().getSubscriptionBySku(purchase.getSku());
		if (liveUpdatesPurchase != null) {
			// bought live updates
			logDebug("Live updates subscription purchased.");
			final String sku = liveUpdatesPurchase.getSku();
			liveUpdatesPurchase.setPurchaseState(PurchaseState.PURCHASED);
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
			getFullVersion().setPurchaseState(PurchaseState.PURCHASED);
			logDebug("Full version purchased.");
			showToast(ctx.getString(R.string.full_version_thanks));
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

			notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
			notifyItemPurchased(getFullVersion().getSku(), false);
			stop(true);

		} else if (purchase.getSku().equals(getDepthContours().getSku())) {
			// bought sea depth contours
			getDepthContours().setPurchaseState(PurchaseState.PURCHASED);
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

	// Do not forget call stop() when helper is not needed anymore
	public void stop() {
		stop(false);
	}

	private void stop(boolean taskDone) {
		logDebug("Destroying helper.");
		BillingManager billingManager = getBillingManager();
		if (billingManager != null) {
			if (taskDone) {
				processingTask = false;
			}
			if (!processingTask) {
				activeTask = null;
				billingManager.destroy();
				this.billingManager = null;
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
				parameters.put("orderId", purchase.getOrderId());
				parameters.put("purchaseToken", purchase.getPurchaseToken());
				parameters.put("email", email);
				parameters.put("token", token);
				addUserInfo(parameters);
				requests.add(new AndroidNetworkUtils.Request(url, parameters, userOperation, true, true));
			}
			AndroidNetworkUtils.sendRequestsAsync(ctx, requests, new OnRequestsResultListener() {
				@Override
				public void onResult(@NonNull List<RequestResponse> results) {
					for (RequestResponse rr : results) {
						String sku = rr.getRequest().getParameters().get("sku");
						Purchase purchase = getPurchase(sku);
						if (purchase != null) {
							updateSentTokens(purchase);
							String result = rr.getResponse();
							if (result != null) {
								try {
									JSONObject obj = new JSONObject(result);
									if (!obj.has("error")) {
										processPurchasedJson(obj);
									} else {
										complain("SendToken Error: "
												+ obj.getString("error")
												+ " (userId=" + userId + " token=" + token + " response=" + result + " google=" + purchase.toString() + ")");
									}
								} catch (JSONException e) {
									logError("SendToken", e);
									complain("SendToken Error: "
											+ (e.getMessage() != null ? e.getMessage() : "JSONException")
											+ " (userId=" + userId + " token=" + token + " response=" + result + " google=" + purchase.toString() + ")");
								}
							}
						}
					}
					if (listener != null) {
						listener.onResult("OK");
					}
				}

				private void updateSentTokens(@NonNull Purchase purchase) {
					String tokensSentStr = ctx.getSettings().BILLING_PURCHASE_TOKENS_SENT.get();
					Set<String> tokensSent = new HashSet<>(Arrays.asList(tokensSentStr.split(";")));
					tokensSent.add(purchase.getSku());
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
				private Purchase getPurchase(String sku) {
					for (Purchase purchase : purchases) {
						if (purchase.getSku().equals(sku)) {
							return purchase;
						}
					}
					return null;
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
