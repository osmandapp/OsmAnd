package net.osmand.plus.chooseplan.button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;

public class SubscriptionButton extends PriceButton<InAppSubscription> {

	public SubscriptionButton(@NonNull String id, InAppSubscription purchaseItem) {
		super(id, purchaseItem);
	}

	@Override
	public void onApply(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper) {
		if (purchaseItem.isPurchased()) {
			purchaseHelper.manageSubscription(activity, purchaseItem.getSku());
		} else {
			InAppPurchaseHelper.subscribe(activity, purchaseHelper, purchaseItem.getSku());
		}
	}
}
