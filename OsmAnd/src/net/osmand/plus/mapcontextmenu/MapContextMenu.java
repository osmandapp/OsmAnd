package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

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
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

public class MapContextMenu {

	private OsmandApplication app;
	private OsmandSettings settings;
	private final MapActivity mapActivity;

	private boolean active;
	private LatLon latLon;
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
	private static final String KEY_CTX_MENU_ACTIVE = "key_ctx_menu_active";
	private static final String KEY_CTX_MENU_LATLON = "key_ctx_menu_latlon";
	private static final String KEY_CTX_MENU_POINT_DESC = "key_ctx_menu_point_desc";
	private static final String KEY_CTX_MENU_NAME_STR = "key_ctx_menu_name_str";
	private static final String KEY_CTX_MENU_TYPE_STR = "key_ctx_menu_type_str";
	private static final String KEY_CTX_MENU_STREET_STR = "key_ctx_menu_street_str";

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public boolean isExtended() {
		return menuController != null;
	}

	public MapContextMenu(OsmandApplication app, MapActivity mapActivity) {
		this.app = app;
		this.mapActivity = mapActivity;
		settings = app.getSettings();
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object) {
		return init(latLon, pointDescription, object, false);
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object, boolean reload) {
		if (!reload && isVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		if (this.object != null) {
			clearSelectedObject(this.object);
		}

		if (pointDescription == null) {
			this.pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		} else {
			this.pointDescription = pointDescription;
		}

		this.latLon = latLon;
		this.object = object;
		leftIconId = 0;
		nameStr = "";
		typeStr = "";
		streetStr = "";

		active = true;

		acquireMenuController();
		acquireIcons();
		acquireNameAndType();
		if (needStreetName()) {
			acquireStreetName(latLon);
		}
		if (menuController != null) {
			menuController.addPlainMenuItems(typeStr, this.pointDescription);
		}

		mapActivity.getMapView().refreshMap();

		return true;
	}

	public void show() {
		if (!isVisible()) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void show(LatLon latLon, PointDescription pointDescription, Object object) {
		if (init(latLon, pointDescription, object)) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void refreshMenu(LatLon latLon, PointDescription pointDescription, Object object) {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			init(latLon, pointDescription, object, true);
			fragment.rebuildMenu();
		}
	}

	public void close() {
		active = false;
		hide();
		mapActivity.getMapView().refreshMap();
	}

	public void hide() {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			fragment.dismissMenu();
		}
	}

	private void clearSelectedObject(Object object) {
		if (object != null) {
			for (OsmandMapLayer l : mapActivity.getMapView().getLayers()) {
				if (l instanceof ContextMenuLayer.IContextMenuProvider) {
					PointDescription pointDescription = ((ContextMenuLayer.IContextMenuProvider) l).getObjectName(object);
					if (pointDescription != null) {
						if (l instanceof ContextMenuLayer.IContextMenuProviderSelection) {
							((ContextMenuLayer.IContextMenuProviderSelection) l).setSelectedObject(object);
						}
					}
				}
			}
		}
	}

	private void acquireMenuController() {
		menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(app, mapActivity, (Amenity) object);
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(app, mapActivity, (FavouritePoint) object);
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

	public MapContextMenuFragment findMenuFragment() {
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
				return PointDescription.getLocationName(mapActivity,
						latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", "");
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
		mapActivity.getMapActions().showNavigationContextMenuPoint(latLon.getLatitude(), latLon.getLongitude());
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getFavoritePointEditor().edit((FavouritePoint) object);
		} else {
			mapActivity.getFavoritePointEditor().add(latLon, getTitleStr());
		}
	}

	public void buttonSharePressed() {
		mapActivity.getMapActions().shareLocation(latLon.getLatitude(), latLon.getLongitude());
	}

	public void buttonMorePressed() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(mapActivity);
		if (object != null) {
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(object, menuAdapter);
			}
		}

		mapActivity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, object);
	}

	public void saveMenuState(Bundle bundle) {
		if (menuController != null) {
			menuController.saveEntityState(bundle, KEY_CTX_MENU_OBJECT);
		}
		bundle.putString(KEY_CTX_MENU_ACTIVE, Boolean.toString(active));
		bundle.putSerializable(KEY_CTX_MENU_LATLON, latLon);
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

		active = Boolean.parseBoolean(bundle.getString(KEY_CTX_MENU_ACTIVE));
		Object latLonObj = bundle.getSerializable(KEY_CTX_MENU_LATLON);
		if (latLonObj != null) {
			latLon = (LatLon) latLonObj;
		} else {
			active = false;
		}

		nameStr = bundle.getString(KEY_CTX_MENU_NAME_STR);
		typeStr = bundle.getString(KEY_CTX_MENU_TYPE_STR);
		streetStr = bundle.getString(KEY_CTX_MENU_STREET_STR);

		acquireIcons();

		if (menuController != null) {
			menuController.addPlainMenuItems(typeStr, this.pointDescription);
		}
	}

	public void setBaseFragmentVisibility(boolean visible) {
		MapContextMenuFragment menuFragment = findMenuFragment();
		if (menuFragment != null) {
			menuFragment.setFragmentVisibility(visible);
		}
	}

	public boolean isLandscapeLayout() {
		return menuController != null && menuController.isLandscapeLayout();
	}

	public float getLandscapeWidthDp() {
		if (menuController != null) {
			return menuController.getLandscapeWidthDp();
		} else {
			return 0f;
		}
	}

	public boolean slideUp() {
		return menuController != null && menuController.slideUp();
	}

	public boolean slideDown() {
		return menuController != null && menuController.slideDown();
	}

	public void build(View rootView) {
		if (menuController != null) {
			menuController.build(rootView);
		}
	}

	public int getCurrentMenuState() {
		if (menuController != null) {
			return menuController.getCurrentMenuState();
		} else {
			return MenuController.MenuState.HEADER_ONLY;
		}
	}

	public float getHalfScreenMaxHeightKoef() {
		if (menuController != null) {
			return menuController.getHalfScreenMaxHeightKoef();
		} else {
			return 0f;
		}
	}

	public int getSlideInAnimation() {
		if (menuController != null) {
			return menuController.getSlideInAnimation();
		} else {
			return 0;
		}
	}

	public int getSlideOutAnimation() {
		if (menuController != null) {
			return menuController.getSlideOutAnimation();
		} else {
			return 0;
		}
	}

}