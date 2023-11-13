package net.osmand.plus.importfiles;

import net.osmand.gpx.GPXFile;

public interface GpxImportListener {

	default void onImportStarted() {}

	default void onImportFinished() {}

	default void onImportComplete(boolean success) {}

	default void onSaveComplete(boolean success, GPXFile gpxFile) {}
}
