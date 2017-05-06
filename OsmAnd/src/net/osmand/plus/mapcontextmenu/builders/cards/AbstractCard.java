package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public abstract class AbstractCard {

	private MapActivity mapActivity;
	private OsmandApplication app;
	protected View view;

	public abstract int getCardLayoutId();

	public AbstractCard(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
	}

	public View build(Context ctx) {
		view = LayoutInflater.from(ctx).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public abstract void update();

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public OsmandApplication getMyApplication() {
		return app;
	}
}
