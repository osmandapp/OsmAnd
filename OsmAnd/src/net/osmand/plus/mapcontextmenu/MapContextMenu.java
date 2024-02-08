package net.osmand.plus.mapcontextmenu;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.OnResultCallback;
import net.osmand.StateChangedListener;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPointChangedListener;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener;
import net.osmand.plus.mapcontextmenu.MenuController.ContextMenuToolbarController;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.controllers.MapDataMenuController;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.MapMarkerEditor;
import net.osmand.plus.mapcontextmenu.editors.PointEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MapContextMenu extends MenuTitleController implements StateChangedListener<ApplicationMode>,
		MapMarkerChangedListener, TargetPointChangedListener {

	@Nullable
	private MapActivity mapActivity;

	@Nullable
	private MapMultiSelectionMenu mapMultiSelectionMenu;

	@Nullable
	private FavoritePointEditor favoritePointEditor;
	@Nullable
	private WptPtEditor wptPtEditor;
	@Nullable
	private MapMarkerEditor mapMarkerEditor;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	@Nullable
	private Object object;
	@Nullable
	private MenuController menuController;

	private LatLon mapCenter;
	private boolean centerMarker;
	private boolean zoomOutOnly;
	private int mapZoom;

	private boolean inLocationUpdate;
	private boolean appModeChanged;
	private boolean appModeListenerAdded;
	private boolean autoHide;
	private boolean shouldUpdateMapDisplayPosition;

	private int favActionIconId;
	private int waypointActionIconId;

	private MenuAction searchDoneAction;

	private final LinkedList<MapContextMenuData> historyStack = new LinkedList<>();

	public void updateNightMode() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			menuController.updateNightMode();
		}
	}

	public static class MapContextMenuData {

		private final LatLon latLon;
		private final PointDescription pointDescription;
		private final Object object;
		private final boolean backAction;

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

	@Nullable
	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		OsmandApplication app = getMyApplication();
		if (app == null && mapActivity != null) {
			app = mapActivity.getMyApplication();
		}
		if (mapActivity != null && !appModeListenerAdded) {
			app.getSettings().APPLICATION_MODE.addListener(this);
			appModeListenerAdded = true;
		} else if (mapActivity == null && app != null) {
			app.getSettings().APPLICATION_MODE.removeListener(this);
			appModeListenerAdded = false;
		}

		this.mapActivity = mapActivity;

		MapMultiSelectionMenu mapMultiSelectionMenu = getMultiSelectionMenu();
		if (mapMultiSelectionMenu == null) {
			if (mapActivity != null) {
				this.mapMultiSelectionMenu = new MapMultiSelectionMenu(mapActivity);
			}
		} else {
			mapMultiSelectionMenu.setMapActivity(mapActivity);
		}

		if (favoritePointEditor != null) {
			favoritePointEditor.setMapActivity(mapActivity);
		}
		if (wptPtEditor != null) {
			wptPtEditor.setMapActivity(mapActivity);
		}
		if (mapMarkerEditor != null) {
			mapMarkerEditor.setMapActivity(mapActivity);
		}

		if (active && mapActivity != null) {
			acquireMenuController(false);
			MenuController menuController = getMenuController();
			if (menuController != null) {
				menuController.addPlainMenuItems(typeStr, getPointDescription(), getLatLon());
			}
			MenuAction searchDoneAction = this.searchDoneAction;
			if (searchDoneAction != null && searchDoneAction.dlg != null && searchDoneAction.dlg.getOwnerActivity() != mapActivity) {
				ProgressDialog dlg = buildSearchActionDialog();
				searchDoneAction.dlg = dlg;
				if (dlg != null) {
					dlg.show();
				}
			}
		} else {
			menuController = null;
		}
	}

	@Nullable
	public MapMultiSelectionMenu getMultiSelectionMenu() {
		return mapMultiSelectionMenu;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	public void hideMenus() {
		if (isVisible()) {
			hide();
		} else {
			MapMultiSelectionMenu mapMultiSelectionMenu = getMultiSelectionMenu();
			if (mapMultiSelectionMenu != null && mapMultiSelectionMenu.isVisible()) {
				mapMultiSelectionMenu.hide();
			}
		}
	}

	@Nullable
	public FavoritePointEditor getFavoritePointEditor() {
		MapActivity mapActivity = getMapActivity();
		if (favoritePointEditor == null && mapActivity != null) {
			favoritePointEditor = new FavoritePointEditor(mapActivity);
		}
		return favoritePointEditor;
	}

	@Nullable
	public WptPtEditor getWptPtPointEditor() {
		MapActivity mapActivity = getMapActivity();
		if (wptPtEditor == null && mapActivity != null) {
			wptPtEditor = new WptPtEditor(mapActivity);
		}
		return wptPtEditor;
	}

	@Nullable
	public MapMarkerEditor getMapMarkerEditor() {
		MapActivity mapActivity = getMapActivity();
		if (mapMarkerEditor == null && mapActivity != null) {
			mapMarkerEditor = new MapMarkerEditor(mapActivity);
		}
		return mapMarkerEditor;
	}

	@Nullable
	public PointEditor getPointEditor(String tag) {
		if (favoritePointEditor != null && favoritePointEditor.getFragmentTag().equals(tag)) {
			return favoritePointEditor;
		} else if (wptPtEditor != null && wptPtEditor.getFragmentTag().equals(tag)) {
			return wptPtEditor;
		} else if (mapMarkerEditor != null && mapMarkerEditor.getFragmentTag().equals(tag)) {
			return mapMarkerEditor;
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

	public boolean isZoomOutOnly() {
		return zoomOutOnly;
	}

	public void setZoomOutOnly(boolean zoomOutOnly) {
		this.zoomOutOnly = zoomOutOnly;
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

	@Override
	public PointDescription getPointDescription() {
		return pointDescription;
	}

	@Nullable
	@Override
	public Object getObject() {
		return object;
	}

	public boolean isExtended() {
		return getMenuController() != null;
	}

	@Nullable
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

		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return false;
		}

		OsmandApplication app = mapActivity.getMyApplication();

		Object thisObject = getObject();
		if (!update && isVisible()) {
			if (thisObject == null || !thisObject.equals(object)
					|| (thisObject instanceof Amenity && !((Amenity) thisObject).strictEquals(object))) {
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

		MenuController menuController = getMenuController();
		boolean needAcquireMenuController = menuController == null
				|| appModeChanged
				|| !update
				|| thisObject == null && object != null
				|| thisObject != null && object == null
				|| (thisObject != null && !thisObject.getClass().equals(object.getClass()));

		this.latLon = latLon;
		this.object = object;

		active = true;
		appModeChanged = false;
		shouldUpdateMapDisplayPosition = true;

		if (needAcquireMenuController) {
			if (menuController != null) {
				menuController.setMapContextMenu(null);
			}
			if (!acquireMenuController(restorePrevious)) {
				active = false;
				clearSelectedObject(object);
				return false;
			}
			menuController = getMenuController();
		} else {
			menuController.update(pointDescription, object);
		}
		initTitle();

		if (menuController != null) {
			menuController.clearPlainMenuItems();
			menuController.addPlainMenuItems(typeStr, getPointDescription(), getLatLon());
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
				wasInit = init(getLatLon(), getPointDescription(), getObject());
			}
			if (wasInit && !MapContextMenuFragment.showInstance(this, getMapActivity(), true)) {
				active = false;
			}
		} else {
			centerMarkerLocation();
		}
	}

	public void centerMarkerLocation() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().centerMarkerLocation();
		}
	}

	public void show(@NonNull LatLon latLon,
	                 @Nullable PointDescription pointDescription,
	                 @Nullable Object object) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && init(latLon, pointDescription, object)) {
			mapActivity.getMyApplication().logEvent("open_context_menu");
			showInternal();
		}
	}

	private void showInternal() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (!MapContextMenuFragment.showInstance(this, mapActivity, centerMarker)) {
				active = false;
			} else {
				MenuController menuController = getMenuController();
				if (menuController != null) {
					menuController.onShow();
				}
			}
		}
		centerMarker = false;
		autoHide = false;
	}

	public void update(LatLon latLon, PointDescription pointDescription, Object object) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
			init(latLon, pointDescription, object, true, false);
			if (fragmentRef != null) {
				fragmentRef.get().rebuildMenu(centerMarker);
			}
			ContextMenuLayer contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
			contextMenuLayer.updateContextMenu();
			centerMarker = false;
		}
	}

	public void showOrUpdate(LatLon latLon, PointDescription pointDescription, Object object) {
		Object thisObject = getObject();
		if (isVisible() && thisObject != null && thisObject.equals(object)) {
			update(latLon, pointDescription, object);
		} else {
			show(latLon, pointDescription, object);
		}
	}

	public void showMinimized(LatLon latLon, PointDescription pointDescription, Object object) {
		init(latLon, pointDescription, object);
	}

	public void onFragmentResume() {
		if (active && displayDistanceDirection()) {
			updateLocation(false, true, false);
		}
	}

	public boolean navigateInPedestrianMode() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.navigateInPedestrianMode();
		}
		return false;
	}

	public boolean close() {
		return close(true);
	}

	public boolean close(boolean animated) {
		boolean result = false;
		if (active) {
			active = false;
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				Object object = getObject();
				if (object instanceof MapMarker) {
					mapActivity.getMyApplication().getMapMarkersHelper().removeListener(this);
				}
				MenuController menuController = getMenuController();
				if (menuController != null) {
					if (menuController.hasBackAction()) {
						clearHistoryStack();
					}
					menuController.onClose();
				}
				if (object != null) {
					clearSelectedObject(object);
				}
				result = hide(animated);
				if (menuController != null) {
					menuController.setActive(false);
				}
				mapActivity.refreshMap();
			}
		}
		return result;
	}

	public boolean hide(boolean animated) {
		boolean result = false;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			shouldUpdateMapDisplayPosition = false;
			MenuController menuController = getMenuController();
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
		}
		return result;
	}

	public boolean hide() {
		return hide(true);
	}

	public void updateControlsVisibility(boolean menuVisible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean topControlsVisible = shouldShowTopControls(menuVisible);
			boolean bottomControlsVisible = shouldShowBottomControls(menuVisible);
			mapActivity.getWidgetsVisibilityHelper().updateControlsVisibility(topControlsVisible, bottomControlsVisible);
			mapActivity.refreshMap();
		}
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMyApplication().runInUIThread(() -> {
				if (autoHide) {
					hide();
				}
			}, timeout);
		}
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
		Object object = getObject();
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
		Object object = getObject();
		if (object != null && object.equals(targetPoint)) {
			String address = targetPoint.getOnlyName();
			updateTitle(address);
		}
	}

	private void updateTitle(String address) {
		setNameStr(address);
		getPointDescription().setName(address);
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().refreshTitle();
	}

	@Override
	public void stateChanged(ApplicationMode change) {
		appModeChanged = active;
	}

	private void clearSelectedObject(Object object) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
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
	}

	private void setSelectedObject(@Nullable Object object) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getContextMenuLayer().setSelectedObject(object);
			if (object != null) {
				for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
					if (layer instanceof IContextMenuProvider) {
						PointDescription pointDescription = ((IContextMenuProvider) layer).getObjectName(object);
						if (pointDescription != null) {
							if (layer instanceof IContextMenuProviderSelection) {
								((ContextMenuLayer.IContextMenuProviderSelection) layer).setSelectedObject(object);
							}
						}
					}
				}
			}
		}
	}

	private boolean acquireMenuController(boolean restorePrevious) {
		MapContextMenuData menuData = null;
		MenuController menuController = getMenuController();
		LatLon latLon = getLatLon();
		Object object = getObject();
		PointDescription pointDescription = getPointDescription();
		if (menuController != null) {
			if (menuController.isActive() && !restorePrevious) {
				menuData = new MapContextMenuData(
						menuController.getLatLon(), menuController.getPointDescription(),
						menuController.getObject(), menuController.hasBackAction());
			}
			menuController.onAcquireNewController(pointDescription, object);
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			menuController = MenuController.getMenuController(mapActivity, latLon, pointDescription, object, MenuType.STANDARD);
		} else {
			menuController = null;
		}
		this.menuController = menuController;
		if (menuController != null && menuController.setActive(true)) {
			menuController.setMapContextMenu(this);
			if (menuData != null && (object != menuData.getObject())
					&& (menuController.hasBackAction() || menuData.hasBackAction())) {
				historyStack.add(menuData);
			}
			if (!(menuController instanceof MapDataMenuController)) {
				menuController.requestMapDownloadInfo(latLon);
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
		MenuController thisMenuController = getMenuController();
		if (!showPreviousMenu() && thisMenuController != null &&
				thisMenuController.getClass() == menuController.getClass()) {
			close();
		}
	}

	public boolean hasActiveToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TopToolbarController toolbarController = mapActivity.getTopToolbarController(TopToolbarControllerType.CONTEXT_MENU);
			return toolbarController instanceof ContextMenuToolbarController;
		} else {
			return false;
		}
	}

	public void closeActiveToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TopToolbarController toolbarController = mapActivity.getTopToolbarController(TopToolbarControllerType.CONTEXT_MENU);
			if (toolbarController instanceof ContextMenuToolbarController) {
				MenuController menuController = ((ContextMenuToolbarController) toolbarController).getMenuController();
				closeToolbar(menuController);
			}
		}
	}

	public void closeToolbar(MenuController menuController) {
		MenuController thisMenuController = getMenuController();
		if (thisMenuController != null && thisMenuController.getClass() == menuController.getClass()) {
			close();
		} else {
			clearHistoryStack();
			menuController.onClose();
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		}
	}

	public boolean onSingleTapOnMap() {
		boolean result = false;
		MenuController menuController = getMenuController();
		if (menuController == null || !menuController.handleSingleTapOnMap()) {
			if (menuController != null && !menuController.isClosable()) {
				result = hide();
			} else {
				updateMapCenter(null);
				result = close();
			}
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && mapActivity.getMapLayers().getMapQuickActionLayer().isLayerOn()) {
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
		MenuAction searchDoneAction = this.searchDoneAction;
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
			this.searchDoneAction = null;
		}
	}

	@Nullable
	public WeakReference<MapContextMenuFragment> findMenuFragment() {
		MapActivity mapActivity = getMapActivity();
		Fragment fragment = mapActivity != null
				? mapActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG) : null;
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
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getFavActionStringId();
		}
		return R.string.shared_string_add;
	}

	boolean isFavButtonEnabled() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.isFavButtonEnabled();
		}
		return true;
	}

	public int getWaypointActionIconId() {
		return waypointActionIconId;
	}

	public int getWaypointActionStringId() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getWaypointActionStringId();
		}
		return R.string.shared_string_marker;
	}

	public boolean isButtonWaypointEnabled() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.isWaypointButtonEnabled();
		}
		return true;
	}

	protected void acquireIcons() {
		super.acquireIcons();
		MenuController menuController = getMenuController();
		if (menuController != null) {
			favActionIconId = menuController.getFavActionIconId();
			waypointActionIconId = menuController.getWaypointActionIconId();
		} else {
			favActionIconId = R.drawable.ic_action_favorite_stroke;
			waypointActionIconId = R.drawable.ic_action_flag_stroke;
		}
	}

	public int getFabIconId() {
		int res = R.drawable.ic_action_gdirections_dark;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				res = R.drawable.ic_action_waypoint;
			}
		}
		return res;
	}

	public List<TransportStopRoute> getTransportStopRoutes() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getTransportStopRoutes();
		}
		return null;
	}

	public List<TransportStopRoute> getLocalTransportStopRoutes() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getLocalTransportStopRoutes();
		}
		return null;
	}

	public List<TransportStopRoute> getNearbyTransportStopRoutes() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getNearbyTransportStopRoutes();
		}
		return null;
	}

	public void navigateButtonPressed() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (navigateInPedestrianMode()) {
				mapActivity.getMyApplication().getSettings().setApplicationMode(ApplicationMode.PEDESTRIAN, false);
			}
			mapActivity.getMapLayers().getMapActionsHelper().navigateButton();
		}
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapMarker marker = getMapMarker();
			if (marker != null) {
				MapMarkerEditor mapMarkerEditor = getMapMarkerEditor();
				if (mapMarkerEditor != null) {
					mapMarkerEditor.edit(marker);
				}
			} else {
				String mapObjectName = null;
				Object object = getObject();
				if (object instanceof Amenity) {
					Amenity amenity = (Amenity) object;
					mapObjectName = amenity.getName() + "_" + amenity.getType().getKeyName();
				}
				LatLon latLon = getLatLon();
				mapActivity.getMapActions().addMapMarker(latLon.getLatitude(), latLon.getLongitude(),
						getPointDescriptionForMarker(), mapObjectName);
				close();
			}
		}
	}

	@Nullable
	private MapMarker getMapMarker() {
		MenuController menuController = getMenuController();
		Object correspondingObj = menuController != null ? menuController.getCorrespondingMapObject() : null;
		if (correspondingObj instanceof MapMarker) {
			return (MapMarker) correspondingObj;
		}
		Object object = getObject();
		if (object instanceof MapMarker) {
			return (MapMarker) object;
		}
		return null;
	}


	public void buttonFavoritePressed() {
		Object object = getObject();
		if (object instanceof FavouritePoint) {
			FavoritePointEditor favoritePointEditor = getFavoritePointEditor();
			if (favoritePointEditor != null) {
				favoritePointEditor.edit((FavouritePoint) object);
			}
		} else {
			callMenuAction(true, new MenuAction() {
				@Override
				public void run() {
					String title = getTitleStr();
					PointDescription pointDescription = getPointDescription();
					if (pointDescription.isFavorite() || !hasValidTitle()) {
						title = "";
					}
					FavoritePointEditor favoritePointEditor = getFavoritePointEditor();
					if (favoritePointEditor != null) {
						favoritePointEditor.add(getLatLon(), title, getStreetStr(), getObject());
					}
				}
			});
		}
	}

	public void buttonSharePressed() {
		MenuController menuController = getMenuController();
		String address = getAddressToShare();
		LatLon latLon = getLatLon();
		if (menuController != null) {
			menuController.share(latLon, nameStr, address);
		} else {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				ShareMenu.show(latLon, nameStr, address, mapActivity);
			}
		}
	}

	public ContextMenuAdapter getActionsContextMenuAdapter(boolean configure) {
		MapActivity mapActivity = getMapActivity();
		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(getMyApplication());
		if (mapActivity != null) {
			LatLon latLon = getLatLon();
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(latLon, getObject(), menuAdapter);
			}
			mapActivity.getMapActions().addActionsToAdapter(configure ? 0 : latLon.getLatitude(), configure ? 0 : latLon.getLongitude(), menuAdapter, configure ? null : getObject(), configure);
		}
		return menuAdapter;
	}

	public ContextMenuItemClickListener getContextMenuItemClickListener(ContextMenuAdapter menuAdapter) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			LatLon latLon = getLatLon();
			return mapActivity.getMapActions().getContextMenuItemClickListener(latLon.getLatitude(), latLon.getLongitude(), menuAdapter);
		}
		return null;
	}

	public void showAdditionalActionsFragment(ContextMenuAdapter adapter, ContextMenuItemClickListener listener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().showAdditionalActionsFragment(adapter, listener);
		}
	}

	@Nullable
	private String getAddressToShare() {
		String address = null;
		Object object = getObject();
		if (object instanceof FavouritePoint) {
			FavouritePoint point = (FavouritePoint) object;
			address = point.getAddress();
		} else if (object instanceof WptPt) {
			WptPt point = (WptPt) object;
			address = point.getAddress();
		}
		return address != null ? address : streetStr;
	}

	private void callMenuAction(boolean waitForAddressLookup, MenuAction menuAction) {
		if (searchingAddress() && waitForAddressLookup) {
			ProgressDialog dlg = buildSearchActionDialog();
			menuAction.dlg = dlg;
			if (dlg != null) {
				dlg.show();
			}
			searchDoneAction = menuAction;
		} else {
			menuAction.run();
		}
	}

	@Nullable
	private ProgressDialog buildSearchActionDialog() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ProgressDialog dlg = new ProgressDialog(mapActivity);
			dlg.setTitle("");
			dlg.setMessage(searchAddressStr);
			dlg.setButton(Dialog.BUTTON_NEGATIVE, mapActivity.getResources().getString(R.string.shared_string_skip), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cancelSearchAddress();
				}
			});
			return dlg;
		} else {
			return null;
		}
	}

	public boolean openEditor() {
		Object object = getObject();
		if (object != null) {
			if (object instanceof FavouritePoint) {
				FavoritePointEditor favoritePointEditor = getFavoritePointEditor();
				if (favoritePointEditor != null) {
					favoritePointEditor.edit((FavouritePoint) object);
					return true;
				}
			} else if (object instanceof WptPt) {
				WptPtEditor wptPtPointEditor = getWptPtPointEditor();
				if (wptPtPointEditor != null) {
					wptPtPointEditor.edit((WptPt) object);
					return true;
				}
			}
		}
		return false;
	}

	public void addAsLastIntermediate() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMyApplication().getTargetPointsHelper().navigateToPoint(getLatLon(),
					true, mapActivity.getMyApplication().getTargetPointsHelper().getIntermediatePoints().size(),
					getPointDescriptionForTarget());
			close();
		}
	}

	public void addWptPt() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();

			PointDescription pointDescription = getPointDescription();
			String title = getTitleStr();
			if (pointDescription.isWpt() || !hasValidTitle()) {
				title = "";
			}

			Amenity amenity = null;
			Object object = getObject();
			if (object instanceof Amenity && pointDescription.isPoi()) {
				amenity = (Amenity) object;
			}

			List<SelectedGpxFile> list = app.getSelectedGpxHelper().getSelectedGPXFiles();
			boolean forceAddToCurrentTrack = PluginsHelper.isActive(OsmandMonitoringPlugin.class)
					&& (list.isEmpty() || (list.size() == 1 && list.get(0).getGpxFile().showCurrentTrack));

			if (forceAddToCurrentTrack) {
				GPXFile gpxFile = app.getSavingTrackHelper().getCurrentGpx();
				WptPtEditor wptPtPointEditor = getWptPtPointEditor();
				if (wptPtPointEditor != null) {
					wptPtPointEditor.add(gpxFile, getLatLon(), title, amenity);
				}
			} else {
				addNewWptToGPXFile(title, amenity);
			}
		}
	}

	public void addWptPt(@NonNull WptPt wptPt, @Nullable String categoryName, int categoryColor,
	                     boolean skipDialog, @Nullable GPXFile gpxFile) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WptPtEditor wptPtPointEditor = getWptPtPointEditor();
			if (wptPtPointEditor == null) {
				return;
			}

			if (gpxFile != null) {
				wptPtPointEditor.add(gpxFile, wptPt, categoryName, categoryColor, skipDialog);
			} else {
				List<SelectedGpxFile> list
						= mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
				if (list.isEmpty() || (list.size() == 1 && list.get(0).getGpxFile().showCurrentTrack)) {
					GPXFile currentGpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
					wptPtPointEditor.add(currentGpxFile, wptPt, categoryName, categoryColor, skipDialog);
				} else {
					addNewWptToGPXFile(wptPt, categoryName, categoryColor, skipDialog);
				}
			}
		}
	}

	public void editWptPt() {
		Object object = getObject();
		if (object instanceof WptPt) {
			WptPtEditor wptPtPointEditor = getWptPtPointEditor();
			if (wptPtPointEditor != null) {
				wptPtPointEditor.edit((WptPt) object);
			}
		}
	}

	public void addNewWptToGPXFile(@NonNull WptPt wptPt, @Nullable String categoryName, int categoryColor, boolean skipDialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
				@Override
				public boolean processResult(GPXFile[] result) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						GPXFile gpxFile = result != null && result.length > 0
								? result[0]
								: mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
						WptPtEditor wptPtPointEditor = getWptPtPointEditor();
						if (wptPtPointEditor != null) {
							wptPtPointEditor.add(gpxFile, wptPt, categoryName, categoryColor, skipDialog);
						}
					}
					return true;
				}
			};
			GpxUiHelper.selectSingleGPXFile(mapActivity, true, callbackWithObject);
		}
	}

	public void addNewWptToGPXFile(@Nullable String title, @Nullable Amenity amenity) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			addNewWptToGPXFileImpl(mapActivity, title, amenity);
		}
	}

	private void addNewWptToGPXFileImpl(@NonNull MapActivity mapActivity,
	                                    @Nullable String title, @Nullable Amenity amenity) {
		GpxUiHelper.selectSingleGPXFile(mapActivity, true, result -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				GPXFile gpxFile;
				if (result != null && result.length > 0) {
					gpxFile = result[0];
				} else {
					gpxFile = activity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
				}
				WptPtEditor wptPtPointEditor = getWptPtPointEditor();
				if (wptPtPointEditor != null) {
					wptPtPointEditor.add(gpxFile, getLatLon(), title, amenity);
				}
			}
			return true;
		});
	}

	@Nullable
	public PointDescription getPointDescriptionForTarget() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PointDescription pointDescription = getPointDescription();
			if (pointDescription.isLocation()
					&& pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
				return new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				return pointDescription;
			}
		} else {
			return null;
		}
	}

	@Nullable
	public PointDescription getPointDescriptionForMarker() {
		PointDescription pd = getPointDescriptionForTarget();
		MapActivity mapActivity = getMapActivity();
		if (pd != null && mapActivity != null) {
			if (Algorithms.isEmpty(pd.getName()) && !Algorithms.isEmpty(nameStr)
					&& !nameStr.equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
				return new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, nameStr);
			} else {
				return pd;
			}
		} else {
			return null;
		}
	}

	public void setBaseFragmentVisibility(boolean visible) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().setFragmentVisibility(visible);
		}
	}

	public boolean isLandscapeLayout() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.isLandscapeLayout();
	}

	public int getLandscapeWidthPx() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getLandscapeWidthPx();
		} else {
			return 0;
		}
	}

	public void openMenuHeaderOnly() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().openMenuHeaderOnly();
	}

	public void openMenuHalfScreen() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().openMenuHalfScreen();
	}

	public void openMenuFullScreen() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().openMenuFullScreen();
	}

	public boolean slideUp() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.slideUp();
	}

	public boolean slideDown() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.slideDown();
	}

	public void build(ViewGroup rootView) {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			menuController.build(rootView);
		}
	}

	public int getCurrentMenuState() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getCurrentMenuState();
		} else {
			return MenuState.HEADER_ONLY;
		}
	}

	public float getHalfScreenMaxHeightKoef() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getHalfScreenMaxHeightKoef();
		} else {
			return .75f;
		}
	}

	public int getSlideInAnimation() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getSlideInAnimation();
		} else {
			return 0;
		}
	}

	public int getSlideOutAnimation() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getSlideOutAnimation();
		} else {
			return 0;
		}
	}

	public TitleButtonController getLeftTitleButtonController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getLeftTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getRightTitleButtonController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getRightTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getBottomTitleButtonController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getBottomTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getLeftDownloadButtonController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getLeftDownloadButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getRightDownloadButtonController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getRightDownloadButtonController();
		} else {
			return null;
		}
	}

	public List<Pair<TitleButtonController, TitleButtonController>> getAdditionalButtonsControllers() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getAdditionalButtonsControllers();
		} else {
			return null;
		}
	}

	public TitleProgressController getTitleProgressController() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getTitleProgressController();
		} else {
			return null;
		}
	}

	public boolean supportZoomIn() {
		MenuController menuController = getMenuController();
		return menuController == null || menuController.supportZoomIn();
	}

	public boolean navigateButtonVisible() {
		MenuController menuController = getMenuController();
		return menuController == null || menuController.navigateButtonVisible();
	}

	public boolean zoomButtonsVisible() {
		MenuController menuController = getMenuController();
		return menuController == null || menuController.zoomButtonsVisible();
	}

	public boolean isClosable() {
		MenuController menuController = getMenuController();
		return menuController == null || menuController.isClosable();
	}

	public boolean buttonsVisible() {
		MenuController menuController = getMenuController();
		return menuController == null || menuController.buttonsVisible();
	}

	public boolean displayDistanceDirection() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.displayDistanceDirection();
	}

	public void getFormattedAltitude(@NonNull OnResultCallback<String> callback) {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			menuController.getFormattedAltitude(callback);
		} else {
			callback.onResult(null);
		}
	}

	public CharSequence getSubtypeStr() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getSubtypeStr();
		}
		return "";
	}

	public Drawable getSubtypeIcon() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getSubtypeIcon();
		}
		return null;
	}

	public int getAdditionalInfoColor() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getAdditionalInfoColorId();
		}
		return 0;
	}

	public CharSequence getAdditionalInfo() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getAdditionalInfoStr();
		}
		return "";
	}

	public int getAdditionalInfoIconRes() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			return menuController.getAdditionalInfoIconRes();
		}
		return 0;
	}

	public boolean isMapDownloaded() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.isMapDownloaded();
	}

	public void updateData() {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			menuController.updateData();
		}
	}

	public boolean hasCustomAddressLine() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.hasCustomAddressLine();
	}

	public void buildCustomAddressLine(LinearLayout ll) {
		MenuController menuController = getMenuController();
		if (menuController != null) {
			menuController.buildCustomAddressLine(ll);
		}
	}

	public boolean hasHiddenBottomInfo() {
		return getCurrentMenuState() == MenuState.HEADER_ONLY;
	}


	private void updateMyLocation(Location location, boolean updateLocationUi) {
		MapActivity mapActivity = getMapActivity();
		if (location == null && mapActivity != null) {
			location = mapActivity.getMyApplication().getLocationProvider().getLastStaleKnownLocation();
		}
		if (location != null) {
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
			updateLocation(false, false, true);
		}
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged,
	                           boolean compassChanged) {
		MapActivity mapActivity = getMapActivity();
		if (inLocationUpdate || mapActivity == null) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(() -> {
			inLocationUpdate = false;
			WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
			if (fragmentRef != null) {
				fragmentRef.get().updateLocation(centerChanged, locationChanged, compassChanged);
			}
		});
	}

	private abstract class MenuAction implements Runnable {
		protected ProgressDialog dlg;
	}
}