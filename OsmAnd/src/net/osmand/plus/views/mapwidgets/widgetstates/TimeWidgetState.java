package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandPreference;

public class TimeWidgetState extends WidgetState {

	public static final int TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME = R.id.time_control_widget_state_arrival_time;
	public static final int TIME_CONTROL_WIDGET_STATE_TIME_TO_GO = R.id.time_control_widget_state_time_to_go;

	private final OsmandPreference<Boolean> showArrival;
	private final boolean intermediate;

	public TimeWidgetState(OsmandApplication app, boolean intermediate) {
		super(app);
		this.intermediate = intermediate;
		if (intermediate) {
			showArrival = app.getSettings().SHOW_INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
		} else {
			showArrival = app.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
		}
	}

	@Override
	public int getMenuTitleId() {
		if (intermediate) {
			return showArrival.get() ? R.string.access_intermediate_arrival_time : R.string.map_widget_intermediate_time;
		}
		return showArrival.get() ? R.string.access_arrival_time : R.string.map_widget_time;
	}

	@Override
	public int getMenuIconId() {
		if (intermediate) {
			return R.drawable.ic_action_intermediate_destination_time;
		}
		return showArrival.get() ? R.drawable.ic_action_time : R.drawable.ic_action_time_to_distance;
	}

	@Override
	public int getMenuItemId() {
		return showArrival.get() ? TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME : TIME_CONTROL_WIDGET_STATE_TIME_TO_GO;
	}

	@Override
	public int[] getMenuTitleIds() {
		if (intermediate) {
			return new int[] {R.string.access_intermediate_arrival_time, R.string.map_widget_intermediate_time};
		}
		return new int[] {R.string.access_arrival_time, R.string.map_widget_time};
	}

	@Override
	public int[] getMenuIconIds() {
		if (intermediate) {
			return new int[] {R.drawable.ic_action_intermediate_destination_time, R.drawable.ic_action_intermediate_destination_time};
		}
		return new int[] {R.drawable.ic_action_time, R.drawable.ic_action_time_to_distance};
	}

	@Override
	public int[] getMenuItemIds() {
		return new int[] {TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME, TIME_CONTROL_WIDGET_STATE_TIME_TO_GO};
	}

	@Override
	public void changeState(int stateId) {
		showArrival.set(stateId == TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME);
	}
}