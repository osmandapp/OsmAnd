package net.osmand.plus.osmedit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideOSMBugAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(24,
			"osmbug.showhide", ShowHideOSMBugAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.osm_notes).iconRes(R.drawable.ic_action_osm_note).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideOSMBugAction() {
		super(TYPE);
	}

	public ShowHideOSMBugAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.getSettings().SHOW_OSM_BUGS.set(!app.getSettings().SHOW_OSM_BUGS.get());
		OsmEditingPlugin plugin = OsmandPlugin.getActivePlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			plugin.updateLayers(mapActivity, mapActivity);
			mapActivity.getMapView().refreshMap(true);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_osmbugs_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {
		String nameRes = application.getString(getNameRes());
		String actionName = isActionWithSlash(application) ? application.getString(R.string.shared_string_hide) : application.getString(R.string.shared_string_show);
		return application.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {

		return application.getSettings().SHOW_OSM_BUGS.get();
	}
}
