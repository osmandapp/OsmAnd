package net.osmand.plus.inapp.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.amazon.device.iap.model.UserDataResponse.RequestStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link PurchasingListener} that listens to Amazon
 * InAppPurchase SDK's events, and call {@link IapManager} to handle the
 * purchase business logic.
 */
public class IapPurchasingListener implements PurchasingListener {

	private static final String TAG = "InAppPurchasingListener";

	private final IapManager iapManager;
	private final List<PurchaseResponseListener> responseListeners = new ArrayList<>();

	public interface PurchaseResponseListener {
		void onUserDataResponse(@Nullable UserIapData userData);
		void onProductDataResponse(@Nullable Map<String, Product> productMap);
		void onPurchaseUpdatesResponse(@Nullable Map<UserData, List<Receipt>> purchaseMap, boolean hasMore);
		void onPurchaseResponse(@Nullable PurchaseResponse response);
	}

	public IapPurchasingListener(final @NonNull IapManager iapManager) {
		this.iapManager = iapManager;
	}

	public void addResponseListener(@NonNull PurchaseResponseListener listener) {
		if (!responseListeners.contains(listener)) {
			responseListeners.add(listener);
		}
	}

	public void removeResponseListener(@NonNull PurchaseResponseListener listener) {
		responseListeners.remove(listener);
	}

	private void notifyUserDataResponseListeners(@Nullable UserIapData userData) {
		for (PurchaseResponseListener listener : responseListeners) {
			listener.onUserDataResponse(userData);
		}
	}

	private void notifyProductDataResponseListeners(@Nullable Map<String, Product> productMap) {
		for (PurchaseResponseListener listener : responseListeners) {
			listener.onProductDataResponse(productMap);
		}
	}

	private void notifyPurchaseUpdatesResponseListeners(@Nullable Map<UserData, List<Receipt>> purchaseMap, boolean hasMore) {
		for (PurchaseResponseListener listener : responseListeners) {
			listener.onPurchaseUpdatesResponse(purchaseMap, hasMore);
		}
	}

	private void notifyPurchaseResponseListeners(@Nullable PurchaseResponse response) {
		for (PurchaseResponseListener listener : responseListeners) {
			listener.onPurchaseResponse(response);
		}
	}

	/**
	 * This is the callback for {@link PurchasingService#getUserData}. For
	 * successful case, get the current user from {@link UserDataResponse} and
	 * call {@link IapManager#setAmazonUserId} method to load the Amazon
	 * user and related purchase information
	 *
	 * @param response - Response object
	 */
	@Override
	public void onUserDataResponse(final UserDataResponse response) {
		Log.d(TAG, "onGetUserDataResponse: requestId (" + response.getRequestId()
				+ ") userIdRequestStatus: " + response.getRequestStatus() + ")");

		final RequestStatus status = response.getRequestStatus();
		switch (status) {
			case SUCCESSFUL:
				Log.d(TAG, "onUserDataResponse: get user id (" + response.getUserData().getUserId()
						+ ", marketplace (" + response.getUserData().getMarketplace() + ") ");
				UserIapData userData = iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
				notifyUserDataResponseListeners(userData);
				break;

			case FAILED:
			case NOT_SUPPORTED:
				Log.d(TAG, "onUserDataResponse failed, status code is " + status);
				iapManager.setAmazonUserId(null, null);
				notifyUserDataResponseListeners(null);
				break;
		}
	}

	/**
	 * This is the callback for {@link PurchasingService#getProductData}.
	 */
	@Override
	public void onProductDataResponse(final ProductDataResponse response) {
		final ProductDataResponse.RequestStatus status = response.getRequestStatus();
		Log.d(TAG, "onProductDataResponse: RequestStatus (" + status + ")");

		switch (status) {
			case SUCCESSFUL:
				Log.d(TAG, "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
				final Set<String> unavailableSkus = response.getUnavailableSkus();
				Log.d(TAG, "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
				notifyProductDataResponseListeners(response.getProductData());
				break;
			case FAILED:
			case NOT_SUPPORTED:
				Log.d(TAG, "onProductDataResponse: failed, should retry request");
				notifyProductDataResponseListeners(null);
				break;
		}
	}

	/**
	 * This is the callback for {@link PurchasingService#getPurchaseUpdates}.
	 * <p>
	 * You will receive receipts for all possible Subscription history from this
	 * callback
	 */
	@Override
	public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
		Log.d(TAG, "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
				+ ") purchaseUpdatesResponseStatus (" + response.getRequestStatus()
				+ ") userId (" + response.getUserData().getUserId() + ")");
		final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
		switch (status) {
			case SUCCESSFUL:
				notifyPurchaseUpdatesResponseListeners(Collections.singletonMap(response.getUserData(), response.getReceipts()), response.hasMore());
				//iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
				if (response.hasMore()) {
					PurchasingService.getPurchaseUpdates(false);
				}
				break;
			case FAILED:
			case NOT_SUPPORTED:
				Log.d(TAG, "onProductDataResponse: failed, should retry request");
				notifyPurchaseUpdatesResponseListeners(null, false);
				break;
		}

	}

	/**
	 * This is the callback for {@link PurchasingService#purchase}. For each
	 * time the application sends a purchase request
	 * {@link PurchasingService#purchase}, Amazon Appstore will call this
	 * callback when the purchase request is completed.
	 */
	@Override
	public void onPurchaseResponse(final PurchaseResponse response) {
		final String requestId = response.getRequestId().toString();
		final String userId = response.getUserData().getUserId();
		final PurchaseResponse.RequestStatus status = response.getRequestStatus();
		Log.d(TAG, "onPurchaseResponse: requestId (" + requestId
				+ ") userId (" + userId + ") purchaseRequestStatus (" + status + ")");
		switch (status) {
			case SUCCESSFUL:
				final Receipt receipt = response.getReceipt();
				Log.d(TAG, "onPurchaseResponse: receipt json:" + receipt.toJSON());
				PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
				notifyPurchaseResponseListeners(response);
				break;
			case ALREADY_PURCHASED:
				Log.i(TAG, "onPurchaseResponse: already purchased, you should verify the subscription " +
						"purchase on your side and make sure the purchase was granted to customer");
				notifyPurchaseResponseListeners(null);
				break;
			case INVALID_SKU:
				Log.d(TAG, "onPurchaseResponse: invalid SKU! onProductDataResponse should have disabled buy button already.");
				notifyPurchaseResponseListeners(null);
				break;
			case FAILED:
			case NOT_SUPPORTED:
				Log.d(TAG, "onPurchaseResponse: failed so remove purchase request from local storage");
				notifyPurchaseResponseListeners(null);
				break;
		}
	}
}
