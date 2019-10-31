package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.render.RenderingRuleProperty;

import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;

public class ContourLinesAction extends QuickAction {

	public static final int TYPE = 29;

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
						final OsmandSettings.CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
						boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);

						if (selected && !plugin.isActive() && !plugin.needsInstallation()) {
							OsmandPlugin.enablePlugin(activity, app, plugin, true);
						}

						SRTMPlugin.refreshMapComplete(activity);
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
	public String getActionText(OsmandApplication app) {
		return SRTMPlugin.isContourLinesLayerEnabled(app) ? app.getString(R.string.quick_action_contour_lines_hide)
				: app.getString(R.string.quick_action_contour_lines_show);
	}
}