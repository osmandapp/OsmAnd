package net.osmand.plus.mapcontextmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.sections.AmenityInfoMenuController;
import net.osmand.plus.mapcontextmenu.sections.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

public class MapContextMenu {

	private OsmandApplication app;
	private OsmandSettings settings;

	private PointDescription pointDescription;
	private Object object;

	private String foundStreetName;

	private static final String KEY_CTX_MENU_OBJECT = "key_ctx_menu_object";
	private static final String KEY_CTX_MENU_POINT_DESC = "key_ctx_menu_point_desc";

	public boolean isMenuVisible(MapActivity mapActivity) {
		return mapActivity.getSupportFragmentManager().findFragmentByTag("MapContextMenuFragment") != null;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public MapContextMenu(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	public void show(MapActivity mapActivity, PointDescription pointDescription, Object object) {

		if (isMenuVisible(mapActivity))
			hide(mapActivity);

		this.pointDescription = pointDescription;
		this.object = object;

		acquireStretName();

		MapContextMenuFragment.showInstance(mapActivity);

	}

	public void hide(MapActivity mapActivity) {

		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag("MapContextMenuFragment");
		if (fragment != null)
			((MapContextMenuFragment)fragment).dismissMenu();
	}

	private void acquireStretName() {
		RouteDataObject rt = app.getLocationProvider().getLastKnownRouteSegment();
		if(rt != null) {
			foundStreetName = RoutingHelper.formatStreetName(rt.getName(settings.MAP_PREFERRED_LOCALE.get()),
					rt.getRef(), rt.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));
		} else {
			foundStreetName = null;
		}
	}

	public int getLeftIconId() {
		if (object != null) {
			if (object instanceof Amenity) {
				String id = null;
				Amenity o = (Amenity) object;
				PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
				if (st != null) {
					if (RenderingIcons.containsSmallIcon(st.getIconKeyName())) {
						id = st.getIconKeyName();
					} else if (RenderingIcons.containsSmallIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
						id = st.getOsmTag() + "_" + st.getOsmValue();
					}
				}
				if (id != null) {
					Integer resId = RenderingIcons.getResId(id);
					if (resId != null) {
						return resId;
					}
				}
			}
		}
		return 0;
	}

	public String getAddressStr() {
		String res = null;

		if (object != null) {
			if (object instanceof Amenity) {
				Amenity amenity = (Amenity) object;
				res = OsmAndFormatter.getPoiStringWithoutType(amenity, settings.MAP_PREFERRED_LOCALE.get());
			}
		}

		if (Algorithms.isEmpty(res)) {
			String typeName = pointDescription.getTypeName();
			String name = pointDescription.getName();

			if (!Algorithms.isEmpty(name))
				res = name;
			else if (!Algorithms.isEmpty(typeName))
				res = typeName;
		}

		return Algorithms.isEmpty(res) ? "Address is unknown yet" : res; // todo: text constant
	}

	public String getLocationStr(MapActivity mapActivity) {
		if (foundStreetName == null)
			return pointDescription.getLocationName(mapActivity, true).replaceAll("\n", "");
		else
			return foundStreetName;
	}

	public MenuController getMenuController() {

		if (object != null) {
			if (object instanceof Amenity) {
				return new AmenityInfoMenuController(app, (Amenity)object);
			}
		}

		return null;
	}

	public void buttonNavigatePressed(MapActivity mapActivity) {
		mapActivity.getMapActions().showNavigationContextMenuPoint(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonFavoritePressed(MapActivity mapActivity) {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getMapActions().editFavoritePoint((FavouritePoint)object);
		} else {
			mapActivity.getMapActions().addFavouritePoint(pointDescription.getLat(), pointDescription.getLon());
		}
	}

	public void buttonSharePressed(MapActivity mapActivity) {
		mapActivity.getMapActions().shareLocation(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonMorePressed(MapActivity mapActivity) {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(mapActivity);
		if (object != null) {
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(object, menuAdapter);
			}
		}

		mapActivity.getMapActions().contextMenuPoint(pointDescription.getLat(), pointDescription.getLon(), menuAdapter, object);
	}

	public void saveMenuState(Bundle bundle) {
		if (object != null) {
			if (object instanceof Amenity)
				bundle.putSerializable(KEY_CTX_MENU_OBJECT, (Amenity)object);
		}
		bundle.putSerializable(KEY_CTX_MENU_POINT_DESC, pointDescription);
	}

	public void restoreMenuState(Bundle bundle) {
		object = bundle.getSerializable(KEY_CTX_MENU_OBJECT);
		Object pDescObj = bundle.getSerializable(KEY_CTX_MENU_POINT_DESC);
		if (pDescObj != null)
			pointDescription = (PointDescription)pDescObj;
	}
}
