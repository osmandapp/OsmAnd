package net.osmand.plus.plugins.monitoring;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;

import java.util.List;
import java.util.Map;

public class SaveGpxResult {

	private final List<String> warnings;
	private final Map<String, GpxFile> gpxFilesByName;

	public SaveGpxResult(@NonNull List<String> warnings, @NonNull Map<String, GpxFile> gpxFilesByName) {
		this.warnings = warnings;
		this.gpxFilesByName = gpxFilesByName;
	}

	@NonNull
	public List<String> getWarnings() {
		return warnings;
	}

	@NonNull
	public Map<String, GpxFile> getGpxFilesByName() {
		return gpxFilesByName;
	}
}
