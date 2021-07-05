package net.osmand.plus.chooseplan.button;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscriptionIntroductoryInfo;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class PriceButtonsUtils {

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
}