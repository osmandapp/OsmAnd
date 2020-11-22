package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandPreference;

public class BearingWidgetState extends WidgetState {

	public static final int BEARING_WIDGET_STATE_RELATIVE_BEARING = R.id.bearing_widget_state_relative_bearing;
	public static final int BEARING_WIDGET_STATE_MAGNETIC_BEARING = R.id.bearing_widget_state_magnetic_bearing;

	private final OsmandPreference<Boolean> showRelativeBearing;

	public BearingWidgetState(OsmandApplication ctx) {
		super(ctx);
		showRelativeBearing = ctx.getSettings().SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING;
	}

	@Override
	public int getMenuTitleId() {
		return showRelativeBearing.get() ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing;
	}

	@Override
	public int getMenuIconId() {
		return showRelativeBearing.get() ? R.drawable.ic_action_relative_bearing : R.drawable.ic_action_bearing;
	}

	@Override
	public int getMenuItemId() {
		return showRelativeBearing.get() ? BEARING_WIDGET_STATE_RELATIVE_BEARING : BEARING_WIDGET_STATE_MAGNETIC_BEARING;
	}

	@Override
	public int[] getMenuTitleIds() {
		return new int[] {R.string.map_widget_magnetic_bearing, R.string.map_widget_bearing};
	}

	@Override
	public int[] getMenuIconIds() {
		return new int[] {R.drawable.ic_action_bearing, R.drawable.ic_action_relative_bearing};
	}

	@Override
	public int[] getMenuItemIds() {
		return new int[] {BEARING_WIDGET_STATE_MAGNETIC_BEARING, BEARING_WIDGET_STATE_RELATIVE_BEARING};
	}

	@Override
	public void changeState(int stateId) {
		showRelativeBearing.set(stateId == BEARING_WIDGET_STATE_RELATIVE_BEARING);
	}
}