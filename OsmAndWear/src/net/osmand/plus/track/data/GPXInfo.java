package net.osmand.plus.track.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxHelper;

import java.io.File;

public class GPXInfo {

	private final File file;
	private final String name;
	private final String fileName;
	private final long fileSize;
	private final long lastModified;

	private GpxFile gpxFile;
	private boolean selected;

	public GPXInfo(@NonNull String fileName, @Nullable File file) {
		this.file = file;
		this.fileName = fileName;
		this.name = GpxHelper.INSTANCE.getGpxTitle(fileName);
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

	@Nullable
	public String getFilePath() {
		return file != null ? file.getAbsolutePath() : null;
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
	public GpxFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(@Nullable GpxFile gpxFile) {
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
