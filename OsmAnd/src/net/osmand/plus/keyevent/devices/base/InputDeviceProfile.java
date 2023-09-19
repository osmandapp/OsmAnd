package net.osmand.plus.keyevent.devices.base;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyEventCommandsFactory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.HashMap;
import java.util.Map;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	private KeyEventCommandsFactory commandsFactory;

	private final int id;
	private final int titleId;

	private StateChangedListener<Boolean> volumeButtonsPrefListener;
	private final Map<Integer, KeyEventCommand> mappedCommands = new HashMap<>();

	public InputDeviceProfile(int id, @StringRes int titleId) {
		this.id = id;
		this.titleId = titleId;
	}

	public int getId() {
		return id;
	}

	public void initialize(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.commandsFactory = app.getKeyEventHelper().getCommandsFactory();
		collectCommands();

		// Update commands when related preferences updated
		volumeButtonsPrefListener = aBoolean -> updateCommands();
		settings.USE_VOLUME_BUTTONS_AS_ZOOM.addListener(volumeButtonsPrefListener);
	}

	/**
	 * Override this method to add or update bindings between
	 * keycodes and commands for a specific input device profile.
	 */
	protected void collectCommands() {
		if (settings.USE_VOLUME_BUTTONS_AS_ZOOM.get()) {
			bindCommand(KeyEvent.KEYCODE_VOLUME_DOWN, MapZoomCommand.CONTINUOUS_ZOOM_OUT_ID);
			bindCommand(KeyEvent.KEYCODE_VOLUME_UP, MapZoomCommand.CONTINUOUS_ZOOM_IN_ID);
		}
	}

	protected void updateCommands() {
		clearCommands();
		collectCommands();
	}

	protected void clearCommands() {
		mappedCommands.clear();
	}

	public void requestBindCommand(int keyCode, @NonNull String commandId) {
		if (!mappedCommands.containsKey(keyCode)) {
			bindCommand(keyCode, commandId);
		}
	}

	protected void bindCommand(int keyCode, @NonNull String commandId) {
		KeyEventCommand command = commandsFactory.getOrCreateCommand(commandId);
		if (command != null) {
			command.initialize(app);
			mappedCommands.put(keyCode, command);
		}
	}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		return mappedCommands.get(keyCode);
	}

	@NonNull
	public String getTitle(@NonNull Context context) {
		return context.getString(titleId);
	}

	public final InputDeviceProfile newInstance(@NonNull OsmandApplication app) {
		InputDeviceProfile newInstance = newInstance();
		newInstance.initialize(app);
		return newInstance;
	}

	protected abstract InputDeviceProfile newInstance();

}
