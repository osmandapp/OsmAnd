package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickAction;

public class ShowHideOSMBugAction extends QuickAction {

	public static final int TYPE = 24;

	public ShowHideOSMBugAction() {
		super(TYPE);
	}

	public ShowHideOSMBugAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		activity.getMyApplication().getSettings().SHOW_OSM_BUGS.set(
				!activity.getMyApplication().getSettings().SHOW_OSM_BUGS.get());

		OsmEditingPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			plugin.updateLayers(activity.getMapView(), activity);
			activity.getMapView().refreshMap(true);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_osmbugs_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return application.getSettings().SHOW_OSM_BUGS.get()
				? application.getString(R.string.quick_action_osmbugs_hide)
				: application.getString(R.string.quick_action_osmbugs_show);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {

		return application.getSettings().SHOW_OSM_BUGS.get();
	}
}
