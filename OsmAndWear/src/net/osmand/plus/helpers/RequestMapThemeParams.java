package net.osmand.plus.helpers;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ApplicationMode;

public class RequestMapThemeParams {

	private ApplicationMode appMode;
	private boolean ignoreExternalProvider = false;

	@NonNull
	public RequestMapThemeParams setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
		return this;
	}

	@NonNull
	public RequestMapThemeParams markIgnoreExternalProvider() {
		this.ignoreExternalProvider = true;
		return this;
	}

	@NonNull
	public ApplicationMode requireAppMode(@NonNull ApplicationMode defaultValue) {
		return appMode != null ? appMode : defaultValue;
	}

	public boolean shouldIgnoreExternalProvider() {
		return ignoreExternalProvider;
	}
}
