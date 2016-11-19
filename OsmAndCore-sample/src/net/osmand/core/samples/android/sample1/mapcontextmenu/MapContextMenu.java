package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController.MenuState;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController.MenuType;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;

public class MapContextMenu extends MenuTitleController {

	private MainActivity mainActivity;
	private MapMultiSelectionMenu mapMultiSelectionMenu;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	private Object object;
	private MenuController menuController;

	private LatLon mapCenter;
	private boolean centerMarker;
	private int mapZoom;

	private LatLon myLocation;
	private Float heading;
	private boolean inLocationUpdate = false;
	private boolean autoHide;

	private MenuAction searchDoneAction;

	public MapContextMenu() {
	}

	@Override
	public MainActivity getMainActivity() {
		return mainActivity;
	}

	public void setMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;

		if (mapMultiSelectionMenu == null) {
			mapMultiSelectionMenu = new MapMultiSelectionMenu(mainActivity);
		} else {
			mapMultiSelectionMenu.setMainActivity(mainActivity);
		}

		if (active) {
			acquireMenuController(false);
			if (menuController != null) {
				menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
			}
			if (searchDoneAction != null && searchDoneAction.dlg != null && searchDoneAction.dlg.getOwnerActivity() != mainActivity) {
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

		if (myLocation == null) {
			myLocation = mainActivity.getMyApplication().getLocationProvider().getLastKnownLocationLatLon();
		}

		if (!update && isVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		if (pointDescription == null) {
			this.pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		} else {
			this.pointDescription = pointDescription;
		}

		boolean needAcquireMenuController = menuController == null
				|| !update
				|| this.object == null && object != null
				|| this.object != null && object == null
				|| (this.object != null && object != null && !this.object.getClass().equals(object.getClass()));

		this.latLon = latLon;
		this.object = object;

		active = true;

		if (needAcquireMenuController) {
			acquireMenuController(restorePrevious);
		} else {
			menuController.update(pointDescription, object);
		}
		initTitle();

		if (menuController != null) {
			menuController.clearPlainMenuItems();
			menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
		}

		mainActivity.refreshMap();

		return true;
	}

	public void show() {
		if (!isVisible()) {
			if (!MapContextMenuFragment.showInstance(this, mainActivity, true)) {
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
			showInternal();
		}
	}

	private void showInternal() {
		if (!MapContextMenuFragment.showInstance(this, mainActivity, centerMarker)) {
			active = false;
		} else {
			mainActivity.showContextMarker(latLon);
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
			if (menuController != null) {
				menuController.onClose();
			}
			hide();
			if (menuController != null) {
				menuController.setActive(false);
			}
			mainActivity.hideContextMarker();
			mainActivity.refreshMap();
		}
	}

	public void hide() {
		if (menuController != null) {
			menuController.onHide();
		}
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismissMenu();
		}
	}

	// timeout in msec
	public void hideWithTimeout(long timeout) {
		autoHide = true;
		mainActivity.getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (autoHide) {
					hide();
				}
			}
		}, timeout);
	}

	public void updateMenuUI() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateMenu();
		}
	}

	private void updateTitle(String address) {
		nameStr = address;
		pointDescription.setName(address);
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().refreshTitle();
	}

	private void acquireMenuController(boolean restorePrevious) {
		if (menuController != null) {
			menuController.onAcquireNewController(pointDescription, object);
		}
		menuController = MenuController.getMenuController(mainActivity, latLon, pointDescription, object, MenuType.STANDARD);
		menuController.setActive(true);
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
		Fragment fragment = mainActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((MapContextMenuFragment) fragment);
		} else {
			return null;
		}
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
		ProgressDialog dlg = new ProgressDialog(mainActivity);
		dlg.setTitle("");
		dlg.setMessage(searchAddressStr);
		dlg.setButton(Dialog.BUTTON_NEGATIVE, mainActivity.getMyApplication().getString("shared_string_skip"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				cancelSearchAddress();
			}
		});
		return dlg;
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

	public MenuController.TitleButtonController getLeftTitleButtonController() {
		if (menuController != null) {
			return menuController.getLeftTitleButtonController();
		} else {
			return null;
		}
	}

	public MenuController.TitleButtonController getRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getRightTitleButtonController();
		} else {
			return null;
		}
	}

	public MenuController.TitleButtonController getTopRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getTopRightTitleButtonController();
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
			return false;
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
		mainActivity.runOnUiThread(new Runnable() {
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