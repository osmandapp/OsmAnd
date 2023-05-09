package net.osmand.plus.chooseplan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.chooseplan.button.SubscriptionButton;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;

import java.util.ArrayList;
import java.util.List;

public class OsmAndProPlanFragment extends SelectedPlanFragment {

	public static void showInstance(@NonNull FragmentActivity activity) {
		OsmAndProPlanFragment fragment = new OsmAndProPlanFragment();
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String selectedButtonId) {
		Bundle args = new Bundle();
		args.putString(SELECTED_PRICE_BTN_ID, selectedButtonId);
		OsmAndProPlanFragment fragment = new OsmAndProPlanFragment();
		fragment.setArguments(args);
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	protected void collectPriceButtons(List<PriceButton<?>> priceButtons) {
		priceButtons.clear();
		priceButtons.addAll(collectPriceButtons(app, purchaseHelper, nightMode));
	}

	@Override
	protected void collectFeatures() {
		previewFeatures.addAll(OsmAndFeature.OSMAND_PRO_PREVIEW_FEATURES);
		includedFeatures.addAll(OsmAndFeature.OSMAND_PRO_PREVIEW_FEATURES);
	}

	@Override
	protected String getHeader() {
		return getString(R.string.osmand_pro);
	}

	@Override
	protected String getTagline() {
		return getString(R.string.osmand_pro_tagline);
	}

	@Override
	protected int getHeaderIconId() {
		return nightMode ?
				R.drawable.ic_action_osmand_pro_logo_colored_night :
				R.drawable.ic_action_osmand_pro_logo_colored;
	}

	public static List<PriceButton<?>> collectPriceButtons(OsmandApplication app,
														   InAppPurchaseHelper purchaseHelper,
	                                                       boolean nightMode) {
		List<InAppSubscription> subscriptions = getVisibleSubscriptions(app, purchaseHelper);
		List<SubscriptionButton> subscriptionButtons =
				PurchasingUtils.collectSubscriptionButtons(app, purchaseHelper, subscriptions, nightMode);
		return new ArrayList<>(subscriptionButtons);
	}

	public static List<InAppSubscription> getVisibleSubscriptions(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		InAppPurchases purchases = app.getInAppPurchaseHelper().getInAppPurchases();
		List<InAppSubscription> subscriptions = new ArrayList<>();
		List<InAppSubscription> visibleSubscriptions = purchaseHelper.getSubscriptions().getVisibleSubscriptions();
		for (InAppSubscription subscription : visibleSubscriptions) {
			if (purchases.isOsmAndProSubscription(subscription)) {
				subscriptions.add(subscription);
			}
		}
		return subscriptions;
	}
}