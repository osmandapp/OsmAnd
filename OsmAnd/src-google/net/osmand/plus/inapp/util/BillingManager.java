package net.osmand.plus.inapp.util;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import net.osmand.PlatformUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
	// Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
	public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(BillingManager.class);
	private static final String TAG = "BillingManager";

	/**
	 * A reference to BillingClient
	 **/
	private BillingClient mBillingClient;

	/**
	 * True if billing service is connected now.
	 */
	private boolean mIsServiceConnected;

	private final Context mContext;

	// Public key for verifying signature, in base64 encoding
	private final String mSignatureBase64;

	private String mObfuscatedAccountId;
	private String mObfuscatedProfileId;

	private final BillingUpdatesListener mBillingUpdatesListener;
	private final List<Purchase> mPurchases = new ArrayList<>();
	private Set<String> mTokensToBeConsumed;

	private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;
	private String mBillingClientResponseMessage;


	/**
	 * Listener to the updates that happen when purchases list was updated or consumption of the
	 * item was finished
	 */
	public interface BillingUpdatesListener {
		void onBillingClientSetupFinished();

		void onConsumeFinished(String token, BillingResult billingResult);

		void onPurchasesUpdated(List<Purchase> purchases);

		void onPurchaseCanceled();
	}

	/**
	 * Listener for query purshases
	 */
	public interface QueryPurchasesListener {
		void onQueryPurchasesFinished();
	}

	/**
	 * Listener for the Billing client state to become connected
	 */
	public interface ServiceConnectedListener {
		void onServiceConnected(BillingResult billingResult);
	}

	public BillingManager(@NonNull Context context, @NonNull String base64PublicKey,
	                      @NonNull BillingUpdatesListener updatesListener) {
		LOG.debug("Creating Billing client.");
		mContext = context;
		mSignatureBase64 = base64PublicKey;
		mBillingUpdatesListener = updatesListener;
		mBillingClient = BillingClient.newBuilder(mContext)
				.enablePendingPurchases()
				.setListener(this)
				.build();

		LOG.debug("Starting setup.");

		// Start setup. This is asynchronous and the specified listener will be called
		// once setup completes.
		// It also starts to report all the new purchases through onPurchasesUpdated() callback.
		startServiceConnection(null);
	}

	/**
	 * Handle a callback that purchases were updated from the Billing library
	 */
	@Override
	public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
		int responseCode = billingResult.getResponseCode();
		if (responseCode == BillingResponseCode.OK) {
			if (purchases != null) {
				for (Purchase purchase : purchases) {
					handlePurchase(purchase);
				}
			} else {
				LOG.info("onPurchasesUpdated() - no purchases");
			}
			mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
		} else if (responseCode == BillingResponseCode.USER_CANCELED) {
			LOG.info("onPurchasesUpdated() - user cancelled the purchase flow - skipping");
			mBillingUpdatesListener.onPurchaseCanceled();
		} else {
			LOG.warn("onPurchasesUpdated() got unknown responseCode: " + responseCode);
		}
	}

	/**
	 * Start a purchase flow
	 */
	public void initiatePurchaseFlow(Activity activity, ProductDetails productDetails, int selectedOfferIndex) {
		initiatePurchaseFlow(activity, productDetails, null, null, selectedOfferIndex);
	}

	/**
	 * Start a purchase or subscription replace flow
	 */
	public void initiatePurchaseFlow(Activity activity, ProductDetails productDetails, String oldProductId, String purchaseToken, int selectedOfferIndex) {
		Runnable purchaseFlowRequest = () -> {
			LOG.debug("Launching in-app purchase flow. Replace old ProductId? " + (oldProductId != null && purchaseToken != null));
			String offerToken = null;
			List<ProductDetails.SubscriptionOfferDetails> offerDetails = productDetails
					.getSubscriptionOfferDetails();
			if (offerDetails != null) {
				offerToken = offerDetails.get(selectedOfferIndex).getOfferToken();
			}
			ArrayList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
			BillingFlowParams.ProductDetailsParams.Builder productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
					.setProductDetails(productDetails);
			if (offerToken != null) {
				productDetailsParams.setOfferToken(offerToken);
			}
			productDetailsParamsList.add(productDetailsParams.build());
			BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder()
					.setProductDetailsParamsList(productDetailsParamsList);
			if (!TextUtils.isEmpty(mObfuscatedAccountId)) {
				billingFlowParams.setObfuscatedAccountId(mObfuscatedAccountId);
			}
			if (!TextUtils.isEmpty(mObfuscatedProfileId)) {
				billingFlowParams.setObfuscatedProfileId(mObfuscatedProfileId);
			}
			if (oldProductId != null && purchaseToken != null) {
				SubscriptionUpdateParams.Builder updateParamsBuilder = SubscriptionUpdateParams.newBuilder();
				updateParamsBuilder.setOldPurchaseToken(purchaseToken);
				billingFlowParams.setSubscriptionUpdateParams(updateParamsBuilder.build());
			}
			mBillingClient.launchBillingFlow(activity, billingFlowParams.build());
		};

		executeServiceRequest(purchaseFlowRequest);
	}

	public Context getContext() {
		return mContext;
	}

	/**
	 * Clear the resources
	 */
	public void destroy() {
		LOG.debug("Destroying the manager.");

		if (mBillingClient != null && mBillingClient.isReady()) {
			mBillingClient.endConnection();
			mBillingClient = null;
		}
	}

	public void queryProductDetailsAsync(@ProductType String itemType, List<String> productIdList,
	                                     ProductDetailsResponseListener listener) {
		// Creating a runnable from the request to use it inside our connection retry policy below
		Runnable queryRequest = () -> {
			// Query the purchase async

			ArrayList<QueryProductDetailsParams.Product> products = new ArrayList<>();
			for (String productId : productIdList) {
				QueryProductDetailsParams.Product pr = QueryProductDetailsParams.Product.newBuilder()
						.setProductType(itemType)
						.setProductId(productId).build();
				products.add(pr);
			}
			QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
					.setProductList(products)
					.build();
			mBillingClient.queryProductDetailsAsync(params, listener);
		};

		executeServiceRequest(queryRequest);
	}

	public void consumeAsync(ConsumeParams consumeParams) {
		// If we've already scheduled to consume this token - no action is needed (this could happen
		// if you received the token when querying purchases inside onReceive() and later from
		// onActivityResult()
		String purchaseToken = consumeParams.getPurchaseToken();
		if (mTokensToBeConsumed == null) {
			mTokensToBeConsumed = new HashSet<>();
		} else if (mTokensToBeConsumed.contains(purchaseToken)) {
			LOG.info("Token was already scheduled to be consumed - skipping...");
			return;
		}
		mTokensToBeConsumed.add(purchaseToken);

		// Generating Consume Response listener
		ConsumeResponseListener onConsumeListener = new ConsumeResponseListener() {
			@Override
			public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
				// If billing service was disconnected, we try to reconnect 1 time
				// (feel free to introduce your retry policy here).
				mBillingUpdatesListener.onConsumeFinished(purchaseToken, billingResult);
			}
		};

		// Creating a runnable from the request to use it inside our connection retry policy below
		Runnable consumeRequest = () -> {
			// Consume the purchase async
			mBillingClient.consumeAsync(consumeParams, onConsumeListener);
		};

		executeServiceRequest(consumeRequest);
	}

	public boolean isServiceConnected() {
		return mIsServiceConnected;
	}

	/**
	 * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
	 * client connection response was not received yet.
	 */
	public int getBillingClientResponseCode() {
		return mBillingClientResponseCode;
	}

	public String getBillingClientResponseMessage() {
		return mBillingClientResponseMessage;
	}

	public List<Purchase> getPurchases() {
		return Collections.unmodifiableList(mPurchases);
	}

	/**
	 * Handles the purchase
	 * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
	 * It's recommended to move this check into your backend.
	 * See {@link Security#verifyPurchase(String, String, String)}
	 * </p>
	 *
	 * @param purchase Purchase to be handled
	 */
	private void handlePurchase(Purchase purchase) {
		if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
			LOG.info("Got a purchase: " + purchase + ", but signature is bad. Skipping...");
			return;
		}

		if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
			// Acknowledge the purchase if it hasn't already been acknowledged.
			if (!purchase.isAcknowledged()) {
				AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchase.getPurchaseToken())
						.build();
				mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
					@Override
					public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
						if (billingResult.getResponseCode() != BillingResponseCode.OK) {
							LOG.info("Acknowledge a purchase: " + purchase + " failed (" + billingResult.getResponseCode() + "). " + billingResult.getDebugMessage());
						}
					}
				});
			}
		} else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
			LOG.info("Got a purchase: " + purchase + ", but purchase state is pending. Skipping...");
			return;
		} else {
			LOG.info("Got a purchase: " + purchase + ", but purchase state is " + purchase.getPurchaseState() + ". Skipping...");
			return;
		}

		LOG.debug("Got a verified purchase: " + purchase);

		mPurchases.add(purchase);
	}

	/**
	 * Handle a result from querying of purchases and report an updated list to the listener
	 */
	private void onQueryPurchasesFinished(BillingResult billingResult, @Nullable List<Purchase> purchases) {
		// Have we been disposed of in the meantime? If so, or bad result code, then quit
		if (mBillingClient == null || billingResult.getResponseCode() != BillingResponseCode.OK) {
			LOG.warn("Billing client was null or result code (" + billingResult.getResponseCode()
					+ ") was bad - quitting");
			return;
		}

		LOG.debug("Query inventory was successful.");

		// Update the UI and purchases inventory with new list of purchases
		mPurchases.clear();
		onPurchasesUpdated(billingResult, purchases);
	}

	/**
	 * Checks if subscriptions are supported for current client
	 * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
	 * It is only used in unit tests and after queryPurchases execution, which already has
	 * a retry-mechanism implemented.
	 * </p>
	 */
	public boolean areSubscriptionsSupported() {
		int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
		if (responseCode != BillingResponseCode.OK) {
			LOG.warn("areSubscriptionsSupported() got an error response: " + responseCode);
		}
		return responseCode == BillingResponseCode.OK;
	}

	/**
	 * Query purchases across various use cases and deliver the result in a formalized way through
	 * a listener
	 */
	public void queryPurchases(@Nullable QueryPurchasesListener queryPurchasesListener) {
		Runnable queryToExecute = () -> {
			long time = System.currentTimeMillis();
			QueryPurchasesParams purchasesParams = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build();
			mBillingClient.queryPurchasesAsync(purchasesParams, (billingResult, purchaseList) -> {
				LOG.info("Querying purchases elapsed time: " + (System.currentTimeMillis() - time) + "ms");

				// If there are subscriptions supported, we add subscription rows as well
				if (areSubscriptionsSupported()) {
					querySubscriptionPurchases(billingResult, purchaseList, time, queryPurchasesListener);
				} else {
					if (billingResult.getResponseCode() == BillingResponseCode.OK) {
						LOG.info("Skipped subscription purchases query since they are not supported");
					}
					onQueryPurchasesFinished(billingResult, purchaseList);
					if (queryPurchasesListener != null) {
						queryPurchasesListener.onQueryPurchasesFinished();
					}
				}
			});
		};
		executeServiceRequest(queryToExecute);
	}

	public void querySubscriptionPurchases(BillingResult billingResult, List<Purchase> purchaseList,
	                                       long time, @Nullable QueryPurchasesListener queryPurchasesListener) {
		QueryPurchasesParams purchasesParams = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build();
		mBillingClient.queryPurchasesAsync(purchasesParams, new PurchasesResponseListener() {
			@Override
			public void onQueryPurchasesResponse(@NonNull BillingResult result, @NonNull List<Purchase> purchases) {
				LOG.info("Querying purchases and subscriptions elapsed time: "
						+ (System.currentTimeMillis() - time) + "ms");
				LOG.info("Querying subscriptions result code: "
						+ result.getResponseCode() + " res: " + purchases.size());

				if (result.getResponseCode() == BillingResponseCode.OK) {
					purchaseList.addAll(purchases);
				} else {
					LOG.error("Got an error response trying to query subscription purchases");
				}
				onQueryPurchasesFinished(billingResult, purchaseList);
				if (queryPurchasesListener != null) {
					queryPurchasesListener.onQueryPurchasesFinished();
				}
			}
		});
	}

	public void startServiceConnection(Runnable executeOnSuccess) {
		mBillingClient.startConnection(new BillingClientStateListener() {
			@Override
			public void onBillingSetupFinished(BillingResult billingResult) {

				int billingResponseCode = billingResult.getResponseCode();
				LOG.debug("Setup finished. Response code: " + billingResponseCode);

				mIsServiceConnected = billingResponseCode == BillingResponseCode.OK;
				mBillingClientResponseCode = billingResponseCode;
				mBillingClientResponseMessage = billingResult.getDebugMessage();
				mBillingUpdatesListener.onBillingClientSetupFinished();

				if (mIsServiceConnected) {
					if (executeOnSuccess != null) {
						executeOnSuccess.run();
					}
				}
			}

			@Override
			public void onBillingServiceDisconnected() {
				mIsServiceConnected = false;
				mBillingUpdatesListener.onBillingClientSetupFinished();
			}
		});
	}

	public void setObfuscatedAccountId(String obfuscatedAccountId) {
		mObfuscatedAccountId = obfuscatedAccountId;
	}

	public void setObfuscatedProfileId(String obfuscatedProfileId) {
		mObfuscatedProfileId = obfuscatedProfileId;
	}

	private void executeServiceRequest(Runnable runnable) {
		if (mIsServiceConnected) {
			runnable.run();
		} else {
			// If billing service was disconnected, we try to reconnect 1 time.
			startServiceConnection(runnable);
		}
	}

	private boolean verifyValidSignature(String signedData, String signature) {
		return Security.verifyPurchase(mSignatureBase64, signedData, signature);
	}
}