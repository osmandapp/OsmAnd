package net.osmand.test.common;

import net.osmand.plus.OsmandApplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.IdlingResource;

public abstract class BaseIdlingResource implements IdlingResource {

	@NonNull
	protected final OsmandApplication app;

	@Nullable
	private ResourceCallback callback;

	public BaseIdlingResource(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void registerIdleTransitionCallback(ResourceCallback callback) {
		this.callback = callback;
	}

	protected void notifyIdleTransition() {
		if (callback != null) {
			callback.onTransitionToIdle();
		}
	}
}
