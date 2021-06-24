package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;

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
import java.util.List;

public class MapsPlusPlanFragment extends SelectedPlanFragment {

	public static void showInstance(@NonNull FragmentActivity activity) {
		MapsPlusPlanFragment fragment = new MapsPlusPlanFragment();
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	protected void collectPriceButtons(List<PriceButton<?>> priceButtons) {
		priceButtons.clear();
		priceButtons.addAll(collectPriceButtons(app, purchaseHelper));
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
