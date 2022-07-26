package net.osmand.plus.activities;

import static net.osmand.plus.settings.backend.OsmandSettings.GENERIC_EXTERNAL_DEVICE;
import static net.osmand.plus.settings.backend.OsmandSettings.PARROT_EXTERNAL_DEVICE;
import static net.osmand.plus.settings.backend.OsmandSettings.WUNDERLINQ_EXTERNAL_DEVICE;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.helpers.ScrollHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;

public class MapActivityKeyListener implements KeyEvent.Callback {

	public static final int LONG_KEYPRESS_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 2;
	public static final int LONG_KEYPRESS_DELAY = 500;

	private final MapActivity mapActivity;
	private final OsmandApplication app;
	private final MapActivityActions mapActions;
	private final OsmandSettings settings;
	// handler to show/hide trackball position and to link map with delay
	private final Handler uiHandler = new Handler();
	private final ScrollHelper mapScrollHelper;

	public MapActivityKeyListener(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		mapActions = mapActivity.getMapActions();
		settings = app.getSettings();
		mapScrollHelper = mapActivity.getMapScrollHelper();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && app.accessibilityEnabled()) {
			if (!uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				Message msg = Message.obtain(uiHandler, new Runnable() {
					@Override
					public void run() {
						app.getLocationProvider().emitNavigationHint();
					}
				});
				msg.what = LONG_KEYPRESS_MSG_ID;
				uiHandler.sendMessageDelayed(msg, LONG_KEYPRESS_DELAY);
			}
			return true;
		} else if (mapScrollHelper.isAvailableKeyCode(keyCode)) {
			return mapScrollHelper.onKeyDown(keyCode);
		}

		if (settings.USE_VOLUME_BUTTONS_AS_ZOOM.get()) {
			OsmandMap osmandMap = app.getOsmandMap();
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				osmandMap.changeZoom(-1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				osmandMap.changeZoom(1);
				return true;
			}
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		OsmandMap osmandMap = app.getOsmandMap();
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			OsmandMapTileView mapView = mapActivity.getMapView();

			if (!app.accessibilityEnabled()) {
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			} else if (uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				uiHandler.removeMessages(LONG_KEYPRESS_MSG_ID);
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU /*&& event.getRepeatCount() == 0*/) {
			// repeat count 0 doesn't work for samsung, 1 doesn't work for lg
			mapActivity.toggleDrawer();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_C) {
			mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
		} else if (keyCode == KeyEvent.KEYCODE_D) {
			mapActivity.getMapViewTrackingUtilities().switchRotateMapMode();
		} else if (keyCode == KeyEvent.KEYCODE_MINUS) {
			osmandMap.changeZoom(-1);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS) {
			osmandMap.changeZoom(1);
			return true;
		} else if (mapScrollHelper.isAvailableKeyCode(keyCode)) {
			return mapScrollHelper.onKeyUp(keyCode);
		} else if (settings.EXTERNAL_INPUT_DEVICE.get() == PARROT_EXTERNAL_DEVICE) {
			// Parrot device has only dpad left and right
			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				osmandMap.changeZoom(-1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				osmandMap.changeZoom(1);
				return true;
			}
		} else if (settings.EXTERNAL_INPUT_DEVICE.get() == WUNDERLINQ_EXTERNAL_DEVICE) {
			// WunderLINQ device, motorcycle smart phone control
			if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				osmandMap.changeZoom(-1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
				osmandMap.changeZoom(1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
				String callingApp = "wunderlinq://datagrid";
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(callingApp));
				AndroidUtils.startActivityIfSafe(mapActivity, intent);
				return true;
			}
		} else if (settings.EXTERNAL_INPUT_DEVICE.get() == GENERIC_EXTERNAL_DEVICE) {
			// currently doesn't process specific commands
		} else if (OsmandPlugin.onMapActivityKeyUp(mapActivity, keyCode)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return false;
	}
}

