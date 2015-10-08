package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.details.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.details.FavouritePointMenuController;
import net.osmand.plus.mapcontextmenu.details.MenuController;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

public class MapContextMenu {

	private OsmandApplication app;
	private OsmandSettings settings;
	private final MapActivity mapActivity;

	private PointDescription pointDescription;
	private Object object;
	MenuController menuController;

	private int leftIconId;
	private Drawable leftIcon;
	private String nameStr;
	private String typeStr;
	private Drawable secondLineIcon;
	private String streetStr;

	private static final String KEY_CTX_MENU_OBJECT = "key_ctx_menu_object";
	private static final String KEY_CTX_MENU_POINT_DESC = "key_ctx_menu_point_desc";
	private static final String KEY_CTX_MENU_NAME_STR = "key_ctx_menu_name_str";
	private static final String KEY_CTX_MENU_TYPE_STR = "key_ctx_menu_type_str";
	private static final String KEY_CTX_MENU_STREET_STR = "key_ctx_menu_street_str";

	public boolean isMenuVisible() {
		return findMenuFragment() != null;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public MenuController getMenuController() {
		return menuController;
	}

	public MapContextMenu(OsmandApplication app, MapActivity mapActivity) {
		this.app = app;
		this.mapActivity = mapActivity;
		settings = app.getSettings();
	}

	public boolean init(PointDescription pointDescription, Object object) {
		return init(pointDescription, object, false);
	}

	public boolean init(PointDescription pointDescription, Object object, boolean reload) {
		if (!reload && isMenuVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		this.pointDescription = pointDescription;
		this.object = object;
		leftIconId = 0;
		nameStr = "";
		typeStr = "";
		streetStr = "";

		acquireMenuController();
		acquireIcons();
		acquireNameAndType();
		if (needStreetName()) {
			acquireStreetName(new LatLon(pointDescription.getLat(), pointDescription.getLon()));
		}
		return true;
	}

	public void show() {
		if (!isMenuVisible()) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void show(PointDescription pointDescription, Object object) {
		if (init(pointDescription, object)) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void refreshMenu(PointDescription pointDescription, Object object) {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			init(pointDescription, object, true);
			fragment.rebuildMenu();
		}
	}

	public void hide() {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			fragment.dismissMenu();
		}
	}

	private void acquireMenuController() {
		menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(app, mapActivity, (Amenity)object);
				if (!Algorithms.isEmpty(typeStr)) {
					menuController.addPlainMenuItem(R.drawable.ic_action_info_dark, typeStr);
				}
				if (pointDescription != null) {
					menuController.addPlainMenuItem(R.drawable.map_my_location, pointDescription.getLocationName(mapActivity, true).replaceAll("\n", ""));
				}
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(app, mapActivity, (FavouritePoint)object);
				if (pointDescription != null) {
					menuController.addPlainMenuItem(R.drawable.map_my_location, pointDescription.getLocationName(mapActivity, true).replaceAll("\n", ""));
				}
			}
		}
	}

	public void onSingleTapOnMap() {
		if (menuController == null || !menuController.handleSingleTapOnMap()) {
			hide();
		}
	}

	private boolean needStreetName() {
		boolean res = object != null || Algorithms.isEmpty(pointDescription.getName());
		if (res && menuController != null) {
			res = menuController.needStreetName();
		}
		return res;
	}

	public void refreshMenuTitle() {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null)
			fragment.refreshTitle();
	}

	private MapContextMenuFragment findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG);
		if (fragment != null) {
			return (MapContextMenuFragment) fragment;
		} else {
			return null;
		}
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public Drawable getLeftIcon() {
		return leftIcon;
	}

	public Drawable getSecondLineIcon() {
		return secondLineIcon;
	}

	public String getTitleStr() {
		return nameStr;
	}

	public String getLocationStr() {
		if (menuController != null && menuController.needTypeStr()) {
			return typeStr;
		} else {
			if (Algorithms.isEmpty(streetStr)) {
				return pointDescription.getLocationName(mapActivity, true).replaceAll("\n", "");
			} else {
				return streetStr;
			}
		}
	}

	private void acquireIcons() {
		leftIconId = 0;
		leftIcon = null;
		secondLineIcon = null;

		if (menuController != null) {
			leftIconId = menuController.getLeftIconId();
			leftIcon = menuController.getLeftIcon();
			secondLineIcon = menuController.getSecondLineIcon();
		}
	}

	private void acquireNameAndType() {
		if (menuController != null) {
			nameStr = menuController.getNameStr();
			typeStr = menuController.getTypeStr();
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
				typeStr = "";
			} else {
				nameStr = app.getResources().getString(R.string.address_unknown);
			}
		}
	}

	private void acquireStreetName(final LatLon loc) {
		Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		app.getLocationProvider().getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object != null) {
					streetStr = RoutingHelper.formatStreetName(object.getName(settings.MAP_PREFERRED_LOCALE.get()),
							object.getRef(), object.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));

					if (!Algorithms.isEmpty(streetStr)) {
						if (getObject() == null) {
							nameStr = streetStr;
							streetStr = "";
						}
						mapActivity.runOnUiThread(new Runnable() {
							public void run() {
								refreshMenuTitle();
							}
						});
					}
				} else {
					streetStr = "";
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
	}

	public void buttonNavigatePressed() {
		mapActivity.getMapActions().showNavigationContextMenuPoint(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getFavoritePointEditor().edit((FavouritePoint)object);
		} else {
			mapActivity.getFavoritePointEditor().add(pointDescription);
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

	public void saveMenuState(Bundle bundle) {
		if (menuController != null) {
			menuController.saveEntityState(bundle, KEY_CTX_MENU_OBJECT);
		}
		bundle.putSerializable(KEY_CTX_MENU_POINT_DESC, pointDescription);
		bundle.putString(KEY_CTX_MENU_NAME_STR, nameStr);
		bundle.putString(KEY_CTX_MENU_TYPE_STR, typeStr);
		bundle.putString(KEY_CTX_MENU_STREET_STR, streetStr);
	}

	public void restoreMenuState(Bundle bundle) {
		object = bundle.getSerializable(KEY_CTX_MENU_OBJECT);
		Object pDescObj = bundle.getSerializable(KEY_CTX_MENU_POINT_DESC);
		if (pDescObj != null) {
			pointDescription = (PointDescription) pDescObj;
		}
		acquireMenuController();

		nameStr = bundle.getString(KEY_CTX_MENU_NAME_STR);
		typeStr = bundle.getString(KEY_CTX_MENU_TYPE_STR);
		streetStr = bundle.getString(KEY_CTX_MENU_STREET_STR);

		acquireIcons();
	}
}
