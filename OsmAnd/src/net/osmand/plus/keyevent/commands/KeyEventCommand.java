package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;
import android.view.KeyEvent.Callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.KeyEventCategory;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Objects;

public abstract class KeyEventCommand implements Callback {

	protected String commandId;
	protected OsmandApplication app;
	protected OsmandSettings settings;

	public void initialize(@NonNull OsmandApplication app, @NonNull String commandId) {
		this.app = app;
		this.settings = app.getSettings();
		this.commandId = commandId;
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return Objects.requireNonNull(getMapActivity());
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return app.getKeyEventHelper().getMapActivity();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return false;
	}

	@NonNull
	public String getId() {
		return commandId;
	}

	@NonNull
	public KeyEventCategory getCategory() {
		return KeyEventCategory.ACTIONS;
	}

	@NonNull
	public abstract String toHumanString(@NonNull Context context);
}
