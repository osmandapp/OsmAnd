package net.osmand.test.common;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.OsmandApplication;

import androidx.annotation.NonNull;

public class AppInitIdlingResource extends BaseIdlingResource implements AppInitializeListener {

	public AppInitIdlingResource(@NonNull OsmandApplication app) {
		super(app);
		app.getAppInitializer().addListener(this);
	}

	@Override
	public boolean isIdleNow() {
		return !app.isApplicationInitializing();
	}

	@Override
	public void onFinish(@NonNull AppInitializer init) {
		app.getAppInitializer().removeListener(this);
		notifyIdleTransition();
	}
}
