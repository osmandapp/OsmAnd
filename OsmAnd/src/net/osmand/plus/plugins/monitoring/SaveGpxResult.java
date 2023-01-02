package net.osmand.plus.plugins.monitoring;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;

import java.util.List;
import java.util.Map;

public class SaveGpxResult {

	private final List<String> warnings;
	private final Map<String, GPXFile> gpxFilesByName;

	public SaveGpxResult(@NonNull List<String> warnings, @NonNull Map<String, GPXFile> gpxFilesByName) {
		this.warnings = warnings;
		this.gpxFilesByName = gpxFilesByName;
	}

	@NonNull
	public List<String> getWarnings() {
		return warnings;
	}

	@NonNull
	public Map<String, GPXFile> getGpxFilesByName() {
		return gpxFilesByName;
	}
}
