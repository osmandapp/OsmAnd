package net.osmand.plus.settings.datastorage;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface StorageMigrationListener {

	void onFileCopyStarted(@NonNull String path);

	void onFilesCopyProgress(int progress);

	void onFilesCopyFinished(@NonNull Map<String, Pair<String, Long>> errors, @NonNull List<File> existingFiles);

	void onRemainingFilesUpdate(@NonNull Pair<Integer, Long> pair);

}
