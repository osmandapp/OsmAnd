package net.osmand.plus.plugins.panoramax;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_PANORAMAX_ACTION_ID;

import android.os.Bundle;
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

public class ShowHidePanoramaxAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_PANORAMAX_ACTION_ID,
			"panoramax.showhide", ShowHidePanoramaxAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.panoramax)
			.iconRes(R.drawable.ic_action_panoramax).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHidePanoramaxAction() {
		super(TYPE);
	}

	public ShowHidePanoramaxAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		PanoramaxPlugin plugin = PluginsHelper.getPlugin(PanoramaxPlugin.class);
		if (plugin != null) {
			plugin.SHOW_PANORAMAX.set(!plugin.SHOW_PANORAMAX.get());
			plugin.updateLayers(mapActivity, mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_panoramax_descr);
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
		PanoramaxPlugin plugin = PluginsHelper.getPlugin(PanoramaxPlugin.class);
		return plugin != null && plugin.SHOW_PANORAMAX.get();
	}
}
