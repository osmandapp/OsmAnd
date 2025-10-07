package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DOWNHILL;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_UPHILL;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidgetState.TripRecordingElevationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.gpx.ElevationDiffsCalculator.SlopeInfo;
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

import java.util.ArrayList;
import java.util.List;

public abstract class TripRecordingElevationWidget extends SimpleWidget {

	protected final TripRecordingElevationWidgetState widgetState;
	protected final SavingTrackHelper savingTrackHelper;
	protected int currentTrackIndex;
	protected SlopeInfo slopeInfo;
	protected double cachedElevationDiff = -1;
	protected double cachedLastElevation = -1;
	private final boolean isUphillType;

	public TripRecordingElevationWidget(@NonNull MapActivity mapActivity, @NonNull TripRecordingElevationWidgetState widgetState,
	                                    @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId ,widgetsPanel);
		this.widgetState = widgetState;
		isUphillType = widgetState.isUphillType;
		this.savingTrackHelper = app.getSavingTrackHelper();
		updateInfo(null);
		updateWidgetName();
		updateIcon();
	}

	public boolean isUphillType() {
		return isUphillType;
	}

	@Nullable
	public OsmandPreference<TripRecordingElevationMode> elevationModePreference() {
		return widgetState != null ? widgetState.getElevationModePreference() : null;
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
		double lastElevation = getLastElevation(this.currentTrackIndex != currentTrackIndex);
		this.currentTrackIndex = currentTrackIndex;

		if (isUpdateNeeded() || cachedElevationDiff != elevationDiff || cachedLastElevation != lastElevation) {
			cachedElevationDiff = elevationDiff;
			cachedLastElevation = lastElevation;
			AltitudeMetrics altitudeMetrics = settings.ALTITUDE_METRIC.get();
			FormattedValue formattedUphill = null;

			if(widgetState.getElevationModePreference().get() == TripRecordingElevationMode.TOTAL){
				formattedUphill = OsmAndFormatter.getFormattedAltitudeValue(elevationDiff, app, altitudeMetrics);
			} else{
				formattedUphill = OsmAndFormatter.getFormattedAltitudeValue(lastElevation, app, altitudeMetrics);
			}
			setText(formattedUphill.value, formattedUphill.unit);
		}
	}

	@Nullable
	@Override
	protected String getAdditionalWidgetName() {
		if (widgetState != null) {
			return getString(widgetState.getModeTitleId());
		}
		return null;
	}

	@StringRes
	protected int getAdditionalWidgetNameDivider() {
		return R.string.ltr_or_rtl_combine_via_colon;
	}

	@Override
	public int getIconId(boolean nightMode) {
		return widgetState.getElevationModePreference().get().getIcon(isUphillType(), nightMode);
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	@Nullable
	@Override
	protected List<PopUpMenuItem> getWidgetActions() {
		List<PopUpMenuItem> actions = new ArrayList<>();
		UiUtilities uiUtilities = app.getUIUtilities();
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		actions.add(new PopUpMenuItem.Builder(app)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_center_on_track, iconColor))
				.setTitleId(R.string.show_track_on_map)
				.setOnClickListener(item -> showOnMap(mapActivity))
				.showTopDivider(true)
				.create());
		return actions;
	}

	public static void showOnMap(@NonNull MapActivity mapActivity) {
		TrackMenuFragment.showInstance(mapActivity, mapActivity.getApp().getSavingTrackHelper().getCurrentTrack(), null);
	}

	protected abstract double getElevationDiff(boolean reset);
	protected abstract double getLastElevation(boolean reset);

	@NonNull
	protected GpxTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}

	public static class TripRecordingUphillWidget extends TripRecordingElevationWidget {

		private double diffElevationUp;

		public TripRecordingUphillWidget(@NonNull MapActivity mapActivity, @NonNull TripRecordingElevationWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
			super(mapActivity, widgetState, TRIP_RECORDING_UPHILL, customId, widgetsPanel);
		}

		@Override
		protected double getElevationDiff(boolean reset) {
			if (reset) {
				diffElevationUp = 0;
			}
			diffElevationUp = Math.max(getAnalysis().getDiffElevationUp(), diffElevationUp);
			return diffElevationUp;
		}

		protected double getLastElevation(boolean reset) {
			if (reset) {
				slopeInfo = null;
			}
			SlopeInfo newSlopeInfo = getAnalysis().getLastUphill();
			if (newSlopeInfo == null) {
				return slopeInfo != null ? slopeInfo.getElevDiff() : 0;
			}

			if (slopeInfo == null
					|| slopeInfo.getStartPointIndex() != newSlopeInfo.getStartPointIndex()
					|| slopeInfo.getElevDiff() < newSlopeInfo.getElevDiff()) {
				slopeInfo = newSlopeInfo;
			}
			return slopeInfo.getElevDiff();
		}
	}

	public static class TripRecordingDownhillWidget extends TripRecordingElevationWidget {
		private double diffElevationDown;

		public TripRecordingDownhillWidget(@NonNull MapActivity mapActivity, @NonNull TripRecordingElevationWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
			super(mapActivity, widgetState, TRIP_RECORDING_DOWNHILL, customId, widgetsPanel);
		}

		@Override
		protected double getElevationDiff(boolean reset) {
			if (reset) {
				diffElevationDown = 0;
			}
			diffElevationDown = Math.max(getAnalysis().getDiffElevationDown(), diffElevationDown);
			return diffElevationDown;
		}

		protected double getLastElevation(boolean reset) {
			if (reset) {
				slopeInfo = null;
			}
			SlopeInfo newSlopeInfo = getAnalysis().getLastDownhill();
			if (newSlopeInfo == null) {
				return slopeInfo != null ? slopeInfo.getElevDiff() : 0;
			}

			if (slopeInfo == null
					|| slopeInfo.getStartPointIndex() != newSlopeInfo.getStartPointIndex()
					|| slopeInfo.getElevDiff() < newSlopeInfo.getElevDiff()) {
				slopeInfo = newSlopeInfo;
			}
			return slopeInfo.getElevDiff();
		}
	}
}