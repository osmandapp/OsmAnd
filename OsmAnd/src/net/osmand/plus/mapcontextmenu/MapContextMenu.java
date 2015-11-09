package net.osmand.plus.mapcontextmenu;

import android.support.v4.app.Fragment;
import android.view.View;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.MapUtils;

public class MapContextMenu extends MenuTitleController {

	private MapActivity mapActivity;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	private Object object;
	private MenuController menuController;

	private LatLon mapCenter;
	private int mapPosition = 0;

	private LatLon myLocation;
	private Float heading;
	private boolean inLocationUpdate = false;

	private int favActionIconId;

	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		if (active) {
			acquireMenuController();
		} else {
			menuController = null;
		}
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

	public MapContextMenu() {
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object) {
		return init(latLon, pointDescription, object, false);
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object, boolean update) {

		if (myLocation == null) {
			myLocation = getMapActivity().getMyApplication().getSettings().getLastKnownMapLocation();
		}

		if (!update && isVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		setSelectedObject(object);

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
			menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
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

	public void update(LatLon latLon, PointDescription pointDescription, Object object) {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			init(latLon, pointDescription, object, true);
			fragment.rebuildMenu();
		}
	}

	public void showOrUpdate(LatLon latLon, PointDescription pointDescription, Object object) {
		if (isVisible() && this.object != null && this.object.equals(object)) {
			update(latLon, pointDescription, object);
		} else {
			show(latLon, pointDescription, object);
		}
	}

	public void close() {
		active = false;
		if (this.object != null) {
			clearSelectedObject(this.object);
		}
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
							((ContextMenuLayer.IContextMenuProviderSelection) l).clearSelectedObject();
						}
					}
				}
			}
		}
	}

	private void setSelectedObject(Object object) {
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
		menuController = MenuController.getMenuController(mapActivity, pointDescription, object, MenuType.STANDARD);
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
		if (menuController != null) {
			menuController.share(latLon, nameStr);
		} else {
			ShareMenu.show(latLon, nameStr, mapActivity);
		}
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

	public TitleButtonController getLeftTitleButtonController() {
		if (menuController != null) {
			return menuController.getLeftTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getRightTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getTopRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getTopRightTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleProgressController getTitleProgressController() {
		if (menuController != null) {
			return menuController.getTitleProgressController();
		} else {
			return null;
		}
	}

	public boolean fabVisible() {
		return menuController == null || menuController.fabVisible();
	}

	public boolean buttonsVisible() {
		return menuController == null || menuController.buttonsVisible();
	}

	public boolean displayDistanceDirection() {
		return menuController != null && menuController.displayDistanceDirection();
	}

	public void updateData() {
		if (menuController != null) {
			menuController.updateData();
		}
	}

	public LatLon getMyLocation() {
		return myLocation;
	}

	public Float getHeading() {
		return heading;
	}

	public void updateMyLocation(net.osmand.Location location) {
		if (location != null) {
			myLocation = new LatLon(location.getLatitude(), location.getLongitude());
			updateLocation(false, true, false);
		}
	}

	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocation(false, false, true);
		} else {
			heading = lastHeading;
		}
	}

	public void updateLocation(final boolean centerChanged, final boolean locationChanged,
							   final boolean compassChanged) {
		if (inLocationUpdate) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				inLocationUpdate = false;
				MapContextMenuFragment menuFragment = findMenuFragment();
				if (menuFragment != null) {
					menuFragment.updateLocation(centerChanged, locationChanged, compassChanged);
				}
			}
		});
	}

}