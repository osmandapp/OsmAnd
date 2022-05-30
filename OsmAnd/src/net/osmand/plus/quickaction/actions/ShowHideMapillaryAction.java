package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideMapillaryAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(33,
			"mapillary.showhide", ShowHideMapillaryAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
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
	public void execute(@NonNull MapActivity mapActivity) {
		MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		if (plugin != null) {
			plugin.SHOW_MAPILLARY.set(!plugin.SHOW_MAPILLARY.get());
			plugin.updateLayers(mapActivity, mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_mapillary_descr);

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
		MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		return plugin != null && plugin.SHOW_MAPILLARY.get();
	}
}
