package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.iap.entity.StartIapActivityReq;
import com.huawei.hms.iap.entity.StartIapActivityResult;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionIntroductoryInfo;
import net.osmand.plus.inapp.InAppPurchases.PurchaseInfo;
import net.osmand.plus.inapp.InAppPurchasesImpl.InAppPurchaseLiveUpdatesOldSubscription;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {

	private boolean envReady = false;
	private boolean purchaseSupported = false;

	private List<ProductInfo> productInfos;
	private OwnedPurchasesResult ownedSubscriptions;
	private final List<OwnedPurchasesResult> ownedInApps = new ArrayList<>();

	private boolean purchasedLocalFullVersion = false;
	private boolean purchasedLocalDepthContours = false;
	private boolean subscribedToLocalLiveUpdates = false;
	private boolean subscribedToLocalOsmAndPro = false;
	private boolean subscribedToLocalMaps = false;

	public InAppPurchaseHelperImpl(OsmandApplication ctx) {
		super(ctx);
		purchases = new InAppPurchasesImpl(ctx);
	}

	@Override
	public boolean isPurchasedLocalFullVersion() {
		return purchasedLocalFullVersion;
	}

	@Override
	public boolean isPurchasedLocalDeepContours() {
		return purchasedLocalDepthContours;
	}

	@Override
	public boolean isSubscribedToLocalLiveUpdates() {
		return subscribedToLocalLiveUpdates;
	}

	@Override
	public boolean isSubscribedToLocalOsmAndPro() {
		return subscribedToLocalOsmAndPro;
	}

	@Override
	public boolean isSubscribedToLocalMaps() {
		return subscribedToLocalMaps;
	}

	@Override
	public void isInAppPurchaseSupported(@NonNull final Activity activity, @Nullable final InAppPurchaseInitCallback callback) {
		if (envReady) {
			if (callback != null) {
				if (purchaseSupported) {
					callback.onSuccess();
				} else {
					callback.onFail();
				}
			}
		} else {
			// Initiating an isEnvReady request when entering the app.
			// Check if the account service country supports IAP.
			IapClient mClient = Iap.getIapClient(activity);
			final WeakReference<Activity> activityRef = new WeakReference<>(activity);
			IapRequestHelper.isEnvReady(mClient, new IapApiCallback<IsEnvReadyResult>() {

				private void onReady(boolean succeed) {
					logDebug("Setup finished.");
					envReady = true;
					purchaseSupported = succeed;
					if (callback != null) {
						if (succeed) {
							callback.onSuccess();
						} else {
							callback.onFail();
						}
					}
				}

				@Override
				public void onSuccess(IsEnvReadyResult result) {
					onReady(true);
				}

				@Override
				public void onFail(Exception e) {
					onReady(false);
					LOG.error("isEnvReady fail, " + e.getMessage(), e);
					ExceptionHandle.handle(activityRef.get(), e);
				}
			});
		}
	}

	@Override
	public String getPlatform() {
		return PLATFORM_HUAWEI;
	}
	protected void execImpl(@NonNull final InAppPurchaseTaskType taskType, @NonNull final InAppCommand command) {
		if (envReady) {
			command.run(this);
		} else {
			command.commandDone();
		}
	}

	private InAppCommand getPurchaseInAppCommand(@NonNull final Activity activity, @NonNull final String productId) throws UnsupportedOperationException {
		return new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
				try {
					ProductInfo productInfo = getProductInfo(productId);
					if (productInfo != null) {
						IapRequestHelper.createPurchaseIntent(getIapClient(), productInfo.getProductId(),
								IapClient.PriceType.IN_APP_NONCONSUMABLE, new IapApiCallback<PurchaseIntentResult>() {
									@Override
									public void onSuccess(PurchaseIntentResult result) {
										if (result == null) {
											logError("result is null");
										} else {
											// you should pull up the page to complete the payment process
											IapRequestHelper.startResolutionForResult(activity, result.getStatus(), Constants.REQ_CODE_BUY_INAPP);
										}
										commandDone();
									}

									@Override
									public void onFail(Exception e) {
										int errorCode = ExceptionHandle.handle(activity, e);
										if (errorCode != ExceptionHandle.SOLVED) {
											logDebug("createPurchaseIntent, returnCode: " + errorCode);
											if (OrderStatusCode.ORDER_PRODUCT_OWNED == errorCode) {
												logError("already own this product");
											} else {
												logError("unknown error");
											}
										}
										commandDone();
									}
								});
					} else {
						commandDone();
					}
				} catch (Exception e) {
					complain("Cannot launch full version purchase!");
					logError("purchaseFullVersion Error", e);
					stop(true);
				}
			}
		};
	}

	@Override
	public void purchaseFullVersion(@NonNull final Activity activity) throws UnsupportedOperationException {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
		InAppPurchase fullVersion = purchases.getFullVersion();
		if (fullVersion != null) {
			exec(InAppPurchaseTaskType.PURCHASE_FULL_VERSION, getPurchaseInAppCommand(activity, fullVersion.getSku()));
		}
	}

	@Override
	public void purchaseDepthContours(@NonNull final Activity activity) throws UnsupportedOperationException {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
		InAppPurchase depthContours = purchases.getDepthContours();
		if (depthContours != null) {
			exec(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS, getPurchaseInAppCommand(activity, depthContours.getSku()));
		}
	}

	@Override
	public void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_CONTOUR_LINES);
		InAppPurchase contourLines = purchases.getContourLines();
		if (contourLines != null) {
			exec(InAppPurchaseTaskType.PURCHASE_CONTOUR_LINES, getPurchaseInAppCommand(activity, contourLines.getSku()));
		}
	}

	@Override
	public void manageSubscription(@NonNull Context ctx, @Nullable String sku) {
		if (uiActivity != null) {
			StartIapActivityReq req = new StartIapActivityReq();
			if (!Algorithms.isEmpty(sku)) {
				req.setSubscribeProductId(sku);
				req.setType(StartIapActivityReq.TYPE_SUBSCRIBE_EDIT_ACTIVITY);
			} else {
				req.setType(StartIapActivityReq.TYPE_SUBSCRIBE_MANAGER_ACTIVITY);
			}
			Task<StartIapActivityResult> task = getIapClient().startIapActivity(req);
			task.addOnSuccessListener(result -> {
				logDebug("startIapActivity: onSuccess");
				Activity activity = (Activity) uiActivity;
				if (result != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					result.startActivity(activity);
				}
			}).addOnFailureListener(e -> logDebug("startIapActivity: onFailure"));
		}
	}

	@Nullable
	private ProductInfo getProductInfo(@NonNull String productId) {
		List<ProductInfo> productInfos = this.productInfos;
		if (productInfos != null) {
			for (ProductInfo info : productInfos) {
				if (info.getProductId().equals(productId)) {
					return info;
				}
			}
		}
		return null;
	}

	private boolean hasDetails(@NonNull String productId) {
		return getProductInfo(productId) != null;
	}

	@Nullable
	private InAppPurchaseData getPurchaseData(@NonNull String productId) {
		InAppPurchaseData data = SubscriptionUtils.getPurchaseData(ownedSubscriptions, productId);
		if (data == null) {
			for (OwnedPurchasesResult result : ownedInApps) {
				data = InAppUtils.getPurchaseData(result, productId);
				if (data != null) {
					break;
				}
			}
		}
		return data;
	}

	private PurchaseInfo getPurchaseInfo(InAppPurchaseData purchase) {
		return new PurchaseInfo(Collections.singletonList(purchase.getProductId()), purchase.getSubscriptionId(), purchase.getPurchaseToken(),
				purchase.getPurchaseTime(), purchase.getPurchaseState(), true, purchase.isAutoRenewing());
	}

	private void fetchInAppPurchase(@NonNull InAppPurchase inAppPurchase, @NonNull ProductInfo productInfo, @Nullable InAppPurchaseData purchaseData) {
		if (purchaseData != null) {
			inAppPurchase.setPurchaseState(InAppPurchase.PurchaseState.PURCHASED);
			inAppPurchase.setPurchaseInfo(ctx, getPurchaseInfo(purchaseData));
		} else {
			inAppPurchase.setPurchaseState(InAppPurchase.PurchaseState.NOT_PURCHASED);
			inAppPurchase.restorePurchaseInfo(ctx);
		}
		inAppPurchase.setPrice(productInfo.getPrice());
		inAppPurchase.setPriceCurrencyCode(productInfo.getCurrency());
		if (productInfo.getMicrosPrice() > 0) {
			inAppPurchase.setPriceValue(productInfo.getMicrosPrice() / 1000000d);
		}
		String subscriptionPeriod = productInfo.getSubPeriod();
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
			String introductoryPrice = productInfo.getSubSpecialPrice();
			String introductoryPricePeriod = productInfo.getSubPeriod();
			int introductoryPriceCycles = productInfo.getSubSpecialPeriodCycles();
			long introductoryPriceAmountMicros = productInfo.getSubSpecialPriceMicros();
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

	@Override
	protected InAppCommand getPurchaseSubscriptionCommand(final WeakReference<Activity> activity,
	                                                      String sku, String userInfo) throws UnsupportedOperationException {
		return new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
				try {
					Activity a = activity.get();
					ProductInfo productInfo = getProductInfo(sku);
					if (AndroidUtils.isActivityNotDestroyed(a) && productInfo != null) {
						IapRequestHelper.createPurchaseIntent(getIapClient(), sku,
								IapClient.PriceType.IN_APP_SUBSCRIPTION, userInfo, new IapApiCallback<PurchaseIntentResult>() {
									@Override
									public void onSuccess(PurchaseIntentResult result) {
										if (result == null) {
											logError("GetBuyIntentResult is null");
										} else {
											Activity a = activity.get();
											if (AndroidUtils.isActivityNotDestroyed(a)) {
												IapRequestHelper.startResolutionForResult(a, result.getStatus(), Constants.REQ_CODE_BUY_SUB);
											} else {
												logError("startResolutionForResult on destroyed activity");
											}
										}
										commandDone();
									}

									@Override
									public void onFail(Exception e) {
										int errorCode = ExceptionHandle.handle(activity.get(), e);
										if (ExceptionHandle.SOLVED != errorCode) {
											logError("createPurchaseIntent, returnCode: " + errorCode);
											if (OrderStatusCode.ORDER_PRODUCT_OWNED == errorCode) {
												logError("already own this product");
											} else {
												logError("unknown error");
											}
										}
										commandDone();
									}
								});
					} else {
						stop(true);
					}
				} catch (Exception e) {
					logError("launchPurchaseFlow Error", e);
					stop(true);
				}
			}
		};
	}

	@Override
	protected InAppCommand getRequestInventoryCommand(boolean userRequested) {
		return new InAppCommand() {

			@Override
			protected void commandDone() {
				super.commandDone();
				inventoryRequested = false;
			}

			@Override
			public void run(InAppPurchaseHelper helper) {
				logDebug("Setup successful. Querying inventory.");
				try {
					productInfos = new ArrayList<>();
					obtainOwnedSubscriptions();
				} catch (Exception e) {
					logError("queryInventoryAsync Error", e);
					notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
					stop(true);
					commandDone();
				}
			}

			private void obtainOwnedSubscriptions() {
				if (uiActivity != null) {
					IapRequestHelper.obtainOwnedPurchases(getIapClient(), IapClient.PriceType.IN_APP_SUBSCRIPTION,
							null, new IapApiCallback<OwnedPurchasesResult>() {
								@Override
								public void onSuccess(OwnedPurchasesResult result) {
									ownedSubscriptions = result;
									obtainOwnedInApps(null);
								}

								@Override
								public void onFail(Exception e) {
									logError("obtainOwnedSubscriptions exception", e);
									ExceptionHandle.handle((Activity) uiActivity, e);
									stop(true);
									commandDone();
								}
							});
				} else {
					stop(true);
					commandDone();
				}
			}

			private void obtainOwnedInApps(final String continuationToken) {
				if (uiActivity != null) {
					// Query users' purchased non-consumable products.
					IapRequestHelper.obtainOwnedPurchases(getIapClient(), IapClient.PriceType.IN_APP_NONCONSUMABLE,
							continuationToken, new IapApiCallback<OwnedPurchasesResult>() {
								@Override
								public void onSuccess(OwnedPurchasesResult result) {
									ownedInApps.add(result);
									if (result != null && !TextUtils.isEmpty(result.getContinuationToken())) {
										obtainOwnedInApps(result.getContinuationToken());
									} else {
										obtainSubscriptionsInfo();
									}
								}

								@Override
								public void onFail(Exception e) {
									logError("obtainOwnedInApps exception", e);
									ExceptionHandle.handle((Activity) uiActivity, e);
									stop(true);
									commandDone();
								}
							});
				} else {
					stop(true);
					commandDone();
				}
			}

			private void obtainSubscriptionsInfo() {
				if (uiActivity != null) {
					Set<String> productIds = new HashSet<>();
					List<InAppSubscription> subscriptions = purchases.getSubscriptions().getAllSubscriptions();
					for (InAppSubscription s : subscriptions) {
						productIds.add(s.getSku());
					}
					productIds.addAll(ownedSubscriptions.getItemList());
					IapRequestHelper.obtainProductInfo(getIapClient(), new ArrayList<>(productIds),
							IapClient.PriceType.IN_APP_SUBSCRIPTION, new IapApiCallback<ProductInfoResult>() {
								@Override
								public void onSuccess(final ProductInfoResult result) {
									if (result != null && result.getProductInfoList() != null) {
										productInfos.addAll(result.getProductInfoList());
									}
									obtainInAppsInfo();
								}

								@Override
								public void onFail(Exception e) {
									int errorCode = ExceptionHandle.handle((Activity) uiActivity, e);
									if (ExceptionHandle.SOLVED != errorCode) {
										LOG.error("Unknown error");
									}
									stop(true);
									commandDone();
								}
							});
				} else {
					stop(true);
					commandDone();
				}
			}

			private void obtainInAppsInfo() {
				if (uiActivity != null) {
					Set<String> productIds = new HashSet<>();
					for (InAppPurchase purchase : getInAppPurchases().getAllInAppPurchases(false)) {
						productIds.add(purchase.getSku());
					}
					for (OwnedPurchasesResult result : ownedInApps) {
						productIds.addAll(result.getItemList());
					}
					IapRequestHelper.obtainProductInfo(getIapClient(), new ArrayList<>(productIds),
							IapClient.PriceType.IN_APP_NONCONSUMABLE, new IapApiCallback<ProductInfoResult>() {
								@Override
								public void onSuccess(ProductInfoResult result) {
									if (result != null && result.getProductInfoList() != null) {
										productInfos.addAll(result.getProductInfoList());
									}
									processInventory();
								}

								@Override
								public void onFail(Exception e) {
									int errorCode = ExceptionHandle.handle((Activity) uiActivity, e);
									if (ExceptionHandle.SOLVED != errorCode) {
										LOG.error("Unknown error");
									}
									stop(true);
									commandDone();
								}
							});
				} else {
					stop(true);
					commandDone();
				}
			}

			private void processInventory() {
				logDebug("Query sku details was successful.");

				/*
				 * Check for items we own. Notice that for each purchase, we check
				 * the developer payload to see if it's correct!
				 */

				List<String> allOwnedSubscriptionSkus = ownedSubscriptions.getItemList();
				for (InAppSubscription s : getSubscriptions().getAllSubscriptions()) {
					if (hasDetails(s.getSku())) {
						InAppPurchaseData purchaseData = getPurchaseData(s.getSku());
						ProductInfo liveUpdatesInfo = getProductInfo(s.getSku());
						if (liveUpdatesInfo != null) {
							fetchInAppPurchase(s, liveUpdatesInfo, purchaseData);
						}
						allOwnedSubscriptionSkus.remove(s.getSku());
					}
				}
				for (String sku : allOwnedSubscriptionSkus) {
					InAppPurchaseData purchaseData = getPurchaseData(sku);
					ProductInfo liveUpdatesInfo = getProductInfo(sku);
					if (liveUpdatesInfo != null) {
						InAppSubscription s = getSubscriptions().upgradeSubscription(sku);
						if (s == null) {
							s = new InAppPurchaseLiveUpdatesOldSubscription(liveUpdatesInfo);
						}
						fetchInAppPurchase(s, liveUpdatesInfo, purchaseData);
					}
				}

				List<InAppPurchaseData> completePurchases = new ArrayList<>();
				InAppPurchase fullVersion = getFullVersion();
				if (fullVersion != null && hasDetails(fullVersion.getSku())) {
					InAppPurchaseData purchaseData = getPurchaseData(fullVersion.getSku());
					ProductInfo fullPriceDetails = getProductInfo(fullVersion.getSku());
					if (fullPriceDetails != null) {
						fetchInAppPurchase(fullVersion, fullPriceDetails, purchaseData);
					}
				}
				InAppPurchase depthContours = getDepthContours();
				if (depthContours != null && hasDetails(depthContours.getSku())) {
					InAppPurchaseData purchaseData = getPurchaseData(depthContours.getSku());
					ProductInfo depthContoursDetails = getProductInfo(depthContours.getSku());
					if (depthContoursDetails != null) {
						fetchInAppPurchase(depthContours, depthContoursDetails, purchaseData);
					}
				}
				InAppPurchase contourLines = getContourLines();
				if (contourLines != null && hasDetails(contourLines.getSku())) {
					InAppPurchaseData purchaseData = getPurchaseData(contourLines.getSku());
					ProductInfo contourLinesDetails = getProductInfo(contourLines.getSku());
					if (contourLinesDetails != null) {
						fetchInAppPurchase(contourLines, contourLinesDetails, purchaseData);
					}
				}

				InAppPurchaseData fullVersionPurchaseData = fullVersion != null ? getPurchaseData(fullVersion.getSku()) : null;
				purchasedLocalFullVersion = fullVersionPurchaseData != null;
				if (fullVersionPurchaseData != null) {
					completePurchases.add(fullVersionPurchaseData);
				}
				InAppPurchaseData depthContoursPurchaseData = depthContours != null ? getPurchaseData(depthContours.getSku()) : null;
				purchasedLocalDepthContours = depthContoursPurchaseData != null;

				InAppPurchaseData countourLinesPurchaseData = contourLines != null ? getPurchaseData(contourLines.getSku()) : null;
				if (countourLinesPurchaseData != null) {
					ctx.getSettings().CONTOUR_LINES_PURCHASED.set(true);
				}

				// Do we have the live updates?
				boolean subscribedToLiveUpdates = false;
				boolean subscribedToOsmAndPro = false;
				boolean subscribedToMaps = false;
				for (InAppSubscription s : getSubscriptions().getAllSubscriptions()) {
					InAppPurchaseData purchaseData = getPurchaseData(s.getSku());
					if (purchaseData != null || s.getState().isActive()) {
						if (purchaseData != null) {
							completePurchases.add(purchaseData);
						}
						if (!subscribedToLiveUpdates && purchases.isLiveUpdates(s)) {
							subscribedToLiveUpdates = true;
						}
						if (!subscribedToOsmAndPro && purchases.isOsmAndPro(s)) {
							subscribedToOsmAndPro = true;
						}
						if (!subscribedToMaps && purchases.isMaps(s)) {
							subscribedToMaps = true;
						}
					}
				}
				subscribedToLocalLiveUpdates = subscribedToLiveUpdates;
				subscribedToLocalOsmAndPro = subscribedToOsmAndPro;
				subscribedToLocalMaps = subscribedToMaps;

				applyPurchases();

				lastValidationCheckTime = System.currentTimeMillis();

				OsmandSettings settings = ctx.getSettings();
				settings.INAPPS_READ.set(true);

				List<InAppPurchaseData> tokensToSend = new ArrayList<>();
				if (!completePurchases.isEmpty()) {
					List<String> tokensSent = Arrays.asList(settings.BILLING_PURCHASE_TOKENS_SENT.get().split(";"));
					for (InAppPurchaseData purchase : completePurchases) {
						if (needRestoreUserInfo()) {
							restoreUserInfo(purchase);
						}
						String sku = purchase.getProductId();
						if (!Algorithms.isEmpty(sku) && !tokensSent.contains(sku)) {
							tokensToSend.add(purchase);
						}
					}
				}
				List<PurchaseInfo> purchaseInfoList = new ArrayList<>();
				for (InAppPurchaseData purchase : tokensToSend) {
					purchaseInfoList.add(getPurchaseInfo(purchase));
				}
				onProductDetailsResponseDone(purchaseInfoList, userRequested);
			}
		};
	}

	private void restoreUserInfo(InAppPurchaseData purchase) {
		restoreUserInfoFromString(purchase.getDeveloperPayload());
	}

	private boolean restoreUserInfoFromString(String userInfo) {
		if (Algorithms.isEmpty(userInfo)) {
			return false;
		}
		OsmandSettings settings = ctx.getSettings();
		String[] arr = userInfo.split(" ");
		if (arr.length > 0) {
			settings.BILLING_USER_ID.set(arr[0]);
		}
		if (arr.length > 1) {
			token = arr[1];
			settings.BILLING_USER_TOKEN.set(token);
		}
		return needRestoreUserInfo();
	}

	private boolean needRestoreUserInfo() {
		OsmandSettings settings = ctx.getSettings();
		return Algorithms.isEmpty(settings.BILLING_USER_ID.get()) || Algorithms.isEmpty(settings.BILLING_USER_TOKEN.get());
	}

	private IapClient getIapClient() {
		return Iap.getIapClient((Activity) uiActivity);
	}

	// Call when a purchase is finished
	private void onPurchaseFinished(InAppPurchaseData purchase) {
		logDebug("Purchase finished: " + purchase.getProductId());
		onPurchaseDone(getPurchaseInfo(purchase));
	}

	@Override
	protected boolean isBillingManagerExists() {
		return false;
	}

	@Override
	protected boolean isBillingUnavailable() {
		return !purchaseSupported;
	}
	@Override
	protected void destroyBillingManager() {
		// non implemented
	}

	@Override
	public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.REQ_CODE_BUY_SUB) {
			boolean succeed = false;
			if (resultCode == Activity.RESULT_OK) {
				PurchaseResultInfo result = SubscriptionUtils.getPurchaseResult(activity, data);
				if (result != null) {
					switch (result.getReturnCode()) {
						case OrderStatusCode.ORDER_STATE_CANCEL:
							logDebug("Purchase cancelled");
							break;
						case OrderStatusCode.ORDER_STATE_FAILED:
							inventoryRequestPending = true;
							logDebug("Purchase failed");
							break;
						case OrderStatusCode.ORDER_PRODUCT_OWNED:
							inventoryRequestPending = true;
							logDebug("Product already owned");
							break;
						case OrderStatusCode.ORDER_STATE_SUCCESS:
							inventoryRequestPending = true;
							InAppPurchaseData purchaseData = SubscriptionUtils.getInAppPurchaseData(null,
									result.getInAppPurchaseData(), result.getInAppDataSignature());
							if (purchaseData != null) {
								onPurchaseFinished(purchaseData);
								succeed = true;
							} else {
								logDebug("Purchase failed");
							}
							break;
						default:
							break;
					}
				} else {
					logDebug("Purchase failed");
				}
			} else {
				logDebug("Purchase cancelled");
			}
			if (!succeed) {
				stop(true);
			}
			return true;
		} else if (requestCode == Constants.REQ_CODE_BUY_INAPP) {
			boolean succeed = false;
			if (data == null) {
				logDebug("data is null");
			} else {
				PurchaseResultInfo buyResultInfo = Iap.getIapClient(activity).parsePurchaseResultInfoFromIntent(data);
				switch (buyResultInfo.getReturnCode()) {
					case OrderStatusCode.ORDER_STATE_CANCEL:
						logDebug("Order has been canceled");
						break;
					case OrderStatusCode.ORDER_STATE_FAILED:
						inventoryRequestPending = true;
						logDebug("Order has been failed");
						break;
					case OrderStatusCode.ORDER_PRODUCT_OWNED:
						inventoryRequestPending = true;
						logDebug("Product already owned");
						break;
					case OrderStatusCode.ORDER_STATE_SUCCESS:
						InAppPurchaseData purchaseData = InAppUtils.getInAppPurchaseData(null,
								buyResultInfo.getInAppPurchaseData(), buyResultInfo.getInAppDataSignature());
						if (purchaseData != null) {
							onPurchaseFinished(purchaseData);
							succeed = true;
						} else {
							logDebug("Purchase failed");
						}
						break;
					default:
						break;
				}
			}
			if (!succeed) {
				stop(true);
			}
			return true;
		}
		return false;
	}
}
