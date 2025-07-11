package net.osmand.plus.resources;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Map;

@SuppressWarnings("deprecation")
public class DetectRegionTask extends AsyncTask<LatLon, Void, WorldRegion> {

	private static final Log log = PlatformUtil.getLog(DetectRegionTask.class);

	private final OsmandApplication app;
	private final CallbackWithObject<WorldRegion> callback;

	public DetectRegionTask(@NonNull OsmandApplication app, @NonNull CallbackWithObject<WorldRegion> callback) {
		this.app = app;
		this.callback = callback;
	}

	@Override
	protected WorldRegion doInBackground(LatLon... latLons) {
		try {
			if (latLons != null && latLons.length > 0) {
				Map.Entry<WorldRegion, BinaryMapDataObject> reg = app.getRegions()
						.getSmallestBinaryMapDataObjectAt(latLons[0]);
				if (reg != null) {
					return reg.getKey();
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	@Override
	protected void onPostExecute(WorldRegion worldRegion) {
		callback.processResult(worldRegion);
	}
}