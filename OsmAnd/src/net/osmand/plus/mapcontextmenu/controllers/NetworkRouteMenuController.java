package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.router.network.NetworkRouteContext.*;

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
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;

public class NetworkRouteMenuController extends MenuController {

	private final OsmandApplication app;
	private Pair<NetworkRouteSegment, QuadRect> pair;

	public NetworkRouteMenuController(@NonNull MapActivity mapActivity,
	                                  @NonNull PointDescription pointDescription,
	                                  @NonNull Pair<?, ?> object) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		app = mapActivity.getMyApplication();
		pair = (Pair<NetworkRouteSegment, QuadRect>) object;

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
			if (pair.first instanceof NetworkRouteSegment && pair.second instanceof QuadRect) {
				this.pair = (Pair<NetworkRouteSegment, QuadRect>) pair;
			}
		}
	}

	@Override
	public Drawable getRightIcon() {
		return getIconForRouteObject(app, pair.first);
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

	public static Drawable getIconForRouteObject(@NonNull OsmandApplication app, @NonNull NetworkRouteSegment routeSegment) {

		RouteKey routeKey = routeSegment.routeKey;

		Pair<String, Integer> foreground = getForegroundIconIdWithName(app, routeKey);
		Pair<String, Integer> background = getBackgroundIconIdWithName(app, routeKey);

		UiUtilities uiUtilities = app.getUIUtilities();
		Drawable foregroundIcon = foreground != null ? uiUtilities.getIcon(foreground.second) : null;
		Drawable backgroundIcon = background != null ? uiUtilities.getIcon(background.second) : null;

		if (foregroundIcon != null && backgroundIcon != null) {
			return UiUtilities.getLayeredIcon(backgroundIcon, foregroundIcon);
		}
		return foregroundIcon != null ? foregroundIcon : backgroundIcon;
	}

	public static Pair<String, Integer> getForegroundIconIdWithName(@NonNull OsmandApplication app, @NonNull RouteKey routeKey) {
		return getIconIdWithName(app, routeKey, "osmc_foreground", "mm_osmc_", "");
	}

	public static Pair<String, Integer> getBackgroundIconIdWithName(@NonNull OsmandApplication app, @NonNull RouteKey routeKey) {
		return getIconIdWithName(app, routeKey, "osmc_background", "h_osmc_", "_bg");
	}

	private static Pair<String, Integer> getIconIdWithName(@NonNull OsmandApplication app, @NonNull RouteKey routeKey,
	                                                       @NonNull String key, @NonNull String prefix, @NonNull String suffix) {
		String name = routeKey.getValue(key);
		String iconName = prefix + name + suffix;
		int iconRes = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
		if (iconRes != 0) {
			return new Pair<>(name, iconRes);
		}
		return null;
	}
}