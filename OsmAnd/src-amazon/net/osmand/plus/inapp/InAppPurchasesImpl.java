package net.osmand.plus.inapp;

import net.osmand.plus.OsmandApplication;

public class InAppPurchasesImpl extends InAppPurchases {

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		inAppPurchases = new InAppPurchase[] {};
		subscriptions = new EmptyLiveUpdatesList();
	}

	@Override
	public boolean isFullVersion(InAppPurchase p) {
		return false;
	}

	@Override
	public boolean isDepthContours(InAppPurchase p) {
		return false;
	}

	@Override
	public boolean isContourLines(InAppPurchase p) {
		return false;
	}

	@Override
	public boolean isLiveUpdatesSubscription(InAppPurchase p) {
		return false;
	}

	@Override
	public boolean isOsmAndProSubscription(InAppPurchase p) {
		return false;
	}

	@Override
	public boolean isMapsSubscription(InAppPurchase p) {
		return false;
	}

	private static class EmptyLiveUpdatesList extends InAppSubscriptionList {

		public EmptyLiveUpdatesList() {
			super(new InAppSubscription[] {});
		}
	}
}