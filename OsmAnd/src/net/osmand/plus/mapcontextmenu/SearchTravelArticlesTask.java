package net.osmand.plus.mapcontextmenu;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchTravelArticlesTask extends AsyncTask<Void, Void, Map<String, Map<String, TravelArticle>>> {

	private final TravelHelper travelHelper;
	private final Map<String, LatLon> routeIds;
	private final CallbackWithObject<Map<String, Map<String, TravelArticle>>> callback;

	public SearchTravelArticlesTask(@NonNull OsmandApplication app,
			@NonNull Map<String, LatLon> routeIds,
			@Nullable CallbackWithObject<Map<String, Map<String, TravelArticle>>> callback) {
		this.travelHelper = app.getTravelHelper();
		this.routeIds = routeIds;
		this.callback = callback;
	}

	@Override
	protected Map<String, Map<String, TravelArticle>> doInBackground(Void... params) {
		Map<String, Map<String, TravelArticle>> result = new LinkedHashMap<>();
		for (Map.Entry<String, LatLon> entry : routeIds.entrySet()) {
			String routeId = entry.getKey();
			LatLon latLon = entry.getValue();

			TravelArticleIdentifier identifier = new TravelArticleIdentifier(null,
					latLon.getLatitude(), latLon.getLongitude(), null, routeId, null);

			Map<String, TravelArticle> map = travelHelper.getArticleByLangs(identifier);
			if (!Algorithms.isEmpty(map)) {
				result.put(routeId, map);
			}
		}
		return result;
	}

	@Override
	protected void onPostExecute(Map<String, Map<String, TravelArticle>> articles) {
		if (callback != null) {
			callback.processResult(articles);
		}
	}
}