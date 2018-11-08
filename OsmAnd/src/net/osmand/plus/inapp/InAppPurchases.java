package net.osmand.plus.inapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.util.SkuDetails;
import net.osmand.util.Algorithms;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InAppPurchases {

	private static final InAppPurchase FULL_VERSION = new InAppPurchaseFullVersion();
	private static final InAppPurchaseDepthContoursFull DEPTH_CONTOURS_FULL = new InAppPurchaseDepthContoursFull();
	private static final InAppPurchaseDepthContoursFree DEPTH_CONTOURS_FREE = new InAppPurchaseDepthContoursFree();
	private static final InAppPurchaseContourLinesFull CONTOUR_LINES_FULL = new InAppPurchaseContourLinesFull();
	private static final InAppPurchaseContourLinesFree CONTOUR_LINES_FREE = new InAppPurchaseContourLinesFree();

	private static final InAppSubscription[] LIVE_UPDATES_FULL = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesOldMonthlyFull(),
			new InAppPurchaseLiveUpdatesMonthlyFull(),
			new InAppPurchaseLiveUpdates3MonthsFull(),
			new InAppPurchaseLiveUpdatesAnnualFull()
	};

	private static final InAppSubscription[] LIVE_UPDATES_FREE = new InAppSubscription[]{
			new InAppPurchaseLiveUpdatesOldMonthlyFree(),
			new InAppPurchaseLiveUpdatesMonthlyFree(),
			new InAppPurchaseLiveUpdates3MonthsFree(),
			new InAppPurchaseLiveUpdatesAnnualFree()
	};

	private InAppPurchase fullVersion;
	private InAppPurchase depthContours;
	private InAppPurchase contourLines;
	private InAppSubscription monthlyLiveUpdates;
	private InAppSubscription discountedMonthlyLiveUpdates;
	private InAppSubscriptionList liveUpdates;

	InAppPurchases(OsmandApplication ctx) {
		fullVersion = FULL_VERSION;
		if (Version.isFreeVersion(ctx)) {
			liveUpdates = new LiveUpdatesInAppPurchasesFree();
		} else {
			liveUpdates = new LiveUpdatesInAppPurchasesFull();
		}
		for (InAppSubscription s : liveUpdates.getAllSubscriptions()) {
			if (s instanceof InAppPurchaseLiveUpdatesMonthly) {
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
	}

	public InAppPurchase getFullVersion() {
		return fullVersion;
	}

	public InAppPurchase getDepthContours() {
		return depthContours;
	}

	public InAppPurchase getContourLines() {
		return contourLines;
	}

	public InAppSubscription getMonthlyLiveUpdates() {
		return monthlyLiveUpdates;
	}

	@Nullable
	public InAppSubscription getPurchasedMonthlyLiveUpdates() {
		if (monthlyLiveUpdates.isAnyPurchased()) {
			return monthlyLiveUpdates;
		} else if (discountedMonthlyLiveUpdates.isAnyPurchased()) {
			return discountedMonthlyLiveUpdates;
		}
		return null;
	}

	public InAppSubscriptionList getLiveUpdates() {
		return liveUpdates;
	}

	public List<InAppPurchase> getAllInAppPurchases() {
		List<InAppPurchase> purchases = new ArrayList<>();
		purchases.add(fullVersion);
		purchases.add(depthContours);
		purchases.add(contourLines);
		purchases.addAll(liveUpdates.getAllSubscriptions());
		return purchases;
	}

	public boolean isFullVersion(String sku) {
		return FULL_VERSION.getSku().equals(sku);
	}

	public boolean isDepthContours(String sku) {
		return DEPTH_CONTOURS_FULL.getSku().equals(sku) || DEPTH_CONTOURS_FREE.getSku().equals(sku);
	}

	public boolean isContourLines(String sku) {
		return CONTOUR_LINES_FULL.getSku().equals(sku) || CONTOUR_LINES_FREE.getSku().equals(sku);
	}

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

	public abstract static class InAppSubscriptionList {

		private List<InAppSubscription> subscriptions;

		InAppSubscriptionList(@NonNull InAppSubscription[] subscriptionsArray) {
			this.subscriptions = Arrays.asList(subscriptionsArray);;
		}

		private List<InAppSubscription> getSubscriptions() {
			return new ArrayList<>(subscriptions);
		}

		public List<InAppSubscription> getAllSubscriptions() {
			List<InAppSubscription> res = new ArrayList<>();
			for (InAppSubscription s : getSubscriptions()) {
				res.add(s);
				res.addAll(s.getDiscounts());
			}
			return res;
		}

		public List<InAppSubscription> getVisibleSubscriptions() {
			List<InAppSubscription> res = new ArrayList<>();
			for (InAppSubscription s : getSubscriptions()) {
				boolean added = false;
				if (s.isPurchased()) {
					res.add(s);
					added = true;
				} else {
					for (InAppSubscription discount : s.getDiscounts()) {
						if (discount.isPurchased()) {
							res.add(discount);
							added = true;
						}
					}
				}
				if (!added) {
					for (InAppSubscription discount : s.getDiscounts()) {
						res.add(discount);
						added = true;
					}
				}
				if (!added && !s.isDiscounted()) {
					res.add(s);
				}
			}
			return res;
		}

		@Nullable
		public InAppSubscription getSubscriptionBySku(@NonNull String sku) {
			for (InAppSubscription s : getAllSubscriptions()) {
				if (s.getSku().equals(sku)) {
					return s;
				}
			}
			return null;
		}

		public boolean containsSku(@NonNull String sku) {
			return getSubscriptionBySku(sku) != null;
		}

		@Nullable
		public InAppSubscription applyDiscountSubscription(String sku) {
			List<InAppSubscription> subscriptions = getAllSubscriptions();
			for (InAppSubscription s : subscriptions) {
				InAppSubscription discount = s.applyDiscountSubscription(sku);
				if (discount != null) {
					return discount;
				}
			}
			return null;
		}
	}

	public static class LiveUpdatesInAppPurchasesFree extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFree() {
			super(LIVE_UPDATES_FREE);
		}
	}

	public static class LiveUpdatesInAppPurchasesFull extends InAppSubscriptionList {

		public LiveUpdatesInAppPurchasesFull() {
			super(LIVE_UPDATES_FULL);
		}
	}

	public abstract static class InAppPurchase {

		public enum PurchaseState {
			UNKNOWN,
			PURCHASED,
			NOT_PURCHASED
		}

		private String sku;
		private String price;
		private double priceValue;
		private String priceCurrencyCode;
		private PurchaseState purchaseState = PurchaseState.UNKNOWN;
		private long purchaseTime;

		double monthlyPriceValue;
		boolean donationSupported = false;
		boolean discounted = false;

		private NumberFormat currencyFormatter;

		private InAppPurchase(String sku) {
			this.sku = sku;
		}

		private InAppPurchase(String sku, boolean discounted) {
			this(sku);
			this.discounted = discounted;
		}

		public String getSku() {
			return sku;
		}

		public String getPrice(Context ctx) {
			if (!Algorithms.isEmpty(price)) {
				return price;
			} else {
				return getDefaultPrice(ctx);
			}
		}

		public void setPrice(String price) {
			this.price = price;
		}

		public long getPurchaseTime() {
			return purchaseTime;
		}

		public void setPurchaseTime(long purchaseTime) {
			this.purchaseTime = purchaseTime;
		}

		public String getDefaultPrice(Context ctx) {
			return "";
		}

		public String getDefaultMonthlyPrice(Context ctx) {
			return "";
		}

		public PurchaseState getPurchaseState() {
			return purchaseState;
		}

		public void setPurchaseState(PurchaseState purchaseState) {
			this.purchaseState = purchaseState;
		}

		public boolean isPurchased() {
			return purchaseState == PurchaseState.PURCHASED;
		}

		public boolean fetchRequired() {
			return purchaseState == PurchaseState.UNKNOWN;
		}

		public boolean isDiscounted() {
			return discounted;
		}

		public CharSequence getTitle(Context ctx) {
			return "";
		}

		public CharSequence getDescription(@NonNull Context ctx) {
			NumberFormat currencyFormatter = getCurrencyFormatter();
			if (currencyFormatter != null) {
				return currencyFormatter.format(getPriceValue());
			} else {
				return ctx.getString(R.string.default_price_currency_format, getPriceValue(), getPriceCurrencyCode());
			}
		}

		public double getPriceValue() {
			return priceValue;
		}

		public void setPriceValue(double priceValue) {
			this.priceValue = priceValue;
		}

		public double getMonthlyPriceValue() {
			return monthlyPriceValue;
		}

		public boolean isDonationSupported() {
			return donationSupported;
		}

		public String getPriceCurrencyCode() {
			return priceCurrencyCode;
		}

		public void setPriceCurrencyCode(String priceCurrencyCode) {
			this.priceCurrencyCode = priceCurrencyCode;

			Locale currencyLocale = null;
			Locale defaultLocale = Locale.getDefault();
			try {
				Currency defaultCurrency = Currency.getInstance(defaultLocale);
				if (defaultCurrency != null && defaultCurrency.getCurrencyCode().equals(priceCurrencyCode)) {
					currencyLocale = defaultLocale;
				}
			} catch (Exception e) {
				// ignore
			}
			if (currencyLocale == null) {
				if ("USD".equals(priceCurrencyCode)) {
					currencyLocale = Locale.US;
				} else if ("EUR".equals(priceCurrencyCode)) {
					currencyLocale = Locale.GERMANY;
				} else if ("GBP".equals(priceCurrencyCode)) {
					currencyLocale = Locale.UK;
				} else if ("JPY".equals(priceCurrencyCode)) {
					currencyLocale = Locale.JAPAN;
				} else if ("CNY".equals(priceCurrencyCode)) {
					currencyLocale = Locale.CHINA;
				} else if ("UAH".equals(priceCurrencyCode)) {
					currencyLocale = new Locale("ukr", "UA");
				} else if ("RUB".equals(priceCurrencyCode)) {
					currencyLocale = new Locale("rus", "RU");
				}
			}
			if (currencyLocale != null) {
				currencyFormatter = NumberFormat.getCurrencyInstance(currencyLocale);
			}
		}

		NumberFormat getCurrencyFormatter() {
			return currencyFormatter;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (obj instanceof InAppPurchase) {
				InAppPurchase purchase = (InAppPurchase) obj;
				return sku.equals(purchase.sku);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return sku.hashCode();
		}
	}

	public static abstract class InAppSubscription extends InAppPurchase {

		private Map<String, InAppSubscription> discounts = new ConcurrentHashMap<>();
		private String skuNoVersion;
		private String subscriptionPeriod;
		protected boolean discount = false;

		InAppSubscription(@NonNull String skuNoVersion, int version) {
			super(skuNoVersion + "_v" + version);
			this.skuNoVersion = skuNoVersion;
		}

		InAppSubscription(@NonNull String sku, boolean discounted) {
			super(sku, discounted);
			this.skuNoVersion = sku;
		}

		@NonNull
		public List<InAppSubscription> getDiscounts() {
			return new ArrayList<>(discounts.values());
		}

		@Nullable
		public InAppSubscription applyDiscountSubscription(@NonNull String sku) {
			InAppSubscription s = null;
			if (!discount) {
				s = discounts.get(sku);
				if (s == null) {
					s = newInstance(sku);
					if (s != null) {
						s.discount = true;
						discounts.put(sku, s);
					}
				}
			}
			return s;
		}

		public boolean isAnyPurchased() {
			if (isPurchased()) {
				return true;
			} else {
				for (InAppSubscription s : getDiscounts()) {
					if (s.isPurchased()) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean isDiscount() {
			return discount;
		}

		public String getSkuNoVersion() {
			return skuNoVersion;
		}

		public String getSubscriptionPeriod() {
			return subscriptionPeriod;
		}

		public void setSubscriptionPeriod(String subscriptionPeriod) {
			this.subscriptionPeriod = subscriptionPeriod;
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			if (getMonthlyPriceValue() == 0) {
				return ctx.getString(R.string.osm_live_payment_month_cost_descr, getDefaultMonthlyPrice(ctx));
			} else {
				NumberFormat currencyFormatter = getCurrencyFormatter();
				if (currencyFormatter != null) {
					return ctx.getString(R.string.osm_live_payment_month_cost_descr, currencyFormatter.format(getMonthlyPriceValue()));
				} else {
					return ctx.getString(R.string.osm_live_payment_month_cost_descr_ex, getMonthlyPriceValue(), getPriceCurrencyCode());
				}
			}
		}

		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return "";
		}

		@Nullable
		protected abstract InAppSubscription newInstance(@NonNull String sku);
	}

	public static class InAppPurchaseFullVersion extends InAppPurchase {

		private static final String SKU_FULL_VERSION_PRICE = "osmand_full_version_price";

		InAppPurchaseFullVersion() {
			super(SKU_FULL_VERSION_PRICE);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.full_version_price);
		}
	}

	public static class InAppPurchaseDepthContours extends InAppPurchase {

		private InAppPurchaseDepthContours(String sku) {
			super(sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.sea_depth_maps_price);
		}
	}

	public static class InAppPurchaseDepthContoursFull extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FULL = "net.osmand.seadepth_plus";

		InAppPurchaseDepthContoursFull() {
			super(SKU_DEPTH_CONTOURS_FULL);
		}
	}

	public static class InAppPurchaseDepthContoursFree extends InAppPurchaseDepthContours {

		private static final String SKU_DEPTH_CONTOURS_FREE = "net.osmand.seadepth";

		InAppPurchaseDepthContoursFree() {
			super(SKU_DEPTH_CONTOURS_FREE);
		}
	}

	public static class InAppPurchaseContourLines extends InAppPurchase {

		private InAppPurchaseContourLines(String sku) {
			super(sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.srtm_plugin_price);
		}
	}

	public static class InAppPurchaseContourLinesFull extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FULL = "net.osmand.contourlines_plus";

		InAppPurchaseContourLinesFull() {
			super(SKU_CONTOUR_LINES_FULL);
		}
	}

	public static class InAppPurchaseContourLinesFree extends InAppPurchaseContourLines {

		private static final String SKU_CONTOUR_LINES_FREE = "net.osmand.contourlines";

		InAppPurchaseContourLinesFree() {
			super(SKU_CONTOUR_LINES_FREE);
		}
	}

	public static abstract class InAppPurchaseLiveUpdatesMonthly extends InAppSubscription {

		InAppPurchaseLiveUpdatesMonthly(String skuNoVersion, int version) {
			super(skuNoVersion, version);
			donationSupported = true;
		}

		InAppPurchaseLiveUpdatesMonthly(@NonNull String sku, boolean discounted) {
			super(sku, discounted);
			donationSupported = true;
		}

		InAppPurchaseLiveUpdatesMonthly(@NonNull String sku) {
			this(sku, false);
		}

		@Override
		public void setPriceValue(double priceValue) {
			super.setPriceValue(priceValue);
			monthlyPriceValue = priceValue;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_monthly_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_monthly_price);
		}

		@Override
		public CharSequence getTitle(Context ctx) {
			return ctx.getString(R.string.osm_live_payment_monthly_title);
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			CharSequence descr = super.getDescription(ctx);
			SpannableStringBuilder text = new SpannableStringBuilder(descr).append(". ").append(ctx.getString(R.string.osm_live_payment_contribute_descr));
			text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), descr.length() + 1, text.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			return text;
		}

		@Override
		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return ctx.getString(R.string.osm_live_payment_renews_monthly);
		}
	}

	public static class InAppPurchaseLiveUpdatesMonthlyFull extends InAppPurchaseLiveUpdatesMonthly {

		private static final String SKU_LIVE_UPDATES_MONTHLY_FULL = "osm_live_subscription_monthly_full";

		InAppPurchaseLiveUpdatesMonthlyFull() {
			super(SKU_LIVE_UPDATES_MONTHLY_FULL, 1);
		}

		private InAppPurchaseLiveUpdatesMonthlyFull(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesMonthlyFull(sku) : null;
		}
	}

	public static class InAppPurchaseLiveUpdatesMonthlyFree extends InAppPurchaseLiveUpdatesMonthly {

		private static final String SKU_LIVE_UPDATES_MONTHLY_FREE = "osm_live_subscription_monthly_free";

		InAppPurchaseLiveUpdatesMonthlyFree() {
			super(SKU_LIVE_UPDATES_MONTHLY_FREE, 1);
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

	public static abstract class InAppPurchaseLiveUpdates3Months extends InAppSubscription {

		InAppPurchaseLiveUpdates3Months(String skuNoVersion, int version) {
			super(skuNoVersion, version);
		}

		InAppPurchaseLiveUpdates3Months(@NonNull String sku) {
			super(sku, false);
		}

		@Override
		public void setPriceValue(double priceValue) {
			super.setPriceValue(priceValue);
			monthlyPriceValue = priceValue / 3d;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_3_months_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_3_months_monthly_price);
		}

		@Override
		public CharSequence getTitle(Context ctx) {
			return ctx.getString(R.string.osm_live_payment_3_months_title);
		}

		@Override
		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return ctx.getString(R.string.osm_live_payment_renews_quarterly);
		}
	}

	public static class InAppPurchaseLiveUpdates3MonthsFull extends InAppPurchaseLiveUpdates3Months {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_FULL = "osm_live_subscription_3_months_full";

		InAppPurchaseLiveUpdates3MonthsFull() {
			super(SKU_LIVE_UPDATES_3_MONTHS_FULL, 1);
		}

		private InAppPurchaseLiveUpdates3MonthsFull(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdates3MonthsFull(sku) : null;
		}
	}

	public static class InAppPurchaseLiveUpdates3MonthsFree extends InAppPurchaseLiveUpdates3Months {

		private static final String SKU_LIVE_UPDATES_3_MONTHS_FREE = "osm_live_subscription_3_months_free";

		InAppPurchaseLiveUpdates3MonthsFree() {
			super(SKU_LIVE_UPDATES_3_MONTHS_FREE, 1);
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

	public static abstract class InAppPurchaseLiveUpdatesAnnual extends InAppSubscription {

		InAppPurchaseLiveUpdatesAnnual(String skuNoVersion, int version) {
			super(skuNoVersion, version);
		}

		InAppPurchaseLiveUpdatesAnnual(@NonNull String sku) {
			super(sku, false);
		}

		@Override
		public void setPriceValue(double priceValue) {
			super.setPriceValue(priceValue);
			monthlyPriceValue = priceValue / 12d;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_annual_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_annual_monthly_price);
		}

		@Override
		public CharSequence getTitle(Context ctx) {
			return ctx.getString(R.string.osm_live_payment_annual_title);
		}

		@Override
		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return ctx.getString(R.string.osm_live_payment_renews_annually);
		}
	}

	public static class InAppPurchaseLiveUpdatesAnnualFull extends InAppPurchaseLiveUpdatesAnnual {

		private static final String SKU_LIVE_UPDATES_ANNUAL_FULL = "osm_live_subscription_annual_full";

		InAppPurchaseLiveUpdatesAnnualFull() {
			super(SKU_LIVE_UPDATES_ANNUAL_FULL, 1);
		}

		private InAppPurchaseLiveUpdatesAnnualFull(@NonNull String sku) {
			super(sku);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return sku.startsWith(getSkuNoVersion()) ? new InAppPurchaseLiveUpdatesAnnualFull(sku) : null;
		}
	}

	public static class InAppPurchaseLiveUpdatesAnnualFree extends InAppPurchaseLiveUpdatesAnnual {

		private static final String SKU_LIVE_UPDATES_ANNUAL_FREE = "osm_live_subscription_annual_free";

		InAppPurchaseLiveUpdatesAnnualFree() {
			super(SKU_LIVE_UPDATES_ANNUAL_FREE, 1);
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

	public static class InAppPurchaseLiveUpdatesOldMonthly extends InAppPurchaseLiveUpdatesMonthly {

		InAppPurchaseLiveUpdatesOldMonthly(String sku) {
			super(sku, true);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_default_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_live_default_price);
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return null;
		}
	}

	public static class InAppPurchaseLiveUpdatesOldMonthlyFull extends InAppPurchaseLiveUpdatesOldMonthly {

		private static final String SKU_LIVE_UPDATES_OLD_MONTHLY_FULL = "osm_live_subscription_2";

		InAppPurchaseLiveUpdatesOldMonthlyFull() {
			super(SKU_LIVE_UPDATES_OLD_MONTHLY_FULL);
		}
	}

	public static class InAppPurchaseLiveUpdatesOldMonthlyFree extends InAppPurchaseLiveUpdatesOldMonthly {

		private static final String SKU_LIVE_UPDATES_OLD_MONTHLY_FREE = "osm_free_live_subscription_2";

		InAppPurchaseLiveUpdatesOldMonthlyFree() {
			super(SKU_LIVE_UPDATES_OLD_MONTHLY_FREE);
		}
	}

	public static class InAppPurchaseLiveUpdatesOldSubscription extends InAppSubscription {

		private SkuDetails details;

		InAppPurchaseLiveUpdatesOldSubscription(@NonNull SkuDetails details) {
			super(details.getSku(), true);
			this.details = details;
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
}

