package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface AbstractWriter {
	void write(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter) throws IOException;
}
