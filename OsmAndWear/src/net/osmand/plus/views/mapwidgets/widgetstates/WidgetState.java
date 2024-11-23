package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class WidgetState {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;

	public WidgetState(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public abstract String getTitle();

	public abstract int getSettingsIconId(boolean nightMode);

	public abstract void changeToNextState();

	public abstract void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId);
	public abstract void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId);
}