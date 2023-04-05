package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;

public class TrackItem {

	private final String name;
	private final String path;
	private final File file;
	private final long lastModified;

	private GpxDataItem dataItem;

	public TrackItem(@NonNull File file) {
		this.file = file;
		this.path = file.getAbsolutePath();
		this.name = GpxUiHelper.getGpxTitle(file.getName());
		this.lastModified = file.lastModified();
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@Nullable
	public GpxDataItem getDataItem() {
		return dataItem;
	}

	public void setDataItem(@Nullable GpxDataItem dataItem) {
		this.dataItem = dataItem;
	}

	public long getLastModified() {
		return lastModified;
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}
