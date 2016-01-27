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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InAppHelper {
	// Debug tag, for logging
	static final String TAG = "InAppHelper";

	private static boolean mSubscribedToLiveUpdates = false;
	private static String mLiveUpdatesPrice;

	private static final String SKU_LIVE_UPDATES_FULL = "osm_live_subscription_2";
	private static final String SKU_LIVE_UPDATES_FREE = "osm_free_live_subscription_2";
	private static String SKU_LIVE_UPDATES;

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

		void showProgress();

		void dismissProgress();
	}

	public static boolean isSubscribedToLiveUpdates() {
		return mSubscribedToLiveUpdates;
	}

	public static String getLiveUpdatesPrice() {
		return mLiveUpdatesPrice;
	}

	public static String getSkuLiveUpdates() {
		return SKU_LIVE_UPDATES;
	}

	public InAppHelper(OsmandApplication ctx, InAppCallbacks callbacks) {
		this.ctx = ctx;
		this.callbacks = callbacks;
		if (SKU_LIVE_UPDATES == null) {
			if (Version.isFreeVersion(ctx)) {
				SKU_LIVE_UPDATES = SKU_LIVE_UPDATES_FREE;
			} else {
				SKU_LIVE_UPDATES = SKU_LIVE_UPDATES_FULL;
			}
		}
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
		mHelper.enableDebugLogging(false);

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
				List<String> skus = new ArrayList<>();
				skus.add(SKU_LIVE_UPDATES);
				mHelper.queryInventoryAsync(true, skus, mGotInventoryListener);
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
			mSubscribedToLiveUpdates = (liveUpdatesPurchase != null);
			Log.d(TAG, "User " + (mSubscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE")
					+ " live updates purchased.");

			if (inventory.hasDetails(SKU_LIVE_UPDATES)) {
				SkuDetails liveUpdatesDetails = inventory.getSkuDetails(SKU_LIVE_UPDATES);
				mLiveUpdatesPrice = liveUpdatesDetails.getPrice();
			}

			if (liveUpdatesPurchase != null && !ctx.getSettings().BILLING_PURCHASE_TOKEN_SENT.get()) {
				sendToken(liveUpdatesPurchase.getToken());
			}

			if (callbacks != null) {
				callbacks.dismissProgress();
				callbacks.onGetItems();
			}
			if (stopAfterResult) {
				stop();
			}

			Log.d(TAG, "Initial inapp query finished");
		}
	};

	public void purchaseLiveUpdates(final Activity activity, final String email, final String userName,
									final String countryDownloadName) {
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
			callbacks.showProgress();
		}

		new AsyncTask<Void, Void, String>() {

			private String userId;

			@Override
			protected String doInBackground(Void... params) {
				userId = ctx.getSettings().BILLING_USER_ID.get();
				if (Algorithms.isEmpty(userId)) {
					try {
						Map<String, String> parameters = new HashMap<>();
						parameters.put("visibleName", userName);
						parameters.put("preferredCountry", countryDownloadName);
						parameters.put("email", email);
						parameters.put("status", "new");

						return sendRequest("http://download.osmand.net/subscription/register.php",
								parameters, "Requesting userId...");

					} catch (Exception e) {
						Log.e(TAG, "sendRequest Error", e);
						return null;
					}
				} else {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (Algorithms.isEmpty(userId)) {
					Log.d(TAG, "Response=" + response);
					if (response == null) {
						complain("Cannot retrieve userId from server.");
						if (callbacks != null) {
							callbacks.dismissProgress();
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
							String message = "JSON parsing error: "
									+ (e.getMessage() == null ? "unknown" : e.getMessage());
							complain(message);
							if (callbacks != null) {
								callbacks.dismissProgress();
								callbacks.onError(message);
							}
							if (stopAfterResult) {
								stop();
							}
						}
					}
				}

				if (callbacks != null) {
					callbacks.dismissProgress();
				}
				if (!Algorithms.isEmpty(userId)) {
					Log.d(TAG, "Launching purchase flow for live updates subscription for userId=" + userId);
					String payload = userId;
					if (mHelper != null) {
						mHelper.launchPurchaseFlow(activity,
								SKU_LIVE_UPDATES, IabHelper.ITEM_TYPE_SUBS,
								RC_REQUEST, mPurchaseFinishedListener, payload);
					}
				} else {
					if (callbacks != null) {
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
		} else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
			return true;
		}
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
					callbacks.dismissProgress();
					callbacks.onError("Error purchasing: " + result);
				}
				if (stopAfterResult) {
					stop();
				}
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(SKU_LIVE_UPDATES)) {
				// bought live updates
				sendToken(purchase.getToken());

				Log.d(TAG, "Live updates subscription purchased.");
				showToast("Thank you for subscribing to live updates!");
				mSubscribedToLiveUpdates = true;
				if (callbacks != null) {
					callbacks.dismissProgress();
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

	private void sendToken(String token) {
		String userId = ctx.getSettings().BILLING_USER_ID.get();
		String email = ctx.getSettings().BILLING_USER_EMAIL.get();
		try {
			Map<String, String> parameters = new HashMap<>();
			parameters.put("userId", userId);
			parameters.put("sku", SKU_LIVE_UPDATES);
			parameters.put("purchaseToken", token);
			parameters.put("email", email);

			sendRequestAsync("http://download.osmand.net/subscription/purchased.php",
					parameters, "Sending purchase info...", new OnRequestResultListener() {
						@Override
						public void onResult(String result) {
							if (result != null && result.trim().toLowerCase().equals("ok")) {
								ctx.getSettings().BILLING_PURCHASE_TOKEN_SENT.set(true);
							}
						}
					});
		} catch (Exception e) {
			Log.e(TAG, "sendToken Error", e);
		}
	}

	private void complain(String message) {
		Log.e(TAG, "**** InAppHelper Error: " + message);
		showToast("Error: " + message);
	}

	private void showToast(final String message) {
		ctx.showToastMessage(message);
	}

	private String sendRequest(String url, Map<String, String> parameters, String userOperation) {
		Log.d(TAG, "Sending request " + url);
		HttpURLConnection connection = null;
		try {
			connection = NetworkUtils.getHttpURLConnection(url);

			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx));
			connection.setConnectTimeout(15000);

			if (parameters != null && parameters.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					if (sb.length() > 0) {
						sb.append("&");
					}
					sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				}
				String params = sb.toString();

				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestMethod("POST");

				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				connection.setRequestProperty("Content-Length", String.valueOf(params.getBytes("UTF-8").length));
				connection.setFixedLengthStreamingMode(params.getBytes("UTF-8").length);

				OutputStream output = new BufferedOutputStream(connection.getOutputStream());
				output.write(params.getBytes("UTF-8"));
				output.flush();
				output.close();

			} else {
				connection.setRequestMethod("GET");
				connection.connect();
			}

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				String msg = userOperation
						+ " " + ctx.getString(R.string.failed_op) + " : " + connection.getResponseMessage();
				Log.e(TAG, msg);
				showToast(msg);
			} else {
				Log.d(TAG, "Response : " + connection.getResponseMessage());
				// populate return fields.
				StringBuilder responseBody = new StringBuilder();
				responseBody.setLength(0);
				InputStream i = connection.getInputStream();
				if (i != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256);
					String s;
					boolean f = true;
					while ((s = in.readLine()) != null) {
						if (!f) {
							responseBody.append("\n");
						} else {
							f = false;
						}
						responseBody.append(s);
					}
					try {
						in.close();
						i.close();
					} catch (Exception e) {
						Log.e(TAG, "sendRequest", e);
					}
				}
				return responseBody.toString();
			}

		} catch (NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			String msg = ctx.getString(R.string.auth_failed);
			Log.e(TAG, msg, e);
			showToast(msg);
		} catch (MalformedURLException e) {
			Log.e(TAG, userOperation + " " + ctx.getString(R.string.failed_op), e);
			showToast(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
		} catch (IOException e) {
			Log.e(TAG, userOperation + " " + ctx.getString(R.string.failed_op), e);
			showToast(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_io_error), userOperation));
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return null;
	}

	private void sendRequestAsync(final String url, final Map<String, String> parameters, final String userOperation, final OnRequestResultListener listener) {

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					return sendRequest(url, parameters, userOperation);
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (listener != null) {
					listener.onResult(response);
				}
			}

		}.execute((Void) null);
	}

	private interface OnRequestResultListener {
		void onResult(String result);
	}
}
