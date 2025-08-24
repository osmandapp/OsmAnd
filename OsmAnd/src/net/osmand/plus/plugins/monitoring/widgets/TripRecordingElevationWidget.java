package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DOWNHILL;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_UPHILL;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.settings.controllers.BatteryOptimizationController;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.shared.settings.enums.AltitudeMetrics;

public abstract class TripRecordingElevationWidget extends SimpleWidget {

	private final SavingTrackHelper savingTrackHelper;
	private int currentTrackIndex;

	private double cachedElevationDiff = -1;

	public TripRecordingElevationWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId ,widgetsPanel);
		savingTrackHelper = app.getSavingTrackHelper();

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
		if (getAnalysis().hasElevationData()) {
			Bundle params = new Bundle();
			params.putString(TrackMenuFragment.OPEN_TAB_NAME, TrackMenuTab.TRACK.name());
			params.putString(TrackMenuFragment.CHART_TAB_NAME, GPXTabItemType.GPX_TAB_ITEM_ALTITUDE.name());
			TrackMenuFragment.showInstance(mapActivity, savingTrackHelper.getCurrentTrack(), null,
					null, null, params);
		}
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int currentTrackIndex = savingTrackHelper.getCurrentTrackIndex();
		double elevationDiff = getElevationDiff(this.currentTrackIndex != currentTrackIndex);
		this.currentTrackIndex = currentTrackIndex;
		if (isUpdateNeeded() || cachedElevationDiff != elevationDiff) {
			cachedElevationDiff = elevationDiff;
			AltitudeMetrics altitudeMetrics = settings.ALTITUDE_METRIC.get();
			FormattedValue formattedUphill = OsmAndFormatter.getFormattedAltitudeValue(elevationDiff, app, altitudeMetrics);
			setText(formattedUphill.value, formattedUphill.unit);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	protected abstract double getElevationDiff(boolean reset);

	@NonNull
	protected GpxTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}

	public static class TripRecordingUphillWidget extends TripRecordingElevationWidget {

		private double diffElevationUp;

		public TripRecordingUphillWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
			super(mapActivity, TRIP_RECORDING_UPHILL, customId, widgetsPanel);
			setIcons(TRIP_RECORDING_UPHILL);
		}

		@Override
		protected double getElevationDiff(boolean reset) {
			if (reset) {
				diffElevationUp = 0;
			}
			diffElevationUp = Math.max(getAnalysis().getDiffElevationUp(), diffElevationUp);
			return diffElevationUp;
		}
	}

	public static class TripRecordingDownhillWidget extends TripRecordingElevationWidget {

		private double diffElevationDown;

		public TripRecordingDownhillWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
			super(mapActivity, TRIP_RECORDING_DOWNHILL, customId, widgetsPanel);
			setIcons(TRIP_RECORDING_DOWNHILL);
		}

		@Override
		protected double getElevationDiff(boolean reset) {
			if (reset) {
				diffElevationDown = 0;
			}
			diffElevationDown = Math.max(getAnalysis().getDiffElevationDown(), diffElevationDown);
			return diffElevationDown;
		}
	}
}