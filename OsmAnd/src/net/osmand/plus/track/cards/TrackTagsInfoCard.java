package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.data.Amenity.DESCRIPTION;
import static net.osmand.data.Amenity.NAME;
import static net.osmand.shared.gpx.GpxUtilities.ACTIVITY_TYPE;

import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.OsmRouteType;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.track.fragments.controller.SelectRouteActivityController;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.*;

public class TrackTagsInfoCard extends BaseMetadataCard {
	public static final Set<String> HIDDEN_GPX_TAGS = Set.of(ACTIVITY_TYPE, NAME, DESCRIPTION);
	private static final String HIDDEN_OSMC_TAGS_PREFIX = "osmc_";
	private static final String HIDDEN_OSMAND_TAGS_PREFIX = "osmand_";
	public static final String OSM_RELATION_URL = "https://www.openstreetmap.org/relation/";
	public static final String OSM_WAY_URL = "https://www.openstreetmap.org/way/";
	private static final Map<String, Integer> TRANSLATABLE_KEYS = new HashMap<>();

	static {
		TRANSLATABLE_KEYS.put("name", R.string.shared_string_name);
		TRANSLATABLE_KEYS.put("alt_name", R.string.shared_string_alt_name);
		TRANSLATABLE_KEYS.put("symbol", R.string.shared_string_symbol);
		TRANSLATABLE_KEYS.put("relation_id", R.string.shared_string_osm_id);
		TRANSLATABLE_KEYS.put("way_id", R.string.shared_string_osm_id);
	}

	private final RouteKey routeKey;
	private final GpxFile gpxFile;
	private final RouteActivitySelectionHelper activityHelper;

	public TrackTagsInfoCard(
			@NonNull MapActivity activity,
			@Nullable RouteKey routeKey,
			@NonNull GpxFile gpxFile,
			@NonNull RouteActivitySelectionHelper activityHelper) {
		super(activity, gpxFile.getMetadata());
		this.routeKey = routeKey;
		this.gpxFile = gpxFile;
		this.activityHelper = activityHelper;
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.poi_information;
	}

	@Override
	public void updateContent() {
		super.updateContent();

		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		RouteActivity routeActivity = activityHelper.getSelectedActivity();
		String label = routeActivity != null
				? routeActivity.getLabel()
				: app.getString(R.string.shared_string_none);
		Drawable icon = getContentIcon(AndroidUtils.getActivityIconId(app, routeActivity));
		createItemRow(getString(R.string.shared_string_activity), label, icon).setOnClickListener(
				v -> SelectRouteActivityController.showDialog(activity, appMode, activityHelper)
		);

		String keywords = metadata != null ? metadata.getKeywords() : null;
		if (!Algorithms.isEmpty(keywords)) {
			createItemRow(getString(R.string.shared_string_keywords), keywords, getContentIcon(R.drawable.ic_action_label));
		}

		String link = (metadata != null && metadata.getLink() != null) ? metadata.getLink().getHref() : null;
		if (!Algorithms.isEmpty(link)) {
			createLinkItemRow(getString(R.string.shared_string_link), link, R.drawable.ic_action_link);
		}

		if (routeKey != null) {
			if (routeKey.type != OsmRouteType.UNKNOWN) {
				String routeTypeToDisplay = AndroidUtils.getActivityTypeTitle(app, routeKey.type);
				addInfoRow(container, app.getString(R.string.layer_route), routeTypeToDisplay, false, false);
			}

			for (TagsRow row : getRows()) {
				LinearLayout expandableView = null;
				int tagsCount = row.tags.size();

				for (int tagIndex = 0; tagIndex < tagsCount; tagIndex++) {
					RouteTag tag = row.tags.get(tagIndex);
					if (!shouldAddRow(tag.key)) break;

					ViewGroup tagContainer = tagIndex == 0 ? container : expandableView;
					View view = addInfoRow(tagContainer, tag);

					if (tagIndex == 0 && tagsCount > 1) {
						expandableView = createExpandableView();
						container.addView(expandableView);
						setupViewExpand(view, expandableView);
					}
				}
			}
		}
		updateVisibility(view, true);
	}

