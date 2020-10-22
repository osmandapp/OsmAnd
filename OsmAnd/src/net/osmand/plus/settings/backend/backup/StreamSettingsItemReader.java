package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

public abstract class StreamSettingsItemReader extends SettingsItemReader<StreamSettingsItem> {

	public StreamSettingsItemReader(@NonNull StreamSettingsItem item) {
		super(item);
	}
}
