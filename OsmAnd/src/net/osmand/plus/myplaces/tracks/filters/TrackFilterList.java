package net.osmand.plus.myplaces.tracks.filters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackFilterList extends ArrayList<BaseTrackFilter> {
	public static Map<String, List<BaseTrackFilter>> parseFilters(String str, SmartFolderHelper smartFolderHelper) {
		Gson gson = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.registerTypeAdapter(BaseTrackFilter.class, new TrackFilterDeserializer(smartFolderHelper))
				.create();

		Type token = new TypeToken<Map<String, List<BaseTrackFilter>>>() {
		}.getType();
		Map<String, List<BaseTrackFilter>> savedFilters = gson.fromJson(str, token);
		return savedFilters;
	}
}
