package net.osmand.plus.track.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;

public class GPXInfo {

	private final File file;
	private final String name;
	private final String fileName;
	private final long fileSize;
	private final long lastModified;

	private GPXFile gpxFile;
	private boolean selected;

	public GPXInfo(@NonNull String fileName, @Nullable File file) {
		this.file = file;
		this.fileName = fileName;
		this.name = GpxUiHelper.getGpxTitle(fileName);
		this.fileSize = file != null ? file.length() : 0;
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

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(@Nullable GPXFile gpxFile) {
		this.gpxFile = gpxFile;
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
