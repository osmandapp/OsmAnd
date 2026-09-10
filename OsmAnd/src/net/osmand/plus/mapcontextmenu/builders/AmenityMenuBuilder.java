package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIPEDIA;
import static net.osmand.data.AdditionalInfoBundle.LOCALIZATIONS;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_POI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_WIKI_KEY;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.WPT_EXTRA_TAGS;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.AdditionalInfoBundle;
import net.osmand.data.Amenity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AmenityMenuBuilder extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);
	protected AmenityUIHelper amenityUIHelper;
	protected Map<String, String> extensions;
	protected AdditionalInfoBundle infoBundle;

	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, @NonNull Amenity amenity) {
		super(mapActivity);
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		extensions = amenity.getAmenityExtensions(app.getPoiTypes(), false);
		setCustomOnlinePhotosPosition(extensions.containsKey(WIKIDATA));
		infoBundle = new AdditionalInfoBundle(app.getPoiTypes(), extensions);

		super.build(view, object);
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	@Override
	protected void buildDescription(View view) {
		createAmenityDescriptionBuilder().buildDescription(view);
		if (isCustomOnlinePhotosPosition()) {
			buildPhotosRow((ViewGroup) view, amenity);
		}
	}

	protected boolean buildShortWikiDescription(@NonNull View view,
			@NonNull Map<String, Object> filteredInfo, boolean allowOnlineWiki) {
		return createAmenityDescriptionBuilder().buildShortWikiDescription(
				view, filteredInfo, allowOnlineWiki);
	}

	@NonNull
	private AmenityDescriptionBuilder createAmenityDescriptionBuilder() {
		return new AmenityDescriptionBuilder(this, amenity, infoBundle, isLightContent());
	}

	@Override
	public void buildInternal(View view) {
		processRoutePointAmenityTags(view);
		buildInternalRows(view);

		buildNearestRows((ViewGroup) view);
		buildAltNamesRow((ViewGroup) view);
		buildNamesRow((ViewGroup) view);
		if (!amenityUIHelper.isFirstRow()) {
			firstRow = amenityUIHelper.isFirstRow();
		}
	}

	public void buildInternalRows(@NonNull View view) {
		amenityUIHelper = new AmenityUIHelper(mapActivity, infoBundle);
		amenityUIHelper.setLight(isLightContent());
		amenityUIHelper.setLatLon(getLatLon());
		amenityUIHelper.setCollapseExpandListener(getCollapseExpandListener());
		amenityUIHelper.buildInternal(view);
	}

	private void buildNamesRow(ViewGroup view) {
		HashMap<String, String> names = new HashMap<>();
		if (!Algorithms.isEmpty(amenity.getName())) {
			names.put("", amenity.getName());
		}
		names.putAll(amenity.getNamesMap(true));
		amenityUIHelper.buildNamesRow(view, names, false);
	}

	private void buildAltNamesRow(ViewGroup view) {
		amenityUIHelper.buildNamesRow(view, amenity.getAltNamesMap(), true);
	}

	private void processRoutePointAmenityTags(View view) {
		if (amenity.isRoutePoint()) {
			String wptExtraTags = extensions.get(WPT_EXTRA_TAGS);
			if (!Algorithms.isEmpty(wptExtraTags)) {
				Gson gson = new Gson();
				Type type = new TypeToken<Map<String, String>>() {}.getType();
				extensions.putAll(gson.fromJson(wptExtraTags, type));
				extensions.remove(WPT_EXTRA_TAGS);
			}
			String url = extensions.get(TAG_URL);
			if (PicassoUtils.isImageUrl(url)) {
				AppCompatImageView imageView = inflateAndGetMainImageView(view);
				PicassoUtils.setupImageViewByUrl(app, imageView, url, true);
			}
		}
	}

	private void buildNearestRows(ViewGroup viewGroup) {
		buildNearestWiki(viewGroup);
		if (!OSMSettings.OSMTagKey.ADMINISTRATIVE.getValue().equals(amenity.getType().getKeyName())) {
			buildNearestPoi(viewGroup);
		}
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
			AmenityInfoRow wikiInfo = new AmenityInfoRow.Builder(NEAREST_WIKI_KEY)
					.setIconId(R.drawable.ic_action_popular_places).setText(text)
					.setCollapsableView(getCollapsableView(context, true, amenities, NEAREST_WIKI_KEY))
					.setOrder(1000)
					.build();

			int safePosition = Math.min(position, group.getChildCount());
			View amenitiesRow = createRowContainer(context, NEAREST_WIKI_KEY);

			firstRow = safePosition == 0 || isDividerAtPosition(group, safePosition - 1);
			amenityUIHelper.buildAmenityRow(amenitiesRow, wikiInfo);
			group.addView(amenitiesRow, safePosition);

			buildNearestRowDividerIfMissing(group, safePosition);
			requestMenuRelayout(group);
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
			String type = "\"" + AmenityMenuController.getTypeStr(app, amenity) + "\"";
			String count = "(" + amenities.size() + ")";
			String text = app.getString(R.string.ltr_or_rtl_triple_combine_via_space, title, type, count);

			Context context = group.getContext();
			AmenityInfoRow poiInfo = new AmenityInfoRow.Builder(NEAREST_POI_KEY)
					.setIconId(AmenityMenuController.getRightIconId(app, amenity)).setText(text)
					.setCollapsableView(getCollapsableView(context, true, amenities, NEAREST_POI_KEY))
					.setOrder(1000)
					.build();

			View wikiRow = group.findViewWithTag(NEAREST_WIKI_KEY);
			int insertIndex = wikiRow != null
					? group.indexOfChild(wikiRow) + 1
					: position;

			View amenitiesRow = createRowContainer(context, NEAREST_POI_KEY);
			firstRow = insertIndex == 0 || isDividerAtPosition(group, insertIndex - 1);
			amenityUIHelper.buildAmenityRow(amenitiesRow, poiInfo);
			group.addView(amenitiesRow, insertIndex);

			buildNearestRowDividerIfMissing(group, insertIndex);
			requestMenuRelayout(group);
		});
	}

	@Override
	@NonNull
	public Map<String, String> getAdditionalImageParams() {
		return AmenityExtensionsHelper.getImagesParams(extensions);
	}
}
