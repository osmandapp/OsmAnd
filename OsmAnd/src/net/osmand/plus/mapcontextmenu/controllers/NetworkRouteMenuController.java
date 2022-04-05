package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Set;

public class NetworkRouteMenuController extends MenuController {

	private final OsmandApplication app;
	private Pair<RenderedObject, QuadRect> pair;

	public NetworkRouteMenuController(@NonNull MapActivity mapActivity,
	                                  @NonNull PointDescription pointDescription,
	                                  @NonNull Pair<?, ?> object) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		app = mapActivity.getMyApplication();
		pair = (Pair<RenderedObject, QuadRect>) object;

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
			if (pair.first instanceof RenderedObject && pair.second instanceof QuadRect) {
				this.pair = (Pair<RenderedObject, QuadRect>) pair;
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

	public static Drawable getIconForRouteObject(@NonNull OsmandApplication app, @NonNull RenderedObject renderedObject) {
		List<RouteKey> routeKeys = RouteType.getRouteStringKeys(renderedObject);
		if (!Algorithms.isEmpty(routeKeys)) {
			RouteKey routeKey = routeKeys.get(0);

			String iconPrefix = "route_" + routeKey.type.getTag() + "__osmc_foreground__";
			String backgroundPrefix = "route_" + routeKey.type.getTag() + "__osmc_background__";

			int iconRes = getIconId(app, routeKey.set, iconPrefix, "mm_osmc_", "");
			int backgroundRes = getIconId(app, routeKey.set, backgroundPrefix, "h_osmc_", "_bg");

			Drawable icon = iconRes != 0 ? app.getUIUtilities().getIcon(iconRes) : null;
			Drawable background = backgroundRes != 0 ? app.getUIUtilities().getIcon(backgroundRes) : null;

			if (icon != null && background != null) {
				return UiUtilities.getLayeredIcon(background, icon);
			}
			return icon != null ? icon : background;
		}
		return null;
	}

	private static int getIconId(@NonNull OsmandApplication app, @NonNull Set<String> routeTags,
	                             @NonNull String key, @NonNull String prefix, @NonNull String suffix) {
		for (String tag : routeTags) {
			if (tag.startsWith(key)) {
				String iconName = prefix + tag.replace(key, "") + suffix;
				int iconRes = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
				if (iconRes != 0) {
					return iconRes;
				}
			}
		}
		return 0;
	}
}