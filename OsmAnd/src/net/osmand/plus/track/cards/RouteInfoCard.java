package net.osmand.plus.track.cards;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.plus.utils.AndroidUtils.getActivityTypeStringPropertyName;
import static net.osmand.plus.utils.AndroidUtils.getStringByProperty;
import static net.osmand.util.Algorithms.capitalizeFirstLetterAndLowercase;

public class RouteInfoCard extends MapBaseCard {

	private final RouteKey routeKey;
	private final GPXFile gpxFile;

	public RouteInfoCard(
			@NonNull MapActivity activity,
			@NonNull RouteKey routeKey,
			@NonNull GPXFile gpxFile
	) {
		super(activity);
		this.routeKey = routeKey;
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_info_card;
	}

	@Override
	public void updateContent() {
		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		RouteKey routeKey = this.routeKey;
		String routeTypeName = routeKey.type.getTag();

		String routeTypeToDisplay = capitalizeFirstLetterAndLowercase(routeTypeName);
		routeTypeToDisplay = getActivityTypeStringPropertyName(app, routeTypeName, routeTypeToDisplay);
		addInfoRow(container, app.getString(R.string.layer_route), routeTypeToDisplay, false);

		for (RouteTag tag : getTagsToDisplay()) {
			String formattedKey = tag.getFormattedKey(app);
			String formattedValue = tag.getFormattedValue(app, routeKey.type);
			boolean linkify = "website".equals(tag.key);

			View view = addInfoRow(container, formattedKey, formattedValue, linkify);

			OsmAndAppCustomization customization = app.getAppCustomization();
			if ("wikipedia".equals(tag.key)) {
				if (Algorithms.isUrl(formattedValue) && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					TextView tvContent = view.findViewById(R.id.title);
					tvContent.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
					view.setOnClickListener(v -> {
						WikiArticleHelper.askShowArticle(activity, nightMode, collectTrackPoints(), formattedValue);
					});
				}
			}
		}
	}

	@NonNull
	private List<RouteTag> getTagsToDisplay() {
		List<RouteTag> tags = new ArrayList<>();

		for (String tag : routeKey.tags) {
			String key = routeKey.getKeyFromTag(tag);
			String value = routeKey.getValue(key);

			if (key.equals("name") || key.contains("osmc")) {
				continue;
			}

			tags.add(new RouteTag(key, value));
		}

		Collections.sort(tags, (o1, o2) -> {
			if (o1.getOrder() != o2.getOrder()) {
				return o1.getOrder() - o2.getOrder();
			}

			return o1.getFormattedKey(app).compareTo(o2.getFormattedKey(app));
		});

		return tags;
	}

	@NonNull
	private View addInfoRow(@NonNull ViewGroup container, @NonNull String key, @NonNull String value, boolean needLinks) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_with_descr, container, false);

		TextView tvLabel = view.findViewById(R.id.description);
		TextView tvContent = view.findViewById(R.id.title);

		tvContent.setText(value);
		tvLabel.setText(key);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(tvContent, Linkify.ALL)) {
			tvContent.setMovementMethod(null);
			tvContent.setLinkTextColor(ColorUtilities.getActiveColor(app, nightMode));
			tvContent.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(tvContent);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), container.getChildCount() > 0);
		container.addView(view);
		return view;
	}

	private List<LatLon> collectTrackPoints() {
		List<LatLon> points = new ArrayList<>();
		if (gpxFile != null) {
			for (WptPt wptPt : gpxFile.getAllPoints()) {
				points.add(new LatLon(wptPt.lat, wptPt.lon));
			}
		}
		return points;
	}

	private static class RouteTag {

		@NonNull
		private final String key;
		@NonNull
		private final String value;

		@Nullable
		private final PoiType poiType;

		public RouteTag(@NonNull String key, @NonNull String value) {
			this.key = key;
			this.value = value;

			AbstractPoiType abstractPoiType = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key);
			poiType = abstractPoiType instanceof PoiType
					? ((PoiType) abstractPoiType)
					: null;
		}

		@NonNull
		public String getFormattedKey(@NonNull OsmandApplication app) {
			if (key.startsWith("name:")) {
				String nameStr = app.getString(R.string.shared_string_name);
				String langId = key.substring("name:".length());
				String displayLanguage = new Locale(langId).getDisplayLanguage();
				return app.getString(R.string.ltr_or_rtl_combine_via_colon, nameStr, displayLanguage);
			}

			return poiType != null ? poiType.getTranslation() : capitalizeFirstLetterAndLowercase(key);
		}

		@NonNull
		public String getFormattedValue(@NonNull OsmandApplication app, @NonNull RouteType routeType) {
			switch (key) {
				case "network":
					String network = getStringByProperty(app, "poi_route_" + routeType.getTag() + "_" + value + "_poi");
					return Algorithms.isEmpty(network) ? value : network;
				case "wikipedia":
					return WikiAlgorithms.getWikiUrl(value);
				default:
					return value;
			}
		}

		public int getOrder() {
			return poiType != null ? poiType.getOrder() : PoiType.DEFAULT_ORDER;
		}
	}
}
