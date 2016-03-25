package net.osmand.plus.mapcontextmenu;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.CallbackWithObject;
import net.osmand.StateChangedListener;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.PointEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.List;

public class MapContextMenu extends MenuTitleController implements StateChangedListener<ApplicationMode>,
		MapMarkerChangedListener {

	private MapActivity mapActivity;
	private OsmandSettings settings;
	private MapMultiSelectionMenu mapMultiSelectionMenu;

	private FavoritePointEditor favoritePointEditor;
	private WptPtEditor wptPtEditor;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	private Object object;
	private MenuController menuController;

	private LatLon mapCenter;
	private int mapPosition = 0;
	private boolean centerMarker;
	private int mapZoom;

	private LatLon myLocation;
	private Float heading;
	private boolean inLocationUpdate = false;
	private boolean appModeChanged;
	private boolean appModeListenerAdded;

	private int favActionIconId;

	private MenuAction searchDoneAction;

	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		if (!appModeListenerAdded) {
			mapActivity.getMyApplication().getSettings().APPLICATION_MODE.addListener(this);
			appModeListenerAdded = true;
		}

		if (mapMultiSelectionMenu == null) {
			mapMultiSelectionMenu = new MapMultiSelectionMenu(mapActivity);
		} else {
			mapMultiSelectionMenu.setMapActivity(mapActivity);
		}

		if (favoritePointEditor != null) {
			favoritePointEditor.setMapActivity(mapActivity);
		}
		if (wptPtEditor != null) {
			wptPtEditor.setMapActivity(mapActivity);
		}

		if (active) {
			acquireMenuController();
			if (menuController != null) {
				menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
			}
		} else {
			menuController = null;
		}
	}

	public MapMultiSelectionMenu getMultiSelectionMenu() {
		return mapMultiSelectionMenu;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	public void hideMenues() {
		if (isVisible()) {
			hide();
		} else if (mapMultiSelectionMenu.isVisible()) {
			mapMultiSelectionMenu.hide();
		}
	}

	public FavoritePointEditor getFavoritePointEditor() {
		if (favoritePointEditor == null) {
			favoritePointEditor = new FavoritePointEditor(mapActivity);
		}
		return favoritePointEditor;
	}

	public WptPtEditor getWptPtPointEditor() {
		if (wptPtEditor == null) {
			wptPtEditor = new WptPtEditor(mapActivity);
		}
		return wptPtEditor;
	}

	public PointEditor getPointEditor(String tag) {
		if (favoritePointEditor != null && favoritePointEditor.getFragmentTag().equals(tag)) {
			return favoritePointEditor;
		} else if (wptPtEditor != null && wptPtEditor.getFragmentTag().equals(tag)) {
			return wptPtEditor;
		}
		return null;
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

	public void setCenterMarker(boolean centerMarker) {
		this.centerMarker = centerMarker;
	}

	public int getMapZoom() {
		return mapZoom;
	}

	public void setMapZoom(int mapZoom) {
		this.mapZoom = mapZoom;
	}

	public void updateMapCenter(LatLon mapCenter) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateMapCenter(mapCenter);
		}
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

		boolean needAcquireMenuController = menuController == null
				|| appModeChanged
				|| !update
				|| this.object == null && object != null
				|| this.object != null && object == null
				|| (this.object != null && object != null && !this.object.getClass().equals(object.getClass()));

		this.latLon = latLon;
		this.object = object;

		active = true;
		appModeChanged = false;

		if (needAcquireMenuController) {
			acquireMenuController();
		} else {
			menuController.update(pointDescription, object);
		}
		initTitle();

		if (menuController != null) {
			menuController.clearPlainMenuItems();
			menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
		}

		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(0);
		}

		mapActivity.refreshMap();

		if (object instanceof MapMarker) {
			mapActivity.getMyApplication().getMapMarkersHelper().addListener(this);
		}

		return true;
	}

	public void show() {
		if (!isVisible()) {
			boolean wasInit = true;
			if (appModeChanged) {
				wasInit = init(latLon, pointDescription, object);
			}
			if (wasInit && !MapContextMenuFragment.showInstance(this, mapActivity, true)) {
				active = false;
			}
		} else {
			WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
			if (fragmentRef != null) {
				fragmentRef.get().centerMarkerLocation();
			}
		}
	}

	public void show(LatLon latLon, PointDescription pointDescription, Object object) {
		if (init(latLon, pointDescription, object)) {
			if (!MapContextMenuFragment.showInstance(this, mapActivity, centerMarker)) {
				active = false;
			}
			centerMarker = false;
		}
	}

	public void update(LatLon latLon, PointDescription pointDescription, Object object) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		init(latLon, pointDescription, object, true);
		if (fragmentRef != null) {
			fragmentRef.get().rebuildMenu();
		}
	}

	public void showOrUpdate(LatLon latLon, PointDescription pointDescription, Object object) {
		if (isVisible() && this.object != null && this.object.equals(object)) {
			update(latLon, pointDescription, object);
		} else {
			show(latLon, pointDescription, object);
		}
	}

	public void showMinimized(LatLon latLon, PointDescription pointDescription, Object object) {
		init(latLon, pointDescription, object);
	}

	public void close() {
		if (active) {
			active = false;
			if (object instanceof MapMarker) {
				mapActivity.getMyApplication().getMapMarkersHelper().removeListener(this);
			}
			if (this.object != null) {
				clearSelectedObject(this.object);
			}
			hide();
			mapActivity.refreshMap();
		}
	}

	public void hide() {
		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(mapPosition);
			mapPosition = 0;
		}
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismissMenu();
		}
	}

	public void updateMenuUI() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateMenu();
		}
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		if (object == mapMarker) {
			String address = ((MapMarker) object).getOnlyName();
			nameStr = address;
			pointDescription.setName(address);
			WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
			if (fragmentRef != null)
				fragmentRef.get().refreshTitle();
		}
	}

	@Override
	public void onMapMarkersChanged() {
	}

	@Override
	public void stateChanged(ApplicationMode change) {
		appModeChanged = active;
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
	public void onSearchAddressDone() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().refreshTitle();

		if (searchDoneAction != null) {
			if (searchDoneAction.dlg != null) {
				searchDoneAction.dlg.dismiss();
				searchDoneAction.dlg = null;
			}
			searchDoneAction.run();
			searchDoneAction = null;
		}
	}

	public WeakReference<MapContextMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((MapContextMenuFragment) fragment);
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
			favActionIconId = R.drawable.map_action_fav_dark;
		}
	}

	public int getFabIconId() {
		RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
			return R.drawable.map_action_waypoint;
		} else {
			return R.drawable.map_directions;
		}
	}

	public void fabPressed() {
		hide();
		final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
		RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
			DirectionsDialogs.addWaypointDialogAndLaunchMap(mapActivity, latLon.getLatitude(),
					latLon.getLongitude(), getPointDescriptionForTarget());
		} else if (targets.getIntermediatePoints().isEmpty()) {
			targets.navigateToPoint(latLon, true, -1, getPointDescriptionForTarget());
			mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true);
			close();
		} else {
			Builder bld = new AlertDialog.Builder(mapActivity);
			bld.setTitle(R.string.new_directions_point_dialog);
			final int[] defaultVls = new int[]{0};
			bld.setSingleChoiceItems(new String[]{
					mapActivity.getString(R.string.clear_intermediate_points),
					mapActivity.getString(R.string.keep_intermediate_points)
			}, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					defaultVls[0] = which;
				}
			});
			bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (defaultVls[0] == 0) {
						targets.removeAllWayPoints(false, true);
						targets.navigateToPoint(latLon, true, -1, getPointDescriptionForTarget());
						mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true);
						close();
					} else {
						targets.navigateToPoint(latLon, true, -1, getPointDescriptionForTarget());
						mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true);
						close();
					}
				}
			});
			bld.setNegativeButton(R.string.shared_string_cancel, null);
			bld.show();
		}
	}

	public void buttonWaypointPressed() {
		if (pointDescription.isDestination()) {
			mapActivity.getMapActions().editWaypoints();
		} else if (settings.USE_MAP_MARKERS.get()) {
			if (pointDescription.isMapMarker()) {
				hide();
				MapActivity.clearPrevActivityIntent();
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
			} else {
				mapActivity.getMapActions().addMapMarker(latLon.getLatitude(), latLon.getLongitude(),
						getPointDescriptionForTarget());
			}
		} else {
			mapActivity.getMapActions().addAsTarget(latLon.getLatitude(), latLon.getLongitude(),
					getPointDescriptionForTarget());
		}
		close();
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			getFavoritePointEditor().edit((FavouritePoint) object);
		} else {
			callMenuAction(true, new MenuAction() {
				@Override
				public void run() {
					String title = getTitleStr();
					if (pointDescription.isFavorite() || !hasValidTitle()) {
						title = "";
					}
					getFavoritePointEditor().add(latLon, title);
				}
			});
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
		for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
			layer.populateObjectContextMenu(latLon, object, menuAdapter, mapActivity);
		}

		mapActivity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, object);
	}

	private void callMenuAction(boolean waitForAddressLookup, MenuAction menuAction) {
		if (searchingAddress() && waitForAddressLookup) {
			menuAction.dlg = new ProgressDialog(mapActivity);
			menuAction.dlg.setTitle("");
			menuAction.dlg.setMessage(searchAddressStr);
			menuAction.dlg.setButton(Dialog.BUTTON_NEGATIVE, mapActivity.getResources().getString(R.string.shared_string_skip), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cancelSearchAddress();
				}
			});
			menuAction.dlg.show();
			searchDoneAction = menuAction;
		} else {
			menuAction.run();
		}
	}

	public void addAsLastIntermediate() {
		mapActivity.getMyApplication().getTargetPointsHelper().navigateToPoint(latLon,
				true, mapActivity.getMyApplication().getTargetPointsHelper().getIntermediatePoints().size(),
				getPointDescriptionForTarget());
		close();
	}

	public void addWptPt() {
		String title = getTitleStr();
		if (pointDescription.isWpt() || !hasValidTitle()) {
			title = "";
		}

		final List<SelectedGpxFile> list
				= mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
		if (list.isEmpty() || (list.size() == 1 && list.get(0).getGpxFile().showCurrentTrack)) {
			GPXFile gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
			getWptPtPointEditor().add(gpxFile, latLon, title);
		} else {
			addNewWptToGPXFile(title);
		}
	}

	public void editWptPt() {
		if (object != null && object instanceof WptPt) {
			getWptPtPointEditor().edit((WptPt) object);
		}
	}

	public AlertDialog addNewWptToGPXFile(final String title) {
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				GPXFile gpxFile;
				if (result != null && result.length > 0) {
					gpxFile = result[0];
				} else {
					gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
				}
				getWptPtPointEditor().add(gpxFile, latLon, title);
				return true;
			}
		};

		return GpxUiHelper.selectSingleGPXFile(mapActivity, true, callbackWithObject);
	}

	private PointDescription getPointDescriptionForTarget() {
		if (pointDescription.isLocation()
				&& pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
			return new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
		} else {
			return pointDescription;
		}
	}

	public void setBaseFragmentVisibility(boolean visible) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().setFragmentVisibility(visible);
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

	public void openMenuFullScreen() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().openMenuFullScreen();
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
			return MenuState.HEADER_ONLY;
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

	public boolean supportZoomIn() {
		return menuController == null || menuController.supportZoomIn();
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

	public boolean hasCustomAddressLine() {
		return menuController != null && menuController.hasCustomAddressLine();
	}

	public void buildCustomAddressLine(LinearLayout ll) {
		if (menuController != null) {
			menuController.buildCustomAddressLine(ll);
		}
	}

	public boolean isNightMode() {
		if (menuController != null) {
			return !menuController.isLight();
		} else {
			return mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
	}

	public boolean hasHiddenBottomInfo() {
		return getCurrentMenuState() == MenuState.HEADER_ONLY;
	}

	public LatLon getMyLocation() {
		return myLocation;
	}

	public Float getHeading() {
		return heading;
	}

	public void updateMyLocation(net.osmand.Location location) {
		if (location != null && active && displayDistanceDirection()) {
			myLocation = new LatLon(location.getLatitude(), location.getLongitude());
			updateLocation(false, true, false);
		}
	}

	public void updateCompassValue(float value) {
		if (active && displayDistanceDirection()) {
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
				WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
				if (fragmentRef != null) {
					fragmentRef.get().updateLocation(centerChanged, locationChanged, compassChanged);
				}
			}
		});
	}

	private abstract class MenuAction implements Runnable {
		protected ProgressDialog dlg;
	}
}