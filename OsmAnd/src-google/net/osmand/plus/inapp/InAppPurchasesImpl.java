package net.osmand.plus.inapp;

import android.content.Context;

import com.android.billingclient.api.SkuDetails;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InAppPurchasesImpl extends InAppPurchases {

	private static final InAppPurchase FULL_VERSION = new InAppPurchaseFullVersion();
	private static final InAppPurchaseDepthContoursFull DEPTH_CONTOURS_FULL = new InAppPurchaseDepthContoursFull();
	private static final InAppPurchaseDepthContoursFree DEPTH_CONTOURS_FREE = new InAppPurchaseDepthContoursFree();
	private static final InAppPurchaseContourLinesFull CONTOUR_LINES_FULL = new InAppPurchaseContourLinesFull();
	private static final InAppPurchaseContourLinesFree CONTOUR_LINES_FREE = new InAppPurchaseContourLinesFree();

	private static final InAppSubscription[] LIVE_UPDATES_FULL = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesOldMonthlyFull(),
			new InAppPurchaseLiveUpdatesMonthlyFull(),
			new InAppPurchaseLiveUpdates3MonthsFull(),
			new InAppPurchaseLiveUpdatesAnnualFull(),

			new InAppPurchaseOsmAndProMonthlyFull(),
			new InAppPurchaseOsmAndProAnnualFull()
	};

	private static final InAppSubscription[] LIVE_UPDATES_FREE = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesOldMonthlyFree(),
			new InAppPurchaseLiveUpdatesMonthlyFree(),
			new InAppPurchaseLiveUpdates3MonthsFree(),
			new InAppPurchaseLiveUpdatesAnnualFree(),

			new InAppPurchaseOsmAndProMonthlyFree(),
			new InAppPurchaseOsmAndProAnnualFree(),
			new InAppPurchaseMapsAnnualFree()
	};

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		fullVersion = FULL_VERSION;
		if (Version.isFreeVersion(ctx)) {
			subscriptions = new LiveUpdatesInAppPurchasesFree();
		} else {
			subscriptions = new LiveUpdatesInAppPurchasesFull();
		}
		for (InAppSubscription s : subscriptions.getAllSubscriptions()) {
			if (s instanceof InAppPurchaseMonthlySubscription) {
				if (s.isDiscounted()) {
					discountedMonthlyLiveUpdates = s;
				} else {
					monthlyLiveUpdates = s;
				}
			}
		}
		if (Version.isFreeVersion(ctx)) {
			depthContours = DEPTH_CONTOURS_FREE;
		} else {
			depthContours = DEPTH_CONTOURS_FULL;
		}
		if (Version.isFreeVersion(ctx)) {
			contourLines = CONTOUR_LINES_FREE;
		} else {
			contourLines = CONTOUR_LINES_FULL;
		}

		inAppPurchases = new InAppPurchase[] { fullVersion, depthContours, contourLines };
	}

	@Override
	public boolean isFullVersion(String sku) {
		return FULL_VERSION.getSku().equals(sku);
	}

	@Override
	public boolean isDepthContours(String sku) {
		return DEPTH_CONTOURS_FULL.getSku().equals(sku) || DEPTH_CONTOURS_FREE.getSku().equals(sku);
	}

	@Override
	public boolean isContourLines(String sku) {
		return CONTOUR_LINES_FULL.getSku().equals(sku) || CONTOUR_LINES_FREE.getSku().equals(sku);
	}

	@Override
	public boolean isLiveUpdates(String sku) {
		for (InAppPurchase p : LIVE_UPDATES_FULL) {
			if (p.getSku().equals(sku)) {
				return true;
			}
		}
		for (InAppPurchase p : LIVE_UPDATES_FREE) {
			if (p.getSku().equals(sku)) {
				return true;
			}
		}
		return false;
	}

	private static class InAppPurchaseFullVersion extends InAppPurchase {

		private static final String SKU_FULL_VERSION_PRICE = "osmand_full_version_price";

		InAppPurchaseFullVersion() {
			super(SKU_FULL_VERSION_PRICE);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.full_version_price);
		}
	}

	private static class InAppPurchaseDepthContoursFull extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FULL = "net.osmand.seadepth_plus";

		InAppPurchaseDepthContoursFull() {
			super(SKU_DEPTH_CONTOURS_FULL);
		}
	}

	private static class InAppPurchaseDepthContoursFree extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.seadepth";

		InAppPurchaseDepthContoursFree() {
			super(SKU_DEPTH_CONTOURS_FREE);
		}
	}

	private static class InAppPurchaseContourLinesFull extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FULL = "net.osmand.contourlines_plus";

		InAppPurchaseContourLinesFull() {
			super(SKU_CONTOUR_LINES_FULL);
		}
	}

	private static class InAppPurchaseContourLinesFree extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.contourlines";

		InAppPurchaseContourLinesFree() {
			super(SKU_CONTOUR_LINES_FREE);
		}
	}

	private static class InAppPurchaseLiveUpdatesMonthlyFull extends InAppPurchaseMonthlySubscription {

		private static final String SKU_LIVE_UPDATES_MONTHLY_FULL = "osm_live_subscription_monthly_full";

		InAppPurchaseLiveUpdatesMonthlyFull() {
			super(SKU_LIVE_UPDATES_MONTHLY_FULL, 1, true);
		}

		private InAppPurchaseLiveUpdatesMonthlyFull(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesMonthlyFull(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesMonthlyFree extends InAppPurchaseMonthlySubscription {

		private static final String SKU_LIVE_UPDATES_MONTHLY_FREE = "osm_live_subscription_monthly_free";

		InAppPurchaseLiveUpdatesMonthlyFree() {
			super(SKU_LIVE_UPDATES_MONTHLY_FREE, 1, true);
		}

		private InAppPurchaseLiveUpdatesMonthlyFree(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesMonthlyFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdates3MonthsFull extends InAppPurchaseQuarterlySubscription {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_FULL = "osm_live_subscription_3_months_full";

		InAppPurchaseLiveUpdates3MonthsFull() {
			super(SKU_LIVE_UPDATES_3_MONTHS_FULL, 1, true);
		}

		private InAppPurchaseLiveUpdates3MonthsFull(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdates3MonthsFull(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdates3MonthsFree extends InAppPurchaseQuarterlySubscription {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_FREE = "osm_live_subscription_3_months_free";

		InAppPurchaseLiveUpdates3MonthsFree() {
			super(SKU_LIVE_UPDATES_3_MONTHS_FREE, 1, true);
		}

		private InAppPurchaseLiveUpdates3MonthsFree(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdates3MonthsFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesAnnualFull extends InAppPurchaseAnnualSubscription {

		private static final String SKU_LIVE_UPDATES_ANNUAL_FULL = "osm_live_subscription_annual_full";

		InAppPurchaseLiveUpdatesAnnualFull() {
			super(SKU_LIVE_UPDATES_ANNUAL_FULL, 1, true);
		}

		private InAppPurchaseLiveUpdatesAnnualFull(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesAnnualFull(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesAnnualFree extends InAppPurchaseAnnualSubscription {

		private static final String SKU_LIVE_UPDATES_ANNUAL_FREE = "osm_live_subscription_annual_free";

		InAppPurchaseLiveUpdatesAnnualFree() {
			super(SKU_LIVE_UPDATES_ANNUAL_FREE, 1, true);
		}

		private InAppPurchaseLiveUpdatesAnnualFree(@NonNull String sku) {
			super(sku, true);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesAnnualFree(sku) : null;
		}
	}

	private static class InAppPurchaseLiveUpdatesOldMonthlyFull extends InAppPurchaseLiveUpdatesOldMonthly {

		private static final String SKU_LIVE_UPDATES_OLD_MONTHLY_FULL = "osm_live_subscription_2";

		InAppPurchaseLiveUpdatesOldMonthlyFull() {
			super(SKU_LIVE_UPDATES_OLD_MONTHLY_FULL);
		}
	}

	private static class InAppPurchaseLiveUpdatesOldMonthlyFree extends InAppPurchaseLiveUpdatesOldMonthly {

		private static final String SKU_LIVE_UPDATES_OLD_MONTHLY_FREE = "osm_free_live_subscription_2";

		InAppPurchaseLiveUpdatesOldMonthlyFree() {
			super(SKU_LIVE_UPDATES_OLD_MONTHLY_FREE);
		}
	}

	public static class InAppPurchaseLiveUpdatesOldSubscription extends InAppSubscription {

		private final SkuDetails details;

		InAppPurchaseLiveUpdatesOldSubscription(@NonNull SkuDetails details) {
			super(details.getSku(), true);
			this.details = details;
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
			return details.getTitle();
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			return details.getDescription();
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return null;
		}
	}

	private static class InAppPurchaseOsmAndProMonthlyFull extends InAppPurchaseMonthlySubscription {

		private static final String SKU_OSMAND_PRO_MONTHLY_FULL = "osmand_pro_monthly_full";

		InAppPurchaseOsmAndProMonthlyFull() {
			super(SKU_OSMAND_PRO_MONTHLY_FULL, 1, false);
		}

		private InAppPurchaseOsmAndProMonthlyFull(@NonNull String sku) {
			super(sku, false);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProMonthlyFull(sku) : null;
		}
	}

	private static class InAppPurchaseOsmAndProMonthlyFree extends InAppPurchaseMonthlySubscription {

		private static final String SKU_OSMAND_PRO_MONTHLY_FREE = "osmand_pro_monthly_free";

		InAppPurchaseOsmAndProMonthlyFree() {
			super(SKU_OSMAND_PRO_MONTHLY_FREE, 1, false);
		}

		private InAppPurchaseOsmAndProMonthlyFree(@NonNull String sku) {
			super(sku, false);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProMonthlyFree(sku) : null;
		}
	}

	private static class InAppPurchaseOsmAndProAnnualFull extends InAppPurchaseAnnualSubscription {

		private static final String SKU_OSMAND_PRO_ANNUAL_FULL = "osmand_pro_annual_full";

		InAppPurchaseOsmAndProAnnualFull() {
			super(SKU_OSMAND_PRO_ANNUAL_FULL, 1, false);
		}

		private InAppPurchaseOsmAndProAnnualFull(@NonNull String sku) {
			super(sku, false);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProAnnualFull(sku) : null;
		}
	}

	private static class InAppPurchaseOsmAndProAnnualFree extends InAppPurchaseAnnualSubscription {

		private static final String SKU_OSMAND_PRO_ANNUAL_FREE = "osmand_pro_annual_free";

		InAppPurchaseOsmAndProAnnualFree() {
			super(SKU_OSMAND_PRO_ANNUAL_FREE, 1, false);
		}

		private InAppPurchaseOsmAndProAnnualFree(@NonNull String sku) {
			super(sku, false);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProAnnualFree(sku) : null;
		}
	}

	private static class InAppPurchaseMapsAnnualFree extends InAppPurchaseAnnualSubscription {

		private static final String SKU_MAPS_ANNUAL_FREE = "osmand_maps_annual_free";

		InAppPurchaseMapsAnnualFree() {
			super(SKU_MAPS_ANNUAL_FREE, 1, false);
		}

		private InAppPurchaseMapsAnnualFree(@NonNull String sku) {
			super(sku, false);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseMapsAnnualFree(sku) : null;
		}
	}

	private static class LiveUpdatesInAppPurchasesFree extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFree() {
			super(LIVE_UPDATES_FREE);
		}
	}

	private static class LiveUpdatesInAppPurchasesFull extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFull() {
			super(LIVE_UPDATES_FULL);
		}
	}
}
