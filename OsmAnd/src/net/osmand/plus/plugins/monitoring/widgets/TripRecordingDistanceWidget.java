package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.showOnMap;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DISTANCE;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingDistanceWidgetState.TripRecordingDistanceMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.gpx.ElevationDiffsCalculator.SlopeInfo;
import net.osmand.shared.gpx.GpxTrackAnalysis;

import java.util.ArrayList;
import java.util.List;

public class TripRecordingDistanceWidget extends SimpleWidget {

	private static final long BLINK_DELAY_MILLIS = 500;

	protected int currentTrackIndex;
	protected SlopeInfo slopeInfo;

	private final TripRecordingDistanceWidgetState distanceWidgetState;
	private final SavingTrackHelper savingTrackHelper;

	private long cachedLastUpdateTime;

	public TripRecordingDistanceWidget(@NonNull MapActivity mapActivity, @NonNull TripRecordingDistanceWidgetState distanceWidgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, TRIP_RECORDING_DISTANCE, customId, widgetsPanel);
		savingTrackHelper = app.getSavingTrackHelper();
		this.distanceWidgetState = distanceWidgetState;
		updateInfo(null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			OsmandMonitoringPlugin plugin = getPlugin();
			if (plugin != null) {
				plugin.askShowTripRecordingDialog(mapActivity);
			}
		};
	}

	public TripRecordingDistanceWidgetState getDistanceWidgetState() {
		return distanceWidgetState;
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		OsmandMonitoringPlugin plugin = getPlugin();
		if (plugin == null) {
			return;
		}

		if (plugin.isSaving()) {
			setText(getString(R.string.shared_string_save), null);
			setIcons(R.drawable.widget_monitoring_rec_big_day, R.drawable.widget_monitoring_rec_big_night);
			return;
		}

		TripRecordingDistanceMode recordingDistanceMode = distanceWidgetState.getDistanceModePreference().get();
		if (recordingDistanceMode == TripRecordingDistanceMode.TOTAL_DISTANCE) {
			long lastUpdateTime = cachedLastUpdateTime;
			boolean globalRecording = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
			boolean recording = savingTrackHelper.getIsRecording();
			boolean liveMonitoring = plugin.isLiveMonitoringEnabled();
			float distance = savingTrackHelper.getDistance();

			setText(distance);
			setIcons(globalRecording, liveMonitoring, recording);

			if (distance > 0) {
				lastUpdateTime = app.getSavingTrackHelper().getLastTimeUpdated();
			}

			if (lastUpdateTime != cachedLastUpdateTime && (globalRecording || recording)) {
				cachedLastUpdateTime = lastUpdateTime;

				// Make bling effect
				setIcons(false, liveMonitoring, true);
				app.runInUIThread(() -> setIcons(globalRecording, liveMonitoring, !globalRecording), BLINK_DELAY_MILLIS);
			}
		} else {
			setLastSlopeDistance(recordingDistanceMode);
			setIcons(distanceWidgetState.getDistanceModePreference().get().getIcon(false), distanceWidgetState.getDistanceModePreference().get().getIcon(true));
		}
	}

	private void setLastSlopeDistance(@NonNull TripRecordingDistanceMode recordingDistanceMode) {
		int currentTrackIndex = savingTrackHelper.getCurrentTrackIndex();
		GpxTrackAnalysis analysis = savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
		if (this.currentTrackIndex != currentTrackIndex) {
			slopeInfo = null;
		}
		SlopeInfo newSlopeInfo = recordingDistanceMode == TripRecordingDistanceMode.LAST_DOWNHILL ? analysis.getLastDownhill() : analysis.getLastUphill();
		if (newSlopeInfo == null) {
			setText(0);
			return;
		}

		if (slopeInfo == null
				|| slopeInfo.getStartPointIndex() != newSlopeInfo.getStartPointIndex()
				|| slopeInfo.getElevDiff() < newSlopeInfo.getElevDiff()) {
			slopeInfo = newSlopeInfo;
		}
		if (slopeInfo != null) {
			setText((float) slopeInfo.getDistance());
		}
	}

	@Nullable
	@Override
	protected String getAdditionalWidgetName() {
		if (distanceWidgetState != null) {
			return getString(distanceWidgetState.getDistanceModePreference().get().titleId);
		}
		return null;
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

	@StringRes
	protected int getAdditionalWidgetNameDivider() {
		return R.string.ltr_or_rtl_combine_via_colon;
	}

	private void setText(float distance) {
		if (distance > 0) {
			FormattedValue formattedDistance = OsmAndFormatter
					.getFormattedDistanceValue(distance, app);
			setText(formattedDistance.value, formattedDistance.unit);
		} else {
			setText(getString(R.string.monitoring_control_start), null);
		}
	}

	private void setIcons(boolean globalRecording, boolean liveMonitoring, boolean recording) {
		int dayIconId = defineIconId(globalRecording, liveMonitoring, recording, false);
		int nightIconId = defineIconId(globalRecording, liveMonitoring, recording, true);
		setIcons(dayIconId, nightIconId);
	}

	@DrawableRes
	private int defineIconId(boolean globalRecording, boolean liveMonitoring, boolean recording, boolean night) {
		if (globalRecording) {
			return getGlobalRecordingIconId(liveMonitoring, night);
		} else if (recording) {
			return getNonGlobalRecordingIconId(liveMonitoring, night);
		}
		return night
				? R.drawable.widget_monitoring_rec_inactive_night
				: R.drawable.widget_monitoring_rec_inactive_day;
	}

	@DrawableRes
	private int getGlobalRecordingIconId(boolean liveMonitoring, boolean night) {
		// Indicates global recording (+background recording)
		if (liveMonitoring) {
			return night
					? R.drawable.widget_live_monitoring_rec_big_night
					: R.drawable.widget_live_monitoring_rec_big_day;
		} else {
			return night
					? R.drawable.widget_monitoring_rec_big_night
					: R.drawable.widget_monitoring_rec_big_day;
		}
	}

	@DrawableRes
	private int getNonGlobalRecordingIconId(boolean liveMonitoring, boolean night) {
		// Indicates (profile-based, configured in settings) recording
		if (liveMonitoring) {
			return night
					? R.drawable.widget_live_monitoring_rec_small_night
					: R.drawable.widget_live_monitoring_rec_small_day;
		} else {
			return night
					? R.drawable.widget_monitoring_rec_small_night
					: R.drawable.widget_monitoring_rec_small_day;
		}
	}

	@Nullable
	private OsmandMonitoringPlugin getPlugin() {
		return PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
	}
}