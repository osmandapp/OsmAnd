package net.osmand.plus.routing;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MissingMapsOnlineSearchTask extends AsyncTask<Void, Void, List<WorldRegion>> {

	private static final Log LOG = PlatformUtil.getLog(MissingMapsOnlineSearchTask.class);

	private final RouteCalculationParams params;
	private final OnlineSearchMissingMapsListener listener;

	public interface OnlineSearchMissingMapsListener {
		void onMissingMapsOnlineSearchComplete(@NonNull List<WorldRegion> missingMaps);
	}

	public MissingMapsOnlineSearchTask(@NonNull RouteCalculationParams params,
									   @Nullable OnlineSearchMissingMapsListener listener) {
		this.params = params;
		this.listener = listener;
	}

	@Override
	protected List<WorldRegion> doInBackground(Void... voids) {
		try {
			MissingMapsHelper missingMapsHelper = new MissingMapsHelper(params);
			List<Location> onlinePoints = missingMapsHelper.findOnlineRoutePoints();
			return missingMapsHelper.getMissingMaps(onlinePoints);
		} catch (IOException | JSONException e) {
			LOG.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	@Override
	protected void onPostExecute(@NonNull List<WorldRegion> worldRegions) {
		if (listener != null) {
			listener.onMissingMapsOnlineSearchComplete(worldRegions);
		}
	}
}
