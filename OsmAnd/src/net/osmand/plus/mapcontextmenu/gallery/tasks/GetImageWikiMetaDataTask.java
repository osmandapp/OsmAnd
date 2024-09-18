package net.osmand.plus.mapcontextmenu.gallery.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class GetImageWikiMetaDataTask extends AsyncTask<Void, Void, Void> {
	private static final Log LOG = PlatformUtil.getLog(GetImageWikiMetaDataTask.class);
	private static final String WIKI_MEDIA_BASE_URL = "https://commons.wikimedia.org/wiki/File:";
	private static final String WIKI_MEDIA_ACTION_RAW = "?action=raw";
	private static final String OSMAND_PARSE_URL = "https://osmand.net/routing/search/parse-image-info";

	private final OsmandApplication app;
	private final WikiImageCard wikiImageCard;
	private final GetImageWikiMetaDataListener listener;

	public GetImageWikiMetaDataTask(@NonNull OsmandApplication app, @NonNull WikiImageCard wikiImageCard, @Nullable GetImageWikiMetaDataListener listener) {
		this.wikiImageCard = wikiImageCard;
		this.listener = listener;
		this.app = app;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		String wikiMediaUrl = WIKI_MEDIA_BASE_URL + wikiImageCard.wikiImage.getWikiMediaTag() + WIKI_MEDIA_ACTION_RAW;

		StringBuilder rawResponse = new StringBuilder();
		String errorMessage = NetworkUtils.sendGetRequest(wikiMediaUrl, null, rawResponse);
		if (!Algorithms.isEmpty(errorMessage)) {
			LOG.error(errorMessage);
		}
		String data = rawResponse.toString();

		if (!Algorithms.isEmpty(data)) {
			try {
				AndroidNetworkUtils.sendRequest(app, OSMAND_PARSE_URL, data, null,
						"application/json", false, true, (result, error, resultCode) -> {
					try {
						JSONObject object = new JSONObject(result);
						if (Algorithms.isEmpty(wikiImageCard.author)) {
							wikiImageCard.author = object.getString("author");
						}
						if (Algorithms.isEmpty(wikiImageCard.date)) {
							wikiImageCard.date = object.getString("date");
						}
						if (Algorithms.isEmpty(wikiImageCard.license)) {
							wikiImageCard.license = object.getString("license");
						}
						wikiImageCard.setWikiMediaMataDataDownloaded(true);
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (Throwable t) {
				LOG.error(t);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.onFinish(wikiImageCard);
		}
	}

	public interface GetImageWikiMetaDataListener {
		void onFinish(@NonNull WikiImageCard wikiImageCard);
	}
}