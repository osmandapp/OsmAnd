package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIPEDIA;
import static net.osmand.shared.data.AdditionalInfoBundle.LOCALIZATIONS;
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
import net.osmand.data.Amenity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.data.GalleryKey;
import net.osmand.plus.gallery.online.OnlinePhotosRowController;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.shared.data.AdditionalInfoBundle;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private static final String NEARBY_HEADER_KEY = "nearby_header";

	private final List<View> aboutHeaderViews = new ArrayList<>();
	private int aboutContentViews;

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		amenityUIHelper = null;
		extensions = amenity.getAmenityExtensions(app.getPoiTypes(), false);
		setCustomOnlinePhotosPosition(extensions.containsKey(WIKIDATA));
		infoBundle = new AdditionalInfoBundle(app.getKPoiTypes(), extensions);

		super.build(view, object);
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	@Override
	protected void buildTopInternal(View view) {
		ViewGroup group = (ViewGroup) view;
		int start = group.getChildCount();
		int headerViews = buildSectionHeader(view, app.getString(R.string.shared_string_about), -1);
		aboutHeaderViews.clear();
		for (int i = start; i < start + headerViews; i++) {
			aboutHeaderViews.add(group.getChildAt(i));
		}
		aboutContentViews = 0;
		super.buildTopInternal(view);
		// the About tags (description:*, start_date, architect, names...) stay with Wikipedia at the top
		AmenityUIHelper helper = getAmenityUIHelper();
		helper.prepareSections(view.getContext());
		helper.continueSection(AmenityUIHelper.CardSection.ABOUT, isFirstRow());
		helper.buildSection(view, AmenityUIHelper.CardSection.ABOUT);
		buildAllNamesRow(group);
		firstRow = helper.isFirstRow();
		aboutContentViews += group.getChildCount() - start - headerViews;
		if (aboutContentViews == 0) {
			removeAboutHeader(group);
			firstRow = start == 0;
		}
	}

	private void removeAboutHeader(@NonNull ViewGroup group) {
		for (View headerView : aboutHeaderViews) {
			group.removeView(headerView);
		}
		aboutHeaderViews.clear();
	}

	// Prototype: About shows the photos of a wikidata object only when there are some
	@Override
	protected void buildOnlinePhotosRow(@NonNull View view, @NonNull GalleryKey.Location key) {
		ViewGroup group = (ViewGroup) view;
		int start = group.getChildCount();
		super.buildOnlinePhotosRow(view, key);
		OnlinePhotosRowController controller = getOnlinePhotosRowController();
		if (!isCustomOnlinePhotosPosition() || controller == null) {
			return;
		}
		List<View> photoViews = new ArrayList<>();
		for (int i = start; i < group.getChildCount(); i++) {
			photoViews.add(group.getChildAt(i));
		}
		aboutContentViews -= photoViews.size();
		WeakReference<ViewGroup> groupRef = new WeakReference<>(group);
		controller.setOnEmptyListener(() -> {
			ViewGroup g = groupRef.get();
			if (g != null) {
				for (View photoView : photoViews) {
					g.removeView(photoView);
				}
				if (aboutContentViews == 0) {
					removeAboutHeader(g);
				}
				requestMenuRelayout(g);
			}
			return null;
		});
		controller.preload();
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

		// Location: within, nearby places, then the coordinates (MenuBuilder) and the photos around
		firstRow = amenityUIHelper.isFirstRow();
		buildSectionHeader(view, app.getString(R.string.poi_card_section_location), -1);
		super.buildWithinRow((ViewGroup) view);
		buildNearestRows((ViewGroup) view);
		if (!amenityUIHelper.isFirstRow()) {
			firstRow = amenityUIHelper.isFirstRow();
		}
	}

	@Override
	protected void buildWithinRow(ViewGroup viewGroup) {
		// built in the Location section, see buildInternal
	}

	private void buildAllNamesRow(ViewGroup view) {
		HashMap<String, String> names = new HashMap<>();
		if (!Algorithms.isEmpty(amenity.getName())) {
			names.put("", amenity.getName());
		}
		names.putAll(amenity.getNamesMap(true));
		getAmenityUIHelper().buildAllNamesRow(view, names, amenity.getAltNamesMap());
	}

	// Prototype: the "Nearby" header appears with the first nearest row
	private int insertNearbyHeader(@NonNull ViewGroup group, int position) {
		View header = group.findViewWithTag(NEARBY_HEADER_KEY);
		if (header != null) {
			return group.indexOfChild(header) + 1;
		}
		int added = buildSectionHeader(group, app.getString(R.string.poi_card_section_nearby), position);
		group.getChildAt(position + added - 1).setTag(NEARBY_HEADER_KEY);
		return position + added;
	}

	public void buildInternalRows(@NonNull View view) {
		getAmenityUIHelper().buildInternal(view);
	}

	@NonNull
	private AmenityUIHelper getAmenityUIHelper() {
		if (amenityUIHelper == null) {
			amenityUIHelper = new AmenityUIHelper(mapActivity, infoBundle);
			amenityUIHelper.setLight(isLightContent());
			amenityUIHelper.setLatLon(getLatLon());
			amenityUIHelper.setCollapseExpandListener(getCollapseExpandListener());
		}
		return amenityUIHelper;
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
					: Math.min(position, group.getChildCount());

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
