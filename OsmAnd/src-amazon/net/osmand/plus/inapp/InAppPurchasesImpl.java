package net.osmand.plus.inapp;

import net.osmand.plus.OsmandApplication;

public class InAppPurchasesImpl extends InAppPurchases {

	private static final InAppSubscription[] LIVE_UPDATES_FREE = new InAppSubscription[] {};

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		inAppPurchases = new InAppPurchase[] {};
		liveUpdates = new LiveUpdatesInAppPurchasesFree();
	}

	@Override
	public boolean isFullVersion(String sku) {
		return false;
	}

	@Override
	public boolean isDepthContours(String sku) {
		return false;
	}

	@Override
	public boolean isContourLines(String sku) {
		return false;
	}

	@Override
	public boolean isLiveUpdates(String sku) {
		return false;
	}

	private static class LiveUpdatesInAppPurchasesFree extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFree() {
			super(LIVE_UPDATES_FREE);
		}
	}
}