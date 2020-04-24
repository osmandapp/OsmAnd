package net.osmand.plus.inapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;

import net.osmand.AndroidUtils;
import net.osmand.Period;
import net.osmand.Period.PeriodUnit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.text.NumberFormat;
import java.text.ParseException;
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
	private InAppPurchase[] inAppPurchases;

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

		inAppPurchases = new InAppPurchase[] { fullVersion, depthContours, contourLines };
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

	@Nullable
	public InAppPurchase getInAppPurchaseBySku(@NonNull String sku) {
		for (InAppPurchase p : inAppPurchases) {
			if (p.getSku().equals(sku)) {
				return p;
			}
		}
		return null;
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

	public List<InAppPurchase> getAllInAppPurchases(boolean includeSubscriptions) {
		List<InAppPurchase> purchases = new ArrayList<>();
		purchases.add(fullVersion);
		purchases.add(depthContours);
		purchases.add(contourLines);
		if (includeSubscriptions) {
			purchases.addAll(liveUpdates.getAllSubscriptions());
		}
		return purchases;
	}

	public List<InAppSubscription> getAllInAppSubscriptions() {
		return liveUpdates.getAllSubscriptions();
	}

	@Nullable
	public InAppSubscription getInAppSubscriptionBySku(@NonNull String sku) {
		for (InAppSubscription s : liveUpdates.getAllSubscriptions()) {
			if (sku.startsWith(s.getSkuNoVersion())) {
				return s;
			}
		}
		return null;
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
			this.subscriptions = Arrays.asList(subscriptionsArray);
		}

		private List<InAppSubscription> getSubscriptions() {
			return new ArrayList<>(subscriptions);
		}

		public List<InAppSubscription> getAllSubscriptions() {
			List<InAppSubscription> res = new ArrayList<>();
			for (InAppSubscription s : getSubscriptions()) {
				res.add(s);
				res.addAll(s.getUpgrades());
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
					for (InAppSubscription upgrade : s.getUpgrades()) {
						if (upgrade.isPurchased()) {
							res.add(upgrade);
							added = true;
						}
					}
				}
				if (!added) {
					for (InAppSubscription upgrade : s.getUpgrades()) {
						res.add(upgrade);
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
		public InAppSubscription upgradeSubscription(String sku) {
			List<InAppSubscription> subscriptions = getAllSubscriptions();
			for (InAppSubscription s : subscriptions) {
				InAppSubscription upgrade = s.upgradeSubscription(sku);
				if (upgrade != null) {
					return upgrade;
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

		private InAppPurchase(@NonNull String sku) {
			this.sku = sku;
		}

		private InAppPurchase(@NonNull String sku, boolean discounted) {
			this(sku);
			this.discounted = discounted;
		}

		@NonNull
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

	public static class InAppSubscriptionIntroductoryInfo {

		private InAppSubscription subscription;

		private String introductoryPrice;
		private long introductoryPriceAmountMicros;
		private String introductoryPeriodString;
		private int introductoryCycles;

		private double introductoryPriceValue;
		private Period introductoryPeriod;

		public InAppSubscriptionIntroductoryInfo(@NonNull InAppSubscription subscription,
												 String introductoryPrice,
												 long introductoryPriceAmountMicros,
												 String introductoryPeriodString,
												 String introductoryCycles) throws ParseException {
			this.subscription = subscription;
			this.introductoryPrice = introductoryPrice;
			this.introductoryPriceAmountMicros = introductoryPriceAmountMicros;
			this.introductoryPeriodString = introductoryPeriodString;
			try {
				this.introductoryCycles = Integer.parseInt(introductoryCycles);
			} catch (NumberFormatException e) {
				throw new ParseException("Cannot parse introductoryCycles = " + introductoryCycles, 0);
			}
			introductoryPriceValue = introductoryPriceAmountMicros / 1000000d;
			introductoryPeriod = Period.parse(introductoryPeriodString);
		}

		public String getIntroductoryPrice() {
			return introductoryPrice;
		}

		public long getIntroductoryPriceAmountMicros() {
			return introductoryPriceAmountMicros;
		}

		public String getIntroductoryPeriodString() {
			return introductoryPeriodString;
		}

		public int getIntroductoryCycles() {
			return introductoryCycles;
		}

		public double getIntroductoryPriceValue() {
			return introductoryPriceValue;
		}

		public double getIntroductoryMonthlyPriceValue() {
			return introductoryPriceValue /
					(introductoryPeriod.getUnit().getMonthsValue() * introductoryPeriod.getNumberOfUnits());
		}

		public Period getIntroductoryPeriod() {
			return introductoryPeriod;
		}

		public long getTotalPeriods() {
			return introductoryPeriod.getNumberOfUnits() * introductoryCycles;
		}

		private String getTotalUnitsString(@NonNull Context ctx, boolean original) {
			String unitStr = "";
			Period subscriptionPeriod = subscription.getSubscriptionPeriod();
			PeriodUnit unit = original && subscriptionPeriod != null ? subscriptionPeriod.getUnit() : introductoryPeriod.getUnit();
			long totalPeriods = original && subscriptionPeriod != null ? subscriptionPeriod.getNumberOfUnits() : getTotalPeriods();
			switch (unit) {
				case YEAR:
					unitStr = ctx.getString(R.string.year);
					break;
				case MONTH:
					if (totalPeriods == 1) {
						unitStr = ctx.getString(R.string.month);
					} else if (totalPeriods < 5) {
						unitStr = ctx.getString(R.string.months_2_4);
					} else {
						unitStr = ctx.getString(R.string.months_5);
					}
					break;
				case WEEK:
					if (totalPeriods == 1) {
						unitStr = ctx.getString(R.string.week);
					} else if (totalPeriods < 5) {
						unitStr = ctx.getString(R.string.weeks_2_4);
					} else {
						unitStr = ctx.getString(R.string.weeks_5);
					}
					break;
				case DAY:
					if (totalPeriods == 1) {
						unitStr = ctx.getString(R.string.day);
					} else if (totalPeriods < 5) {
						unitStr = ctx.getString(R.string.days_2_4);
					} else {
						unitStr = ctx.getString(R.string.days_5);
					}
					break;
			}
			return unitStr;
		}

		private String getUnitString(@NonNull Context ctx) {
			PeriodUnit unit = introductoryPeriod.getUnit();
			switch (unit) {
				case YEAR:
					return ctx.getString(R.string.year);
				case MONTH:
					return ctx.getString(R.string.month);
				case WEEK:
					return ctx.getString(R.string.week);
				case DAY:
					return ctx.getString(R.string.day);
			}
			return "";
		}

		private String getDisountPeriodString(String unitStr, long totalPeriods) {
			if (totalPeriods == 1)
				return unitStr;
			if (AndroidUtils.isRTL()) {
				return unitStr + " " + totalPeriods;
			} else {
				return totalPeriods + " " + unitStr;
			}
		}

		public CharSequence getDescriptionTitle(@NonNull Context ctx) {
			long totalPeriods = getTotalPeriods();
			String unitStr = getTotalUnitsString(ctx, false).toLowerCase();
			int discountPercent = subscription.getDiscountPercent(null);
			return ctx.getString(R.string.get_discount_title, totalPeriods, unitStr, discountPercent + "%");
		}

		public CharSequence getFormattedDescription(@NonNull Context ctx, @ColorInt int textColor) {
			long totalPeriods = getTotalPeriods();
			String singleUnitStr = getUnitString(ctx).toLowerCase();
			String unitStr = getTotalUnitsString(ctx, false).toLowerCase();
			long numberOfUnits = introductoryPeriod.getNumberOfUnits();
			Period subscriptionPeriod = subscription.getSubscriptionPeriod();
			long originalNumberOfUnits = subscriptionPeriod != null ? subscriptionPeriod.getNumberOfUnits() : 1;
			String originalUnitsStr = getTotalUnitsString(ctx, true).toLowerCase();
			String originalPriceStr = subscription.getPrice(ctx);
			String priceStr = introductoryPrice;

			String pricePeriod;
			String originalPricePeriod;

			if (AndroidUtils.isRTL()) {
				pricePeriod = singleUnitStr + " / " + priceStr;
				originalPricePeriod = originalUnitsStr + " / " + originalPriceStr;
				if (numberOfUnits > 1) {
					pricePeriod = unitStr + " " + numberOfUnits + " / " + priceStr;
				}
				if (originalNumberOfUnits == 3 && subscriptionPeriod.getUnit() == PeriodUnit.MONTH) {
					originalPricePeriod = ctx.getString(R.string.months_3).toLowerCase() + " / " + originalPriceStr;
				} else if (originalNumberOfUnits > 1) {
					originalPricePeriod = originalUnitsStr + " " + originalNumberOfUnits + " / " + originalPriceStr;
				}
			} else {
				pricePeriod = priceStr + " / " + singleUnitStr;
				originalPricePeriod = originalPriceStr + " / " + originalUnitsStr;
				if (numberOfUnits > 1) {
					pricePeriod = priceStr + " / " + numberOfUnits + " " + unitStr;
				}
				if (originalNumberOfUnits == 3 && subscriptionPeriod.getUnit() == PeriodUnit.MONTH) {
					originalPricePeriod = originalPriceStr + " / " + ctx.getString(R.string.months_3).toLowerCase();
				} else if (originalNumberOfUnits > 1) {
					originalPricePeriod = originalPriceStr + " / " + originalNumberOfUnits + " " + originalUnitsStr;
				}
			}
			String periodPriceStr = introductoryCycles == 1 ? priceStr : pricePeriod;

			int firstPartRes = totalPeriods == 1 ? R.string.get_discount_first_part : R.string.get_discount_first_few_part;
			SpannableStringBuilder mainPart = new SpannableStringBuilder(ctx.getString(firstPartRes, periodPriceStr, getDisountPeriodString(unitStr, totalPeriods)));
			SpannableStringBuilder thenPart = new SpannableStringBuilder(ctx.getString(R.string.get_discount_second_part, originalPricePeriod));
			Typeface typefaceRegular = FontCache.getRobotoRegular(ctx);
			Typeface typefaceBold = FontCache.getRobotoMedium(ctx);
			mainPart.setSpan(new ForegroundColorSpan(textColor), 0, mainPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mainPart.setSpan(new CustomTypefaceSpan(typefaceBold), 0, mainPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			int secondaryTextColor = Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor));
			thenPart.setSpan(new ForegroundColorSpan(secondaryTextColor), 0, thenPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			thenPart.setSpan(new CustomTypefaceSpan(typefaceRegular), 0, thenPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			return new SpannableStringBuilder(mainPart).append("\n").append(thenPart);
		}
	}

	public static abstract class InAppSubscription extends InAppPurchase {

		private Map<String, InAppSubscription> upgrades = new ConcurrentHashMap<>();
		private String skuNoVersion;
		private String subscriptionPeriodString;
		private Period subscriptionPeriod;
		private boolean upgrade = false;

		private InAppSubscriptionIntroductoryInfo introductoryInfo;

		InAppSubscription(@NonNull String skuNoVersion, int version) {
			super(skuNoVersion + "_v" + version);
			this.skuNoVersion = skuNoVersion;
		}

		InAppSubscription(@NonNull String sku, boolean discounted) {
			super(sku, discounted);
			this.skuNoVersion = sku;
		}

		@NonNull
		private List<InAppSubscription> getUpgrades() {
			return new ArrayList<>(upgrades.values());
		}

		@Nullable
		InAppSubscription upgradeSubscription(@NonNull String sku) {
			InAppSubscription s = null;
			if (!upgrade) {
				s = getSku().equals(sku) ? this : upgrades.get(sku);
				if (s == null) {
					s = newInstance(sku);
					if (s != null) {
						s.upgrade = true;
						upgrades.put(sku, s);
					}
				}
			}
			return s;
		}

		public boolean isUpgrade() {
			return upgrade;
		}

		public boolean isAnyPurchased() {
			if (isPurchased()) {
				return true;
			} else {
				for (InAppSubscription s : getUpgrades()) {
					if (s.isPurchased()) {
						return true;
					}
				}
			}
			return false;
		}

		public String getSkuNoVersion() {
			return skuNoVersion;
		}

		@Nullable
		public String getSubscriptionPeriodString() {
			return subscriptionPeriodString;
		}

		@Nullable
		public Period getSubscriptionPeriod() {
			return subscriptionPeriod;
		}

		public void setSubscriptionPeriodString(String subscriptionPeriodString) throws ParseException {
			this.subscriptionPeriodString = subscriptionPeriodString;
			this.subscriptionPeriod = Period.parse(subscriptionPeriodString);
		}

		public InAppSubscriptionIntroductoryInfo getIntroductoryInfo() {
			/*
			try {
				if (subscriptionPeriod != null && subscriptionPeriod.getUnit() == PeriodUnit.YEAR) {
					introductoryInfo = new InAppSubscriptionIntroductoryInfo(this, "30 грн.", 30000000L, "P1Y", "1");
				}
			} catch (ParseException e) {
				//
			}
			*/
			return introductoryInfo;
		}

		public void setIntroductoryInfo(InAppSubscriptionIntroductoryInfo introductoryInfo) {
			this.introductoryInfo = introductoryInfo;
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

		public String getDiscountTitle(@NonNull Context ctx, @Nullable InAppSubscription monthlyLiveUpdates) {
			int discountPercent = getDiscountPercent(monthlyLiveUpdates);
			return discountPercent > 0 ? ctx.getString(R.string.osm_live_payment_discount_descr, discountPercent + "%") : "";
		}

		public int getDiscountPercent(@Nullable InAppSubscription monthlyLiveUpdates) {
			double monthlyPriceValue = getMonthlyPriceValue();
			if (monthlyLiveUpdates != null) {
				double regularMonthlyPrice = monthlyLiveUpdates.getPriceValue();
				if (regularMonthlyPrice > 0 && monthlyPriceValue > 0 && monthlyPriceValue < regularMonthlyPrice) {
					return (int) ((1 - monthlyPriceValue / regularMonthlyPrice) * 100d);
				}
			} else if (introductoryInfo != null) {
				double introductoryMonthlyPrice = introductoryInfo.getIntroductoryMonthlyPriceValue();
				if (introductoryMonthlyPrice > 0 && monthlyPriceValue > 0 && monthlyPriceValue > introductoryMonthlyPrice) {
					return (int) ((1 - introductoryMonthlyPrice / monthlyPriceValue) * 100d);
				}
			}
			return 0;
		}

		public String getPriceWithPeriod(Context ctx) {
			return getPrice(ctx);
		}
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
		public String getPriceWithPeriod(Context ctx) {
			return ctx.getString(R.string.ltr_or_rtl_combine_via_slash_with_space, getPrice(ctx),
					ctx.getString(R.string.month).toLowerCase());
		}

		@Override
		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return ctx.getString(R.string.osm_live_payment_renews_monthly);
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			return "";
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
		public String getPriceWithPeriod(Context ctx) {
			return ctx.getString(R.string.ltr_or_rtl_combine_via_slash_with_space, getPrice(ctx),
					ctx.getString(R.string.months_3).toLowerCase());
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
		public String getPriceWithPeriod(Context ctx) {
			return ctx.getString(R.string.ltr_or_rtl_combine_via_slash_with_space, getPrice(ctx),
					ctx.getString(R.string.year).toLowerCase());
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

