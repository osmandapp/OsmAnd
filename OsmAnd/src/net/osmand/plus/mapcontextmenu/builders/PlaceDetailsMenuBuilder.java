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
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.SearchTravelArticlesTask;
import net.osmand.plus.views.layers.PlaceDetailsObject;
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

	private final PlaceDetailsObject detailsObject;

	public PlaceDetailsMenuBuilder(@NonNull MapActivity activity,
			@NonNull PlaceDetailsObject detailsObject) {
		super(activity, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
	}

	@Override
	public void buildNearestRows(@NonNull ViewGroup viewGroup, @Nullable Object object) {
		super.buildNearestRows(viewGroup, object);
		buildGuidesRow(viewGroup);
	}

	private void buildGuidesRow(@NonNull ViewGroup viewGroup) {
		Map<String, Amenity> travelAmenities = getTravelAmenities();
		if (Algorithms.isEmpty(travelAmenities)) {
			return;
		}
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		searchTravelArticles(travelAmenities, articles -> {
			ViewGroup group = viewGroupRef.get();
			if (group != null && !Algorithms.isEmpty(articles)) {
				int insertIndex = position == 0 ? 0 : position + 1;
				firstRow = insertIndex == 0 || isDividerAtPosition(group, insertIndex - 1);

				int iconId = R.drawable.ic_action_travel_guides;
				String title = app.getString(R.string.travel_guides);
				CollapsableView collapsableView = getGuidesCollapsableView(articles);
				View container = createRowContainer(group.getContext(), TRAVEL_GUIDES_KEY);
				buildRow(container, iconId, null, title, 0, true,
						collapsableView, false, 1, false, null, false);

				group.addView(container, insertIndex);
				buildNearestRowDividerIfMissing(group, insertIndex);
			}
			return true;
		});
	}

	private void searchTravelArticles(@NonNull Map<String, Amenity> amenities,
			@Nullable CallbackWithObject<Map<String, Map<String, TravelArticle>>> callback) {
		execute(new SearchTravelArticlesTask(app, amenities, callback));
	}

	@NonNull
	protected CollapsableView getGuidesCollapsableView(
			@NonNull Map<String, Map<String, TravelArticle>> articles) {
		String lang = app.getLanguage();
		LinearLayout view = buildCollapsableContentView(mapActivity, true, true);
		for (Map<String, TravelArticle> articleMap : articles.values()) {
			TravelArticle article = getArticle(articleMap, lang);

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
	private TravelArticle getArticle(@NonNull Map<String, TravelArticle> map, @NonNull String lang) {
		TravelArticle article = map.get(lang);
		return article != null ? article : map.entrySet().iterator().next().getValue();
	}

	@NonNull
	private Map<String, Amenity> getTravelAmenities() {
		Map<String, Amenity> map = new LinkedHashMap<>();
		for (Amenity amenity : detailsObject.getAmenities()) {
			if (CollectionUtils.equalsToAny(amenity.getSubType(), ROUTE_ARTICLE_POINT, ROUTE_TRACK_POINT)) {
				String routeId = amenity.getRouteId();
				if (!map.containsKey(routeId)) {
					map.put(amenity.getRouteId(), amenity);
				}
			}
		}
		return map;
	}
}
