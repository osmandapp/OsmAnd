package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.util.IabHelper;
import net.osmand.plus.inapp.util.IabResult;
import net.osmand.plus.inapp.util.Inventory;
import net.osmand.plus.inapp.util.Purchase;
import net.osmand.plus.inapp.util.SkuDetails;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.MessageFormat;

public class InAppHelper {
	// Debug tag, for logging
	static final String TAG = "InAppHelper";

	private static boolean mSubscribedToLiveUpdates = false;
	private static String mLiveUpdatesPrice;

	public static final String SKU_LIVE_UPDATES = "osm_live_subscription_1";

	// Static test
	//public static final String SKU_LIVE_UPDATES = "android.test.purchased";
	//public static final String SKU_LIVE_UPDATES = "android.test.canceled";
	//public static final String SKU_LIVE_UPDATES = "android.test.refunded";
	//public static final String SKU_LIVE_UPDATES = "android.test.item_unavailable";


	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	// The helper object
	private IabHelper mHelper;
	private boolean stopAfterResult = false;

	private OsmandApplication ctx;
	private InAppCallbacks callbacks;

	public interface InAppCallbacks {
		void onError(String error);
		void onGetItems();
		void onItemPurchased(String sku);

		void showHideProgress(boolean show);
	}

	public static boolean isSubscribedToLiveUpdates() {
		return mSubscribedToLiveUpdates;
	}

	public static String getLiveUpdatesPrice() {
		return mLiveUpdatesPrice;
	}

	public InAppHelper(OsmandApplication ctx, InAppCallbacks callbacks) {
		this.ctx = ctx;
		this.callbacks = callbacks;
	}

