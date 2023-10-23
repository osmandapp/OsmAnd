package net.osmand.plus.keyevent.fragments.keybindings;

import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.SPACE;

import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.fragments.EditKeyActionFragment;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map<KeyEventCategory, List<KeyBinding>> categorizedActions = collectCategorizedActions();
		if (Algorithms.isEmpty(categorizedActions)) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		for (KeyEventCategory category : KeyEventCategory.values()) {
			List<KeyBinding> actions = categorizedActions.get(category);
			if (actions != null) {
				String categoryName = app.getString(category.getTitleId());
				screenItems.add(new ScreenItem(CARD_DIVIDER, categoryName));
				screenItems.add(new ScreenItem(HEADER, categoryName));
				for (KeyBinding action : actions) {
					screenItems.add(new ScreenItem(ACTION_ITEM, action));
				}
			}
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	@NonNull
	private Map<KeyEventCategory, List<KeyBinding>> collectCategorizedActions() {
		if (inputDevice == null) {
			return new HashMap<>();
		}
		Map<KeyEventCategory, List<KeyBinding>> categorizedActions = new HashMap<>();
		ArrayMap<Integer, KeyEventCommand> mappedCommands = inputDevice.getMappedCommands();
		for (int i = 0; i < mappedCommands.size(); i++) {
			Integer keyCode = mappedCommands.keyAt(i);
			KeyEventCommand command = mappedCommands.valueAt(i);
			if (command != null) {
				KeyEventCategory category = command.getCategory();
				List<KeyBinding> actions = categorizedActions.get(category);
				if (actions == null) {
					actions = new ArrayList<>();
					categorizedActions.put(category, actions);
				}
				actions.add(new KeyBinding(keyCode, command));
			}
		}
		return categorizedActions;
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
			EditKeyActionFragment.showInstance(fm, appMode, action, inputDevice.getId());
		}
	}
}
