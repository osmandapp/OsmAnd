package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.backup.items.StreamSettingsItem;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamSettingsItemWriter extends SettingsItemWriter<StreamSettingsItem> {

	public StreamSettingsItemWriter(StreamSettingsItem item) {
		super(item);
	}

	@Override
	public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
		InputStream inputStream = getItem().getInputStream();
		if (inputStream != null) {
			try {
				Algorithms.streamCopy(inputStream, outputStream, progress, 1024);
			} finally {
				Algorithms.closeStream(inputStream);
			}
		}
	}
}
