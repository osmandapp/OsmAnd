package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamSettingsItemWriter extends SettingsItemWriter<StreamSettingsItem> {

	public StreamSettingsItemWriter(StreamSettingsItem item) {
		super(item);
	}

	@Override
	public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
		boolean hasData = false;
		InputStream is = getItem().getInputStream();
		if (is != null) {
			byte[] data = new byte[SettingsHelper.BUFFER];
			int count;
			while ((count = is.read(data, 0, SettingsHelper.BUFFER)) != -1) {
				outputStream.write(data, 0, count);
				if (!hasData) {
					hasData = true;
				}
			}
			Algorithms.closeStream(is);
		}
		return hasData;
	}
}
