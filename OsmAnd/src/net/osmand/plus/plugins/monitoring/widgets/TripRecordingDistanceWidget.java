package net.osmand.plus.plugins.monitoring.widgets;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TripRecordingDistanceWidget extends TextInfoWidget {

	private static final long BLINK_DELAY_MILLIS = 500;

	private final SavingTrackHelper savingTrackHelper;

	private long cachedLastUpdateTime;

	public TripRecordingDistanceWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		savingTrackHelper = app.getSavingTrackHelper();

		updateInfo(null);
		setOnClickListener(v -> {
			OsmandMonitoringPlugin plugin = getPlugin();
			if (plugin != null) {
				plugin.showTripRecordingDialog(mapActivity);
			}
		});
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		OsmandMonitoringPlugin plugin = getPlugin();
		if (plugin == null) {
			return;
		}

		if (plugin.isSaving()) {
			setText(getString(R.string.shared_string_save), null);
			setIcons(R.drawable.widget_monitoring_rec_big_day, R.drawable.widget_monitoring_rec_big_night);
			return;
		}

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
	}

	private void setText(float distance) {
		if (distance > 0) {
			FormattedValue formattedDistance = OsmAndFormatter
					.getFormattedDistanceValue(distance, app, true, settings.METRIC_SYSTEM.get());
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
		return OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
	}
}