package net.osmand.plus.importfiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;

import java.util.List;

public interface SaveImportedGpxListener {

	void onGpxSavingStarted();

	void onGpxSaved(@Nullable String error, @NonNull GPXFile gpxFile);

	void onGpxSavingFinished(@NonNull List<String> warnings);
}