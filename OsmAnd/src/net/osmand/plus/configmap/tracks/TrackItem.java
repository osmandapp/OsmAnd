package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;

public class TrackItem {

	private final String name;
	private final File file;
	private final GPXFile gpxFile;

	private GpxDataItem dataItem;
	private WptPt nearestPoint;

	private long lastModified;
	private long creationTime;

	public TrackItem(@NonNull File file, @NonNull GPXFile gpxFile) {
		this.file = file;
		this.gpxFile = gpxFile;
		this.name = GpxUiHelper.getGpxTitle(file.getName());
		this.lastModified = file.lastModified();
		this.creationTime = gpxFile.getCreationTime();
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	@Nullable
	public GpxDataItem getDataItem() {
		return dataItem;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setDataItem(@Nullable GpxDataItem dataItem) {
		this.dataItem = dataItem;
	}

	public long getCreationTime() {
		return creationTime;
	}

	@Nullable
	public WptPt getNearestPoint() {
		return nearestPoint;
	}

	public void setNearestPoint(@Nullable WptPt wptPt) {
		this.nearestPoint = wptPt;
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}
