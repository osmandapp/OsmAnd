package net.osmand.plus.keyevent.fragments.inputdevices;

import static net.osmand.plus.keyevent.fragments.inputdevices.InputDevicesAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.inputdevices.InputDevicesAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.inputdevices.InputDevicesAdapter.DEVICE_ITEM;
import static net.osmand.plus.keyevent.fragments.inputdevices.InputDevicesAdapter.SPACE;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.devices.CustomInputDeviceProfile;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class InputDevicesController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final boolean usedOnMap;

	public InputDevicesController(@NonNull OsmandApplication app,
	                              @NonNull ApplicationMode appMode, boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.deviceHelper = app.getInputDeviceHelper();
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(CARD_DIVIDER));
		for (InputDeviceProfile device : deviceHelper.getAllDevices(appMode)) {
			screenItems.add(new ScreenItem(DEVICE_ITEM, device));
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void selectDevice(@NonNull InputDeviceProfile device) {
		deviceHelper.selectInputDevice(appMode, device.getId());
	}

	public void askAddNewCustomDevice() {
		String title = app.getString(R.string.add_new_type);
		showEnterNameDialog(title, "", newName -> {
			deviceHelper.createAndSaveCustomDevice(appMode, newName);
			return true;
		});
	}

	public void askRenameDevice(@NonNull InputDeviceProfile device) {
		String title = app.getString(R.string.shared_string_rename);
		showEnterNameDialog(title, device.toHumanString(app), newName -> {
			if (device instanceof CustomInputDeviceProfile) {
				deviceHelper.renameCustomDevice(appMode, device.getId(), newName);
			}
			return true;
		});
	}

	private void showEnterNameDialog(@NonNull String title, @NonNull String oldName,
	                                 @NonNull CallbackWithObject<String> callback) {
		FragmentActivity activity = app.getKeyEventHelper().getMapActivity();
		if (activity == null) return;

		boolean nightMode = isNightMode();
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(title)
				.setControlsColor(ColorUtilities.getActiveColor(activity, nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null);
		dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				EditText editText = (EditText) extra;
				String newName = editText.getText().toString();
				if (Algorithms.isBlank(newName)) {
					app.showToastMessage(R.string.empty_name);
				} else {
					if (deviceHelper.hasDeviceNameDuplicate(app, appMode, newName)) {
						app.showToastMessage(R.string.message_name_is_already_exists);
					} else {
						callback.processResult(newName.trim());
					}
				}
			}
		});
		String caption = activity.getString(R.string.shared_string_name);
		CustomAlert.showInput(dialogData, activity, oldName, caption);
	}

	public void duplicateDevice(@NonNull InputDeviceProfile device) {
		deviceHelper.createAndSaveDeviceDuplicate(appMode, device);
	}

	public void askRemoveDevice(@NonNull InputDeviceProfile device) {
		FragmentActivity activity = app.getKeyEventHelper().getMapActivity();
		if (activity == null) return;

		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.shared_string_remove)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButtonTextColor(ColorUtilities.getColor(app, R.color.color_warning))
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					deviceHelper.removeCustomDevice(appMode, device.getId());
				});
		String typeName = device.toHumanString(app);
		String message = app.getString(R.string.remove_type_q, typeName);
		CustomAlert.showSimpleMessage(dialogData, message);
	}

	public boolean isSelected(@NonNull InputDeviceProfile device) {
		InputDeviceProfile selectedDevice = deviceHelper.getSelectedDevice(appMode);
		return Objects.equals(selectedDevice.getId(), device.getId());
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
