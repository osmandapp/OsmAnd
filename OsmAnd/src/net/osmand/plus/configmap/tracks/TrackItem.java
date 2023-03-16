package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackItem {

	private final String name;
	private final String path;
	private final File file;
	private final long lastModified;

	private GpxDataItem dataItem;
	private String regionName;

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

	@Nullable
	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(@Nullable String regionName) {
		this.regionName = regionName;
	}

	public long getLastModified() {
		return lastModified;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackItem other = (TrackItem) obj;
		return Algorithms.objectEquals(file, other.file);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(file);
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}