	public void start(final boolean stopAfterResult) {
		this.stopAfterResult = stopAfterResult;
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
		String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgk8cEx" +
				"UO4mfEwWFLkQnX1Tkzehr4SnXLXcm2Osxs5FTJPEgyTckTh0POKVMrxeGLn0KoTY2NTgp1U/inp" +
				"wccWisPhVPEmw9bAVvWsOkzlyg1kv03fJdnAXRBSqDDPV6X8Z3MtkPVqZkupBsxyIllEILKHK06" +
				"OCw49JLTsMR3oTRifGzma79I71X0spw0fM+cIRlkS2tsXN8GPbdkJwHofZKPOXS51pgC1zU8uWX" +
				"I+ftJO46a1XkNh1dO2anUiQ8P/H4yOTqnMsXF7biyYuiwjXPOcy0OMhEHi54Dq6Mr3u5ZALOAkc" +
				"YTjh1H/ZgqIHy5ZluahINuDE76qdLYMXrDMQIDAQAB";

		// Create the helper, passing it our context and the public key to verify signatures with
		Log.d(TAG, "Creating InAppHelper.");
		mHelper = new IabHelper(ctx, base64EncodedPublicKey);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(true);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					complain("Problem setting up in-app billing: " + result);
					if (callbacks != null) {
						callbacks.onError(result.getMessage());
					}
					if (stopAfterResult) {
						stop();
					}
					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null) return;

				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
	}

	// Listener that's called when we finish querying the items and subscriptions we own
	private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null) return;

			// Is it a failure?
			if (result.isFailure()) {
				complain("Failed to query inventory: " + result);
				if (callbacks != null) {
					callbacks.onError(result.getMessage());
				}
				if (stopAfterResult) {
					stop();
				}
				return;
			}

			Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

			// Do we have the live updates?
			Purchase liveUpdatesPurchase = inventory.getPurchase(SKU_LIVE_UPDATES);
			mSubscribedToLiveUpdates = (liveUpdatesPurchase != null &&
					verifyDeveloperPayload(liveUpdatesPurchase));
			Log.d(TAG, "User " + (mSubscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE")
					+ " live updates purchased.");

			if (inventory.hasDetails(SKU_LIVE_UPDATES)) {
				SkuDetails liveUpdatesDetails = inventory.getSkuDetails(SKU_LIVE_UPDATES);
				mLiveUpdatesPrice = liveUpdatesDetails.getPrice();
			}

			if (callbacks != null) {
				callbacks.showHideProgress(false);
				callbacks.onGetItems();
			}
			if (stopAfterResult) {
				stop();
			}

			Log.d(TAG, "Initial inapp query finished");
		}
	};

	public void purchaseLiveUpdates(final Activity activity, final String email, final String userName,
									final String country) {
		if (!mHelper.subscriptionsSupported()) {
			complain("Subscriptions not supported on your device yet. Sorry!");
			if (callbacks != null) {
				callbacks.onError("Subscriptions not supported on your device yet. Sorry!");
			}
			if (stopAfterResult) {
				stop();
			}
			return;
		}

		if (callbacks != null) {
			callbacks.showHideProgress(true);
		}

		new AsyncTask<Void, Void, String>() {

			private String userId;

			@Override
			protected String doInBackground(Void... params) {
				userId = ctx.getSettings().BILLING_USER_ID.get();
				if (Algorithms.isEmpty(userId)) {
					return sendRequest("http://download.osmand.net/subscription/register?email=" + email
									+ "&visibleName=" + userName + "&preferredCountry=" + country
									+ (Algorithms.isEmpty(userId) ? "&status=new" : ""),
							"POST", "Requesting userId...");
				} else {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (Algorithms.isEmpty(userId)) {
					Log.d(TAG, "Response=" + response);
					if (response == null) {
						if (callbacks != null) {
							callbacks.showHideProgress(false);
							callbacks.onError("Cannot retrieve userId from server.");
						}
						if (stopAfterResult) {
							stop();
						}
						return;

					} else {
						try {
							JSONObject obj = new JSONObject(response);
							userId = obj.getString("userid");
							ctx.getSettings().BILLING_USER_ID.set(userId);
							Log.d(TAG, "UserId=" + userId);
						} catch (JSONException e) {
							if (callbacks != null) {
								callbacks.showHideProgress(false);
								callbacks.onError("JSON parsing error: " + e.getMessage());
							}
							if (stopAfterResult) {
								stop();
							}
						}
					}
				}

				if (!Algorithms.isEmpty(userId)) {
					Log.d(TAG, "Launching purchase flow for live updates subscription for userId=" + userId);
					String payload = userId;
					mHelper.launchPurchaseFlow(activity,
							SKU_LIVE_UPDATES, IabHelper.ITEM_TYPE_SUBS,
							RC_REQUEST, mPurchaseFinishedListener, payload);
				} else {
					if (callbacks != null) {
						callbacks.showHideProgress(false);
						callbacks.onError("Empty userId");
					}
					if (stopAfterResult) {
						stop();
					}
				}
			}
		}.execute((Void) null);
	}

	public boolean onActivityResultHandled(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
		if (mHelper == null) return false;

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			//super.onActivityResult(requestCode, resultCode, data);
			return false;
		}
		else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
			return true;
		}
	}

	/** Verifies the developer payload of a purchase. */
	private boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

		return true;
	}

	// Callback for when a purchase is finished
	private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

			// if we were disposed of in the meantime, quit.
			if (mHelper == null) return;

			if (result.isFailure()) {
				complain("Error purchasing: " + result);
				if (callbacks != null) {
					callbacks.showHideProgress(false);
					callbacks.onError("Error purchasing: " + result);
				}
				if (stopAfterResult) {
					stop();
				}
				return;
			}
			if (!verifyDeveloperPayload(purchase)) {
				complain("Error purchasing. Authenticity verification failed.");
				if (callbacks != null) {
					callbacks.showHideProgress(false);
					callbacks.onError("Error purchasing. Authenticity verification failed.");
				}
				if (stopAfterResult) {
					stop();
				}
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(SKU_LIVE_UPDATES)) {
				// bought the infinite gas subscription
				Log.d(TAG, "Live updates subscription purchased.");
				showToast("Thank you for subscribing to live updates!");
				mSubscribedToLiveUpdates = true;
				if (callbacks != null) {
					callbacks.showHideProgress(false);
					callbacks.onItemPurchased(SKU_LIVE_UPDATES);
				}
				if (stopAfterResult) {
					stop();
				}
			}
		}
	};

	// Do not forget call stop() when helper is not needed anymore
	public void stop() {
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}

	void complain(String message) {
		Log.e(TAG, "**** InAppHelper Error: " + message);
		showToast("Error: " + message);
	}

	void showToast(final String message) {
		ctx.showToastMessage(message);
	}

	private String sendRequest(String url, String requestMethod, String userOperation) {
		Log.d(TAG, "Sending request " + url); //$NON-NLS-1$
		try {
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);

			connection.setConnectTimeout(15000);
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx)); //$NON-NLS-1$
			StringBuilder responseBody = new StringBuilder();
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				String msg = userOperation
						+ " " + ctx.getString(R.string.failed_op) + " : " + connection.getResponseMessage(); //$NON-NLS-1$//$NON-NLS-2$
				Log.e(TAG, msg);
				showToast(msg);
			} else {
				Log.d(TAG, "Response : " + connection.getResponseMessage()); //$NON-NLS-1$
				// populate return fields.
				responseBody.setLength(0);
				InputStream i = connection.getInputStream();
				if (i != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256); //$NON-NLS-1$
					String s;
					boolean f = true;
					while ((s = in.readLine()) != null) {
						if (!f) {
							responseBody.append("\n"); //$NON-NLS-1$
						} else {
							f = false;
						}
						responseBody.append(s);
					}
					try {
						in.close();
						i.close();
					} catch (Exception e) {
						Log.d(TAG, e.getMessage());
					}
				}
				return responseBody.toString();
			}
			connection.disconnect();
		} catch (NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			String msg = ctx.getString(R.string.auth_failed);
			Log.e(TAG, msg, e);
			showToast(msg);
		} catch (MalformedURLException e) {
			Log.e(TAG, userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showToast(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
		} catch (IOException e) {
			Log.e(TAG, userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showToast(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_io_error), userOperation));
		}

		return null;
	}
}
