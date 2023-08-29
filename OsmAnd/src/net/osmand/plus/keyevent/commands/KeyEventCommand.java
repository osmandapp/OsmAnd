package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;
import android.view.KeyEvent.Callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Objects;

public abstract class KeyEventCommand implements Callback {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected KeyEventHelper keyEventHelper;

	public void initialize(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.keyEventHelper = app.getKeyEventHelper();
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return Objects.requireNonNull(getMapActivity());
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return keyEventHelper.getMapActivity();
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
}
