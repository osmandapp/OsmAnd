package net.osmand.plus.importfiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;

import java.util.List;

public interface SaveImportedGpxListener {

	default void onGpxSavingStarted() {}

	default void onGpxSaved(@Nullable String error, @NonNull GpxFile gpxFile) {}

	default void onGpxSavingFinished(@NonNull List<String> warnings) {}
}