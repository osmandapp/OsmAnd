package net.osmand.plus.openplacereviews;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidNetworkUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OpenPlaceReviewsPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(OpenPlaceReviewsPlugin.class);

	public static final String ID = "osmand.openplacereviews";

	private MapActivity mapActivity;

	public OpenPlaceReviewsPlugin(OsmandApplication app) {
		super(app);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return app.getString(R.string.open_place_reviews);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.open_place_reviews_plugin_description);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.OPEN_PLACE_REVIEWS;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_img_logo_openplacereview;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.img_plugin_openplacereviews);
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResumeOnTop(MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		this.mapActivity = null;
	}

	@Override
	protected List<ImageCard> getContextMenuImageCards(@NonNull Map<String, String> params,
													   @Nullable Map<String, String> additionalParams,
													   @Nullable GetImageCardsListener listener) {
		List<ImageCard> imageCards = new ArrayList<>();
		if (mapActivity != null) {
			Object object = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObject();
			if (object instanceof Amenity) {
				Amenity am = (Amenity) object;
				long amenityId = am.getId() >> 1;
				String baseUrl = OPRConstants.getBaseUrl(app);
				String url = baseUrl + "api/objects-by-index?type=opr.place&index=osmid&key=" + amenityId;
				String response = AndroidNetworkUtils.sendRequest(app, url, Collections.<String, String>emptyMap(),
						"Requesting location images...", false, false);
				if (response != null) {
					getPicturesForPlace(imageCards, response);
					if (listener != null) {
						listener.onPlaceIdAcquired(getIdFromResponse(response));
					}
				}
			}
		}
		return imageCards;
	}

	@Override
	protected ImageCard createContextMenuImageCard(@NonNull JSONObject imageObject) {
		ImageCard imageCard = null;
		if (mapActivity != null && imageObject != JSONObject.NULL) {
			imageCard = createCardOpr(mapActivity, imageObject);
		}
		return imageCard;
	}

	private void getPicturesForPlace(List<ImageCard> result, String response) {
		try {
			if (!Algorithms.isEmpty(response)) {
				JSONArray obj = new JSONObject(response).getJSONArray("objects");
				JSONObject imagesWrapper = ((JSONObject) ((JSONObject) obj.get(0)).get("images"));
				Iterator<String> it = imagesWrapper.keys();
				while (it.hasNext()) {
					JSONArray images = imagesWrapper.getJSONArray(it.next());
					if (images.length() > 0) {
						for (int i = 0; i < images.length(); i++) {
							try {
								JSONObject imageObject = (JSONObject) images.get(i);
								if (imageObject != JSONObject.NULL) {
									ImageCard imageCard = createCardOpr(mapActivity, imageObject);
									if (imageCard != null) {
										result.add(imageCard);
									}
								}
							} catch (JSONException e) {
								LOG.error(e);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public static ImageCard createCardOpr(MapActivity mapActivity, JSONObject imageObject) {
		ImageCard imageCard = null;
		if (imageObject.has("cid")) {
			imageCard = new IPFSImageCard(mapActivity, imageObject);
		}
		return imageCard;
	}

	private static String[] getIdFromResponse(String response) {
		try {
			JSONArray obj = new JSONObject(response).getJSONArray("objects");
			JSONArray images = (JSONArray) ((JSONObject) obj.get(0)).get("id");
			return toStringArray(images);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new String[0];
	}

	private static String[] toStringArray(JSONArray array) {
		if (array == null)
			return null;

		String[] arr = new String[array.length()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = array.optString(i);
		}
		return arr;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		if (activity instanceof MapActivity) {
			mapActivity = (MapActivity) activity;
		}
		return true;
	}

	@Override
	public void disable(OsmandApplication app) {
		if (app.getSettings().OPR_USE_DEV_URL.get()) {
			app.getSettings().OPR_USE_DEV_URL.set(false);
			app.getOprAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}
}
