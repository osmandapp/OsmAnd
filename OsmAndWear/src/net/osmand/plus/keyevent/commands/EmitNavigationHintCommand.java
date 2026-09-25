package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.views.OsmandMapTileView;

public class EmitNavigationHintCommand extends KeyEventCommand {

	public static final String ID = "emit_navigation_hint";

	public static final int LONG_KEYPRESS_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 2;
	public static final int LONG_KEYPRESS_DELAY = 500;

	// Handler to show/hide trackball position and to link map with delay
	private final Handler uiHandler = new Handler();

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
			Message msg = Message.obtain(uiHandler, () -> {
				app.getLocationProvider().emitNavigationHint();
			});
			msg.what = LONG_KEYPRESS_MSG_ID;
			uiHandler.sendMessageDelayed(msg, LONG_KEYPRESS_DELAY);
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		MapActivity mapActivity = requireMapActivity();
		OsmandMapTileView mapView = mapActivity.getMapView();
		MapActivityActions mapActions = mapActivity.getMapActions();
		if (!app.accessibilityEnabled()) {
			mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
		} else if (uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
			uiHandler.removeMessages(LONG_KEYPRESS_MSG_ID);
			mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
		}
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_emit_navigation_hint);
	}
}
