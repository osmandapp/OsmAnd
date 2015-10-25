package net.osmand.plus.mapcontextmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;

public class MapContextMenu extends MenuTitleController {

	private final MapActivity mapActivity;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	private Object object;
	private MenuController menuController;

	private LatLon mapCenter;
	private int mapPosition = 0;

	private int favActionIconId;

	private static final String KEY_CTX_MENU_OBJECT = "key_ctx_menu_object";
	private static final String KEY_CTX_MENU_ACTIVE = "key_ctx_menu_active";
	private static final String KEY_CTX_MENU_LATLON = "key_ctx_menu_latlon";
	private static final String KEY_CTX_MENU_POINT_DESC = "key_ctx_menu_point_desc";
	private static final String KEY_CTX_MENU_NAME_STR = "key_ctx_menu_name_str";
	private static final String KEY_CTX_MENU_TYPE_STR = "key_ctx_menu_type_str";
	private static final String KEY_CTX_MENU_STREET_STR = "key_ctx_menu_street_str";
	private static final String KEY_CTX_MENU_ADDR_UNKNOWN = "key_ctx_menu_addr_unknown";
	private static final String KEY_CTX_MENU_MAP_CENTER = "key_ctx_menu_map_center";
	private static final String KEY_CTX_MENU_MAP_POSITION = "key_ctx_menu_map_position";

	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	@Override
	public LatLon getLatLon() {
		return latLon;
	}

	public LatLon getMapCenter() {
		return mapCenter;
	}

	public void setMapCenter(LatLon mapCenter) {
		this.mapCenter = mapCenter;
	}

	public void setMapPosition(int mapPosition) {
		this.mapPosition = mapPosition;
	}

	@Override
	public PointDescription getPointDescription() {
		return pointDescription;
	}

	@Override
	public Object getObject() {
		return object;
	}

	public boolean isExtended() {
		return menuController != null;
	}

	@Override
	public MenuController getMenuController() {
		return menuController;
	}

	public MapContextMenu(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
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

		active = true;

		acquireMenuController();
		initTitle();

		if (menuController != null) {
			menuController.addPlainMenuItems(typeStr, this.pointDescription);
		}

		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(0);
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
		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(mapPosition);
			mapPosition = 0;
		}
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
		menuController = MenuController.getMenuController(mapActivity, latLon, pointDescription, object);
	}

	public void onSingleTapOnMap() {
		if (menuController == null || !menuController.handleSingleTapOnMap()) {
			hide();
		}
	}

	@Override
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

	public int getFavActionIconId() {
		return favActionIconId;
	}

	protected void acquireIcons() {
		super.acquireIcons();

		if (menuController != null) {
			favActionIconId = menuController.getFavActionIconId();
		} else {
			favActionIconId = R.drawable.ic_action_fav_dark;
		}
	}

	public void fabPressed() {
		mapActivity.getMapActions().directionTo(latLon.getLatitude(), latLon.getLongitude());
		hide();
		mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoControlDialog();
	}

	public void buttonWaypointPressed() {
		if (pointDescription.isDestination()) {
			mapActivity.getMapActions().editWaypoints();
		} else {
			mapActivity.getMapActions().addAsWaypoint(latLon.getLatitude(), latLon.getLongitude());
		}
		close();
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getFavoritePointEditor().edit((FavouritePoint) object);
		} else {
			mapActivity.getFavoritePointEditor().add(latLon, getTitleStr());
		}
	}

	public void buttonSharePressed() {
		ShareMenu.show(latLon, nameStr, mapActivity);
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
		bundle.putString(KEY_CTX_MENU_ADDR_UNKNOWN, Boolean.toString(addressUnknown));
		bundle.putSerializable(KEY_CTX_MENU_MAP_CENTER, mapCenter);
		bundle.putInt(KEY_CTX_MENU_MAP_POSITION, mapPosition);
	}

	public void restoreMenuState(Bundle bundle) {
		object = bundle.getSerializable(KEY_CTX_MENU_OBJECT);
		Object pDescObj = bundle.getSerializable(KEY_CTX_MENU_POINT_DESC);
		if (pDescObj != null) {
			pointDescription = (PointDescription) pDescObj;
		}

		active = Boolean.parseBoolean(bundle.getString(KEY_CTX_MENU_ACTIVE));
		Object latLonObj = bundle.getSerializable(KEY_CTX_MENU_LATLON);
		if (latLonObj != null) {
			latLon = (LatLon) latLonObj;
		} else {
			active = false;
		}

		acquireMenuController();

		Object mapCenterObj = bundle.getSerializable(KEY_CTX_MENU_MAP_CENTER);
		if (mapCenterObj != null) {
			mapCenter = (LatLon) mapCenterObj;
		}

		nameStr = bundle.getString(KEY_CTX_MENU_NAME_STR);
		typeStr = bundle.getString(KEY_CTX_MENU_TYPE_STR);
		streetStr = bundle.getString(KEY_CTX_MENU_STREET_STR);
		addressUnknown = Boolean.parseBoolean(bundle.getString(KEY_CTX_MENU_ADDR_UNKNOWN));
		mapPosition = bundle.getInt(KEY_CTX_MENU_MAP_POSITION, 0);

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

	public boolean hasTitleButton() {
		return menuController != null && menuController.hasTitleButton();
	}

	public String getTitleButtonCaption() {
		if (menuController != null) {
			return menuController.getTitleButtonCaption();
		} else {
			return "";
		}
	}

	public void titleButtonPressed() {
		if (menuController != null) {
			menuController.titleButtonPressed();
		}
	}
}