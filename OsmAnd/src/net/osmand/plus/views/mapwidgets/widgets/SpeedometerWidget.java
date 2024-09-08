package net.osmand.plus.views.mapwidgets.widgets;

import static android.graphics.Typeface.DEFAULT;
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
import androidx.appcompat.widget.AppCompatTextView;
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
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

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
	private final static int SPEED_LIMIT_SIZE_S = 72;
	private final static int SPEED_LIMIT_SIZE_M = 72;
	private final static int SPEED_LIMIT_SIZE_L = 94;

	private final static int SPEEDOMETER_PADDING_SIDE_S = 9;
	private final static int SPEEDOMETER_PADDING_SIDE_ML = 12;
	private final static int SPEEDOMETER_PADDING_TOP = 3;
	private final static int SPEEDOMETER_PADDING_BOTTOM_S = 6;
	private final static int SPEEDOMETER_PADDING_BOTTOM_M = 9;
	private final static int SPEEDOMETER_PADDING_BOTTOM_L = 12;
	private final static int SPEEDOMETER_PADDING_SIDE_AA = 12;
	private final static int SPEEDOMETER_PADDING_TOP_BOTTOM_AA = 9;
	private final static int US_SPEED_LIMIT_BOTTOM = 18;
	private final static int CAN_SPEED_LIMIT_BOTTOM = 20;
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
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_BOTTOM_M));
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
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_ML), dpToPx(SPEEDOMETER_PADDING_BOTTOM_L));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_L);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_L);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_L);
				speedLimitDescriptionParams.setMargins(0, dpToPx(14), 0, 0);
				break;
			case SMALL:
				speedometerLayoutParams.height = dpToPx(SPEEDOMETER_HEIGHT_S);
				speedometerLayoutParams.width = dpToPx(SPEEDOMETER_WIDTH_S);
				speedometerContainer.setLayoutParams(speedometerLayoutParams);
				speedometerContainer.setPadding(dpToPx(SPEEDOMETER_PADDING_SIDE_S), dpToPx(SPEEDOMETER_PADDING_TOP), dpToPx(SPEEDOMETER_PADDING_SIDE_S), dpToPx(SPEEDOMETER_PADDING_BOTTOM_S));
				speedometerValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEEDOMETER_TEXT_SIZE_S);

				speedLimitLayoutParams.height = dpToPx(SPEED_LIMIT_SIZE_S);
				speedLimitLayoutParams.width = dpToPx(SPEED_LIMIT_SIZE_S);
				speedLimitContainer.setLayoutParams(speedLimitLayoutParams);
				speedLimitValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SPEED_LIMIT_TEXT_SIZE_S);
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
		updateInfo(drawSettings, false, app.getDaynightHelper().isNightMode());
	}

	public void updateInfo(@Nullable DrawSettings drawSettings, boolean drawBitmap, boolean nightMode) {
		setupWidget();
		if (view != null) {
			updateColor(drawSettings != null ? drawSettings.isNightMode() : nightMode);
		}
		boolean show = shouldShowWidget();
		if (show) {
			boolean speedExceed = false;
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
				if (Math.abs(location.getSpeed() - cachedSpeed) > updateThreshold || cachedSpeed == UNDEFINED_SPEED) {
					cachedSpeed = location.getSpeed();
					isChanged = true;
				}
				OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
				if (isChanged) {
					setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				}
				AlarmInfo alarm = getSpeedLimitInfo();
				String speedLimitText = null;
				int cachedSpeedLimit = 0;
				if (alarm != null) {
					cachedSpeedLimit = alarm.getIntValue();
					speedLimitText = String.valueOf(cachedSpeedLimit);
					if (!Algorithms.stringsEqual(speedLimitText, cachedSpeedLimitText)) {
						cachedSpeedLimitText = speedLimitText;
						isChanged = true;
					}
				} else {
					cachedSpeedLimitText = null;
				}
				if (alarm != null) {
					setSpeedLimitText(speedLimitText);
				}
				AndroidUiHelper.updateVisibility(view, true);
				AndroidUiHelper.updateVisibility(speedLimitContainer, alarm != null);
				float delta = app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f;
				speedExceed = formattedSpeed.valueSrc > 0 && cachedSpeedLimit > 0 &&
						formattedSpeed.valueSrc > cachedSpeedLimit + delta;
			} else if (cachedSpeed != 0) {
				cachedSpeed = 0;
				OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
				setSpeedText(formattedSpeed.value, formattedSpeed.unit);
				AndroidUiHelper.updateVisibility(view, true);
			} else {
				AndroidUiHelper.updateVisibility(view, false);
			}
			setSpeedTextColor(getSpeedTextColor(speedExceed));
			if (drawBitmap) {
				if (isChanged) {
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
					if (cachedSpeedLimitText != null) {
						Drawable speedLimitDrawable = isUsaOrCanadaRegion() ?
								AppCompatResources.getDrawable(app, R.drawable.ic_limit_us_canada) : getSpeedLimitDrawable(nightMode, density);
						float speedLimitSize = (newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEED_LIMIT_SIZE_S : SPEED_LIMIT_SIZE_M) * density;
						speedLimitWidth = speedLimitSize;
						speedLimitHeight = speedLimitSize;
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
		Rect speedArea = new Rect((int) speedometerLeft, (int) speedometerTop, (int) (speedometerLeft + speedometerBg.getWidth()), (int) (speedometerTop + speedometerBg.getHeight()));
		int textSize = newWidgetSize == WidgetSize.LARGE ? SPEEDOMETER_TEXT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEEDOMETER_TEXT_SIZE_S : SPEEDOMETER_TEXT_SIZE_M;
		drawCurrentSpeed(widgetCanvas, textSize, speedArea, density, speedExceed);

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
		if (!isUsaOrCanadaRegion()) {
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
		drawSpeedLimit(widgetCanvas, newWidgetSize == WidgetSize.LARGE ? SPEED_LIMIT_TEXT_SIZE_L : newWidgetSize == WidgetSize.SMALL ? SPEED_LIMIT_TEXT_SIZE_S : SPEED_LIMIT_TEXT_SIZE_M, density, alertRect);
	}

	private void drawSpeedLimit(Canvas canvas, int textSize, float density, Rect alertRect) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(app.getColor(R.color.widgettext_day));
		textPaint.setTextSize(textSize * density);
		textPaint.setTypeface(FontCache.getMediumFont());

		Rect textBounds = new Rect();
		textPaint.getTextBounds(cachedSpeedLimitText, 0, cachedSpeedLimitText.length(), textBounds);
		float x = alertRect.left + (float) alertRect.width() / 2 - textPaint.measureText(cachedSpeedLimitText) / 2;
		float y;
		if (isUsaRegion()) {
			y = alertRect.bottom - US_SPEED_LIMIT_BOTTOM * density;
		} else if (isCanadaRegion()) {
			y = alertRect.bottom - CAN_SPEED_LIMIT_BOTTOM * density;
		} else {
			y = alertRect.top + (float) alertRect.height() / 2 + (float) textBounds.height() / 2;
		}
		canvas.drawText(cachedSpeedLimitText, x, y, textPaint);
	}

	private void drawCurrentSpeed(Canvas canvas, int textSize, Rect speedArea, float density, boolean speedExceed) {
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setTypeface(FontCache.getMediumFont());

		OsmAndFormatter.FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
		float unitTextSize = SPEEDOMETER_UNIT_TEXT_SIZE * density;
		textPaint.setTextSize(unitTextSize);
		textPaint.setTypeface(DEFAULT);
		Rect speedUnitRect = new Rect();
		String speedUnitText = formattedSpeed.unit.toUpperCase();
		textPaint.getTextBounds(speedUnitText, 0, speedUnitText.length(), speedUnitRect);
		float speedUnitWidth = speedUnitRect.width();

		float xUnit = canvas.getWidth() - speedUnitWidth - SPEEDOMETER_PADDING_SIDE_AA * density;
		float yUnit = speedArea.bottom - SPEEDOMETER_PADDING_TOP_BOTTOM_AA * density;
		textPaint.setColor(app.getColor(R.color.text_color_secondary_dark));
		canvas.drawText(speedUnitText, xUnit, yUnit, textPaint);

		textPaint.setColor(getSpeedTextColor(speedExceed));
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

	private int getSpeedTextColor(boolean speedExceed) {
		return app.getColor(speedExceed ? R.color.text_color_negative : lastNightMode ? R.color.widgettext_night : R.color.widgettext_day);
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

	private void setSpeedTextColor(int color) {
		if (speedometerValueView != null) {
			speedometerValueView.setTextColor(color);
		}
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
