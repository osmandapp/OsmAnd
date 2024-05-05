package net.osmand.plus.myplaces.tracks.filters;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.plus.track.data.SmartFolder;

import org.apache.commons.logging.Log;

import java.lang.reflect.Type;
import java.util.List;

public class TrackFilterList {
	private static final Log LOG = PlatformUtil.getLog(SmartFolderHelper.class);

	@Nullable
	public static List<SmartFolder> parseFilters(String str) {
		Gson gson = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.registerTypeAdapter(BaseTrackFilter.class, new TrackFilterDeserializer())
				.create();

		Type token = new TypeToken<List<SmartFolder>>() {
		}.getType();
		List<SmartFolder> savedFilters = null;
		try {
			savedFilters = gson.fromJson(str, token);
		} catch (Throwable error) {
			error.printStackTrace();
			LOG.error(error.getMessage(), error);
		}
		return savedFilters;
	}
}
