package net.osmand.plus.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class InAppPurchasesImpl extends InAppPurchases {

	private static final int FULL_VERSION_ID = 1;
	private static final int DEPTH_CONTOURS_ID = 2;
	private static final int CONTOUR_LINES_ID = 3;

	private static final int LIVE_UPDATES_ID = 5;
	private static final int OSMAND_PRO_ID = 6;
	private static final int MAPS_ID = 7;

	private static final int[] OSMAND_PRO_SCOPE = new int[]{
			FULL_VERSION_ID,
			DEPTH_CONTOURS_ID,
			CONTOUR_LINES_ID,
			LIVE_UPDATES_ID,
	};

	private static final int[] MAPS_SCOPE = new int[]{
			FULL_VERSION_ID,
	};

	private static final InAppSubscription[] SUBSCRIPTIONS_FREE = new InAppSubscription[]{
			new InAppPurchaseOsmAndProMonthlyFree(),
			new InAppPurchaseOsmAndProAnnualFree(),
			new InAppPurchaseMapsAnnualFree()
	};

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		fullVersion = null;
		depthContours = null;
		contourLines = null;
		inAppPurchases = new InAppPurchase[0];

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

	private static class InAppPurchaseOsmAndProMonthlyFree extends InAppPurchaseMonthlySubscription {

		private static final String SKU_OSMAND_PRO_MONTHLY_FREE = "net.osmand.amazon.monthly.pro";

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

	private static class InAppPurchaseOsmAndProAnnualFree extends InAppPurchaseAnnualSubscription {

		private static final String SKU_OSMAND_PRO_ANNUAL_FREE = "net.osmand.amazon.annual.pro";

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

		private static final String SKU_MAPS_ANNUAL_FREE = "net.osmand.amazon.annual.maps";

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

		@Override
		public String getRegularPrice(@NonNull Context ctx, @NonNull InAppSubscription monthlyLiveUpdates) {
			double regularPrice = getPriceValue();
			return getFormattedPrice(ctx, regularPrice, getPriceCurrencyCode());
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
}