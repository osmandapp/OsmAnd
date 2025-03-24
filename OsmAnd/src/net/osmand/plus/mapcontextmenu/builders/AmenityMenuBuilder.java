package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_POI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_WIKI_KEY;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.WPT_EXTRA_TAGS;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Map;

public class AmenityMenuBuilder extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);

	protected Amenity amenity;
	private AmenityUIHelper rowsBuilder;
	protected Map<String, String> additionalInfo;

	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, @NonNull Amenity amenity) {
		super(mapActivity);
		this.amenity = amenity;
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
		additionalInfo = amenity.getAmenityExtensions(app.getPoiTypes(), false);
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	protected void buildMainImage(View view) {
		if (amenity.getWikiImageStubUrl() != null) {
			AppCompatImageView imageView = inflateAndGetMainImageView(view);
			PicassoUtils.setupImageViewByUrl(app, imageView, amenity.getWikiImageStubUrl(), false);
		}
	}

	@Override
	public void buildInternal(View view) {
		if (amenity.isRoutePoint()) {
			processRoutePointAmenityTags(view);
		}

		rowsBuilder = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), additionalInfo);
		rowsBuilder.setLight(isLightContent());
		rowsBuilder.setLatLon(getLatLon());
		rowsBuilder.setCollapseExpandListener(getCollapseExpandListener());
		rowsBuilder.buildInternal(view);

		buildNearestRows((ViewGroup) view);
		rowsBuilder.buildNamesRow((ViewGroup) view, amenity.getAltNamesMap(), true);
		rowsBuilder.buildNamesRow((ViewGroup) view, amenity.getNamesMap(true), false);
		if (!rowsBuilder.isFirstRow()) {
			firstRow = rowsBuilder.isFirstRow();
		}
	}

	private void processRoutePointAmenityTags(View view) {
		final String wptExtraTags = additionalInfo.get(WPT_EXTRA_TAGS);
		if (!Algorithms.isEmpty(wptExtraTags)) {
			Gson gson = new Gson();
			Type type = new TypeToken<Map<String, String>>() {}.getType();
			additionalInfo.putAll(gson.fromJson(wptExtraTags, type));
			additionalInfo.remove(WPT_EXTRA_TAGS);
		}
		final String url = additionalInfo.get(TAG_URL);
		if (PicassoUtils.isImageUrl(url)) {
			AppCompatImageView imageView = inflateAndGetMainImageView(view);
			PicassoUtils.setupImageViewByUrl(app, imageView, url, true);
		}
		final String description = additionalInfo.get(Amenity.DESCRIPTION);
		if (!Algorithms.isEmpty(description)) {
			buildDescriptionRow(view, description);
			additionalInfo.remove(Amenity.DESCRIPTION);
		}
	}

	private void buildNearestRows(ViewGroup viewGroup) {
		buildNearestWiki(viewGroup);
		buildNearestPoi(viewGroup);
	}

	private void buildNearestWiki(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestWikiRow(viewGroup, amenities -> {
			ViewGroup group = viewGroupRef.get();
			if (group == null || Algorithms.isEmpty(amenities)) {
				return;
			}
			String title = app.getString(R.string.wiki_around);
			String count = "(" + amenities.size() + ")";
			String text = app.getString(R.string.ltr_or_rtl_combine_via_space, title, count);

			Context context = group.getContext();
			AmenityInfoRow wikiInfo = new AmenityInfoRow(
					NEAREST_WIKI_KEY, R.drawable.ic_plugin_wikipedia, null, text,
					null, true, getCollapsableView(context, true, amenities, NEAREST_WIKI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);

			View amenitiesRow = createRowContainer(context, NEAREST_WIKI_KEY);

			firstRow = position == 0 || isDividerAtPosition(group, position - 1);
			rowsBuilder.buildAmenityRow(amenitiesRow, wikiInfo);
			group.addView(amenitiesRow, position);

			buildNearestRowDividerIfMissing(group, position);
		});
	}

	private void buildNearestPoi(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestPoiRow(amenities -> {
			ViewGroup group = viewGroupRef.get();
			if (group == null) {
				return;
			}
			String title = app.getString(R.string.speak_poi);
			String type = "\"" + AmenityMenuController.getTypeStr(amenity) + "\"";
			String count = "(" + amenities.size() + ")";
			String text = app.getString(R.string.ltr_or_rtl_triple_combine_via_space, title, type, count);

			Context context = group.getContext();
			AmenityInfoRow poiInfo = new AmenityInfoRow(
					NEAREST_POI_KEY, AmenityMenuController.getRightIconId(app, amenity), null, text,
					null, true, getCollapsableView(context, true, amenities, NEAREST_POI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);

			View wikiRow = group.findViewWithTag(NEAREST_WIKI_KEY);
			int insertIndex = wikiRow != null
					? group.indexOfChild(wikiRow) + 1
					: position;

			View amenitiesRow = createRowContainer(context, NEAREST_POI_KEY);
			firstRow = insertIndex == 0 || isDividerAtPosition(group, insertIndex - 1);
			rowsBuilder.buildAmenityRow(amenitiesRow, poiInfo);
			group.addView(amenitiesRow, insertIndex);

			buildNearestRowDividerIfMissing(group, insertIndex);
		});
	}

	@Override
	protected Map<String, String> getAdditionalCardParams() {
		return AmenityExtensionsHelper.getImagesParams(additionalInfo);
	}
}