package net.osmand.plus.plugins.monitoring.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.shared.gpx.ElevationDiffsCalculator.SlopeInfo;
import net.osmand.shared.gpx.GpxTrackAnalysis;

public class BaseLastSlopeRecordingWidget extends SimpleWidget {

	protected int currentTrackIndex;
	protected final SavingTrackHelper savingTrackHelper;

	protected SlopeInfo slopeUphillInfo;
	protected SlopeInfo slopeDownhillInfo;


	public BaseLastSlopeRecordingWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, widgetType, customId, panel);
		this.savingTrackHelper = app.getSavingTrackHelper();
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int currentTrackIndex = savingTrackHelper.getCurrentTrackIndex();
		if (this.currentTrackIndex != currentTrackIndex) {
			resetCachedValue();
		}
		this.currentTrackIndex = currentTrackIndex;

		slopeUphillInfo = updateSlopeInfo(slopeUphillInfo, getAnalysis().getLastUphill());
		slopeDownhillInfo = updateSlopeInfo(slopeDownhillInfo, getAnalysis().getLastDownhill());
	}

	private SlopeInfo updateSlopeInfo(@Nullable SlopeInfo oldInfo, @Nullable SlopeInfo newInfo) {
		if (oldInfo == null) {
			return newInfo;
		}
		if (newInfo == null) {
			return oldInfo;
		}

		boolean isSameSlope = oldInfo.getStartPointIndex() == newInfo.getStartPointIndex();
		boolean isNextSlope = oldInfo.getStartPointIndex() < newInfo.getStartPointIndex();

		if (isSameSlope) {
			oldInfo.setElevDiff(Math.max(oldInfo.getElevDiff(), newInfo.getElevDiff()));
			oldInfo.setDistance(Math.max(oldInfo.getDistance(), newInfo.getDistance()));
			oldInfo.setMaxSpeed(Math.max(oldInfo.getMaxSpeed(), newInfo.getMaxSpeed()));
			return oldInfo;
		} else if (isNextSlope) {
			return newInfo;
		} else {
			return oldInfo;
		}
	}

	protected void resetCachedValue(){
		slopeUphillInfo = null;
		slopeDownhillInfo = null;
	}

	protected SlopeInfo getLastSlope(boolean isUphill) {
		return isUphill ? slopeUphillInfo : slopeDownhillInfo;
	}

	@NonNull
	protected GpxTrackAnalysis getAnalysis() {
		return savingTrackHelper.getCurrentTrack().getTrackAnalysis(app);
	}
}
