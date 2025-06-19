package net.osmand.test.common;

import androidx.annotation.NonNull;
import androidx.test.espresso.IdlingResource;

import net.osmand.plus.OsmandApplication;

import java.util.concurrent.atomic.AtomicBoolean;

public class PoiTypesInitIdlingResource implements IdlingResource {

	private final String resourceName;
	private final OsmandApplication appInstance;
	private volatile ResourceCallback resourceCallback;
	private final AtomicBoolean isIdle = new AtomicBoolean(false);

	public PoiTypesInitIdlingResource(@NonNull String resourceName, @NonNull OsmandApplication appInstance) {
		this.resourceName = resourceName;
		this.appInstance = appInstance;
	}

	@Override
	public String getName() {
		return resourceName;
	}

	@Override
	public boolean isIdleNow() {
		boolean currentIdleState = appInstance.getPoiTypes().isInit();
		if (isIdle.get() != currentIdleState) {
			isIdle.set(currentIdleState);
			if (currentIdleState && resourceCallback != null) {
				resourceCallback.onTransitionToIdle();
			}
		}
		return currentIdleState;
	}

	@Override
	public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
		this.resourceCallback = resourceCallback;
	}
}