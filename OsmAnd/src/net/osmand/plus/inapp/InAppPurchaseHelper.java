package net.osmand.plus.inapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.inapp.util.IabHelper.IABHELPER_USER_CANCELLED;

public class InAppPurchaseHelper {
	// Debug tag, for logging
	private static final String TAG = InAppPurchaseHelper.class.getSimpleName();
	boolean mDebugLog = true;

	public static final long SUBSCRIPTION_HOLDING_TIME_MSEC = 1000 * 60 * 60 * 24 * 3; // 3 days

	private long lastValidationCheckTime;
	private String liveUpdatesPrice;
	private String fullVersionPrice;
	private String depthContoursPrice;
	private String contourLinesPrice;

	public static final String SKU_FULL_VERSION_PRICE = "osmand_full_version_price";

	private static final String SKU_LIVE_UPDATES_FULL = "osm_live_subscription_2";
	private static final String SKU_LIVE_UPDATES_FREE = "osm_free_live_subscription_2";
	private static final String SKU_DEPTH_CONTOURS_FULL = "net.osmand.seadepth_plus";
	private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.seadepth";
	private static final String SKU_CONTOUR_LINES_FULL = "net.osmand.contourlines_plus";
	private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.contourlines";

	public static String SKU_LIVE_UPDATES;
	public static String SKU_DEPTH_CONTOURS;
	public static String SKU_CONTOUR_LINES;

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily
	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	// The helper object
	private IabHelper mHelper;
	private boolean isDeveloperVersion;
	private String token = "";
	private InAppPurchaseTaskType activeTask;
	private boolean processingTask = false;

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

	public String getLiveUpdatesPrice() {
		return liveUpdatesPrice;
	}

	public String getFullVersionPrice() {
		return fullVersionPrice;
	}

	public String getDepthContoursPrice() {
		return depthContoursPrice;
	}

	public String getContourLinesPrice() {
		return contourLinesPrice;
	}

	public String getSkuLiveUpdates() {
		return SKU_LIVE_UPDATES;
	}

	public boolean hasPrices() {
		return !Algorithms.isEmpty(liveUpdatesPrice)
				&& (!Version.isFreeVersion(ctx) || !Algorithms.isEmpty(fullVersionPrice));
	}

	private void initialize() {
		if (SKU_LIVE_UPDATES == null) {
			if (Version.isFreeVersion(ctx)) {
				SKU_LIVE_UPDATES = SKU_LIVE_UPDATES_FREE;
			} else {
				SKU_LIVE_UPDATES = SKU_LIVE_UPDATES_FULL;
			}
		}
		if (SKU_DEPTH_CONTOURS == null) {
			if (Version.isFreeVersion(ctx)) {
				SKU_DEPTH_CONTOURS = SKU_DEPTH_CONTOURS_FREE;
			} else {
				SKU_DEPTH_CONTOURS = SKU_DEPTH_CONTOURS_FULL;
			}
		}
		if (SKU_CONTOUR_LINES == null) {
			if (Version.isFreeVersion(ctx)) {
				SKU_CONTOUR_LINES = SKU_CONTOUR_LINES_FREE;
			} else {
				SKU_CONTOUR_LINES = SKU_CONTOUR_LINES_FULL;
			}
		}
	}

