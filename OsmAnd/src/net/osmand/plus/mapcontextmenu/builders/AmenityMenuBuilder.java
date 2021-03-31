package net.osmand.plus.mapcontextmenu.builders;

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
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikipedia.WikipediaArticleWikiLinkFragment;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AmenityMenuBuilder extends MenuBuilder {

	private static final String WIKI_LINK = ".wikipedia.org/w";
	public final static Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);
	private final static DecimalFormat DF = new DecimalFormat("#.##");
	private MetricsConstants metricSystem;
	private final Amenity amenity;


	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, final @NonNull Amenity amenity) {
		super(mapActivity);
		this.amenity = amenity;
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
		metricSystem = mapActivity.getMyApplication().getSettings().METRIC_SYSTEM.get();
	}

	@Override
	protected void buildNearestWikiRow(View view) {
	}

	@Override
	protected void buildNearestPoiRow(View view) {
	}

	private void buildRow(View view, int iconId, String text, String textPrefix, String socialMediaUrl,
						  boolean collapsable, final CollapsableView collapsableView,
						  int textColor, boolean isWiki, boolean isText, boolean needLinks,
						  boolean isPhoneNumber, boolean isUrl, boolean matchWidthDivider, int textLinesLimit) {
		buildRow(view, iconId == 0 ? null : getRowIcon(iconId), text, textPrefix, socialMediaUrl,
				collapsable, collapsableView, textColor,
				isWiki, isText, needLinks, isPhoneNumber, isUrl, matchWidthDivider, textLinesLimit);
	}

	protected void buildRow(final View view, Drawable icon, final String text, final String textPrefix,
							final String socialMediaUrl, boolean collapsable,
							final CollapsableView collapsableView, int textColor, boolean isWiki,
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
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				String textToCopy;
				if (text.contains(WIKI_LINK)) {
					textToCopy = text;
				} else {
					textToCopy = !Algorithms.isEmpty(textPrefix) ? textPrefix + ": " + text : text;
				}
				copyToClipboard(textToCopy, view.getContext());
				return true;
			}
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
			textPrefixView.setTextColor(app.getResources().getColor(R.color.ctx_menu_buttons_text_color));
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
		textView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));

		int linkTextColor = ContextCompat.getColor(view.getContext(), light ? R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark);

		if (isPhoneNumber || isUrl) {
			textView.setTextColor(linkTextColor);
			needLinks = false;
		}
		textView.setText(text);
		if (needLinks && Linkify.addLinks(textView, Linkify.ALL)) {
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
			textView.setTextColor(view.getResources().getColor(textColor));
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

		final ImageView iconViewCollapse = new ImageView(view.getContext());
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
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (collapsableView.getContentView().getVisibility() == View.VISIBLE) {
						collapsableView.getContentView().setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(true));
						collapsableView.setCollapsed(true);
					} else {
						collapsableView.getContentView().setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(false));
						collapsableView.setCollapsed(false);
					}
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContentView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			baseView.addView(collapsableView.getContentView());
		}

		if (isWiki) {
			buildReadFullButton(llText, app.getString(R.string.context_menu_read_full_article), new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					WikipediaDialogFragment.showInstance(mapActivity, amenity);
				}
			});
		}

		((LinearLayout) view).addView(baseView);

		if (isPhoneNumber) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					showDialog(text, Intent.ACTION_DIAL, "tel:", v);
				}
			});
		} else if (isUrl) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (text.contains(WIKI_LINK)) {
						if (Version.isPaidVersion(app)) {
							WikiArticleHelper wikiArticleHelper = new WikiArticleHelper(mapActivity, !light);
							wikiArticleHelper.showWikiArticle(amenity.getLocation(), text);
						} else {
							WikipediaArticleWikiLinkFragment.showInstance(mapActivity.getSupportFragmentManager(), text);
						}
					} else {
						String uri = socialMediaUrl == null ? text : socialMediaUrl;
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(uri));
						v.getContext().startActivity(intent);
					}
				}
			});
		} else if (isWiki) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					WikipediaDialogFragment.showInstance(mapActivity, amenity);
				}
			});
		} else if (isText && text.length() > 200) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, text, textPrefix);
				}
			});
		}

		rowBuilt();

		setDividerWidth(matchWidthDivider);
	}

	@Override
	public void buildInternal(View view) {
		boolean hasWiki = false;
		MapPoiTypes poiTypes = app.getPoiTypes();
		String preferredLang = getPreferredMapAppLang();
		List<AmenityInfoRow> infoRows = new LinkedList<>();
		List<AmenityInfoRow> descriptions = new LinkedList<>();

		Map<String, List<PoiType>> poiAdditionalCategories = new HashMap<>();
		AmenityInfoRow cuisineRow = null;
		List<PoiType> collectedPoiTypes = new ArrayList<>();

		boolean osmEditingEnabled = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null;

		for (String key : amenity.getAdditionalInfoKeys()) {
			int iconId = 0;
			Drawable icon = null;
			int textColor = 0;
			String vl = amenity.getAdditionalInfo(key);

			if (key.equals("image")
					|| key.equals("mapillary")
					|| key.equals("subway_region")
					|| (key.equals("note") && !osmEditingEnabled)) {
				continue;
			}

			String socialMediaUrl = null;
			String textPrefix = "";
			CollapsableView collapsableView = null;
			boolean collapsable = false;
			boolean isWiki = false;
			boolean isText = false;
			boolean isDescription = false;
			boolean needLinks = !("population".equals(key) || "height".equals(key) || Amenity.OPENING_HOURS.equals(key));
			boolean needIntFormatting = "population".equals(key);
			boolean isPhoneNumber = false;
			boolean isUrl = false;
			boolean isCuisine = false;
			int poiTypeOrder = 0;
			String poiTypeKeyName = "";

			PoiType poiType = amenity.getType().getPoiTypeByKeyName(key);
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

			if (vl.startsWith("http://") || vl.startsWith("https://") || vl.startsWith("HTTP://") || vl.startsWith("HTTPS://")) {
				isUrl = true;
			} else if (needLinks) {
				socialMediaUrl = getSocialMediaUrl(key, vl);
				if (socialMediaUrl != null) {
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

			if (amenity.getType().isWiki()) {
				if (!hasWiki) {
					String articleLang = OsmandPlugin.onGetMapObjectsLocale(amenity, preferredLang);
					String lng = amenity.getContentLanguage("content", articleLang, "en");
					if (Algorithms.isEmpty(lng)) {
						lng = "en";
					}

					final String langSelected = lng;
					String content = amenity.getDescription(langSelected);
					vl = (content != null) ? WikiArticleHelper.getPartialContent(content) : "";
					vl = vl == null ? "" : vl;
					hasWiki = true;
					isWiki = true;
					needLinks = false;
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
					amenity.getAdditionalInfo(key).replace("; ", "\n").replace(",", ", "));
				collapsable = true;
				OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getAdditionalInfo(key));
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
					|| key.equals(Amenity.WIKIMEDIA_COMMONS))  {
				continue;
			} else {
				if (key.contains(Amenity.DESCRIPTION)) {
					iconId = R.drawable.ic_action_note_dark;
				} else if (isUrl && vl.contains(WIKI_LINK)) {
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
					poiTypeOrder = pType.getOrder();
					poiTypeKeyName = pType.getKeyName();
					if (pType.getParentType() != null && pType.getParentType() instanceof PoiType) {
						icon = getRowIcon(view.getContext(), ((PoiType) pType.getParentType()).getOsmTag() + "_" + pType.getOsmTag().replace(':', '_') + "_" + pType.getOsmValue());
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

			String[] formattedPrefixAndText = getFormattedPrefixAndText(key, textPrefix, vl, amenity);
			textPrefix = formattedPrefixAndText[0];
			vl = formattedPrefixAndText[1];

			if ("ele".equals(key)) {
				try {
					float distance = Float.parseFloat(vl);
					vl = OsmAndFormatter.getFormattedAlt(distance, app, metricSystem);
					String collapsibleVal;
					if (metricSystem == MetricsConstants.MILES_AND_FEET || metricSystem == MetricsConstants.MILES_AND_YARDS) {
						collapsibleVal = OsmAndFormatter.getFormattedAlt(distance, app, MetricsConstants.KILOMETERS_AND_METERS);
					} else {
						collapsibleVal = OsmAndFormatter.getFormattedAlt(distance, app, MetricsConstants.MILES_AND_FEET);
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
				row = new AmenityInfoRow(key, icon, textPrefix, vl, socialMediaUrl, collapsable,
						collapsableView, textColor, isWiki, isText, needLinks, poiTypeOrder,
						poiTypeKeyName, isPhoneNumber, isUrl, matchWidthDivider, 0);
			} else {
				row = new AmenityInfoRow(key, iconId, textPrefix, vl, socialMediaUrl, collapsable,
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
			boolean hasCuisineOrDish = poiAdditionalCategories.get(Amenity.CUISINE) != null
					|| poiAdditionalCategories.get(Amenity.DISH) != null;
			if (!hasCuisineOrDish) {
				infoRows.add(cuisineRow);
			}
		}

		for (Map.Entry<String, List<PoiType>> e : poiAdditionalCategories.entrySet()) {
			String categoryName = e.getKey();
			List<PoiType> categoryTypes = e.getValue();
			if (categoryTypes.size() > 0) {
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

				StringBuilder sb = new StringBuilder();
				for (PoiType pt : categoryTypes) {
					if (sb.length() > 0) {
						sb.append(" • ");
					}
					sb.append(pt.getTranslation());
				}
				boolean cuisineOrDish = categoryName.equals(Amenity.CUISINE) || categoryName.equals(Amenity.DISH);
				CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true, categoryTypes, true, cuisineOrDish ? cuisineRow : null);
				infoRows.add(new AmenityInfoRow(poiAdditionalCategoryName, icon,
						pType.getPoiAdditionalCategoryTranslation(), sb.toString(), null,
						true, collapsableView, 0, false, false,
						false, pType.getOrder(), pType.getKeyName(), false,
						false, false, 1));
			}
		}

		if (collectedPoiTypes.size() > 0) {
			CollapsableView collapsableView = getPoiTypeCollapsableView(view.getContext(), true, collectedPoiTypes, false, null);
			PoiCategory poiCategory = amenity.getType();
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

		Collections.sort(infoRows, new Comparator<AmenityInfoRow>() {
			@Override
			public int compare(AmenityInfoRow row1, AmenityInfoRow row2) {
				if (row1.order < row2.order) {
					return -1;
				} else if (row1.order == row2.order) {
					return row1.name.compareTo(row2.name);
				} else {
					return 1;
				}
			}
		});

		for (AmenityInfoRow info : infoRows) {
			buildAmenityRow(view, info);
		}

		String langSuffix = ":" + preferredLang;
		AmenityInfoRow descInPrefLang = null;
		for (AmenityInfoRow desc : descriptions) {
			if (desc.key.length() > langSuffix.length()
					&& desc.key.substring(desc.key.length() - langSuffix.length()).equals(langSuffix)) {
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

		if (processNearestWiki() && nearestWiki.size() > 0) {
			AmenityInfoRow wikiInfo = new AmenityInfoRow(
					NEAREST_WIKI_KEY, R.drawable.ic_plugin_wikipedia, null,
					app.getString(R.string.wiki_around) + " (" + nearestWiki.size() + ")",
					null, true, getCollapsableView(view.getContext(), true, nearestWiki, NEAREST_WIKI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);
			buildAmenityRow(view, wikiInfo);
		}

		if (processNearestPoi() && nearestPoi.size() > 0) {
			AmenityInfoRow poiInfo = new AmenityInfoRow(
					NEAREST_POI_KEY, AmenityMenuController.getRightIconId(amenity), null,
					app.getString(R.string.speak_poi) + " \"" + AmenityMenuController.getTypeStr(amenity) + "\" (" + nearestPoi.size() + ")",
					null, true, getCollapsableView(view.getContext(), true, nearestPoi, NEAREST_POI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);
			buildAmenityRow(view, poiInfo);
		}

		if (osmEditingEnabled && amenity.getId() != null
				&& amenity.getId() > 0 &&
				(amenity.getId() % 2 == 0 || (amenity.getId() >> 1) < Integer.MAX_VALUE)) {
			String link;
			if (amenity.getId() % 2 == 0) {
				link = "https://www.openstreetmap.org/node/";
			} else {
				link = "https://www.openstreetmap.org/way/";
			}
			buildRow(view, R.drawable.ic_action_openstreetmap_logo, null, link + (amenity.getId() >> 1),
					0, false, null, true, 0, true, null, false);
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

	private String[] getFormattedPrefixAndText(String key, String prefix, String value, Amenity amenity) {
		DF.setRoundingMode(RoundingMode.CEILING);
		String formattedValue = value;
		String formattedPrefix = prefix;
		switch (key) {
			case "width":
			case "height":
				if (key.equals("width")) {
					formattedPrefix = mapActivity.getResources().getString(R.string.shared_string_width);
				} else {
					formattedPrefix = mapActivity.getResources().getString(R.string.shared_string_height);
				}
			case "depth":
			case "seamark_height":
				if (Algorithms.isFloat(value)) {
					double valueAsDouble = Double.valueOf(value);
					if (metricSystem == MetricsConstants.MILES_AND_FEET) {
						formattedValue = String.valueOf(DF.format(valueAsDouble * OsmAndFormatter.FEET_IN_ONE_METER))
								+ " " + mapActivity.getResources().getString(R.string.foot);
					} else if (metricSystem == MetricsConstants.MILES_AND_YARDS) {
						formattedValue = String.valueOf(DF.format(valueAsDouble * OsmAndFormatter.YARDS_IN_ONE_METER))
								+ " " + mapActivity.getResources().getString(R.string.yard);
					} else {
						formattedValue = value + " " + mapActivity.getResources().getString(R.string.m);
					}
				}
				break;
			case "distance":
				if(Algorithms.isFloat(value)) {
					float valueAsFloatInMeters = Float.parseFloat(value) * 1000;
					if (metricSystem == MetricsConstants.KILOMETERS_AND_METERS) {
						formattedValue =
							value + " " + mapActivity.getResources().getString(R.string.km);
					} else {
						formattedValue = OsmAndFormatter.getFormattedDistance(valueAsFloatInMeters,
							mapActivity.getMyApplication());
					}
					formattedPrefix = formatPrefix(prefix,
						mapActivity.getResources().getString(R.string.distance));
					break;
				}
			case "capacity":
				if (amenity.getSubType().equals("water_tower") || amenity.getSubType().equals("storage_tank")) {
					if(Algorithms.isFloat(value)) {
						formattedValue = value + " " + mapActivity.getResources().getString(R.string.cubic_m);
					}
				}
				break;
			case "maxweight":
				if(Algorithms.isInt(value)) {
					formattedValue = value + " " + mapActivity.getResources().getString(R.string.metric_ton);
				}
				break;
			case "students":
			case "spots":
			case "seats":
				if(Algorithms.isInt(value)) {
					formattedPrefix = formatPrefix(prefix, mapActivity.getResources().getString(R.string.shared_string_capacity));
				}
				break;
			case "wikipedia":
				formattedPrefix = app.getString(R.string.shared_string_wikipedia);
		}
		return new String[]{formattedPrefix, formattedValue};
	}

	private String formatPrefix(String prefix, String units) {
		return (!prefix.isEmpty()) ? (prefix + ", " + units): units;
	}

	public void buildAmenityRow(View view, AmenityInfoRow info) {
		if (info.icon != null) {
			buildRow(view, info.icon, info.text, info.textPrefix, info.socialMediaUrl,
					info.collapsable, info.collapsableView, info.textColor, info.isWiki, info.isText,
					info.needLinks, info.isPhoneNumber,
					info.isUrl, info.matchWidthDivider, info.textLinesLimit);
		} else {
			buildRow(view, info.iconId, info.text, info.textPrefix, info.socialMediaUrl,
					info.collapsable, info.collapsableView, info.textColor, info.isWiki, info.isText,
					info.needLinks, info.isPhoneNumber,
					info.isUrl, info.matchWidthDivider, info.textLinesLimit);
		}
	}

	@Override
	protected Map<String, String> getAdditionalCardParams() {
		Map<String, String> params = new HashMap<>();
		String imageValue = amenity.getAdditionalInfo("image");
		String mapillaryValue = amenity.getAdditionalInfo("mapillary");
		String wikidataValue = amenity.getAdditionalInfo(Amenity.WIKIDATA);
		String wikimediaValue = amenity.getAdditionalInfo(Amenity.WIKIMEDIA_COMMONS);
		if (!Algorithms.isEmpty(imageValue)) {
			params.put("osm_image", getDecodedAdditionalInfo(imageValue));
		}
		if (!Algorithms.isEmpty(mapillaryValue)) {
			params.put("osm_mapillary_key", getDecodedAdditionalInfo(mapillaryValue));
		}
		if (!Algorithms.isEmpty(wikidataValue)) {
			params.put(Amenity.WIKIDATA, getDecodedAdditionalInfo(wikidataValue));
		}
		if (!Algorithms.isEmpty(wikimediaValue)) {
			params.put(Amenity.WIKIMEDIA_COMMONS, getDecodedAdditionalInfo(wikimediaValue));
		}
		return params;
	}

	private String getDecodedAdditionalInfo(String additionalInfo) {
		try {
			return URLDecoder.decode(additionalInfo, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error(e);
		}
		return additionalInfo;
	}

	private CollapsableView getPoiTypeCollapsableView(final Context context, boolean collapsed,
													  @NonNull final List<PoiType> categoryTypes,
													  final boolean poiAdditional, AmenityInfoRow textRow) {

		final List<TextViewEx> buttons = new ArrayList<>();

		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		for (final PoiType pt : categoryTypes) {
			TextViewEx button = buildButtonInCollapsableView(context, false, false);
			String name = pt.getTranslation();
			button.setText(name);

			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (amenity.getType() != null) {
						PoiUIFilter filter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + amenity.getType().getKeyName());
						if (filter != null) {
							filter.clearFilter();
							if (poiAdditional) {
								filter.setTypeToAccept(amenity.getType(), true);
								filter.updateTypesToAccept(pt);
								filter.setFilterByName(pt.getKeyName().replace('_', ':').toLowerCase());
							} else {
								LinkedHashSet<String> accept = new LinkedHashSet<>();
								accept.add(pt.getKeyName());
								filter.selectSubTypesToAccept(amenity.getType(), accept);
							}
							getMapActivity().showQuickSearch(filter);
						}
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
			final TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					for (TextViewEx b : buttons) {
						if (b.getVisibility() != View.VISIBLE) {
							b.setVisibility(View.VISIBLE);
						}
					}
					button.setVisibility(View.GONE);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}

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
		if (sb.indexOf("/") != -1) {
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

	private static class AmenityInfoRow {
		private String key;
		private Drawable icon;
		private int iconId;
		private String textPrefix;
		private String text;
		private String socialMediaUrl;
		private CollapsableView collapsableView;
		private boolean collapsable;
		private int textColor;
		private boolean isWiki;
		private boolean isText;
		private boolean needLinks;
		private boolean isPhoneNumber;
		private boolean isUrl;
		private int order;
		private String name;
		private boolean matchWidthDivider;
		private int textLinesLimit;

		public AmenityInfoRow(String key, Drawable icon, String textPrefix, String text,
							  String socialMediaUrl, boolean collapsable,
							  CollapsableView collapsableView, int textColor, boolean isWiki,
							  boolean isText, boolean needLinks, int order, String name,
							  boolean isPhoneNumber, boolean isUrl,
							  boolean matchWidthDivider, int textLinesLimit) {
			this.key = key;
			this.icon = icon;
			this.textPrefix = textPrefix;
			this.text = text;
			this.socialMediaUrl = socialMediaUrl;
			this.collapsable = collapsable;
			this.collapsableView = collapsableView;
			this.textColor = textColor;
			this.isWiki = isWiki;
			this.isText = isText;
			this.needLinks = needLinks;
			this.order = order;
			this.name = name;
			this.isPhoneNumber = isPhoneNumber;
			this.isUrl = isUrl;
			this.matchWidthDivider = matchWidthDivider;
			this.textLinesLimit = textLinesLimit;
		}

		public AmenityInfoRow(String key, int iconId, String textPrefix, String text,
							  String socialMediaUrl, boolean collapsable,
							  CollapsableView collapsableView, int textColor, boolean isWiki,
							  boolean isText, boolean needLinks, int order, String name,
							  boolean isPhoneNumber, boolean isUrl,
							  boolean matchWidthDivider, int textLinesLimit) {
			this.key = key;
			this.iconId = iconId;
			this.textPrefix = textPrefix;
			this.text = text;
			this.socialMediaUrl = socialMediaUrl;
			this.collapsable = collapsable;
			this.collapsableView = collapsableView;
			this.textColor = textColor;
			this.isWiki = isWiki;
			this.isText = isText;
			this.needLinks = needLinks;
			this.order = order;
			this.name = name;
			this.isPhoneNumber = isPhoneNumber;
			this.isUrl = isUrl;
			this.matchWidthDivider = matchWidthDivider;
			this.textLinesLimit = textLinesLimit;
		}
	}
}
