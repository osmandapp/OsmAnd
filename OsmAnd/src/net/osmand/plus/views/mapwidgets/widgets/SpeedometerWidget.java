package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.settings.enums.SpeedLimitWarningState.ALWAYS;
import static net.osmand.plus.settings.enums.SpeedLimitWarningState.WHEN_EXCEEDED;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.LOW_SPEED_UPDATE_THRESHOLD_MPS;
import static net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget.UPDATE_THRESHOLD_MPS;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

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
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

public class SpeedometerWidget {

	private final static int PREVIEW_VALUE = 85;

	private final static int SPEEDOMETER_AA_STROKE = 3;
	private final static int SPEEDOMETER_WIDTH_M = 72;
	private final static int SPEEDOMETER_WIDTH_M_AA = 88 + SPEEDOMETER_AA_STROKE;
	private final static int SPEEDOMETER_HEIGHT_M = 72;
	private final static int SPEEDOMETER_HEIGHT_AA_M = 72 + SPEEDOMETER_AA_STROKE;
	private final static int SPEEDOMETER_WIDTH_L = 126;
	private final static int SPEEDOMETER_WIDTH_AA_L = 126 + SPEEDOMETER_AA_STROKE;
	private final static int SPEEDOMETER_HEIGHT_L = 96;
	private final static int SPEEDOMETER_HEIGHT_AA_L = 96 + SPEEDOMETER_AA_STROKE;
	private final static int SPEEDOMETER_TEXT_SIZE_M = 36;
	private final static int SPEEDOMETER_UNIT_TEXT_SIZE = 11;
	private final static int SPEEDOMETER_TEXT_SIZE_L = 60;
	private final static int SPEED_LIMIT_TEXT_SIZE_M = 24;
	private final static int SPEED_LIMIT_TEXT_SIZE_L = 36;
	private final static int SPEED_LIMIT_SIZE_M = 72;
	private final static int SPEED_LIMIT_SIZE_L = 94;

	private final static int SPEEDOMETER_PADDING_SIDE = 9;
	private final static int SPEEDOMETER_PADDING_TOP_BOTTOM = 0;
	private final static int SPEEDOMETER_PADDING_SIDE_AA = 12;
	private final static int SPEEDOMETER_PADDING_TOP_BOTTOM_AA = 9;
	private final static int US_SPEED_LIMIT_BOTTOM = 18;
	private final static int CAN_SPEED_LIMIT_BOTTOM = 20;
	private final static int SHADOW_SIZE = 4;
	private static final int SPEED_LIMIT_WIDGET_OVERLAP_MARGIN = 14;


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
	private final TextView speedometerValueView;
	private final TextView speedometerUnitsView;
	private final TextView speedLimitDescription;

	private ApplicationMode mode;
	private WidgetSize previousWidgetSize;

	private float cachedSpeed;
	private String cachedSpeedLimit;
	private boolean lastNightMode;

	@Nullable
	private Bitmap widgetBitmap;

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

