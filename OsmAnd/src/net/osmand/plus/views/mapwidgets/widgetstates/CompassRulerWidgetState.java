package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandPreference;

public class CompassRulerWidgetState extends WidgetState {

	public static final int COMPASS_CONTROL_WIDGET_STATE_SHOW = R.id.compass_ruler_control_widget_state_show;
	public static final int COMPASS_CONTROL_WIDGET_STATE_HIDE = R.id.compass_ruler_control_widget_state_hide;

	private final OsmandPreference<Boolean> showCompass;

	public CompassRulerWidgetState(OsmandApplication app) {
		super(app);
		showCompass = app.getSettings().SHOW_COMPASS_CONTROL_RULER;
	}

	@Override
	public int getMenuTitleId() {
		return R.string.map_widget_ruler_control;
	}

	@Override
	public int getMenuIconId() {
		return R.drawable.ic_action_ruler_circle;
	}

	@Override
	public int getMenuItemId() {
		return showCompass.get() ? COMPASS_CONTROL_WIDGET_STATE_SHOW : COMPASS_CONTROL_WIDGET_STATE_HIDE;
	}

	@Override
	public int[] getMenuTitleIds() {
		return new int[] {R.string.show_compass_ruler, R.string.hide_compass_ruler};
	}

	@Override
	public int[] getMenuIconIds() {
		return new int[] {R.drawable.ic_action_compass_widget, R.drawable.ic_action_compass_widget_hide};
	}

	@Override
	public int[] getMenuItemIds() {
		return new int[] {COMPASS_CONTROL_WIDGET_STATE_SHOW, COMPASS_CONTROL_WIDGET_STATE_HIDE};
	}

	@Override
	public void changeState(int stateId) {
		showCompass.set(stateId == COMPASS_CONTROL_WIDGET_STATE_SHOW);
	}
}