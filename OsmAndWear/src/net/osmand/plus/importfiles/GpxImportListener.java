package net.osmand.plus.importfiles;

import net.osmand.shared.gpx.GpxFile;

public interface GpxImportListener {

	default void onImportStarted() {
	}

	default void onImportFinished() {
	}

	default void onImportComplete(boolean success) {
	}

	default void onSaveComplete(boolean success, GpxFile gpxFile) {
	}
}
