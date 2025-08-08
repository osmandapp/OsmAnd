package net.osmand.plus.plugins.monitoring.actions;

import static net.osmand.plus.quickaction.QuickActionIds.TRIP_RECORDING_ACTION;

import android.content.Context;
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
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class TripRecordingAction extends BaseMonitoringAction {

	public static final QuickActionType TYPE = new QuickActionType(TRIP_RECORDING_ACTION,
			"trip.recording.startpause", TripRecordingAction.class)
			.nameRes(R.string.shared_string_trip_recording)
			.iconRes(R.drawable.ic_action_trip_rec_start)
			.nonEditable()
			.category(QuickActionType.MY_PLACES)
			.nameActionRes(R.string.quick_action_verb_start_pause);

	public TripRecordingAction() {
		super(TYPE);
	}

	public TripRecordingAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmandMonitoringPlugin plugin = getPlugin();
		if (plugin != null) {
			if (hasDataToSave()) {
				plugin.pauseOrResumeRecording();
			} else {
				plugin.askShowTripRecordingDialog(mapActivity);
			}
		}
	}

	@Override
	public int getIconRes(Context context) {
		return isRecordingTrack()
				? R.drawable.ic_action_trip_rec_pause
				: R.drawable.ic_action_trip_rec_start;
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isRecordingTrack() ? app.getString(R.string.shared_string_pause) : app.getString(R.string.shared_string_control_start);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_start_pause_recording);
		parent.addView(view);
	}
}
