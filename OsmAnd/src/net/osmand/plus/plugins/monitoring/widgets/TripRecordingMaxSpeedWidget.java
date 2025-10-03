package net.osmand.plus.plugins.monitoring.widgets;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingMaxSpeedWidgetState.MaxSpeedMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.shared.gpx.ElevationDiffsCalculator;
import net.osmand.shared.gpx.GpxTrackAnalysis;

public class TripRecordingMaxSpeedWidget extends SimpleWidget {
	private final TripRecordingMaxSpeedWidgetState widgetState;


	protected final SavingTrackHelper savingTrackHelper;
	protected int currentTrackIndex;

	protected double cachedMaxSpeed = -1;
	private int lastMaxSpeed;
	private boolean forceUpdate;


	public TripRecordingMaxSpeedWidget(@NonNull MapActivity mapActivity, @NonNull TripRecordingMaxSpeedWidgetState widgetState, @NonNull WidgetType widgetType,
	                                @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, widgetsPanel);
		this.widgetState = widgetState;
		savingTrackHelper = app.getSavingTrackHelper();
		updateWidgetView();
		updateInfo(null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			forceUpdate = true;
			widgetState.changeToNextState();
			updateInfo(null);
			mapActivity.refreshMap();
			updateWidgetName();
			updateIcon();
		};
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int currentTrackIndex = savingTrackHelper.getCurrentTrackIndex();
		double maxSpeed = getMaxSpeed(this.currentTrackIndex != currentTrackIndex);
		this.currentTrackIndex = currentTrackIndex;
		if (forceUpdate || isUpdateNeeded() || cachedMaxSpeed != maxSpeed) {
			cachedMaxSpeed = maxSpeed;
			forceUpdate = false;
			setText(String.valueOf(maxSpeed), "%");
		}
	}

	protected double getMaxSpeed(boolean reset) {
		if (reset) {
			lastMaxSpeed = 0;
		}
		MaxSpeedMode mode = widgetState.getMaxSpeedModePreference().get();
		if(mode == MaxSpeedMode.TOTAL){
			lastMaxSpeed = (int) getAnalysis().getMaxSpeed();
		} else{
			/*ElevationDiffsCalculator.SlopeInfo slopeInfo = mode == MaxSpeedMode.LAST_UPHILL ? getAnalysis().getLastUphill() : getAnalysis().getLastDownhill();
			if (slopeInfo != null) {
				lastMaxSpeed = (int) (slopeInfo.() / slopeInfo.getDistance() * 100);
			}*/
		}
		return lastMaxSpeed;
	}

	@Override
	public int getIconId(boolean nightMode) {
		return widgetState.getMaxSpeedModePreference().get().getIcon(nightMode);
	}

	@Nullable
	@Override
	protected String getAdditionalWidgetName() {
		if (widgetState != null) {
			return getString(widgetState.getMaxSpeedModePreference().get().titleId);
		}
		return null;
	}

	@StringRes
	protected int getAdditionalWidgetNameDivider() {
		return R.string.ltr_or_rtl_combine_via_colon;
	}

	@NonNull
	protected GpxTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}

	@Nullable
	public OsmandPreference<MaxSpeedMode> getMaxSpeedModeOsmandPreference() {
		return widgetState != null ? widgetState.getMaxSpeedModePreference() : null;
	}
}
