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
		int bytesDivisor = 1024;
		StreamSettingsItem item = getItem();
		if (progress != null) {
			progress.startWork((int) (item.getSize() / bytesDivisor));
		}
		InputStream inputStream = item.getInputStream();
		if (inputStream != null) {
			try {
				Algorithms.streamCopy(inputStream, outputStream, progress, bytesDivisor);
			} finally {
				Algorithms.closeStream(inputStream);
			}
		}
		if (progress != null) {
			progress.finishTask();
		}
	}
}
