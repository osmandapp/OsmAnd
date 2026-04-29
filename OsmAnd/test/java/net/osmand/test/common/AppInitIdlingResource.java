package net.osmand.test.common;

import androidx.annotation.NonNull;

import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;

public class AppInitIdlingResource extends BaseIdlingResource implements AppInitializeListener {

	public AppInitIdlingResource(@NonNull OsmandApplication app) {
		super(app);
		app.getAppInitializer().addListener(this);
	}

	@Override
	public boolean isIdleNow() {
		boolean isIdle = !app.isApplicationInitializing();
		if (isIdle) {
			notifyIdleTransition();
		}
		return isIdle;
	}

	@Override
	public void onFinish(@NonNull AppInitializer init) {
		app.getAppInitializer().removeListener(this);
		unregisterListener();
		notifyIdleTransition();
	}

	public void unregisterListener() {
		app.getAppInitializer().removeListener(this);
	}
}