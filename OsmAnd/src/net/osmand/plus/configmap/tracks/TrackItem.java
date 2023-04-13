package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;

public class TrackItem {

	private final String name;
	private final String path;
	@Nullable
	private final File file;
	private final long lastModified;
	private final boolean showCurrentTrack;

	private GpxDataItem dataItem;

	public TrackItem(@NonNull File file) {
		this.file = file;
		this.path = file.getAbsolutePath();
		this.name = GpxUiHelper.getGpxTitle(file.getName());
		this.lastModified = file.lastModified();
		this.showCurrentTrack = false;
	}

	public TrackItem(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		this.showCurrentTrack = gpxFile.showCurrentTrack;
		if (showCurrentTrack) {
			this.file = null;
			this.path = gpxFile.path;
			this.name = app.getString(R.string.shared_string_currently_recording_track);
			this.lastModified = gpxFile.modifiedTime;
		} else {
			this.file = new File(gpxFile.path);
			this.path = file.getAbsolutePath();
			this.name = GpxUiHelper.getGpxTitle(file.getName());
			this.lastModified = file.lastModified();
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
		return showCurrentTrack;
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}
