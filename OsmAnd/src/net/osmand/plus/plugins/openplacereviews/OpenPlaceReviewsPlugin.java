package net.osmand.plus.plugins.openplacereviews;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OPEN_PLACE_REVIEWS;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class OpenPlaceReviewsPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(OpenPlaceReviewsPlugin.class);

	public final OsmandPreference<String> OPR_ACCESS_TOKEN;
	public final OsmandPreference<String> OPR_USERNAME;
	public final OsmandPreference<String> OPR_BLOCKCHAIN_NAME;
	public final OsmandPreference<Boolean> OPR_USE_DEV_URL;

	private MapActivity mapActivity;

	public OpenPlaceReviewsPlugin(OsmandApplication app) {
		super(app);

		OPR_ACCESS_TOKEN = registerStringPreference("opr_user_access_token_secret", "").makeGlobal();
		OPR_USERNAME = registerStringPreference("opr_username_secret", "").makeGlobal();
		OPR_BLOCKCHAIN_NAME = registerStringPreference("opr_blockchain_name", "").makeGlobal();
		OPR_USE_DEV_URL = registerBooleanPreference("opr_use_dev_url", false).makeGlobal().makeShared();
	}

	@Override
	public String getId() {
		return PLUGIN_OPEN_PLACE_REVIEWS;
	}

	@Override
	public String getName() {
		return app.getString(R.string.open_place_reviews);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_opr);
		String description = app.getString(R.string.open_place_reviews_plugin_description, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(description, docsUrl) : description;
	}

	@Nullable
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
	public boolean isEnableByDefault() {
		return false;
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResumeOnTop(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		this.mapActivity = null;
	}

	@Override
	protected void collectContextMenuImageCards(@NonNull ImageCardsHolder holder,
	                                            @NonNull Map<String, String> params,
	                                            @Nullable Map<String, String> additionalParams,
	                                            @Nullable GetImageCardsListener listener) {
		if (mapActivity != null) {
			Object object = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObject();
			if (object instanceof Amenity) {
				Amenity am = (Amenity) object;
				long amenityId = am.getId() >> 1;
				String baseUrl = OPRConstants.getBaseUrl(app);
				String url = baseUrl + "api/objects-by-index?type=opr.place&index=osmid&key=" + amenityId;
				String response = AndroidNetworkUtils.sendRequest(app, url, Collections.emptyMap(),
						"Requesting location images...", false, false);
				if (response != null) {
					getPicturesForPlace(holder, response);
					if (listener != null) {
						listener.onPlaceIdAcquired(getIdFromResponse(response));
					}
				}
			}
		}
	}

	@Override
	protected boolean createContextMenuImageCard(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		ImageCard imageCard = null;
		if (mapActivity != null && imageObject != JSONObject.NULL) {
			imageCard = createCardOpr(mapActivity, imageObject);
		}
		return holder.add(ImageCardType.OPR, imageCard);
	}

	private void getPicturesForPlace(ImageCardsHolder holder, String response) {
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
										holder.add(ImageCardType.OPR, imageCard);
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
			if (obj.length() == 0) {
				return new String[0];
			}
			JSONArray images = (JSONArray) ((JSONObject) obj.get(0)).get("id");
			return toStringArray(images);
		} catch (JSONException e) {
			LOG.error(e.getMessage(), e);
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
	public void disable(@NonNull OsmandApplication app) {
		if (OPR_USE_DEV_URL.get()) {
			OPR_USE_DEV_URL.set(false);
			app.getOprAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}
}
