package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.helpers.MapScrollHelper.ScrollDirection;
import net.osmand.plus.keyevent.commands.ActivityBackPressedCommand;
import net.osmand.plus.keyevent.commands.BackToLocationCommand;
import net.osmand.plus.keyevent.commands.EmitNavigationHintCommand;
import net.osmand.plus.keyevent.commands.MapScrollCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.commands.OpenNavigationDialogCommand;
import net.osmand.plus.keyevent.commands.OpenQuickSearchDialogCommand;
import net.osmand.plus.keyevent.commands.SwitchAppModeCommand;
import net.osmand.plus.keyevent.commands.SwitchCompassCommand;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.keyevent.commands.ToggleDrawerCommand;
import net.osmand.plus.keyevent.commands.OpenWunderLINQDatagridCommand;
import net.osmand.plus.keyevent.commands.KeyEventCommand;

class KeyEventCommandsFactory {

	@Nullable
	public KeyEventCommand createCommand(@NonNull String commandId) {
		switch (commandId) {
			// Map scroll
			case MapScrollCommand.SCROLL_UP_ID:
				return new MapScrollCommand(ScrollDirection.UP);
			case MapScrollCommand.SCROLL_DOWN_ID:
				return new MapScrollCommand(ScrollDirection.DOWN);
			case MapScrollCommand.SCROLL_LEFT_ID:
				return new MapScrollCommand(ScrollDirection.LEFT);
			case MapScrollCommand.SCROLL_RIGHT_ID:
				return new MapScrollCommand(ScrollDirection.RIGHT);

			// Map zoom
			case MapZoomCommand.ZOOM_IN_ID:
				return new MapZoomCommand(false, true);
			case MapZoomCommand.ZOOM_OUT_ID:
				return new MapZoomCommand(false, false);
			case MapZoomCommand.CONTINUOUS_ZOOM_IN_ID:
				return new MapZoomCommand(true, true);
			case MapZoomCommand.CONTINUOUS_ZOOM_OUT_ID:
				return new MapZoomCommand(true, false);

			// Dialogs and UI
			case ActivityBackPressedCommand.ID:
				return new ActivityBackPressedCommand();
			case ToggleDrawerCommand.ID:
				return new ToggleDrawerCommand();
			case OpenQuickSearchDialogCommand.ID:
				return new OpenQuickSearchDialogCommand();
			case OpenNavigationDialogCommand.ID:
				return new OpenNavigationDialogCommand();

			// Other
			case BackToLocationCommand.ID:
				return new BackToLocationCommand();
			case SwitchAppModeCommand.SWITCH_TO_NEXT_ID:
				return new SwitchAppModeCommand(true);
			case SwitchAppModeCommand.SWITCH_TO_PREVIOUS_ID:
				return new SwitchAppModeCommand(false);
			case SwitchCompassCommand.ID:
				return new SwitchCompassCommand();
			case EmitNavigationHintCommand.ID:
				return new EmitNavigationHintCommand();
			case OpenWunderLINQDatagridCommand.ID:
				return new OpenWunderLINQDatagridCommand();
		}
		return PluginsHelper.createKeyEventCommand(commandId);
	}

}
