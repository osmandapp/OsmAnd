package net.osmand.plus.base.globallistener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.base.globallistener.implementations.GlobalGpxSelectionListener;
import net.osmand.plus.base.globallistener.implementations.GlobalInAppPurchaseListener;

import java.util.Arrays;
import java.util.List;

public class GlobalListenersManager {

	private final GlobalInAppPurchaseListener inAppPurchaseListener = new GlobalInAppPurchaseListener();
	private final GlobalGpxSelectionListener gpxSelectionListener = new GlobalGpxSelectionListener();

	private final List<BaseGlobalListener> globalListeners = Arrays.asList(inAppPurchaseListener, gpxSelectionListener);

	public void setActivity(@Nullable AppCompatActivity activity) {
		for (BaseGlobalListener globalListener : globalListeners) {
			globalListener.setActivity(activity);
		}
	}

	@NonNull
	public GlobalInAppPurchaseListener getInAppPurchaseListener() {
		return inAppPurchaseListener;
	}

	@NonNull
	public GlobalGpxSelectionListener getGpxSelectionListener() {
		return gpxSelectionListener;
	}

}
