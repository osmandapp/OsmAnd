package net.osmand.plus.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GpxAppearanceAdapter extends ArrayAdapter<GpxAppearanceAdapter.AppearanceListItem> {

	public static final String TRACK_WIDTH_BOLD = "bold";
	public static final String TRACK_WIDTH_MEDIUM = "medium";
	public static final String SHOW_START_FINISH_ATTR = "show_start_finish_attr";

	private OsmandApplication app;
	private GpxAppearanceAdapterType adapterType;
	private int currentColor;
	private boolean showStartFinishIcons;
	private boolean nightMode;

	public enum GpxAppearanceAdapterType {
		TRACK_WIDTH,
		TRACK_COLOR,
		TRACK_WIDTH_COLOR
	}

	public GpxAppearanceAdapter(Context context, String currentColorValue, GpxAppearanceAdapterType adapterType,
								boolean showStartFinishIcons, boolean nightMode) {
		super(context, R.layout.rendering_prop_menu_item);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.adapterType = adapterType;
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		this.currentColor = parseTrackColor(renderer, currentColorValue);
		this.showStartFinishIcons = showStartFinishIcons;
		this.nightMode = nightMode;
		init();
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		AppearanceListItem item = getItem(position);
		View v = convertView;
		Context context = getContext();
		if (v == null) {
			v = LayoutInflater.from(context).inflate(R.layout.rendering_prop_menu_item, null);
		}
		if (item != null) {
			TextView textView = (TextView) v.findViewById(R.id.text1);
			textView.setText(item.localizedValue);
			if (ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR.equals(item.attrName)) {
				int iconId = getWidthIconId(item.value);
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getUIUtilities().getPaintedIcon(iconId, currentColor), null);
			} else if (ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR.equals(item.attrName)) {
				if (item.color == -1) {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
							app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle), null);
				} else {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
							app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, item.color), null);
				}
			} else if (SHOW_START_FINISH_ATTR.equals(item.attrName)) {
				int iconId = showStartFinishIcons ? R.drawable.ic_check_box_dark : R.drawable.ic_check_box_outline_dark;
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null, app.getUIUtilities().getIcon(iconId,
						nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light), null);
			}
			textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(context, 10f));
			v.findViewById(R.id.divider).setVisibility(item.lastItem
					&& position < getCount() - 1 ? View.VISIBLE : View.GONE);
		}
		return v;
	}

	public static int getWidthIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_gpx_width_bold;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_gpx_width_medium;
		} else {
			return R.drawable.ic_action_gpx_width_thin;
		}
	}

	private void init() {
		addAll(getAppearanceItems(app, adapterType, showStartFinishIcons));
	}

	public static List<AppearanceListItem> getAppearanceItems(OsmandApplication app, GpxAppearanceAdapterType adapterType) {
		return getAppearanceItems(app, adapterType, false);
	}

	public static List<AppearanceListItem> getAppearanceItems(OsmandApplication app, GpxAppearanceAdapterType adapterType,
															  boolean showStartFinishIcons) {
		List<AppearanceListItem> items = new ArrayList<>();
		RenderingRuleProperty trackWidthProp = null;
		RenderingRuleProperty trackColorProp = null;
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			if (adapterType == GpxAppearanceAdapterType.TRACK_WIDTH || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
				trackWidthProp = renderer.PROPS.getCustomRule(ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR);
			}
			if (adapterType == GpxAppearanceAdapterType.TRACK_COLOR || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
				trackColorProp = renderer.PROPS.getCustomRule(ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR);
			}
		}

		if (trackWidthProp != null) {
			for (int j = 0; j < trackWidthProp.getPossibleValues().length; j++) {
				AppearanceListItem item = new AppearanceListItem(ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR,
						trackWidthProp.getPossibleValues()[j],
						AndroidUtils.getRenderingStringPropertyValue(app, trackWidthProp.getPossibleValues()[j]));
				items.add(item);
				if (adapterType != GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
					if (j == trackWidthProp.getPossibleValues().length - 1) {
						item.setLastItem(true);
					}
				}
			}
		}
		if (adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
			AppearanceListItem startFinishIconsitem = new AppearanceListItem(SHOW_START_FINISH_ATTR,
					showStartFinishIcons ? "false" : "true", app.getString(R.string.start_finish_icons));
			items.add(startFinishIconsitem);
			startFinishIconsitem.setLastItem(true);
		}
		if (trackColorProp != null) {
			for (int j = 0; j < trackColorProp.getPossibleValues().length; j++) {
				AppearanceListItem item = new AppearanceListItem(ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR,
						trackColorProp.getPossibleValues()[j],
						AndroidUtils.getRenderingStringPropertyValue(app, trackColorProp.getPossibleValues()[j]),
						parseTrackColor(renderer, trackColorProp.getPossibleValues()[j]));
				items.add(item);
				if (j == trackColorProp.getPossibleValues().length - 1) {
					item.setLastItem(true);
				}
			}
		}
		return items;
	}

	public static int parseTrackColor(RenderingRulesStorage renderer, String colorName) {
		int defaultColor = -1;
		RenderingRule gpxRule = null;
		if (renderer != null) {
			gpxRule = renderer.getRenderingAttributeRule("gpx");
		}
		if (gpxRule != null && gpxRule.getIfElseChildren().size() > 0) {
			List<RenderingRule> rules = gpxRule.getIfElseChildren().get(0).getIfElseChildren();
			for (RenderingRule r : rules) {
				String cName = r.getStringPropertyValue(ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR);
				if (!Algorithms.isEmpty(cName) && cName.equals(colorName)) {
					return r.getIntPropertyValue(ConfigureMapMenu.COLOR_ATTR);
				}
				if (cName == null && defaultColor == -1) {
					defaultColor = r.getIntPropertyValue(ConfigureMapMenu.COLOR_ATTR);
				}
			}
		}
		return defaultColor;
	}

	public static String parseTrackColorName(RenderingRulesStorage renderer, int color) {
		RenderingRule gpxRule = null;
		if (renderer != null) {
			gpxRule = renderer.getRenderingAttributeRule("gpx");
		}
		if (gpxRule != null && gpxRule.getIfElseChildren().size() > 0) {
			List<RenderingRule> rules = gpxRule.getIfElseChildren().get(0).getIfElseChildren();
			for (RenderingRule r : rules) {
				String cName = r.getStringPropertyValue(ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR);
				if (!Algorithms.isEmpty(cName) && color == r.getIntPropertyValue(ConfigureMapMenu.COLOR_ATTR)) {
					return cName;
				}
			}
		}
		return Algorithms.colorToString(color);
	}

	public static class AppearanceListItem {

		private String attrName;
		private String value;
		private String localizedValue;
		private int color;
		private boolean lastItem;

		public AppearanceListItem(String attrName, String value, String localizedValue) {
			this.attrName = attrName;
			this.value = value;
			this.localizedValue = localizedValue;
		}

		public AppearanceListItem(String attrName, String value, String localizedValue, int color) {
			this.attrName = attrName;
			this.value = value;
			this.localizedValue = localizedValue;
			this.color = color;
		}

		public String getAttrName() {
			return attrName;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getLocalizedValue() {
			return localizedValue;
		}

		public int getColor() {
			return color;
		}

		public boolean isLastItem() {
			return lastItem;
		}

		public void setLastItem(boolean lastItem) {
			this.lastItem = lastItem;
		}
	}
}