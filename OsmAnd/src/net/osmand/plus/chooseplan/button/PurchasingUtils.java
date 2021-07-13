package net.osmand.plus.chooseplan.button;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
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

	public static String PROMO_PREFIX = "promo_";

	public static List<SubscriptionButton> collectSubscriptionButtons(OsmandApplication app,
																	  InAppPurchaseHelper purchaseHelper,
																	  List<InAppSubscription> subscriptions) {
		List<SubscriptionButton> priceButtons = new ArrayList<>();
		InAppSubscription maxDiscountSubscription = null;
		double maxDiscount = 0;
		boolean anyPurchased = false;
		for (InAppSubscription s : subscriptions) {
			if (s.isPurchased()) {
				anyPurchased = true;
			}
			double discount = s.getDiscountPercent(purchaseHelper.getMonthlyLiveUpdates());
			if (discount > maxDiscount) {
				maxDiscountSubscription = s;
				maxDiscount = discount;
			}
		}
		boolean maxDiscountAction = maxDiscountSubscription != null && maxDiscountSubscription.hasDiscountOffer();
		for (InAppSubscription s : subscriptions) {
			InAppSubscriptionIntroductoryInfo introductoryInfo = s.getIntroductoryInfo();
			boolean hasIntroductoryInfo = introductoryInfo != null;
			CharSequence descriptionText = s.getDescription(app);
			if (s.isPurchased()) {
				SubscriptionButton priceBtn = new SubscriptionButton(s.getSkuNoVersion(), s);
				priceBtn.setTitle(s.getTitle(app));

				boolean showSolidButton = !anyPurchased
						&& (!maxDiscountAction || hasIntroductoryInfo || maxDiscountSubscription.isUpgrade());
				int descriptionColor = Color.RED; // tvTitle.getCurrentTextColor();
				// todo
				// showSolidButton ? buttonExTitle.getCurrentTextColor() : buttonTitle.getCurrentTextColor();
				CharSequence priceTitle = hasIntroductoryInfo ?
						introductoryInfo.getFormattedDescription(app, descriptionColor) : s.getPriceWithPeriod(app);
				priceBtn.setPrice(priceTitle);

				String discount = s.getDiscount(purchaseHelper.getMonthlyLiveUpdates());
				if (!Algorithms.isEmpty(discount)) {
					priceBtn.setDiscount(discount);

					String regularPrice = s.getRegularPrice(app, purchaseHelper.getMonthlyLiveUpdates());
					priceBtn.setRegularPrice(regularPrice);
				}

				priceButtons.add(priceBtn);
			} else {
				SubscriptionButton priceBtn = new SubscriptionButton(s.getSkuNoVersion(), s);
				priceBtn.setTitle(s.getTitle(app));

				boolean showSolidButton = !anyPurchased
						&& (!maxDiscountAction || hasIntroductoryInfo || maxDiscountSubscription.isUpgrade());
				int descriptionColor = Color.RED; // tvTitle.getCurrentTextColor();
				// todo
				// showSolidButton ? buttonExTitle.getCurrentTextColor() : buttonTitle.getCurrentTextColor();
				CharSequence priceTitle = hasIntroductoryInfo ?
						introductoryInfo.getFormattedDescription(app, descriptionColor) : s.getPriceWithPeriod(app);
				priceBtn.setPrice(priceTitle);

				String discount = s.getDiscount(purchaseHelper.getMonthlyLiveUpdates());
				if (!Algorithms.isEmpty(discount)) {
					priceBtn.setDiscount(discount);

					String regularPrice = s.getRegularPrice(app, purchaseHelper.getMonthlyLiveUpdates());
					priceBtn.setRegularPrice(regularPrice);
				}

				priceButtons.add(priceBtn);
			}
		}
		return priceButtons;
	}

	@Nullable
	public static OneTimePaymentButton getOneTimePaymentButton(OsmandApplication app) {
		InAppPurchase purchase = getPlanTypePurchase(app);
		if (purchase == null) return null;

		String title = app.getString(R.string.in_app_purchase_desc);
		OneTimePaymentButton btn = new OneTimePaymentButton(title, purchase);
		btn.setTitle(title);
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

		ItemClickListener listener = (adapter1, itemId, position, isChecked, viewCoordinates) -> {
			ChoosePlanFragment.showInstance(mapActivity, feature);
			return false;
		};

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(PROMO_PREFIX + id)
				.setLayout(R.layout.list_item_promo)
				.setTitleId(titleId, mapActivity)
				.setDescription(app.getString(descriptionId))
				.setIcon(feature.getIconId(nightMode))
				.setSkipPaintingWithoutColor(true)
				.setListener(listener)
				.createItem());
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