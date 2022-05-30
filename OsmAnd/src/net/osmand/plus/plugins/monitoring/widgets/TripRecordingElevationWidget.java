package net.osmand.plus.plugins.monitoring.widgets;

import android.os.Bundle;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.ui.GPXTabItemType;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.track.cards.SegmentsCard;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuType;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class TripRecordingElevationWidget extends TextInfoWidget {

	private final SavingTrackHelper savingTrackHelper;

	private double cachedElevationDiff = -1;

	public TripRecordingElevationWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		savingTrackHelper = app.getSavingTrackHelper();

		updateInfo(null);
		setOnClickListener(v -> {
			if (getAnalysis().hasElevationData) {
				Bundle params = new Bundle();
				params.putString(TrackMenuFragment.OPEN_TAB_NAME, TrackMenuType.TRACK.name());
				params.putString(TrackMenuFragment.CHART_TAB_NAME, GPXTabItemType.GPX_TAB_ITEM_ALTITUDE.name());
				TrackMenuFragment.showInstance(mapActivity, savingTrackHelper.getCurrentTrack(), null,
						null, null, params);
			}
		});
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		double elevationDiff = getElevationDiff();
		if (cachedElevationDiff != elevationDiff) {
			cachedElevationDiff = elevationDiff;
			MetricsConstants metricsConstants = settings.METRIC_SYSTEM.get();
			FormattedValue formattedUphill = OsmAndFormatter.getFormattedAltitudeValue(elevationDiff, app, metricsConstants);
			setText(formattedUphill.value, formattedUphill.unit);
		}
	}

	protected abstract double getElevationDiff();

	@NonNull
	protected GPXTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}

	public static class TripRecordingUphillWidget extends TripRecordingElevationWidget {

		public TripRecordingUphillWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity);
			setIcons(WidgetParams.TRIP_RECORDING_UPHILL);
		}

		@Override
		protected double getElevationDiff() {
			return getAnalysis().diffElevationUp;
		}
	}

	public static class TripRecordingDownhillWidget extends TripRecordingElevationWidget {

		public TripRecordingDownhillWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity);
			setIcons(WidgetParams.TRIP_RECORDING_DOWNHILL);
		}

		@Override
		protected double getElevationDiff() {
			return getAnalysis().diffElevationDown;
		}
	}
}