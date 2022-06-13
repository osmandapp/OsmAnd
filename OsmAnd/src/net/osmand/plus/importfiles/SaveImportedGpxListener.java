package net.osmand.plus.importfiles;

import java.util.List;

import androidx.annotation.NonNull;

public interface SaveImportedGpxListener {

	void onGpxSavingStarted();

	void onGpxSavingFinished(@NonNull List<String> warnings);
}