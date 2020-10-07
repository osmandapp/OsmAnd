package net.osmand.plus.osmedit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideOSMBugAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(24,
			"osmbug.showhide", ShowHideOSMBugAction.class).
			nameRes(R.string.quick_action_showhide_osmbugs_title).iconRes(R.drawable.ic_action_osm_note).nonEditable().
			category(QuickActionType.CONFIGURE_MAP);


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
