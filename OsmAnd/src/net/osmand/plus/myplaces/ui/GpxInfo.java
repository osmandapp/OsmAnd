package net.osmand.plus.myplaces.ui;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.helpers.GpxUiHelper;

import java.io.File;

public class GpxInfo {
	public boolean currentlyRecordingTrack;
	public GPXFile gpx;
	public File file;
	public String subfolder;

	private String name;
	private int sz = -1;
	private String fileName;
	private boolean corrupted;

	public GpxInfo() {
	}

	public GpxInfo(GPXFile file, String name) {
		this.gpx = file;
		this.name = name;
	}

	public String getName() {
		if (name == null) {
			name = GpxUiHelper.getGpxTitle(file.getName());
		}
		return name;
	}

	public boolean isCorrupted() {
		return corrupted;
	}

	public int getSize() {
		if (sz == -1) {
			if (file == null) {
				return -1;
			}
			sz = (int) ((file.length() + 512));
		}
		return sz;
	}

	public long getFileDate() {
		if (file == null) {
			return 0;
		}
		return file.lastModified();
	}

	public void setGpx(GPXFile gpx) {
		this.gpx = gpx;
	}

	@NonNull
	public String getFileName() {
		if (fileName != null) {
			return fileName;
		} else if (file == null) {
			return "";
		} else {
			fileName = file.getName();
			return fileName;
		}
	}
}
