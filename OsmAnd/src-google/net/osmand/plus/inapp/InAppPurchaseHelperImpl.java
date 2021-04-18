package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseState;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.inapp.InAppPurchases.PurchaseInfo;
import net.osmand.plus.inapp.InAppPurchasesImpl.InAppPurchaseLiveUpdatesOldSubscription;
import net.osmand.plus.inapp.util.BillingManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {

	// The helper object
	private BillingManager billingManager;
	private List<SkuDetails> skuDetailsList;

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

	public InAppPurchaseHelperImpl(OsmandApplication ctx) {
		super(ctx);
		purchases = new InAppPurchasesImpl(ctx);
	}

	@Override
	public void isInAppPurchaseSupported(@NonNull final Activity activity, @Nullable final InAppPurchaseInitCallback callback) {
		if (callback != null) {
			callback.onSuccess();
		}
	}

	private BillingManager getBillingManager() {
		return billingManager;
	}

	@Override
	protected void execImpl(@NonNull final InAppPurchaseTaskType taskType, @NonNull final InAppCommand runnable) {
		billingManager = new BillingManager(ctx, BASE64_ENCODED_PUBLIC_KEY, new BillingManager.BillingUpdatesListener() {

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

				runnable.run(InAppPurchaseHelperImpl.this);
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
					billingManager.querySkuDetailsAsync(BillingClient.SkuType.INAPP, skuInApps, new SkuDetailsResponseListener() {
						@Override
						public void onSkuDetailsResponse(@NonNull BillingResult billingResult, final List<SkuDetails> skuDetailsListInApps) {
							// Is it a failure?
							if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
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
							skuSubscriptions.addAll(subscriptionStateMap.keySet());

							BillingManager billingManager = getBillingManager();
							// Have we been disposed of in the meantime? If so, quit.
							if (billingManager == null) {
								stop(true);
								return;
							}

							billingManager.querySkuDetailsAsync(BillingClient.SkuType.SUBS, skuSubscriptions, new SkuDetailsResponseListener() {
								@Override
								public void onSkuDetailsResponse(@NonNull BillingResult billingResult, final List<SkuDetails> skuDetailsListSubscriptions) {
									// Is it a failure?
									if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
										logError("Failed to query subscriptipons sku details: " + billingResult.getResponseCode());
										notifyError(InAppPurchaseTaskType.REQUEST_INVENTORY, billingResult.getDebugMessage());
										stop(true);
										return;
									}

									List<SkuDetails> skuDetailsList = new ArrayList<>(skuDetailsListInApps);
									skuDetailsList.addAll(skuDetailsListSubscriptions);
									InAppPurchaseHelperImpl.this.skuDetailsList = skuDetailsList;

									mSkuDetailsResponseListener.onSkuDetailsResponse(billingResult, skuDetailsList);
								}
							});
						}
					});
				}
				for (Purchase purchase : purchases) {
					InAppSubscription subscription = getLiveUpdates().getSubscriptionBySku(purchase.getSku());
					if (!purchase.isAcknowledged() || (subscription != null && !subscription.isPurchased())) {
						onPurchaseFinished(purchase);
					}
				}
			}

			@Override
			public void onPurchaseCanceled() {
				stop(true);
			}
		});
	}

	@Override
	public void purchaseFullVersion(@NonNull final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_FULL_VERSION);
		exec(InAppPurchaseTaskType.PURCHASE_FULL_VERSION, new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
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
					commandDone();
				} catch (Exception e) {
					complain("Cannot launch full version purchase!");
					logError("purchaseFullVersion Error", e);
					stop(true);
				}
			}
		});
	}

	@Override
	public void purchaseDepthContours(@NonNull final Activity activity) {
		notifyShowProgress(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS);
		exec(InAppPurchaseTaskType.PURCHASE_DEPTH_CONTOURS, new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
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
					commandDone();
				} catch (Exception e) {
					complain("Cannot launch depth contours purchase!");
					logError("purchaseDepthContours Error", e);
					stop(true);
				}
			}
		});
	}

	@Override
	public void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException {
		OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if(plugin == null || plugin.getInstallURL() == null) {
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.activate_srtm_plugin), Toast.LENGTH_LONG).show();
		} else {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
		}
	}

	@Override
	public void manageSubscription(@NonNull Context ctx, @Nullable String sku) {
		String url = "https://play.google.com/store/account/subscriptions?package=" + ctx.getPackageName();
		if (!Algorithms.isEmpty(sku)) {
			url += "&sku=" + sku;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		ctx.startActivity(intent);
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
	private final SkuDetailsResponseListener mSkuDetailsResponseListener = new SkuDetailsResponseListener() {

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
			for (Entry<String, SubscriptionStateHolder> entry : subscriptionStateMap.entrySet()) {
				SubscriptionState state = entry.getValue().state;
				if (state == SubscriptionState.PAUSED || state == SubscriptionState.ON_HOLD) {
					String sku = entry.getKey();
					if (!result.contains(sku)) {
						result.add(sku);
					}
				}
			}
			return result;
		}

		@Override
		public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {

			logDebug("Query sku details finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (getBillingManager() == null) {
				stop(true);
				return;
			}

			// Is it a failure?
			if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
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
			for (InAppSubscription s : getLiveUpdates().getAllSubscriptions()) {
				Purchase purchase = getPurchase(s.getSku());
				if (purchase != null || s.getState().isActive()) {
					if (purchase != null) {
						liveUpdatesPurchases.add(purchase);
					}
					if (!subscribedToLiveUpdates) {
						subscribedToLiveUpdates = true;
					}
				}
			}
			if (!subscribedToLiveUpdates && ctx.getSettings().LIVE_UPDATES_PURCHASED.get()) {
				ctx.getSettings().LIVE_UPDATES_PURCHASED.set(false);
				if (!isDepthContoursPurchased(ctx)) {
					ctx.getSettings().getCustomRenderBooleanProperty("depthContours").set(false);
				}
			} else if (subscribedToLiveUpdates) {
				ctx.getSettings().LIVE_UPDATES_PURCHASED.set(true);
			}

			lastValidationCheckTime = System.currentTimeMillis();
			logDebug("User " + (subscribedToLiveUpdates ? "HAS" : "DOES NOT HAVE") + " live updates purchased.");

			OsmandSettings settings = ctx.getSettings();
			settings.INAPPS_READ.set(true);

			List<Purchase> tokensToSend = new ArrayList<>();
			if (liveUpdatesPurchases.size() > 0) {
				List<String> tokensSent = Arrays.asList(settings.BILLING_PURCHASE_TOKENS_SENT.get().split(";"));
				for (Purchase purchase : liveUpdatesPurchases) {
					if (needRestoreUserInfo()) {
						restoreUserInfo(purchase);
					}
					if (!tokensSent.contains(purchase.getSku())) {
						tokensToSend.add(purchase);
					}
				}
			}
			List<PurchaseInfo> purchaseInfoList = new ArrayList<>();
			for (Purchase purchase : tokensToSend) {
				purchaseInfoList.add(getPurchaseInfo(purchase));
			}
			onSkuDetailsResponseDone(purchaseInfoList);
		}
	};

	private void restoreUserInfo(Purchase purchase) {
		boolean restored = restoreUserInfoFromString(purchase.getDeveloperPayload());
		if (!restored) {
			AccountIdentifiers accountIdentifiers = purchase.getAccountIdentifiers();
			if (accountIdentifiers != null) {
				restoreUserInfoFromString(accountIdentifiers.getObfuscatedAccountId());
			}
		}
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

	private PurchaseInfo getPurchaseInfo(Purchase purchase) {
		return new PurchaseInfo(purchase.getSku(), purchase.getOrderId(), purchase.getPurchaseToken(),
				purchase.getPurchaseTime(), purchase.getPurchaseState(), purchase.isAcknowledged(), purchase.isAutoRenewing());
	}

	private void fetchInAppPurchase(@NonNull InAppPurchase inAppPurchase, @NonNull SkuDetails skuDetails, @Nullable Purchase purchase) {
		if (purchase != null) {
			inAppPurchase.setPurchaseState(PurchaseState.PURCHASED);
			inAppPurchase.setPurchaseInfo(ctx, getPurchaseInfo(purchase));
		} else {
			inAppPurchase.setPurchaseState(PurchaseState.NOT_PURCHASED);
			inAppPurchase.restorePurchaseInfo(ctx);
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
			InAppSubscription s = (InAppSubscription) inAppPurchase;
			s.restoreState(ctx);
			s.restoreExpireTime(ctx);
			SubscriptionStateHolder stateHolder = subscriptionStateMap.get(s.getSku());
			if (stateHolder != null) {
				s.setState(ctx, stateHolder.state);
				s.setExpireTime(ctx, stateHolder.expireTime);
			}
			if (s.getState().isGone() && s.hasStateChanged()) {
				ctx.getSettings().LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME.set(0L);
				ctx.getSettings().LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME.set(0L);
			}
			String introductoryPrice = skuDetails.getIntroductoryPrice();
			String introductoryPricePeriod = skuDetails.getIntroductoryPricePeriod();
			int introductoryPriceCycles = skuDetails.getIntroductoryPriceCycles();
			long introductoryPriceAmountMicros = skuDetails.getIntroductoryPriceAmountMicros();
			if (!Algorithms.isEmpty(introductoryPrice)) {
				try {
					s.setIntroductoryInfo(new InAppPurchases.InAppSubscriptionIntroductoryInfo(s, introductoryPrice,
							introductoryPriceAmountMicros, introductoryPricePeriod, introductoryPriceCycles));
				} catch (ParseException e) {
					LOG.error(e);
				}
			}
		}
	}

	@Override
	protected InAppCommand getPurchaseLiveUpdatesCommand(final WeakReference<Activity> activity, final String sku, final String userInfo) {
		return new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
				try {
					Activity a = activity.get();
					SkuDetails skuDetails = getSkuDetails(sku);
					if (AndroidUtils.isActivityNotDestroyed(a) && skuDetails != null) {
						BillingManager billingManager = getBillingManager();
						if (billingManager != null) {
							billingManager.setObfuscatedAccountId(userInfo);
							billingManager.initiatePurchaseFlow(a, skuDetails);
						} else {
							throw new IllegalStateException("BillingManager disposed");
						}
						commandDone();
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
	protected InAppCommand getRequestInventoryCommand() {
		return new InAppCommand() {
			@Override
			public void run(InAppPurchaseHelper helper) {
				logDebug("Setup successful. Querying inventory.");
				try {
					BillingManager billingManager = getBillingManager();
					if (billingManager != null) {
						billingManager.queryPurchases();
					} else {
						throw new IllegalStateException("BillingManager disposed");
					}
					commandDone();
				} catch (Exception e) {
					logError("queryInventoryAsync Error", e);
					notifyDismissProgress(InAppPurchaseTaskType.REQUEST_INVENTORY);
					stop(true);
				}
			}
		};
	}

	// Call when a purchase is finished
	private void onPurchaseFinished(Purchase purchase) {
		logDebug("Purchase finished: " + purchase);

		// if we were disposed of in the meantime, quit.
		if (getBillingManager() == null) {
			stop(true);
			return;
		}

		onPurchaseDone(getPurchaseInfo(purchase));
	}

	@Override
	protected boolean isBillingManagerExists() {
		return getBillingManager() != null;
	}

	@Override
	protected void destroyBillingManager() {
		BillingManager billingManager = getBillingManager();
		if (billingManager != null) {
			billingManager.destroy();
			this.billingManager = null;
		}
	}
}
