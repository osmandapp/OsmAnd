package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.LANG_YES;
import static net.osmand.data.Amenity.ROUTE_NAME;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.views.layers.PlaceDetailsObject;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlaceDetailsMenuBuilder extends AmenityMenuBuilder {

	private static final String LANG_PREFIX = LANG_YES + ":";

	private final PlaceDetailsObject detailsObject;

	public PlaceDetailsMenuBuilder(@NonNull MapActivity activity,
			@NonNull PlaceDetailsObject detailsObject) {
		super(activity, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
	}

	@Override
	public void buildPlaceRows(@NonNull ViewGroup view, @Nullable Object object) {
		super.buildPlaceRows(view, object);
		buildGuidesRow(view);
	}

	private void buildGuidesRow(View view) {
		Map<String, Amenity> amenities = getTravelAmenities();
		if (!Algorithms.isEmpty(amenities)) {
			String title = app.getString(R.string.travel_guides);
			CollapsableView cv = getGuidesCollapsableView(amenities);
			buildRow(view, R.drawable.ic_action_travel_guides, null, title, 0, true,
					cv, false, 1, false, null, false);
		}
	}

	@NonNull
	protected CollapsableView getGuidesCollapsableView(@NonNull Map<String, Amenity> amenities) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (Amenity amenity : amenities.values()) {
			TextViewEx button = buildButtonInCollapsableView(mapActivity, false, false);
			String name = amenity.getTagContent(ROUTE_NAME);
			String lang = amenity.getTagSuffix(LANG_PREFIX);

			button.setText(name);
			button.setOnClickListener(v -> {
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				WikivoyageArticleDialogFragment.showInstanceByTitle(app, manager, name, lang);
			});
			llv.addView(button);
		}
		return new CollapsableView(llv, this, true);
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
