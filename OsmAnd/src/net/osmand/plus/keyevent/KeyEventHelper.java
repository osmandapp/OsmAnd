package net.osmand.plus.keyevent;

import android.view.KeyEvent;
import android.view.KeyEvent.Callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.HashMap;
import java.util.Map;

public class KeyEventHelper implements KeyEvent.Callback, InputDevicesEventListener {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final InputDevicesHelper deviceHelper;
	private MapActivity mapActivity;

	private final Map<Integer, QuickAction> globalActions = new HashMap<>();

	private StateChangedListener<Boolean> volumeButtonsPrefListener;
	private KeyEvent.Callback externalCallback;

	public KeyEventHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		deviceHelper = app.getInputDeviceHelper();
		deviceHelper.addListener(this);

		// Update commands when related preferences updated
		volumeButtonsPrefListener = aBoolean -> updateGlobalCommands();
		settings.USE_VOLUME_BUTTONS_AS_ZOOM.addListener(volumeButtonsPrefListener);
		updateGlobalCommands();
	}

	public void updateGlobalCommands() {
		globalActions.clear();
		if (settings.USE_VOLUME_BUTTONS_AS_ZOOM.get()) {
			bindCommand(KeyEvent.KEYCODE_VOLUME_DOWN, MapZoomCommand.CONTINUOUS_ZOOM_OUT_ID);
			bindCommand(KeyEvent.KEYCODE_VOLUME_UP, MapZoomCommand.CONTINUOUS_ZOOM_IN_ID);
		}
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	/**
	 * Sets an external callback to process key events in another place with a custom algorithm.
	 */
	public void setExternalCallback(@Nullable Callback externalCallback) {
		this.externalCallback = externalCallback;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyDown(keyCode, event);
		}
		QuickAction action = findAction(keyCode);
		if (action != null && action.onKeyDown(mapActivity, keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyLongPress(keyCode, event);
		}
		QuickAction action = findAction(keyCode);
		return action != null && action.onKeyLongPress(mapActivity, keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (settings.SHOW_INFO_ABOUT_PRESSED_KEY.get()) {
			showToastAboutPressedKey(event);
		}
		if (externalCallback != null) {
			return externalCallback.onKeyUp(keyCode, event);
		}
		QuickAction action = findAction(keyCode);
		if (action != null && action.onKeyUp(mapActivity, keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyMultiple(keyCode, count, event);
		}
		QuickAction action = findAction(keyCode);
		return action != null && action.onKeyMultiple(mapActivity, keyCode, count, event);
	}

	@Override
	public void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		// If custom preference for current app mode was updated,
		// We need to reload device from preferences to use it with actual customizations.
		if (deviceHelper.getFunctionalityAppMode() == appMode && event.isCustomPreferenceRelated()) {
			deviceHelper.reloadFunctionalityCollection(appMode);
		}
	}

	private void showToastAboutPressedKey(@NonNull KeyEvent keyEvent) {
		int keyCode = keyEvent.getKeyCode();
		int deviceId = keyEvent.getDeviceId();
		String keyLabel = KeySymbolMapper.getKeySymbol(app, keyCode);
		app.showShortToastMessage("Device id: " + deviceId + ", key code: " + keyCode + ", label: \"" + keyLabel + "\"");
	}

	@Nullable
	private QuickAction findAction(int keyCode) {
		if (mapActivity == null || isLetterKeyCode(keyCode) && !mapActivity.isMapVisible()) {
			// Reject using of letter keycodes when the focus isn't on the Activity
			return null;
		}
		// Search action in global bound commands
		QuickAction globalAction = globalActions.get(keyCode);
		if (globalAction != null) {
			return globalAction;
		}
		// Search action for current input device profile
		ApplicationMode appMode = settings.getApplicationMode();
		InputDeviceProfile device = deviceHelper.getFunctionalityDevice(appMode);
		return device != null ? device.findAction(keyCode) : null;
	}

	private void bindCommand(int keyCode, @NonNull String commandId) {
		QuickAction action = CommandToActionConverter.createQuickAction(commandId);
		if (action != null) {
			globalActions.put(keyCode, action);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	private static boolean isLetterKeyCode(int keyCode) {
		return keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
	}
}

