package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_TIME;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.controllers.BatteryOptimizationController;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

public class TripRecordingTimeWidget extends SimpleWidget {

	private final SavingTrackHelper savingTrackHelper;

	private float cachedTimeSpan = -1;

	public TripRecordingTimeWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, TRIP_RECORDING_TIME, customId, widgetsPanel);
		savingTrackHelper = app.getSavingTrackHelper();

		setIcons(TRIP_RECORDING_TIME);
		updateInfo(null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> askShowBatteryOptimizationDialog();
	}

	private void askShowBatteryOptimizationDialog() {
		BatteryOptimizationController.askShowDialog(mapActivity, true, activity -> askShowTrackMenuDialog());
	}

	private void askShowTrackMenuDialog() {
		if (cachedTimeSpan > 0) {
			Bundle params = new Bundle();
			params.putString(TrackMenuFragment.OPEN_TAB_NAME, TrackMenuTab.TRACK.name());
			TrackMenuFragment.showInstance(mapActivity, savingTrackHelper.getCurrentTrack(), null,
					null, null, params);
		}
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float timeSpan = getTimeSpan();
		if (cachedTimeSpan != timeSpan) {
			cachedTimeSpan = timeSpan;
			String formattedTime = Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled());
			setText(formattedTime, null);
		}
	}

	private float getTimeSpan() {
		SelectedGpxFile currentTrack = savingTrackHelper.getCurrentTrack();
		GpxFile gpxFile = currentTrack.getGpxFile();
		boolean withoutGaps = !currentTrack.isJoinSegments()
				&& (Algorithms.isEmpty(gpxFile.getTracks()) || gpxFile.getTracks().get(0).isGeneralTrack());
		GpxTrackAnalysis analysis = currentTrack.getTrackAnalysis(app);
		return withoutGaps ? analysis.getTimeSpanWithoutGaps() : analysis.getTimeSpan();
	}
}