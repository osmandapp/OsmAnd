package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_UPDATE_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.UPDATE_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget.SpeedLimitWarningState.*;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState.WidgetSize;

public class SpeedometerWidget {

	private final static int PREVIEW_VALUE = 85;

	public final View view;
	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ApplicationMode mode;
	private final OsmandPreference<Boolean> showWidgetPref;
	private final OsmandPreference<WidgetSize> sizePref;
	private final CommonPreference<SpeedLimitWarningState> speedLimitPref;

	private WidgetSize previousWidgetSize;

	private final TextView speedLimitValueView;
	private final TextView speedometerValueView;
	private final TextView speedometerUnitsView;
	private final TextView speedLimitDescription;

	private final View speedLimitContainer;
	private final View speedometerContainer;

	protected final OsmAndLocationProvider locationProvider;
	private final RoutingHelper routingHelper;
	private final WaypointHelper wh;

	private float cachedSpeed;


	public SpeedometerWidget(View view, OsmandApplication app, ApplicationMode mode) {
		this.view = view;
		this.app = app;
		this.mode = mode;
		this.locationProvider = app.getLocationProvider();

		speedometerContainer = view.findViewById(R.id.speedometer_container);
		speedLimitContainer = view.findViewById(R.id.speed_limit_container);
		speedometerValueView = view.findViewById(R.id.speedometer_value);
		speedLimitValueView = view.findViewById(R.id.speed_limit_value);
		speedometerUnitsView = view.findViewById(R.id.speedometer_units);
		speedLimitDescription = speedLimitContainer.findViewById(R.id.limit_description);

		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		wh = app.getWaypointHelper();

		showWidgetPref = settings.SHOW_SPEEDOMETER;
		sizePref = settings.SPEEDOMETER_SIZE;
		speedLimitPref = settings.SHOW_SPEED_LIMIT_WARNING;

		setupWidget();
	}

	private void setupWidget() {
		WidgetSize newWidgetSize = sizePref.getModeValue(mode);
		if (previousWidgetSize == newWidgetSize) {
			return;
		}

		previousWidgetSize = newWidgetSize;
		LinearLayout.LayoutParams speedometerLayoutParams = (LinearLayout.LayoutParams) speedometerContainer.getLayoutParams();
		LinearLayout.LayoutParams speedLimitLayoutParams = (LinearLayout.LayoutParams) speedLimitContainer.getLayoutParams();
		FrameLayout.LayoutParams speedLimitDescriptionParams = (FrameLayout.LayoutParams) speedLimitDescription.getLayoutParams();
		switch (previousWidgetSize) {
			case MEDIUM:
				speedometerLayoutParams.height = dpToPx(72);
				speedometerLayoutParams.width = dpToPx(88);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(9), 0, dpToPx(9), 0);
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);