	@NonNull
	private List<TagsRow> getRows() {
		List<TagsRow> rows = new ArrayList<>();
		Map<String, TagsRow> rowsByKey = new HashMap<>();

		for (String tag : routeKey.tags) {
			String key = routeKey.getKeyFromTag(tag);
			String value = routeKey.getValue(key);

			if (routeKey.type != OsmRouteType.UNKNOWN &&
					(key.equals("name") || key.equals("type") || key.contains("osmc"))) {
				continue;
			} else if (HIDDEN_GPX_TAGS.contains(key) || key.startsWith(HIDDEN_OSMC_TAGS_PREFIX)
					|| GpxAppearanceInfo.isGpxAppearanceTag(key)) {
				continue;
			} else if (key.contains(":") && !key.startsWith("name:") && !key.startsWith("ref:")) {
				String mainTag = key.split(":")[1];
				if (routeKey.tags.stream().anyMatch((t) -> mainTag.equals(routeKey.getKeyFromTag(t)))) {
					continue; // skip synthetic xxx:ref if ref exists (piste:ref, etc)
				}
			} else if ("network".equals(key) && "#".equals(value)) {
				continue; // avoid synthetic empty network caused by MapRenderingTypesEncoder.getNetwork()
			}

			RouteTag routeTag = new RouteTag(key, value);

			if (routeTag.poiType != null && routeTag.poiType.isHidden()) {
				continue;
			}

			String keyBase = key.split(":")[0];
			if (TRANSLATABLE_KEYS.containsKey(keyBase)) {
				TagsRow row = rowsByKey.get(keyBase);
				if (row == null) {
					row = new TagsRow();
					rowsByKey.put(keyBase, row);
					rows.add(row);
				}
				row.tags.add(routeTag);
			} else {
				TagsRow row = new TagsRow();
				row.tags.add(routeTag);
				rows.add(row);
			}
		}

		for (TagsRow row : rowsByKey.values()) {
			row.sort(app);
		}

		Collections.sort(rows, (o1, o2) -> {
			if (o1.getOrder() != o2.getOrder()) {
				return o1.getOrder() - o2.getOrder();
			}

			String formattedKey1 = o1.tags.get(0).getFormattedKey(app);
			String formattedKey2 = o2.tags.get(0).getFormattedKey(app);
			return formattedKey1.compareTo(formattedKey2);
		});

		return rows;
	}

	@NonNull
	private View addInfoRow(@NonNull ViewGroup container, @NonNull RouteTag tag) {
		String formattedKey = tag.getFormattedKey(app);
		String formattedValue = tag.getFormattedValue(app, routeKey.type);
		boolean linkify = "website".equals(tag.key);

		View view = addInfoRow(container, formattedKey, formattedValue, linkify, true);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if ("wikipedia".equals(tag.key)) {
			if (Algorithms.isUrl(formattedValue) && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
				setupClickableContent(view,
						v -> WikiArticleHelper.askShowArticle(activity, nightMode, collectTrackPoints(), formattedValue));
			}
		} else if ("relation_id".equals(tag.key)) {
			String url = OSM_RELATION_URL + formattedValue;
			setupClickableContent(view, v -> AndroidUtils.openUrl(activity, url, nightMode));
		} else if ("way_id".equals(tag.key)) {
			String url = OSM_WAY_URL + formattedValue;
			setupClickableContent(view, v -> AndroidUtils.openUrl(activity, url, nightMode));
		} else if ("route_id".equals(tag.key)) {
			long osmId = ObfConstants.getOsmIdFromPrefixedRouteId(tag.value);
			if (osmId > 0) {
				setupClickableContent(view, v -> AndroidUtils.openUrl(activity, OSM_RELATION_URL + osmId, nightMode));
			}
		}
		return view;
	}

	private void setupClickableContent(@NonNull View view, @NonNull OnClickListener listener) {
		TextView tvContent = view.findViewById(R.id.title);
		tvContent.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
		tvContent.setOnClickListener(listener);
	}

