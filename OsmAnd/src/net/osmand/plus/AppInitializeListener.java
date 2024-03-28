package net.osmand.plus;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

public interface AppInitializeListener {

	@WorkerThread
	default void onStart(@NonNull AppInitializer init) {

	}

	@UiThread
	default void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {

	}

	@UiThread
	default void onFinish(@NonNull AppInitializer init) {

	}
}