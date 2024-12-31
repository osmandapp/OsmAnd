package net.osmand.plus.chooseplan.button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;

public class OneTimePaymentButton extends PriceButton<InAppPurchase> {

	public OneTimePaymentButton(@NonNull String id, @NonNull InAppPurchase purchaseItem) {
		super(id, purchaseItem);
	}

	@Override
	public void onApply(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (app.isPlusVersionInApp()) {
			app.logEvent("in_app_purchase_redirect_from_banner");
		} else {
			app.logEvent("paid_version_redirect_from_banner");
		}
		OsmandInAppPurchaseActivity.purchaseFullVersion(activity);
	}
}
