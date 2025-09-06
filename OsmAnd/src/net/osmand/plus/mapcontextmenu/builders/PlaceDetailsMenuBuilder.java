package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.DESCRIPTION;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.SearchTravelArticlesTask;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlaceDetailsMenuBuilder extends AmenityMenuBuilder {

	private static final String TRAVEL_GUIDES_KEY = "travel_guides_key";

	private final BaseDetailsObject detailsObject;

	public PlaceDetailsMenuBuilder(@NonNull MapActivity activity,
			@NonNull BaseDetailsObject detailsObject) {
		super(activity, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
	}

	@Override
	protected void buildDescription(View view) {
		List<Amenity> wikiAmenities = getWikiAmenities();
		boolean hasDescription = buildDescription(view, wikiAmenities, false);

		if (!hasDescription) {
			Map<String, Object> filteredInfo = infoBundle.getFilteredLocalizedInfo();
			buildShortWikiDescription(view, filteredInfo, true);

			hasDescription = buildDescription(view, getTravelAmenities(), false);
		}
		if (hasDescription) {
			infoBundle.setCustomHiddenExtensions(Collections.singletonList(DESCRIPTION));
		}
		if (isCustomOnlinePhotosPosition()) {
			buildPhotosRow((ViewGroup) view, amenity);
		}
	}

	private boolean buildDescription(@NonNull View view, @NonNull List<Amenity> amenities,
			boolean allowOnlineWiki) {
		if (detailsObject != null && buildDescription(view, detailsObject.getSyntheticAmenity(), allowOnlineWiki)) {
			return true;
		}
		for (Amenity amenity : amenities) {
			if (buildDescription(view, amenity, allowOnlineWiki)) {
				return true;
			}
		}
		return false;
	}

	private boolean buildDescription(@NonNull View view, @NonNull Amenity amenity,
			boolean allowOnlineWiki) {
		Map<String, String> extensions = amenity.getAmenityExtensions(app.getPoiTypes(), false);
		AdditionalInfoBundle bundle = new AdditionalInfoBundle(app, extensions);
		Map<String, Object> filteredInfo = bundle.getFilteredLocalizedInfo();

		if (buildShortWikiDescription(view, filteredInfo, allowOnlineWiki)) {
			return true;
		}
		Pair<String, Locale> pair = AmenityUIHelper.getDescriptionWithPreferredLang(app, amenity, DESCRIPTION, filteredInfo);
		if (pair != null) {
			String routeId = amenity.getRouteId();
			if (!Algorithms.isEmpty(routeId) && app.getResourceManager().hasTravelRepositories()) {
				buildDescriptionRow(view, pair.first, v -> {
					FragmentManager manager = mapActivity.getSupportFragmentManager();
					TravelArticleIdentifier identifier = new TravelArticleIdentifier(null,
							amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(),
							null, routeId, null);
					WikivoyageArticleDialogFragment.showInstance(app, manager, identifier, null);
				}, app.getString(R.string.context_menu_read_article));
			} else {
				buildDescriptionRow(view, pair.first);
			}
			return true;
		}
		return false;
	}

	@Override
	public void buildPhotosRow(@NonNull ViewGroup viewGroup, @Nullable Object object) {
		super.buildPhotosRow(viewGroup, object);
		buildGuidesRow(viewGroup);
	}

	private void buildGuidesRow(@NonNull ViewGroup viewGroup) {
		Map<String, LatLon> routeIds = getTravelIds();
		if (Algorithms.isEmpty(routeIds)) {
			return;
		}
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		searchTravelArticles(routeIds, articles -> {
			ViewGroup group = viewGroupRef.get();
			if (group != null && !Algorithms.isEmpty(articles)) {
				firstRow = position == 0 || isDividerAtPosition(group, position - 1);

				int iconId = R.drawable.ic_action_travel_guides;
				String title = app.getString(R.string.travel_guides);
				CollapsableView collapsableView = getGuidesCollapsableView(articles);
				View container = createRowContainer(group.getContext(), TRAVEL_GUIDES_KEY);
				buildRow(container, iconId, null, title, 0, true,
						collapsableView, false, 1, false, null, false);

				group.addView(container, position);
				buildNearestRowDividerIfMissing(group, position);
			}
			return true;
		});
	}

	private void searchTravelArticles(@NonNull Map<String, LatLon> routeIds,
			@Nullable CallbackWithObject<Map<String, Map<String, TravelArticle>>> callback) {
		OsmAndTaskManager.executeTask(new SearchTravelArticlesTask(app, routeIds, callback));
	}

	@NonNull
	protected CollapsableView getGuidesCollapsableView(
			@NonNull Map<String, Map<String, TravelArticle>> articles) {
		String appLang = app.getLanguage();
		String mapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		LinearLayout view = buildCollapsableContentView(mapActivity, true, true);
		for (Map<String, TravelArticle> articleMap : articles.values()) {
			TravelArticle article = getArticle(articleMap, appLang, mapLang);

			TextViewEx button = buildButtonInCollapsableView(mapActivity, false, false);
			button.setText(article.getTitle());
			button.setOnClickListener(v -> {
				List<String> langs = new ArrayList<>(articleMap.keySet());
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				TravelArticleIdentifier identifier = article.generateIdentifier();
				WikivoyageArticleDialogFragment.showInstance(manager, identifier, langs, article.getLang());
			});
			view.addView(button);
		}
		return new CollapsableView(view, this, true);
	}

	@NonNull
	private TravelArticle getArticle(@NonNull Map<String, TravelArticle> articleMap,
			@NonNull String appLang, @Nullable String mapLang) {
		TravelArticle article = articleMap.get(appLang);
		if (article == null) {
			article = articleMap.get(mapLang);
		}
		return article != null ? article : articleMap.entrySet().iterator().next().getValue();
	}

	@Nullable
	private Map<String, LatLon> getTravelIds() {
		if (app.getResourceManager().hasTravelRepositories()) {
			Map<String, LatLon> map = new LinkedHashMap<>();

			String routeId = amenity.getRouteId();
			if (!Algorithms.isEmpty(routeId)) {
				map.put(routeId, amenity.getLocation());
			}
			for (Amenity amenity : detailsObject.getAmenities()) {
				if (CollectionUtils.equalsToAny(amenity.getSubType(), ROUTE_ARTICLE_POINT, ROUTE_TRACK_POINT)) {
					String id = amenity.getRouteId();
					String wikidata = amenity.getWikidata();

					if (!Algorithms.isEmpty(id) && !map.containsKey(id)) {
						map.put(id, amenity.getLocation());
					}
					if (!Algorithms.isEmpty(wikidata) && !map.containsKey(wikidata)) {
						map.put(wikidata, amenity.getLocation());
					}
				}
			}
			return map;
		}
		return null;
	}

	@NonNull
	private List<Amenity> getWikiAmenities() {
		List<Amenity> amenities = new ArrayList<>();
		for (Amenity amenity : detailsObject.getAmenities()) {
			if (amenity.getType().isWiki()) {
				amenities.add(amenity);
			}
		}
		return amenities;
	}

	@NonNull
	private List<Amenity> getTravelAmenities() {
		List<Amenity> amenities = new ArrayList<>();
		for (Amenity amenity : detailsObject.getAmenities()) {
			if (amenity.isRoutePoint()) {
				amenities.add(amenity);
			}
		}
		return amenities;
	}
}