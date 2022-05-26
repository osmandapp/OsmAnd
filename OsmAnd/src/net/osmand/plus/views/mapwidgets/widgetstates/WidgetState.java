package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;

import androidx.annotation.NonNull;

public abstract class WidgetState {

	protected final OsmandApplication app;

	public WidgetState(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public abstract String getTitle();

	public abstract int getSettingsIconId(boolean nightMode);

	public abstract void changeState(int stateId);
}