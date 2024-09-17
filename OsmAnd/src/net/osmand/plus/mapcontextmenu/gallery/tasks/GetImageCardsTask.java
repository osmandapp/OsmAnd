package net.osmand.plus.mapcontextmenu.gallery.tasks;

import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.MAPILLARY_AMENITY;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType.WIKIMEDIA;
import static net.osmand.plus.plugins.mapillary.MapillaryPlugin.TYPE_MAPILLARY_CONTRIBUTE;
import static net.osmand.plus.plugins.mapillary.MapillaryPlugin.TYPE_MAPILLARY_PHOTO;

import android.net.TrafficStats;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.UrlImageCard;
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryImageCard;
import net.osmand.plus.plugins.mapillary.MapillaryOsmTagHelper;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiImage;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetImageCardsTask extends AsyncTask<Void, Void, ImageCardsHolder> {
	private static final Log LOG = PlatformUtil.getLog(GetImageCardsTask.class);

	private final MapActivity mapActivity;
	private final OsmandApplication app;
	private final LatLon latLon;
	private final Map<String, String> params;
	private final GetImageCardsListener listener;
	private static final int GET_IMAGE_CARD_THREAD_ID = 10104;

	public GetImageCardsTask(@NonNull MapActivity mapActivity, LatLon latLon,
	                         @Nullable Map<String, String> params, @Nullable GetImageCardsListener listener) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.latLon = latLon;
		this.params = params;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onTaskStarted();
		}
	}

	@Override
	protected ImageCardsHolder doInBackground(Void... voids) {
		TrafficStats.setThreadStatsTag(GET_IMAGE_CARD_THREAD_ID);
		ImageCardsHolder holder = new ImageCardsHolder();
		try {
			Map<String, String> httpPms = new LinkedHashMap<>();
			httpPms.put("lat", "" + (float) latLon.getLatitude());
			httpPms.put("lon", "" + (float) latLon.getLongitude());
			Location myLocation = app.getLocationProvider().getLastKnownLocation();
			if (myLocation != null) {
				httpPms.put("mloc", "" + (float) myLocation.getLatitude() + "," + (float) myLocation.getLongitude());
			}
			httpPms.put("app", Version.isPaidVersion(app) ? "paid" : "free");
			String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			if (Algorithms.isEmpty(preferredLang)) {
				preferredLang = app.getLanguage();
			}
			if (!Algorithms.isEmpty(preferredLang)) {
				httpPms.put("lang", preferredLang);
			}
			List<WikiImage> wikimediaImageList = WikiCoreHelper.getWikiImageList(params);
			for (WikiImage wikiImage : wikimediaImageList) {
				holder.add(WIKIMEDIA, new WikiImageCard(mapActivity, wikiImage));
			}

			if (!Algorithms.isEmpty(params.get(Amenity.MAPILLARY))) {
				JSONObject imageObject = MapillaryOsmTagHelper.getImageByKey(params.get(Amenity.MAPILLARY));
				if (imageObject != null) {
					holder.add(MAPILLARY_AMENITY, new MapillaryImageCard(mapActivity, imageObject));
				}
			}
			PluginsHelper.populateContextMenuImageCards(holder, httpPms, params, listener);
			String response = AndroidNetworkUtils.sendRequest(app, "https://osmand.net/api/cm_place", httpPms,
					"Requesting location images...", false, false);
			if (!Algorithms.isEmpty(response)) {
				JSONObject obj = new JSONObject(response);
				JSONArray images = obj.getJSONArray("features");
				if (images.length() > 0) {
					for (int i = 0; i < images.length(); i++) {
						try {
							JSONObject imageObject = (JSONObject) images.get(i);
							if (imageObject != JSONObject.NULL) {
								if (!PluginsHelper.createImageCardForJson(holder, imageObject)) {
									ImageCard imageCard = createCard(mapActivity, imageObject);
									if (imageCard != null) {
										holder.add(ImageCard.ImageCardType.OTHER, imageCard);
									}
								}
							}
						} catch (JSONException e) {
							LOG.error(e);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}

		return holder;
	}

	@Override
	protected void onPostExecute(ImageCardsHolder cardList) {
		if (listener != null) {
			listener.onFinish(cardList);
		}
	}
	private static ImageCard createCard(MapActivity mapActivity, JSONObject imageObject) {
		ImageCard imageCard = null;
		try {
			if (imageObject.has("type")) {
				String type = imageObject.getString("type");
				if (!TYPE_MAPILLARY_CONTRIBUTE.equals(type) && !TYPE_MAPILLARY_PHOTO.equals(type)) {
					imageCard = new UrlImageCard(mapActivity, imageObject);
				}
			}
		} catch (JSONException e) {
			LOG.error(e);
		}
		return imageCard;
	}

	public interface GetImageCardsListener {
		void onTaskStarted();
		void onFinish(ImageCardsHolder cardsHolder);
	}
}