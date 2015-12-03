package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.Amenity;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AmenityMenuBuilder extends MenuBuilder {

	private final Amenity amenity;

	public AmenityMenuBuilder(OsmandApplication app, final Amenity amenity) {
		super(app);
		this.amenity = amenity;
	}

	private void buildRow(View view, int iconId, String text, String textPrefix, int textColor, boolean isWiki, boolean isText, boolean needLinks) {
		buildRow(view, getRowIcon(iconId), text, textPrefix, textColor, isWiki, isText, needLinks);
	}

	protected void buildRow(final View view, Drawable icon, final String text, final String textPrefix, int textColor, boolean isWiki, boolean isText, boolean needLinks) {
		boolean light = app.getSettings().isLightContent();

		if (!isFirstRow()) {
			buildRowDivider(view, false);
		}

		final String txt;
		if (!Algorithms.isEmpty(textPrefix)) {
			txt = textPrefix + ": " + text;
		} else {
			txt = text;
		}

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(txt, view.getContext());
				return true;
			}
		});

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView iconView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
		llIconParams.setMargins(dpToPx(16f), dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(llIconParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		iconView.setImageDrawable(icon);
		llIcon.addView(iconView);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		if (needLinks) {
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setLinksClickable(true);
		}
		textView.setEllipsize(TextUtils.TruncateAt.END);
		if (isWiki) {
			textView.setMinLines(1);
			textView.setMaxLines(15);
		} else if (isText) {
			textView.setMinLines(1);
			textView.setMaxLines(10);
		}
		textView.setText(txt);
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		((LinearLayout) view).addView(ll);

		if (isWiki) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showWikipediaDialog(view.getContext(), app, amenity);
				}
			});
		} else if (isText && text.length() > 200) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(view.getContext(), app, text, textPrefix);
				}
			});
		}

		rowBuilt();
	}

	@Override
	public void buildInternal(View view) {
		boolean hasWiki = false;
		MapPoiTypes poiTypes = app.getPoiTypes();
		String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = app.getLanguage();
		}
		List<AmenityInfoRow> infoRows = new LinkedList<>();
		List<AmenityInfoRow> descriptions = new LinkedList<>();

		for (Map.Entry<String, String> e : amenity.getAdditionalInfo().entrySet()) {
			int iconId;
			Drawable icon = null;
			int textColor = 0;
			String key = e.getKey();
			String vl = e.getValue();

			String textPrefix = "";
			boolean isWiki = false;
			boolean isText = false;
			boolean needLinks = !"population".equals(key);
			int poiTypeOrder = 0;
			String poiTypeKeyName = "";

			AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
			PoiType pType = null;
			if (pt != null) {
				pType = (PoiType) pt;
				poiTypeOrder = pType.getOrder();
				poiTypeKeyName = pType.getKeyName();
			}

			if (amenity.getType().isWiki()) {
				if (!hasWiki) {
					iconId = R.drawable.ic_action_note_dark;
					String lng = amenity.getContentSelected("content", preferredLang, "en");
					if (Algorithms.isEmpty(lng)) {
						lng = "en";
					}

					final String langSelected = lng;
					String content = amenity.getDescription(langSelected);
					vl = Html.fromHtml(content).toString();
					if (vl.length() > 300) {
						vl = vl.substring(0, 300);
					}
					hasWiki = true;
					isWiki = true;
				} else {
					continue;
				}
			} else if (key.startsWith("name:")) {
				continue;
			} else if (Amenity.OPENING_HOURS.equals(key)) {
				iconId = R.drawable.ic_action_time;

				OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean opened = rs.isOpenedForTime(inst);
					if (opened) {
						textColor = R.color.color_ok;
					} else {
						textColor = R.color.color_invalid;
					}
				}

			} else if (Amenity.PHONE.equals(key)) {
				iconId = R.drawable.ic_action_call_dark;
			} else if (Amenity.WEBSITE.equals(key)) {
				iconId = R.drawable.ic_world_globe_dark;
				vl = vl.replace(' ', '_');
			} else {
				if (Amenity.DESCRIPTION.equals(key)) {
					iconId = R.drawable.ic_action_note_dark;
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
						iconId = R.drawable.ic_action_note_dark;
						textPrefix = pType.getTranslation();
						vl = amenity.unzipContent(e.getValue());
					}
				} else {
					textPrefix = Algorithms.capitalizeFirstLetterAndLowercase(e.getKey());
					vl = amenity.unzipContent(e.getValue());
				}
			}

			if (isText && iconId == R.drawable.ic_action_note_dark) {
				descriptions.add(new AmenityInfoRow(key, R.drawable.ic_action_note_dark, textPrefix,
						vl, 0, false, true, true, 0, ""));
			} else if (icon != null) {
				infoRows.add(new AmenityInfoRow(key, icon, textPrefix, vl, textColor, isWiki, isText,
						needLinks, poiTypeOrder, poiTypeKeyName));
			} else {
				infoRows.add(new AmenityInfoRow(key, iconId, textPrefix, vl, textColor, isWiki, isText,
						needLinks, poiTypeOrder, poiTypeKeyName));
			}
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
					&& desc.key.substring(desc.key.length() - langSuffix.length(), desc.key.length()).equals(langSuffix)) {
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

		buildRow(view, R.drawable.ic_action_get_my_location, PointDescription.getLocationName(app,
				amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), true)
				.replaceAll("\n", ""), 0, false, 0);
	}

	public void buildAmenityRow(View view, AmenityInfoRow info) {
		if (info.icon != null) {
			buildRow(view, info.icon, info.text, info.textPrefix, info.textColor, info.isWiki, info.isText, info.needLinks);
		} else if (info.iconId != 0) {
			buildRow(view, info.iconId, info.text, info.textPrefix, info.textColor, info.isWiki, info.isText, info.needLinks);
		}
	}

	private static class AmenityInfoRow {
		private String key;
		private Drawable icon;
		private int iconId;
		private String textPrefix;
		private String text;
		private int textColor;
		private boolean isWiki;
		private boolean isText;
		private boolean needLinks;
		private int order;
		private String name;

		public AmenityInfoRow(String key, Drawable icon, String textPrefix, String text,
							  int textColor, boolean isWiki, boolean isText, boolean needLinks,
							  int order, String name) {
			this.key = key;
			this.icon = icon;
			this.textPrefix = textPrefix;
			this.text = text;
			this.textColor = textColor;
			this.isWiki = isWiki;
			this.isText = isText;
			this.needLinks = needLinks;
			this.order = order;
			this.name = name;
		}

		public AmenityInfoRow(String key, int iconId, String textPrefix, String text,
							  int textColor, boolean isWiki, boolean isText, boolean needLinks,
							  int order, String name) {
			this.key = key;
			this.iconId = iconId;
			this.textPrefix = textPrefix;
			this.text = text;
			this.textColor = textColor;
			this.isWiki = isWiki;
			this.isText = isText;
			this.needLinks = needLinks;
			this.order = order;
			this.name = name;
		}
	}
}
