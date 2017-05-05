package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;

public abstract class AbstractCard {

	private OsmandApplication app;
	protected View view;

	public abstract int getCardLayoutId();

	public AbstractCard(OsmandApplication app) {
		this.app = app;
	}

	public View build(Context ctx) {
		view = LayoutInflater.from(ctx).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public abstract void update();

	public OsmandApplication getOsmandApplication() {
		return app;
	}
}
