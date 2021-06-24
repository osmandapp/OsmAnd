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
import androidx.annotation.StringRes;

import net.osmand.AndroidUtils;
import net.osmand.Period;
import net.osmand.Period.PeriodUnit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InAppPurchases {

	protected InAppPurchase fullVersion;
	protected InAppPurchase depthContours;
	protected InAppPurchase contourLines;
	protected InAppSubscription monthlyLiveUpdates;
	protected InAppSubscription legacyMonthlyLiveUpdates;
	protected InAppSubscriptionList subscriptions;
	protected InAppPurchase[] inAppPurchases;

	protected InAppPurchases(OsmandApplication ctx) {
	}

	private static OsmandSettings getSettings(@NonNull Context ctx) {
		return ((OsmandApplication) ctx.getApplicationContext()).getSettings();
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
		} else if (legacyMonthlyLiveUpdates != null && legacyMonthlyLiveUpdates.isAnyPurchased()) {
			return legacyMonthlyLiveUpdates;
		}
		return null;
	}

	@Nullable
	public InAppSubscription getAnyPurchasedSubscription() {
		List<InAppSubscription> allSubscriptions = subscriptions.getAllSubscriptions();
		for (InAppSubscription subscription : allSubscriptions) {
			if (subscription.isPurchased()) {
				return subscription;
			}
		}
		return null;
	}

	public InAppSubscriptionList getSubscriptions() {
		return subscriptions;
	}

	public List<InAppPurchase> getAllInAppPurchases(boolean includeSubscriptions) {
		List<InAppPurchase> purchases = new ArrayList<>();
		purchases.add(fullVersion);
		purchases.add(depthContours);
		purchases.add(contourLines);
		if (includeSubscriptions) {
			purchases.addAll(subscriptions.getAllSubscriptions());
		}
		return purchases;
	}

	public List<InAppSubscription> getAllInAppSubscriptions() {
		return subscriptions.getAllSubscriptions();
	}

	@Nullable
	public InAppSubscription getInAppSubscriptionBySku(@NonNull String sku) {
		for (InAppSubscription s : subscriptions.getAllSubscriptions()) {
			if (sku.startsWith(s.getSkuNoVersion())) {
				return s;
			}
		}
		return null;
	}

	public abstract boolean isFullVersion(InAppPurchase p);

	public abstract boolean isDepthContours(InAppPurchase p);

	public abstract boolean isContourLines(InAppPurchase p);

	public abstract boolean isLiveUpdatesSubscription(InAppPurchase p);

	public abstract boolean isOsmAndProSubscription(InAppPurchase p);

	public abstract boolean isMapsSubscription(InAppPurchase p);

	public abstract static class InAppSubscriptionList {

		private final List<InAppSubscription> subscriptions;

		InAppSubscriptionList(@NonNull InAppSubscription[] subscriptionsArray) {
			this.subscriptions = Arrays.asList(subscriptionsArray);
		}

		public List<InAppSubscription> getSubscriptions() {
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
				if (!added && !s.isLegacy()) {
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

		@Nullable
		public InAppSubscription getTopExpiredSubscription() {
			List<InAppSubscription> expiredSubscriptions = new ArrayList<>();
			for (InAppSubscription s : getAllSubscriptions()) {
				if (s.getState().isGone()) {
					expiredSubscriptions.add(s);
				}
			}
			Collections.sort(expiredSubscriptions, new Comparator<InAppSubscription>() {
				@Override
				public int compare(InAppSubscription s1, InAppSubscription s2) {
					int orderS1 = s1.getState().ordinal();
					int orderS2 = s2.getState().ordinal();
					if (orderS1 != orderS2) {
						return (orderS1 < orderS2) ? -1 : ((orderS1 == orderS2) ? 0 : 1);
					}
					return Double.compare(s1.getMonthlyPriceValue(), s2.getMonthlyPriceValue());
				}
			});
			return expiredSubscriptions.isEmpty() ? null : expiredSubscriptions.get(0);
		}
	}

	public abstract static class InAppPurchase {

		public enum PurchaseState {
			UNKNOWN,
			PURCHASED,
			NOT_PURCHASED
		}

		private final int featureId;
		private final String sku;
		private String price;
		private double priceValue;
		private String priceCurrencyCode;
		private PurchaseState purchaseState = PurchaseState.UNKNOWN;
		private PurchaseInfo purchaseInfo;

		double monthlyPriceValue;
		boolean donationSupported = false;

		private NumberFormat currencyFormatter;

		protected InAppPurchase(int featureId, @NonNull String sku) {
			this.featureId = featureId;
			this.sku = sku;
		}

		public int getFeatureId() {
			return featureId;
		}

		@NonNull
		public abstract int[] getScope();

		public boolean hasFeatureInScope(int featureId) {
			for (int id : getScope()) {
				if (featureId == id) {
					return true;
				}
			}
			return false;
		}

		@NonNull
		public String getSku() {
			return sku;
		}

		@Nullable
		public String getOrderId() {
			return purchaseInfo != null ? purchaseInfo.getOrderId() : null;
		}

		private CommonPreference<String> getPurchaseInfoPref(@NonNull Context ctx) {
			return getSettings(ctx).registerStringPreference(sku + "_purchase_info", "").makeGlobal();
		}

		public boolean storePurchaseInfo(@NonNull Context ctx) {
			PurchaseInfo purchaseInfo = this.purchaseInfo;
			if (purchaseInfo != null) {
				getPurchaseInfoPref(ctx).set(purchaseInfo.toJson());
				return true;
			}
			return false;
		}

		public boolean restorePurchaseInfo(@NonNull Context ctx) {
			String json = getPurchaseInfoPref(ctx).get();
			if (!Algorithms.isEmpty(json)) {
				try {
					purchaseInfo = new PurchaseInfo(json);
				} catch (JSONException e) {
					// ignore
				}
				return true;
			}
			return false;
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
			return purchaseInfo != null ? purchaseInfo.getPurchaseTime() : 0;
		}

		public PurchaseInfo getPurchaseInfo() {
			return purchaseInfo;
		}

		void setPurchaseInfo(@NonNull Context ctx, PurchaseInfo purchaseInfo) {
			this.purchaseInfo = purchaseInfo;
			storePurchaseInfo(ctx);
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

		public abstract boolean isLegacy();

		public CharSequence getTitle(Context ctx) {
			return "";
		}

		public CharSequence getDescription(@NonNull Context ctx) {
			return getFormattedPrice(ctx, getPriceValue(), getPriceCurrencyCode());
		}

		public String getFormattedPrice(@NonNull Context ctx, double priceValue, String priceCurrencyCode) {
			NumberFormat currencyFormatter = getCurrencyFormatter();
			if (currencyFormatter != null) {
				return currencyFormatter.format(priceValue);
			} else {
				return ctx.getString(R.string.default_price_currency_format, priceValue, priceCurrencyCode);
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
												 int introductoryCycles) throws ParseException {
			this.subscription = subscription;
			this.introductoryPrice = introductoryPrice;
			this.introductoryPriceAmountMicros = introductoryPriceAmountMicros;
			this.introductoryPeriodString = introductoryPeriodString;
			this.introductoryCycles = introductoryCycles;
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

		private final Map<String, InAppSubscription> upgrades = new ConcurrentHashMap<>();
		private final String skuNoVersion;
		private String subscriptionPeriodString;
		private Period subscriptionPeriod;
		private boolean upgrade = false;
		private SubscriptionState state = SubscriptionState.UNDEFINED;
		private SubscriptionState previousState = SubscriptionState.UNDEFINED;
		private long expireTime = 0;

		private InAppSubscriptionIntroductoryInfo introductoryInfo;

		public enum SubscriptionState {
			UNDEFINED("undefined", R.string.shared_string_undefined),
			ACTIVE("active", R.string.osm_live_active),
			CANCELLED("cancelled", R.string.osmand_live_cancelled),
			IN_GRACE_PERIOD("in_grace_period", R.string.in_grace_period),
			ON_HOLD("on_hold", R.string.on_hold),
			PAUSED("paused", R.string.shared_string_paused),
			EXPIRED("expired", R.string.expired);

			private final String stateStr;
			@StringRes
			private final int stringRes;

			SubscriptionState(@NonNull String stateStr, @StringRes int stringRes) {
				this.stateStr = stateStr;
				this.stringRes = stringRes;
			}

			public String getStateStr() {
				return stateStr;
			}

			@StringRes
			public int getStringRes() {
				return stringRes;
			}

			@NonNull
			public static SubscriptionState getByStateStr(@NonNull String stateStr) {
				for (SubscriptionState state : SubscriptionState.values()) {
					if (state.stateStr.equals(stateStr)) {
						return state;
					}
				}
				return UNDEFINED;
			}

			public boolean isGone() {
				return this == ON_HOLD || this == PAUSED || this == EXPIRED;
			}

			public boolean isActive() {
				return this == ACTIVE || this == CANCELLED || this == IN_GRACE_PERIOD;
			}
		}

		InAppSubscription(int id, @NonNull String skuNoVersion, int version) {
			super(id, skuNoVersion + "_v" + version);
			this.skuNoVersion = skuNoVersion;
		}

		InAppSubscription(int id, @NonNull String sku) {
			super(id, sku);
			this.skuNoVersion = sku;
		}

		@StringRes
		public abstract int getPeriodTypeString();

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

		@NonNull
		public SubscriptionState getState() {
			return state;
		}

		public void setState(@NonNull Context ctx, @NonNull SubscriptionState state) {
			this.state = state;
			storeState(ctx, state);
		}

		@NonNull
		public SubscriptionState getPreviousState() {
			return previousState;
		}

		public boolean hasStateChanged() {
			return state != previousState;
		}

		private CommonPreference<String> getStatePref(@NonNull Context ctx) {
			return getSettings(ctx).registerStringPreference(getSku() + "_state", "").makeGlobal();
		}

		void storeState(@NonNull Context ctx, @NonNull SubscriptionState state) {
			getStatePref(ctx).set(state.getStateStr());
		}

		boolean restoreState(@NonNull Context ctx) {
			String stateStr = getStatePref(ctx).get();
			if (!Algorithms.isEmpty(stateStr)) {
				SubscriptionState state = SubscriptionState.getByStateStr(stateStr);
				this.previousState = state;
				this.state = state;
				return true;
			}
			return false;
		}

		public long getCalculatedExpiredTime() {
			long purchaseTime = getPurchaseTime();
			Period period = getSubscriptionPeriod();
			if (purchaseTime == 0 || period == null || period.getUnit() == null) {
				return 0;
			}
			Date date = new Date(purchaseTime);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			long currentTime = System.currentTimeMillis();
			while (calendar.getTimeInMillis() < currentTime) {
				calendar.add(period.getUnit().getCalendarIdx(), period.getNumberOfUnits());
			}
			return calendar.getTimeInMillis();
		}

		public long getExpireTime() {
			return expireTime;
		}

		public void setExpireTime(@NonNull Context ctx, long expireTime) {
			this.expireTime = expireTime;
			storeExpireTime(ctx, expireTime);
		}

		private CommonPreference<Long> getExpireTimePref(@NonNull Context ctx) {
			return getSettings(ctx).registerLongPreference(getSku() + "_expire_time", 0L).makeGlobal();
		}

		boolean restoreExpireTime(@NonNull Context ctx) {
			Long expireTime = getExpireTimePref(ctx).get();
			if (expireTime != null) {
				this.expireTime = expireTime;
				return true;
			}
			return false;
		}

		void storeExpireTime(@NonNull Context ctx, long expireTime) {
			getExpireTimePref(ctx).set(expireTime);
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
			double monthlyPriceValue = getMonthlyPriceValue();
			if (monthlyPriceValue == 0) {
				return ctx.getString(R.string.osm_live_payment_month_cost_descr, getDefaultMonthlyPrice(ctx));
			} else {
				NumberFormat currencyFormatter = getCurrencyFormatter();
				if (getIntroductoryInfo() != null)
					monthlyPriceValue = getIntroductoryInfo().getIntroductoryMonthlyPriceValue();
				if (currencyFormatter != null) {
					return ctx.getString(R.string.osm_live_payment_month_cost_descr, currencyFormatter.format(monthlyPriceValue));
				} else {
					return ctx.getString(R.string.osm_live_payment_month_cost_descr_ex, monthlyPriceValue, getPriceCurrencyCode());
				}
			}
		}

		public CharSequence getRenewDescription(@NonNull Context ctx) {
			return "";
		}

		@Nullable
		protected abstract InAppSubscription newInstance(@NonNull String sku);

		public String getDiscountTitle(@NonNull Context ctx, @NonNull InAppSubscription monthlyLiveUpdates) {
			int discountPercent = getDiscountPercent(monthlyLiveUpdates);
			return discountPercent > 0 ? ctx.getString(R.string.osm_live_payment_discount_descr, discountPercent + "%") : "";
		}

		public String getDiscount(@NonNull InAppSubscription monthlyLiveUpdates) {
			int discountPercent = getDiscountPercent(monthlyLiveUpdates);
			return discountPercent > 0 ? "-" + discountPercent + "%" : "";
		}

		public int getDiscountPercent(@NonNull InAppSubscription monthlyLiveUpdates) {
			double regularMonthlyPrice = monthlyLiveUpdates.getPriceValue();
			if (introductoryInfo != null) {
				double introductoryMonthlyPrice = introductoryInfo.getIntroductoryMonthlyPriceValue();
				if (introductoryMonthlyPrice >= 0 && regularMonthlyPrice > 0 && introductoryMonthlyPrice < regularMonthlyPrice) {
					return (int) ((1 - introductoryMonthlyPrice / regularMonthlyPrice) * 100d);
				}
			} else {
				double monthlyPriceValue = getMonthlyPriceValue();
				if (regularMonthlyPrice >= 0 && monthlyPriceValue > 0 && monthlyPriceValue < regularMonthlyPrice) {
					return (int) ((1 - monthlyPriceValue / regularMonthlyPrice) * 100d);
				}
			}
			return 0;
		}

		public String getRegularPrice(@NonNull Context ctx, @NonNull InAppSubscription monthlyLiveUpdates) {
			Period period = getSubscriptionPeriod();
			if (period != null) {
				double price = monthlyLiveUpdates.getPriceValue();
				double months = period.getUnit().getMonthsValue();
				double regularPrice = price * months;
				return getFormattedPrice(ctx, regularPrice, getPriceCurrencyCode());
			}
			return "";
		}

		public String getPriceWithPeriod(Context ctx) {
			return getPrice(ctx);
		}

		public boolean hasDiscountOffer() {
			return getIntroductoryInfo() != null || isUpgrade();
		}
	}

	public static abstract class InAppPurchaseDepthContours extends InAppPurchase {

		protected InAppPurchaseDepthContours(int featureId, String sku) {
			super(featureId, sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.sea_depth_maps_price);
		}
	}

	public static abstract class InAppPurchaseContourLines extends InAppPurchase {

		protected InAppPurchaseContourLines(int featureId, String sku) {
			super(featureId, sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.srtm_plugin_price);
		}
	}

	protected static abstract class InAppPurchaseMonthlySubscription extends InAppSubscription {

		InAppPurchaseMonthlySubscription(int featureId, String skuNoVersion, int version) {
			super(featureId, skuNoVersion, version);
			donationSupported = true;
		}

		InAppPurchaseMonthlySubscription(int featureId, @NonNull String sku) {
			super(featureId, sku);
			donationSupported = true;
		}

		@Override
		public int getPeriodTypeString() {
			return R.string.monthly_subscription;
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

	protected static abstract class InAppPurchaseQuarterlySubscription extends InAppSubscription {

		InAppPurchaseQuarterlySubscription(int featureId, String skuNoVersion, int version) {
			super(featureId, skuNoVersion, version);
		}

		InAppPurchaseQuarterlySubscription(int featureId, @NonNull String sku) {
			super(featureId, sku);
		}

		@Override
		public int getPeriodTypeString() {
			return R.string.three_months_subscription;
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

	protected static abstract class InAppPurchaseAnnualSubscription extends InAppSubscription {

		InAppPurchaseAnnualSubscription(int featureId, String skuNoVersion, int version) {
			super(featureId, skuNoVersion, version);
		}

		InAppPurchaseAnnualSubscription(int featureId, @NonNull String sku) {
			super(featureId, sku);
		}

		@Override
		public int getPeriodTypeString() {
			return R.string.annual_subscription;
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

	public abstract static class InAppPurchaseLiveUpdatesOldMonthly extends InAppPurchaseMonthlySubscription {

		InAppPurchaseLiveUpdatesOldMonthly(int featureId, String sku) {
			super(featureId, sku);
		}

		@Override
		public boolean isLegacy() {
			return true;
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

	public static class PurchaseInfo {
		private String sku;
		private String orderId;
		private String purchaseToken;
		private long purchaseTime;
		private int purchaseState;
		private boolean acknowledged;
		private boolean autoRenewing;

		PurchaseInfo(String sku, String orderId, String purchaseToken, long purchaseTime,
							int purchaseState, boolean acknowledged, boolean autoRenewing) {
			this.sku = sku;
			this.orderId = orderId;
			this.purchaseToken = purchaseToken;
			this.purchaseTime = purchaseTime;
			this.purchaseState = purchaseState;
			this.acknowledged = acknowledged;
			this.autoRenewing = autoRenewing;
		}

		PurchaseInfo(@NonNull String json) throws JSONException {
			parseJson(json);
		}

		public String getSku() {
			return sku;
		}

		public String getOrderId() {
			return orderId;
		}

		public String getPurchaseToken() {
			return purchaseToken;
		}

		public long getPurchaseTime() {
			return purchaseTime;
		}

		public int getPurchaseState() {
			return purchaseState;
		}

		public boolean isAcknowledged() {
			return acknowledged;
		}

		public boolean isAutoRenewing() {
			return autoRenewing;
		}

		public String toJson() {
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("sku", sku);
			jsonMap.put("orderId", orderId);
			jsonMap.put("purchaseToken", purchaseToken);
			jsonMap.put("purchaseTime", purchaseTime);
			jsonMap.put("purchaseState", purchaseState);
			jsonMap.put("acknowledged", acknowledged);
			jsonMap.put("autoRenewing", autoRenewing);
			return new JSONObject(jsonMap).toString();
		}

		public void parseJson(@NonNull String json) throws JSONException {
			JSONObject jsonObj = new JSONObject(json);
			if (jsonObj.has("sku")) {
				this.sku = jsonObj.getString("sku");
			}
			if (jsonObj.has("orderId")) {
				this.orderId = jsonObj.getString("orderId");
			}
			if (jsonObj.has("purchaseToken")) {
				this.purchaseToken = jsonObj.getString("purchaseToken");
			}
			if (jsonObj.has("purchaseTime")) {
				this.purchaseTime = jsonObj.getLong("purchaseTime");
			}
			if (jsonObj.has("purchaseState")) {
				this.purchaseState = jsonObj.getInt("purchaseState");
			}
			if (jsonObj.has("acknowledged")) {
				this.acknowledged = jsonObj.getBoolean("acknowledged");
			}
			if (jsonObj.has("autoRenewing")) {
				this.autoRenewing = jsonObj.getBoolean("autoRenewing");
			}
		}
	}
}

