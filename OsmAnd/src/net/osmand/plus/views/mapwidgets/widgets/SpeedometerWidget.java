package net.osmand.plus.views.mapwidgets.widgets;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.settings.enums.SpeedLimitWarningState.ALWAYS;
import static net.osmand.plus.settings.enums.SpeedLimitWarningState.WHEN_EXCEEDED;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_UPDATE_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.UPDATE_THRESHOLD_MPS;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.views.mapwidgets.widgets.speedometer.SpeedState;
import net.osmand.plus.views.mapwidgets.widgets.speedometer.SpeedometerAnimationDrawable;
import net.osmand.router.RouteSegmentResult;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

import java.util.List;

public class SpeedometerWidget {

	private final static int PREVIEW_VALUE = 85;

	private final static int SPEEDOMETER_AA_STROKE = 3;
	private final static int SPEEDOMETER_WIDTH_M = 72;
	private final static int SPEEDOMETER_WIDTH_M_AA = 66 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_HEIGHT_M = 72;
	private final static int SPEEDOMETER_HEIGHT_AA_M = 66 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_WIDTH_L = 96;
	private final static int SPEEDOMETER_WIDTH_AA_L = 93 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_HEIGHT_S = 56;
	private final static int SPEEDOMETER_HEIGHT_AA_S = 50 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_WIDTH_S = 56;
	private final static int SPEEDOMETER_WIDTH_AA_S = 50 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_HEIGHT_L = 96;
	private final static int SPEEDOMETER_HEIGHT_AA_L = 93 + SPEEDOMETER_AA_STROKE * 2;
	private final static int SPEEDOMETER_TEXT_SIZE_S = 24;
	private final static int SPEEDOMETER_TEXT_SIZE_M = 36;
	private final static int SPEEDOMETER_UNIT_TEXT_SIZE = 11;
	private final static int SPEEDOMETER_TEXT_SIZE_L = 60;
	private final static int SPEED_LIMIT_TEXT_SIZE_S = 24;
	private final static int SPEED_LIMIT_TEXT_SIZE_M = 24;
	private final static int SPEED_LIMIT_TEXT_SIZE_L = 36;
	private final static int SPEED_LIMIT_SIZE_S = 56;
	private final static int SPEED_LIMIT_SIZE_M = 72;
	private final static int SPEED_LIMIT_SIZE_L = 96;
	private final static int SPEED_LIMIT_DESCRIPTION_SIZE_USUAL = 11;
	private final static int SPEED_LIMIT_DESCRIPTION_SIZE_CANADA_S = 7;
	private final static int SPEED_LIMIT_DESCRIPTION_SIZE_CANADA_M = 9;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_S = 7;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_S = 5;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_M = 9;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_M = 7;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_L = 12;
	private final static int SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_L = 9;
	private final static int SPEED_LIMIT_CONTAINER_PADDING_S = 10;
	private final static int SPEED_LIMIT_CONTAINER_PADDING_M = 12;
	private final static int SPEED_LIMIT_CONTAINER_PADDING_L = 14;
	private final static int SPEED_LIMIT_VALUE_WEIGHT = 2;
	private final static int SPEED_LIMIT_DESCRIPTION_WEIGHT = 1;

	private final static int SPEEDOMETER_PADDING_SIDE_S = 9;
	private final static int SPEEDOMETER_PADDING_SIDE_ML = 12;
	private final static int SPEEDOMETER_PADDING_TOP = 3;
	private final static int SPEEDOMETER_PADDING_BOTTOM_S = 6;
	private final static int SPEEDOMETER_PADDING_BOTTOM_M = 9;
	private final static int SPEEDOMETER_PADDING_BOTTOM_L = 12;
	private final static int SPEEDOMETER_PADDING_SIDE_AA = 12;
	private final static int SPEEDOMETER_PADDING_TOP_BOTTOM_AA = 9;
	private final static int SPEEDOMETER_UNIT_DESCR_CANADA_SIZE_S = 8;

	private final static int SHADOW_SIZE = 4;
	private static final int SPEED_LIMIT_WIDGET_OVERLAP_MARGIN = 6;
	private static final int UNDEFINED_SPEED = -1;


	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final WaypointHelper waypointHelper;
	private final OsmAndLocationProvider provider;
	@Nullable
	private final WidgetsVisibilityHelper visibilityHelper;

	private final View view;
	private final View speedLimitContainer;
	private final View speedometerContainer;
	private final TextView speedLimitValueView;
	private final AppCompatTextView speedometerValueView;
	private final TextView speedometerUnitsView;
	private final TextView speedLimitDescription;

	private ApplicationMode mode;
	private WidgetSize previousWidgetSize;

	private float cachedSpeed = UNDEFINED_SPEED;
	private String cachedSpeedLimitText;
	private boolean lastNightMode;
	private Integer cachedMetricSystem;
	private DrivingRegion cachedRegion;

	@Nullable
	private Bitmap widgetBitmap;

