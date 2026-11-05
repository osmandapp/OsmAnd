package net.osmand.plus.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.ProductDetails;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

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
	private static final InAppPurchase DEPTH_CONTOURS_FULL = new InAppPurchaseDepthContoursFull();
	private static final InAppPurchase DEPTH_CONTOURS_FREE = new InAppPurchaseDepthContoursFree();
	private static final InAppPurchase CONTOUR_LINES_FULL = new InAppPurchaseContourLinesFull();
	private static final InAppPurchase CONTOUR_LINES_FREE = new InAppPurchaseContourLinesFree();

	private static final InAppSubscription[] SUBSCRIPTIONS_FULL = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesOldMonthlyFull(),
			new InAppPurchaseLiveUpdatesMonthlyFull(),
			new InAppPurchaseLiveUpdates3MonthsFull(),
			new InAppPurchaseLiveUpdatesAnnualFull(),

			new InAppPurchaseOsmAndProMonthlyFull(),
			new InAppPurchaseOsmAndProAnnualFull()
	};

	private static final InAppSubscription[] SUBSCRIPTIONS_FREE = new InAppSubscription[]{
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
			subscriptions = new SubscriptionsPurchasesFree();
		} else {
			subscriptions = new SubscriptionsPurchasesFull();
		}
		for (InAppSubscription s : subscriptions.getAllSubscriptions()) {
			if (s instanceof InAppPurchaseMonthlySubscription) {
				if (s.isLegacy()) {
					legacyMonthlySubscription = s;
				} else {
					monthlySubscription = s;
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
	public boolean isFullVersion(InAppPurchase p) {
		return FULL_VERSION.getSku().equals(p.getSku());
	}

	@Override
	public boolean isDepthContours(InAppPurchase p) {
		return DEPTH_CONTOURS_FULL.getSku().equals(p.getSku()) || DEPTH_CONTOURS_FREE.getSku().equals(p.getSku());
	}

	@Override
	public boolean isContourLines(InAppPurchase p) {
		return CONTOUR_LINES_FULL.getSku().equals(p.getSku()) || CONTOUR_LINES_FREE.getSku().equals(p.getSku());
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

		private static final String SKU_FULL_VERSION_PRICE = "osmand_full_version_price";

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

	private static class InAppPurchaseDepthContoursFull extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FULL = "net.osmand.seadepth_plus";

		InAppPurchaseDepthContoursFull() {
			super(DEPTH_CONTOURS_ID, SKU_DEPTH_CONTOURS_FULL);
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

	private static class InAppPurchaseDepthContoursFree extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.seadepth";

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

	private static class InAppPurchaseContourLinesFull extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FULL = "net.osmand.contourlines_plus";

		InAppPurchaseContourLinesFull() {
			super(CONTOUR_LINES_ID, SKU_CONTOUR_LINES_FULL);
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

		private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.contourlines";

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

	private static class InAppPurchaseLiveUpdatesMonthlyFull extends InAppPurchaseMonthlySubscription {

		private static final String SKU_LIVE_UPDATES_MONTHLY_FULL = "osm_live_subscription_monthly_full";

		InAppPurchaseLiveUpdatesMonthlyFull() {
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_MONTHLY_FULL, 1);
		}

		private InAppPurchaseLiveUpdatesMonthlyFull(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_MONTHLY_FREE, 1);
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
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_3_MONTHS_FULL, 1);
		}

		private InAppPurchaseLiveUpdates3MonthsFull(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_3_MONTHS_FREE, 1);
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
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_ANNUAL_FULL, 1);
		}

		private InAppPurchaseLiveUpdatesAnnualFull(@NonNull String sku) {
			super(LIVE_UPDATES_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_ANNUAL_FREE, 1);
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
			return true;
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
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_OLD_MONTHLY_FULL);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}
	}

	private static class InAppPurchaseLiveUpdatesOldMonthlyFree extends InAppPurchaseLiveUpdatesOldMonthly {

		private static final String SKU_LIVE_UPDATES_OLD_MONTHLY_FREE = "osm_free_live_subscription_2";

		InAppPurchaseLiveUpdatesOldMonthlyFree() {
			super(LIVE_UPDATES_ID, SKU_LIVE_UPDATES_OLD_MONTHLY_FREE);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}
	}

	public static class InAppPurchaseLiveUpdatesOldSubscription extends InAppSubscription {

		private final ProductDetails details;

		InAppPurchaseLiveUpdatesOldSubscription(@NonNull ProductDetails details) {
			super(LIVE_UPDATES_ID, details.getProductId());
			this.details = details;
		}

		@NonNull
		@Override
		public int[] getScope() {
			return LIVE_UPDATES_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return true;
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
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_MONTHLY_FULL, 1);
		}

		private InAppPurchaseOsmAndProMonthlyFull(@NonNull String sku) {
			super(OSMAND_PRO_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return OSMAND_PRO_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
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
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_MONTHLY_FREE, 1);
		}

		private InAppPurchaseOsmAndProMonthlyFree(@NonNull String sku) {
			super(OSMAND_PRO_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return OSMAND_PRO_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
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
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_ANNUAL_FULL, 1);
		}

		private InAppPurchaseOsmAndProAnnualFull(@NonNull String sku) {
			super(OSMAND_PRO_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return OSMAND_PRO_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_monthly_price);
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
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_ANNUAL_FREE, 1);
		}

		private InAppPurchaseOsmAndProAnnualFree(@NonNull String sku) {
			super(OSMAND_PRO_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return OSMAND_PRO_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_monthly_price);
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
			super(MAPS_ID, SKU_MAPS_ANNUAL_FREE, 1);
		}

		private InAppPurchaseMapsAnnualFree(@NonNull String sku) {
			super(MAPS_ID, sku);
		}

		@NonNull
		@Override
		public int[] getScope() {
			return MAPS_SCOPE;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_maps_plus_annual_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_maps_plus_annual_monthly_price);
		}

		@Override
		public int getDiscountPercent(@NonNull InAppSubscription monthlyLiveUpdates) {
			InAppSubscriptionIntroductoryInfo introductoryInfo = getIntroductoryInfo();
			if (introductoryInfo != null) {
				double regularPrice = getPriceValue();
				double introductoryPrice = introductoryInfo.getIntroductoryPriceValue();
				if (introductoryPrice >= 0 && introductoryPrice < regularPrice) {
					return (int) ((1 - introductoryPrice / regularPrice) * 100d);
				}
			}
			return 0;
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseMapsAnnualFree(sku) : null;
		}
	}

	private static class SubscriptionsPurchasesFree extends InAppSubscriptionList {

		public SubscriptionsPurchasesFree() {
			super(SUBSCRIPTIONS_FREE);
		}
	}

	private static class SubscriptionsPurchasesFull extends InAppSubscriptionList {

		public SubscriptionsPurchasesFull() {
			super(SUBSCRIPTIONS_FULL);
		}
	}
}
