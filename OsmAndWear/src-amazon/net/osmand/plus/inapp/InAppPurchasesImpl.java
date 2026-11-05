package net.osmand.plus.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	private static final InAppSubscription[] SUBSCRIPTIONS_FREE = new InAppSubscription[]{
			new InAppPurchaseOsmAndProMonthlyFree(),
			new InAppPurchaseOsmAndProAnnualFree(),
			new InAppPurchaseMapsAnnualFree()
	};

	private static final InAppSubscription[] SUBSCRIPTIONS_FULL = new InAppSubscription[]{
			new InAppPurchaseOsmAndProMonthlyFull(),
			new InAppPurchaseOsmAndProAnnualFull(),
	};

	public InAppPurchasesImpl(OsmandApplication ctx) {
		super(ctx);
		fullVersion = FULL_VERSION;
		depthContours = null;
		contourLines = null;
		inAppPurchases = new InAppPurchase[]{FULL_VERSION};

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
	}

	@Override
	public boolean isFullVersion(InAppPurchase p) {
		return FULL_VERSION.getSku().equals(p.getSku());
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

	private static class InAppPurchaseFullVersion extends InAppPurchase {

		private static final String SKU_FULL_VERSION_PRICE = "net.osmand.amazon.maps.inapp";

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

	private static class InAppPurchaseOsmAndProMonthlyFree extends InAppPurchaseMonthlySubscription {

		private static final String SKU_OSMAND_PRO_MONTHLY_FREE = "net.osmand.amazon.pro.monthly";

		InAppPurchaseOsmAndProMonthlyFree() {
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_MONTHLY_FREE);
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

		private static final String SKU_OSMAND_PRO_ANNUAL_FREE = "net.osmand.amazon.pro.annual";

		InAppPurchaseOsmAndProAnnualFree() {
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_ANNUAL_FREE);
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

		private static final String SKU_MAPS_ANNUAL_FREE = "net.osmand.amazon.maps.annual";

		InAppPurchaseMapsAnnualFree() {
			super(MAPS_ID, SKU_MAPS_ANNUAL_FREE);
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

	private static class InAppPurchaseOsmAndProMonthlyFull extends InAppPurchaseMonthlySubscription {

		private static final String SKU_OSMAND_PRO_MONTHLY_FULL = "net.osmand.plus.amazon.pro.monthly";

		InAppPurchaseOsmAndProMonthlyFull() {
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_MONTHLY_FULL);
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
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProMonthlyFree(sku) : null;
		}
	}

	private static class InAppPurchaseOsmAndProAnnualFull extends InAppPurchaseAnnualSubscription {

		private static final String SKU_OSMAND_PRO_ANNUAL_FULL = "net.osmand.plus.amazon.pro.annual";

		InAppPurchaseOsmAndProAnnualFull() {
			super(OSMAND_PRO_ID, SKU_OSMAND_PRO_ANNUAL_FULL);
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
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseOsmAndProAnnualFree(sku) : null;
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