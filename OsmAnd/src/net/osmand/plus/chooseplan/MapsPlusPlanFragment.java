package net.osmand.plus.chooseplan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.button.OneTimePaymentButton;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class MapsPlusPlanFragment extends SelectedPlanFragment {

	@Override
	protected void collectPriceButtons(List<PriceButton<?>> priceButtons) {
		priceButtons.clear();
		priceButtons.addAll(collectPriceButtons(app, purchaseHelper, nightMode));
	}

	@Override
	protected void collectFeatures() {
		previewFeatures.addAll(OsmAndFeature.MAPS_PLUS_PREVIEW_FEATURES);

		for (OsmAndFeature feature : previewFeatures) {
			if (feature.isAvailableInMapsPlus()) {
				includedFeatures.add(feature);
			} else {
				noIncludedFeatures.add(feature);
			}
		}
	}

	@Override
	protected String getHeader() {
		return getString(R.string.maps_plus);
	}

	@Override
	protected String getTagline() {
		return getString(R.string.osmand_maps_plus_tagline);
	}

	@Override
	protected int getHeaderIconId() {
		return R.drawable.ic_action_osmand_maps_plus;
	}

	public static List<PriceButton<?>> collectPriceButtons(OsmandApplication app,
														   InAppPurchaseHelper purchaseHelper,
														   boolean nightMode) {
		List<InAppSubscription> subscriptions = getVisibleSubscriptions(app, purchaseHelper);
		List<PriceButton<?>> priceButtons = new ArrayList<>(
				PurchasingUtils.collectSubscriptionButtons(app, purchaseHelper, subscriptions, nightMode));
		OneTimePaymentButton oneTimePaymentButton = PurchasingUtils.getOneTimePaymentButton(app);
		if (oneTimePaymentButton != null) {
			priceButtons.add(oneTimePaymentButton);
		}
		return priceButtons;
	}

	protected static List<InAppSubscription> getVisibleSubscriptions(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		InAppPurchases purchases = app.getInAppPurchaseHelper().getInAppPurchases();
		List<InAppSubscription> subscriptions = new ArrayList<>();
		List<InAppSubscription> visibleSubscriptions = purchaseHelper.getSubscriptions().getVisibleSubscriptions();
		for (InAppSubscription subscription : visibleSubscriptions) {
			if (purchases.isMaps(subscription)) {
				subscriptions.add(subscription);
			}
		}
		return subscriptions;
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			new MapsPlusPlanFragment().show(manager, TAG);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String selectedButtonId) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(SELECTED_PRICE_BTN_ID, selectedButtonId);
			MapsPlusPlanFragment fragment = new MapsPlusPlanFragment();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}
