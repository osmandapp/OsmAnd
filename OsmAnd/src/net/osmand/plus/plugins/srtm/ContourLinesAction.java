package net.osmand.plus.plugins.srtm;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;
import static net.osmand.plus.quickaction.QuickActionIds.CONTOUR_LINES_ACTION_ID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContourLinesAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(CONTOUR_LINES_ACTION_ID,
			"contourlines.showhide", ContourLinesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.download_srtm_maps).iconRes(R.drawable.ic_plugin_srtm).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ContourLinesAction() {
		super(TYPE);
	}

	public ContourLinesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			boolean enabled = SRTMPlugin.isContourLinesLayerEnabled(mapActivity.getApp());
			plugin.toggleContourLines(mapActivity, !enabled, () -> {
				OsmandApplication app = mapActivity.getApp();
				RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
				if (contourLinesProp != null) {
					OsmandSettings settings = app.getSettings();
					if (!settings.getRenderPropertyValue(contourLinesProp).equals(CONTOUR_LINES_DISABLED_VALUE)) {
						PluginsHelper.enablePluginIfNeeded(mapActivity, app, plugin, true);
					}
					mapActivity.refreshMapComplete();
				}
			});
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_contour_lines_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		return R.drawable.ic_plugin_srtm;
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		return SRTMPlugin.isContourLinesLayerEnabled(app);
	}
}