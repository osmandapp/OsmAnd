package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;

import java.io.File;

public class GpxDataItem {

	@NonNull
	private final File file;
	@NonNull
	private final GpxData gpxData = new GpxData();

	public GpxDataItem(@NonNull File file) {
		this.file = file;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public GpxData getGpxData() {
		return gpxData;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GpxDataItem)) {
			return false;
		}
		GpxDataItem other = (GpxDataItem) obj;
		return file.equals(other.file);
	}
}
