package net.osmand.plus.mapcontextmenu;

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
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

public class MapContextMenu {

	private MapActivity mapActivity;
	private OsmandApplication app;
	private OsmandSettings settings;

	private PointDescription pointDescription;
	private Object object;

	private String foundStreetName;

	public boolean isMenuVisible() {
		return mapActivity.getSupportFragmentManager().findFragmentByTag("MapContextMenuFragment") != null;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public void setApp(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	private static MapContextMenu ourInstance = new MapContextMenu();

	public static MapContextMenu getInstance() {
		return ourInstance;
	}

	private MapContextMenu() {

	}

	public void show(PointDescription pointDescription, Object object) {

		if (isMenuVisible())
			hide();

		this.pointDescription = pointDescription;
		this.object = object;

		acquireStretName();

		MapContextMenuFragment.showInstance(mapActivity);

	}

	public void hide() {

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

		return Algorithms.isEmpty(res) ? "Address is unknown yet" : res;
	}

	public String getLocationStr() {
		if (foundStreetName == null)
			return pointDescription.getLocationName(mapActivity, true).replaceAll("\n", "");
		else
			return foundStreetName;
	}

	public void buttonNavigatePressed() {
		mapActivity.getMapActions().showNavigationContextMenuPoint(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getMapActions().editFavoritePoint((FavouritePoint)object);
		} else {
			mapActivity.getMapActions().addFavouritePoint(pointDescription.getLat(), pointDescription.getLon());
		}
	}

	public void buttonSharePressed() {
		mapActivity.getMapActions().shareLocation(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonMorePressed() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(mapActivity);
		if (object != null) {
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(object, menuAdapter);
			}
		}

		mapActivity.getMapActions().contextMenuPoint(pointDescription.getLat(), pointDescription.getLon(), menuAdapter, object);
	}
}
