package net.osmand.plus.srtmplugin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.render.RenderingRuleProperty;

import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;

public class ContourLinesAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(29,
			"contourlines.showhide", ContourLinesAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.srtm_plugin_name).iconRes(R.drawable.ic_plugin_srtm).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ContourLinesAction() {
		super(TYPE);
	}

	public ContourLinesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(final MapActivity activity) {
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			boolean enabled = SRTMPlugin.isContourLinesLayerEnabled(activity.getMyApplication());
			plugin.toggleContourLines(activity, !enabled, new Runnable() {
				@Override
				public void run() {
					OsmandApplication app = activity.getMyApplication();
					RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
					if (contourLinesProp != null) {
						final CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
						boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);

						if (selected && !plugin.isActive() && !plugin.needsInstallation()) {
							OsmandPlugin.enablePlugin(activity, app, plugin, true);
						}
						activity.refreshMapComplete();
					}
				}
			});
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_contour_lines_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		return R.drawable.ic_plugin_srtm;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		String nameRes = application.getString(getNameRes());
		String actionName = isActionWithSlash(application) ? application.getString(R.string.shared_string_hide) : application.getString(R.string.shared_string_show);
		return application.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return SRTMPlugin.isContourLinesLayerEnabled(application);
	}
}