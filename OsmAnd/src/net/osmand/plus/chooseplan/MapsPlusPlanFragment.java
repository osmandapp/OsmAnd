package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.button.OneTimePaymentButton;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.chooseplan.button.PriceButtonsUtils;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsPlusPlanFragment extends SelectedPlanFragment {

	public static void showInstance(@NonNull FragmentActivity activity) {
		MapsPlusPlanFragment fragment = new MapsPlusPlanFragment();
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	public OsmAndFeature[] getSubscriptionFeatures() {
		return OsmAndFeature.mapsPlusFeatures;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		includedFeatures.addAll(Arrays.asList(OsmAndFeature.mapsPlusFeatures));

		for (OsmAndFeature feature : OsmAndFeature.values()) {
			if (!OsmAndFeature.isAvailableInMapsPlus(feature)) {
				noIncludedFeatures.add(feature);
			}
		}
	}

	@Override
	protected void collectPriceButtons(List<PriceButton<?>> priceButtons) {
		priceButtons.clear();
		priceButtons.addAll(collectPriceButtons(app, purchaseHelper));
	}

	@Override
	protected int getHeaderBgColorId() {
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
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

	@Override
	protected Drawable getPreviewListCheckmark() {
		return getContentIcon(R.drawable.ic_action_done);
	}

	public static List<PriceButton<?>> collectPriceButtons(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		List<InAppSubscription> subscriptions = getVisibleSubscriptions(app, purchaseHelper);
		List<PriceButton<?>> priceButtons = new ArrayList<>(PriceButtonsUtils.collectSubscriptionButtons(app, purchaseHelper, subscriptions));
		OneTimePaymentButton oneTimePaymentButton = PriceButtonsUtils.getOneTimePaymentButton(app);
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
			if (purchases.isMapsSubscription(subscription)) {
				subscriptions.add(subscription);
			}
		}
		return subscriptions;
	}
}
