package net.osmand.plus.mapcontextmenu.other;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MenuObjectUtils {

	public static List<MenuObject> createMenuObjectsList(@NonNull MapActivity mapActivity,
	                                                     @NonNull List<RenderedObject> objects,
	                                                     @NonNull LatLon latLon) {
		List<MenuObject> result = new ArrayList<>();
		OsmandApplication app = mapActivity.getMyApplication();
		IContextMenuProvider contextObject = app.getOsmandMap().getMapLayers().getPoiMapLayer();
		for (RenderedObject object : objects) {
			result.add(createMenuObject(object, contextObject, latLon, mapActivity));
		}
		return result;
	}

	public static MenuObject createMenuObject(@NonNull Object selectedObj,
	                                          @Nullable IContextMenuProvider contextObject,
	                                          @NonNull LatLon latLon, @Nullable MapActivity mapActivity) {
		LatLon ll = null;
		PointDescription pointDescription = null;
		if (contextObject != null) {
			ll = contextObject.getObjectLocation(selectedObj);
			pointDescription = contextObject.getObjectName(selectedObj);
		}
		if (ll == null) {
			ll = latLon;
		}
		if (pointDescription == null) {
			pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		}
		return new MenuObject(ll, pointDescription, selectedObj, mapActivity);
	}

	@NonNull
	public static String getSecondLineText(MenuObject item) {
		StringBuilder line2Str = new StringBuilder(item.getTypeStr());
		if (item.getObject() instanceof Pair<?, ?> pair) {
			if (pair.first instanceof RouteKey key) {
				OsmandApplication app = item.getMyApplication();
				if (app != null) {
					String routeType = AndroidUtils.getActivityTypeTitle(app, key.type);
					line2Str.append(" - ").append(routeType);
				}
			}
		}
		String streetStr = item.getStreetStr();
		if (!Algorithms.isEmpty(streetStr) && !item.displayStreetNameInTitle()) {
			if (line2Str.length() > 0) {
				line2Str.append(", ");
			}
			line2Str.append(streetStr);
		}
		return line2Str.toString();
	}

	@NonNull
	public static String getMenuObjectsNamesByComma(@NonNull List<MenuObject> menuObjects) {
		List<String> names = new ArrayList<>();
		for (MenuObject menuObject : menuObjects) {
			names.add(menuObject.getTitleStr());
		}
		return TextUtils.join(", ", names);
	}
}
