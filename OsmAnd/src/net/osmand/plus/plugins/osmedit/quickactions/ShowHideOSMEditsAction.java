package net.osmand.plus.plugins.osmedit.quickactions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_OSM_EDITS_ACTION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class ShowHideOSMEditsAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_OSM_EDITS_ACTION,
			"osmedit.showhide", ShowHideOSMEditsAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.osm_edits)
			.iconRes(R.drawable.ic_action_openstreetmap_logo)
			.category(QuickActionType.MY_PLACES)
			.nonEditable();

	public ShowHideOSMEditsAction() {
		super(TYPE);
	}

	public ShowHideOSMEditsAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			plugin.SHOW_OSM_EDITS.set(!plugin.SHOW_OSM_EDITS.get());
			plugin.updateLayers(mapActivity, mapActivity);
			mapActivity.getMapView().refreshMap(true);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_osm_edits_descr);
		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		return plugin != null && plugin.SHOW_OSM_EDITS.get();
	}
}
