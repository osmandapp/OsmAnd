package net.osmand.plus.base;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;

public interface AppModeDependentComponent {

	String APP_MODE_KEY = "app_mode_key";

	@NonNull
	ApplicationMode getAppMode();

	void setAppMode(@NonNull ApplicationMode appMode);

	@NonNull
	default ApplicationMode restoreAppMode(@NonNull OsmandApplication app, @Nullable ApplicationMode appMode,
	                                       @Nullable Bundle savedState, @Nullable Bundle arguments) {
		if (savedState != null) {
			appMode = readAppModeFromBundle(savedState);
		}
		if (appMode == null && arguments != null) {
			appMode = readAppModeFromBundle(arguments);
		}
		if (appMode == null) {
			appMode = app.getSettings().getApplicationMode();
		}
		return appMode;
	}

	default void saveAppModeToBundle(@Nullable ApplicationMode appMode, @NonNull Bundle bundle) {
		if (appMode != null) {
			bundle.putString(APP_MODE_KEY, appMode.getStringKey());
		}
	}

	@Nullable
	default ApplicationMode readAppModeFromBundle(@NonNull Bundle bundle) {
		return ApplicationMode.valueOfStringKey(bundle.getString(APP_MODE_KEY), null);
	}
}
