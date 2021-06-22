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
//				View buttonPurchased = inflater.inflate(R.layout.purchase_dialog_card_button_active_ex, container, false);
//				TextViewEx title = (TextViewEx) buttonPurchased.findViewById(R.id.title);
//				TextViewEx description = (TextViewEx) buttonPurchased.findViewById(R.id.description);
//				TextViewEx descriptionContribute = (TextViewEx) buttonPurchased.findViewById(R.id.description_contribute);
//				descriptionContribute.setVisibility(s.isDonationSupported() ? View.VISIBLE : View.GONE);
//				TextViewEx buttonTitle = (TextViewEx) buttonPurchased.findViewById(R.id.button_title);
//				View buttonView = buttonPurchased.findViewById(R.id.button_view);
//				View buttonCancelView = buttonPurchased.findViewById(R.id.button_cancel_view);
//				View div = buttonPurchased.findViewById(R.id.div);
//				AppCompatImageView rightImage = (AppCompatImageView) buttonPurchased.findViewById(R.id.right_image);

//				CharSequence priceTitle = hasIntroductoryInfo ?
//						introductoryInfo.getFormattedDescription(themedCtx, buttonTitle.getCurrentTextColor()) : s.getPriceWithPeriod(app);
//				title.setText(s.getTitle(app));
//				if (Algorithms.isEmpty(descriptionText.toString())) {
//					description.setVisibility(View.GONE);
//				} else {
//					description.setText(descriptionText);
//				}
//				buttonTitle.setText(priceTitle);
//				buttonView.setVisibility(View.VISIBLE);
//				buttonCancelView.setVisibility(View.GONE);
//				buttonPurchased.setOnClickListener(null);
//				div.setVisibility(View.GONE);
//				rightImage.setVisibility(View.GONE);
//				if (s.isDonationSupported()) {
//					buttonPurchased.setOnClickListener(v -> {
//						showDonationSettings();
//						dismiss();
//					});
//				} else {
//					buttonPurchased.setOnClickListener(null);
//				}
//				container.addView(buttonPurchased);

//				View buttonCancel = inflater.inflate(R.layout.purchase_dialog_card_button_active_ex, container, false);
//				title = (TextViewEx) buttonCancel.findViewById(R.id.title);
//				description = (TextViewEx) buttonCancel.findViewById(R.id.description);
//				buttonView = buttonCancel.findViewById(R.id.button_view);
//				buttonCancelView = buttonCancel.findViewById(R.id.button_cancel_view);
//				div = buttonCancel.findViewById(R.id.div);
//				rightImage = (AppCompatImageView) buttonCancel.findViewById(R.id.right_image);

//				title.setText(getString(R.string.osm_live_payment_current_subscription));
//				description.setText(s.getRenewDescription(themedCtx));
//				buttonView.setVisibility(View.GONE);
//				buttonCancelView.setVisibility(View.VISIBLE);
//				buttonCancelView.setOnClickListener(v -> purchaseHelper.manageSubscription(app, s.getSku()));
//				div.setVisibility(View.VISIBLE);
//				rightImage.setVisibility(View.VISIBLE);
//				container.addView(buttonCancel);
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