package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_PHONE_ID;
import static net.osmand.data.Amenity.ALT_NAME_WITH_LANG_PREFIX;
import static net.osmand.gpx.GPXUtilities.ADDRESS_EXTENSION;
import static net.osmand.gpx.GPXUtilities.AMENITY_ORIGIN_EXTENSION;
import static net.osmand.gpx.GPXUtilities.AMENITY_PREFIX;
import static net.osmand.gpx.GPXUtilities.BACKGROUND_TYPE_EXTENSION;
import static net.osmand.gpx.GPXUtilities.COLOR_NAME_EXTENSION;
import static net.osmand.gpx.GPXUtilities.ICON_NAME_EXTENSION;
import static net.osmand.gpx.GPXUtilities.PROFILE_TYPE_EXTENSION;
import static net.osmand.plus.settings.enums.MetricsConstants.KILOMETERS_AND_METERS;
import static net.osmand.plus.settings.enums.MetricsConstants.MILES_AND_FEET;
import static net.osmand.plus.settings.enums.MetricsConstants.MILES_AND_YARDS;
import static net.osmand.plus.settings.enums.MetricsConstants.NAUTICAL_MILES_AND_FEET;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKIPEDIA;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKI_LINK;
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

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class AmenityUIHelper extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);

	public static final String COLLAPSABLE_PREFIX = "collapsable_";
	public static final List<String> HIDING_EXTENSIONS_AMENITY_TAGS = Arrays.asList("phone", "website");

	private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#.##");

	private final MetricsConstants metricSystem;
	private final Map<String, String> additionalInfo;

	private String preferredLang;
	private Amenity wikiAmenity;

	public static final List<String> HIDDEN_EXTENSIONS = Arrays.asList(COLOR_NAME_EXTENSION,
			ICON_NAME_EXTENSION, BACKGROUND_TYPE_EXTENSION, PROFILE_TYPE_EXTENSION, ADDRESS_EXTENSION,
			AMENITY_ORIGIN_EXTENSION, AMENITY_PREFIX + Amenity.NAME, AMENITY_PREFIX + Amenity.TYPE,
			AMENITY_PREFIX + Amenity.SUBTYPE);

	public AmenityUIHelper(@NonNull MapActivity mapActivity, String preferredLang, Map<String, String> additionalInfo) {
		super(mapActivity);
		this.preferredLang = preferredLang;
		this.additionalInfo = additionalInfo;
		this.metricSystem = mapActivity.getMyApplication().getSettings().METRIC_SYSTEM.get();
	}

	public void setPreferredLang(String lang) {
		this.preferredLang = lang;
	}

	@Override
	public void buildInternal(View view) {
		PoiCategory type = null;
		String typeTag = getAdditionalInfo(AMENITY_PREFIX + Amenity.TYPE);
		if (!Algorithms.isEmpty(typeTag)) {
			type = MapPoiTypes.getDefault().getPoiCategoryByName(typeTag);
		}
		if (type == null) {
			type = MapPoiTypes.getDefault().getOtherPoiCategory();
		}

		String subtype = getAdditionalInfo(AMENITY_PREFIX + Amenity.SUBTYPE);

		boolean hasWiki = false;
		MapPoiTypes poiTypes = app.getPoiTypes();
		List<AmenityInfoRow> infoRows = new LinkedList<>();
		List<AmenityInfoRow> descriptions = new LinkedList<>();

		Map<String, List<PoiType>> poiAdditionalCategories = new HashMap<>();
		AmenityInfoRow cuisineRow = null;
		List<PoiType> collectedPoiTypes = new ArrayList<>();

		boolean osmEditingEnabled = PluginsHelper.isActive(OsmEditingPlugin.class);

		Map<String, String> additionalInfoFiltered = new HashMap<>();
		for (String origKey : getAdditionalInfoKeys()) {
			String key = origKey;
			if (origKey.equals(AMENITY_PREFIX + Amenity.OPENING_HOURS)) {
				key = origKey.replace(AMENITY_PREFIX, "");
			} else if (origKey.startsWith(AMENITY_PREFIX)) {
				continue;
			} else {
				key = origKey.replace(GPXUtilities.OSM_PREFIX, "");
			}
			if (HIDDEN_EXTENSIONS.contains(key)) {
				continue;
			}
			additionalInfoFiltered.put(key, getAdditionalInfo(origKey));
		}


		for (Map.Entry<String, String> e : additionalInfoFiltered.entrySet()) {
			String key = e.getKey();
			String vl = e.getValue();

			if (key.startsWith(COLLAPSABLE_PREFIX) || key.startsWith(ALT_NAME_WITH_LANG_PREFIX)) {
				continue;
			}
			if (key.equals("image")
					|| key.equals("mapillary")
					|| key.equals("subway_region")
					|| (key.equals("note") && !osmEditingEnabled)
					|| key.startsWith("lang_yes")) {
				continue;
			}

			int iconId = 0;
			int textColor = 0;
			Drawable icon = null;
			String hiddenUrl = null;
			String textPrefix = "";
			CollapsableView collapsableView = null;
			boolean collapsable = false;
			boolean isWikipediaLink = false;
			boolean isWiki = false;
			boolean isText = false;
			boolean isDescription = false;
			boolean needLinks = !(CollectionUtils.equalsToAny(key, Amenity.OPENING_HOURS, "population", "height"));
			boolean needIntFormatting = "population".equals(key);
			boolean isPhoneNumber = false;
			boolean isUrl = false;
			boolean isCuisine = false;
			int poiTypeOrder = 0;
			String poiTypeKeyName = "";

			PoiType poiType = type.getPoiTypeByKeyName(key);
			AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
			if (pt == null && !Algorithms.isEmpty(vl) && vl.length() < 50) {
				pt = poiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + vl);
			}
			PoiType pType = null;
			if (pt != null) {
				pType = (PoiType) pt;
				if (pType.isFilterOnly()) {
					continue;
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
					continue;
				}
			}

			if (type.isWiki()) {
				if (!hasWiki) {
					wikiAmenity = new Amenity();
					wikiAmenity.setType(type);
					wikiAmenity.setSubType(subtype);
					wikiAmenity.setAdditionalInfo(additionalInfoFiltered);
					wikiAmenity.setLocation(getLatLon());
					String name = additionalInfoFiltered.get("name");
					if (Algorithms.isEmpty(name)) {
						wikiAmenity.setName(name);
					}

					String articleLang = PluginsHelper.onGetMapObjectsLocale(wikiAmenity, preferredLang);
					String lng = wikiAmenity.getContentLanguage("content", articleLang, "en");
					if (Algorithms.isEmpty(lng)) {
						lng = "en";
					}

					String langSelected = lng;
					String content = wikiAmenity.getDescription(langSelected);
					vl = (content != null) ? WikiArticleHelper.getPartialContent(content) : "";
					vl = vl == null ? "" : vl;
					hasWiki = true;
					isWiki = true;
					needLinks = false;
					hiddenUrl = null;
					isUrl = false;
				} else {
					continue;
				}
			} else if (key.startsWith("name:")) {
				continue;
			} else if (Amenity.COLLECTION_TIMES.equals(key) || Amenity.SERVICE_TIMES.equals(key)) {
				iconId = R.drawable.ic_action_time;
				needLinks = false;
			} else if (Amenity.OPENING_HOURS.equals(key)) {
				iconId = R.drawable.ic_action_time;
				collapsableView = getCollapsableTextView(view.getContext(), true,
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

			} else if (Amenity.PHONE.equals(key)) {
				iconId = R.drawable.ic_action_call_dark;
				isPhoneNumber = true;
			} else if (Amenity.MOBILE.equals(key)) {
				iconId = R.drawable.ic_action_phone;
				isPhoneNumber = true;
			} else if (Amenity.WEBSITE.equals(key)) {
				iconId = R.drawable.ic_world_globe_dark;
				isUrl = true;
			} else if (Amenity.CUISINE.equals(key)) {
				isCuisine = true;
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
					|| key.equals(Amenity.WIKIDATA)
					|| key.equals(Amenity.WIKIMEDIA_COMMONS)) {
				continue;
			} else {
				if (key.contains(Amenity.DESCRIPTION)) {
					iconId = R.drawable.ic_action_note_dark;
				} else if (isWikipediaLink) {
					iconId = R.drawable.ic_plugin_wikipedia;
				} else if (key.equals("addr:housename") || key.equals("whitewater:rapid_name")) {
					iconId = R.drawable.ic_action_poi_name;
				} else if (key.equals("operator") || key.equals("brand")) {
					iconId = R.drawable.ic_action_poi_brand;
				} else if (key.equals("internet_access_fee_yes")) {
					iconId = R.drawable.ic_action_internet_access_fee;
				} else if (key.equals("instagram")) {
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
						icon = getRowIcon(view.getContext(), ((PoiType) pType.getParentType()).getOsmTag() + "_" + cat + "_" + pType.getOsmValue());
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
						icon = getRowIcon(view.getContext(), pType.getIconKeyName());
						if (isText && icon != null) {
							textPrefix = "";
						}
					}
					if (icon == null && isText && iconId == 0) {
						iconId = R.drawable.ic_action_note_dark;
					}
				} else if (poiType != null) {
					collectedPoiTypes.add(poiType);
				} else {
					textPrefix = Algorithms.capitalizeFirstLetterAndLowercase(key);
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

			boolean matchWidthDivider = !isDescription && isWiki;
			AmenityInfoRow row;
			if (isDescription) {
				row = new AmenityInfoRow(key, R.drawable.ic_action_note_dark, textPrefix,
						vl, null, collapsable, collapsableView, 0, false,
						true, true, 0, "", false, false, matchWidthDivider, 0);
			} else if (icon != null) {
				row = new AmenityInfoRow(key, icon, textPrefix, vl, hiddenUrl, collapsable,
						collapsableView, textColor, isWiki, isText, needLinks, poiTypeOrder,
						poiTypeKeyName, isPhoneNumber, isUrl, matchWidthDivider, 0);
			} else {
				row = new AmenityInfoRow(key, iconId, textPrefix, vl, hiddenUrl, collapsable,
						collapsableView, textColor, isWiki, isText, needLinks, poiTypeOrder,
						poiTypeKeyName, isPhoneNumber, isUrl, matchWidthDivider, 0);
			}
			if (isDescription) {
				descriptions.add(row);
			} else if (isCuisine) {
				cuisineRow = row;
			} else if (poiType == null) {
				infoRows.add(row);
			}
		}

		if (cuisineRow != null) {
			boolean hasCuisineOrDish = getAdditionalInfoKeys().contains(COLLAPSABLE_PREFIX + "cuisine") ||
					getAdditionalInfoKeys().contains(COLLAPSABLE_PREFIX + "dish");
			if (!hasCuisineOrDish) {
				infoRows.add(cuisineRow);
			}
		}


		for (Map.Entry<String, String> e : additionalInfoFiltered.entrySet()) {
			if (e.getKey().startsWith(COLLAPSABLE_PREFIX)) {
				List<PoiType> categoryTypes = new ArrayList<>();

				if (!Algorithms.isEmpty(e.getValue())) {
					StringBuilder sb = new StringBuilder();
					List<String> records = new ArrayList<String>(Arrays.asList(e.getValue().split(Amenity.SEPARATOR)));
					for (String record : records) {
						AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(record);
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
					icon = getRowIcon(view.getContext(), poiAdditionalIconName);
					if (icon == null) {
						icon = getRowIcon(view.getContext(), poiAdditionalCategoryName);
					}
					if (icon == null) {
						icon = getRowIcon(view.getContext(), pType.getIconKeyName());
					}
					if (icon == null) {
						icon = getRowIcon(R.drawable.ic_action_note_dark);
					}
					boolean cuisineOrDish = CollectionUtils.equalsToAny(e.getKey(), Amenity.CUISINE, Amenity.DISH);
					CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true,
							categoryTypes, true, cuisineOrDish ? cuisineRow : null, type);
					infoRows.add(new AmenityInfoRow(poiAdditionalCategoryName, icon,
							pType.getPoiAdditionalCategoryTranslation(), sb.toString(), null,
							true, collapsableView, 0, false, false,
							false, pType.getOrder(), pType.getKeyName(), false,
							false, false, 1));
				}
			}
		}


		if (collectedPoiTypes.size() > 0) {
			CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true, collectedPoiTypes, false, null, type);
			PoiCategory poiCategory = type;
			Drawable icon = getRowIcon(view.getContext(), poiCategory.getIconKeyName());
			StringBuilder sb = new StringBuilder();
			for (PoiType pt : collectedPoiTypes) {
				if (sb.length() > 0) {
					sb.append(" • ");
				}
				sb.append(pt.getTranslation());
			}

			infoRows.add(new AmenityInfoRow(poiCategory.getKeyName(), icon,
					poiCategory.getTranslation(), sb.toString(), null, true,
					collapsableView, 0, false, false, false, 40,
					poiCategory.getKeyName(), false, false, false, 1));
		}


		Collections.sort(infoRows, (row1, row2) -> {
			if (row1.order < row2.order) {
				return -1;
			} else if (row1.order == row2.order) {
				return row1.name.compareTo(row2.name);
			} else {
				return 1;
			}
		});

		for (AmenityInfoRow info : infoRows) {
			buildAmenityRow(view, info);
		}


		String preferredLang = getPreferredMapAppLang();
		String langSuffix = ":" + preferredLang;
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
		for (AmenityInfoRow info : descriptions) {
			buildAmenityRow(view, info);
		}
	}

	@Nullable
	private String getSocialMediaUrl(String key, String value) {
		// Remove leading and closing slashes
		StringBuilder sb = new StringBuilder(value.trim());
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

		if (urls.containsKey(key)) {
			return String.format(urls.get(key), value);
		} else {
			return null;
		}
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
				try {
					double valueAsDouble = Double.parseDouble(value);
					if (metricSystem == MILES_AND_FEET || metricSystem == NAUTICAL_MILES_AND_FEET) {
						formattedValue = DISTANCE_FORMAT.format(valueAsDouble * FEET_IN_ONE_METER) + " " + app.getString(R.string.foot);
					} else if (metricSystem == MILES_AND_YARDS) {
						formattedValue = DISTANCE_FORMAT.format(valueAsDouble * YARDS_IN_ONE_METER) + " " + app.getString(R.string.yard);
					} else {
						formattedValue = value + " " + app.getString(R.string.m);
					}
				} catch (RuntimeException e) {
					LOG.error(e.getMessage(), e);
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
		buildRow(view, iconId == 0 ? null : getRowIcon(iconId), text, textPrefix, hiddenUrl,
				collapsable, collapsableView, textColor,
				isWiki, isText, needLinks, isPhoneNumber, isUrl, matchWidthDivider, textLinesLimit);
	}

	protected void buildRow(View view, Drawable icon, String text, String textPrefix,
	                        String hiddenUrl, boolean collapsable,
	                        CollapsableView collapsableView, int textColor, boolean isWiki,
	                        boolean isText, boolean needLinks, boolean isPhoneNumber, boolean isUrl,
	                        boolean matchWidthDivider, int textLinesLimit) {

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
				WikipediaDialogFragment.showInstance(mapActivity, wikiAmenity);
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
			ll.setOnClickListener(v -> WikipediaDialogFragment.showInstance(mapActivity, wikiAmenity));
		} else if (isText && text.length() > 200) {
			ll.setOnClickListener(v -> POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, text, textPrefix));
		}

		rowBuilt();

		setDividerWidth(matchWidthDivider);
	}

	public void buildNamesRow(@NonNull View view, @Nullable Drawable icon, String text, String textPrefix,
	                          @Nullable CollapsableView collapsableView, boolean parentRow) {
		if (!isFirstRow() && !parentRow) {
			View horizontalLine = new View(view.getContext());
			horizontalLine.setTag(DIVIDER_ROW_KEY);
			LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
			llHorLineParams.gravity = Gravity.BOTTOM;
			AndroidUtils.setMargins(llHorLineParams, icon != null ? dpToPx(64f) : 0, 0, 0, 0);

			horizontalLine.setLayoutParams(llHorLineParams);
			horizontalLine.setBackgroundColor(getColor(light ? R.color.divider_color_light : R.color.divider_color_dark));
			((LinearLayout) view).addView(horizontalLine);
		}
		boolean collapsable = collapsableView != null;

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
			copyToClipboard(text, view.getContext());
			return true;
		});

		baseView.addView(ll);

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
			iconView.setImageDrawable(icon);
			llIcon.addView(iconView);
		}

		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textPrefixView = null;
		if (!Algorithms.isEmpty(textPrefix)) {
			textPrefixView = new TextView(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			int topMargin = parentRow ? (icon == null ? dpToPx(16f) : 0) : 0;
			AndroidUtils.setMargins(llTextParams, topMargin, dpToPx(8f), 0, 0);
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
		int topMargin = parentRow ? (icon == null ? dpToPx(16f) : 0) : 0;
		AndroidUtils.setMargins(llTextParams, topMargin,
				textPrefixView == null ? (collapsable ? dpToPx(13f) : dpToPx(8f)) : dpToPx(2f), 0, collapsable && textPrefixView == null ? dpToPx(13f) : dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));
		textView.setText(text);
		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setMinLines(1);
		textView.setMaxLines(10);

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
		if (collapsableView != null) {
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

		((LinearLayout) view).addView(baseView);
		rowBuilt();
	}

	public void buildAmenityRow(View view, AmenityInfoRow info) {
		if (info.icon != null) {
			buildRow(view, info.icon, info.text, info.textPrefix, info.hiddenUrl,
					info.collapsable, info.collapsableView, info.textColor, info.isWiki, info.isText,
					info.needLinks, info.isPhoneNumber,
					info.isUrl, info.matchWidthDivider, info.textLinesLimit);
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

			button.setOnClickListener(v -> {
				if (type != null) {
					PoiUIFilter filter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + type.getKeyName());
					if (filter != null) {
						filter.clearFilter();
						if (poiAdditional) {
							filter.setTypeToAccept(type, true);
							filter.updateTypesToAccept(pt);
							filter.setFilterByName(pt.getKeyName().replace('_', ':').toLowerCase());
						} else {
							LinkedHashSet<String> accept = new LinkedHashSet<>();
							accept.add(pt.getKeyName());
							filter.selectSubTypesToAccept(type, accept);
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

	public Collection<String> getAdditionalInfoKeys() {
		if (additionalInfo == null) {
			return Collections.emptyList();
		}
		return additionalInfo.keySet();
	}

	public String getAdditionalInfo(String key) {
		if (additionalInfo == null) {
			return null;
		}
		String str = additionalInfo.get(key);
		str = Amenity.unzipContent(str);
		return str;
	}
}