package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_PHONE_ID;
import static net.osmand.data.Amenity.*;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.WIKI_LANG;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.ALT_NAMES_ROW_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NAMES_ROW_KEY;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKIPEDIA;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKI_LINK;
import static net.osmand.shared.settings.enums.MetricsConstants.KILOMETERS_AND_METERS;
import static net.osmand.shared.settings.enums.MetricsConstants.MILES_AND_FEET;
import static net.osmand.shared.settings.enums.MetricsConstants.MILES_AND_YARDS;
import static net.osmand.shared.settings.enums.MetricsConstants.NAUTICAL_MILES_AND_FEET;
import static net.osmand.util.Algorithms.isUrl;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.util.PatternsCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.Map.Entry;


public class AmenityUIHelper extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityUIHelper.class);

	private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#.##");

	private static final String CUISINE_INFO_ID = COLLAPSABLE_PREFIX + "cuisine";
	private static final String DISH_INFO_ID = COLLAPSABLE_PREFIX + "dish";
	private static final String US_MAPS_RECREATION_AREA = "us_maps_recreation_area";
	private static final String WIKI_DATA_BASE_URL = "https://www.wikidata.org/wiki/";

	private final MetricsConstants metricSystem;
	private final AdditionalInfoBundle additionalInfo;

	private String preferredLang;
	private Amenity wikiAmenity;
	private MapPoiTypes poiTypes;
	private PoiCategory poiCategory;
	private PoiType poiType;
	private String subtype;
	private AmenityInfoRow cuisineRow = null;
	private Map<String, List<PoiType>> poiAdditionalCategories = new HashMap<>();
	private Map<String, List<PoiType>> collectedPoiTypes = new HashMap<>();
	private boolean osmEditingEnabled = PluginsHelper.isActive(OsmEditingPlugin.class);
	private boolean lastBuiltRowIsDescription = false;

	public AmenityUIHelper(@NonNull MapActivity mapActivity, String preferredLang,
			@NonNull AdditionalInfoBundle infoBundle) {
		super(mapActivity);
		this.preferredLang = preferredLang;
		this.additionalInfo = infoBundle;
		this.metricSystem = mapActivity.getMyApplication().getSettings().METRIC_SYSTEM.get();
	}

	public void setPreferredLang(String lang) {
		this.preferredLang = lang;
	}

	@Override
	public void buildInternal(View view) {
		initVariables();
		Context context = view.getContext();
		List<AmenityInfoRow> infoRows = new LinkedList<>();
		List<AmenityInfoRow> descriptions = new LinkedList<>();
		Map<String, Object> filteredInfo = additionalInfo.getFilteredLocalizedInfo();
		for (Entry<String, Object> entry : filteredInfo.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key) instanceof PoiType that) {
				if (that.isHidden()) {
					continue;
				}
			}
			if (key.contains(WIKIPEDIA) || key.contains(CONTENT)
					|| key.contains(SHORT_DESCRIPTION) || key.contains(WIKI_LANG)) {
				continue;
			}
			if (ROUTE_ARTICLE.equals(subtype) && key.contains(DESCRIPTION)) {
				continue;
			}
			if (key.equals(NAME)) {
				continue; // will be added in buildNamesRow
			}
			AmenityInfoRow infoRow = null;
			if (value instanceof String strValue) {
				infoRow = createAmenityInfoRow(context, key, strValue, null);
			} else if (value != null) {
				infoRow = createLocalizedAmenityInfoRow(context, key, value);
			}
			if (infoRow != null) {
				if (lastBuiltRowIsDescription) {
					descriptions.add(infoRow);
				} else if (Amenity.CUISINE.equals(key)) {
					cuisineRow = infoRow;
				} else if (poiType == null) {
					infoRows.add(infoRow);
				}
			}
		}

		if (cuisineRow != null && !additionalInfo.containsAny(CUISINE_INFO_ID, DISH_INFO_ID)) {
			infoRows.add(cuisineRow);
		}

		for (Map.Entry<String, String> e : additionalInfo.getFilteredInfo().entrySet()) {
			if (e.getKey().startsWith(COLLAPSABLE_PREFIX)) {
				List<PoiType> categoryTypes = new ArrayList<>();

				if (!Algorithms.isEmpty(e.getValue())) {
					StringBuilder sb = new StringBuilder();
					List<String> records = new ArrayList<>(Arrays.asList(e.getValue().split(Amenity.SEPARATOR)));
					for (String record : records) {
						AbstractPoiType pt = poiTypes.getPoiAdditionalType(poiCategory, record);
						if (pt == null) {
							pt = poiTypes.getAnyPoiAdditionalTypeByKey(record);
						}
						categoryTypes.add((PoiType) pt);
						if (sb.length() > 0) {
							sb.append(" • ");
						}
						sb.append(pt.getTranslation());
					}

					Drawable icon;
					PoiType pType = categoryTypes.get(0);
					String poiAdditionalCategoryName = pType.getPoiAdditionalCategory();
					String poiAdditionalIconName = poiTypes.getPoiAdditionalCategoryIconName(poiAdditionalCategoryName);
					String iconIdDescription = poiAdditionalIconName;
					icon = getRowIcon(view.getContext(), poiAdditionalIconName);
					if (icon == null) {
						iconIdDescription = poiAdditionalCategoryName;
						icon = getRowIcon(view.getContext(), poiAdditionalCategoryName);
					}
					if (icon == null) {
						iconIdDescription = pType.getIconKeyName();
						icon = getRowIcon(view.getContext(), pType.getIconKeyName());
					}
					if (icon == null) {
						iconIdDescription = "ic_action_note_dark";
						icon = getRowIcon(R.drawable.ic_action_note_dark);
					}
					boolean cuisineOrDish = CollectionUtils.equalsToAny(e.getKey(), Amenity.CUISINE, Amenity.DISH);
					CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true,
							categoryTypes, true, cuisineOrDish ? cuisineRow : null, poiCategory);
					infoRows.add(new AmenityInfoRow(poiAdditionalCategoryName, icon,
							pType.getPoiAdditionalCategoryTranslation(), sb.toString(), null,
							true, collapsableView, 0, false, false,
							false, pType.getOrder(), pType.getKeyName(), false,
							false, false, 1, iconIdDescription));
				}
			}
		}

		if (collectedPoiTypes.size() > 0) {
			for (Map.Entry<String, List<PoiType>> e : collectedPoiTypes.entrySet()) {
				List<PoiType> poiTypeList = e.getValue();
				CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true, poiTypeList, false, null, poiCategory);
				PoiCategory poiCategory = this.poiCategory;
				StringBuilder sb = new StringBuilder();
				for (PoiType pt : poiTypeList) {
					if (sb.length() > 0) {
						sb.append(" • ");
					}
					sb.append(pt.getTranslation());
					poiCategory = pt.getCategory();
				}
				Drawable icon = getRowIcon(view.getContext(), poiCategory.getIconKeyName());
				infoRows.add(new AmenityInfoRow(poiCategory.getKeyName(), icon,
						poiCategory.getTranslation(), sb.toString(), null, true,
						collapsableView, 0, false, false, false, 40,
						poiCategory.getKeyName(), false, false, false, 1, poiCategory.getIconKeyName()));
			}
		}

		sortInfoRows(infoRows);
		for (AmenityInfoRow info : infoRows) {
			buildAmenityRow(view, info);
		}

		sortDescriptionRows(descriptions);
		for (AmenityInfoRow info : descriptions) {
			buildAmenityRow(view, info);
		}
	}

	public void buildWikiDataRow(@NonNull View view) {
		String wikidataValue = additionalInfo.get(WIKIDATA);
		if (wikidataValue != null) {
			int iconId = R.drawable.ic_action_logo_wikidata;
			PoiType pType;
			AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(WIKIDATA);
			if (pt != null) {
				pType = (PoiType) pt;
				String textPrefix = pType.getTranslation();
				String hiddenUrl = getSocialMediaUrl(WIKIDATA, wikidataValue);
				AmenityInfoRow infoRow = new AmenityInfoRow(WIKIDATA, iconId, textPrefix, wikidataValue, hiddenUrl, false,
						null, 0, false, true, true, pType.getOrder(),
						pType.getKeyName(), false, true, matchWidthDivider, 0);
				buildAmenityRow(view, infoRow);
			}
		}
	}

	private void initVariables() {
		poiCategory = null;
		String typeTag = additionalInfo.get(TYPE);
		if (!Algorithms.isEmpty(typeTag)) {
			poiCategory = MapPoiTypes.getDefault().getPoiCategoryByName(typeTag);
		}
		if (poiCategory == null) {
			poiCategory = MapPoiTypes.getDefault().getOtherPoiCategory();
		}
		subtype = additionalInfo.get(SUBTYPE);
		poiTypes = app.getPoiTypes();
		cuisineRow = null;
		poiAdditionalCategories = new HashMap<>();
		collectedPoiTypes = new HashMap<>();
		osmEditingEnabled = PluginsHelper.isActive(OsmEditingPlugin.class);
	}

	private void sortInfoRows(@NonNull List<AmenityInfoRow> infoRows) {
		Collections.sort(infoRows, (row1, row2) -> {
			if (row1.order < row2.order) {
				return -1;
			} else if (row1.order == row2.order) {
				return row1.name.compareTo(row2.name);
			} else {
				return 1;
			}
		});
	}

	@Nullable
	private AmenityInfoRow createLocalizedAmenityInfoRow(@NonNull Context context, @NonNull String key, @NonNull Object vl) {
		Map<String, Object> map = (Map<String, Object>) vl;
		Map<String, String> localizedAdditionalInfo = (Map<String, String>) map.get("localizations");
		if (Algorithms.isEmpty(localizedAdditionalInfo)) {
			return null;
		}
		Collection<String> availableLocales = collectAvailableLocalesFromTags(localizedAdditionalInfo.keySet());
		Locale prefferedLocale = getPreferredLocale(availableLocales);
		String headerKey = prefferedLocale != null ? key + ":" + prefferedLocale.getLanguage() : key;
		String headerValue = localizedAdditionalInfo.get(headerKey);
		if (headerValue == null) {
			Entry<String, String> entry = new ArrayList<>(localizedAdditionalInfo.entrySet()).get(0);
			headerKey = entry.getKey();
			headerValue = entry.getValue();
		}

		CollapsableView collapsableView = null;
		if (!Algorithms.isEmpty(localizedAdditionalInfo)) {
			List<AmenityInfoRow> infoRows = new ArrayList<>();
			for (Entry<String, String> localizedEntry : localizedAdditionalInfo.entrySet()) {
				String localizedKey = localizedEntry.getKey();
				String localizedValue = localizedEntry.getValue();
				if (localizedKey != null && localizedValue != null && !Objects.equals(headerKey, localizedKey)) {
					AmenityInfoRow infoRow = createAmenityInfoRow(context, localizedKey, localizedValue, null);
					if (infoRow != null) {
						infoRows.add(infoRow);
					}
				}
			}
			if (infoRows.size() > 1) {
				sortInfoRows(infoRows);
			}
			LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
			for (AmenityInfoRow infoRow : infoRows) {
				View container = createRowContainer(app, null);
				buildDetailsRow(container, null, infoRow.text, infoRow.textPrefix, null, null, false, null, null);
				llv.addView(container);
			}
			collapsableView = new CollapsableView(llv, this, true);
		}
		return createAmenityInfoRow(context, headerKey, headerValue, collapsableView);
	}

	@Nullable
	private AmenityInfoRow createAmenityInfoRow(@NonNull Context context,
	                                            @NonNull String key, @NonNull String vl,
	                                            @Nullable CollapsableView collapsableView) {
		if (key.startsWith(COLLAPSABLE_PREFIX) || key.startsWith(ALT_NAME_WITH_LANG_PREFIX)) {
			return null;
		}
		if (CollectionUtils.equalsToAny(key, "image", "mapillary", "subway_region")
				|| (key.equals("note") && !osmEditingEnabled)
				|| key.startsWith("lang_yes")) {
			return null;
		}
		String baseKey = key.contains(":") ? key.split(":")[0] : key;

		int iconId = 0;
		int textColor = 0;
		Drawable icon = null;
		String iconIdDescription = null;
		String hiddenUrl = null;
		String textPrefix = "";
		boolean collapsable = collapsableView != null;
		boolean isWikipediaLink = false;
		boolean isWiki = false;
		boolean isText = false;
		boolean isDescription = false;
		boolean needLinks = !(CollectionUtils.equalsToAny(key, OPENING_HOURS, "population", "height")) && !collapsable;
		boolean needIntFormatting = "population".equals(key);
		boolean isPhoneNumber = false;
		boolean isUrl = false;
		int poiTypeOrder = 0;
		String poiTypeKeyName = "";

		poiType = poiCategory.getPoiTypeByKeyName(key);
		AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (pt == null && !Algorithms.isEmpty(vl) && vl.length() < 50) {
			pt = poiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + vl);
		}
		if (poiType == null && pt == null) {
			poiType = poiTypes.getPoiTypeByKey(key);
		}
		PoiType pType = null;
		if (pt != null) {
			pType = (PoiType) pt;
			if (pType.isFilterOnly()) {
				return null;
			}
			poiTypeOrder = pType.getOrder();
			poiTypeKeyName = pType.getKeyName();
		}

		isUrl = isUrl(vl);
		if (key.contains(WIKIPEDIA)) {
			Pair<String, String> wikiParams = WikiAlgorithms.getWikiParams(key, vl);
			vl = wikiParams.first;
			hiddenUrl = wikiParams.second;
			isWikipediaLink = isUrl = true;
		} else if (!isUrl && needLinks) {
			hiddenUrl = getSocialMediaUrl(key, vl);
			if (hiddenUrl != null) {
				isUrl = true;
			}
		}

		if (pType != null && !pType.isText()) {
			String categoryName = pType.getPoiAdditionalCategory();
			if (!Algorithms.isEmpty(categoryName)) {
				List<PoiType> poiAdditionalCategoryTypes = poiAdditionalCategories.get(categoryName);
				if (poiAdditionalCategoryTypes == null) {
					poiAdditionalCategoryTypes = new ArrayList<>();
					poiAdditionalCategories.put(categoryName, poiAdditionalCategoryTypes);
				}
				poiAdditionalCategoryTypes.add(pType);
				return null;
			}
		}

		if (MapObject.isNameLangTag(key)) {
			return null;
		} else if (Amenity.COLLECTION_TIMES.equals(baseKey) || Amenity.SERVICE_TIMES.equals(baseKey)) {
			iconId = R.drawable.ic_action_time;
			needLinks = false;
		} else if (OPENING_HOURS.equals(baseKey)) {
			iconId = R.drawable.ic_action_time;
			collapsableView = getCollapsableTextView(context, true,
					vl.replace("; ", "\n").replace(",", ", "));
			collapsable = true;
			OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(vl);
			if (rs != null) {
				vl = rs.toLocalString();
				Calendar inst = Calendar.getInstance();
				inst.setTimeInMillis(System.currentTimeMillis());
				boolean opened = rs.isOpenedForTime(inst);
				if (opened) {
					textColor = R.color.color_ok;
				} else {
					textColor = R.color.color_invalid;
				}
			}
			vl = vl.replace("; ", "\n");
			needLinks = false;

		} else if (Amenity.PHONE.equals(baseKey)) {
			iconId = R.drawable.ic_action_call_dark;
			isPhoneNumber = true;
		} else if (Amenity.MOBILE.equals(baseKey)) {
			iconId = R.drawable.ic_action_phone;
			isPhoneNumber = true;
		} else if (Amenity.WEBSITE.equals(baseKey)) {
			iconId = R.drawable.ic_world_globe_dark;
			isUrl = true;
		} else if (Amenity.CUISINE.equals(baseKey)) {
			iconId = R.drawable.ic_action_cuisine;
			StringBuilder sb = new StringBuilder();
			for (String c : vl.split(";")) {
				if (sb.length() > 0) {
					sb.append(", ");
					sb.append(poiTypes.getPoiTranslation("cuisine_" + c).toLowerCase());
				} else {
					sb.append(poiTypes.getPoiTranslation("cuisine_" + c));
				}
			}
			textPrefix = app.getString(R.string.poi_cuisine);
			vl = sb.toString();
		} else if (key.contains(Amenity.ROUTE)
				|| key.equals(Amenity.WIKI_PHOTO)
				|| key.equals(Amenity.WIKIDATA)
				|| key.equals(Amenity.WIKIMEDIA_COMMONS)) {
			return null;
		} else {
			if (baseKey.contains(Amenity.DESCRIPTION)) {
				iconId = R.drawable.ic_action_note_dark;
			} else if (isWikipediaLink) {
				iconId = R.drawable.ic_plugin_wikipedia;
			} else if (baseKey.equals("addr:housename") || baseKey.equals("whitewater:rapid_name")) {
				iconId = R.drawable.ic_action_poi_name;
			} else if (baseKey.equals("operator") || baseKey.equals("brand")) {
				iconId = R.drawable.ic_action_poi_brand;
			} else if (baseKey.equals("internet_access_fee_yes")) {
				iconId = R.drawable.ic_action_internet_access_fee;
			} else if (baseKey.equals("instagram")) {
				iconId = R.drawable.ic_action_social_instagram;
			} else {
				iconId = R.drawable.ic_action_info_dark;
			}
			if (pType != null) {
				String cat = pType.getOsmTag().replace(':', '_');
				if (!cat.isEmpty()) {
					int catIconId = app.getResources().getIdentifier("mx_" + cat, "drawable", app.getPackageName());
					iconId = catIconId != 0 ? catIconId : iconId;
				}
				poiTypeOrder = pType.getOrder();
				poiTypeKeyName = pType.getKeyName();
				if (pType.getParentType() != null && pType.getParentType() instanceof PoiType) {
					iconIdDescription = ((PoiType) pType.getParentType()).getOsmTag() + "_" + cat + "_" + pType.getOsmValue();
					icon = getRowIcon(context, iconIdDescription);
				}
				if (!pType.isText()) {
					vl = pType.getTranslation();
				} else {
					isText = true;
					isDescription = iconId == R.drawable.ic_action_note_dark;
					textPrefix = pType.getTranslation();
					if (needIntFormatting) {
						vl = getFormattedInt(vl);
					}
				}
				if (!isDescription && icon == null) {
					iconIdDescription = pType.getIconKeyName();
					icon = getRowIcon(context, iconIdDescription);
					if (isText && icon != null) {
						textPrefix = "";
					}
				}
				if (icon == null && isText && iconId == 0) {
					iconId = R.drawable.ic_action_note_dark;
				}
			} else if (poiType != null) {
				String catKey = poiType.getCategory().getKeyName();
				if (MapPoiTypes.OTHER_MAP_CATEGORY.equals(catKey)) {
					return null; // the "Others" value is already displayed as a title
				}
				List<PoiType> list = collectedPoiTypes.computeIfAbsent(catKey, s -> new ArrayList<>());
				list.add(poiType);
			} else if (baseKey.startsWith(US_MAPS_RECREATION_AREA)) {
				String translatedUsMapsKey = app.getPoiTypes().getPoiTranslator().getTranslation(baseKey);
				if (!Algorithms.isEmpty(translatedUsMapsKey)) {
					textPrefix = translatedUsMapsKey;
				} else {
					textPrefix = Algorithms.capitalizeFirstLetterAndLowercase(key);
				}
			} else {
				return null; // skip non-translatable NON-poiType tags
			}
		}

		String[] formattedPrefixAndText = getFormattedPrefixAndText(key, textPrefix, vl, subtype);
		textPrefix = formattedPrefixAndText[0];
		vl = formattedPrefixAndText[1];

		if ("ele".equals(key)) {
			try {
				float distance = Float.parseFloat(vl);
				vl = OsmAndFormatter.getFormattedAlt(distance, app, metricSystem);
				String collapsibleVal;
				if (metricSystem == MILES_AND_FEET || metricSystem == MILES_AND_YARDS || metricSystem == NAUTICAL_MILES_AND_FEET) {
					collapsibleVal = OsmAndFormatter.getFormattedAlt(distance, app, KILOMETERS_AND_METERS);
				} else {
					collapsibleVal = OsmAndFormatter.getFormattedAlt(distance, app, MILES_AND_FEET);
				}
				Set<String> elevationData = new HashSet<>();
				elevationData.add(collapsibleVal);
				collapsableView = getDistanceCollapsableView(elevationData);
				collapsable = true;
			} catch (NumberFormatException ex) {
				LOG.error(ex);
			}
		}

		lastBuiltRowIsDescription = isDescription;
		boolean matchWidthDivider = !isDescription && isWiki;
		AmenityInfoRow row;
		if (isDescription) {
			row = new AmenityInfoRow(key, R.drawable.ic_action_note_dark, textPrefix,
					vl, null, collapsable, collapsableView, 0, false,
					true, true, 0, "", false, false, matchWidthDivider, 0);
		} else if (icon != null) {
			row = new AmenityInfoRow(key, icon, textPrefix, vl, hiddenUrl, collapsable,
					collapsableView, textColor, isWiki, isText, needLinks, poiTypeOrder,
					poiTypeKeyName, isPhoneNumber, isUrl, matchWidthDivider, 0, iconIdDescription);
		} else {
			row = new AmenityInfoRow(key, iconId, textPrefix, vl, hiddenUrl, collapsable,
					collapsableView, textColor, isWiki, isText, needLinks, poiTypeOrder,
					poiTypeKeyName, isPhoneNumber, isUrl, matchWidthDivider, 0);
		}
		return row;
	}

	public void buildNamesRow(ViewGroup viewGroup, Map<String, String> namesMap, boolean altName) {
		if (!namesMap.isEmpty()) {
			Locale nameLocale = getPreferredLocale(namesMap.keySet());
			if (nameLocale == null) {
				String localeId = (String) namesMap.keySet().toArray()[0];
				nameLocale = new Locale(localeId);
			}
			String name = namesMap.get(nameLocale.getLanguage());

			Context context = viewGroup.getContext();
			View amenitiesRow = createRowContainer(context, altName ? ALT_NAMES_ROW_KEY : NAMES_ROW_KEY);
			String hint = app.getString(altName ? R.string.shared_string_alt_name : R.string.shared_string_name);
			buildDetailsRow(amenitiesRow, getRowIcon(R.drawable.ic_action_map_language), name,
					app.getString(R.string.ltr_or_rtl_combine_via_colon, hint, nameLocale.getDisplayLanguage()), null,
					namesMap.size() > 1 ? getNamesCollapsableView(namesMap, nameLocale.getLanguage(), hint) : null, true, null, "ic_action_map_language");
			int viewGroupChildCount = viewGroup.getChildCount();
			if (viewGroupChildCount > 0 && !isDividerAtPosition(viewGroup, viewGroupChildCount - 1)) {
				buildRowDivider(viewGroup, viewGroupChildCount);
			}
			viewGroup.addView(amenitiesRow);
		}
	}

	protected CollapsableView getNamesCollapsableView(@NonNull Map<String, String> mapNames,
	                                                  @Nullable String excludedLanguageKey,
	                                                  @NonNull String hint) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (int i = 0; i < mapNames.size(); i++) {
			String key = (String) mapNames.keySet().toArray()[i];
			if (!key.equals(excludedLanguageKey)) {
				Locale locale = new Locale(key);
				String name = mapNames.get(key);

				View amenitiesRow = createRowContainer(mapActivity, null);
				buildDetailsRow(amenitiesRow, null, name,
						app.getString(R.string.ltr_or_rtl_combine_via_colon, hint, locale.getDisplayLanguage()),
						null, null, false, null, null);
				llv.addView(amenitiesRow);
			}
		}
		return new CollapsableView(llv, this, true);
	}

	private void sortDescriptionRows(@NonNull List<AmenityInfoRow> descriptions) {
		String langSuffix = ":" + getPreferredMapAppLang();
		AmenityInfoRow descInPrefLang = null;
		for (AmenityInfoRow desc : descriptions) {
			if (desc.key.length() > langSuffix.length() && desc.key.endsWith(langSuffix)) {
				descInPrefLang = desc;
				break;
			}
		}
		if (descInPrefLang != null) {
			descriptions.remove(descInPrefLang);
			descriptions.add(0, descInPrefLang);
		}
	}

	@Nullable
	private String getSocialMediaUrl(String key, String value) {
		// Remove leading and closing slashes
		value = value.trim();
		if (Algorithms.isEmpty(value)) {
			return null;
		}
		StringBuilder sb = new StringBuilder(value);
		if (sb.charAt(0) == '/') {
			sb.deleteCharAt(0);
		}
		int lastIdx = sb.length() - 1;
		if (sb.charAt(lastIdx) == '/') {
			sb.deleteCharAt(lastIdx);
		}

		// It cannot be username
		if (PatternsCompat.AUTOLINK_WEB_URL.matcher(sb.toString()).matches()) {
			return "https://" + value;
		}

		Map<String, String> urls = new HashMap<>(7);
		urls.put("facebook", "https://facebook.com/%s");
		urls.put("vk", "https://vk.com/%s");
		urls.put("instagram", "https://instagram.com/%s");
		urls.put("twitter", "https://twitter.com/%s");
		urls.put("ok", "https://ok.ru/%s");
		urls.put("telegram", "https://t.me/%s");
		urls.put("flickr", "https://flickr.com/%s");
		urls.put("wikidata", WIKI_DATA_BASE_URL + "%s");

		String url = urls.get(key);
		if (url != null) {
			return String.format(url, value);
		}
		return null;
	}

	private String getFormattedInt(String value) {
		try {
			int number = Integer.parseInt(value);
			DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
			DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
			symbols.setGroupingSeparator(' ');
			formatter.setDecimalFormatSymbols(symbols);
			return formatter.format(number);
		} catch (NumberFormatException e) {
			return value;
		}
	}

	private String[] getFormattedPrefixAndText(String key, String prefix, String value, String subtype) {
		DISTANCE_FORMAT.setRoundingMode(RoundingMode.CEILING);
		String formattedValue = value;
		String formattedPrefix = prefix;
		boolean formatted = true;
		switch (key) {
			case "width":
			case "height":
				if (key.equals("width")) {
					formattedPrefix = app.getString(R.string.shared_string_width);
				} else {
					formattedPrefix = app.getString(R.string.shared_string_height);
				}
			case "depth":
			case "seamark_height":
				double valueAsDouble = Algorithms.parseDoubleSilently(value, 0);
				if (valueAsDouble > 0) {
					if (metricSystem == MILES_AND_FEET || metricSystem == NAUTICAL_MILES_AND_FEET) {
						formattedValue = DISTANCE_FORMAT.format(valueAsDouble * FEET_IN_ONE_METER) + " " + app.getString(R.string.foot);
					} else if (metricSystem == MILES_AND_YARDS) {
						formattedValue = DISTANCE_FORMAT.format(valueAsDouble * YARDS_IN_ONE_METER) + " " + app.getString(R.string.yard);
					} else {
						formattedValue = value + " " + app.getString(R.string.m);
					}
				}
				break;
			case "distance":
				try {
					float valueAsFloatInMeters = Float.parseFloat(value) * 1000;
					if (metricSystem == KILOMETERS_AND_METERS) {
						formattedValue = value + " " + app.getString(R.string.km);
					} else {
						formattedValue = OsmAndFormatter.getFormattedDistance(valueAsFloatInMeters, app);
					}
					formattedPrefix = formatPrefix(prefix, app.getString(R.string.distance));
				} catch (RuntimeException e) {
					LOG.error(e.getMessage(), e);
				}
				break;
			case "capacity":
				if (subtype.equals("water_tower") || subtype.equals("storage_tank")) {
					formattedValue = value + " " + app.getString(R.string.cubic_m);
				}
				break;
			case "maxweight":
				if (Algorithms.isInt(value)) {
					formattedValue = value + " " + app.getString(R.string.metric_ton);
				}
				break;
			case "students":
			case "spots":
			case "seats":
				if (Algorithms.isInt(value)) {
					formattedPrefix = formatPrefix(prefix, app.getString(R.string.shared_string_capacity));
				}
				break;
			case "wikipedia":
				formattedPrefix = app.getString(R.string.shared_string_wikipedia);
				break;
			default:
				formatted = false;
		}
		if (!formatted && formattedPrefix.contains(" (")) {
			String[] prefixParts = formattedPrefix.split(" [(]");
			if (prefixParts.length == 2) {
				formattedPrefix = app.getString(R.string.ltr_or_rtl_combine_via_colon, prefixParts[0],
						Algorithms.capitalizeFirstLetterAndLowercase(prefixParts[1]).replaceAll("[()]", ""));
			}
		}
		return new String[] {formattedPrefix, formattedValue};
	}

	private String formatPrefix(String prefix, String units) {
		return (!prefix.isEmpty()) ? (prefix + ", " + units) : units;
	}

	private void buildRow(View view, int iconId, String text, String textPrefix, String hiddenUrl,
	                      boolean collapsable, CollapsableView collapsableView,
	                      int textColor, boolean isWiki, boolean isText, boolean needLinks,
	                      boolean isPhoneNumber, boolean isUrl, boolean matchWidthDivider, int textLinesLimit) {
		String iconIdDescription = iconId > 0 ? app.getResources().getResourceEntryName(iconId) : null;
		buildRow(view, iconId == 0 ? null : getRowIcon(iconId), text, textPrefix, hiddenUrl,
				collapsable, collapsableView, textColor,
				isWiki, isText, needLinks, isPhoneNumber, isUrl, matchWidthDivider, textLinesLimit, iconIdDescription);
	}

	protected void buildRow(View view, Drawable icon, String text, String textPrefix,
	                        String hiddenUrl, boolean collapsable,
	                        CollapsableView collapsableView, int textColor, boolean isWiki,
	                        boolean isText, boolean needLinks, boolean isPhoneNumber, boolean isUrl,
	                        boolean matchWidthDivider, int textLinesLimit, @Nullable String iconIdDescription) {
		boolean light = isLightContent();

		if (!isFirstRow()) {
			buildRowDivider(view);
		}

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(v -> {
			String textToCopy;
			if (hiddenUrl != null && hiddenUrl.contains(WIKI_LINK)) {
				textToCopy = hiddenUrl;
			} else if (hiddenUrl != null && hiddenUrl.contains(WIKI_DATA_BASE_URL)) {
				textToCopy = text;
			} else {
				textToCopy = !Algorithms.isEmpty(textPrefix) ? textPrefix + ": " + text : text;
			}
			copyToClipboard(textToCopy, view.getContext());
			return true;
		});

		baseView.addView(ll);

		// Icon
		if (icon != null) {
			LinearLayout llIcon = new LinearLayout(view.getContext());
			llIcon.setOrientation(LinearLayout.HORIZONTAL);
			llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(64f), dpToPx(48f)));
			llIcon.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIcon);

			ImageView iconView = new ImageView(view.getContext());
			LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconParams, dpToPx(16f), dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconParams.gravity = Gravity.CENTER_VERTICAL;
			iconView.setLayoutParams(llIconParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconView.setTag(R.id.testId, iconIdDescription);
			iconView.setImageDrawable(icon);
			llIcon.addView(iconView);
		}

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textPrefixView = null;
		if (!Algorithms.isEmpty(textPrefix)) {
			textPrefixView = new TextView(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			AndroidUtils.setMargins(llTextParams, icon == null ? dpToPx(16f) : 0, dpToPx(8f), 0, 0);
			textPrefixView.setLayoutParams(llTextParams);
			textPrefixView.setTextSize(12);
			textPrefixView.setTextColor(getColor(R.color.text_color_secondary_light));
			textPrefixView.setEllipsize(TextUtils.TruncateAt.END);
			textPrefixView.setMinLines(1);
			textPrefixView.setMaxLines(1);
			textPrefixView.setText(textPrefix);
		}

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextParams, icon == null ? dpToPx(16f) : 0,
				textPrefixView == null ? (collapsable ? dpToPx(13f) : dpToPx(8f)) : dpToPx(2f), 0, collapsable && textPrefixView == null ? dpToPx(13f) : dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));

		int linkTextColor = ContextCompat.getColor(view.getContext(), light ? R.color.active_color_primary_light : R.color.active_color_primary_dark);

		if (isPhoneNumber || isUrl) {
			textView.setTextColor(linkTextColor);
			needLinks = false;
		}
		textView.setText(text);
		if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(textView, Linkify.ALL)) {
			textView.setMovementMethod(null);
			textView.setLinkTextColor(linkTextColor);
			textView.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(textView);
		}
		textView.setEllipsize(TextUtils.TruncateAt.END);
		if (textLinesLimit > 0) {
			textView.setMinLines(1);
			textView.setMaxLines(textLinesLimit);
		} else if (isWiki) {
			textView.setMinLines(1);
			textView.setMaxLines(15);
		} else if (isText) {
			textView.setMinLines(1);
			textView.setMaxLines(10);
		}
		if (textColor > 0) {
			textView.setTextColor(getColor(textColor));
		}

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		AndroidUtils.setMargins(llTextViewParams, 0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		if (textPrefixView != null) {
			llText.addView(textPrefixView);
		}
		llText.addView(textView);

		ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsable && collapsableView != null) {
			// Icon
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40f), ViewGroup.LayoutParams.MATCH_PARENT));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconCollapseParams, 0, dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContentView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(v -> {
				if (collapsableView.getContentView().getVisibility() == View.VISIBLE) {
					collapsableView.getContentView().setVisibility(View.GONE);
					iconViewCollapse.setImageDrawable(getCollapseIcon(true));
					collapsableView.setCollapsed(true);
				} else {
					collapsableView.getContentView().setVisibility(View.VISIBLE);
					iconViewCollapse.setImageDrawable(getCollapseIcon(false));
					collapsableView.setCollapsed(false);
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContentView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			baseView.addView(collapsableView.getContentView());
		}

		if (isWiki) {
			buildReadFullButton(llText, app.getString(R.string.context_menu_read_full_article), v -> {
				WikipediaDialogFragment.showInstance(mapActivity, wikiAmenity, null);
			});
		}

		((LinearLayout) view).addView(baseView);

		if (isPhoneNumber) {
			ll.setOnClickListener(v -> {
				if (customization.isFeatureEnabled(CONTEXT_MENU_PHONE_ID)) {
					showDialog(text, Intent.ACTION_DIAL, "tel:", v);
				}
			});
		} else if (isUrl) {
			ll.setOnClickListener(v -> {
				if (customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					String url = hiddenUrl == null ? text : hiddenUrl;
					if (url.contains(WIKI_LINK)) {
						LatLon location = wikiAmenity != null ? wikiAmenity.getLocation() : getLatLon();
						WikiArticleHelper.askShowArticle(mapActivity, !light, location, url);
					} else {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url));
						AndroidUtils.startActivityIfSafe(v.getContext(), intent);
					}
				}
			});
		} else if (isWiki) {
			ll.setOnClickListener(v -> WikipediaDialogFragment.showInstance(mapActivity, wikiAmenity, null));
		} else if (isText && text.length() > 200) {
			ll.setOnClickListener(v -> POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, text, textPrefix));
		}

		rowBuilt();

		setDividerWidth(matchWidthDivider);
	}

	public void buildAmenityRow(View view, AmenityInfoRow info) {
		if (info.icon != null) {
			buildRow(view, info.icon, info.text, info.textPrefix, info.hiddenUrl,
					info.collapsable, info.collapsableView, info.textColor, info.isWiki, info.isText,
					info.needLinks, info.isPhoneNumber,
					info.isUrl, info.matchWidthDivider, info.textLinesLimit, info.iconIdDescription);
		} else {
			buildRow(view, info.iconId, info.text, info.textPrefix, info.hiddenUrl,
					info.collapsable, info.collapsableView, info.textColor, info.isWiki, info.isText,
					info.needLinks, info.isPhoneNumber,
					info.isUrl, info.matchWidthDivider, info.textLinesLimit);
		}
	}

	private CollapsableView getPoiTypeCollapsableView(Context context, boolean collapsed,
	                                                  @NonNull List<PoiType> categoryTypes,
	                                                  boolean poiAdditional, AmenityInfoRow textRow, PoiCategory type) {

		List<TextViewEx> buttons = new ArrayList<>();

		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		for (PoiType pt : categoryTypes) {
			TextViewEx button = buildButtonInCollapsableView(context, false, false);
			String name = pt.getTranslation();
			button.setText(name);
			PoiCategory category = pt.getCategory() != null ? pt.getCategory() : type;

			button.setOnClickListener(v -> {
				if (category != null) {
					PoiUIFilter filter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + category.getKeyName());
					if (filter != null) {
						filter.clearFilter();
						if (poiAdditional) {
							filter.setTypeToAccept(category, true);
							filter.updateTypesToAccept(pt);
							filter.setFilterByName(pt.getKeyName().replace('_', ':').toLowerCase());
						} else {
							LinkedHashSet<String> accept = new LinkedHashSet<>();
							accept.add(pt.getKeyName());
							filter.selectSubTypesToAccept(category, accept);
						}
						getMapActivity().getFragmentsHelper().showQuickSearch(filter);
					}
				}
			});
			buttons.add(button);
			if (buttons.size() > 3 && categoryTypes.size() > 4) {
				button.setVisibility(View.GONE);
			}
			view.addView(button);
		}

		if (textRow != null) {
			TextViewEx button = buildButtonInCollapsableView(context, true, false, false);
			String name = textRow.textPrefix + ": " + textRow.text.toLowerCase();
			button.setText(name);
			view.addView(button);
		}

		if (categoryTypes.size() > 4) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(v -> {
				for (TextViewEx b : buttons) {
					if (b.getVisibility() != View.VISIBLE) {
						b.setVisibility(View.VISIBLE);
					}
				}
				button.setVisibility(View.GONE);
				notifyCollapseExpand(false);
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}

	@NonNull
	public static Set<String> collectAvailableLocalesFromTags(@NonNull Collection<String> tags) {
		Set<String> result = new HashSet<>();
		for (String tag : tags) {
			String[] parts = tag.split(":");
			String locale = parts.length > 1 ? parts[1] : "en";
			if (locale != null) {
				result.add(locale);
			}
		}
		return result;
	}

	@Nullable
	private Locale getPreferredLocale(Collection<String> locales) {
		return LocaleHelper.getPreferredNameLocale(app, locales);
	}


	@Nullable
	public static Pair<String, Locale> getDescriptionWithPreferredLang(@NonNull OsmandApplication app,
			@NonNull Amenity amenity, @NonNull String key, @NonNull Map<String, Object> map) {
		Object object = map.get(key);
		if (object instanceof Map<?, ?>) {
			Map<String, Object> descriptions = (Map<String, Object>) object;
			Map<String, String> localizations = (Map<String, String>) descriptions.get("localizations");
			Collection<String> locales = AmenityUIHelper.collectAvailableLocalesFromTags(localizations.keySet());

			Locale locale = LocaleHelper.getPreferredNameLocale(app, locales);
			String localeKey = locale != null ? key + ":" + locale.getLanguage() : key;

			String description = localizations.get(localeKey);
			if (description == null && locale != null && Algorithms.stringsEqual(locale.getLanguage(), "en")) {
				description = localizations.get(key);
			}
			if (description == null) {
				Map.Entry<String, String> entry = new ArrayList<>(localizations.entrySet()).get(0);
				description = entry.getValue();
			}
			return Pair.create(description, locale);
		}
		String description = amenity.getAdditionalInfo(key);
		if (!Algorithms.isEmpty(description)) {
			return Pair.create(description, null);
		}
		return null;
	}
}