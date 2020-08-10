package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;

public abstract class WidgetState {

	private OsmandApplication ctx;

	public OsmandApplication getCtx() {
		return ctx;
	}

	public WidgetState(OsmandApplication ctx) {
		this.ctx = ctx;
	}

	public abstract int getMenuTitleId();

	public abstract int getMenuIconId();

	public abstract int getMenuItemId();

	public abstract int[] getMenuTitleIds();

	public abstract int[] getMenuIconIds();

	public abstract int[] getMenuItemIds();

	public abstract void changeState(int stateId);
}