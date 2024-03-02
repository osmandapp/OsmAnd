package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.SunPositionMode;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.SunriseSunsetWidgetState;
import net.osmand.util.Algorithms;
import net.osmand.util.SunriseSunset;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SunriseSunsetWidget extends SimpleWidget {

	private static final String NEXT_TIME_FORMAT = "HH:mm E";

	private static final int TIME_LEFT_UPDATE_INTERVAL_MS = 60_000; // every minute

	private final OsmandMapTileView mapView;
	private final DayNightHelper dayNightHelper;
	private final SunriseSunsetWidgetState widgetState;

	private boolean lastIsDaytime;
	private long lastUpdateTime;
	private long timeToNextUpdate;
	private long cachedNextTime;
	private LatLon cachedCenterLatLon;
	private boolean isLocationChanged;
	private boolean forceUpdate;

	public SunriseSunsetWidget(@NonNull MapActivity mapActivity, @NonNull SunriseSunsetWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetState.getWidgetType(), customId, widgetsPanel);
		dayNightHelper = app.getDaynightHelper();
		this.widgetState = widgetState;
		this.mapView = mapActivity.getMapView();
		setIcons(widgetState.getWidgetType());
		setText(NO_VALUE, null);
		setOnClickListener(getOnClickListener());
		updateWidgetName();
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			forceUpdate = true;
			widgetState.changeToNextState();
			updateInfo(null);
			mapActivity.refreshMap();
			updateWidgetName();
		};
	}

	@Nullable
	protected String getAdditionalWidgetName() {
		if (widgetState != null) {
			return getString(widgetState.getPreference().get() ? R.string.shared_string_time_left : R.string.shared_string_next);
		}
		return null;
	}

	@Nullable
	protected String getWidgetName() {
		SunPositionMode sunPositionMode = null;
		if (widgetState != null) {
			sunPositionMode = widgetState.getSunPositionPreference().get();
		}
		int sunsetStringId = R.string.shared_string_sunset;
		int sunriseStringId = R.string.shared_string_sunrise;
		if (WidgetType.SUNSET == widgetType || (WidgetType.SUN_POSITION == widgetType && sunPositionMode == SunPositionMode.SUNSET_MODE)) {
			return getString(sunsetStringId);
		} else if (WidgetType.SUN_POSITION == widgetType && sunPositionMode == SunPositionMode.SUN_POSITION_MODE) {
			return getString(lastIsDaytime ? sunsetStringId : sunriseStringId);
		} else {
			return getString(sunriseStringId);
		}
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		updateCachedLocation();
		if (!isUpdateNeeded()) {
			return;
		}
		String value = isShowTimeLeft() ? formatTimeLeft(app, getTimeLeft()) : formatNextTime(getNextTime());
		if (!Algorithms.isBlank(value)) {
			String[] split = value.split(" ");
			if (split.length == 2) {
				setText(split[0], split[1]);
			} else {
				setText(value, null);
			}
			forceUpdate = false;
			isLocationChanged = false;
			lastUpdateTime = System.currentTimeMillis();
			timeToNextUpdate = (cachedNextTime - lastUpdateTime) % TIME_LEFT_UPDATE_INTERVAL_MS;
		} else {
			setText(NO_VALUE, null);
			forceUpdate = true;
		}
		if (widgetType == WidgetType.SUN_POSITION) {
			updateWidgetName();
		}
	}

	@Override
	public boolean isUpdateNeeded() {
		if (forceUpdate || isLocationChanged) {
			return true;
		}
		if (isShowTimeLeft()) {
			return (lastUpdateTime + timeToNextUpdate) <= System.currentTimeMillis();
		} else {
			return cachedNextTime <= System.currentTimeMillis();
		}
	}

	public boolean isShowTimeLeft() {
		return getPreference().get();
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return widgetState.getPreference();
	}

	@Nullable
	public OsmandPreference<SunPositionMode> getSunPositionPreference() {
		return widgetState != null ? widgetState.getSunPositionPreference() : null;
	}

	private void updateCachedLocation() {
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
		LatLon newCenterLatLon = tileBox.getCenterLatLon();
		if (!isLocationsEqual(cachedCenterLatLon, newCenterLatLon)) {
			cachedCenterLatLon = newCenterLatLon;
			isLocationChanged = true;
		}
	}

	private boolean isLocationsEqual(@Nullable LatLon previousLatLon, @Nullable LatLon newLatLon) {
		if (previousLatLon != null && newLatLon != null) {
			double lat = previousLatLon.getLatitude();
			double newLat = newLatLon.getLatitude();
			return Math.abs(lat - newLat) <= 0.001;
		}
		return false;
	}

	public long getTimeLeft() {
		long nextTime = getNextTime();
		return nextTime > 0 ? Math.abs(nextTime - System.currentTimeMillis()) : -1;
	}

	public long getNextTime() {
		if (cachedCenterLatLon != null) {
			double lat = cachedCenterLatLon.getLatitude();
			double lon = cachedCenterLatLon.getLongitude();
			SunriseSunset sunriseSunset = dayNightHelper.getSunriseSunset(lat, lon);
			Date sunrise = sunriseSunset.getSunrise();
			Date sunset = sunriseSunset.getSunset();
			Date nextTimeDate;
			SunPositionMode sunPositionMode = widgetState.getSunPositionPreference().get();
			WidgetType type = widgetState.getWidgetType();
			if (WidgetType.SUNSET == type || (WidgetType.SUN_POSITION == type && sunPositionMode == SunPositionMode.SUNSET_MODE)) {
				nextTimeDate = sunset;
			} else if (WidgetType.SUN_POSITION == type && sunPositionMode == SunPositionMode.SUN_POSITION_MODE) {
				lastIsDaytime = sunriseSunset.isDaytime();
				nextTimeDate = lastIsDaytime ? sunset : sunrise;
			} else {
				nextTimeDate = sunrise;
			}
			if (nextTimeDate != null) {
				long now = System.currentTimeMillis();
				if (isLocationChanged || now >= cachedNextTime) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(nextTimeDate);
					// If sunrise or sunset has passed today, move the date to the next day
					if (calendar.getTimeInMillis() <= now) {
						calendar.add(Calendar.DAY_OF_MONTH, 1);
					}
					cachedNextTime = calendar.getTimeInMillis();
				}
				return cachedNextTime;
			}
		}
		return -1;
	}

	@Nullable
	public static String formatTimeLeft(@NonNull Context ctx, long timeLeft) {
		if (timeLeft >= 0) {
			long diffInMinutes = TimeUnit.MINUTES.convert(timeLeft, TimeUnit.MILLISECONDS);
			String hour = ctx.getString(R.string.int_hour);
			String minute = ctx.getString(R.string.int_min);
			String timeUnits = diffInMinutes >= 60 ? hour : minute;
			String formattedDuration = Algorithms.formatMinutesDuration((int) diffInMinutes);
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, formattedDuration, timeUnits);
		}
		return null;
	}

	@Nullable
	public static String formatNextTime(long nextTime) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(NEXT_TIME_FORMAT, Locale.getDefault());
		return nextTime > 0 ? dateFormat.format(nextTime) : null;
	}
}
