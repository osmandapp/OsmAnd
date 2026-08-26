package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.data.Amenity.*;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.ALT_NAMES_ROW_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NAMES_ROW_KEY;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKI_DATA_BASE_URL;
import static net.osmand.plus.wikipedia.WikiAlgorithms.WIKI_LINK;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import net.osmand.data.AdditionalInfoBundle;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityTagEntry;
import net.osmand.data.AmenityTagEntriesBuilder;
import net.osmand.data.LatLon;
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
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRule;
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRules;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.*;


public class AmenityUIHelper extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityUIHelper.class);

	private final AdditionalInfoBundle additionalInfo;

	private MapPoiTypes poiTypes;
	private PoiCategory poiCategory;
	private String subtype;
	private boolean osmEditingEnabled = PluginsHelper.isActive(OsmEditingPlugin.class);
	private List<String> preferredLangCandidates;

	public AmenityUIHelper(@NonNull MapActivity mapActivity,
	                       @NonNull AdditionalInfoBundle infoBundle) {
		super(mapActivity);
		this.additionalInfo = infoBundle;
	}

	@Override
	public void buildInternal(View view) {
		initVariables();
		Context context = view.getContext();
		List<AmenityTagEntry> infoEntries = new ArrayList<>();
		List<AmenityTagEntry> descriptions = new ArrayList<>();

		List<AmenityTagEntry> visibleTags = additionalInfo.getVisibleTags(osmEditingEnabled, preferredLangCandidates);
		for (AmenityTagEntry baseEntry : visibleTags) {
			AmenityTagEntry amenityEntry = buildEntryData(context, baseEntry);
			if (amenityEntry == null) {
				continue;
			}
			if (amenityEntry.isDescription) {
				descriptions.add(amenityEntry);
			} else {
				infoEntries.add(amenityEntry);
			}
		}

		AmenityTagEntriesBuilder.sortInfoEntries(infoEntries);
		for (AmenityTagEntry info : infoEntries) {
			buildAmenityRow(view, toAmenityInfoRow(context, info));
		}

		AmenityTagEntriesBuilder.sortDescriptionEntries(descriptions, getPreferredMapAppLang());
		for (AmenityTagEntry info : descriptions) {
			buildAmenityRow(view, toAmenityInfoRow(context, info));
		}
		if (PluginsHelper.getActivePlugin(OsmEditingPlugin.class) != null) {
			buildWikiDataRow(view);
		}
	}

	@Nullable
	private AmenityTagEntry buildEntryData(@NonNull Context context, @NonNull AmenityTagEntry baseEntry) {
		if (baseEntry.collapsableEntryType == AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP) {
			return buildPoiTypeGroupEntryData(baseEntry);
		}
		if (!Algorithms.isEmpty(baseEntry.collapsableEntries)) {
			return buildLocalizedEntryData(context, baseEntry);
		}
		AmenityTagEntry.Builder entryBuilder = getEntryDataBuilder(context, baseEntry.key, baseEntry.value,
				baseEntry.isDescription, baseEntry.resolvedType);
		return entryBuilder != null ? entryBuilder.build() : null;
	}

	@NonNull
	private AmenityTagEntry buildPoiTypeGroupEntryData(@NonNull AmenityTagEntry baseEntry) {
		List<PoiType> categoryTypes = baseEntry.collapsablePoiTypes;
		PoiType firstType = categoryTypes.get(0);
		if (baseEntry.poiAdditional) {
			String poiAdditionalCategoryName = baseEntry.key;
			String poiAdditionalIconName = poiTypes.getPoiAdditionalCategoryIconName(poiAdditionalCategoryName);
			List<String> iconNameCandidates = nonEmptyIconNames(
					poiAdditionalIconName, poiAdditionalCategoryName, firstType.getIconKeyName());
			return AmenityTagEntriesBuilder.buildPoiTypesGroupEntry(poiAdditionalCategoryName, firstType.getKeyName(),
					firstType.getPoiAdditionalCategoryTranslation(), categoryTypes, firstType.getOrder(), 0,
					iconNameCandidates, R.drawable.ic_action_note_dark, true, baseEntry.collapsableCategory);
		}
		PoiCategory groupCategory = firstType.getCategory();
		return AmenityTagEntriesBuilder.buildPoiTypesGroupEntry(groupCategory.getKeyName(),
				groupCategory.getKeyName(), groupCategory.getTranslation(), categoryTypes,
				PoiType.DEFAULT_GROUP_ORDER, 0, List.of(groupCategory.getIconKeyName()), 0,
				false, baseEntry.collapsableCategory);
	}

	@Nullable
	private AmenityTagEntry buildLocalizedEntryData(@NonNull Context context, @NonNull AmenityTagEntry baseEntry) {
		if (isNoteKeyHiddenFromEditing(baseEntry.key)) {
			return null;
		}

		List<AmenityTagEntry> localizedEntries = new ArrayList<>();
		for (AmenityTagEntry child : baseEntry.collapsableEntries) {
			if (!isNoteKeyHiddenFromEditing(child.key)) {
				AdditionalInfoBundle.ResolvedPoiType childResolved =
						additionalInfo.resolvePoiType(poiCategory, child.key, child.value);
				AmenityTagEntry.Builder childBuilder =
						getEntryDataBuilder(context, child.key, child.value, baseEntry.isDescription, childResolved);
				if (childBuilder != null) {
					localizedEntries.add(childBuilder.build());
				}
			}
		}
		AmenityTagEntriesBuilder.sortInfoEntries(localizedEntries);

		AmenityTagEntry.Builder headerBuilder = getEntryDataBuilder(context, baseEntry.key, baseEntry.value,
				baseEntry.isDescription, baseEntry.resolvedType);
		if (headerBuilder == null) {
			return null;
		}
		if (headerBuilder.getCollapsableEntryType() == AmenityTagEntry.CollapsableEntryType.NONE) {
			headerBuilder.setCollapsableEntries(localizedEntries);
		}
		return headerBuilder.build();
	}

	private boolean isNoteKeyHiddenFromEditing(@NonNull String key) {
		return NOTE.equals(key) && !osmEditingEnabled;
	}

	@Nullable
	private AmenityTagEntry.Builder getEntryDataBuilder(@NonNull Context context, @NonNull String key,
	                                                    @NonNull String value, boolean isDescription,
	                                                    @NonNull AdditionalInfoBundle.ResolvedPoiType resolvedType) {
		if (resolvedType.additionalType() == null && resolvedType.categoryType() != null) {
			return null;
		}
		AmenityTagEntry.Builder entryBuilder =
				new AmenityTagEntry.Builder(key).setValue(value).setIsDescription(isDescription);
		PoiAdditionalUiRule poiAdditionalUiRule = PoiAdditionalUiRules.INSTANCE.findRule(key);
		if (resolvedType.additionalType() != null) {
			poiAdditionalUiRule.fillRow(app, context, entryBuilder, this, resolvedType.additionalType(),
					key, value, subtype);
		} else {
			PoiType fallbackType = new PoiType(poiTypes, poiCategory, null, key, poiCategory.getIconKeyName());
			fallbackType.setText(true);
			poiAdditionalUiRule.fillRow(app, context, entryBuilder, this, fallbackType, key,
					poiTypes.getPoiTranslation(value), subtype);
		}
		entryBuilder.setMatchWidthDivider(!entryBuilder.isDescription() && entryBuilder.isWiki());
		return entryBuilder;
	}

	@Override
	protected void openWikiUrl(@NonNull String url, boolean light) {
		WikiArticleHelper.askShowArticle(mapActivity, !light, getLatLon(), url);
	}

	public void buildWikiDataRow(@NonNull View view) {
		String value = additionalInfo.get(WIKIDATA);
		if (value != null) {
			AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(WIKIDATA);
			PoiType pType = pt != null ? (PoiType) pt : null;
			if (pType != null) {
				AmenityInfoRow infoRow = new AmenityInfoRow.Builder(WIKIDATA)
						.setIconId(R.drawable.ic_action_logo_wikidata)
						.setTextPrefix(pType.getTranslation()).setText(value)
						.setHiddenUrl(getSocialMediaUrl(WIKIDATA, value))
						.setIsText(true).setNeedLinks(true)
						.setOrder(pType.getOrder()).setName(pType.getKeyName())
						.setMatchWidthDivider(matchWidthDivider).setIsUrl(true)
						.build();
				buildAmenityRow(view, infoRow);
			}
		}
	}

	private void initVariables() {
		poiCategory = additionalInfo.getCategory();
		subtype = additionalInfo.get(SUBTYPE);
		poiTypes = app.getPoiTypes();
		osmEditingEnabled = PluginsHelper.isActive(OsmEditingPlugin.class);
		preferredLangCandidates = LocaleHelper.getPreferredLangCandidates(app);
	}

	@NonNull
	private AmenityInfoRow toAmenityInfoRow(@NonNull Context context, @NonNull AmenityTagEntry data) {
		AmenityInfoRow.Builder rowBuilder = new AmenityInfoRow.Builder(data.key)
				.setIconId(data.iconId)
				.setTextPrefix(data.textPrefix)
				.setText(data.text)
				.setHiddenUrl(data.hiddenUrl)
				.setCollapsableView(resolveCollapsableView(context, data))
				.setTextColor(data.textColor)
				.setIsWiki(data.isWiki)
				.setIsText(data.isText)
				.setNeedLinks(data.needLinks)
				.setIsPhoneNumber(data.isPhoneNumber)
				.setIsUrl(data.isUrl)
				.setOrder(data.order)
				.setName(data.name)
				.setMatchWidthDivider(data.matchWidthDivider)
				.setTextLinesLimit(data.textLinesLimit);
		if (data.iconId == 0) {
			Drawable icon = null;
			for (String candidate : data.iconNameCandidates) {
				icon = getRowIcon(context, candidate);
				if (icon != null) {
					break;
				}
			}
			if (icon != null) {
				rowBuilder.setIcon(icon);
			} else if (data.fallbackIconId != 0) {
				rowBuilder.setIconId(data.fallbackIconId);
			}
		}
		return rowBuilder.build();
	}

	@Nullable
	private CollapsableView resolveCollapsableView(@NonNull Context context, @NonNull AmenityTagEntry data) {
		switch (data.collapsableEntryType) {
			case PLAIN:
				return buildCollapsableViewFromEntries(context, data.collapsableEntries);
			case POI_TYPE_GROUP:
				return getPoiTypeCollapsableView(context, true, data.collapsablePoiTypes,
						data.poiAdditional, data.collapsableCategory);
			case ELEVATION_PILLS: {
				Set<String> texts = new LinkedHashSet<>();
				for (AmenityTagEntry entry : data.collapsableEntries) {
					texts.add(entry.text);
				}
				return getDistanceCollapsableView(texts);
			}
			case OPENING_HOURS:
				return getCollapsableTextView(app, true, data.collapsableEntries.get(0).text);
			case NONE:
			default:
				return null;
		}
	}

	@NonNull
	private List<String> nonEmptyIconNames(String... candidates) {
		List<String> result = new ArrayList<>();
		for (String candidate : candidates) {
			if (!Algorithms.isEmpty(candidate)) {
				result.add(candidate);
			}
		}
		return result;
	}

	@NonNull
	private CollapsableView buildCollapsableViewFromEntries(@NonNull Context context,
	                                                        @NonNull List<AmenityTagEntry> entries) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (AmenityTagEntry entry : entries) {
			View container = createRowContainer(context, null);
			buildDetailsRow(container, null, entry.text, entry.textPrefix, null, null, false, null);
			llv.addView(container);
		}
		return new CollapsableView(llv, this, true);
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
					namesMap.size() > 1 ? getNamesCollapsableView(namesMap, nameLocale.getLanguage(), hint) : null, true, null);
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
						null, null, false, null);
				llv.addView(amenitiesRow);
			}
		}
		return new CollapsableView(llv, this, true);
	}

	@Nullable
	public static String getSocialMediaUrl(String key, String value) {
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
		urls.put("twitter", "https://x.com/%s");
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
		boolean isEmailAction = isEmailAction(text);

		if (isPhoneNumber || isUrl || isEmailAction) {
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

		((LinearLayout) view).addView(baseView);

		if (!collapsable) {
			if (isPhoneNumber) {
				ll.setOnClickListener(v -> handlePhoneClick(textPrefix, text, v));
			} else if (isUrl) {
				ll.setOnClickListener(v -> handleUrlClick(textPrefix, text, hiddenUrl, light, v));
			} else if (isEmailAction) {
				ll.setOnClickListener(v -> handleEmailClick(textPrefix, text, v));
			} else if (isText && text.length() > 200) {
				ll.setOnClickListener(v -> POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, text, textPrefix));
			}
		}

		rowBuilt();

		setDividerWidth(matchWidthDivider);
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
	                                                  boolean poiAdditional, PoiCategory type) {

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
			Map<String, String> localizations = (Map<String, String>) descriptions.get(AdditionalInfoBundle.LOCALIZATIONS);
			Collection<String> locales = AmenityUIHelper.collectAvailableLocalesFromTags(localizations.keySet());

			Locale locale = LocaleHelper.getPreferredNameLocale(app, locales);
			String localeKey = locale != null ? key + ":" + locale.getLanguage() : key;

			String description = localizations.get(localeKey);
			if (description == null && locale != null && Algorithms.stringsEqual(locale.getLanguage(), "en")) {
				description = localizations.get(key);
			}
			return description != null ? Pair.create(description, locale) : null;
		}
		String description = amenity.getAdditionalInfo(key);
		if (!Algorithms.isEmpty(description)) {
			return Pair.create(description, null);
		}
		return null;
	}

}
