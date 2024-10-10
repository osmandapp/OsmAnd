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
import net.osmand.wiki.Metadata;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadImageMetadataTask extends AsyncTask<Void, Void, Void> {

	private static final Log LOG = PlatformUtil.getLog(LoadImageMetadataTask.class);

	private static final String WIKI_MEDIA_ACTION_RAW = "?action=raw";
	private static final String WIKI_MEDIA_BASE_URL = "https://commons.wikimedia.org/wiki/File:";
	private static final String OSMAND_PARSE_URL = "https://osmand.net/routing/search/parse-image-info";

	private final OsmandApplication app;
	private final WikiImageCard imageCard;
	private final GetImageWikiMetaDataListener listener;

	public LoadImageMetadataTask(@NonNull OsmandApplication app, @NonNull WikiImageCard imageCard,
	                             @Nullable GetImageWikiMetaDataListener listener) {
		this.imageCard = imageCard;
		this.listener = listener;
		this.app = app;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		String wikiMediaUrl = WIKI_MEDIA_BASE_URL + imageCard.getWikiImage().getWikiMediaTag() + WIKI_MEDIA_ACTION_RAW;

		StringBuilder builder = new StringBuilder();
		String error = NetworkUtils.sendGetRequest(wikiMediaUrl, null, builder);
		if (Algorithms.isEmpty(error)) {
			String data = builder.toString();
			if (!Algorithms.isEmpty(data)) {
				requestWikiDataParsing(data);
			}
		} else {
			LOG.error(error);
		}

		return null;
	}

	private void requestWikiDataParsing(@NonNull String data) {
		AndroidNetworkUtils.sendRequest(app, OSMAND_PARSE_URL, data, null,
				"application/json", false, true, (result, error, resultCode) -> {
					if (!Algorithms.isEmpty(error)) {
						LOG.error(error);
					} else if (!Algorithms.isEmpty(result)) {
						try {
							JSONObject object = new JSONObject(result);
							Metadata metadata = imageCard.getWikiImage().getMetadata();
							if (Algorithms.isEmpty(metadata.getAuthor())) {
								metadata.setAuthor(object.getString("author"));
							}
							if (Algorithms.isEmpty(metadata.getDate())) {
								metadata.setDate(object.getString("date"));
							}
							if (Algorithms.isEmpty(metadata.getLicense())) {
								metadata.setLicense(object.getString("license"));
							}
							imageCard.setMetaDataDownloaded(true);
						} catch (JSONException e) {
							LOG.error(e);
						}
					} else {
						LOG.error("Empty metadata response");
					}
				});
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.onFinish(imageCard);
		}
	}

	public interface GetImageWikiMetaDataListener {
		void onFinish(@NonNull WikiImageCard wikiImageCard);
	}
}