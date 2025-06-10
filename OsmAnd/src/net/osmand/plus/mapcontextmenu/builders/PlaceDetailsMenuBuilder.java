package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
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
import java.util.LinkedHashMap;
import java.util.List;
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
	public void buildNearestRows(@NonNull ViewGroup viewGroup, @Nullable Object object) {
		super.buildNearestRows(viewGroup, object);
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
		execute(new SearchTravelArticlesTask(app, routeIds, callback));
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
					routeId = amenity.getRouteId();
					if (!Algorithms.isEmpty(routeId) && !map.containsKey(routeId)) {
						map.put(routeId, amenity.getLocation());
					}
				}
			}
			return map;
		}
		return null;
	}
}
