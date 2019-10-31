package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.srtmplugin.SRTMPlugin;

public class HillshadeAction extends QuickAction {

	public static final int TYPE = 30;

	public HillshadeAction() {
		super(TYPE);
	}

	public HillshadeAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(final MapActivity activity) {
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			plugin.toggleHillshade(activity, !plugin.isHillShadeLayerEnabled(), new Runnable() {
				@Override
				public void run() {
					if (plugin.isHillShadeLayerEnabled() && !plugin.isActive() && !plugin.needsInstallation()) {
						OsmandPlugin.enablePlugin(activity, activity.getMyApplication(), plugin, true);
					}
					plugin.updateLayers(activity.getMapView(), activity);
					SRTMPlugin.refreshMapComplete(activity);
				}
			});
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_hillshade_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		return R.drawable.ic_action_hillshade_dark;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		return application.getSettings().HILLSHADE.get() ? application.getString(R.string.quick_action_hillshade_hide)
				: application.getString(R.string.quick_action_hillshade_show);
	}
}