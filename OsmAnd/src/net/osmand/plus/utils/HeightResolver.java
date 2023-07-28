package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;

import java.util.Collections;

public class HeightResolver {

	public static void resolveHeight(@NonNull LatLon point,
	                                 @NonNull ResolveHeightCallback callback) {
		HeightsResolverTask task = new HeightsResolverTask(Collections.singletonList(point), heights -> {
			Float result = heights != null && heights.length > 0 ? heights[0] : null;
			callback.onHeightResolved(result);
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public interface ResolveHeightCallback {
		void onHeightResolved(@Nullable Float height);
	}

}
