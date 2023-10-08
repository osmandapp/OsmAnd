package net.osmand.plus.keyevent.ui.keybindings;

import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.ui.EditKeyActionFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScreenController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDeviceHelper deviceHelper;
	private FragmentActivity activity;

	public ScreenController(@NonNull OsmandApplication app,
	                        @NonNull ApplicationMode appMode) {
		this.app = app;
		this.appMode = appMode;
		this.deviceHelper = app.getInputDeviceHelper();
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		Map<KeyEventCategory, List<ActionItem>> categorizedActions = collectCategorizedActions();
		if (Algorithms.isEmpty(categorizedActions)) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		for (KeyEventCategory category : KeyEventCategory.values()) {
			List<ActionItem> actions = categorizedActions.get(category);
			if (actions != null) {
				String categoryName = app.getString(category.getTitleId());
				screenItems.add(new ScreenItem(ScreenItemType.CARD_DIVIDER, categoryName));
				screenItems.add(new ScreenItem(ScreenItemType.HEADER, categoryName));
				for (ActionItem action : actions) {
					screenItems.add(new ScreenItem(ScreenItemType.ACTION_ITEM, action));
				}
			}
		}
		screenItems.add(new ScreenItem(ScreenItemType.CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(ScreenItemType.SPACE));
		return screenItems;
	}

	@Nullable
	private Map<KeyEventCategory, List<ActionItem>> collectCategorizedActions() {
		InputDeviceProfile device = deviceHelper.getSelectedDevice(appMode);
		if (device == null) {
			return null;
		}
		Map<KeyEventCategory, List<ActionItem>> categorizedActions = new HashMap<>();
		ArrayMap<Integer, KeyEventCommand> mappedCommands = device.getMappedCommands();
		for (int i = 0; i < mappedCommands.size(); i++) {
			Integer keyCode = mappedCommands.keyAt(i);
			KeyEventCommand command = mappedCommands.valueAt(i);
			if (command != null) {
				KeyEventCategory category = command.getCategory();
				List<ActionItem> actions = categorizedActions.get(category);
				if (actions == null) {
					actions = new ArrayList<>();
					categorizedActions.put(category, actions);
				}
				actions.add(new ActionItem(keyCode, command));
			}
		}
		return categorizedActions;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	public boolean isEditableDeviceType() {
		InputDeviceProfile device = deviceHelper.getSelectedDevice(appMode);
		return device != null && deviceHelper.isCustomDevice(device);
	}

	public void askEditKeyAction(ActionItem action) {
		FragmentManager fm = activity.getSupportFragmentManager();
		EditKeyActionFragment.showInstance(fm, appMode, action);
	}
}
