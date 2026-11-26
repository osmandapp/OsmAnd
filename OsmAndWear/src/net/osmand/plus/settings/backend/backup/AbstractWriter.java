package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;

public abstract class AbstractWriter {

	private boolean cancelled;

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		cancelled = true;
	}

	public abstract void write(@NonNull SettingsItem item) throws IOException;
}
