package net.osmand.plus.keyevent.devices;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.ActivityBackPressedCommand;
import net.osmand.plus.keyevent.commands.BackToLocationCommand;
import net.osmand.plus.keyevent.commands.EmitNavigationHintCommand;
import net.osmand.plus.keyevent.commands.MapScrollCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.commands.OpenNavigationDialogCommand;
import net.osmand.plus.keyevent.commands.OpenQuickSearchDialogCommand;
import net.osmand.plus.keyevent.commands.SwitchAppModeCommand;
import net.osmand.plus.keyevent.commands.SwitchCompassCommand;
import net.osmand.plus.keyevent.commands.ToggleDrawerCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.plugins.PluginsHelper;

import java.util.ArrayList;
import java.util.List;

public class KeyboardDeviceProfile extends PredefinedInputDeviceProfile {

	public static final String ID = "keyboard";

	@Override
	@NonNull
	protected List<KeyAssignment> collectAssignments() {
		List<KeyAssignment> list = new ArrayList<>();
		
		// Default letter keycodes
		addAssignment(list, BackToLocationCommand.ID, KeyEvent.KEYCODE_C);
		addAssignment(list, SwitchCompassCommand.ID, KeyEvent.KEYCODE_D);
		addAssignment(list, OpenNavigationDialogCommand.ID, KeyEvent.KEYCODE_N);
		addAssignment(list, OpenQuickSearchDialogCommand.ID, KeyEvent.KEYCODE_S);
		addAssignment(list, SwitchAppModeCommand.SWITCH_TO_NEXT_ID, KeyEvent.KEYCODE_P);
		addAssignment(list, SwitchAppModeCommand.SWITCH_TO_PREVIOUS_ID, KeyEvent.KEYCODE_O);

		// Default map scroll keycodes
		addAssignment(list, MapScrollCommand.SCROLL_UP_ID, KeyEvent.KEYCODE_DPAD_UP);
		addAssignment(list, MapScrollCommand.SCROLL_DOWN_ID, KeyEvent.KEYCODE_DPAD_DOWN);
		addAssignment(list, MapScrollCommand.SCROLL_LEFT_ID, KeyEvent.KEYCODE_DPAD_LEFT);
		addAssignment(list, MapScrollCommand.SCROLL_RIGHT_ID, KeyEvent.KEYCODE_DPAD_RIGHT);

		// Default map zoom keycodes
		addAssignment(list, MapZoomCommand.ZOOM_IN_ID, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS);
		addAssignment(list, MapZoomCommand.ZOOM_OUT_ID, KeyEvent.KEYCODE_MINUS);

		// Other default keycodes
		addAssignment(list, EmitNavigationHintCommand.ID, KeyEvent.KEYCODE_DPAD_CENTER);
		addAssignment(list, ToggleDrawerCommand.ID, KeyEvent.KEYCODE_M);
		addAssignment(list, ActivityBackPressedCommand.ID, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK);

		PluginsHelper.addCommonKeyEventAssignments(list);
		return list;
	}

	@NonNull
	@Override
	public String getId() {
		return ID;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.sett_generic_ext_input);
	}
}