	private final ValueAnimator speedAlertAnimator;
	private ValueAnimator speedLimitAnimator;
	private SpeedometerAnimationDrawable animationDrawable;
	private boolean isExceeding = false;
	private boolean isExceedWarning = false;
	private static final int SPEED_REVEAL_ANIM_DURATION = 1000;
	private static final int SPEED_LIMIT_REVEAL_ANIM_DURATION = 1000;
	private SpeedState currentState = SpeedState.SAFE;
	private int previousSpeedValueTextColor = 0;
	private int previousSpeedUnitTextColor = 0;
	private int speedValueTextColor = 0;
	private int speedUnitTextColor = 0;
	private float speedLimitSize;
	private final Paint revealPaint = new Paint();
	private float revealLastProgress;
	private boolean drawBitmap;
	private int revealColor = Color.RED;
	private int revealBackgroundColor = Color.TRANSPARENT;
	private float speedLimitAlpha = 0;
	private float lastSpeedLimitAlpha = 0;
	private float targetSpeedLimitAlpha = 0;
	private final Paint speedLimitPaint = new Paint();

	public SpeedometerWidget(@NonNull OsmandApplication app) {
		this(app, null, null);
		drawBitmap = true;
		speedLimitAnimator = ValueAnimator.ofFloat(0f, 1f);
		speedLimitAnimator.setDuration(SPEED_REVEAL_ANIM_DURATION);
		speedLimitAnimator.setRepeatCount(0);
		speedLimitAnimator.addUpdateListener(animation -> {
			speedLimitAlpha = (float) animation.getAnimatedValue();
			if (speedLimitAlpha < 0.0001) {
				cachedSpeedLimitText = null;
			}
		});
	}

	public SpeedometerWidget(@NonNull OsmandApplication app, @Nullable MapActivity mapActivity, @Nullable View view) {
		this.app = app;
		settings = app.getSettings();
		provider = app.getLocationProvider();
		routingHelper = app.getRoutingHelper();
		waypointHelper = app.getWaypointHelper();
		visibilityHelper = mapActivity != null ? mapActivity.getWidgetsVisibilityHelper() : null;

		this.view = view;
		boolean hasView = view != null;
		speedometerContainer = hasView ? view.findViewById(R.id.speedometer_container) : null;
		speedLimitContainer = hasView ? view.findViewById(R.id.speed_limit_container) : null;
		speedometerValueView = hasView ? view.findViewById(R.id.speedometer_value) : null;
		speedLimitValueView = hasView ? view.findViewById(R.id.speed_limit_value) : null;
		speedometerUnitsView = hasView ? view.findViewById(R.id.speedometer_units) : null;
		speedLimitDescription = hasView ? view.findViewById(R.id.limit_description) : null;

		lastNightMode = app.getDaynightHelper().isNightMode(settings.getApplicationMode(), ThemeUsageContext.MAP);
		previousSpeedValueTextColor = currentState.getSpeedTextColor(app, lastNightMode);
		previousSpeedUnitTextColor = currentState.getSpeedUnitTextColor(app, lastNightMode);
		speedValueTextColor = previousSpeedValueTextColor;
		speedUnitTextColor = previousSpeedUnitTextColor;

		animationDrawable = new SpeedometerAnimationDrawable(dpToPx(8));
		if (speedometerContainer != null) {
			speedometerContainer.setBackground(animationDrawable);
		}

		setupWidget();
		speedAlertAnimator = new ValueAnimator();
		speedAlertAnimator.setDuration(SPEED_REVEAL_ANIM_DURATION);
		speedAlertAnimator.setRepeatCount(0);
		speedAlertAnimator.addUpdateListener(animation -> {
			float progress = (float) animation.getAnimatedValue();
			animationDrawable.setProgress(progress);
			updateTextColor(animation.getAnimatedFraction());
		});
	}

