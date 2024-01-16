package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.PurchaseInfo;
import net.osmand.plus.inapp.util.IapManager;
import net.osmand.plus.inapp.util.IapPurchasingListener;
import net.osmand.plus.inapp.util.IapPurchasingListener.PurchaseResponseListener;
import net.osmand.plus.inapp.util.UserIapData;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnSendRequestsListener;
import net.osmand.plus.utils.AndroidNetworkUtils.Request;
import net.osmand.plus.utils.AndroidNetworkUtils.RequestResponse;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {

	private final IapPurchasingListener purchasingListener;

	private UserIapData userData;
	private Map<String, Product> productMap;
	private List<Receipt> receipts = new ArrayList<>();
	private Map<String, Receipt> subscriptionReceiptMap = new HashMap<>();

	public InAppPurchaseHelperImpl(OsmandApplication ctx) {
		super(ctx);
		purchases = new InAppPurchasesImpl(ctx);

		IapManager iapManager = new IapManager();
		purchasingListener = new IapPurchasingListener(iapManager);
		PurchasingService.registerListener(ctx, purchasingListener);
	}

	@Override
	public void isInAppPurchaseSupported(@NonNull Activity activity, @Nullable InAppPurchaseInitCallback callback) {
		if (callback != null) {
			callback.onSuccess();
		}
	}

	@Override
	protected void execImpl(@NonNull InAppPurchaseTaskType taskType, @NonNull InAppCommand command) {
		command.run(this);
	}

	@Override
	public void purchaseFullVersion(@NonNull Activity activity) throws UnsupportedOperationException {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
		InAppPurchase fullVersion = purchases.getFullVersion();
		if (fullVersion != null) {
			exec(InAppPurchaseTaskType.PURCHASE_FULL_VERSION, new PurchaseInAppCommand(new WeakReference<>(activity), fullVersion.getSku()));
		}
	}

	@Override
	public void purchaseDepthContours(@NonNull Activity activity) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Attempt to purchase depth contours (amazon build)");
	}

	@Override
	public void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Attempt to purchase contour lines (amazon build)");
	}

	@Override
	public void manageSubscription(@NonNull Context ctx, @Nullable String sku) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		app.showToastMessage(R.string.amz_manage_subscription_descr);
	}

	@Override
	protected boolean isUserInfoSupported() {
		return false;
	}

	@Override
	protected InAppCommand getPurchaseSubscriptionCommand(WeakReference<Activity> activity, String sku, String userInfo) throws UnsupportedOperationException {
		return new PurchaseInAppCommand(activity, sku);
	}

	@Override
	protected InAppCommand getRequestInventoryCommand(boolean userRequested) throws UnsupportedOperationException {
		return new RequestInventoryCommand(userRequested);
	}

	@Override
	protected boolean isBillingManagerExists() {
		return false;
	}

	@Override
	protected void destroyBillingManager() {
	}

	@Nullable
	private Product getProductInfo(@NonNull String productId) {
		Map<String, Product> productMap = this.productMap;
		if (productMap != null) {
			Collection<Product> products = productMap.values();
			for (Product product : products) {
				if (product.getSku().equals(productId)) {
					return product;
				}
			}
		}
		return null;
	}

	// Call when a purchase is finished
	private void onPurchaseFinished(@NonNull String sku, @NonNull PurchaseResponse response) {
		Receipt receipt = response.getReceipt();
		logDebug("Purchase finished: " + receipt.getSku());
		PurchaseInfo info = getPurchaseInfo(sku, receipt);
		UserData userData = response.getUserData();
		if (userData != null) {
			info.setPurchaseToken(userData.getUserId());
		} else {
			info.setPurchaseToken(getUserId());
		}
		onPurchaseDone(info);
	}

	private boolean hasDetails(@NonNull String productId) {
		return getProductInfo(productId) != null;
	}

	@NonNull
	private String getUserId() {
		UserIapData userData = this.userData;
		String userId = userData != null ? userData.getAmazonUserId() : null;
		return userId != null ? userId : "";
	}

	@Nullable
	private Receipt getReceipt(@NonNull String productId) {
		return subscriptionReceiptMap.get(productId);
	}

	private PurchaseInfo getPurchaseInfo(String sku, Receipt receipt) {
		return new PurchaseInfo(Collections.singletonList(sku), receipt.getReceiptId(), getUserId(),
				receipt.getPurchaseDate().getTime(), 0, true, !receipt.isCanceled());
	}

	private void fetchInAppPurchase(@NonNull InAppPurchase inAppPurchase, @NonNull Product product, @Nullable Receipt receipt) {
		if (receipt != null) {
			inAppPurchase.setPurchaseState(InAppPurchase.PurchaseState.PURCHASED);
			inAppPurchase.setPurchaseInfo(ctx, getPurchaseInfo(product.getSku(), receipt));
		} else {
			inAppPurchase.setPurchaseState(InAppPurchase.PurchaseState.NOT_PURCHASED);
			inAppPurchase.restorePurchaseInfo(ctx);
		}
		String price = product.getPrice();
		inAppPurchase.setPrice(price);

		double priceValue = 0;
		String countryCode = userData.getAmazonMarketplace();
		String currencyCode = !Algorithms.isEmpty(countryCode) ? Currency.getInstance(new Locale("", countryCode)).getCurrencyCode() : "";
		Pattern regex = Pattern.compile("\\d[\\d,.]+");
		Matcher finder = regex.matcher(price);
		if (finder.find()) {
			try {
				String rawPrice = finder.group(0);
				if (!Algorithms.isEmpty(rawPrice)) {
					priceValue = Double.parseDouble(rawPrice.trim().replaceAll(",", "."));
				}
				// do something with value
			} catch (Exception e) {
				priceValue = 0;
			}
		}
		inAppPurchase.setPriceCurrencyCode(currencyCode);
		if (priceValue > 0) {
			inAppPurchase.setPriceValue(priceValue);
		}
		if (inAppPurchase instanceof InAppSubscription) {
			InAppSubscription s = (InAppSubscription) inAppPurchase;
			s.restoreState(ctx);
			s.restoreExpireTime(ctx);
			SubscriptionStateHolder stateHolder = subscriptionStateMap.get(s.getSku());
			if (stateHolder != null) {
				s.setState(ctx, stateHolder.state);
				s.setExpireTime(ctx, stateHolder.expireTime);
			}
			String subscriptionPeriod = null;
			if (product.getSku().contains(".annual")) {
				subscriptionPeriod = "P1Y";
			} else if (product.getSku().contains(".monthly")) {
				subscriptionPeriod = "P1M";
			}
			if (!Algorithms.isEmpty(subscriptionPeriod)) {
				try {
					s.setSubscriptionPeriodString(subscriptionPeriod);
				} catch (ParseException e) {
					LOG.error(e);
				}
			}
		}
	}

	private class RequestInventoryCommand extends InAppCommand {

		private final boolean userRequested;
		private final PurchaseResponseListener responseListener;

		private RequestInventoryCommand(boolean userRequested) {
			this.userRequested = userRequested;
			this.responseListener = new PurchaseResponseListener() {
				@Override
				public void onUserDataResponse(@Nullable UserIapData userData) {
					InAppPurchaseHelperImpl.this.userData = userData;
					if (userData != null) {
						logDebug("getPurchaseUpdates");
						PurchasingService.getPurchaseUpdates(true);
					} else {
						obtainInAppsInfo();
					}
				}

				@Override
				public void onPurchaseUpdatesResponse(@Nullable Map<UserData, List<Receipt>> purchaseMap, boolean hasMore) {
					List<Receipt> receipts = new ArrayList<>(InAppPurchaseHelperImpl.this.receipts);
					UserIapData userData = InAppPurchaseHelperImpl.this.userData;
					if (!Algorithms.isEmpty(purchaseMap) && userData != null) {
						for (Entry<UserData, List<Receipt>> receiptsEntry : purchaseMap.entrySet()) {
							UserData ud = receiptsEntry.getKey();
							if (Algorithms.stringsEqual(userData.getAmazonUserId(), ud.getUserId()) &&
									Algorithms.stringsEqual(userData.getAmazonMarketplace(), ud.getMarketplace())) {
								receipts.addAll(receiptsEntry.getValue());
							}
						}
					}
					InAppPurchaseHelperImpl.this.receipts = receipts;
					if (!hasMore) {
						obtainInAppsInfo();
					}
				}

				@Override
				public void onProductDataResponse(@Nullable Map<String, Product> productMap) {
					InAppPurchaseHelperImpl.this.productMap = productMap;
					if (productMap != null) {
						purchasingListener.removeResponseListener(responseListener);
						processInventory();
					} else {
						commandDone();
					}
				}

				@Override
				public void onPurchaseResponse(@Nullable PurchaseResponse response) {
				}

				private void processInventory() {
					logDebug("Query sku details was successful.");

					/*
					 * Check for items we own. Notice that for each purchase, we check
					 * the developer payload to see if it's correct!
					 */

					List<String> allOwnedSubscriptionSkus = new ArrayList<>(subscriptionStateMap.keySet());
					for (InAppSubscription s : getSubscriptions().getAllSubscriptions()) {
						if (hasDetails(s.getSku())) {
							Receipt receipt = getReceipt(s.getSku());
							Product productInfo = getProductInfo(s.getSku());
							if (productInfo != null) {
								fetchInAppPurchase(s, productInfo, receipt);
							}
							allOwnedSubscriptionSkus.remove(s.getSku());
						}
					}
					for (String sku : allOwnedSubscriptionSkus) {
						Receipt receipt = getReceipt(sku);
						Product productInfo = getProductInfo(sku);
						if (productInfo != null) {
							InAppSubscription s = getSubscriptions().upgradeSubscription(sku);
							if (s != null) {
								fetchInAppPurchase(s, productInfo, receipt);
							}
						}
					}

					InAppPurchase fullVersion = getFullVersion();
					if (fullVersion != null && hasDetails(fullVersion.getSku())) {
						Receipt receipt = getReceipt(fullVersion.getSku());
						Product productInfo = getProductInfo(fullVersion.getSku());
						if (productInfo != null) {
							fetchInAppPurchase(fullVersion, productInfo, receipt);
						}
					}
					if (fullVersion != null && getReceipt(fullVersion.getSku()) != null) {
						ctx.getSettings().FULL_VERSION_PURCHASED.set(true);
					}

					// Do we have the live updates?
					boolean subscribedToLiveUpdates = false;
					boolean subscribedToOsmAndPro = false;
					boolean subscribedToMaps = false;
					Map<String, Receipt> subscriptionPurchases = new HashMap<>();
					for (InAppSubscription s : getSubscriptions().getAllSubscriptions()) {
						Receipt receipt = getReceipt(s.getSku());
						if (receipt != null || s.getState().isActive()) {
							if (receipt != null) {
								subscriptionPurchases.put(s.getSku(), receipt);
							}
							if (!subscribedToLiveUpdates && purchases.isLiveUpdatesSubscription(s)) {
								subscribedToLiveUpdates = true;
							}
							if (!subscribedToOsmAndPro && purchases.isOsmAndProSubscription(s)) {
								subscribedToOsmAndPro = true;
							}
							if (!subscribedToMaps && purchases.isMapsSubscription(s)) {
								subscribedToMaps = true;
							}
						}
					}
					if (!subscribedToLiveUpdates && ctx.getSettings().LIVE_UPDATES_PURCHASED.get()) {
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(false);
					} else if (subscribedToLiveUpdates) {
						ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
					}
					if (!subscribedToOsmAndPro && ctx.getSettings().OSMAND_PRO_PURCHASED.get()) {
						ctx.getSettings().OSMAND_PRO_PURCHASED.set(false);
					} else if (subscribedToOsmAndPro) {
						ctx.getSettings().OSMAND_PRO_PURCHASED.set(true);
					}
					if (!subscribedToMaps && ctx.getSettings().OSMAND_MAPS_PURCHASED.get()) {
						ctx.getSettings().OSMAND_MAPS_PURCHASED.set(false);
					} else if (subscribedToMaps) {
						ctx.getSettings().OSMAND_MAPS_PURCHASED.set(true);
					}
					if (!subscribedToLiveUpdates && !subscribedToOsmAndPro && !subscribedToMaps) {
						onSubscriptionExpired();
					}

					lastValidationCheckTime = System.currentTimeMillis();
					logDebug("User " + (subscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE") + " live updates purchased.");
					logDebug("User " + (subscribedToOsmAndPro ? "HAS" : "DOES NOT HAVE") + " OsmAnd Pro purchased.");
					logDebug("User " + (subscribedToMaps ? "HAS" : "DOES NOT HAVE") + " Maps purchased.");

					OsmandSettings settings = ctx.getSettings();
					settings.INAPPS_READ.set(true);

					Map<String, Receipt> tokensToSend = new HashMap<>();
					if (subscriptionPurchases.size() > 0) {
						List<String> tokensSent = Arrays.asList(settings.BILLING_PURCHASE_TOKENS_SENT.get().split(";"));
						for (Entry<String, Receipt> receiptEntry : subscriptionPurchases.entrySet()) {
							String sku = receiptEntry.getKey();
							if (!tokensSent.contains(sku)) {
								tokensToSend.put(sku, receiptEntry.getValue());
							}
						}
					}
					List<PurchaseInfo> purchaseInfoList = new ArrayList<>();
					for (Entry<String, Receipt> receiptEntry : tokensToSend.entrySet()) {
						purchaseInfoList.add(getPurchaseInfo(receiptEntry.getKey(), receiptEntry.getValue()));
					}
					onSkuDetailsResponseDone(purchaseInfoList, userRequested);
				}

				private void onSubscriptionExpired() {
					if (!InAppPurchaseUtils.isDepthContoursPurchased(ctx)) {
						ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(false);
					}
				}
			};
			purchasingListener.addResponseListener(responseListener);
		}

		@Override
		protected void commandDone() {
			super.commandDone();
			purchasingListener.removeResponseListener(responseListener);
			inventoryRequested = false;
		}

		@Override
		protected boolean userRequested() {
			return userRequested;
		}

		@Override
		public void run(InAppPurchaseHelper helper) {
			logDebug("Setup successful. Querying inventory.");
			InAppPurchaseHelperImpl.this.receipts = new ArrayList<>();
			try {
				logDebug("getUserData");
				PurchasingService.getUserData();
			} catch (Exception e) {
				logError("queryInventoryAsync Error", e);
				notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
				stop(true);
				commandDone();
			}
		}

		private void obtainInAppsInfo() {
			if (uiActivity != null) {
				Set<String> productIds = new HashSet<>();
				List<Receipt> receipts = InAppPurchaseHelperImpl.this.receipts;
				Map<String, SubscriptionStateHolder> subscriptionStateMap = new HashMap<>();
				Map<String, Receipt> subscriptionReceiptMap = new HashMap<>();
				boolean requested = false;
				if (!Algorithms.isEmpty(receipts)) {
					String url = "https://osmand.net/api/subscriptions/get";
					String userOperation = "Requesting subscription state...";
					List<Request> requests = new ArrayList<>();
					for (Receipt receipt : receipts) {
						if (receipt.getProductType() == ProductType.SUBSCRIPTION) {
							productIds.add(receipt.getSku());
							Map<String, String> parameters = new HashMap<>();
							parameters.put("androidPackage", ctx.getPackageName());
							addUserInfo(parameters);
							parameters.put("orderId", receipt.getReceiptId());
							requests.add(new Request(url, parameters, userOperation, false, false));
						}
					}
					if (!Algorithms.isEmpty(requests)) {
						requested = true;
						AndroidNetworkUtils.sendRequestsAsync(ctx, requests, new OnSendRequestsListener() {
							@Override
							public void onRequestSending(@NonNull Request request) {
							}

							@Override
							public void onRequestSent(@NonNull RequestResponse response) {
							}

							@Override
							public void onRequestsSent(@NonNull List<RequestResponse> results) {
								for (RequestResponse response : results) {
									String subscriptionStateStr = response.getResponse();
									if (!Algorithms.isEmpty(subscriptionStateStr)) {
										Map<String, SubscriptionStateHolder> subscriptionState = parseSubscriptionStates(subscriptionStateStr);
										if (!subscriptionState.isEmpty()) {
											Entry<String, SubscriptionStateHolder> subStateEntry = subscriptionState.entrySet().iterator().next();
											String sku = subStateEntry.getKey();
											SubscriptionStateHolder state = subStateEntry.getValue();
											subscriptionStateMap.put(sku, state);
											String receiptId = response.getRequest().getParameters().get("orderId");
											for (Receipt receipt : receipts) {
												if (receipt.getReceiptId().equals(receiptId)) {
													subscriptionReceiptMap.put(sku, receipt);
													break;
												}
											}
											productIds.add(sku);
										}
									}
								}
								InAppPurchaseHelperImpl.this.subscriptionStateMap = subscriptionStateMap;
								InAppPurchaseHelperImpl.this.subscriptionReceiptMap = subscriptionReceiptMap;
								for (InAppPurchase purchase : getInAppPurchases().getAllInAppPurchases(true)) {
									productIds.add(purchase.getSku());
								}
								PurchasingService.getProductData(productIds);
							}
						});
					}
				}
				if (!requested) {
					for (InAppPurchase purchase : getInAppPurchases().getAllInAppPurchases(true)) {
						productIds.add(purchase.getSku());
					}
					PurchasingService.getProductData(productIds);
				}
			} else {
				commandDone();
			}
		}
	}

	private class PurchaseInAppCommand extends InAppCommand {

		private final WeakReference<Activity> activityRef;
		private final String sku;
		private final PurchaseResponseListener responseListener;

		public PurchaseInAppCommand(WeakReference<Activity> activity, String sku) {
			this.activityRef = activity;
			this.sku = sku;

			responseListener = new PurchaseResponseListener() {
				@Override
				public void onUserDataResponse(@Nullable UserIapData userData) {
				}

				@Override
				public void onProductDataResponse(@Nullable Map<String, Product> productMap) {
				}

				@Override
				public void onPurchaseUpdatesResponse(@Nullable Map<UserData, List<Receipt>> purchaseMap, boolean hasMore) {
				}

				@Override
				public void onPurchaseResponse(@Nullable PurchaseResponse response) {
					if (response != null) {
						Activity a = activity.get();
						if (AndroidUtils.isActivityNotDestroyed(a)) {
							onPurchaseFinished(sku, response);
						} else {
							logError("startResolutionForResult on destroyed activity");
						}
					} else {
						logError("Purchase failed");
					}
					commandDone();
				}
			};
			purchasingListener.addResponseListener(responseListener);
		}

		@Override
		protected void commandDone() {
			super.commandDone();
			purchasingListener.removeResponseListener(responseListener);
		}

		@Override
		public void run(InAppPurchaseHelper helper) {
			try {
				Activity a = activityRef.get();
				Product productInfo = getProductInfo(sku);
				if (AndroidUtils.isActivityNotDestroyed(a) && productInfo != null) {
					logDebug("getPurchaseUpdates");
					PurchasingService.purchase(sku);
				} else {
					stop(true);
				}
			} catch (Exception e) {
				logError("launchPurchaseFlow Error", e);
				stop(true);
			}
		}
	}
}