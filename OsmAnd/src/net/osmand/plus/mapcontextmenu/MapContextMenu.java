package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
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

	private int leftIconId;
	private String nameStr;
	private String typeStr;
	private String streetStr;

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
		leftIconId = 0;
		nameStr = null;
		typeStr = null;
		streetStr = null;

		acquireLeftIconId();
		acquireNameAndType();
		acquireStreetName(mapActivity, new LatLon(pointDescription.getLat(), pointDescription.getLon()));

		MapContextMenuFragment.showInstance(mapActivity);

	}

	public void hide(MapActivity mapActivity) {
		MapContextMenuFragment fragment = findMenuFragment(mapActivity);
		if (fragment != null)
			fragment.dismissMenu();
	}

	public void refreshMenuTitle(MapActivity mapActivity) {
		MapContextMenuFragment fragment = findMenuFragment(mapActivity);
		if (fragment != null)
			fragment.refreshTitle();
	}

	private MapContextMenuFragment findMenuFragment(MapActivity mapActivity) {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag("MapContextMenuFragment");
		if (fragment != null)
			return (MapContextMenuFragment)fragment;
		else
			return null;
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public String getTitleStr() {
		return nameStr;
	}

	public String getLocationStr(MapActivity mapActivity) {
		if (Algorithms.isEmpty(streetStr))
			return pointDescription.getLocationName(mapActivity, true).replaceAll("\n", "");
		else
			return streetStr;
	}

	private void acquireLeftIconId() {
		leftIconId = 0;
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
						leftIconId = resId;
					}
				}
			}
		}
	}

	private void acquireNameAndType() {
		if (object != null) {
			if (object instanceof Amenity) {
				Amenity amenity = (Amenity) object;

				PoiCategory pc = amenity.getType();
				PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
				typeStr = amenity.getSubType();
				if (pt != null) {
					typeStr = pt.getTranslation();
				} else if(typeStr != null){
					typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
				}
				nameStr = amenity.getName(settings.MAP_PREFERRED_LOCALE.get());
			}
		}

		if (Algorithms.isEmpty(nameStr)) {
			nameStr = pointDescription.getName();
		}
		if (Algorithms.isEmpty(typeStr)) {
			typeStr = pointDescription.getTypeName();
		}

		if (Algorithms.isEmpty(nameStr)) {
			if (!Algorithms.isEmpty(typeStr)) {
				nameStr = typeStr;
				typeStr = null;
			} else {
				nameStr = app.getResources().getString(R.string.address_unknown);
			}
		}
	}

	private void acquireStreetName(final MapActivity activity, final LatLon loc) {
		Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		app.getLocationProvider().getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object != null) {
					streetStr = RoutingHelper.formatStreetName(object.getName(settings.MAP_PREFERRED_LOCALE.get()),
							object.getRef(), object.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));
					if (streetStr != null && streetStr.trim().length() == 0) {
						streetStr = null;
					}

					if (streetStr != null) {
						activity.runOnUiThread(new Runnable() {
							public void run() {
								refreshMenuTitle(activity);
							}
						});
					}
				} else {
					streetStr = null;
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
	}

	public MenuController getMenuController(Activity activity) {

		if (object != null) {
			if (object instanceof Amenity) {
				MenuController menuController = new AmenityInfoMenuController(app, activity, (Amenity)object);
				if (!Algorithms.isEmpty(typeStr)) {
					menuController.addPlainMenuItem(R.drawable.ic_action_info_dark, typeStr);
				}
				menuController.addPlainMenuItem(R.drawable.map_my_location, pointDescription.getLocationName(activity, true).replaceAll("\n", ""));
				return menuController;
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
