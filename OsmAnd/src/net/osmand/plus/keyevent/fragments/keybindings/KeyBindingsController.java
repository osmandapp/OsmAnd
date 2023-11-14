package net.osmand.plus.keyevent.fragments.keybindings;

import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.KEY_BINDING_ITEM;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.SPACE;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.fragments.EditKeyBindingFragment;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

class KeyBindingsController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDeviceHelper deviceHelper;
	private final InputDeviceProfile inputDevice;
	private FragmentActivity activity;

	public KeyBindingsController(@NonNull OsmandApplication app,
	                             @NonNull ApplicationMode appMode) {
		this.app = app;
		this.appMode = appMode;
		this.deviceHelper = app.getInputDeviceHelper();
		this.inputDevice = deviceHelper.getSelectedDevice(appMode);
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		if (inputDevice == null || inputDevice.getActionsCount() == 0) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		for (KeyEventCategory category : KeyEventCategory.values()) {
			List<KeyBinding> keyBindings = inputDevice.getKeyBindingsForCategory(category);
			if (Algorithms.isEmpty(keyBindings)) continue;

			String categoryName = app.getString(category.getTitleId());
			screenItems.add(new ScreenItem(CARD_DIVIDER, categoryName));
			screenItems.add(new ScreenItem(HEADER, categoryName));
			for (KeyBinding keyBinding : keyBindings) {
				screenItems.add(new ScreenItem(KEY_BINDING_ITEM, keyBinding));
			}
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	public boolean isDeviceEditable() {
		return inputDevice != null && deviceHelper.isCustomDevice(inputDevice);
	}

	public void askEditKeyAction(KeyBinding action) {
		if (inputDevice != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			EditKeyBindingFragment.showInstance(fm, appMode, action, inputDevice.getId());
		}
	}
}
