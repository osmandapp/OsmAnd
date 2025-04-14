package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;

public class RequestThemeParams {

	private final ApplicationMode appMode;
	private final boolean ignoreExternalProvider;

	public RequestThemeParams() {
		this(null, false);
	}

	public RequestThemeParams(@Nullable ApplicationMode appMode) {
		this(appMode, false);
	}

	public RequestThemeParams(@Nullable ApplicationMode appMode, boolean ignoreExternalProvider) {
		this.appMode = appMode;
		this.ignoreExternalProvider = ignoreExternalProvider;
	}

	@NonNull
	public ApplicationMode requireAppMode(@NonNull OsmandApplication app) {
		return appMode != null ? appMode : app.getSettings().getApplicationMode();
	}

	public boolean shouldIgnoreExternalProvider() {
		return ignoreExternalProvider;
	}
}
