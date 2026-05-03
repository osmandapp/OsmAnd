package net.osmand.plus.mapcontextmenu.gallery.tasks;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryMediaGroup.MAPILLARY_AMENITY;
import static net.osmand.plus.mapcontextmenu.gallery.GalleryMediaGroup.OTHER;
import static net.osmand.plus.mapcontextmenu.gallery.GalleryMediaGroup.WIKIMEDIA;

import android.net.TrafficStats;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.mapcontextmenu.gallery.GalleryItemsHolder;
import net.osmand.plus.mapcontextmenu.gallery.RemoteMediaFactory;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryOsmTagHelper;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaOrigin;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.util.Algorithms;
import net.osmand.shared.wiki.WikiCoreHelper;
import net.osmand.shared.wiki.WikiCoreHelper.NetworkResponseListener;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetOnlineImagesTask extends AsyncTask<Void, Void, GalleryItemsHolder> {
	private static final Log LOG = PlatformUtil.getLog(GetOnlineImagesTask.class);

	private static final int GET_IMAGE_CARD_THREAD_ID = 10104;

	private final OsmandApplication app;

	private final LatLon latLon;
	private final Map<String, String> params;
	private final GetImageCardsListener imageCardsListener;
	private final NetworkResponseListener networkResponseListener;

	public GetOnlineImagesTask(@NonNull OsmandApplication app, @NonNull LatLon latLon,
	                           @Nullable Map<String, String> params,
	                           @Nullable GetImageCardsListener imageCardsListener,
	                           @NonNull NetworkResponseListener networkResponseListener) {
		this.app = app;
		this.latLon = latLon;
		this.params = params;
		this.imageCardsListener = imageCardsListener;
		this.networkResponseListener = networkResponseListener;
	}

	@Override
	protected void onPreExecute() {
		if (imageCardsListener != null) {
			imageCardsListener.onTaskStarted();
		}
	}

	@Override
	protected GalleryItemsHolder doInBackground(Void... voids) {
		TrafficStats.setThreadStatsTag(GET_IMAGE_CARD_THREAD_ID);
		GalleryItemsHolder holder = new GalleryItemsHolder(latLon, params);
		try {
			Map<String, String> httpPms = new LinkedHashMap<>();
			httpPms.put("lat", String.valueOf((float) latLon.getLatitude()));
			httpPms.put("lon", String.valueOf((float) latLon.getLongitude()));
			Location myLocation = app.getLocationProvider().getLastKnownLocation();
			if (myLocation != null) {
				httpPms.put("mloc", (float) myLocation.getLatitude() + "," + (float) myLocation.getLongitude());
			}
			httpPms.put("app", Version.isPaidVersion(app) ? "paid" : "free");
			String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			if (Algorithms.isEmpty(preferredLang)) {
				preferredLang = app.getLanguage();
			}
			if (!Algorithms.isEmpty(preferredLang)) {
				httpPms.put("lang", preferredLang);
			}

			List<WikiImage> wikimediaImageList = WikiCoreHelper.INSTANCE.getWikiImageList(params, networkResponseListener);
			for (WikiImage wikiImage : wikimediaImageList) {
				holder.addMediaItem(WIKIMEDIA, RemoteMediaFactory.fromWikiImage(wikiImage));
			}
			String key = params.remove(Amenity.MAPILLARY);
			if (!Algorithms.isEmpty(key)) {
				JSONObject imageObject = MapillaryOsmTagHelper.getImageByKey(key);
				if (imageObject != null) {
					MediaItem item = RemoteMediaFactory.fromJson(imageObject, MediaOrigin.MAPILLARY);
					if (item != null) {
						holder.addMediaItem(MAPILLARY_AMENITY, item);
					}
				}
			}
			httpPms.putAll(params);
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
								if (!PluginsHelper.addContextMenuGalleryItem(holder, imageObject)) {
									MediaItem.Remote mediaItem = RemoteMediaFactory.fromUrlImageJson(imageObject);
									if (mediaItem != null) {
										holder.addMediaItem(OTHER, mediaItem);
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
	protected void onPostExecute(GalleryItemsHolder holder) {
		if (imageCardsListener != null) {
			imageCardsListener.onFinish(holder);
		}
	}

	public interface GetImageCardsListener {
		void onTaskStarted();

		void onFinish(GalleryItemsHolder cardsHolder);
	}
}