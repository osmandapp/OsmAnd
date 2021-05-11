package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.lang.ref.WeakReference;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {

	public InAppPurchaseHelperImpl(OsmandApplication ctx) {
		super(ctx);
		purchases = new InAppPurchasesImpl(ctx);
	}

	@Override
	public void isInAppPurchaseSupported(@NonNull Activity activity, @Nullable InAppPurchaseInitCallback callback) {

	}

	@Override
	protected void execImpl(@NonNull InAppPurchaseTaskType taskType, @NonNull InAppCommand command) {

	}

	@Override
	public void purchaseFullVersion(@NonNull Activity activity) throws UnsupportedOperationException {

	}

	@Override
	public void purchaseDepthContours(@NonNull Activity activity) throws UnsupportedOperationException {

	}

	@Override
	public void purchaseContourLines(@NonNull Activity activity) throws UnsupportedOperationException {

	}

	@Override
	public void manageSubscription(@NonNull Context ctx, @Nullable String sku) {

	}

	@Override
	protected InAppCommand getPurchaseSubscriptionCommand(WeakReference<Activity> activity, String sku, String userInfo) throws UnsupportedOperationException {
		return null;
	}

	@Override
	protected InAppCommand getRequestInventoryCommand() throws UnsupportedOperationException {
		return null;
	}

	@Override
	protected boolean isBillingManagerExists() {
		return false;
	}

	@Override
	protected void destroyBillingManager() {

	}
}