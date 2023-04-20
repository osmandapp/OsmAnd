package net.osmand.plus.track.data;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPXFolderInfo {

	private final List<GPXFolderInfo> folders = new ArrayList<>();
	private final List<GPXInfo> files = new ArrayList<>();

	private final File path;

	public GPXFolderInfo(@NonNull File path) {
		this.path = path;
	}

	@NonNull
	public File getPath() {
		return path;
	}

	public void addFolder(@NonNull GPXFolderInfo folder) {
		folders.add(folder);
	}

	public void addFile(@NonNull GPXInfo gpxInfo) {
		files.add(gpxInfo);
	}

	@ColorInt
	public int getColor() {
		return Color.parseColor("#F52887"); // todo use real folder color
	}

	public int getTotalTracksCount() {
		int total = files.size();
		for (GPXFolderInfo folder : folders) {
			total += folder.getTotalTracksCount();
		}
		return total;
	}

	public long getLastModified() {
		long lastUpdateTime = 0;
		for (GPXFolderInfo folder : folders) {
			long folderLastUpdate = folder.getLastModified();
			lastUpdateTime = Math.max(lastUpdateTime, folderLastUpdate);
		}
		for (GPXInfo file : files) {
			long fileLastUpdate = file.getLastModified();
			lastUpdateTime = Math.max(lastUpdateTime, fileLastUpdate);
		}
		return lastUpdateTime;
	}

	@NonNull
	public String getName() {
		return Algorithms.capitalizeFirstLetter(path.getName());
	}
}
