package net.osmand.plus.base.globallistener.implementations;

import androidx.fragment.app.Fragment;

import net.osmand.plus.base.globallistener.BaseGlobalListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;

public class GlobalInAppPurchaseListener extends BaseGlobalListener implements InAppPurchaseListener {

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		for (Fragment f : getAddedFragments()) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onError(taskType, error);
			}
		}
	}

	@Override
	public void onGetItems() {
		for (Fragment f : getAddedFragments()) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onGetItems();
			}
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		for (Fragment f : getAddedFragments()) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onItemPurchased(sku, active);
			}
		}
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		for (Fragment f : getAddedFragments()) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).showProgress(taskType);
			}
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		for (Fragment f : getAddedFragments()) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).dismissProgress(taskType);
			}
		}
	}
	
}
