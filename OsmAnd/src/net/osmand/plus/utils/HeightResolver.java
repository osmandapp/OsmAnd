package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.OnResultCallback;
import net.osmand.data.LatLon;

import java.util.Collections;

public class HeightResolver {

	public static void resolveHeight(@NonNull LatLon point,
	                                 @NonNull OnResultCallback<Float> callback) {
		HeightsResolverTask task = new HeightsResolverTask(Collections.singletonList(point), heights -> {
			Float result = heights != null && heights.length > 0 ? heights[0] : null;
			callback.onResult(result);
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

}
