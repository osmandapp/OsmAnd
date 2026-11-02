package net.osmand.plus.views.mapwidgets.widgets;

import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.utils.InclinometerSensor;
import net.osmand.plus.views.mapwidgets.utils.InclinometerSensor.InclinometerListener;
import net.osmand.plus.views.mapwidgets.widgetstates.InclinometerWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.ResizableWidgetState;

import java.util.Locale;
import java.util.Objects;

public class InclinometerWidget extends SimpleWidget implements InclinometerListener {

	private static final float ZONE_HYSTERESIS_DEGREES = 1f;
	private static final int ALERT_TONE_DURATION_MS = 150;
	private static final int ALERT_TONE_PAUSE_MS = 250;
	private static final int ALERT_TONE_VOLUME = 80;

	// Solid triangles: unlike the arrow glyphs, all four come from the same font and look alike
	private static final String ARROW_FRONT_UP = "\u25B2";
	private static final String ARROW_FRONT_DOWN = "\u25BC";
	private static final String ARROW_RIGHT = "\u25B6";
	private static final String ARROW_LEFT = "\u25C0";

	private enum TiltZone {
		SAFE(R.color.inclinometer_level_safe),
		WARNING(R.color.inclinometer_level_warning),
		CRITICAL(R.color.inclinometer_level_critical);

		@ColorRes
		final int colorId;

		TiltZone(@ColorRes int colorId) {
			this.colorId = colorId;
		}
	}

	private final InclinometerWidgetState widgetState;
	private final InclinometerSensor sensor;
	private final Handler handler = new Handler(Looper.getMainLooper());

	private TiltZone zone = TiltZone.SAFE;
	private boolean hasValue;
	private float angle;
	@Nullable
	private String cachedText;
	@Nullable
	private String cachedDirection;

	public InclinometerWidget(@NonNull MapActivity mapActivity, @NonNull InclinometerWidgetState widgetState,
	                          @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetState.getWidgetType(), customId, widgetsPanel);
		this.widgetState = widgetState;
		this.sensor = app.getInclinometerSensor();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(widgetType);
		cachedText = null;
		cachedDirection = null;
		if (hasValue) {
			updateText();
		} else {
			setText(NO_VALUE, null);
		}
		applyZoneColor();
	}

	@Override
	public InclinometerWidgetState getWidgetState() {
		return widgetState;
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			// the units are switched either by tap or from the widget settings
			widgetState.changeToNextState();
			updateInfo(null);
		};
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		if (sensor.isSensorAvailable()) {
			sensor.requestUpdates(this);
		}
		if (hasValue) {
			updateText();
		}
	}

	@Override
	public void onInclinationChanged(float pitchDegrees, float rollDegrees) {
		angle = widgetType == WidgetType.INCLINOMETER_PITCH ? pitchDegrees : rollDegrees;
		hasValue = true;
		updateZone();
		updateText();
	}

	@Override
	public boolean isInclinometerActive() {
		return isAttached() && isViewVisible() && !mapActivity.isActivityDestroyed()
				&& mapActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
	}

	private void updateText() {
		String text = formatAngle(angle, widgetState.getPercentUnitsPref().get());
		String direction = getDirectionArrow(widgetType, angle);
		if (!Objects.equals(text, cachedText) || !Objects.equals(direction, cachedDirection)) {
			cachedText = text;
			cachedDirection = direction;
			// the arrow is part of the value, so it has the full text height and the zone color
			setText(direction != null ? text + " " + direction : text, null);
		}
	}

	@NonNull
	public static String formatAngle(float degrees, boolean percentUnits) {
		int value = Math.round(Math.abs(percentUnits ? InclinometerWidgetState.toPercent(degrees) : degrees));
		return String.format(Locale.getDefault(), percentUnits ? "%d%%" : "%d°", value);
	}

	// Arrow showing where the vehicle leans: up/down for the front (pitch), left/right for the lowered side (roll)
	@Nullable
	public static String getDirectionArrow(@NonNull WidgetType widgetType, float degrees) {
		if (Math.round(Math.abs(degrees)) == 0) {
			return null;
		}
		if (widgetType == WidgetType.INCLINOMETER_PITCH) {
			return degrees > 0 ? ARROW_FRONT_UP : ARROW_FRONT_DOWN;
		}
		return degrees > 0 ? ARROW_RIGHT : ARROW_LEFT;
	}

	private void updateZone() {
		float tilt = Math.abs(angle);
		int warningAngle = widgetState.getWarningAnglePref().get();
		int criticalAngle = widgetState.getCriticalAnglePref().get();
		TiltZone newZone = switch (zone) {
			case SAFE -> tilt >= criticalAngle ? TiltZone.CRITICAL
					: tilt >= warningAngle ? TiltZone.WARNING : TiltZone.SAFE;
			case WARNING -> tilt >= criticalAngle ? TiltZone.CRITICAL
					: tilt < warningAngle - ZONE_HYSTERESIS_DEGREES ? TiltZone.SAFE : TiltZone.WARNING;
			case CRITICAL -> tilt >= criticalAngle - ZONE_HYSTERESIS_DEGREES ? TiltZone.CRITICAL
					: tilt >= warningAngle ? TiltZone.WARNING : TiltZone.SAFE;
		};
		if (newZone != zone) {
			boolean escalated = newZone.ordinal() > zone.ordinal();
			zone = newZone;
			applyZoneColor();
			if (escalated && widgetState.getSoundAlertPref().get()) {
				playAlert(newZone);
			}
		}
	}

	private void applyZoneColor() {
		if (textView != null) {
			textView.setTextColor(ContextCompat.getColor(app, zone.colorId));
		}
	}

	@Override
	protected void onPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {
		super.onPanelAppearanceChanged(appearance);
		applyZoneColor();
	}

	private void playAlert(@NonNull TiltZone zone) {
		ToneGenerator toneGenerator;
		try {
			int stream = settings.AUDIO_MANAGER_STREAM.getModeValue(settings.getApplicationMode());
			toneGenerator = new ToneGenerator(stream, ALERT_TONE_VOLUME);
		} catch (RuntimeException e) {
			return;
		}
		toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, ALERT_TONE_DURATION_MS);
		int beeps = zone == TiltZone.CRITICAL ? 2 : 1;
		for (int i = 1; i < beeps; i++) {
			handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, ALERT_TONE_DURATION_MS),
					(long) i * (ALERT_TONE_DURATION_MS + ALERT_TONE_PAUSE_MS));
		}
		handler.postDelayed(toneGenerator::release, (long) beeps * (ALERT_TONE_DURATION_MS + ALERT_TONE_PAUSE_MS));
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copySettingsFromMode(appMode, appMode, customId);
		// copySettingsFromMode does not copy the Height setting, so copy it here
		ResizableWidgetState duplicateState = new ResizableWidgetState(app, customId, widgetType, getDefaultWidgetSize());
		duplicateState.getWidgetSizePref().setModeValue(appMode, getWidgetSizePref().getModeValue(appMode));
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}
}
