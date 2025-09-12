package net.osmand.plus.inapp;

import static android.graphics.Typeface.DEFAULT;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.Period;
import net.osmand.Period.PeriodUnit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InAppPurchases {

	protected static final int FULL_VERSION_ID = 1;
	protected static final int DEPTH_CONTOURS_ID = 2;
	protected static final int CONTOUR_LINES_ID = 3;

	protected static final int LIVE_UPDATES_ID = 5;
	protected static final int OSMAND_PRO_ID = 6;
	protected static final int MAPS_ID = 7;

	protected static final int[] LIVE_UPDATES_SCOPE = new int[]{
			FULL_VERSION_ID,
			DEPTH_CONTOURS_ID,
			CONTOUR_LINES_ID,
	};

	protected static final int[] OSMAND_PRO_SCOPE = new int[]{
			FULL_VERSION_ID,
			DEPTH_CONTOURS_ID,
			CONTOUR_LINES_ID,
			LIVE_UPDATES_ID,
	};

	protected static final int[] MAPS_SCOPE = new int[]{
			FULL_VERSION_ID,
	};

	protected InAppPurchase fullVersion;
	protected InAppPurchase depthContours;
	protected InAppPurchase contourLines;
	protected InAppSubscription monthlySubscription;
	protected InAppSubscription legacyMonthlySubscription;
	protected InAppSubscriptionList subscriptions;
	protected InAppPurchase[] inAppPurchases;

	protected InAppPurchases(OsmandApplication ctx) {
	}

	private static OsmandSettings getSettings(@NonNull Context ctx) {
		return ((OsmandApplication) ctx.getApplicationContext()).getSettings();
	}

	@Nullable
	public InAppPurchase getFullVersion() {
		return fullVersion;
	}

	@Nullable
	public InAppPurchase getDepthContours() {
		return depthContours;
	}

	@Nullable
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

	public InAppSubscription getMonthlySubscription() {
		return monthlySubscription;
	}

	@Nullable
	public InAppSubscription getPurchasedMonthlySubscription() {
		if (monthlySubscription.isAnyPurchased()) {
			return monthlySubscription;
		} else if (legacyMonthlySubscription != null && legacyMonthlySubscription.isAnyPurchased()) {
			return legacyMonthlySubscription;
		}
		return null;
	}

	@Nullable
	public InAppSubscription getAnyPurchasedOsmAndProSubscription() {
		List<InAppSubscription> allSubscriptions = subscriptions.getAllSubscriptions();
		for (InAppSubscription subscription : allSubscriptions) {
			if (isOsmAndPro(subscription) && subscription.isPurchased()) {
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
		if (fullVersion != null) {
			purchases.add(fullVersion);
		}
		if (depthContours != null) {
			purchases.add(depthContours);
		}
		if (contourLines != null) {
			purchases.add(contourLines);
		}
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

	public abstract boolean isLiveUpdates(InAppPurchase p);

	public abstract boolean isOsmAndPro(InAppPurchase p);

	public abstract boolean isMaps(InAppPurchase p);

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
			Collections.sort(expiredSubscriptions, (s1, s2) -> {
				int orderS1 = s1.getState().ordinal();
				int orderS2 = s2.getState().ordinal();
				if (orderS1 != orderS2) {
					return (orderS1 < orderS2) ? -1 : ((orderS1 == orderS2) ? 0 : 1);
				}
				return Double.compare(s1.getMonthlyPriceValue(), s2.getMonthlyPriceValue());
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

		public enum PurchaseOrigin {

			UNDEFINED(R.string.shared_string_undefined),
			GOOGLE(R.string.google_play),
			AMAZON(R.string.amazon_market),
			HUAWEI(R.string.huawei_market),
			IOS(R.string.apple_app_store),
			FASTSPRING(R.string.osmand_web_market),
			PROMO(R.string.promo),
			TRIPLTEK_PROMO(R.string.tripltek),
			HUGEROCK_PROMO(R.string.hugerock),
			HMD_PROMO(R.string.hmd);

			private final int storeNameId;

			PurchaseOrigin(@StringRes int storeNameId) {
				this.storeNameId = storeNameId;
			}

			@StringRes
			public int getStoreNameId() {
				return storeNameId;
			}
		}

		private final int featureId;
		private final String sku;
		private String price;
		private String originalPrice;
		private double priceValue;
		private double originalPriceValue;
		private String priceCurrencyCode;
		private PurchaseState purchaseState = PurchaseState.UNKNOWN;
		private PurchaseInfo purchaseInfo;

		double monthlyPriceValue;
		double monthlyOriginalPriceValue;
		boolean donationSupported;

		private NumberFormat currencyFormatter;

		protected InAppPurchase(int featureId, @NonNull String sku) {
			this.featureId = featureId;
			this.sku = sku;
		}

		public boolean isFullVersion() {
			return featureId == FULL_VERSION_ID;
		}

		public boolean isDepthContours() {
			return featureId == DEPTH_CONTOURS_ID;
		}

		public boolean isContourLines() {
			return featureId == CONTOUR_LINES_ID;
		}

		public boolean isLiveUpdates() {
			return featureId == LIVE_UPDATES_ID;
		}

		public boolean isOsmAndPro() {
			return featureId == OSMAND_PRO_ID;
		}

		public boolean isMaps() {
			return featureId == MAPS_ID || featureId == FULL_VERSION_ID;
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

		public String getOriginalPrice(Context ctx) {
			if (!Algorithms.isEmpty(originalPrice)) {
				return originalPrice;
			} else {
				return getDefaultPrice(ctx);
			}
		}

		public void setPrice(String price) {
			this.price = price;
		}

		public void setOriginalPrice(String originalPrice) {
			this.originalPrice = originalPrice;
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

		public double getOriginalPriceValue() {
			return originalPriceValue;
		}

		public void setPriceValue(double priceValue) {
			this.priceValue = priceValue;
		}

		public void setOriginalPriceValue(double originalPriceValue) {
			this.originalPriceValue = originalPriceValue;
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

		private final InAppSubscription subscription;

		private final String introductoryPrice;
		private final long introductoryPriceAmountMicros;
		private final String introductoryPeriodString;
		private final int introductoryCycles;

		private final double introductoryPriceValue;
		private final Period introductoryPeriod;

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

		private String getDisountPeriodString(@NonNull Context ctx, String unitStr, long totalPeriods) {
			if (totalPeriods == 1)
				return unitStr;
			if (AndroidUtils.isLayoutRtl(ctx)) {
				return unitStr + " " + totalPeriods;
			} else {
				return totalPeriods + " " + unitStr;
			}
		}

		public Pair<Spannable, Spannable> getFormattedDescription(@NonNull Context ctx, @ColorInt int textColor) {
			long totalPeriods = getTotalPeriods();
			String singleUnitStr = getUnitString(ctx).toLowerCase();
			String unitStr = getTotalUnitsString(ctx, false).toLowerCase();
			long numberOfUnits = introductoryPeriod.getNumberOfUnits();
			Period subscriptionPeriod = subscription.getSubscriptionPeriod();
			long originalNumberOfUnits = subscriptionPeriod != null ? subscriptionPeriod.getNumberOfUnits() : 1;
			String originalUnitsStr = getTotalUnitsString(ctx, true).toLowerCase();
			String originalPriceStr = subscription.getOriginalPrice(ctx);
			String priceStr = introductoryPrice;

			String pricePeriod;
			String originalPricePeriod;

			if (AndroidUtils.isLayoutRtl(ctx)) {
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
			Spannable mainPart = new SpannableStringBuilder(ctx.getString(firstPartRes, periodPriceStr, getDisountPeriodString(ctx, unitStr, totalPeriods)));
			Spannable thenPart = new SpannableStringBuilder(ctx.getString(R.string.get_discount_second_part, originalPricePeriod));
			mainPart.setSpan(new ForegroundColorSpan(textColor), 0, mainPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mainPart.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), 0, mainPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			int secondaryTextColor = ColorUtilities.getColorWithAlpha(textColor, 0.5f);
			thenPart.setSpan(new ForegroundColorSpan(secondaryTextColor), 0, thenPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			thenPart.setSpan(new CustomTypefaceSpan(DEFAULT), 0, thenPart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			return new Pair<>(mainPart, thenPart);
		}
	}

	public abstract static class InAppSubscription extends InAppPurchase {

		private static final int MIN_SHOWN_DISCOUNT_PERCENT = 10;

		private final Map<String, InAppSubscription> upgrades = new ConcurrentHashMap<>();
		private final String skuNoVersion;
		private String subscriptionPeriodString;
		private Period subscriptionPeriod;
		private boolean upgrade;
		private SubscriptionState state = SubscriptionState.UNDEFINED;
		private SubscriptionState previousState = SubscriptionState.UNDEFINED;
		private long startTime;
		private long expireTime;

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
				for (SubscriptionState state : values()) {
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

		public long getStartTime() {
			return startTime;
		}

		public void setStartTime(@NonNull Context ctx, long expireTime) {
			this.startTime = expireTime;
			storeStartTime(ctx, expireTime);
		}

		private CommonPreference<Long> getStartTimePref(@NonNull Context ctx) {
			return getSettings(ctx).registerLongPreference(getSku() + "_start_time", 0L).makeGlobal();
		}

		boolean restoreStartTime(@NonNull Context ctx) {
			Long expireTime = getStartTimePref(ctx).get();
			if (expireTime != null) {
				this.startTime = expireTime;
				return true;
			}
			return false;
		}

		void storeStartTime(@NonNull Context ctx, long expireTime) {
			getStartTimePref(ctx).set(expireTime);
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
					introductoryInfo = new InAppSubscriptionIntroductoryInfo(this, "50 EUR", 50000000L, "P1Y", 1);
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
			return discountPercent > MIN_SHOWN_DISCOUNT_PERCENT ? ctx.getString(R.string.osm_live_payment_discount_descr, discountPercent + "%") : "";
		}

		public String getDiscount(@NonNull InAppSubscription monthlyLiveUpdates) {
			int discountPercent = getDiscountPercent(monthlyLiveUpdates);
			return discountPercent > MIN_SHOWN_DISCOUNT_PERCENT ? "-" + discountPercent + "%" : "";
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

		public String getRegularPrice(@NonNull Context ctx) {
			return getFormattedPrice(ctx, getOriginalPriceValue(), getPriceCurrencyCode());
		}

		public String getPriceWithPeriod(Context ctx) {
			return getPrice(ctx);
		}

		public boolean hasDiscountOffer() {
			return getIntroductoryInfo() != null || isUpgrade();
		}
	}

	public abstract static class InAppPurchaseDepthContours extends InAppPurchase {

		protected InAppPurchaseDepthContours(int featureId, String sku) {
			super(featureId, sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.sea_depth_maps_price);
		}
	}

	public abstract static class InAppPurchaseContourLines extends InAppPurchase {

		protected InAppPurchaseContourLines(int featureId, String sku) {
			super(featureId, sku);
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.srtm_plugin_price);
		}
	}

	protected abstract static class InAppPurchaseMonthlySubscription extends InAppSubscription {

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
		public void setOriginalPriceValue(double originalPriceValue) {
			super.setOriginalPriceValue(originalPriceValue);
			monthlyOriginalPriceValue = originalPriceValue;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_monthly_price);
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

	protected abstract static class InAppPurchaseQuarterlySubscription extends InAppSubscription {

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
		public void setOriginalPriceValue(double originalPriceValue) {
			super.setOriginalPriceValue(originalPriceValue);
			monthlyOriginalPriceValue = originalPriceValue / 3d;
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

	protected abstract static class InAppPurchaseAnnualSubscription extends InAppSubscription {

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
		public void setOriginalPriceValue(double originalPriceValue) {
			super.setOriginalPriceValue(originalPriceValue);
			monthlyOriginalPriceValue = originalPriceValue / 12d;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_price);
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return ctx.getString(R.string.osm_pro_annual_monthly_price);
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

	public static class InAppPurchaseExternalSubscription extends InAppSubscription {

		private final PurchaseOrigin origin;
		private final int[] scope;
		private String defaultPrice;
		private String defaultMonthlyPrice;
		private int monthlyDuration;

		private InAppPurchaseExternalSubscription(int featureId, int[] scope, @NonNull String sku,
												  @NonNull PurchaseOrigin origin) {
			super(featureId, sku);
			this.scope = scope;
			this.origin = origin;
		}

		public PurchaseOrigin getOrigin() {
			return origin;
		}

		@NonNull
		@Override
		public int[] getScope() {
			return scope;
		}

		@Override
		public boolean isLegacy() {
			return false;
		}

		@Override
		public String getDefaultPrice(Context ctx) {
			return defaultPrice == null ? super.getDefaultPrice(ctx) : defaultPrice;
		}

		@Override
		public String getDefaultMonthlyPrice(Context ctx) {
			return defaultMonthlyPrice == null ? super.getDefaultMonthlyPrice(ctx) : defaultMonthlyPrice;
		}

		@Override
		public int getPeriodTypeString() {
			if (monthlyDuration == 1) {
				return R.string.monthly_subscription;
			} else if (monthlyDuration == 3) {
				return R.string.three_months_subscription;
			} else if (monthlyDuration == 12) {
				return R.string.annual_subscription;
			} else if (monthlyDuration == 36) {
				return R.string.three_years_subscription;
			} else {
				return R.string.monthly_subscription;
			}
		}

		@Override
		public void setPriceValue(double priceValue) {
			super.setPriceValue(priceValue);
			monthlyPriceValue = priceValue / monthlyDuration;
		}

		@Override
		public void setOriginalPriceValue(double originalPriceValue) {
			super.setOriginalPriceValue(originalPriceValue);
			monthlyOriginalPriceValue = originalPriceValue / monthlyDuration;
		}

		@Override
		public CharSequence getTitle(Context ctx) {
			if (monthlyDuration == 1) {
				return ctx.getString(R.string.osm_live_payment_monthly_title);
			} else if (monthlyDuration == 3) {
				return ctx.getString(R.string.osm_live_payment_3_months_title);
			} else if (monthlyDuration == 12) {
				return ctx.getString(R.string.osm_live_payment_annual_title);
			} else if (monthlyDuration == 36) {
				return ctx.getString(R.string.osm_live_payment_3_years_title);
			} else {
				return ctx.getString(R.string.osm_live_payment_monthly_title);
			}
		}

		@Override
		public String getPriceWithPeriod(Context ctx) {
			String period;
			if (monthlyDuration == 1) {
				period = ctx.getString(R.string.month);
			} else if (monthlyDuration == 3) {
				period = ctx.getString(R.string.months_3);
			} else if (monthlyDuration == 12) {
				period = ctx.getString(R.string.year);
			} else if (monthlyDuration == 36) {
				period = ctx.getString(R.string.years_3);
			} else {
				period = ctx.getString(R.string.month);
			}
			return ctx.getString(R.string.ltr_or_rtl_combine_via_slash_with_space, getPrice(ctx),
					period.toLowerCase());
		}

		@Override
		public CharSequence getRenewDescription(@NonNull Context ctx) {
			if (monthlyDuration == 1) {
				return ctx.getString(R.string.osm_live_payment_renews_monthly);
			} else if (monthlyDuration == 3) {
				return ctx.getString(R.string.osm_live_payment_renews_quarterly);
			} else if (monthlyDuration == 12) {
				return ctx.getString(R.string.osm_live_payment_renews_annually);
			} else if (monthlyDuration == 36) {
				return ctx.getString(R.string.osm_live_payment_renews_3_years);
			} else {
				return ctx.getString(R.string.osm_live_payment_renews_monthly);
			}
		}

		@Override
		public CharSequence getDescription(@NonNull Context ctx) {
			return "";
		}

		@Nullable
		@Override
		protected InAppSubscription newInstance(@NonNull String sku) {
			return null;
		}

		public static InAppPurchaseExternalSubscription buildFromJson(@NonNull OsmandApplication ctx,
																	  @NonNull JSONObject json) throws Exception {
			if (!json.has("cross-platform")) {
				return null;
			}
			boolean crossPlatform = json.getString("cross-platform").equals("true");
			if (!crossPlatform) {
				throw new IllegalArgumentException("Subscription is not cross-platform");
			}
			int monthlyDuration;
			String durationUnit = json.getString("duration_unit");
			if (durationUnit.equals("month")) {
				monthlyDuration = Integer.parseInt(json.getString("duration"));
			} else if (durationUnit.equals("year")) {
				monthlyDuration = Integer.parseInt(json.getString("duration")) * 12;
			} else {
				throw new IllegalArgumentException("Unknown duration unit: " + durationUnit);
			}

			String sku = json.getString("sku");
			if (Algorithms.isEmpty(sku)) {
				throw new IllegalArgumentException("SKU is empty");
			}

			String platform = json.getString("platform");
			if (Algorithms.isEmpty(platform)) {
				throw new IllegalArgumentException("Platform is empty");
			}
			PurchaseOrigin origin = ctx.getInAppPurchaseHelper().getPurchaseOriginByPlatform(platform);

			int defaultPriceMillis = Integer.parseInt(json.getString("defPriceEurMillis"));
			String defaultPrice = String.format(Locale.US, "€%.2f", defaultPriceMillis / 1000d).replace('.', ',');
			String defaultMonthlyPrice = String.format(Locale.US, "€%.2f", defaultPriceMillis / 1000d / monthlyDuration).replace('.', ',');

			boolean featurePro = json.getString("feature_pro").equals("true")
					|| json.getString("feature_pro_no_cloud").equals("true");
			boolean featureMaps = json.getString("feature_maps").equals("true");
			boolean featureLive = json.getString("feature_live_maps").equals("true");

			int featureId;
			int[] scope;
			if (featurePro) {
				featureId = OSMAND_PRO_ID;
				scope = OSMAND_PRO_SCOPE;
			} else if (featureMaps) {
				featureId = FULL_VERSION_ID;
				scope = MAPS_SCOPE;
			} else if (featureLive) {
				featureId = LIVE_UPDATES_ID;
				scope = LIVE_UPDATES_SCOPE;
			} else {
				throw new IllegalArgumentException("Subscription is not supported");
			}
			InAppPurchaseExternalSubscription res = new InAppPurchaseExternalSubscription(featureId, scope, sku, origin);
			res.monthlyDuration = monthlyDuration;
			res.defaultPrice = defaultPrice;
			res.defaultMonthlyPrice = defaultMonthlyPrice;
			return res;
		}
	}

	public static class InAppPurchaseExternalInApp extends InAppPurchase {

		private final PurchaseOrigin origin;

		private InAppPurchaseExternalInApp(int featureId, @NonNull String sku, @NonNull PurchaseOrigin origin) {
			super(featureId, sku);
			this.origin = origin;
		}

		public PurchaseOrigin getOrigin() {
			return origin;
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

		public static InAppPurchaseExternalInApp buildFromJson(@NonNull OsmandApplication ctx,
														@NonNull JSONObject json) throws Exception {
			if (!json.has("cross-platform")) {
				return null;
			}
			boolean crossPlatform = json.getString("cross-platform").equals("true");
			if (!crossPlatform) {
				throw new IllegalArgumentException("InApp is not cross-platform");
			}
			String sku = json.getString("sku");
			if (Algorithms.isEmpty(sku)) {
				throw new IllegalArgumentException("SKU is empty");
			}

			String platform = json.getString("platform");
			if (Algorithms.isEmpty(platform)) {
				throw new IllegalArgumentException("Platform is empty");
			}
			PurchaseOrigin origin = ctx.getInAppPurchaseHelper().getPurchaseOriginByPlatform(platform);

			boolean featurePro = json.getString("feature_pro").equals("true");
			boolean featureMaps = json.getString("feature_maps").equals("true");
			boolean featureContour = json.getString("feature_contours").equals("true");
			boolean featureNautical = json.getString("feature_nautical").equals("true");

			int featureId;
			if (featurePro) {
				featureId = OSMAND_PRO_ID;
			} else if (featureMaps) {
				featureId = FULL_VERSION_ID;
			} else if (featureContour) {
				featureId = CONTOUR_LINES_ID;
			} else if (featureNautical) {
				featureId = DEPTH_CONTOURS_ID;
			} else {
				throw new IllegalArgumentException("InApp is not supported");
			}
			return new InAppPurchaseExternalInApp(featureId, sku, origin);
		}
	}

	public static class PurchaseInfo {

		private List<String> sku = new ArrayList<>();
		private String orderId;
		private String purchaseToken;
		private long purchaseTime;
		private int purchaseState;
		private boolean acknowledged;
		private boolean autoRenewing;

		PurchaseInfo(List<String> sku, String orderId, String purchaseToken, long purchaseTime,
		             int purchaseState, boolean acknowledged, boolean autoRenewing) {
			this.sku.addAll(sku);
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

		public List<String> getSku() {
			return sku;
		}

		public String getOrderId() {
			return orderId;
		}

		public String getPurchaseToken() {
			return purchaseToken;
		}

		protected void setPurchaseToken(String purchaseToken) {
			this.purchaseToken = purchaseToken;
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
			jsonMap.put("sku", sku.get(0));
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
				this.sku.add(jsonObj.getString("sku"));
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

