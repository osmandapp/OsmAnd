package net.osmand.plus.views.mapwidgets.configure;

import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;

public class BaseWidgetPurchaseFragment extends BaseOsmAndFragment implements InAppPurchaseListener {

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {}

	@Override
	public void onGetItems() {}

	@Override
	public void onItemPurchased(String sku, boolean active) {}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {}

}
