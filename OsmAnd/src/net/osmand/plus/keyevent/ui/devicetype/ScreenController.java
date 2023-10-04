package net.osmand.plus.keyevent.ui.devicetype;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

class ScreenController {

	private final ApplicationMode appMode;
	private final InputDeviceHelper deviceHelper;

	public ScreenController(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		this.appMode = appMode;
		this.deviceHelper = app.getInputDeviceHelper();
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(ScreenItemType.CARD_DIVIDER));
		for (InputDeviceProfile device : deviceHelper.getAvailableDevices()) {
			screenItems.add(new ScreenItem(ScreenItemType.DEVICE_ITEM, device));
		}
		screenItems.add(new ScreenItem(ScreenItemType.CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(ScreenItemType.SPACE));
		return screenItems;
	}

	public void selectDevice(@NonNull InputDeviceProfile device) {
		deviceHelper.selectInputDevice(appMode, device.getId());
	}

	public void askRenameDevice(@NonNull InputDeviceProfile device) {

	}

	public void duplicateDevice(@NonNull InputDeviceProfile device) {

	}

	public void removeDevice(@NonNull InputDeviceProfile device) {

	}

	public boolean isSelected(@NonNull InputDeviceProfile device) {
		return deviceHelper.isSelectedDevice(appMode, device.getId());
	}

	public boolean isCustom(@NonNull InputDeviceProfile device) {
		return deviceHelper.isCustomDevice(device);
	}
}
