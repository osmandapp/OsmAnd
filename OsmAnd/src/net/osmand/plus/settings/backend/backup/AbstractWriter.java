package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;

public interface AbstractWriter {
	void write(@NonNull SettingsItem item) throws IOException;
}
