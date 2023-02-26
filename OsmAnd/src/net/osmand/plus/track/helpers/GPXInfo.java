package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;

public class GPXInfo {

	private final File file;
	private final String name;
	private final String fileName;
	private final long fileSize;
	private final long lastModified;

	private GPXFile gpxFile;
	private GpxDataItem dataItem;
	private boolean selected;

	public String subfolder;

	public GPXInfo(@NonNull String fileName, @Nullable File file) {
		this.file = file;
		this.fileName = fileName;
		this.name = GpxUiHelper.getGpxTitle(fileName);
		this.fileSize = file != null ? file.length() + 512 : 0;
		this.lastModified = file != null ? file.lastModified() : 0;
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getFileSize() {
		return fileSize;
	}

	@Nullable
	public File getFile() {
		return file;
	}

	public boolean isCurrentRecordingTrack() {
		return gpxFile != null && gpxFile.showCurrentTrack;
	}

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(@Nullable GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	@Nullable
	public GpxDataItem getDataItem() {
		return dataItem;
	}

	public void setDataItem(@Nullable GpxDataItem dataItem) {
		this.dataItem = dataItem;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@NonNull
	@Override
	public String toString() {
		return fileName;
	}
}
