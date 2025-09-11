package net.osmand.plus.plugins.monitoring.actions;

import static net.osmand.plus.quickaction.QuickActionIds.FINISH_TRIP_RECORDING_ACTION;

import android.os.Bundle;
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

public class FinishTripRecordingAction extends BaseMonitoringAction {

	public static final QuickActionType TYPE = new QuickActionType(FINISH_TRIP_RECORDING_ACTION,
			"finish.trip.recording", FinishTripRecordingAction.class)
			.nameRes(R.string.shared_string_trip_recording)
			.iconRes(R.drawable.ic_action_trip_rec_finish)
			.nonEditable()
			.category(QuickActionType.MY_PLACES)
			.nameActionRes(R.string.shared_string_finish);

	public FinishTripRecordingAction() {
		super(TYPE);
	}

	public FinishTripRecordingAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmandMonitoringPlugin plugin = getPlugin();
		if (plugin != null) {
			OsmandApplication app = mapActivity.getApp();
			if (!isRecordingTrack()) {
				app.showToastMessage(R.string.start_trip_recording_first_m);
			} else if (!hasDataToSave()) {
				app.showToastMessage(R.string.track_does_not_contain_data_to_save);
			} else {
				plugin.finishRecording();
			}
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_finish_trip_recording_summary);
		parent.addView(view);
	}
}
