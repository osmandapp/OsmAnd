package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.MAPILLARY;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmenityMenuBuilder extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);

	private final Amenity amenity;
	private AmenityUIHelper rowsBuilder;

	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, @NonNull Amenity amenity) {
		super(mapActivity);
		this.amenity = amenity;
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	@Override
	public void buildInternal(View view) {
		AmenityExtensionsHelper extensionsHelper = new AmenityExtensionsHelper(app);
		Map<String, String> additionalInfo = extensionsHelper.getAmenityExtensions(amenity);

		rowsBuilder = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), additionalInfo);
		rowsBuilder.setLight(light);
		rowsBuilder.setLatLon(getLatLon());
		rowsBuilder.buildInternal(view);

		buildNearestRows((ViewGroup) view);
	}

	private void buildNearestRows(ViewGroup viewGroup) {
		buildNearestWiki(viewGroup);
		buildNearestPoi(viewGroup);
	}

	private void buildNearestWiki(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestWikiRow(viewGroup, new SearchAmenitiesListener() {
			@Override
			public void onFinish(List<Amenity> amenities) {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null || Algorithms.isEmpty(amenities)) {
					return;
				}
				String title = app.getString(R.string.wiki_around);
				String count = "(" + amenities.size() + ")";
				String text = app.getString(R.string.ltr_or_rtl_combine_via_space, title, count);

				Context context = viewGroup.getContext();
				AmenityInfoRow wikiInfo = new AmenityInfoRow(
						NEAREST_WIKI_KEY, R.drawable.ic_plugin_wikipedia, null, text,
						null, true, getCollapsableView(context, true, amenities, NEAREST_WIKI_KEY),
						0, false, false, false, 1000, null, false, false, false, 0);

				View amenitiesRow = createRowContainer(context, NEAREST_WIKI_KEY);

				int insertIndex = position == 0 ? 0 : position + 1;

				firstRow = insertIndex == 0 || isDividerAtPosition(viewGroup, insertIndex - 1);
				rowsBuilder.buildAmenityRow(amenitiesRow, wikiInfo);
				viewGroup.addView(amenitiesRow, insertIndex);

				buildNearestRowDividerIfMissing(viewGroup, insertIndex);
			}
		});
	}

	private void buildNearestPoi(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestPoiRow(new SearchAmenitiesListener() {
			@Override
			public void onFinish(List<Amenity> amenities) {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null) {
					return;
				}
				String title = app.getString(R.string.speak_poi);
				String type = "\"" + AmenityMenuController.getTypeStr(amenity) + "\"";
				String count = "(" + amenities.size() + ")";
				String text = app.getString(R.string.ltr_or_rtl_triple_combine_via_space, title, type, count);

				Context context = viewGroup.getContext();
				AmenityInfoRow poiInfo = new AmenityInfoRow(
						NEAREST_POI_KEY, AmenityMenuController.getRightIconId(amenity), null, text,
						null, true, getCollapsableView(context, true, amenities, NEAREST_POI_KEY),
						0, false, false, false, 1000, null, false, false, false, 0);

				View wikiRow = viewGroup.findViewWithTag(NEAREST_WIKI_KEY);
				int insertIndex = wikiRow != null
						? viewGroup.indexOfChild(wikiRow) + 1
						: position == 0 ? 0 : position + 1;

				View amenitiesRow = createRowContainer(context, NEAREST_POI_KEY);
				firstRow = insertIndex == 0 || isDividerAtPosition(viewGroup, insertIndex - 1);
				rowsBuilder.buildAmenityRow(amenitiesRow, poiInfo);
				viewGroup.addView(amenitiesRow, insertIndex);

				buildNearestRowDividerIfMissing(viewGroup, insertIndex);
			}
		});
	}

	@Override
	protected Map<String, String> getAdditionalCardParams() {
		Map<String, String> params = new HashMap<>();
		String imageValue = amenity.getAdditionalInfo("image");
		String mapillaryValue = amenity.getAdditionalInfo(MAPILLARY);
		String wikidataValue = amenity.getAdditionalInfo(Amenity.WIKIDATA);
		String wikimediaValue = amenity.getAdditionalInfo(Amenity.WIKIMEDIA_COMMONS);
		if (!Algorithms.isEmpty(imageValue)) {
			params.put("osm_image", getDecodedAdditionalInfo(imageValue));
		}
		if (!Algorithms.isEmpty(mapillaryValue)) {
			params.put(MAPILLARY, getDecodedAdditionalInfo(mapillaryValue));
		}
		if (!Algorithms.isEmpty(wikidataValue)) {
			params.put(Amenity.WIKIDATA, getDecodedAdditionalInfo(wikidataValue));
		}
		if (!Algorithms.isEmpty(wikimediaValue)) {
			params.put(Amenity.WIKIMEDIA_COMMONS, getDecodedAdditionalInfo(wikimediaValue));
		}
		return params;
	}

	private String getDecodedAdditionalInfo(String additionalInfo) {
		try {
			return URLDecoder.decode(additionalInfo, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error(e);
		}
		return additionalInfo;
	}
}