	private void setupWidget() {
		mode = settings.getApplicationMode();
		if (view == null) {
			return;
		}

		boolean isUsaOrCanada = isUsaOrCanadaRegion();
		AndroidUiHelper.updateVisibility(speedLimitDescription, isUsaOrCanada);
		WidgetSize newWidgetSize = settings.SPEEDOMETER_SIZE.getModeValue(mode);
		DrivingRegion newDrivingRegion = settings.DRIVING_REGION.getModeValue(mode);
		if (previousWidgetSize == newWidgetSize && cachedRegion == newDrivingRegion) {
			return;
		}
		previousWidgetSize = newWidgetSize;
		cachedRegion = newDrivingRegion;

		LinearLayout.LayoutParams speedLimitLayoutParams = (LinearLayout.LayoutParams) speedLimitContainer.getLayoutParams();
		speedLimitLayoutParams.gravity = Gravity.CENTER;
		LinearLayout.LayoutParams speedometerLayoutParams = (LinearLayout.LayoutParams) speedometerContainer.getLayoutParams();

		int speedLimitPaddingTop;
		int speedLimitPaddingBottom;
		int speedLimitPaddingHorizontal;
		speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
		switch (previousWidgetSize) {
			case MEDIUM:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_M);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_M);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_BOTTOM_M));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_M);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_M);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_M);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_M);
				speedLimitPaddingTop = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_M : SPEED_LIMIT_CONTAINER_PADDING_M);
				speedLimitPaddingBottom = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_M : SPEED_LIMIT_CONTAINER_PADDING_M);
				speedLimitPaddingHorizontal = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_M : SPEED_LIMIT_CONTAINER_PADDING_M);
				speedLimitContainer.setPadding(speedLimitPaddingHorizontal, speedLimitPaddingTop, speedLimitPaddingHorizontal, speedLimitPaddingBottom);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, isCanadaRegion() ? SPEED_LIMIT_DESCRIPTION_SIZE_CANADA_M : SPEED_LIMIT_DESCRIPTION_SIZE_USUAL);
				break;
			case LARGE:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_L);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_L);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_BOTTOM_L));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_L);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitPaddingTop = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_L : SPEED_LIMIT_CONTAINER_PADDING_L);
				speedLimitPaddingBottom = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_L : SPEED_LIMIT_CONTAINER_PADDING_L);
				speedLimitPaddingHorizontal = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_L : SPEED_LIMIT_CONTAINER_PADDING_L);
				speedLimitContainer.setPadding(speedLimitPaddingHorizontal, speedLimitPaddingTop, speedLimitPaddingHorizontal, speedLimitPaddingBottom);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_L);
				speedLimitDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_DESCRIPTION_SIZE_USUAL);
				break;
			case SMALL:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_S);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_S);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_S), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_S), dpToPx(SPEEDOMETER_PADDING_BOTTOM_S));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_S);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_S);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_S);
				speedLimitPaddingTop = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_S : SPEED_LIMIT_CONTAINER_PADDING_S);
				speedLimitPaddingBottom = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_BOTTOM_S : SPEED_LIMIT_CONTAINER_PADDING_S);
				speedLimitPaddingHorizontal = dpToPx(isUsaOrCanada ? SPEED_LIMIT_CONTAINER_US_PADDING_S : SPEED_LIMIT_CONTAINER_PADDING_S);
				speedLimitContainer.setPadding(speedLimitPaddingHorizontal, speedLimitPaddingTop, speedLimitPaddingHorizontal, speedLimitPaddingBottom);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_S);
				speedLimitDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, isCanadaRegion() ? SPEED_LIMIT_DESCRIPTION_SIZE_CANADA_S : SPEED_LIMIT_DESCRIPTION_SIZE_USUAL);
				break;
		}
		speedLimitContainer.setAlpha(speedLimitContainer.getVisibility() == View.VISIBLE ? 1f : 0f);
		AndroidUiHelper.updateVisibility(speedLimitContainer, false);
	}

	private int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	public void updatePreviewInfo(boolean nightMode) {
		if (view != null) {
			setupWidget();
			updateColor(nightMode);
			FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(PREVIEW_VALUE, app);
			setSpeedText(String.valueOf(PREVIEW_VALUE), formattedSpeed.unit);
			setSpeedLimitText(String.valueOf(PREVIEW_VALUE));
			setSpeedLimitDescription();
			if (speedLimitContainer != null) {
				speedLimitContainer.setVisibility(settings.SHOW_SPEED_LIMIT_WARNING.getModeValue(mode) == ALWAYS ? View.VISIBLE : View.INVISIBLE);
			}
		}
	}

	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean nightMode = drawSettings != null ? drawSettings.isNightMode() : app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		updateInfo(drawSettings, false, nightMode);
	}

	public void updateInfo(@Nullable DrawSettings drawSettings, boolean drawBitmap, boolean nightMode) {
		setupWidget();
		if (view != null) {
			updateColor(nightMode);
		}
		boolean show = shouldShowWidget();
		if (show) {
			boolean speedExceed = false;
			boolean speedExceedWarning;
			boolean isChanged = false;
			if (lastNightMode != nightMode) {
				lastNightMode = nightMode;
				isChanged = true;
			}
			if (isMetricUpdateNeeded()) {
				isChanged = true;
			}
			Location location = provider.getLastKnownLocation();
			if (location != null && location.hasSpeed()) {
				float updateThreshold = cachedSpeed < LOW_SPEED_THRESHOLD_MPS
						? LOW_SPEED_UPDATE_THRESHOLD_MPS
						: UPDATE_THRESHOLD_MPS;
				if (Math.abs(location.getSpeed() - cachedSpeed) > updateThreshold || cachedSpeed == UNDEFINED_SPEED) {
					cachedSpeed = location.getSpeed();
					isChanged = true;
				}
				FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
				if (isChanged) {
					setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				}
				AlarmInfo actualExceededAlarm = getSpeedLimitInfo(false);
				AlarmInfo alarm = getSpeedLimitInfo();
				String speedLimitText = null;
				int cachedSpeedLimit = 0;
				int cachedWarningSpeedLimit = 0;
				if (alarm != null) {
					cachedSpeedLimit = alarm.getIntValue();
					speedLimitText = String.valueOf(cachedSpeedLimit);
					if (!Algorithms.stringsEqual(speedLimitText, cachedSpeedLimitText)) {
						cachedSpeedLimitText = speedLimitText;
						isChanged = true;
					}
				}
				if (actualExceededAlarm != null) {
					cachedWarningSpeedLimit = actualExceededAlarm.getIntValue();
				}
				if (alarm != null) {
					setSpeedLimitText(speedLimitText);
				}
				AndroidUiHelper.updateVisibility(view, true);
				updateSpeedLimitVisibility(alarm != null);

				float delta = app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f;
				speedExceed = formattedSpeed.valueSrc > 0 && cachedSpeedLimit > 0 &&
						formattedSpeed.valueSrc > cachedSpeedLimit + delta;

				speedExceedWarning = formattedSpeed.valueSrc > 0 && cachedWarningSpeedLimit > 0 &&
						formattedSpeed.valueSrc > cachedWarningSpeedLimit && !speedExceed;

				if (speedExceed != isExceeding || speedExceedWarning != isExceedWarning) {
					isExceeding = speedExceed;
					isExceedWarning = speedExceedWarning;
					updateBackgroundColors(nightMode);
				}
			} else if (cachedSpeed != 0) {
				cachedSpeed = 0;
				FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
				setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				AndroidUiHelper.updateVisibility(view, true);
			} else {
				AndroidUiHelper.updateVisibility(view, false);
			}
			setSpeedLimitDescription();
			updateTextColor(speedAlertAnimator.getAnimatedFraction());
			if (drawBitmap) {
				if (isChanged || revealLastProgress != animationDrawable.getProgress() || speedLimitAlpha != lastSpeedLimitAlpha) {
					revealLastProgress = animationDrawable.getProgress();
					lastSpeedLimitAlpha = speedLimitAlpha;
					float density = (drawSettings == null || drawSettings.getDensity() == 0) ? 1 : drawSettings.getDensity() * 0.77f;
					Paint paint = new Paint();
					paint.setMaskFilter(new BlurMaskFilter(40, BlurMaskFilter.Blur.NORMAL));
					int shadowColor = Color.BLACK;
					WidgetSize newWidgetSize = settings.SPEEDOMETER_SIZE.getModeValue(mode);
					float speedometerWidth = (newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_WIDTH_AA_L : newWidgetSize == WidgetSize.SMALL ? SPEEDOMETER_WIDTH_AA_S : SPEEDOMETER_WIDTH_M_AA) * density;
					float speedometerHeight = (newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_HEIGHT_AA_L : newWidgetSize == WidgetSize.SMALL ? SPEEDOMETER_HEIGHT_AA_S : SPEEDOMETER_HEIGHT_AA_M) * density;
					float speedLimitWidth = 0;
					float speedLimitHeight = 0;
					Bitmap speedLimitBitmap = null;
					speedLimitSize = (newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEED_LIMIT_SIZE_S : SPEED_LIMIT_SIZE_M) * density;
					speedLimitWidth = speedLimitSize;
					speedLimitHeight = speedLimitSize;
					if (cachedSpeedLimitText != null) {
						Drawable speedLimitDrawable = getSpeedLimitDrawable(nightMode, density);
						if (speedLimitDrawable != null) {
							speedLimitBitmap = getDrawableBitmap(speedLimitDrawable, (int) speedLimitWidth, (int) speedLimitHeight);
						}
					}
					widgetBitmap = Bitmap.createBitmap((int) (speedometerWidth + speedLimitWidth) + (int) (SHADOW_SIZE * 2 * density),
							(int) (Math.max(speedometerHeight, speedLimitHeight) + SHADOW_SIZE * 2 * density), Bitmap.Config.ARGB_8888);

					Canvas widgetCanvas = new Canvas(widgetBitmap);
					float speedometerLeft = drawSpeedometerPart(nightMode, density,
							newWidgetSize, speedometerWidth, speedometerHeight, widgetCanvas, speedExceed);

					if (speedLimitBitmap != null) {
						drawSpeedLimitPart(density, paint, shadowColor, newWidgetSize, speedLimitWidth,
								speedLimitHeight, speedLimitBitmap, widgetCanvas, speedometerLeft, nightMode);
					}
				}
			} else {
				widgetBitmap = null;
			}
		} else {
			widgetBitmap = null;
			AndroidUiHelper.updateVisibility(view, false);
		}
	}

	private void updateSpeedLimitVisibility(boolean show) {
		float targetAlpha = show ? 1f : 0f;
		if (targetSpeedLimitAlpha != targetAlpha) {
			targetSpeedLimitAlpha = targetAlpha;
			if (drawBitmap) {
				if (speedLimitAnimator.isRunning()) {
					speedLimitAnimator.cancel();
				}
				startFloatValueAnimator(speedLimitAnimator, !show);
			} else {
				if (speedLimitContainer.getAlpha() == targetAlpha &&
						speedLimitContainer.getVisibility() == (show ? View.VISIBLE : View.GONE)) {
					return;
				}
				if (show || speedLimitContainer.getAlpha() > 0) {
					speedLimitContainer.setVisibility(View.VISIBLE);
				}
				speedLimitContainer.animate()
						.alpha(targetAlpha)
						.setDuration(SPEED_LIMIT_REVEAL_ANIM_DURATION)
						.withEndAction(() -> {
							if (!show) {
								speedLimitContainer.setVisibility(View.GONE);
							}
						})
						.start();
			}
		}
	}


	private void updateBackgroundColors(boolean nightMode) {
		SpeedState targetState = SpeedState.SAFE;
		if (isExceeding) {
			targetState = SpeedState.EXCEED;
		} else if (isExceedWarning) {
			targetState = SpeedState.WARNING;
		}
		if (!drawBitmap) {
			float speedLimitCenterX = speedLimitContainer.getLeft() + (speedLimitContainer.getWidth() / 2f);
			float relativeCenterX = speedLimitCenterX - speedometerContainer.getLeft();
			if (animationDrawable != null) {
				animationDrawable.setAnimationCenter(relativeCenterX, speedometerContainer.getHeight() / 2f);
			}
		}
		int themeBg = ColorUtilities.getWidgetBackgroundColor(app, nightMode);
		if (targetState != currentState) {
			if (!speedAlertAnimator.isRunning()) {
				if (drawBitmap) {
					updateBackgroundColorsForBitmapMode(targetState);
				} else {
					updateBackgroundColorsForViewMode(themeBg, targetState);
				}
				currentState = targetState;
			}
		} else if (!speedAlertAnimator.isRunning()) {
			int activeAlert = (currentState == SpeedState.SAFE) ? themeBg : currentState.getAlertColor();
			animationDrawable.setColors(themeBg, activeAlert);
			animationDrawable.setProgress(currentState == SpeedState.SAFE ? 0f : 1f);
		}
	}

	private void updateBackgroundColorsForViewMode(int themeBg, SpeedState targetState) {
		if (drawBitmap) {
			return;
		}
		int fromColor = (currentState == SpeedState.SAFE) ? themeBg : currentState.getAlertColor();
		int toColor = (targetState == SpeedState.SAFE) ? themeBg : targetState.getAlertColor();
		if (targetState == SpeedState.SAFE) {
			previousSpeedValueTextColor = speedometerValueView.getCurrentTextColor();
			previousSpeedUnitTextColor = speedometerUnitsView.getCurrentTextColor();
			animationDrawable.setColors(themeBg, currentState.getAlertColor());
			startFloatValueAnimator(speedAlertAnimator, true);
		} else {
			previousSpeedValueTextColor = speedometerValueView.getCurrentTextColor();
			previousSpeedUnitTextColor = speedometerUnitsView.getCurrentTextColor();
			animationDrawable.setColors(fromColor, toColor);
			animationDrawable.setProgress(0f);
			startFloatValueAnimator(speedAlertAnimator, false);
		}
	}

	private void updateBackgroundColorsForBitmapMode(SpeedState targetState) {
		if (!drawBitmap) {
			return;
		}
		previousSpeedValueTextColor = speedValueTextColor;
		previousSpeedUnitTextColor = speedUnitTextColor;
		if (currentState == SpeedState.WARNING && targetState == SpeedState.EXCEED ||
				currentState == SpeedState.EXCEED && targetState == SpeedState.WARNING) {
			revealBackgroundColor = SpeedState.WARNING.getAlertColor();
		} else {
			revealBackgroundColor = Color.TRANSPARENT;
		}
		switch (currentState) {
			case SAFE -> {
				if (targetState == SpeedState.WARNING) {
					revealColor = SpeedState.WARNING.getAlertColor();
				} else {
					revealColor = SpeedState.EXCEED.getAlertColor();
				}
			}
			case WARNING -> {
				if (targetState == SpeedState.SAFE) {
					revealColor = SpeedState.WARNING.getAlertColor();
				} else {
					revealColor = SpeedState.EXCEED.getAlertColor();
				}
			}
			case EXCEED -> revealColor = SpeedState.EXCEED.getAlertColor();
		}
		if (currentState == SpeedState.SAFE || currentState == SpeedState.WARNING && targetState == SpeedState.EXCEED) {
			startFloatValueAnimator(speedAlertAnimator, false);
		} else {
			startFloatValueAnimator(speedAlertAnimator, true);
		}
	}

	public boolean isMetricUpdateNeeded() {
		int metricSystem = app.getSettings().METRIC_SYSTEM.get().ordinal();
		if (cachedMetricSystem == null || cachedMetricSystem != metricSystem) {
			cachedMetricSystem = metricSystem;
			return true;
		}
		return false;
	}

	private boolean shouldShowWidget() {
		boolean showSpeedometerSetting = settings.SHOW_SPEEDOMETER.getModeValue(mode);
		if (visibilityHelper != null) {
			return showSpeedometerSetting && visibilityHelper.shouldShowSpeedometer();
		}
		return showSpeedometerSetting;
	}

	private float drawSpeedometerPart(boolean nightMode, float density,
	                                  WidgetSize newWidgetSize, float speedometerWidth,
	                                  float speedometerHeight, Canvas widgetCanvas, boolean speedExceed) {
		LayerDrawable speedometerDrawable = (LayerDrawable) app.getUIUtilities().getIcon(R.drawable.speedometer_aa_shape);
		setDrawableColor((GradientDrawable) speedometerDrawable.getDrawable(0), nightMode);
		GradientDrawable bg = ((GradientDrawable) speedometerDrawable.findDrawableByLayerId(R.id.background));
		GradientDrawable stroke = ((GradientDrawable) speedometerDrawable.findDrawableByLayerId(R.id.stroke));
		bg.setColor(ColorUtilities.getWidgetBackgroundColor(app, nightMode));
		speedometerDrawable.setLayerInset(speedometerDrawable.findIndexByLayerId(R.id.background),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density));
		bg.setCornerRadius(8 * density);
		stroke.setCornerRadius(10 * density);

		((GradientDrawable) speedometerDrawable.findDrawableByLayerId(R.id.stroke))
				.setColor(app.getColor(nightMode ? R.color.map_window_stroke_dark : R.color.map_alert_stroke_light));
		Bitmap speedometerBg = getDrawableBitmap(speedometerDrawable, (int) speedometerWidth, (int) speedometerHeight);
		float speedometerLeft = widgetBitmap.getWidth() - speedometerWidth;
		float speedometerTop = (float) widgetBitmap.getHeight() / 2 - speedometerHeight / 2;
		widgetCanvas.drawBitmap(speedometerBg, speedometerLeft, speedometerTop, null);
		float revealCx = (speedometerLeft - speedLimitSize + SPEED_LIMIT_WIDGET_OVERLAP_MARGIN * density) + speedLimitSize / 2;
		float revealCy = speedometerTop + (float) widgetBitmap.getHeight() / 2;
		float revealMaxRadius = speedometerLeft - revealCx + (float) Math.hypot(widgetBitmap.getHeight(), widgetBitmap.getWidth());
		revealPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		if (revealBackgroundColor != Color.TRANSPARENT) {
			revealPaint.setColor(revealBackgroundColor);
			widgetCanvas.drawRect(speedometerLeft, speedometerTop, widgetBitmap.getWidth(), widgetBitmap.getHeight(), revealPaint);
		}
		revealPaint.setColor(revealColor);
		widgetCanvas.drawCircle(revealCx, revealCy, revealMaxRadius * revealLastProgress, revealPaint);

		Rect speedArea = new Rect((int) speedometerLeft, (int) speedometerTop, (int) (speedometerLeft + speedometerBg.getWidth()), (int) (speedometerTop + speedometerBg.getHeight()));
		int textSize = newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_TEXT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEEDOMETER_TEXT_SIZE_S : SPEEDOMETER_TEXT_SIZE_M;
		drawCurrentSpeed(widgetCanvas, textSize, speedArea, density, speedExceed);

		return speedometerLeft;
	}

	private void drawSpeedLimitPart(float density, Paint paint, int shadowColor, WidgetSize newWidgetSize,
	                                float speedLimitWidth, float speedLimitHeight, Bitmap speedLimitBitmap,
	                                Canvas widgetCanvas, float speedometerLeft, boolean nightMode) {
		if (widgetBitmap == null) {
			return;
		}
		speedLimitPaint.setAlpha((int) (255 * speedLimitAlpha));
		int speedLimitLeft = (int) (speedometerLeft - speedLimitWidth + SPEED_LIMIT_WIDGET_OVERLAP_MARGIN * density);
		int speedLimitTop = (int) ((float) widgetBitmap.getHeight() / 2 - speedLimitHeight / 2);
		Rect alertRect = new Rect(speedLimitLeft, speedLimitTop,
				speedLimitLeft + speedLimitBitmap.getWidth(), speedLimitTop + speedLimitBitmap.getHeight());
		if (!isUsaOrCanadaRegion()) {
			ShapeDrawable sd = new ShapeDrawable();
			OvalShape os = new OvalShape();
			sd.getPaint().setColor(shadowColor);
			sd.getPaint().setAlpha((int) (255 * speedLimitAlpha));
			sd.getPaint().setStyle(Paint.Style.FILL);
			sd.getPaint().setAntiAlias(true);
			sd.getPaint().setShadowLayer(SHADOW_SIZE * density, 0, 0, Color.BLACK);
			sd.setShape(os);
			Bitmap sdb = getDrawableBitmap(sd, speedLimitBitmap.getWidth() - 2 + (int) (SHADOW_SIZE * density),
					speedLimitBitmap.getHeight() - 2 + (int) (SHADOW_SIZE * density), (int) (SHADOW_SIZE * density));

			widgetCanvas.drawBitmap(sdb, alertRect.left - (int) (SHADOW_SIZE * density / 2),
					alertRect.top - (int) (SHADOW_SIZE * density / 2), speedLimitPaint);
		}
		widgetCanvas.drawBitmap(speedLimitBitmap, alertRect.left, alertRect.top, speedLimitPaint);
		drawSpeedLimit(widgetCanvas, newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_TEXT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEED_LIMIT_TEXT_SIZE_S : SPEED_LIMIT_TEXT_SIZE_M,
				density, alertRect, nightMode, newWidgetSize);
	}

	private void drawSpeedLimit(Canvas canvas, int textSize, float density, Rect alertRect, boolean nightMode, WidgetSize newWidgetSize) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);

		int limitColor = getSpeedLimitColor(nightMode);
		textPaint.setColor(limitColor);
		textPaint.setAlpha((int) (255 * speedLimitAlpha));
		textPaint.setTypeface(FontCache.getMediumFont());

		if (isUsaOrCanadaRegion()) {
			drawSpeedLimitDescription(canvas, textPaint, alertRect, density, newWidgetSize);
		}

		textPaint.setTextSize(textSize * density);
		Rect textBounds = new Rect();
		textPaint.getTextBounds(cachedSpeedLimitText, 0, cachedSpeedLimitText.length(), textBounds);
		float x = alertRect.left + (float) alertRect.width() / 2 - textPaint.measureText(cachedSpeedLimitText) / 2;
		float y;
		if (isUsaOrCanadaRegion()) {
			Rect contentRect = getSpeedlimitContentRect(alertRect, newWidgetSize, density);
			y = calculateYWeightPose(contentRect, SPEED_LIMIT_VALUE_WEIGHT, List.of(SPEED_LIMIT_DESCRIPTION_WEIGHT, SPEED_LIMIT_VALUE_WEIGHT), List.of(SPEED_LIMIT_DESCRIPTION_WEIGHT), textBounds);
		} else {
			y = alertRect.top + (float) alertRect.height() / 2 + (float) textBounds.height() / 2;
		}
		canvas.drawText(cachedSpeedLimitText, x, y, textPaint);
	}

	private float calculateFrameTextHeight(float weight, float totalWeight, float rectHeight) {
		return ((weight * rectHeight / totalWeight));
	}

	private void drawSpeedLimitDescription(Canvas canvas, TextPaint textPaint, Rect alertRect, float density, WidgetSize newWidgetSize) {
		float unitTextSize;
		String textLimitDescription;
		if (isUsaRegion()) {
			textLimitDescription = app.getString(R.string.shared_string_limit).toUpperCase();
			unitTextSize = SPEEDOMETER_UNIT_TEXT_SIZE;
		} else {
			textLimitDescription = app.getString(R.string.speedometer_maximum).toUpperCase();
			if (newWidgetSize == WidgetSize.SMALL) {
				unitTextSize = SPEEDOMETER_UNIT_DESCR_CANADA_SIZE_S;
			} else {
				unitTextSize = SPEEDOMETER_UNIT_TEXT_SIZE;
			}
		}
		Rect contentRect = getSpeedlimitContentRect(alertRect, newWidgetSize, density);
		textPaint.setTextSize(unitTextSize * density);
		Rect textBounds = new Rect();
		textPaint.getTextBounds(textLimitDescription, 0, textLimitDescription.length(), textBounds);
		float xDescr = contentRect.left + (float) contentRect.width() / 2 - textPaint.measureText(textLimitDescription) / 2;
		float yDescr = calculateYWeightPose(contentRect, SPEED_LIMIT_DESCRIPTION_WEIGHT, List.of(SPEED_LIMIT_VALUE_WEIGHT, SPEED_LIMIT_DESCRIPTION_WEIGHT), null, textBounds);

		canvas.drawText(textLimitDescription, xDescr, yDescr, textPaint);
	}

	private float calculateYWeightPose(@NonNull Rect contentRect, int frameWeight, @NonNull List<Integer> allWeights,
	                                   @Nullable List<Integer> weightsAbove, @NonNull Rect textBounds) {
		float halfTextHeight = textBounds.height() / 2.0f;
		float totalWeight = allWeights.stream().reduce(0, Integer::sum);

		float totalHeightAbove = 0;
		if (weightsAbove != null) {
			for (Integer weight : weightsAbove) {
				totalHeightAbove += calculateFrameTextHeight(weight, totalWeight, contentRect.height());
			}
		}

		float calculatedFrameTextHeight = calculateFrameTextHeight(frameWeight, totalWeight, contentRect.height());
		float frameCenterPosition = calculatedFrameTextHeight / 2 + totalHeightAbove + halfTextHeight;
		return frameCenterPosition + contentRect.top;
	}

	private Rect getSpeedlimitContentRect(@NonNull Rect speedlimitRect, @NonNull WidgetSize newWidgetSize, float density) {
		if (isUsaOrCanadaRegion()) {
			int padding;
			if (newWidgetSize == WidgetSize.SMALL) {
				padding = SPEED_LIMIT_CONTAINER_US_PADDING_S;
			} else if (newWidgetSize == WidgetSize.MEDIUM) {
				padding = SPEED_LIMIT_CONTAINER_US_PADDING_M;
			} else {
				padding = SPEED_LIMIT_CONTAINER_US_PADDING_L;
			}
			float scaledPadding = padding * density;
			return new Rect((int) (speedlimitRect.left + scaledPadding),
					(int) (speedlimitRect.top + scaledPadding),
					(int) (speedlimitRect.right - scaledPadding),
					(int) (speedlimitRect.bottom - scaledPadding));
		} else {
			return speedlimitRect;
		}
	}

	private void drawCurrentSpeed(Canvas canvas, int textSize, Rect speedArea, float density, boolean speedExceed) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setTypeface(FontCache.getMediumFont());

		FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
		float unitTextSize = SPEEDOMETER_UNIT_TEXT_SIZE * density;
		textPaint.setTextSize(unitTextSize);
		textPaint.setTypeface(DEFAULT);
		Rect speedUnitRect = new Rect();
		String speedUnitText = formattedSpeed.unit.toUpperCase();
		textPaint.getTextBounds(speedUnitText, 0, speedUnitText.length(), speedUnitRect);
		float speedUnitWidth = speedUnitRect.width();

		float xUnit = canvas.getWidth() - speedUnitWidth - SPEEDOMETER_PADDING_SIDE_AA * density;
		float yUnit = speedArea.bottom - SPEEDOMETER_PADDING_TOP_BOTTOM_AA * density;
		textPaint.setColor(speedUnitTextColor);
		canvas.drawText(speedUnitText, xUnit, yUnit, textPaint);

		textPaint.setColor(speedValueTextColor);
		textPaint.setTextSize(textSize * density);

		Rect textBounds = new Rect();
		textPaint.getTextBounds(formattedSpeed.value, 0, formattedSpeed.value.length(), textBounds);
		float speedWidth = textPaint.measureText(formattedSpeed.value);
		float x = canvas.getWidth() - speedWidth - SPEEDOMETER_PADDING_SIDE_AA * density - SPEEDOMETER_AA_STROKE * density;
		while (x < speedArea.left + SPEEDOMETER_PADDING_SIDE_AA * density + SPEEDOMETER_AA_STROKE * density) {
			textSize--;
			textPaint.setTextSize(textSize * density);
			textPaint.getTextBounds(formattedSpeed.value, 0, formattedSpeed.value.length(), textBounds);
			speedWidth = textPaint.measureText(formattedSpeed.value);
			x = canvas.getWidth() - speedWidth - SPEEDOMETER_PADDING_SIDE_AA * density - SPEEDOMETER_AA_STROKE * density;
		}

		float speedValueAreaHeight = yUnit - speedUnitRect.height() - speedArea.top;
		float y = yUnit - speedUnitRect.height() - speedValueAreaHeight / 2 + (float) textBounds.height() / 2;
		canvas.drawText(formattedSpeed.value, x, y, textPaint);
	}

	private int getSpeedLimitColor(boolean nightMode) {
		if (isUsaOrCanadaRegion() || isEuropeRegion()) {
			return app.getColor(nightMode ? R.color.widgettext_night : R.color.widgettext_day);
		} else {
			return app.getColor(R.color.widgettext_day);
		}
	}


	@NonNull
	private Bitmap getDrawableBitmap(@NonNull Drawable drawable, int width, int height) {
		return getDrawableBitmap(drawable, width, height, 0);
	}

	@NonNull
	private Bitmap getDrawableBitmap(@NonNull Drawable drawable, int width, int height, int padding) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(padding, padding, width - padding, height - padding);
		drawable.draw(canvas);
		return bitmap;
	}

	@Nullable
	private AlarmInfo getSpeedLimitInfo() {
		boolean whenExceeded = settings.SHOW_SPEED_LIMIT_WARNING.getModeValue(mode) == WHEN_EXCEEDED;
		return getSpeedLimitInfo(whenExceeded);
	}

	@Nullable
	private AlarmInfo getSpeedLimitInfo(boolean whenExceeded) {
		SpeedConstants speedFormat = settings.SPEED_SYSTEM.get();
		AlarmInfo alarm = waypointHelper.getSpeedLimitAlarm(speedFormat, whenExceeded);
		if (alarm == null) {
			Location loc = provider.getLastKnownLocation();
			RouteSegmentResult current = routingHelper.getRoute().getCurrentSegmentResult();
			RouteDataObject dataObject = null;
			if (current != null) {
				dataObject = current.getObject();
			} else {
				dataObject = provider.getLastKnownRouteSegment();
			}
			if (dataObject != null && loc != null) {
				alarm = waypointHelper.calculateSpeedLimitAlarm(dataObject, loc, speedFormat, whenExceeded);
			}
		}
		return alarm;
	}

	private void updateColor(boolean nightMode) {
		updateBackgroundColors(nightMode);
		updateTextColor(speedAlertAnimator.getAnimatedFraction());
		if (!drawBitmap) {
			speedLimitContainer.setBackground(getSpeedLimitDrawable(nightMode, app.getResources().getDisplayMetrics().density));
			int limitColor = getSpeedLimitColor(nightMode);
			speedLimitValueView.setTextColor(limitColor);
			speedLimitDescription.setTextColor(limitColor);
		}
	}

	private void setDrawableColor(@NonNull GradientDrawable drawable, boolean nightMode) {
		drawable.setColor(ColorUtilities.getWidgetBackgroundColor(app, nightMode));
	}

	@Nullable
	private Drawable getSpeedLimitDrawable(boolean nightMode, float density) {
		Drawable drawable;
		if (isUsaOrCanadaRegion()) {
			drawable = AppCompatResources.getDrawable(app, nightMode ? R.drawable.ic_warning_speedlimit_us_dark : R.drawable.ic_warning_speedlimit_us_light);
		} else if (isEuropeRegion()) {
			drawable = AppCompatResources.getDrawable(app, nightMode ? R.drawable.ic_warning_speedlimit_eu_dark : R.drawable.ic_warning_speedlimit_eu_light);
		} else {
			drawable = AppCompatResources.getDrawable(app, R.drawable.speed_limit_shape);
			if (drawable != null) {
				LayerDrawable layerDrawable = (LayerDrawable) drawable;
				GradientDrawable background = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.background);

				background.setColor(ColorUtilities.getWidgetSecondaryBackgroundColor(app, nightMode));
				if (!isUsaOrCanadaRegion()) {
					background.setStroke((int) (5 * density), ContextCompat.getColor(app, nightMode ? R.color.map_window_stroke_dark : R.color.widget_background_color_light));
				}
			}
		}
		return drawable;
	}

	private void setSpeedText(String value, String units) {
		if (speedometerValueView != null) {
			speedometerValueView.setText(value);
		}
		if (speedometerUnitsView != null) {
			speedometerUnitsView.setText(units);
		}
	}

	private void setSpeedLimitText(String value) {
		if (speedLimitValueView != null) {
			speedLimitValueView.setText(value);
		}
	}

	private void setSpeedLimitDescription() {
		if (isUsaOrCanadaRegion() && speedLimitDescription != null) {
			String textLimitDescription = app.getString(isCanadaRegion() ? R.string.speedometer_maximum : R.string.shared_string_limit).toUpperCase();
			speedLimitDescription.setText(textLimitDescription);
		}
	}

	private boolean isUsaOrCanadaRegion() {
		return isUsaRegion() || isCanadaRegion();
	}

	private boolean isUsaRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.US;
	}

	private boolean isCanadaRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.CANADA;
	}

	private boolean isEuropeRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.EUROPE_ASIA;
	}

	public void setVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(view, visible);
	}

	@Nullable
	public Bitmap getWidgetBitmap() {
		return widgetBitmap;
	}

	private void updateTextColor(float progress) {
		int currentTextColor = ColorUtils.blendARGB(previousSpeedValueTextColor, currentState.getSpeedTextColor(app, lastNightMode), progress);
		int currentSpeedUnitColor = ColorUtils.blendARGB(previousSpeedUnitTextColor, currentState.getSpeedUnitTextColor(app, lastNightMode), progress);
		if (drawBitmap) {
			speedValueTextColor = 0xff000000 | currentTextColor;
			speedUnitTextColor = 0xff000000 | currentSpeedUnitColor;
		} else {
			speedometerValueView.setTextColor(0xff000000 | currentTextColor);
			speedometerUnitsView.setTextColor(0xff000000 | currentSpeedUnitColor);
		}
	}

	private void startFloatValueAnimator(ValueAnimator animator, boolean isReversed) {
		if (isReversed) {
			animator.setFloatValues(1f, 0f);
		} else {
			animator.setFloatValues(0f, 1f);
		}
		animator.start();
	}
}
