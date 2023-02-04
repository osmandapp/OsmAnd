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
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.settings.backend.OsmandSettings;

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
		boolean isLiveUpdateSubscription = purchases.isLiveUpdatesSubscription(purchase);
		boolean autoRenewing = false;
		boolean renewVisible = false;
		SubscriptionState state = null;
		SubscriptionOrigin origin = purchaseHelper.getSubscriptionOriginBySku(sku);
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

			state = subscription.getState();
			if (subscription.isPurchased() && subscription.getPurchaseInfo() != null) {
				autoRenewing = subscription.getPurchaseInfo().isAutoRenewing();
				state = ACTIVE;
			} else if (state != UNDEFINED) {
				autoRenewing = state == ACTIVE || state == IN_GRACE_PERIOD;
			}
			expireTime = subscription.getExpireTime();
			if (expireTime == 0) {
				expireTime = subscription.getCalculatedExpiredTime();
			}
			if (!autoRenewing && state != ACTIVE && state != CANCELLED) {
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
				isLiveUpdateSubscription, autoRenewing,
				renewVisible, state, origin);
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
		SubscriptionOrigin origin = settings.BACKUP_SUBSCRIPTION_ORIGIN.get();
		if (origin == SubscriptionOrigin.PROMO) {
			title = app.getString(R.string.promo);
			purchaseType = app.getString(R.string.promo_subscription);
		} else {
			title = app.getString(R.string.osmand_pro);
			PeriodUnit periodUnit = settings.BACKUP_PURCHASE_PERIOD.get();
			purchaseType = app.getString(periodUnit == PeriodUnit.YEAR ?
					R.string.annual_subscription: R.string.monthly_subscription);
		}

		boolean isLiveUpdateSubscription = false;
		boolean autoRenewing = false;
		boolean renewVisible = false;
		boolean isSubscription = true;

		return new PurchaseUiData(sku, title, iconId, purchaseType,
				expireTime, purchaseTime, isSubscription,
				isLiveUpdateSubscription, autoRenewing,
				renewVisible, state, origin);
	}

}
