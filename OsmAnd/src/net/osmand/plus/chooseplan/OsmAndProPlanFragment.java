package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.chooseplan.button.PriceButtonsUtils;
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

	@Override
	protected void collectPriceButtons(List<PriceButton<?>> priceButtons) {
		priceButtons.clear();
		priceButtons.addAll(collectPriceButtons(app, purchaseHelper));
	}

	@Override
	protected void collectFeatures() {
		previewFeatures.addAll(OsmAndFeature.OSMAND_PRO_PREVIEW_FEATURES);
		includedFeatures.addAll(OsmAndFeature.OSMAND_PRO_PREVIEW_FEATURES);
	}

	@Override
	protected int getHeaderBgColorId() {
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
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
		return R.drawable.ic_action_osmand_pro_logo;
	}

	@Override
	protected Drawable getPreviewListCheckmark() {
		return getCheckmark();
	}

	public static List<PriceButton<?>> collectPriceButtons(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		List<InAppSubscription> subscriptions = getVisibleSubscriptions(app, purchaseHelper);
		return new ArrayList<>(PriceButtonsUtils.collectSubscriptionButtons(app, purchaseHelper, subscriptions));
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