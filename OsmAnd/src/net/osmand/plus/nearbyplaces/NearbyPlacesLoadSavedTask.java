package net.osmand.plus.nearbyplaces;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.FileUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class NearbyPlacesLoadSavedTask extends AsyncTask<Void, Void, List<NearbyPlacePoint>> {

	private static final Log LOG = PlatformUtil.getLog(NearbyPlacesLoadSavedTask.class.getName());

	private final OsmandApplication app;
	private File file;

	public NearbyPlacesLoadSavedTask(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	protected List<NearbyPlacePoint> doInBackground(Void... voids) {
		file = new File(FileUtils.getTempDir(app), "nearby_places");
		return loadPlaces();
	}

	@NonNull
	private List<NearbyPlacePoint> loadPlaces() {
		List<NearbyPlacePoint> nearbyPlaces = Collections.emptyList();
		Gson gson = new Gson();
		try (FileReader reader = new FileReader(file)) {
			Type type = new TypeToken<List<NearbyPlacePoint>>() {
			}.getType();
			nearbyPlaces = gson.fromJson(reader, type);
		} catch (Exception e) {
			LOG.error("Error loading nearby places from file", e);
		}
		return nearbyPlaces;
	}

	@Override
	protected void onPostExecute(@NonNull List<NearbyPlacePoint> nearbyPlaces) {
		NearbyPlacesHelper.INSTANCE.onCacheLoaded(nearbyPlaces);
	}
}