				speedLimitLayoutParams.height = dpToPx(72);
				speedLimitLayoutParams.width = dpToPx(72);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitDescriptionParams.setMargins(0, dpToPx(10), 0, 0);
				break;
			case LARGE:
				speedometerLayoutParams.height = dpToPx(96);
				speedometerLayoutParams.width = dpToPx(126);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(9), dpToPx(0), dpToPx(9), dpToPx(0));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);

				speedLimitLayoutParams.height = dpToPx(94);
				speedLimitLayoutParams.width = dpToPx(94);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
				speedLimitDescriptionParams.setMargins(0, dpToPx(14), 0, 0);
				break;
		}
		FrameLayout.LayoutParams speedLimitValueParams = (FrameLayout.LayoutParams) speedLimitValueView.getLayoutParams();
		speedLimitValueParams.setMargins(0, isUsaRegion() ? dpToPx(2) : 0, 0, 0);
		AndroidUiHelper.updateVisibility(speedLimitDescription, isUsaRegion());
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	public void updatePreviewInfo(boolean nightMode) {
		setupWidget();
		updateColor(nightMode);
		OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(PREVIEW_VALUE, app);
		setSpeedText(String.valueOf(PREVIEW_VALUE), formattedSpeed.unit);
		setSpeedLimitText(String.valueOf(PREVIEW_VALUE));
		AndroidUiHelper.updateVisibility(speedLimitContainer, speedLimitPref.getModeValue(mode) == ALWAYS);
	}

	public void updateInfo(@Nullable DrawSettings drawSettings, boolean drawBitmap) {
		setupWidget();
		updateColor(drawSettings != null ? drawSettings.isNightMode() : app.getDaynightHelper().isNightMode(true));
		boolean showSpeedometer = showWidgetPref.getModeValue(mode);

		if (routingHelper.isFollowingMode() && showSpeedometer) {
			Location location = locationProvider.getLastKnownLocation();
			if (location != null && location.hasSpeed()) {
				float updateThreshold = cachedSpeed < LOW_SPEED_THRESHOLD_MPS
						? LOW_SPEED_UPDATE_THRESHOLD_MPS
						: UPDATE_THRESHOLD_MPS;
				if (Math.abs(location.getSpeed() - cachedSpeed) > updateThreshold) {
					cachedSpeed = location.getSpeed();
					OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
					setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				}

				AlarmInfo alarm = getSpeedLimitInfo();
				if (alarm != null) {
					setSpeedLimitText(String.valueOf(alarm.getIntValue()));
					AndroidUiHelper.updateVisibility(speedLimitContainer, true);
				} else {
					AndroidUiHelper.updateVisibility(speedLimitContainer, false);
				}
				AndroidUiHelper.updateVisibility(view, true);
			} else if (cachedSpeed != 0) {
				cachedSpeed = 0;
				setSpeedText(null, null);
				AndroidUiHelper.updateVisibility(view, true);
			} else {
				AndroidUiHelper.updateVisibility(view, false);
			}
		} else {
			AndroidUiHelper.updateVisibility(view, false);
		}
	}

	@Nullable
	private AlarmInfo getSpeedLimitInfo() {
		AlarmInfo alarm = wh.getSpeedLimitAlarm(settings.SPEED_SYSTEM.get(), speedLimitPref.getModeValue(mode) == WHEN_EXCEEDED);
		if (alarm == null) {
			RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
			Location loc = locationProvider.getLastKnownLocation();
			if (ro != null && loc != null) {
				alarm = wh.calculateSpeedLimitAlarm(ro, loc, settings.SPEED_SYSTEM.get(), speedLimitPref.getModeValue(mode) == WHEN_EXCEEDED);
			}
		}
		return alarm;
	}

	private void updateColor(boolean nightMode) {
		Drawable speedometerDrawable = speedometerContainer.getBackground();
		GradientDrawable gradientDrawable = (GradientDrawable) speedometerDrawable;
		gradientDrawable.setColor(ColorUtilities.getWidgetBackgroundColor(app, nightMode));

		speedLimitContainer.setBackground(AppCompatResources.getDrawable(app, isUsaRegion() ? R.drawable.speed_limit_usa_shape : R.drawable.speed_limit_shape));
		Drawable speedLimitDrawable = speedLimitContainer.getBackground();
		LayerDrawable layerDrawable = (LayerDrawable) speedLimitDrawable;
		GradientDrawable backgroundDrawable = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.background);
		backgroundDrawable.setColor(ColorUtilities.getWidgetSecondaryBackgroundColor(app, nightMode));
		if (!isUsaRegion()) {
			backgroundDrawable.setStroke(20, ContextCompat.getColor(app, nightMode ? R.color.map_window_stroke_dark : R.color.widget_background_color_light));
		}

		speedometerValueView.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
	}

	private void setSpeedText(String value, String units) {
		speedometerValueView.setText(value);
		speedometerUnitsView.setText(units);
	}

	private void setSpeedLimitText(String value) {
		speedLimitValueView.setText(value);
	}

	private boolean isUsaRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.US;
	}

	public void setVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(view, visible);
	}

	public enum SpeedLimitWarningState {
		ALWAYS(R.string.shared_string_always),
		WHEN_EXCEEDED(R.string.when_exceeded);
		final int stringId;

		SpeedLimitWarningState(int stringId) {
			this.stringId = stringId;
		}

		public String toHumanString(OsmandApplication app) {
			return app.getString(stringId);
		}
	}
}
