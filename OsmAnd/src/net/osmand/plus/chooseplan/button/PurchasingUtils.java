package net.osmand.plus.chooseplan.button;

import android.text.Spannable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionIntroductoryInfo;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PurchasingUtils {

	public static final String PROMO_PREFIX = "promo_";

	public static List<SubscriptionButton> collectSubscriptionButtons(@NonNull OsmandApplication app,
	                                                                  @NonNull InAppPurchaseHelper purchaseHelper,
	                                                                  @NonNull List<InAppSubscription> subscriptions,
	                                                                  boolean nightMode) {
		List<SubscriptionButton> priceButtons = new ArrayList<>();
		InAppSubscription monthlySubscription = purchaseHelper.getMonthlySubscription();
		int primaryTextColor = ColorUtilities.getPrimaryTextColor(app, nightMode);

		for (InAppSubscription subscription : subscriptions) {
			SubscriptionButton subscriptionButton = new SubscriptionButton(subscription.getSku(), subscription);
			subscriptionButton.setTitle(subscription.getTitle(app));

			String discount = subscription.getDiscount(monthlySubscription);
			String discountTitle = subscription.getDiscountTitle(app, monthlySubscription);

			InAppSubscriptionIntroductoryInfo info = subscription.getIntroductoryInfo();
			if (info != null) {
				Pair<Spannable, Spannable> pair = info.getFormattedDescription(app, primaryTextColor);
				subscriptionButton.setDiscount(discount);
				subscriptionButton.setDiscountApplied(!Algorithms.isEmpty(discount));
				subscriptionButton.setPrice(pair.first.toString());
				subscriptionButton.setDescription(pair.second.toString());
			} else {
				subscriptionButton.setPrice(subscription.getPriceWithPeriod(app));

				boolean discountApplied = !Algorithms.stringsEqual(subscription.getPrice(app), subscription.getOriginalPrice(app));
				subscriptionButton.setDiscountApplied(discountApplied);
				subscriptionButton.setDiscount(discountApplied ? discount : discountTitle);

				if (!Algorithms.isEmpty(discount) && discountApplied) {
					String pattern = app.getString(R.string.ltr_or_rtl_combine_via_colon);
					String regularPrice = subscription.getRegularPrice(app);
					subscriptionButton.setDescription(String.format(pattern, app.getString(R.string.regular_price), regularPrice));
				}
			}
			priceButtons.add(subscriptionButton);
		}
		return priceButtons;
	}

	@Nullable
	public static OneTimePaymentButton getOneTimePaymentButton(OsmandApplication app) {
		InAppPurchase purchase = getPlanTypePurchase(app);
		if (purchase == null) return null;

		OneTimePaymentButton btn = new OneTimePaymentButton(purchase.getSku(), purchase);
		btn.setTitle(app.getString(R.string.in_app_purchase_desc));
		btn.setPrice(purchase.getPrice(app));
		return btn;
	}

	@Nullable
	public static InAppPurchase getPlanTypePurchase(@NonNull OsmandApplication app) {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			return purchaseHelper.getFullVersion();
		}
		return null;
	}

	public static void createPromoItem(@NonNull ContextMenuAdapter adapter,
	                                   @NonNull MapActivity mapActivity,
	                                   @NonNull OsmAndFeature feature,
	                                   @NonNull String id,
	                                   @StringRes int titleId,
	                                   @StringRes int descriptionId) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();

		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			ChoosePlanFragment.showInstance(mapActivity, feature);
			return false;
		};

		adapter.addItem(new ContextMenuItem(PROMO_PREFIX + id)
				.setLayout(R.layout.list_item_promo)
				.setTitleId(titleId, mapActivity)
				.setDescription(app.getString(descriptionId))
				.setIcon(feature.getIconId(nightMode))
				.setUseNaturalIconColor(true)
				.setListener(listener));
	}

	public static void removePromoItems(ContextMenuAdapter contextMenuAdapter) {
		Iterator<ContextMenuItem> iterator = contextMenuAdapter.getItems().listIterator();
		while (iterator.hasNext()) {
			ContextMenuItem item = iterator.next();
			if (item.getId().startsWith(PROMO_PREFIX)) {
				iterator.remove();
			}
		}
	}
}