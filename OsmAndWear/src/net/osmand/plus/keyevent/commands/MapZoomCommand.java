package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class MapZoomCommand extends KeyEventCommand {

	public static final String ZOOM_IN_ID = "zoom_in";
	public static final String ZOOM_OUT_ID = "zoom_out";
	public static final String CONTINUOUS_ZOOM_IN_ID = "continuous_zoom_in";
	public static final String CONTINUOUS_ZOOM_OUT_ID = "continuous_zoom_out";

	private final boolean continuous;
	private final boolean increment;

	public MapZoomCommand(boolean continuous, boolean increment) {
		this.continuous = continuous;
		this.increment = increment;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (continuous) {
			changeZoom(increment ? 1 : -1);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!continuous) {
			changeZoom(increment ? 1 : -1);
		}
		return super.onKeyUp(keyCode, event);
	}

	private void changeZoom(int zoomStep) {
		app.getOsmandMap().getMapView().changeZoomManually(zoomStep);
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(increment
				? R.string.key_event_action_zoom_in
				: R.string.key_event_action_zoom_out);
	}
}
