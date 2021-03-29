package net.osmand.plus.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.iap.entity.ProductInfo;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class InAppPurchasesImpl extends InAppPurchases {

	private static final InAppPurchase FULL_VERSION = new InAppPurchaseFullVersion();
	private static final InAppPurchaseDepthContoursFree DEPTH_CONTOURS_FREE = new InAppPurchaseDepthContoursFree();
	private static final InAppPurchaseContourLinesFree CONTOUR_LINES_FREE = new InAppPurchaseContourLinesFree();


	private static final InAppSubscription[] LIVE_UPDATES_FREE = new InAppSubscription[]{
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

		liveUpdates = new LiveUpdatesInAppPurchasesFree();
		for (InAppSubscription s : liveUpdates.getAllSubscriptions()) {
			if (s instanceof InAppPurchaseLiveUpdatesMonthly) {
				if (s.isDiscounted()) {
					discountedMonthlyLiveUpdates = s;
				} else {
					monthlyLiveUpdates = s;
				}
			}
		}
	}

	@Override
	public boolean isFullVersion(String sku) {
		return FULL_VERSION.getSku().equals(sku);
	}

	@Override
	public boolean isDepthContours(String sku) {
		return DEPTH_CONTOURS_FREE.getSku().equals(sku);
	}

	@Override
	public boolean isContourLines(String sku) {
		return CONTOUR_LINES_FREE.getSku().equals(sku);
	}

	@Override
	public boolean isLiveUpdates(String sku) {
		for (InAppPurchase p : LIVE_UPDATES_FREE) {
			if (p.getSku().equals(sku)) {
				return true;
			}
		}
		return false;
	}

	private static class InAppPurchaseFullVersion extends InAppPurchase {

		private static final String SKU_FULL_VERSION_PRICE = "net.osmand.huawei.full";

		InAppPurchaseFullVersion() {
			super(SKU_FULL_VERSION_PRICE);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.full_version_price);
		}
	}

	private static class InAppPurchaseDepthContoursFree extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.huawei.seadepth";

		InAppPurchaseDepthContoursFree() {
			super(SKU_DEPTH_CONTOURS_FREE);
		}
	}

	private static class InAppPurchaseContourLinesFree extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.huawei.contourlines";

		InAppPurchaseContourLinesFree() {
			super(SKU_CONTOUR_LINES_FREE);
		}
	}

	private static class InAppPurchaseLiveUpdatesMonthlyFree extends InAppPurchaseLiveUpdatesMonthly {

		private static final String SKU_LIVE_UPDATES_MONTHLY_HW_FREE = "net.osmand.huawei.monthly";

		InAppPurchaseLiveUpdatesMonthlyFree() {
			super(SKU_LIVE_UPDATES_MONTHLY_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdatesMonthlyFree(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesMonthlyFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdates3MonthsFree extends InAppPurchaseLiveUpdates3Months {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_HW_FREE = "net.osmand.huawei.3months";

		InAppPurchaseLiveUpdates3MonthsFree() {
			super(SKU_LIVE_UPDATES_3_MONTHS_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdates3MonthsFree(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdates3MonthsFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesAnnualFree extends InAppPurchaseLiveUpdatesAnnual {

		private static final String SKU_LIVE_UPDATES_ANNUAL_HW_FREE = "net.osmand.huawei.annual";

		InAppPurchaseLiveUpdatesAnnualFree() {
			super(SKU_LIVE_UPDATES_ANNUAL_HW_FREE, 1);
		}

		private InAppPurchaseLiveUpdatesAnnualFree(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesAnnualFree(sku) : null;
		}
	}

	public static class InAppPurchaseLiveUpdatesOldSubscription extends InAppSubscription {

		private ProductInfo info;

		InAppPurchaseLiveUpdatesOldSubscription(@NonNull ProductInfo info) {
			super(info.getProductId(), true);
			this.info = info;
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

	private static class LiveUpdatesInAppPurchasesFree extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFree() {
			super(LIVE_UPDATES_FREE);
		}
	}
}
