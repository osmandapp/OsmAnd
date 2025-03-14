package net.osmand.plus.mapcontextmenu.gallery.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LoadImagesMetadataTask extends AsyncTask<Void, Void, Map<String, Map<String, String>>> {

	private static final Log LOG = PlatformUtil.getLog(LoadImagesMetadataTask.class);

	private static final String OSMAND_PARSE_URL = "https://osmand.net/search/parse-images-list-info?";

	private final OsmandApplication app;
	private final Set<WikiImageCard> cards;
	private Map<String, Map<String, String>> resultMap = null;

	public LoadImagesMetadataTask(@NonNull OsmandApplication app, @NonNull Set<WikiImageCard> cards) {
		this.cards = cards;
		this.app = app;
	}

	public record WikiImageInfo(String title, Long pageId, String data) {
	}

	// parse-images-list-info api call
	@Override
	protected Map<String, Map<String, String>> doInBackground(Void... voids) {
		List<WikiImageInfo> data = getData();
		Gson gson = new Gson();
		String jsonData = gson.toJson(data);
		String url = "";

		String deviceId = app.getSettings().BACKUP_DEVICE_ID.get();
		String accessToken = app.getSettings().BACKUP_ACCESS_TOKEN.get();
		String lang;

		LocaleHelper localeHelper = app.getLocaleHelper();
		Locale preferredLocale = localeHelper.getPreferredLocale();
		Locale locale = preferredLocale != null ? preferredLocale : localeHelper.getDefaultLocale();
		lang = locale.getLanguage();

		try {
			if (!Algorithms.isEmpty(lang)) {
				url += OSMAND_PARSE_URL + "lang=" + URLEncoder.encode(lang, "UTF-8");
			}
			if (!Algorithms.isEmpty(deviceId)) {
				url += (url.isEmpty() ? OSMAND_PARSE_URL : "&") + "deviceId=" + URLEncoder.encode(deviceId, "UTF-8");
			}
			if (!Algorithms.isEmpty(accessToken)) {
				url += (url.isEmpty() ? OSMAND_PARSE_URL : "&") + "accessToken=" + URLEncoder.encode(accessToken, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		AndroidNetworkUtils.sendRequest(app, url, jsonData, null,
				"application/json", false, true, (result, error, resultCode) -> {
					if (!Algorithms.isEmpty(error)) {
						LOG.error(error);
					} else if (!Algorithms.isEmpty(result)) {
						try {
							Type mapType = new TypeToken<Map<String, Map<String, String>>>() {
							}.getType();
							resultMap = gson.fromJson(result, mapType);
						} catch (Exception e) {
							LOG.error(e);
						}
					} else {
						LOG.error("Empty metadata response");
					}
				});

		return resultMap;
	}

	private List<WikiImageInfo> getData() {
		List<WikiImageInfo> data = new ArrayList<>();
		for (WikiImageCard card : cards) {
			String title = card.getWikiImage().getWikiMediaTag();
			long pageId = card.getWikiImage().getMediaId();

			if (!Algorithms.isEmpty(title) && pageId != -1) {
				data.add(new WikiImageInfo(title, pageId, null));
			}
		}
		return data;
	}

	@Override
	protected void onPostExecute(Map<String, Map<String, String>> result) {
		GalleryController controller = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);
		if (controller != null) {
			controller.updateMetadata(result, cards);
		}
	}
}