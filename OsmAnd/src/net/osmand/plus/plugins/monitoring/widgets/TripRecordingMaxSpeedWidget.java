package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.showOnMap;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingMaxSpeedWidgetState.MaxSpeedMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.gpx.ElevationDiffsCalculator.SlopeInfo;
import net.osmand.shared.gpx.GpxTrackAnalysis;

import java.util.ArrayList;
import java.util.List;

public class TripRecordingMaxSpeedWidget extends BaseRecordingWidget {
	private final TripRecordingMaxSpeedWidgetState widgetState;

	protected final SavingTrackHelper savingTrackHelper;
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
		super.updateSimpleWidgetInfo(drawSettings);
		float maxSpeed = getMaxSpeed();
		if (forceUpdate || isUpdateNeeded() || cachedMaxSpeed != maxSpeed) {
			cachedMaxSpeed = maxSpeed;
			forceUpdate = false;
			FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(maxSpeed, app);
			setText(formattedSpeed.value, formattedSpeed.unit);
		}
	}

	protected float getMaxSpeed() {
		MaxSpeedMode mode = widgetState.getMaxSpeedModePreference().get();
		if (mode == MaxSpeedMode.TOTAL) {
			lastMaxSpeed = (int) getAnalysis().getMaxSpeed();
		} else {
			return getLastSlopeMaxSpeed(mode);
		}
		return lastMaxSpeed;
	}

	private float getLastSlopeMaxSpeed(@NonNull MaxSpeedMode mode) {
		SlopeInfo lastSlope = getLastSlope(mode == MaxSpeedMode.LAST_UPHILL);
		if (lastSlope != null) {
			return (float) lastSlope.getMaxSpeed();
		} else {
			return 0;
		}
	}

	@Override
	protected void resetCachedValue(){
		super.resetCachedValue();
		lastMaxSpeed = 0;
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
