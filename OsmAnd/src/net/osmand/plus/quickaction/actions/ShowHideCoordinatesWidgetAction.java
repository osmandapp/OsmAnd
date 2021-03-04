package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideCoordinatesWidgetAction extends QuickAction {
	public static final QuickActionType TYPE = new QuickActionType(35,
			"coordinates.showhide", ShowHideCoordinatesWidgetAction.class)
			.nameActionRes(R.string.quick_action_showhide_title)
			.nameRes(R.string.coordinates_widget)
			.iconRes(R.drawable.ic_action_coordinates_widget).nonEditable()
			.category(QuickActionType.CONFIGURE_SCREEN);

	public ShowHideCoordinatesWidgetAction() {
		super(TYPE);
	}

	public ShowHideCoordinatesWidgetAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		activity.getMyApplication().getSettings().SHOW_COORDINATES_WIDGET.set(
				!activity.getMyApplication().getSettings().SHOW_COORDINATES_WIDGET.get());

		activity.getMapLayers().updateLayers(activity.getMapView());
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_coordinates_widget_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return application.getSettings().SHOW_COORDINATES_WIDGET.get()
				? application.getString(R.string.quick_action_coordinates_widget_hide)
				: application.getString(R.string.quick_action_coordinates_widget_show);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return application.getSettings().SHOW_COORDINATES_WIDGET.get();
	}
}
