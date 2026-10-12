package net.osmand.plus.track;

import static net.osmand.plus.configmap.ConfigureMapMenu.COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.track.GpxAppearanceAdapter.GpxAppearanceAdapterType.TRACK_COLOR;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GpxAppearanceAdapter extends ArrayAdapter<AppearanceListItem> {

	public static final String TRACK_WIDTH_BOLD = "bold";
	public static final String TRACK_WIDTH_MEDIUM = "medium";
	public static final String TRACK_WIDTH_THIN = "thin";
	public static final String SHOW_START_FINISH_ATTR = "show_start_finish_attr";

	private final OsmandApplication app;
	private final GpxAppearanceAdapterType adapterType;
	private final int currentColor;
	private final boolean showStartFinishIcons;
	private final boolean nightMode;

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
			TextView textView = v.findViewById(R.id.text1);
			textView.setText(item.getLocalizedValue());
			String attrName = item.getAttrName();
			if (CURRENT_TRACK_WIDTH_ATTR.equals(attrName)) {
				int iconId = getWidthIconId(item.getValue());
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getUIUtilities().getPaintedIcon(iconId, currentColor), null);
			} else if (CURRENT_TRACK_COLOR_ATTR.equals(attrName)) {
				if (item.getColor() == -1) {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
							app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle), null);
				} else {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
							app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, item.getColor()), null);
				}
			} else if (SHOW_START_FINISH_ATTR.equals(attrName)) {
				int iconId = showStartFinishIcons ? R.drawable.ic_check_box_dark : R.drawable.ic_check_box_outline_dark;
				Drawable icon = app.getUIUtilities().getIcon(iconId, ColorUtilities.getActiveColorId(nightMode));
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			}
			textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(context, 10f));
			v.findViewById(R.id.divider).setVisibility(item.isLastItem()
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

	@NonNull
	public static List<AppearanceListItem> getUniqueTrackColorItems(@NonNull OsmandApplication app) {
		List<Integer> colors = new ArrayList<>();
		List<AppearanceListItem> result = new ArrayList<>();
		for (AppearanceListItem item : getAppearanceItems(app, TRACK_COLOR)) {
			if (!colors.contains(item.getColor())) {
				colors.add(item.getColor());
				result.add(item);
			}
		}
		return result;
	}

	public static List<AppearanceListItem> getAppearanceItems(@NonNull OsmandApplication app, GpxAppearanceAdapterType adapterType) {
		return getAppearanceItems(app, adapterType, false);
	}

	private static List<AppearanceListItem> getAppearanceItems(@NonNull OsmandApplication app, GpxAppearanceAdapterType adapterType,
	                                                           boolean showStartFinishIcons) {
		List<AppearanceListItem> items = new ArrayList<>();
		RenderingRuleProperty trackWidthProp = null;
		RenderingRuleProperty trackColorProp = null;
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			if (adapterType == GpxAppearanceAdapterType.TRACK_WIDTH || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
				trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
			}
			if (adapterType == GpxAppearanceAdapterType.TRACK_COLOR || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
				trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
			}
		}

		if (trackWidthProp != null) {
			for (int j = 0; j < trackWidthProp.getPossibleValues().length; j++) {
				AppearanceListItem item = new AppearanceListItem(CURRENT_TRACK_WIDTH_ATTR,
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
			AppearanceListItem startFinishIconsItem = new AppearanceListItem(SHOW_START_FINISH_ATTR,
					showStartFinishIcons ? "false" : "true", app.getString(R.string.start_finish_icons));
			items.add(startFinishIconsItem);
			startFinishIconsItem.setLastItem(true);
		}
		if (trackColorProp != null) {
			for (int j = 0; j < trackColorProp.getPossibleValues().length; j++) {
				AppearanceListItem item = new AppearanceListItem(CURRENT_TRACK_COLOR_ATTR,
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

	@ColorInt
	public static int getTrackColor(@NonNull OsmandApplication app) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		CommonPreference<String> prefColor = app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);
		return parseTrackColor(renderer, prefColor.get());
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
				String cName = r.getStringPropertyValue(CURRENT_TRACK_COLOR_ATTR);
				if (!Algorithms.isEmpty(cName) && cName.equals(colorName)) {
					return r.getIntPropertyValue(COLOR_ATTR);
				}
				if (cName == null && defaultColor == -1) {
					defaultColor = r.getIntPropertyValue(COLOR_ATTR);
				}
			}
		}
		return GpxUtilities.INSTANCE.parseColor(colorName, defaultColor);
	}

	public static String parseTrackColorName(RenderingRulesStorage renderer, int color) {
		RenderingRule gpxRule = null;
		if (renderer != null) {
			gpxRule = renderer.getRenderingAttributeRule("gpx");
		}
		if (gpxRule != null && gpxRule.getIfElseChildren().size() > 0) {
			List<RenderingRule> rules = gpxRule.getIfElseChildren().get(0).getIfElseChildren();
			for (RenderingRule r : rules) {
				String cName = r.getStringPropertyValue(CURRENT_TRACK_COLOR_ATTR);
				if (!Algorithms.isEmpty(cName) && color == r.getIntPropertyValue(COLOR_ATTR)) {
					return cName;
				}
			}
		}
		return Algorithms.colorToString(color);
	}
}