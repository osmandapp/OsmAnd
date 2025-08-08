package net.osmand.plus.settings.purchase.data;

import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.HMD_PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.HUGEROCK_PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.TRIPLTEK_PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.ACTIVE;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.CANCELLED;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.EXPIRED;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.IN_GRACE_PERIOD;
import static net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState.UNDEFINED;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.Period.PeriodUnit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseUiDataUtils {

	public static final int UNDEFINED_TIME = -1;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

	@Nullable
	public static PurchaseUiData createUiData(@NonNull OsmandApplication app, @NonNull InAppPurchase purchase) {
		long expireTime = UNDEFINED_TIME;
		SubscriptionState subscriptionState = null;
		if (purchase instanceof InAppSubscription) {
			expireTime = ((InAppSubscription) purchase).getExpireTime();
			subscriptionState = ((InAppSubscription) purchase).getState();
		}
		return createUiData(app, purchase, null, null, purchase.getPurchaseTime(), expireTime,
				app.getInAppPurchaseHelper().getPurchaseOriginBySku(purchase.getSku()), subscriptionState);
	}

	@SuppressLint("DiscouragedApi")
	@Nullable
	public static PurchaseUiData createUiData(@NonNull OsmandApplication app,
											  @NonNull InAppPurchase purchase,
											  @Nullable String name, @Nullable String icon,
											  long purchaseTime, long expireTime,
											  @NonNull PurchaseOrigin origin,
											  @Nullable SubscriptionState subscriptionState) {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		InAppPurchases purchases = purchaseHelper.getInAppPurchases();

		String sku = purchase.getSku();
		String title = name;
		int iconId = !Algorithms.isEmpty(icon) ? app.getResources().getIdentifier(icon, "drawable", app.getPackageName()) : 0;
		String purchaseType;
		boolean liveUpdateSubscription = purchases.isLiveUpdates(purchase);
		boolean autoRenewing = false;
		boolean renewVisible = false;
		if (subscriptionState == null) {
			subscriptionState = UNDEFINED;
		}
		boolean isSubscription = purchase instanceof InAppSubscription;

		if (purchases.isOsmAndPro(purchase)) {
			title = Algorithms.isEmpty(title) ? app.getString(R.string.osmand_pro) : title;
			iconId = iconId == 0 ? R.drawable.ic_action_osmand_pro_logo_colored : iconId;
		} else if (purchases.isLiveUpdates(purchase)) {
			title = Algorithms.isEmpty(title) ? app.getString(R.string.osm_live) : title;
			iconId = iconId == 0 ? R.drawable.ic_action_subscription_osmand_live : iconId;
		} else if (purchases.isMaps(purchase) || purchases.isFullVersion(purchase)) {
			title = Algorithms.isEmpty(title) ? app.getString(R.string.maps_plus) : title;
			iconId = iconId == 0 ? R.drawable.ic_action_osmand_maps_plus : iconId;
		} else {
			return null;
		}

		if (isSubscription) {
			InAppSubscription subscription = (InAppSubscription) purchase;
			purchaseType = app.getString(subscription.getPeriodTypeString());

			if (origin == purchaseHelper.getPlatformOrigin()) {
				subscriptionState = subscription.getState();
				if (subscription.isPurchased() && subscription.getPurchaseInfo() != null) {
					autoRenewing = subscription.getPurchaseInfo().isAutoRenewing();
					subscriptionState = ACTIVE;
				} else if (subscriptionState != UNDEFINED) {
					autoRenewing = subscriptionState == ACTIVE || subscriptionState == IN_GRACE_PERIOD;
				}
				if (expireTime == UNDEFINED_TIME) {
					expireTime = subscription.getExpireTime();
					if (expireTime == 0) {
						expireTime = subscription.getCalculatedExpiredTime();
					}
				}
				if (!autoRenewing && subscriptionState != ACTIVE && subscriptionState != CANCELLED) {
					if (purchases.isMaps(subscription)) {
						boolean isFullVersion = !Version.isFreeVersion(app) || InAppPurchaseUtils.isFullVersionAvailable(app, false);
						renewVisible = !isFullVersion && !InAppPurchaseUtils.isSubscribedToAny(app, false);
					} else if (purchases.isOsmAndPro(subscription)) {
						renewVisible = !InAppPurchaseUtils.isOsmAndProAvailable(app, false);
					}
				}
			}
		} else {
			purchaseType = app.getString(R.string.in_app_purchase_desc);
		}

		return new PurchaseUiData(sku, title, iconId, purchaseType,
				expireTime, purchaseTime, isSubscription,
				liveUpdateSubscription, autoRenewing,
				renewVisible, subscriptionState, origin);
	}

	@NonNull
	public static PurchaseUiData createBackupSubscriptionUiData(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();

		String sku = settings.BACKUP_SUBSCRIPTION_SKU.get();
		String title;
		String purchaseType;
		int iconId = R.drawable.ic_action_osmand_pro_logo_colored;
		long purchaseTime = settings.BACKUP_PURCHASE_START_TIME.get();
		long expireTime = settings.BACKUP_PURCHASE_EXPIRE_TIME.get();
		SubscriptionState state = settings.BACKUP_PURCHASE_STATE.get();
		PurchaseOrigin origin = settings.BACKUP_SUBSCRIPTION_ORIGIN.get();
		if (origin == PROMO) {
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
				renewVisible, state, origin);
	}

	public static boolean shouldShowBackupSubscription(@NonNull OsmandApplication app,
	                                                   @NonNull List<InAppPurchase> mainPurchases) {
		OsmandSettings settings = app.getSettings();
		if (settings.BACKUP_SUBSCRIPTION_ORIGIN.get() != PurchaseOrigin.UNDEFINED) {
			InAppPurchaseHelper helper = app.getInAppPurchaseHelper();
			InAppPurchases purchases = helper.getInAppPurchases();
			for (InAppPurchase purchase : mainPurchases) {
				if (purchases.isOsmAndPro(purchase)) {
					String sku = purchase.getSku();
					return settings.BACKUP_SUBSCRIPTION_ORIGIN.get() != helper.getPurchaseOriginBySku(sku);
				}
			}
			return true;
		}
		return false;
	}

	public static boolean shouldShowFreeAccRegistration(@NonNull OsmandApplication app) {
		boolean available = InAppPurchaseUtils.isBackupAvailable(app);
		boolean isRegistered = app.getBackupHelper().isRegistered();
		return !available && isRegistered;
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
				renewVisible, state, origin);
	}

	@NonNull
	public static PurchaseUiData createTripltekPurchaseUiData(@NonNull OsmandApplication app) {
		long expireTime = InAppPurchaseUtils.getTripltekPromoExpirationTime(app);
		SubscriptionState state = InAppPurchaseUtils.isTripltekPromoAvailable(app) ? ACTIVE : EXPIRED;

		return createPromoUiData(app, TRIPLTEK_PROMO, state, expireTime);
	}

	@NonNull
	public static PurchaseUiData createHugerockPurchaseUiData(@NonNull OsmandApplication app) {
		long expireTime = InAppPurchaseUtils.getHugerockPromoExpirationTime(app);
		SubscriptionState state = InAppPurchaseUtils.isHugerockPromoAvailable(app) ? ACTIVE : EXPIRED;

		return createPromoUiData(app, HUGEROCK_PROMO, state, expireTime);
	}

	@NonNull
	public static PurchaseUiData createHMDPurchaseUiData(@NonNull OsmandApplication app) {
		long expireTime = InAppPurchaseUtils.getHMDPromoExpirationTime(app);
		SubscriptionState state = InAppPurchaseUtils.isHMDPromoAvailable(app) ? ACTIVE : EXPIRED;

		return createPromoUiData(app, HMD_PROMO, state, expireTime);
	}

	@NonNull
	public static PurchaseUiData createPromoUiData(@NonNull OsmandApplication app, @NonNull PurchaseOrigin origin,
	                                               @NonNull SubscriptionState state, long expireTime) {
		long installTime = Version.getInstallTime(app);
		String title = app.getString(origin.getStoreNameId());
		String purchaseType = app.getString(R.string.free_account);

		return new PurchaseUiData(null, title, R.drawable.ic_action_osmand_start, purchaseType,
				expireTime, installTime, false, false,
				false, false, state, origin);
	}

	@NonNull
	public static Pair<String, String> parseSubscriptionState(@NonNull OsmandApplication app,
	                                                          @NonNull SubscriptionState state,
	                                                          long startTime, long expireTime) {
		String title;
		String desc;
		switch (state) {
			case ACTIVE:
			case CANCELLED:
			case IN_GRACE_PERIOD:
				title = expireTime > 0 ? app.getString(R.string.shared_string_expires) : app.getString(R.string.shared_string_purchased);
				desc = expireTime > 0 ? DATE_FORMAT.format(expireTime) : DATE_FORMAT.format(startTime);
				break;
			case EXPIRED:
				title = app.getString(R.string.expired);
				desc = DATE_FORMAT.format(expireTime);
				break;
			case ON_HOLD:
				title = app.getString(R.string.on_hold_since, "");
				desc = DATE_FORMAT.format(startTime);
				break;
			case PAUSED:
				title = app.getString(R.string.shared_string_paused);
				desc = DATE_FORMAT.format(expireTime);
				break;
			default:
				title = app.getString(R.string.shared_string_undefined);
				desc = "";
		}
		return new Pair<>(title, desc);
	}
}
