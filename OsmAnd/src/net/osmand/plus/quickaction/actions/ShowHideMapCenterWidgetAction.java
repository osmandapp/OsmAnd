package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideMapCenterWidgetAction extends QuickAction {
	public static final QuickActionType TYPE = new QuickActionType(38,
			"coordinates.showhide", ShowHideMapCenterWidgetAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.map_center_widget)
			.iconRes(R.drawable.ic_action_coordinates_map_center).nonEditable()
			.category(QuickActionType.CONFIGURE_SCREEN);

	public ShowHideMapCenterWidgetAction() {
		super(TYPE);
	}

	public ShowHideMapCenterWidgetAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		mapActivity.getMyApplication().getSettings().SHOW_MAP_CENTER_COORDINATES_WIDGET.set(
				!mapActivity.getMyApplication().getSettings().SHOW_MAP_CENTER_COORDINATES_WIDGET.get());
		mapActivity.getMapLayers().updateLayers(mapActivity);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_map_center_widget_descr);
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
		return application.getSettings().SHOW_MAP_CENTER_COORDINATES_WIDGET.get();
	}
}
