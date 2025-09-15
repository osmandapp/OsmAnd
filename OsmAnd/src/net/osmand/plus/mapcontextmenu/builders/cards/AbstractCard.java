package net.osmand.plus.mapcontextmenu.builders.cards;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public abstract class AbstractCard {

	protected final OsmandApplication app;
	protected final MapActivity mapActivity;

	public abstract int getCardLayoutId();

	public AbstractCard(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
	}
}
