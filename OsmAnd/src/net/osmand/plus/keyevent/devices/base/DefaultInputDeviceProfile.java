package net.osmand.plus.keyevent.devices.base;

import android.view.KeyEvent;

import androidx.annotation.StringRes;

import net.osmand.plus.keyevent.commands.BackToLocationCommand;
import net.osmand.plus.keyevent.commands.EmitNavigationHintCommand;
import net.osmand.plus.keyevent.commands.MapScrollCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.commands.OpenQuickSearchDialogCommand;
import net.osmand.plus.keyevent.commands.OpenNavigationDialogCommand;
import net.osmand.plus.keyevent.commands.SwitchAppModeCommand;
import net.osmand.plus.keyevent.commands.SwitchCompassCommand;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.keyevent.commands.ToggleDrawerCommand;

public abstract class DefaultInputDeviceProfile extends InputDeviceProfile {

	public DefaultInputDeviceProfile(int id, @StringRes int titleId) {
		super(id, titleId);
	}

	/**
	 * Collects default bindings, which are common for all device profiles.
	 * Some types of devices may not support some of the keycodes.
	 */
	@Override
	protected void collectCommands() {
		super.collectCommands();

		// Default map zoom keycodes
		bindCommand(KeyEvent.KEYCODE_PLUS, MapZoomCommand.ZOOM_IN_ID);
		bindCommand(KeyEvent.KEYCODE_EQUALS, MapZoomCommand.ZOOM_IN_ID);
		bindCommand(KeyEvent.KEYCODE_MINUS, MapZoomCommand.ZOOM_OUT_ID);

		// Default map scroll keycodes
		bindCommand(KeyEvent.KEYCODE_DPAD_UP, MapScrollCommand.SCROLL_UP_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_DOWN, MapScrollCommand.SCROLL_DOWN_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_LEFT, MapScrollCommand.SCROLL_LEFT_ID);
		bindCommand(KeyEvent.KEYCODE_DPAD_RIGHT, MapScrollCommand.SCROLL_RIGHT_ID);

		// Default letter keycodes
		bindCommand(KeyEvent.KEYCODE_C, BackToLocationCommand.ID);
		bindCommand(KeyEvent.KEYCODE_D, SwitchCompassCommand.ID);
		bindCommand(KeyEvent.KEYCODE_N, OpenNavigationDialogCommand.ID);
		bindCommand(KeyEvent.KEYCODE_O, SwitchAppModeCommand.SWITCH_TO_PREVIOUS_ID);
		bindCommand(KeyEvent.KEYCODE_P, SwitchAppModeCommand.SWITCH_TO_NEXT_ID);
		bindCommand(KeyEvent.KEYCODE_S, OpenQuickSearchDialogCommand.ID);

		// Other default keycodes
		bindCommand(KeyEvent.KEYCODE_DPAD_CENTER, EmitNavigationHintCommand.ID);
		bindCommand(KeyEvent.KEYCODE_MENU, ToggleDrawerCommand.ID);

		PluginsHelper.bindCommonKeyEventCommands(this);
	}
}