	@NonNull
	private View addInfoRow(@NonNull ViewGroup container, @NonNull String key,
			@NonNull String value, boolean needLinks, boolean showDivider) {
		View view = themedInflater.inflate(R.layout.list_item_with_descr, container, false);

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
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), showDivider);
		container.addView(view);
		return view;
	}

	@NonNull
	private LinearLayout createExpandableView() {
		LinearLayout view = new LinearLayout(this.view.getContext());
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(View.GONE);
		LinearLayout.LayoutParams layoutParams = new LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		view.setLayoutParams(layoutParams);
		return view;
	}

	private void setupViewExpand(@NonNull View viewToClick, @NonNull View expandableView) {
		ImageView expandIcon = viewToClick.findViewById(R.id.expand_button);
		expandIcon.setVisibility(View.VISIBLE);
		viewToClick.setOnClickListener(new OnClickListener() {

			private boolean expanded = false;

			@Override
			public void onClick(View v) {
				expanded = !expanded;

				int iconId = expanded ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
				expandIcon.setImageDrawable(getIcon(iconId));

				AndroidUiHelper.updateVisibility(expandableView, expanded);
			}
		});
	}

	private List<LatLon> collectTrackPoints() {
		List<LatLon> points = new ArrayList<>();
		if (gpxFile != null) {
			for (WptPt wptPt : gpxFile.getAllPoints()) {
				points.add(new LatLon(wptPt.getLat(), wptPt.getLon()));
			}
		}
		return points;
	}

	private boolean shouldAddRow(@NonNull String key) {
		if ("relation_id".equals(key) || "way_id".equals(key)) {
			return PluginsHelper.isEnabled(OsmEditingPlugin.class);
		}
		return !key.startsWith(HIDDEN_OSMAND_TAGS_PREFIX);
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
			for (Map.Entry<String, Integer> translatableKey : TRANSLATABLE_KEYS.entrySet()) {
				String keyStart = translatableKey.getKey() + ":";
				if (key.startsWith(keyStart)) {
					String nameStr = app.getString(translatableKey.getValue());
					String langId = key.substring(keyStart.length());
					String displayLanguage = new Locale(langId).getDisplayLanguage();
					return app.getString(R.string.ltr_or_rtl_combine_via_colon, nameStr, displayLanguage);
				} else if (key.equals(translatableKey.getKey())) {
					return app.getString(translatableKey.getValue());
				}
			}
			if (key.startsWith("network_")) {
				return app.getString(R.string.poi_network);
			} else if (poiType != null) {
				return poiType.getTranslation();
			} else {
				String stringKey = key.toLowerCase().replace(":", "_");
				String translatedName = AndroidUtils.getStringByProperty(app, "poi_" + stringKey);
				if (Algorithms.isEmpty(translatedName)) {
					translatedName = AndroidUtils.getRenderingStringPropertyName(app, stringKey, null);
				}
				if (Algorithms.isEmpty(translatedName)) {
					translatedName = Algorithms.capitalizeFirstLetterAndLowercase(key);
				}
				return translatedName;
			}
		}

		@NonNull
		public String getFormattedValue(@NonNull OsmandApplication app, @NonNull OsmRouteType routeType) {
			switch (key) {
				case "network":
				case "network_lwn":
				case "network_rwn":
				case "network_nwn":
				case "network_iwn":
				case "network_lcn":
				case "network_rcn":
				case "network_ncn":
				case "network_icn":
					String network = AndroidUtils.getStringByProperty(app, "poi_network_" + value);
					return Algorithms.isEmpty(network) ? value : network;
				case "wikipedia":
					return WikiAlgorithms.getWikiUrl(value);
				case "ascent":
				case "descent":
					return getValueWithUnits(app, R.string.m);
				case "distance":
					return getValueWithUnits(app, R.string.km);
				default:
					return value;
			}
		}

		private String getValueWithUnits(@NonNull OsmandApplication app, @StringRes int resId) {
			if (value.split(" ").length > 1) {
				return value;
			} else {
				return value + " " + app.getString(resId);
			}
		}

		public int getOrder() {
			return poiType != null ? poiType.getOrder() : PoiType.DEFAULT_ORDER;
		}
	}

	private static class TagsRow {

		private final List<RouteTag> tags = new ArrayList<>();

		public void sort(@NonNull OsmandApplication app) {
			List<String> localeIds = new ArrayList<>();
			boolean hasNativeName = false;
			for (RouteTag tag : tags) {
				String[] keySplit = tag.key.split(":");
				if (keySplit.length == 1) {
					hasNativeName = true;
				} else {
					localeIds.add(keySplit[1]);
				}
			}

			Locale preferredLocale = LocaleHelper.getPreferredNameLocale(app, localeIds);
			String preferredLocaleId = preferredLocale != null ? preferredLocale.getLanguage() : null;
			boolean finalHasNativeName = hasNativeName;

			Collections.sort(tags, (o1, o2) -> {
				if (preferredLocaleId != null) {
					if (o1.key.contains(preferredLocaleId)) {
						return -1;
					} else if (o2.key.contains(preferredLocaleId)) {
						return 1;
					}
				} else if (finalHasNativeName) {
					if (!o1.key.contains(":")) {
						return -1;
					} else if (!o2.key.contains(":")) {
						return 1;
					}
				}

				return o1.getFormattedKey(app).compareTo(o2.getFormattedKey(app));
			});
		}

		public int getOrder() {
			return tags.get(0).getOrder();
		}
	}
}
