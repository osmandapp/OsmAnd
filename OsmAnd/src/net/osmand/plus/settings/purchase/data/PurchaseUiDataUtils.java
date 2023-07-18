package net.osmand.plus.settings.purchase.data;

import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.ACTIVE;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.CANCELLED;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.IN_GRACE_PERIOD;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.UNDEFINED;

import androidx.annotation.NonNull;

import net.osmand.Period.PeriodUnit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseUiDataUtils {

	public static final int INVALID = -1;

	@NonNull
	public static PurchaseUiData createUiData(@NonNull OsmandApplication app, @NonNull InAppPurchase purchase) {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		InAppPurchases purchases = purchaseHelper.getInAppPurchases();

		String sku = purchase.getSku();
		String title = app.getString(R.string.shared_string_undefined);
		int iconId = INVALID;
		String purchaseType;
		long purchaseTime = purchase.getPurchaseTime();
		long expireTime = INVALID;
		boolean liveUpdateSubscription = purchases.isLiveUpdatesSubscription(purchase);
		boolean autoRenewing = false;
		boolean renewVisible = false;
		SubscriptionState subscriptionState = UNDEFINED;
		PurchaseOrigin origin = purchaseHelper.getPurchaseOriginBySku(sku);
		boolean isSubscription = purchase instanceof InAppSubscription;

		if (purchases.isOsmAndProSubscription(purchase)) {
			title = app.getString(R.string.osmand_pro);
			iconId = R.drawable.ic_action_osmand_pro_logo_colored;
		} else if (purchases.isLiveUpdatesSubscription(purchase)) {
			title = app.getString(R.string.osm_live);
			iconId = R.drawable.ic_action_subscription_osmand_live;
		} else if (purchases.isMapsSubscription(purchase) || purchases.isFullVersion(purchase)) {
			title = app.getString(R.string.maps_plus);
			iconId = R.drawable.ic_action_osmand_maps_plus;
		}

		if (isSubscription) {
			InAppSubscription subscription = (InAppSubscription) purchase;
			purchaseType = app.getString(subscription.getPeriodTypeString());

			subscriptionState = subscription.getState();
			if (subscription.isPurchased() && subscription.getPurchaseInfo() != null) {
				autoRenewing = subscription.getPurchaseInfo().isAutoRenewing();
				subscriptionState = ACTIVE;
			} else if (subscriptionState != UNDEFINED) {
				autoRenewing = subscriptionState == ACTIVE || subscriptionState == IN_GRACE_PERIOD;
			}
			expireTime = subscription.getExpireTime();
			if (expireTime == 0) {
				expireTime = subscription.getCalculatedExpiredTime();
			}
			if (!autoRenewing && subscriptionState != ACTIVE && subscriptionState != CANCELLED) {
				if (purchases.isMapsSubscription(subscription)) {
					boolean isFullVersion = !Version.isFreeVersion(app) || InAppPurchaseHelper.isFullVersionPurchased(app, false);
					renewVisible = !isFullVersion && !InAppPurchaseHelper.isSubscribedToAny(app, false);
				} else if (purchases.isOsmAndProSubscription(subscription)) {
					renewVisible = !InAppPurchaseHelper.isOsmAndProAvailable(app, false);
				}
			}
		} else {
			purchaseType = app.getString(R.string.in_app_purchase_desc);
			purchaseTime = purchase.getPurchaseTime();
		}

		return new PurchaseUiData(sku, title, iconId, purchaseType,
				expireTime, purchaseTime, isSubscription,
				liveUpdateSubscription, autoRenewing,
				renewVisible, subscriptionState, origin, false);
	}

	@NonNull
	public static PurchaseUiData createBackupSubscriptionUiData(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();

		String sku = null;
		String title;
		String purchaseType;
		int iconId = R.drawable.ic_action_osmand_pro_logo_colored;
		long purchaseTime = settings.BACKUP_PURCHASE_START_TIME.get();
		long expireTime = settings.BACKUP_PURCHASE_EXPIRE_TIME.get();
		SubscriptionState state = settings.BACKUP_PURCHASE_STATE.get();
		PurchaseOrigin origin = settings.BACKUP_SUBSCRIPTION_ORIGIN.get();
		if (origin == InAppPurchase.PurchaseOrigin.PROMO) {
			title = app.getString(R.string.promo);
			purchaseType = app.getString(R.string.promo_subscription);
		} else {
			title = app.getString(R.string.osmand_pro);
			PeriodUnit periodUnit = settings.BACKUP_PURCHASE_PERIOD.get();
			purchaseType = app.getString(periodUnit == PeriodUnit.YEAR ?
					R.string.annual_subscription : R.string.monthly_subscription);
		}

		boolean isLiveUpdateSubscription = false;
		boolean autoRenewing = false;
		boolean renewVisible = false;
		boolean isSubscription = true;

		return new PurchaseUiData(sku, title, iconId, purchaseType,
				expireTime, purchaseTime, isSubscription,
				isLiveUpdateSubscription, autoRenewing,
				renewVisible, state, origin, false);
	}

	public static boolean shouldShowBackupSubscription(@NonNull OsmandApplication app,
	                                                   @NonNull List<InAppPurchase> mainPurchases) {
		OsmandSettings settings = app.getSettings();
		if (settings.BACKUP_PURCHASE_ACTIVE.get()) {
			InAppPurchaseHelper helper = app.getInAppPurchaseHelper();
			InAppPurchases purchases = helper.getInAppPurchases();
			for (InAppPurchase purchase : mainPurchases) {
				if (purchases.isOsmAndProSubscription(purchase)) {
					String sku = purchase.getSku();
					return settings.BACKUP_SUBSCRIPTION_ORIGIN.get() != helper.getPurchaseOriginBySku(sku);
				}
			}
			return true;
		}
		return false;
	}

	public static boolean shouldShowFreeAccRegistration(@NonNull OsmandApplication app) {
		boolean proAvailable = InAppPurchaseHelper.isOsmAndProAvailable(app);
		boolean isRegistered = app.getBackupHelper().isRegistered();
		return !proAvailable && isRegistered;
	}

	@NonNull
	public static PurchaseUiData createFreeAccPurchaseUiData(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		String sku = null;
		String title = app.getString(R.string.osmand_start);
		String purchaseType = app.getString(R.string.free_account);
		int iconId = R.drawable.ic_action_osmand_start;
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
		Date purchaseDate = null;
		try {
			purchaseDate = dateFormat.parse(settings.BACKUP_ACCESS_TOKEN_UPDATE_TIME.get());
		} catch (Exception error) {
		}
		long purchaseTime = purchaseDate == null ? 0 : purchaseDate.getTime();
		long expireTime = 0;
		SubscriptionState state = ACTIVE;
		PurchaseOrigin origin = PurchaseOrigin.UNDEFINED;
		boolean isLiveUpdateSubscription = false;
		boolean autoRenewing = false;
		boolean renewVisible = false;
		boolean isSubscription = false;
		return new PurchaseUiData(sku, title, iconId, purchaseType,
				expireTime, purchaseTime, isSubscription,
				isLiveUpdateSubscription, autoRenewing,
				renewVisible, state, origin, true);
	}
}
