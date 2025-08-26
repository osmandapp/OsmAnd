package net.osmand.plus.plugins.mapillary;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_MAPILLARY_ACTION_ID;

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
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class ShowHideMapillaryAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_MAPILLARY_ACTION_ID,
			"mapillary.showhide", ShowHideMapillaryAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.mapillary)
			.iconRes(R.drawable.ic_action_mapillary).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideMapillaryAction() {
		super(TYPE);
	}

	public ShowHideMapillaryAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		if (plugin != null) {
			plugin.SHOW_MAPILLARY.set(!plugin.SHOW_MAPILLARY.get());
			plugin.updateLayers(mapActivity, mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_mapillary_descr);
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
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		return plugin != null && plugin.SHOW_MAPILLARY.get();
	}
}
