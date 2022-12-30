package net.osmand.plus.download;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemporaryFilesRegistry {

	private final OsmandApplication app;
	private ListStringPreference cachedFilesPreference;

	public TemporaryFilesRegistry(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void deleteTemporaryFilesAtStartup() {
		cachedFilesPreference = app.getSettings().FILES_WITH_TEMPORARY_EXTENSION;
		deleteTemporaryFiles();
	}

	private void deleteTemporaryFiles() {
		for (File file : collectTemporaryFiles()) {
			if (file.exists()) {
				Algorithms.removeAllFiles(file);
			}
			remove(file);
		}
		clear();
	}

	public void add(@NonNull File file) {
		String path = getRelativePath(file);
		cachedFilesPreference.addValue(path);
	}

	public void remove(@NonNull File file) {
		String path = getRelativePath(file);
		cachedFilesPreference.removeValue(path);
	}

	private void clear() {
		cachedFilesPreference.resetToDefault();
	}

	@NonNull
	private List<File> collectTemporaryFiles() {
		List<File> tempFiles = new ArrayList<>();
		tempFiles.add(getTempDir());
		tempFiles.addAll(getCachedTempFiles());
		return tempFiles;
	}

	@NonNull
	private File getTempDir() {
		return getAppPath(IndexConstants.TEMP_DIR);
	}

	@NonNull
	private List<File> getCachedTempFiles() {
		String listAsString = cachedFilesPreference.get();
		List<String> paths = cachedFilesPreference.getStringsList();
		if (Algorithms.isEmpty(listAsString) || Algorithms.isEmpty(paths)) {
			paths = Collections.emptyList();
		}
		List<File> files = new ArrayList<>();
		for (String path : paths) {
			files.add(getAppPath(path));
		}
		return files;
	}

	@NonNull
	private String getRelativePath(@NonNull File file) {
		URI appDirUri = getAppPath("").toURI();
		URI fileUri = file.toURI();
		return appDirUri.relativize(fileUri).getPath();
	}

	@NonNull
	private File getAppPath(@NonNull String path) {
		return app.getAppPath(path);
	}

}
