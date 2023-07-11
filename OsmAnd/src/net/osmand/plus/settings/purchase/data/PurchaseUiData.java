package net.osmand.plus.settings.purchase.data;


import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;

public class PurchaseUiData {

	private final String sku;
	private final String title;
	private final int iconId;
	private final String purchaseType;
	private final long expireTime;
	private final long purchaseTime;
	private final boolean isFreeAccountSubscription;
	private final boolean isSubscription;
	private final boolean isLiveUpdateSubscription;
	private final boolean isAutoRenewing;
	private final boolean isRenewVisible;
	private final SubscriptionState subscriptionState;
	private final PurchaseOrigin origin;

	public PurchaseUiData(String sku, @NonNull String title, @DrawableRes int iconId,
	                      @NonNull String purchaseType, long expireTime, long purchaseTime,
	                      boolean isSubscription, boolean isLiveUpdateSubscription,
	                      boolean isAutoRenewing, boolean isRenewVisible,
	                      @NonNull SubscriptionState subscriptionState, @NonNull PurchaseOrigin origin, boolean isFreeAccountSubscription) {
		this.sku = sku;
		this.title = title;
		this.iconId = iconId;
		this.purchaseType = purchaseType;
		this.expireTime = expireTime;
		this.purchaseTime = purchaseTime;
		this.isSubscription = isSubscription;
		this.isLiveUpdateSubscription = isLiveUpdateSubscription;
		this.isAutoRenewing = isAutoRenewing;
		this.isRenewVisible = isRenewVisible;
		this.subscriptionState = subscriptionState;
		this.origin = origin;
		this.isFreeAccountSubscription = isFreeAccountSubscription;
	}

	@NonNull
	public String getSku() {
		return sku;
	}

	@NonNull
	public String getTitle() {
		return title;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getPurchaseType() {
		return purchaseType;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public long getPurchaseTime() {
		return purchaseTime;
	}

	public boolean isAutoRenewing() {
		return isAutoRenewing;
	}

	public boolean isRenewVisible() {
		return isRenewVisible;
	}

	@NonNull
	public SubscriptionState getSubscriptionState() {
		return subscriptionState;
	}

	@NonNull
	public PurchaseOrigin getOrigin() {
		return origin;
	}

	public boolean isSubscription() {
		return isSubscription;
	}

	public boolean isLiveUpdateSubscription() {
		return isLiveUpdateSubscription;
	}

	public boolean isPromo() {
		return origin == PurchaseOrigin.PROMO;
	}

	public boolean isFreeAccountSubscription() {
		return isFreeAccountSubscription;
	}

}
