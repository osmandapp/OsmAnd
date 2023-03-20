package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;

import java.io.File;

public class GPXInfo {

	private final File file;
	private final String name;
	private final String fileName;
	private final long fileSize;
	private final long lastModified;

	private GPXFile gpxFile;
	private boolean selected;

	public String subfolder;

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

	/**
	 * @return a file size increased by 512 bytes,
	 * which allows for proper rounding of small files less than 1KB in size.
	 * Without this rounding, the file size may be displayed as 0KB, which is inaccurate.
	 */
	public long getIncreasedFileSize() {
		return fileSize > 0 ? fileSize + 512 : 0;
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
