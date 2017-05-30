package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InAppHelper {
	// Debug tag, for logging
	static final String TAG = "InAppHelper";
	boolean mDebugLog = false;

	private static boolean mSubscribedToLiveUpdates = false;
	private static boolean mFullVersionPurchased = false;
	private static boolean mDepthContoursPurchased = false;
	private static String mLiveUpdatesPrice;
	private static long lastValidationCheckTime;
	private static String mFullVersionPrice;
	private static String mDepthContoursPrice;

	public static final String SKU_FULL_VERSION_PRICE = "osmand_full_version_price";
	private static final String SKU_LIVE_UPDATES_FULL = "osm_live_subscription_2";
	private static final String SKU_LIVE_UPDATES_FREE = "osm_free_live_subscription_2";
	private static final String SKU_DEPTH_CONTOURS_FULL = "net.osmand.seadepth_plus";
	private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.seadepth";
	public static String SKU_LIVE_UPDATES;
	public static String SKU_DEPTH_CONTOURS;

	private static final long PURCHASE_VALIDATION_PERIOD_MSEC = 1000 * 60 * 60 * 24; // daily
	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	// The helper object
	private IabHelper mHelper;
	private boolean stopAfterResult = false;
	private boolean isDeveloperVersion = false;
	private boolean forceRequestInventory = false;
	private String token = "";
	private boolean inventoryRequesting = false;
	private boolean stopRequested = false;

	private OsmandApplication ctx;
	private List<InAppListener> listeners = new ArrayList<>();

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

	public interface InAppListener {
		void onError(String error);

		void onGetItems();

		void onItemPurchased(String sku);

		void showProgress();

		void dismissProgress();
	}

	public interface InAppRunnable {
		void run(InAppHelper helper);
	}

	public String getToken() {
		return token;
	}

	public static boolean isSubscribedToLiveUpdates() {
		return mSubscribedToLiveUpdates;
	}

	public static boolean isFullVersionPurchased() {
		return mFullVersionPurchased;
	}

	public static boolean isDepthContoursPurchased() {
		return mDepthContoursPurchased;
	}

	public static String getLiveUpdatesPrice() {
		return mLiveUpdatesPrice;
	}

	public static String getDepthContoursPrice() {
		return mDepthContoursPrice;
	}

	public static String getFullVersionPrice() {
		return mFullVersionPrice;
	}

	public static String getSkuLiveUpdates() {
		return SKU_LIVE_UPDATES;
	}

	public static boolean hasPrices(OsmandApplication app) {
		return !Algorithms.isEmpty(mLiveUpdatesPrice)
				&& (!Version.isFreeVersion(app) || !Algorithms.isEmpty(mFullVersionPrice));
	}

	public static void initialize(OsmandApplication ctx) {
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
	}

	public InAppHelper(OsmandApplication ctx, boolean forceRequestInventory) {
		this.ctx = ctx;
		this.forceRequestInventory = forceRequestInventory;

		isDeveloperVersion = Version.isDeveloperVersion(ctx);
		if (isDeveloperVersion) {
			mSubscribedToLiveUpdates = true;
			mFullVersionPurchased = true;
			mDepthContoursPurchased = true;
			ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
			ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
			ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
		}
	}

	public static boolean isInAppIntentoryRead() {
		return lastValidationCheckTime != 0;
	}

	public static boolean isPurchased(OsmandApplication ctx, String inAppSku) {
		OsmandSettings settings = ctx.getSettings();
		if (inAppSku.equals(SKU_FULL_VERSION_PRICE)) {
			return settings.FULL_VERSION_PURCHASED.get();
		} else if (inAppSku.equals(SKU_LIVE_UPDATES_FULL) || inAppSku.equals(SKU_LIVE_UPDATES_FREE)) {
			return settings.LIVE_UPDATES_PURCHASED.get();
		} else if (inAppSku.equals(SKU_DEPTH_CONTOURS_FULL) || inAppSku.equals(SKU_DEPTH_CONTOURS_FREE)) {
			return settings.DEPTH_CONTOURS_PURCHASED.get();
		}
		return false;
	}

	public void exec(final @NonNull InAppRunnable runnable) {
		this.stopAfterResult = true;

		// Create the helper, passing it our context and the public key to verify signatures with
		logDebug("Creating InAppHelper.");
		mHelper = new IabHelper(ctx, BASE64_ENCODED_PUBLIC_KEY);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(false);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		logDebug("Starting setup.");
		try {
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					logDebug("Setup finished.");

					if (!result.isSuccess()) {
						// Oh noes, there was a problem.
						//complain("Problem setting up in-app billing: " + result);
						notifyError(result.getMessage());
						if (stopAfterResult) {
							stop();
						}
						return;
					}

					// Have we been disposed of in the meantime? If so, quit.
					if (mHelper == null) return;

					runnable.run(InAppHelper.this);
				}
			});
		} catch (Exception e) {
			logError("exec Error", e);
			if (stopAfterResult) {
				stop();
			}
		}
	}

	public void start(final boolean stopAfterResult) {
		this.stopAfterResult = stopAfterResult;

		// Create the helper, passing it our context and the public key to verify signatures with
		logDebug("Creating InAppHelper.");
		mHelper = new IabHelper(ctx, BASE64_ENCODED_PUBLIC_KEY);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(false);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		logDebug("Starting setup.");
		try {
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					logDebug("Setup finished.");

					if (!result.isSuccess()) {
						// Oh noes, there was a problem.
						//complain("Problem setting up in-app billing: " + result);
						notifyError(result.getMessage());
						if (stopAfterResult) {
							stop();
						}
						return;
					}

					// Have we been disposed of in the meantime? If so, quit.
					if (mHelper == null) return;

					// IAB is fully set up. Now, let's get an inventory of stuff we own if needed.
					if (forceRequestInventory || (!isDeveloperVersion &&
							(!mSubscribedToLiveUpdates
									|| !ctx.getSettings().BILLING_PURCHASE_TOKEN_SENT.get()
									|| System.currentTimeMillis() - lastValidationCheckTime > PURCHASE_VALIDATION_PERIOD_MSEC))) {

						logDebug("Setup successful. Querying inventory.");
						List<String> skus = new ArrayList<>();
						skus.add(SKU_LIVE_UPDATES);
						skus.add(SKU_DEPTH_CONTOURS);
						skus.add(SKU_FULL_VERSION_PRICE);
						try {
							inventoryRequesting = true;
							mHelper.queryInventoryAsync(true, skus, mGotInventoryListener);
						} catch (Exception e) {
							inventoryRequesting = false;
							logError("queryInventoryAsync Error", e);
							notifyDismissProgress();
							if (stopAfterResult) {
								stop();
							}
						}
					} else {
						notifyDismissProgress();
						if (stopAfterResult) {
							stop();
						}
					}
				}
			});
		} catch (Exception e) {
			logError("start Error", e);
			if (stopAfterResult) {
				stop();
			}
		}
	}

	// Listener that's called when we finish querying the items and subscriptions we own
	private QueryInventoryFinishedListener mGotInventoryListener = new QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			logDebug("Query inventory finished.");
			inventoryRequesting = false;

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null) return;

			// Is it a failure?
			if (result.isFailure() || stopRequested) {
				logError("Failed to query inventory: " + result);
				notifyError(result.getMessage());
				if (stopAfterResult || stopRequested) {
					stop();
				}
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
			mSubscribedToLiveUpdates = isDeveloperVersion || (liveUpdatesPurchase != null && liveUpdatesPurchase.getPurchaseState() == 0);
			if (mSubscribedToLiveUpdates) {
				ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
			}

			Purchase fullVersionPurchase = inventory.getPurchase(SKU_FULL_VERSION_PRICE);
			mFullVersionPurchased = isDeveloperVersion || (fullVersionPurchase != null && fullVersionPurchase.getPurchaseState() == 0);
			if (mFullVersionPurchased) {
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
			}

			Purchase depthContoursPurchase = inventory.getPurchase(SKU_DEPTH_CONTOURS);
			mDepthContoursPurchased = isDeveloperVersion || (depthContoursPurchase != null && depthContoursPurchase.getPurchaseState() == 0);
			if (mDepthContoursPurchased) {
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
			}

			lastValidationCheckTime = System.currentTimeMillis();
			logDebug("User " + (mSubscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE")
					+ " live updates purchased.");

			if (inventory.hasDetails(SKU_LIVE_UPDATES)) {
				SkuDetails liveUpdatesDetails = inventory.getSkuDetails(SKU_LIVE_UPDATES);
				mLiveUpdatesPrice = liveUpdatesDetails.getPrice();
			}

			if (inventory.hasDetails(SKU_DEPTH_CONTOURS)) {
				SkuDetails depthContoursDetails = inventory.getSkuDetails(SKU_DEPTH_CONTOURS);
				mDepthContoursPrice = depthContoursDetails.getPrice();
			}

			if (inventory.hasDetails(SKU_FULL_VERSION_PRICE)) {
				SkuDetails fullPriceDetails = inventory.getSkuDetails(SKU_FULL_VERSION_PRICE);
				mFullVersionPrice = fullPriceDetails.getPrice();
			}

			boolean needSendToken = false;
			if (!isDeveloperVersion && liveUpdatesPurchase != null) {
				OsmandSettings settings = ctx.getSettings();
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
					notifyDismissProgress();
					notifyGetItems();
					if (stopAfterResult || stopRequested) {
						stop();
					}
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

	public void purchaseFullVersion(final Activity activity) {
		if (mHelper == null) {
			//complain("In-app hepler is not initialized!");
			notifyError("In-app hepler is not initialized!");
			if (stopAfterResult) {
				stop();
			}
			return;
		}

		logDebug("Launching purchase flow for full version");
		if (mHelper != null) {
			try {
				mHelper.launchPurchaseFlow(activity,
						SKU_FULL_VERSION_PRICE, RC_REQUEST, mPurchaseFinishedListener);
			} catch (Exception e) {
				complain("Cannot launch full version purchase!");
				logError("purchaseFullVersion Error", e);
				if (stopAfterResult) {
					stop();
				}
			}
		}
	}

	public void purchaseDepthContours(final Activity activity) {
		if (mHelper == null) {
			//complain("In-app hepler is not initialized!");
			notifyError("In-app hepler is not initialized!");
			if (stopAfterResult) {
				stop();
			}
			return;
		}

		logDebug("Launching purchase flow for sea depth contours");
		if (mHelper != null) {
			try {
				mHelper.launchPurchaseFlow(activity,
						SKU_DEPTH_CONTOURS, RC_REQUEST, mPurchaseFinishedListener);
			} catch (Exception e) {
				complain("Cannot launch depth contours purchase!");
				logError("purchaseDepthContours Error", e);
				if (stopAfterResult) {
					stop();
				}
			}
		}
	}

	public void purchaseLiveUpdates(final Activity activity, final String email, final String userName,
									final String countryDownloadName, final boolean hideUserName) {
		try {
			if (mHelper == null || !mHelper.subscriptionsSupported()) {
				complain("Subscriptions not supported on your device yet. Sorry!");
				notifyError("Subscriptions not supported on your device yet. Sorry!");
				if (stopAfterResult) {
					stop();
				}
				return;
			}
		} catch (Exception e) {
			logError("purchaseLiveUpdates Error", e);
			if (stopAfterResult) {
				stop();
			}
			return;
		}

		notifyShowProgress();

		new AsyncTask<Void, Void, String>() {

			private String userId;

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
							"http://download.osmand.net/subscription/register.php",
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
					notifyDismissProgress();
					notifyError("Cannot retrieve userId from server.");
					if (stopAfterResult) {
						stop();
					}
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
						notifyDismissProgress();
						notifyError(message);
						if (stopAfterResult) {
							stop();
						}
					}
				}

				notifyDismissProgress();
				if (!Algorithms.isEmpty(userId)) {
					logDebug("Launching purchase flow for live updates subscription for userId=" + userId);
					String payload = userId + " " + token;
					if (mHelper != null) {
						try {
							mHelper.launchPurchaseFlow(activity,
									SKU_LIVE_UPDATES, IabHelper.ITEM_TYPE_SUBS,
									RC_REQUEST, mPurchaseFinishedListener, payload);
						} catch (Exception e) {
							logError("launchPurchaseFlow Error", e);
							if (stopAfterResult) {
								stop();
							}
						}
					}
				} else {
					notifyError("Empty userId");
					if (stopAfterResult) {
						stop();
					}
				}
			}
		}.execute((Void) null);
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
			if (mHelper == null) return;

			if (result.isFailure()) {
				complain("Error purchasing: " + result);
				notifyDismissProgress();
				notifyError("Error purchasing: " + result);
				if (stopAfterResult) {
					stop();
				}
				return;
			}

			logDebug("Purchase successful.");

			if (purchase.getSku().equals(SKU_LIVE_UPDATES)) {
				// bought live updates
				logDebug("Live updates subscription purchased.");
				sendToken(purchase.getToken(), new OnRequestResultListener() {
					@Override
					public void onResult(String result) {
						showToast(ctx.getString(R.string.osm_live_thanks));
						mSubscribedToLiveUpdates = true;
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);

						notifyDismissProgress();
						notifyItemPurchased(SKU_LIVE_UPDATES);
						if (stopAfterResult) {
							stop();
						}
					}
				});
			}
			if (purchase.getSku().equals(SKU_FULL_VERSION_PRICE)) {
				// bought full version
				logDebug("Full version purchased.");
				showToast(ctx.getString(R.string.full_version_thanks));
				mFullVersionPurchased = true;
				ctx.getSettings().FULL_VERSION_PURCHASED.set(true);

				notifyDismissProgress();
				notifyItemPurchased(SKU_FULL_VERSION_PRICE);
				if (stopAfterResult) {
					stop();
				}
			}
			if (purchase.getSku().equals(SKU_DEPTH_CONTOURS)) {
				// bought sea depth contours
				logDebug("Sea depth contours purchased.");
				showToast(ctx.getString(R.string.sea_depth_thanks));
				mDepthContoursPurchased = true;
				ctx.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
				ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);

				notifyDismissProgress();
				notifyItemPurchased(SKU_DEPTH_CONTOURS);
				if (stopAfterResult) {
					stop();
				}
			}
		}
	};

	// Do not forget call stop() when helper is not needed anymore
	public void stop() {
		logDebug("Destroying helper.");
		if (mHelper != null) {
			if (!inventoryRequesting) {
				stopRequested = false;
				mHelper.dispose();
				mHelper = null;
			} else {
				stopRequested = true;
			}
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
					"http://download.osmand.net/subscription/purchased.php",
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

	private void notifyError(String message) {
		for (InAppListener l : listeners) {
			l.onError(message);
		}
	}

	private void notifyGetItems() {
		for (InAppListener l : listeners) {
			l.onGetItems();
		}
	}

	private void notifyItemPurchased(String sku) {
		for (InAppListener l : listeners) {
			l.onItemPurchased(sku);
		}
	}

	private void notifyShowProgress() {
		for (InAppListener l : listeners) {
			l.showProgress();
		}
	}

	private void notifyDismissProgress() {
		for (InAppListener l : listeners) {
			l.dismissProgress();
		}
	}

	public void addListener(InAppListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(InAppListener listener) {
		this.listeners.remove(listener);
	}

	private void complain(String message) {
		logError("**** InAppHelper Error: " + message);
		showToast(message);
	}

	private void showToast(final String message) {
		ctx.showToastMessage(message);
	}

	private void logDebug(String msg) {
		if (mDebugLog) Log.d(TAG, msg);
	}

	private void logError(String msg) {
		Log.e(TAG, msg);
	}

	private void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

}
