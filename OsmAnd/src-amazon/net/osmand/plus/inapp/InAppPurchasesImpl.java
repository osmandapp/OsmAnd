package net.osmand.plus.inapp;

import net.osmand.plus.OsmandApplication;

public class InAppPurchasesImpl extends InAppPurchases {

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		inAppPurchases = new InAppPurchase[] {};
		liveUpdates = new EmptyLiveUpdatesList();
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

	private static class EmptyLiveUpdatesList extends InAppSubscriptionList {

		public EmptyLiveUpdatesList() {
			super(new InAppSubscription[] {});
		}
	}
}