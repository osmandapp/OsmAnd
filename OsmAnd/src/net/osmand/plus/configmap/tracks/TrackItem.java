package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackItem {

	private final String name;
	private final String path;
	@Nullable
	private final File file;
	private final long lastModified;

	private GpxDataItem dataItem;

	public TrackItem(@NonNull File file) {
		this.file = file;
		path = file.getAbsolutePath();
		name = GpxUiHelper.getGpxTitle(file.getName());
		lastModified = file.lastModified();
	}

	public TrackItem(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		if (gpxFile.showCurrentTrack) {
			file = null;
			path = gpxFile.path;
			name = app.getString(R.string.shared_string_currently_recording_track);
			lastModified = gpxFile.modifiedTime;
		} else {
			file = new File(gpxFile.path);
			path = file.getAbsolutePath();
			name = GpxUiHelper.getGpxTitle(file.getName());
			lastModified = file.lastModified();
		}
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	@Nullable
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

	public boolean isShowCurrentTrack() {
		return file == null;
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		TrackItem trackItem = (TrackItem) obj;
		return Algorithms.objectEquals(file, trackItem.file);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(file);
	}
}