	public InAppPurchaseHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		isDeveloperVersion = Version.isDeveloperVersion(ctx);
		initialize();
	}

	public boolean hasInventory() {
		return lastValidationCheckTime != 0;
	}

	public boolean isPurchased(String inAppSku) {
		if (inAppSku.equals(SKU_FULL_VERSION_PRICE)) {
			return isFullVersionPurchased(ctx);
		} else if (inAppSku.equals(SKU_LIVE_UPDATES_FULL) || inAppSku.equals(SKU_LIVE_UPDATES_FREE)) {
			return isSubscribedToLiveUpdates(ctx);
		} else if (inAppSku.equals(SKU_DEPTH_CONTOURS_FULL) || inAppSku.equals(SKU_DEPTH_CONTOURS_FREE)) {
			return isDepthContoursPurchased(ctx);
		}
		return false;
	}

	private void exec(final @NonNull InAppPurchaseTaskType taskType, final @NonNull InAppRunnable runnable) {
		if (isDeveloperVersion || !Version.isGooglePlayEnabled(ctx)) {
			return;
		}

		if (processingTask) {
			logError("Already processing task: " + taskType + ". Exit.");
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
		return (isSubscribedToLiveUpdates(ctx) && !ctx.getSettings().BILLING_PURCHASE_TOKEN_SENT.get())
				|| System.currentTimeMillis() - lastValidationCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC;
	}

	public void requestInventory() {
		notifyShowProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
		exec(InAppPurchaseTaskType.REQUEST_INVENTORY, new InAppRunnable() {
			@Override
			public boolean run(InAppPurchaseHelper helper) {
				logDebug("Setup successful. Querying inventory.");
				List<String> skus = new ArrayList<>();
				skus.add(SKU_LIVE_UPDATES);
				skus.add(SKU_DEPTH_CONTOURS);
				skus.add(SKU_CONTOUR_LINES);
				skus.add(SKU_FULL_VERSION_PRICE);
				try {
					mHelper.queryInventoryAsync(true, skus, mGotInventoryListener);
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

	public void purchaseFullVersion(final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
		exec(InAppPurchaseTaskType.PURCHASE_FULL_VERSION, new InAppRunnable() {
			@Override
			public boolean run(InAppPurchaseHelper helper) {
				try {
					mHelper.launchPurchaseFlow(activity,
							SKU_FULL_VERSION_PRICE, RC_REQUEST, mPurchaseFinishedListener);
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

	public void purchaseLiveUpdates(Activity activity, String email, String userName,
									String countryDownloadName, boolean hideUserName) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
		new LiveUpdatesPurchaseTask(activity, email, userName, countryDownloadName, hideUserName)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public void purchaseDepthContours(final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
		exec(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS, new InAppRunnable() {
			@Override
			public boolean run(InAppPurchaseHelper helper) {
				try {
					mHelper.launchPurchaseFlow(activity,
							SKU_DEPTH_CONTOURS, RC_REQUEST, mPurchaseFinishedListener);
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

			// Do we have the live updates?
			Purchase liveUpdatesPurchase = inventory.getPurchase(SKU_LIVE_UPDATES);
			boolean subscribedToLiveUpdates = (liveUpdatesPurchase != null && liveUpdatesPurchase.getPurchaseState() == 0);
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

			Purchase fullVersionPurchase = inventory.getPurchase(SKU_FULL_VERSION_PRICE);
			boolean fullVersionPurchased = (fullVersionPurchase != null && fullVersionPurchase.getPurchaseState() == 0);
			if (fullVersionPurchased) {
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
			}

			Purchase depthContoursPurchase = inventory.getPurchase(SKU_DEPTH_CONTOURS);
			boolean depthContoursPurchased = (depthContoursPurchase != null && depthContoursPurchase.getPurchaseState() == 0);
			if (depthContoursPurchased) {
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			}

			lastValidationCheckTime = System.currentTimeMillis();
			logDebug("User " + (subscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE")
					+ " live updates purchased.");

			if (inventory.hasDetails(SKU_LIVE_UPDATES)) {
				SkuDetails liveUpdatesDetails = inventory.getSkuDetails(SKU_LIVE_UPDATES);
				liveUpdatesPrice = liveUpdatesDetails.getPrice();
			}
			if (inventory.hasDetails(SKU_FULL_VERSION_PRICE)) {
				SkuDetails fullPriceDetails = inventory.getSkuDetails(SKU_FULL_VERSION_PRICE);
				fullVersionPrice = fullPriceDetails.getPrice();
			}
			if (inventory.hasDetails(SKU_DEPTH_CONTOURS)) {
				SkuDetails depthContoursDetails = inventory.getSkuDetails(SKU_DEPTH_CONTOURS);
				depthContoursPrice = depthContoursDetails.getPrice();
			}
			if (inventory.hasDetails(SKU_CONTOUR_LINES)) {
				SkuDetails contourLinesDetails = inventory.getSkuDetails(SKU_CONTOUR_LINES);
				contourLinesPrice = contourLinesDetails.getPrice();
			}
			OsmandSettings settings = ctx.getSettings();
			settings.INAPPS_READ.set(true);

			boolean needSendToken = false;
			if (liveUpdatesPurchase != null) {
				if ((Algorithms.isEmpty(settings.BILLING_USER_ID.get()) || Algorithms.isEmpty(settings.BILLING_USER_TOKEN.get()))
						&& !Algorithms.isEmpty(liveUpdatesPurchase.getDeveloperPayload())) {
					String payload = liveUpdatesPurchase.getDeveloperPayload();
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
				if (!settings.BILLING_PURCHASE_TOKEN_SENT.get()) {
					needSendToken = true;
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

			if (needSendToken) {
				sendToken(liveUpdatesPurchase.getToken(), listener);
			} else {
				listener.onResult("OK");
			}
		}
	};

	@SuppressLint("StaticFieldLeak")
	private class LiveUpdatesPurchaseTask extends AsyncTask<Void, Void, String> {

		private WeakReference<Activity> activity;

		private String email;
		private String userName;
		private String countryDownloadName;
		private boolean hideUserName;

		private String userId;

		LiveUpdatesPurchaseTask(Activity activity, String email, String userName,
								String countryDownloadName, boolean hideUserName) {
			this.activity = new WeakReference<>(activity);

			this.email = email;
			this.userName = userName;
			this.countryDownloadName = countryDownloadName;
			this.hideUserName = hideUserName;
		}

		@Override
		protected String doInBackground(Void... params) {
			userId = ctx.getSettings().BILLING_USER_ID.get();
			try {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("visibleName", hideUserName ? "" : userName);
				parameters.put("preferredCountry", countryDownloadName);
				parameters.put("email", email);
				if (Algorithms.isEmpty(userId)) {
					parameters.put("status", "new");
				}

				return AndroidNetworkUtils.sendRequest(ctx,
						"https://osmand.net/subscription/register",
						parameters, "Requesting userId...", true, true);

			} catch (Exception e) {
				logError("sendRequest Error", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String response) {
			logDebug("Response=" + response);
			if (response == null) {
				complain("Cannot retrieve userId from server.");
				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
				notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, "Cannot retrieve userId from server.");
				stop(true);
				return;

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
			if (!Algorithms.isEmpty(userId)) {
				logDebug("Launching purchase flow for live updates subscription for userId=" + userId);
				final String payload = userId + " " + token;
				exec(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES, new InAppRunnable() {
					@Override
					public boolean run(InAppPurchaseHelper helper) {
						try {
							Activity a = activity.get();
							if (a != null) {
								mHelper.launchPurchaseFlow(a,
										SKU_LIVE_UPDATES, IabHelper.ITEM_TYPE_SUBS,
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
				notifyError(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES,"Empty userId");
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

			if (purchase.getSku().equals(SKU_LIVE_UPDATES)) {
				// bought live updates
				logDebug("Live updates subscription purchased.");
				sendToken(purchase.getToken(), new OnRequestResultListener() {
					@Override
					public void onResult(String result) {
						boolean active = ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_TIME.set(0L);
						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_FIRST_DLG_SHOWN.set(false);
						ctx.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_SECOND_DLG_SHOWN.set(false);

						notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_LIVE_UPDATES);
						notifyItemPurchased(SKU_LIVE_UPDATES, active);
						stop(true);
					}
				});

			} else if (purchase.getSku().equals(SKU_FULL_VERSION_PRICE)) {
				// bought full version
				logDebug("Full version purchased.");
				showToast(ctx.getString(R.string.full_version_thanks));
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
				notifyItemPurchased(SKU_FULL_VERSION_PRICE, false);
				stop(true);

			} else if (purchase.getSku().equals(SKU_DEPTH_CONTOURS)) {
				// bought sea depth contours
				logDebug("Sea depth contours purchased.");
				showToast(ctx.getString(R.string.sea_depth_thanks));
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
				ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

				notifyDismissProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
				notifyItemPurchased(SKU_DEPTH_CONTOURS, false);
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
	}

	private void sendToken(String purchaseToken, final OnRequestResultListener listener) {
		final String userId = ctx.getSettings().BILLING_USER_ID.get();
		final String token = ctx.getSettings().BILLING_USER_TOKEN.get();
		final String email = ctx.getSettings().BILLING_USER_EMAIL.get();
		try {
			Map<String, String> parameters = new HashMap<>();
			parameters.put("userid", userId);
			parameters.put("sku", SKU_LIVE_UPDATES);
			parameters.put("purchaseToken", purchaseToken);
			parameters.put("email", email);
			parameters.put("token", token);

			AndroidNetworkUtils.sendRequestAsync(ctx,
					"https://osmand.net/subscription/purchased",
					parameters, "Sending purchase info...", true, true, new OnRequestResultListener() {
						@Override
						public void onResult(String result) {
							if (result != null) {
								try {
									JSONObject obj = new JSONObject(result);
									if (!obj.has("error")) {
										ctx.getSettings().BILLING_PURCHASE_TOKEN_SENT.set(true);
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