		setupWidget();
	}

	private void setupWidget() {
		mode = settings.getApplicationMode();
		if (view == null) {
			return;
		}

		FrameLayout.LayoutParams speedLimitValueParams = (FrameLayout.LayoutParams) speedLimitValueView.getLayoutParams();
		speedLimitValueParams.setMargins(0, isUsaOrCanadaRegion() ? dpToPx(2) : 0, 0, 0);
		AndroidUiHelper.updateVisibility(speedLimitDescription, false);
		WidgetSize newWidgetSize = settings.SPEEDOMETER_SIZE.getModeValue(mode);
		if (previousWidgetSize == newWidgetSize) {
			return;
		}
		previousWidgetSize = newWidgetSize;

		LinearLayout.LayoutParams speedLimitLayoutParams = (LinearLayout.LayoutParams) speedLimitContainer.getLayoutParams();
		LinearLayout.LayoutParams speedometerLayoutParams = (LinearLayout.LayoutParams) speedometerContainer.getLayoutParams();
		FrameLayout.LayoutParams speedLimitDescriptionParams = (FrameLayout.LayoutParams) speedLimitDescription.getLayoutParams();

		switch (previousWidgetSize) {
			case MEDIUM:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_M);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_M);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE), SPEEDOMETER_PADDING_TOP_BOTTOM, dpToPx(SPEEDOMETER_PADDING_SIDE), SPEEDOMETER_PADDING_TOP_BOTTOM);
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_M);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_M);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_M);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_M);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitDescriptionParams.setMargins(0, dpToPx(10), 0, 0);
				break;
			case LARGE:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_L);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_L);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE), dpToPx(SPEEDOMETER_PADDING_TOP_BOTTOM), dpToPx(SPEEDOMETER_PADDING_SIDE), dpToPx(SPEEDOMETER_PADDING_TOP_BOTTOM));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_L);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_L);
				speedLimitDescriptionParams.setMargins(0, dpToPx(14), 0, 0);
				break;
		}
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
			AndroidUiHelper.updateVisibility(speedLimitContainer, settings.SHOW_SPEED_LIMIT_WARNING.getModeValue(mode) == ALWAYS);
		}
	}

	public void updateInfo(@Nullable DrawSettings drawSettings) {
		updateInfo(drawSettings, false, app.getDaynightHelper().isNightMode(true));
	}

	public void updateInfo(@Nullable DrawSettings drawSettings, boolean drawBitmap, boolean nightMode) {
		setupWidget();
		if (view != null) {
			updateColor(drawSettings != null ? drawSettings.isNightMode() : nightMode);
		}
		boolean show = shouldShowWidget();
		if (routingHelper.isFollowingMode() && show) {
			boolean isChanged = false;
			if (lastNightMode != nightMode) {
				lastNightMode = nightMode;
				isChanged = true;
			}
			Location location = provider.getLastKnownLocation();
			if (location != null && location.hasSpeed()) {
				float updateThreshold = cachedSpeed < LOW_SPEED_THRESHOLD_MPS
						? LOW_SPEED_UPDATE_THRESHOLD_MPS
						: UPDATE_THRESHOLD_MPS;
				if (Math.abs(location.getSpeed() - cachedSpeed) > updateThreshold) {
					cachedSpeed = location.getSpeed();
					isChanged = true;
					OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
					setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				}
				AlarmInfo alarm = getSpeedLimitInfo();
				String speedLimitText = null;
				if (alarm != null) {
					speedLimitText = String.valueOf(alarm.getIntValue());
					if (!Algorithms.stringsEqual(speedLimitText, cachedSpeedLimit)) {
						cachedSpeedLimit = speedLimitText;
						isChanged = true;
					}
				} else {
					cachedSpeedLimit = null;
				}
				if (alarm != null) {
					setSpeedLimitText(speedLimitText);
				}
				AndroidUiHelper.updateVisibility(view, true);
				AndroidUiHelper.updateVisibility(speedLimitContainer, alarm != null);
			} else if (cachedSpeed != 0) {
				cachedSpeed = 0;
				setSpeedText(null, null);
				AndroidUiHelper.updateVisibility(view, true);
			} else {
				AndroidUiHelper.updateVisibility(view, false);
			}
			if (drawBitmap) {
				if (isChanged) {
					float density = (drawSettings == null || drawSettings.getDensity() == 0) ? 1 : drawSettings.getDensity();
					Paint paint = new Paint();
					paint.setMaskFilter(new BlurMaskFilter(40, BlurMaskFilter.Blur.NORMAL));
					int shadowColor = Color.BLACK;
					WidgetSize newWidgetSize = settings.SPEEDOMETER_SIZE.getModeValue(mode);
					float speedometerWidth = (newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_WIDTH_AA_L : SPEEDOMETER_WIDTH_M_AA) * density;
					float speedometerHeight = (newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_HEIGHT_AA_L : SPEEDOMETER_HEIGHT_AA_M) * density;
					float speedLimitWidth = 0;
					float speedLimitHeight = 0;
					Bitmap speedLimitBitmap = null;
					if (cachedSpeedLimit != null) {
						Drawable speedLimitDrawable = getSpeedLimitDrawable(nightMode, density);
						speedLimitWidth = (newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_SIZE_L : SPEED_LIMIT_SIZE_M) * density;
						speedLimitHeight = (newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_SIZE_L : SPEED_LIMIT_SIZE_M) * density;
						if (speedLimitDrawable != null) {
							speedLimitBitmap = getDrawableBitmap(speedLimitDrawable, (int) speedLimitWidth, (int) speedLimitHeight);
						}
					}
					widgetBitmap = Bitmap.createBitmap((int) (speedometerWidth + speedLimitWidth) + (int) (SHADOW_SIZE * 2 * density),
							(int) (Math.max(speedometerHeight, speedLimitHeight) + SHADOW_SIZE * 2 * density), Bitmap.Config.ARGB_8888);

					Canvas widgetCanvas = new Canvas(widgetBitmap);
					float speedometerLeft = drawSpeedometerPart(nightMode, density, paint, shadowColor,
							newWidgetSize, speedometerWidth, speedometerHeight, widgetCanvas);

					if (speedLimitBitmap != null) {
						drawSpeedLimitPart(density, paint, shadowColor, newWidgetSize, speedLimitWidth,
								speedLimitHeight, speedLimitBitmap, widgetCanvas, speedometerLeft);
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

	private boolean shouldShowWidget() {
		boolean showSpeedometerSetting = settings.SHOW_SPEEDOMETER.getModeValue(mode);
		if (visibilityHelper != null) {
			return showSpeedometerSetting && visibilityHelper.shouldShowSpeedometer();
		}
		return showSpeedometerSetting;
	}

	private float drawSpeedometerPart(boolean nightMode, float density, Paint paint, int shadowColor,
	                                  WidgetSize newWidgetSize, float speedometerWidth,
	                                  float speedometerHeight, Canvas widgetCanvas) {
		LayerDrawable speedometerDrawable = (LayerDrawable) app.getUIUtilities().getIcon(R.drawable.speedometer_aa_shape);
		setDrawableColor((GradientDrawable) speedometerDrawable.getDrawable(0), nightMode);
		GradientDrawable bg = ((GradientDrawable) speedometerDrawable.findDrawableByLayerId(R.id.background));
		bg.setColor(ColorUtilities.getWidgetBackgroundColor(app, nightMode));
		speedometerDrawable.setLayerInset(speedometerDrawable.findIndexByLayerId(R.id.background),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density),
				(int) (SPEEDOMETER_AA_STROKE * density));

		((GradientDrawable) speedometerDrawable.findDrawableByLayerId(R.id.stroke))
				.setColor(app.getColor(nightMode ? R.color.map_window_stroke_dark : R.color.map_alert_stroke_light));
		Bitmap speedometerBg = getDrawableBitmap(speedometerDrawable, (int) speedometerWidth, (int) speedometerHeight);
		float speedometerLeft = widgetBitmap.getWidth() - speedometerWidth - SHADOW_SIZE * density / 2;
		float speedometerTop = (float) widgetBitmap.getHeight() / 2 - speedometerHeight / 2;
		Bitmap speedometerShadowBitmap = getShadowBitmap(density, speedometerBg, shadowColor);
		widgetCanvas.drawBitmap(speedometerShadowBitmap,
				speedometerLeft - (float) (speedometerShadowBitmap.getWidth() - speedometerBg.getWidth()) / 2,
				speedometerTop - (float) (speedometerShadowBitmap.getHeight() - speedometerBg.getHeight()) / 2,
				paint);
		widgetCanvas.drawBitmap(speedometerBg, speedometerLeft, speedometerTop, null);
		drawCurrentSpeed(widgetCanvas, newWidgetSize == WidgetSize.LARGE, density);

		return speedometerLeft;
	}

	private void drawSpeedLimitPart(float density, Paint paint, int shadowColor, WidgetSize newWidgetSize,
	                                float speedLimitWidth, float speedLimitHeight, Bitmap speedLimitBitmap,
	                                Canvas widgetCanvas, float speedometerLeft) {
		if (widgetBitmap == null) {
			return;
		}
		int speedLimitLeft = (int) (speedometerLeft - speedLimitWidth + SPEED_LIMIT_WIDGET_OVERLAP_MARGIN * density);
		int speedLimitTop = (int) ((float) widgetBitmap.getHeight() / 2 - speedLimitHeight / 2);
		Rect alertRect = new Rect(speedLimitLeft, speedLimitTop,
				speedLimitLeft + speedLimitBitmap.getWidth(), speedLimitTop + speedLimitBitmap.getHeight());
		if (isUsaOrCanadaRegion()) {
			Bitmap speedLimitShadowBitmap = getShadowBitmap(density, speedLimitBitmap, shadowColor);
			widgetCanvas.drawBitmap(speedLimitShadowBitmap,
					alertRect.left - (float) (speedLimitShadowBitmap.getWidth() - speedLimitBitmap.getWidth()) / 2,
					alertRect.top - (float) (speedLimitShadowBitmap.getHeight() - speedLimitBitmap.getHeight()) / 2, paint);
		} else {
			ShapeDrawable sd = new ShapeDrawable();
			OvalShape os = new OvalShape();
			sd.getPaint().setColor(shadowColor);
			sd.getPaint().setStyle(Paint.Style.FILL);
			sd.getPaint().setAntiAlias(true);
			sd.getPaint().setShadowLayer(SHADOW_SIZE * density, 0, 0, Color.BLACK);
			sd.setShape(os);
			Bitmap sdb = getDrawableBitmap(sd, speedLimitBitmap.getWidth() - 2 + (int) (SHADOW_SIZE * density),
					speedLimitBitmap.getHeight() - 2 + (int) (SHADOW_SIZE * density), (int) (SHADOW_SIZE * density));

			widgetCanvas.drawBitmap(sdb, alertRect.left - (int) (SHADOW_SIZE * density / 2),
					alertRect.top - (int) (SHADOW_SIZE * density / 2), null);
		}
		widgetCanvas.drawBitmap(speedLimitBitmap, alertRect.left, alertRect.top, null);
		drawSpeedLimit(widgetCanvas, newWidgetSize == WidgetSize.LARGE, density, alertRect);
	}

	@NonNull
	private Bitmap getShadowBitmap(float density, Bitmap srcBitmap, int shadowColor) {
		BitmapDrawable shadowDrawable = new BitmapDrawable(app.getResources(), srcBitmap);
		shadowDrawable.setTint(shadowColor);
		return getDrawableBitmap(shadowDrawable, srcBitmap.getWidth() + (int) (SHADOW_SIZE * 2 * density),
				srcBitmap.getHeight() + (int) (SHADOW_SIZE * 2 * density));
	}

	private void drawSpeedLimit(Canvas canvas, boolean isLarge, float density, Rect alertRect) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(app.getColor(R.color.widgettext_day));
		textPaint.setTextSize((isLarge ? SPEED_LIMIT_TEXT_SIZE_L : SPEED_LIMIT_TEXT_SIZE_M) * density);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);

		Rect textBounds = new Rect();
		textPaint.getTextBounds(cachedSpeedLimit, 0, cachedSpeedLimit.length(), textBounds);
		float x = alertRect.left + (float) alertRect.width() / 2 - textPaint.measureText(cachedSpeedLimit) / 2;
		float y;
		if (isUsaRegion()) {
			y = alertRect.bottom - US_SPEED_LIMIT_BOTTOM * density;
		} else if (isCanadaRegion()) {
			y = alertRect.bottom - CAN_SPEED_LIMIT_BOTTOM * density;
		} else {
			y = alertRect.top + (float) alertRect.height() / 2 + (float) textBounds.height() / 2;
		}
		canvas.drawText(cachedSpeedLimit, x, y, textPaint);
	}

	private void drawCurrentSpeed(Canvas canvas, boolean isLarge, float density) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(lastNightMode ? app.getColor(R.color.widgettext_night) : app.getColor(R.color.widgettext_day));
		textPaint.setTextSize((isLarge ? SPEEDOMETER_TEXT_SIZE_L : SPEEDOMETER_TEXT_SIZE_M) * density);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);

		OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
		Rect textBounds = new Rect();
		textPaint.getTextBounds(formattedSpeed.value, 0, formattedSpeed.value.length(), textBounds);
		float speedWidth = textPaint.measureText(formattedSpeed.value);
		float x = canvas.getWidth() - speedWidth - SPEEDOMETER_PADDING_SIDE_AA * density;
		float y = (isLarge ? SPEEDOMETER_TEXT_SIZE_L : SPEEDOMETER_TEXT_SIZE_M) * density + SPEEDOMETER_PADDING_TOP_BOTTOM_AA * density;
		canvas.drawText(formattedSpeed.value, x, y, textPaint);

		float unitTextSize = SPEEDOMETER_UNIT_TEXT_SIZE * density;
		textPaint.setTextSize(unitTextSize);
		textPaint.setTypeface(Typeface.DEFAULT);
		Rect speedUnitRect = new Rect();
		String speedUnitText = formattedSpeed.unit.toUpperCase();
		textPaint.getTextBounds(speedUnitText, 0, speedUnitText.length(), speedUnitRect);
		float speedUnitWidth = speedUnitRect.width();

		x = canvas.getWidth() - speedUnitWidth - SPEEDOMETER_PADDING_SIDE_AA * density;
		y = canvas.getHeight() - unitTextSize;
		textPaint.setColor(app.getColor(R.color.text_color_secondary_dark));
		canvas.drawText(speedUnitText, x, y, textPaint);
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
		SpeedConstants speedFormat = settings.SPEED_SYSTEM.get();
		boolean whenExceeded = settings.SHOW_SPEED_LIMIT_WARNING.getModeValue(mode) == WHEN_EXCEEDED;

		AlarmInfo alarm = waypointHelper.getSpeedLimitAlarm(speedFormat, whenExceeded);
		if (alarm == null) {
			Location loc = provider.getLastKnownLocation();
			RouteDataObject dataObject = provider.getLastKnownRouteSegment();
			if (dataObject != null && loc != null) {
				alarm = waypointHelper.calculateSpeedLimitAlarm(dataObject, loc, speedFormat, whenExceeded);
			}
		}
		return alarm;
	}

	private void updateColor(boolean nightMode) {
		Drawable drawable = speedometerContainer.getBackground();
		setDrawableColor((GradientDrawable) drawable, nightMode);
		speedLimitContainer.setBackground(getSpeedLimitDrawable(nightMode, app.getResources().getDisplayMetrics().density));
		speedometerValueView.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
	}

	private void setDrawableColor(@NonNull GradientDrawable drawable, boolean nightMode) {
		drawable.setColor(ColorUtilities.getWidgetBackgroundColor(app, nightMode));
	}

	@Nullable
	private Drawable getSpeedLimitDrawable(boolean nightMode, float density) {
		Drawable drawable;
		if (isUsaRegion()) {
			drawable = AppCompatResources.getDrawable(app, R.drawable.warnings_speed_limit_ca);
		} else if (isCanadaRegion()) {
			drawable = AppCompatResources.getDrawable(app, R.drawable.warnings_speed_limit_us);
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

	private boolean isUsaOrCanadaRegion() {
		return isUsaRegion() || isCanadaRegion();
	}

	private boolean isUsaRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.US;
	}

	private boolean isCanadaRegion() {
		return settings.DRIVING_REGION.getModeValue(mode) == DrivingRegion.CANADA;
	}

	public void setVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(view, visible);
	}

	@Nullable
	public Bitmap getWidgetBitmap() {
		return widgetBitmap;
	}
}
