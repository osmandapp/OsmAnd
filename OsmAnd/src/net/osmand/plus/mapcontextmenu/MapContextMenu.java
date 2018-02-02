package net.osmand.plus.mapcontextmenu;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.TargetPointsHelper.TargetPointChangedListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController.ContextMenuToolbarController;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.controllers.MapDataMenuController;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.PointEditor;
import net.osmand.plus.mapcontextmenu.editors.RtePtEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.RenameMarkerBottomSheetDialogFragment;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class MapContextMenu extends MenuTitleController implements StateChangedListener<ApplicationMode>,
		MapMarkerChangedListener, TargetPointChangedListener {

	private MapActivity mapActivity;
	private OsmandSettings settings;
	private MapMultiSelectionMenu mapMultiSelectionMenu;

	private FavoritePointEditor favoritePointEditor;
	private WptPtEditor wptPtEditor;
	private RtePtEditor rtePtEditor;

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
	private boolean cachedMyLocation;
	private boolean appModeChanged;
	private boolean appModeListenerAdded;
	private boolean autoHide;

	private int favActionIconId;
	private int waypointActionIconId;

	private MenuAction searchDoneAction;

	private LinkedList<MapContextMenuData> historyStack = new LinkedList<>();

	public static class MapContextMenuData {
		private LatLon latLon;
		private PointDescription pointDescription;
		private Object object;
		private boolean backAction;

		public MapContextMenuData(LatLon latLon, PointDescription pointDescription, Object object, boolean backAction) {
			this.latLon = latLon;
			this.pointDescription = pointDescription;
			this.object = object;
			this.backAction = backAction;
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

		public boolean hasBackAction() {
			return backAction;
		}
	}

	public MapContextMenu() {
	}

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
		if (rtePtEditor != null) {
			rtePtEditor.setMapActivity(mapActivity);
		}

		if (active) {
			acquireMenuController(false);
			if (menuController != null) {
				menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
			}
			if (searchDoneAction != null && searchDoneAction.dlg != null && searchDoneAction.dlg.getOwnerActivity() != mapActivity) {
				searchDoneAction.dlg = buildSearchActionDialog();
				searchDoneAction.dlg.show();
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

	public RtePtEditor getRtePtPointEditor() {
		if (rtePtEditor == null) {
			rtePtEditor = new RtePtEditor(mapActivity);
		}
		return rtePtEditor;
	}

	public PointEditor getPointEditor(String tag) {
		if (favoritePointEditor != null && favoritePointEditor.getFragmentTag().equals(tag)) {
			return favoritePointEditor;
		} else if (wptPtEditor != null && wptPtEditor.getFragmentTag().equals(tag)) {
			return wptPtEditor;
		} else if (rtePtEditor != null && rtePtEditor.getFragmentTag().equals(tag)) {
			return rtePtEditor;
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

	public boolean init(@NonNull LatLon latLon,
						@Nullable PointDescription pointDescription,
						@Nullable Object object) {
		return init(latLon, pointDescription, object, false, false);
	}

	public boolean init(@NonNull LatLon latLon,
						@Nullable PointDescription pointDescription,
						@Nullable Object object,
						boolean update, boolean restorePrevious) {
		OsmandApplication app = mapActivity.getMyApplication();

		if (myLocation == null) {
			updateMyLocation(app.getLocationProvider().getLastKnownLocation(), false);
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
			if (menuController != null) {
				menuController.setMapContextMenu(null);
			}
			if (!acquireMenuController(restorePrevious)) {
				active = false;
				clearSelectedObject(object);
				return false;
			}
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
			app.getMapMarkersHelper().addListener(this);
		} else if (object instanceof TargetPoint) {
			app.getTargetPointsHelper().addPointListener(this);
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

	public void show(@NonNull LatLon latLon,
					 @Nullable PointDescription pointDescription,
					 @Nullable Object object) {
		if (init(latLon, pointDescription, object)) {
			mapActivity.getMyApplication().logEvent(mapActivity, "open_context_menu");
			showInternal();
		}
	}

	private void showInternal() {
		if (!MapContextMenuFragment.showInstance(this, mapActivity, centerMarker)) {
			active = false;
		} else {
			if (menuController != null) {
				menuController.onShow();
			}
		}
		centerMarker = false;
		autoHide = false;
	}

	public void update(LatLon latLon, PointDescription pointDescription, Object object) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		init(latLon, pointDescription, object, true, false);
		if (fragmentRef != null) {
			fragmentRef.get().rebuildMenu(centerMarker);
		}
		ContextMenuLayer contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
		contextMenuLayer.updateContextMenu();
		centerMarker = false;
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

	public void onFragmentResume() {
		if (active && displayDistanceDirection() && myLocation != null) {
			updateLocation(false, true, false);
		}
	}

	public boolean navigateInPedestrianMode() {
		if (menuController != null) {
			return menuController.navigateInPedestrianMode();
		}
		return false;
	}

	public boolean close() {
		boolean result = false;
		if (active) {
			active = false;
			if (object instanceof MapMarker) {
				mapActivity.getMyApplication().getMapMarkersHelper().removeListener(this);
			}
			if (menuController != null) {
				if (menuController.hasBackAction()) {
					clearHistoryStack();
				}
				menuController.onClose();
			}
			if (this.object != null) {
				clearSelectedObject(this.object);
			}
			result = hide();
			if (menuController != null) {
				menuController.setActive(false);
			}
			mapActivity.refreshMap();
		}
		return result;
	}

	public boolean hide(boolean animated) {
		boolean result = false;
		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(mapPosition);
			mapPosition = 0;
		}
		if (menuController != null) {
			menuController.onHide();
		}
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			if (!animated) {
				fragmentRef.get().disableTransitionAnimation();
			}
			fragmentRef.get().dismissMenu();
			result = true;
		}
		return result;
	}

	public boolean hide() {
		return hide(true);
	}

	public void updateControlsVisibility(boolean menuVisible) {
		int topControlsVisibility = shouldShowTopControls(menuVisible) ? View.VISIBLE : View.GONE;
		mapActivity.findViewById(R.id.map_center_info).setVisibility(topControlsVisibility);
		mapActivity.findViewById(R.id.map_left_widgets_panel).setVisibility(topControlsVisibility);
		mapActivity.findViewById(R.id.map_right_widgets_panel).setVisibility(topControlsVisibility);

		int bottomControlsVisibility = shouldShowBottomControls(menuVisible) ? View.VISIBLE : View.GONE;
		mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(bottomControlsVisibility);

		mapActivity.refreshMap();
	}

	public boolean shouldShowTopControls() {
		return shouldShowTopControls(isVisible());
	}

	public boolean shouldShowTopControls(boolean menuVisible) {
		return !menuVisible || isLandscapeLayout() || getCurrentMenuState() == MenuController.MenuState.HEADER_ONLY;
	}

	public boolean shouldShowBottomControls(boolean menuVisible) {
		return !menuVisible || isLandscapeLayout();
	}

	// timeout in msec
	public void hideWithTimeout(long timeout) {
		autoHide = true;
		mapActivity.getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (autoHide) {
					hide();
				}
			}
		}, timeout);
	}

	public void updateLayout() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateLayout();
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
		if (object != null && object.equals(mapMarker)) {
			String address = mapMarker.getOnlyName();
			updateTitle(address);
		}
	}

	@Override
	public void onMapMarkersChanged() {
	}

	@Override
	public void onTargetPointChanged(TargetPoint targetPoint) {
		if (object != null && object.equals(targetPoint)) {
			String address = targetPoint.getOnlyName();
			updateTitle(address);
		}
	}

	private void updateTitle(String address) {
		nameStr = address;
		pointDescription.setName(address);
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().refreshTitle();
	}

	@Override
	public void stateChanged(ApplicationMode change) {
		appModeChanged = active;
	}

	private void clearSelectedObject(Object object) {
		mapActivity.getMapLayers().getContextMenuLayer().setSelectedObject(null);
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

	private void setSelectedObject(@Nullable Object object) {
		mapActivity.getMapLayers().getContextMenuLayer().setSelectedObject(object);
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

	private boolean acquireMenuController(boolean restorePrevious) {
		MapContextMenuData menuData = null;
		if (menuController != null) {
			if (menuController.isActive() && !restorePrevious) {
				menuData = new MapContextMenuData(
						menuController.getLatLon(), menuController.getPointDescription(),
						menuController.getObject(), menuController.hasBackAction());
			}
			menuController.onAcquireNewController(pointDescription, object);
		}
		menuController = MenuController.getMenuController(mapActivity, latLon, pointDescription, object, MenuType.STANDARD);
		if (menuController.setActive(true)) {
			menuController.setMapContextMenu(this);
			if (menuData != null && (object != menuData.getObject())
					&& (menuController.hasBackAction() || menuData.hasBackAction())) {
				historyStack.add(menuData);
			}
			if (!(menuController instanceof MapDataMenuController)) {
				menuController.buildMapDownloadButtonAndSizeInfo(latLon);
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean showPreviousMenu() {
		MapContextMenuData menuData;
		if (hasHistoryStackBackAction()) {
			do {
				menuData = historyStack.pollLast();
			} while (menuData != null && !menuData.hasBackAction());
		} else {
			menuData = historyStack.pollLast();
		}
		if (menuData != null) {
			if (init(menuData.getLatLon(), menuData.getPointDescription(), menuData.getObject(), false, true)) {
				showInternal();
			}
			return active;
		} else {
			return false;
		}
	}

	private boolean hasHistoryStackBackAction() {
		for (MapContextMenuData menuData : historyStack) {
			if (menuData.hasBackAction()) {
				return true;
			}
		}
		return false;
	}

	private void clearHistoryStack() {
		historyStack.clear();
	}

	public void backToolbarAction(MenuController menuController) {
		menuController.onClose();
		if (!showPreviousMenu() && this.menuController.getClass() == menuController.getClass()) {
			close();
		}
	}

	public boolean hasActiveToolbar() {
		TopToolbarController toolbarController = mapActivity.getTopToolbarController(TopToolbarControllerType.CONTEXT_MENU);
		return toolbarController != null && toolbarController instanceof ContextMenuToolbarController;
	}

	public void closeActiveToolbar() {
		TopToolbarController toolbarController = mapActivity.getTopToolbarController(TopToolbarControllerType.CONTEXT_MENU);
		if (toolbarController != null && toolbarController instanceof ContextMenuToolbarController) {
			MenuController menuController = ((ContextMenuToolbarController) toolbarController).getMenuController();
			closeToolbar(menuController);
		}
	}

	public void closeToolbar(MenuController menuController) {
		if (this.menuController.getClass() == menuController.getClass()) {
			close();
		} else {
			clearHistoryStack();
			menuController.onClose();
			mapActivity.refreshMap();
		}
	}

	public boolean onSingleTapOnMap() {
		boolean result = false;
		if (menuController == null || !menuController.handleSingleTapOnMap()) {
			if (menuController != null && !menuController.isClosable()) {
				result = hide();
			} else {
				updateMapCenter(null);
				result = close();
			}
			if (mapActivity.getMapLayers().getMapQuickActionLayer().isLayerOn()) {
				mapActivity.getMapLayers().getMapQuickActionLayer().refreshLayer();
			}
		}
		return result;
	}

	@Override
	public void onSearchAddressDone() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().refreshTitle();
		}

		if (searchDoneAction != null) {
			if (searchDoneAction.dlg != null) {
				try {
					searchDoneAction.dlg.dismiss();
				} catch (Exception e) {
					// ignore
				} finally {
					searchDoneAction.dlg = null;
				}
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

	public int getFavActionStringId() {
		if (menuController != null)
			return menuController.getFavActionStringId();
		return R.string.shared_string_add;
	}

	public int getWaypointActionIconId() {
		return waypointActionIconId;
	}

	public int getWaypointActionStringId() {
		if (menuController != null) {
			return menuController.getWaypointActionStringId();
		}
		return settings.USE_MAP_MARKERS.get()
				? R.string.shared_string_add_to_map_markers : R.string.context_menu_item_destination_point;
	}

	public boolean isButtonWaypointEnabled() {
		if (menuController != null) {
			return menuController.isWaypointButtonEnabled();
		}
		return true;
	}

	protected void acquireIcons() {
		super.acquireIcons();
		if (menuController != null) {
			favActionIconId = menuController.getFavActionIconId();
			waypointActionIconId = menuController.getWaypointActionIconId();
		} else {
			favActionIconId = R.drawable.map_action_fav_dark;
			waypointActionIconId = settings.USE_MAP_MARKERS.get()
					? R.drawable.map_action_flag_dark : R.drawable.map_action_waypoint;
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

	public List<TransportStopRoute> getTransportStopRoutes() {
		if (menuController != null) {
			return menuController.getTransportStopRoutes();
		}
		return null;
	}

	public void navigateButtonPressed() {
		if (navigateInPedestrianMode()) {
			settings.APPLICATION_MODE.set(ApplicationMode.PEDESTRIAN);
		}
		mapActivity.getMapLayers().getMapControlsLayer().navigateButton();
	}

	public boolean zoomInPressed() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().doZoomIn();
			return true;
		}
		return false;
	}

	public boolean zoomOutPressed() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().doZoomOut();
			return true;
		}
		return false;
	}

	public void buttonWaypointPressed() {
		if (object != null && object instanceof MapMarker) {
			RenameMarkerBottomSheetDialogFragment
					.showInstance(mapActivity.getSupportFragmentManager(), (MapMarker) object);
		} else {
			if (pointDescription.isDestination()) {
				mapActivity.getMapActions().editWaypoints();
			} else if (settings.USE_MAP_MARKERS.get()) {
				if (pointDescription.isMapMarker()) {
					hide();
					MapActivity.clearPrevActivityIntent();
					MapMarkersDialogFragment.showInstance(mapActivity);
				} else {
					String mapObjectName = null;
					if (object instanceof Amenity) {
						Amenity amenity = (Amenity) object;
						mapObjectName = amenity.getName() + "_" + amenity.getType().getKeyName();
					}
					mapActivity.getMapActions().addMapMarker(latLon.getLatitude(), latLon.getLongitude(),
							getPointDescriptionForMarker(), mapObjectName);
				}
			} else {
				mapActivity.getMapActions().addAsTarget(latLon.getLatitude(), latLon.getLongitude(),
						getPointDescriptionForTarget());
			}
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
					String originObjectName = "";
					if (object != null) {
						if (object instanceof Amenity) {
							originObjectName = ((Amenity) object).toStringEn();
						} else if (object instanceof TransportStop) {
							originObjectName = ((TransportStop) object).toStringEn();
						}
					}
					getFavoritePointEditor().add(latLon, title, originObjectName);
				}
			});
		}
	}

	public void buttonSharePressed() {
		if (menuController != null) {
			menuController.share(latLon, nameStr, streetStr);
		} else {
			ShareMenu.show(latLon, nameStr, streetStr, mapActivity);
		}
	}

	public void buttonMorePressed() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter();
		for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
			layer.populateObjectContextMenu(latLon, object, menuAdapter, mapActivity);
		}

		mapActivity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, object);
	}

	private void callMenuAction(boolean waitForAddressLookup, MenuAction menuAction) {
		if (searchingAddress() && waitForAddressLookup) {
			menuAction.dlg = buildSearchActionDialog();
			menuAction.dlg.show();
			searchDoneAction = menuAction;
		} else {
			menuAction.run();
		}
	}

	private ProgressDialog buildSearchActionDialog() {
		ProgressDialog dlg = new ProgressDialog(mapActivity);
		dlg.setTitle("");
		dlg.setMessage(searchAddressStr);
		dlg.setButton(Dialog.BUTTON_NEGATIVE, mapActivity.getResources().getString(R.string.shared_string_skip), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				cancelSearchAddress();
			}
		});
		return dlg;
	}

	public boolean openEditor() {
		if (object != null) {
			if (object instanceof FavouritePoint) {
				getFavoritePointEditor().edit((FavouritePoint) object);
				return true;
			} else if (object instanceof WptPt) {
				getWptPtPointEditor().edit((WptPt) object);
				return true;
			}
		}
		return false;
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
		if ((list.isEmpty() || (list.size() == 1 && list.get(0).getGpxFile().showCurrentTrack))
				&& OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
			GPXFile gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
			getWptPtPointEditor().add(gpxFile, latLon, title);
		} else {
			addNewWptToGPXFile(title);
		}
	}

	public void addWptPt(LatLon latLon, String title, String categoryName, int categoryColor, boolean skipDialog) {

		final List<SelectedGpxFile> list
				= mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
		if (list.isEmpty() || (list.size() == 1 && list.get(0).getGpxFile().showCurrentTrack)) {
			GPXFile gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
			getWptPtPointEditor().add(gpxFile, latLon, title, categoryName, categoryColor, skipDialog);
		} else {
			addNewWptToGPXFile(latLon, title, categoryName, categoryColor, skipDialog);
		}
	}

	public void editWptPt() {
		if (object != null && object instanceof WptPt) {
			getWptPtPointEditor().edit((WptPt) object);
		}
	}

	public AlertDialog addNewWptToGPXFile(final LatLon latLon, final String title,
										  final String categoryName,
										  final int categoryColor, final boolean skipDialog) {
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				GPXFile gpxFile;
				if (result != null && result.length > 0) {
					gpxFile = result[0];
				} else {
					gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
				}
				getWptPtPointEditor().add(gpxFile, latLon, title, categoryName, categoryColor, skipDialog);
				return true;
			}
		};

		return GpxUiHelper.selectSingleGPXFile(mapActivity, true, callbackWithObject);
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

	public PointDescription getPointDescriptionForTarget() {
		if (pointDescription.isLocation()
				&& pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
			return new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
		} else {
			return pointDescription;
		}
	}

	public PointDescription getPointDescriptionForMarker() {
		PointDescription pd = getPointDescriptionForTarget();
		if (Algorithms.isEmpty(pd.getName()) && !Algorithms.isEmpty(nameStr)
				&& !nameStr.equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
			return new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, nameStr);
		} else {
			return pd;
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

	public int getLandscapeWidthPx() {
		if (menuController != null) {
			return menuController.getLandscapeWidthPx();
		} else {
			return 0;
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
			return .75f;
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

	public TitleButtonController getBottomTitleButtonController() {
		if (menuController != null) {
			return menuController.getBottomTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getLeftDownloadButtonController() {
		if (menuController != null) {
			return menuController.getLeftDownloadButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getRightDownloadButtonController() {
		if (menuController != null) {
			return menuController.getRightDownloadButtonController();
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

	public boolean navigateButtonVisible() {
		return menuController == null || menuController.navigateButtonVisible();
	}

	public boolean zoomButtonsVisible() {
		return menuController == null || menuController.zoomButtonsVisible();
	}

	public boolean isClosable() {
		return menuController == null || menuController.isClosable();
	}

	public boolean buttonsVisible() {
		return menuController == null || menuController.buttonsVisible();
	}

	public boolean displayDistanceDirection() {
		return menuController != null && menuController.displayDistanceDirection();
	}

	public String getSubtypeStr() {
		if (menuController != null) {
			return menuController.getSubtypeStr();
		}
		return "";
	}

	public Drawable getSubtypeIcon() {
		if (menuController != null) {
			return menuController.getSubtypeIcon();
		}
		return null;
	}

	public int getAdditionalInfoColor() {
		if (menuController != null) {
			return menuController.getAdditionalInfoColorId();
		}
		return 0;
	}

	public CharSequence getAdditionalInfo() {
		if (menuController != null) {
			return menuController.getAdditionalInfoStr();
		}
		return "";
	}

	public int getAdditionalInfoIconRes() {
		if (menuController != null) {
			return menuController.getAdditionalInfoIconRes();
		}
		return 0;
	}

	public boolean isMapDownloaded() {
		return menuController != null && menuController.isMapDownloaded();
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

	public boolean isCachedMyLocation() {
		return cachedMyLocation;
	}

	public Float getHeading() {
		return heading;
	}

	private void updateMyLocation(Location location, boolean updateLocationUi) {
		if (location == null) {
			location = getMapActivity().getMyApplication().getLocationProvider().getLastStaleKnownLocation();
			cachedMyLocation = location != null;
		} else {
			cachedMyLocation = false;
		}
		if (location != null) {
			myLocation = new LatLon(location.getLatitude(), location.getLongitude());
			if (updateLocationUi) {
				updateLocation(false, true, false);
			}
		}
	}

	public void updateMyLocation(net.osmand.Location location) {
		if (active && displayDistanceDirection()) {
			updateMyLocation(location, true);
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