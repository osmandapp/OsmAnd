package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;

public abstract class WidgetState {

	private final OsmandApplication app;

	public WidgetState(OsmandApplication app) {
		this.app = app;
	}

	public OsmandApplication getApp() {
		return app;
	}

	public abstract int getMenuTitleId();

	public abstract int getSettingsIconId();

	public abstract int getMenuItemId();

	public abstract int[] getMenuTitleIds();

	public abstract int[] getMenuIconIds();

	public abstract int[] getMenuItemIds();

	public abstract void changeState(int stateId);
}