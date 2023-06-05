package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DOWNHILL;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_UPHILL;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public abstract class TripRecordingElevationWidget extends TextInfoWidget {

	private final SavingTrackHelper savingTrackHelper;

	private double cachedElevationDiff = -1;

	public TripRecordingElevationWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType) {
		super(mapActivity, widgetType);
		savingTrackHelper = app.getSavingTrackHelper();

		updateInfo(null);
		setOnClickListener(v -> {
			if (getAnalysis().hasElevationData()) {
				Bundle params = new Bundle();
				params.putString(TrackMenuFragment.OPEN_TAB_NAME, TrackMenuTab.TRACK.name());
				params.putString(TrackMenuFragment.CHART_TAB_NAME, GPXTabItemType.GPX_TAB_ITEM_ALTITUDE.name());
				TrackMenuFragment.showInstance(mapActivity, savingTrackHelper.getCurrentTrack(), null,
						null, null, params);
			}
		});
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		double elevationDiff = getElevationDiff();
		if (isUpdateNeeded() || cachedElevationDiff != elevationDiff) {
			cachedElevationDiff = elevationDiff;
			MetricsConstants metricsConstants = settings.METRIC_SYSTEM.get();
			FormattedValue formattedUphill = OsmAndFormatter.getFormattedAltitudeValue(elevationDiff, app, metricsConstants);
			setText(formattedUphill.value, formattedUphill.unit);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	protected abstract double getElevationDiff();

	@NonNull
	protected GPXTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}

	public static class TripRecordingUphillWidget extends TripRecordingElevationWidget {

		public TripRecordingUphillWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity, TRIP_RECORDING_UPHILL);
			setIcons(TRIP_RECORDING_UPHILL);
		}

		@Override
		protected double getElevationDiff() {
			return getAnalysis().diffElevationUp;
		}
	}

	public static class TripRecordingDownhillWidget extends TripRecordingElevationWidget {

		public TripRecordingDownhillWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity, TRIP_RECORDING_DOWNHILL);
			setIcons(TRIP_RECORDING_DOWNHILL);
		}

		@Override
		protected double getElevationDiff() {
			return getAnalysis().diffElevationDown;
		}
	}
}