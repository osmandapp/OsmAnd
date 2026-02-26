package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.router.network.NetworkRouteSelector.*;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class NetworkRouteMenuController extends MenuController {

	private final OsmandApplication app;
	private Pair<RouteKey, QuadRect> pair;

	public NetworkRouteMenuController(@NonNull MapActivity mapActivity,
	                                  @NonNull PointDescription pointDescription,
	                                  @NonNull Pair<?, ?> object) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		app = mapActivity.getMyApplication();
		pair = (Pair<RouteKey, QuadRect>) object;

		builder.setShowNearestWiki(true);
	}

	@Override
	protected Object getObject() {
		return pair;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) object;
			if (pair.first instanceof RouteKey && pair.second instanceof QuadRect) {
				this.pair = (Pair<RouteKey, QuadRect>) pair;
			}
		}
	}

	@Override
	public Drawable getRightIcon() {
		return new NetworkRouteDrawable(app, pair.first, !isLight());
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.layer_route);
		} else {
			return "";
		}
	}
}