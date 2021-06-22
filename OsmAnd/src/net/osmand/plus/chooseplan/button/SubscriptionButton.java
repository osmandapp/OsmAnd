package net.osmand.plus.chooseplan.button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;

import static net.osmand.plus.chooseplan.ChoosePlanDialogFragment.subscribe;

public class SubscriptionButton extends PriceButton<InAppSubscription> {

	public SubscriptionButton(@NonNull String id, InAppSubscription purchaseItem) {
		super(id, purchaseItem);
	}

	@Override
	public void onApply(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		subscribe(app, activity, purchaseHelper, purchaseItem.getSku());
	}
}
