package net.osmand.plus.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.iap.entity.ProductInfo;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class InAppPurchasesImpl extends InAppPurchases {

	private static final int FULL_VERSION_ID = 1;
	private static final int DEPTH_CONTOURS_ID = 2;
	private static final int CONTOUR_LINES_ID = 3;

	private static final int LIVE_UPDATES_ID = 5;
	private static final int OSMAND_PRO_ID = 6;
	private static final int MAPS_ID = 7;

	private static final int[] LIVE_UPDATES_SCOPE = new int[]{
			FULL_VERSION_ID,
			DEPTH_CONTOURS_ID,
			CONTOUR_LINES_ID,
	};

	private static final int[] OSMAND_PRO_SCOPE = new int[]{
			FULL_VERSION_ID,
			DEPTH_CONTOURS_ID,
			CONTOUR_LINES_ID,
			LIVE_UPDATES_ID,
	};

	private static final int[] MAPS_SCOPE = new int[]{
			FULL_VERSION_ID,
	};

	private static final InAppPurchase FULL_VERSION = new InAppPurchaseFullVersion();
	private static final InAppPurchaseDepthContoursFree DEPTH_CONTOURS_FREE = new InAppPurchaseDepthContoursFree();
	private static final InAppPurchaseContourLinesFree CONTOUR_LINES_FREE = new InAppPurchaseContourLinesFree();
	
	private static final InAppSubscription[] SUBSCRIPTIONS_FREE = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesMonthlyFree(),
			new InAppPurchaseLiveUpdates3MonthsFree(),
			new InAppPurchaseLiveUpdatesAnnualFree()
	};

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		fullVersion = FULL_VERSION;
		depthContours = DEPTH_CONTOURS_FREE;
		contourLines = CONTOUR_LINES_FREE;
		inAppPurchases = new InAppPurchase[] { fullVersion, depthContours, contourLines };

		subscriptions = new SubscriptionsPurchasesFree();
		for (InAppSubscription s : subscriptions.getAllSubscriptions()) {
			if (s instanceof InAppPurchaseMonthlySubscription) {
				if (s.isLegacy()) {
					legacyMonthlyLiveUpdates = s;
				} else {
					monthlyLiveUpdates = s;
				}
			}
		}
	}

	@Override
	public boolean isFullVersion(InAppPurchase p) {
		return FULL_VERSION.getSku().equals(p.getSku());
	}

	@Override
	public boolean isDepthContours(InAppPurchase p) {
		return DEPTH_CONTOURS_FREE.getSku().equals(p.getSku());
	}

	@Override
	public boolean isContourLines(InAppPurchase p) {
		return CONTOUR_LINES_FREE.getSku().equals(p.getSku());
	}

	@Override
	public boolean isLiveUpdatesSubscription(InAppPurchase p) {
		return p.getFeatureId() == LIVE_UPDATES_ID;
	}

	@Override
	public boolean isOsmAndProSubscription(InAppPurchase p) {
		return p.getFeatureId() == OSMAND_PRO_ID;
	}

	@Override
	public boolean isMapsSubscription(InAppPurchase p) {
		return p.getFeatureId() == MAPS_ID;
	}

	private static class InAppPurchaseFullVersion extends InAppPurchase {

		private static final String SKU_FULL_VERSION_PRICE = "net.osmand.huawei.full";

		InAppPurchaseFullVersion() {
			super(FULL_VERSION_ID, SKU_FULL_VERSION_PRICE);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return new int[0];
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.full_version_price);
		}
	}

	private static class InAppPurchaseDepthContoursFree extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.huawei.seadepth";

		InAppPurchaseDepthContoursFree() {
			super(DEPTH_CONTOURS_ID, SKU_DEPTH_CONTOURS_FREE);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return new int[0];
		}

		@Override
		public boolean isLegacy() {
			return false;
		}
	}

	private static class InAppPurchaseContourLinesFree extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.huawei.contourlines";

		InAppPurchaseContourLinesFree() {
			super(CONTOUR_LINES_ID, SKU_CONTOUR_LINES_FREE);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return new int[0];
		}

		@Override
		public boolean isLegacy() {
			return false;
		}
	}

	private static class InAppPurchaseLiveUpdatesMonthlyFree extends InAppPurchaseMonthlySubscription {

		private static final String SKU_LIVE_UPDATES_MONTHLY_HW_FREE = "net.osmand.huawei.monthly";

		InAppPurchaseLiveUpdatesMonthlyFree() {
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_MONTHLY_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdatesMonthlyFree(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesMonthlyFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdates3MonthsFree extends InAppPurchaseQuarterlySubscription {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_HW_FREE = "net.osmand.huawei.3months";

		InAppPurchaseLiveUpdates3MonthsFree() {
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_3_MONTHS_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdates3MonthsFree(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdates3MonthsFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesAnnualFree extends InAppPurchaseAnnualSubscription {

		private static final String SKU_LIVE_UPDATES_ANNUAL_HW_FREE = "net.osmand.huawei.annual";

		InAppPurchaseLiveUpdatesAnnualFree() {
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_ANNUAL_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdatesAnnualFree(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesAnnualFree(sku) : null;
		}
	}

	public static class InAppPurchaseLiveUpdatesOldSubscription extends InAppSubscription {

		private final ProductInfo info;

		InAppPurchaseLiveUpdatesOldSubscription(@NonNull ProductInfo info) {
			super(LIVE_UPDATES_ID, info.getProductId());
			this.info = info;
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public int getPeriodTypeString() {
			return R.string.monthly_subscription;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return "";
		}

		@Override
		public CharSequence getTitle(Context ctx) {
			return info.getProductName();
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			return info.getProductDesc();
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return null;
		}
	}

	private static class SubscriptionsPurchasesFree extends InAppSubscriptionList {

		public SubscriptionsPurchasesFree() {
			super(SUBSCRIPTIONS_FREE);
		}
	}
}
