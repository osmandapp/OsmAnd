package net.osmand.plus.plugins.monitoring.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SAVE_RECORDED_TRIP_AND_CONTINUE_ACTION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.shared.gpx.GpxFile;

public class SaveRecordedTripAndContinueAction extends BaseMonitoringAction {

	public static final QuickActionType TYPE = new QuickActionType(SAVE_RECORDED_TRIP_AND_CONTINUE_ACTION,
			"save.trip.and.continue", SaveRecordedTripAndContinueAction.class)
			.nameRes(R.string.quick_action_save_recorded_trip_and_continue)
			.iconRes(R.drawable.ic_action_trip_rec_save)
			.nonEditable()
			.category(QuickActionType.MY_PLACES)
			.nameActionRes(R.string.shared_string_save);

	public SaveRecordedTripAndContinueAction() {
		super(TYPE);
	}

	public SaveRecordedTripAndContinueAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandMonitoringPlugin plugin = getPlugin();
		if (plugin != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (!isRecordingTrack()) {
				app.showToastMessage(R.string.start_trip_recording_first_m);
			} else if (!hasDataToSave()) {
				app.showToastMessage(R.string.track_does_not_contain_data_to_save);
			} else {
				GpxFile gpxFile = app.getSavingTrackHelper().getCurrentTrack().getGpxFile();
				SaveGpxHelper.saveCurrentTrack(app, gpxFile, errorMessage -> {
					plugin.saveCurrentTrack(null, mapActivity, false, true);
				});
			}
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_save_recorded_trip_and_continue_summary);
		parent.addView(view);
	}
}
