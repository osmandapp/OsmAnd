package net.osmand.plus.keyevent.fragments.keybindings;

import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.KEY_BINDING_ITEM;
import static net.osmand.plus.keyevent.fragments.keybindings.KeyBindingsAdapter.SPACE;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.fragments.EditKeyBindingFragment;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

class KeyBindingsController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDeviceHelper deviceHelper;
	private final InputDeviceProfile inputDevice;
	private FragmentActivity activity;
	private final boolean usedOnMap;

	public KeyBindingsController(@NonNull OsmandApplication app,
	                             @NonNull ApplicationMode appMode,
	                             boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.deviceHelper = app.getInputDeviceHelper();
		this.inputDevice = deviceHelper.getSelectedDevice(appMode);
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		if (inputDevice == null || inputDevice.getKeyBindingsCount() == 0) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		for (KeyEventCategory category : KeyEventCategory.values()) {
			List<KeyBinding> keyBindings = inputDevice.getKeyBindingsForCategory(category);
			if (Algorithms.isEmpty(keyBindings)) continue;

			String categoryName = app.getString(category.getTitleId());
			screenItems.add(new ScreenItem(category.ordinal() > 0 ? CARD_DIVIDER : CARD_TOP_DIVIDER, categoryName));
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

	public void askRemoveAllKeyBindings() {
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.reset_key_assignments)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_reset_all, (dialog, which) -> {
					deviceHelper.resetAllKeyBindings(appMode, inputDevice.getId());
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.reset_key_assignments_desc);
	}

	public boolean isDeviceTypeEditable() {
		return inputDevice != null && deviceHelper.isCustomDevice(inputDevice);
	}

	public void askEditKeyAction(KeyBinding action) {
		if (inputDevice != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			EditKeyBindingFragment.showInstance(fm, appMode, action, inputDevice.getId());
		}
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
