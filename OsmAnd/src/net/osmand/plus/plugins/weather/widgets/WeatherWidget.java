package net.osmand.plus.plugins.weather.widgets;

import android.view.View;

import net.osmand.IndexConstants;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Metric;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherTileResourcesManager.IObtainValueAsyncCallback;
import net.osmand.core.jni.WeatherTileResourcesManager.ValueRequest;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.WeatherUtils;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WeatherWidget extends SimpleWidget {

	public static final String TAG = WeatherWidget.class.getSimpleName();

	private static final long TRUNCATE_MINUTES = 60 * 60 * 1000;

	private static final int MAX_METERS_TO_PREVIOUS_FORECAST = 30 * 1000;
	private static final int HIDE_OLD_DATA_DELAY = 1000;

	private static final DateFormat forecastNamingFormat = new SimpleDateFormat("yyyyMMdd_HH00");
	private static final DateFormat timeFormat = new SimpleDateFormat("d MMM HH:mm");

	static {
		forecastNamingFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final WeatherHelper weatherHelper;
	private final IObtainValueAsyncCallback callback;
	private final WeatherBand weatherBand;
	private final short band;
	private final int hideOldDataMessageId;

	private PointI lastPotition31;
	private ZoomLevel lastZoom;
	private Long dateTime;
	private long lastDateTime;

	private boolean lastObtainingFailed;
	private PointI lastDisplayedForecastPoint31;
	private long lastDisplayedForecastTime;
	private WeatherPlugin plugin;

	public WeatherWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel panel, short band) {
		super(mapActivity, widgetType, customId, panel);
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		this.band = band;
		this.hideOldDataMessageId = OsmAndConstants.UI_HANDLER_WEATHER_WIDGET + band;
		this.weatherHelper = app.getWeatherHelper();
		this.weatherBand = weatherHelper.getWeatherBand(band);
		this.callback = new IObtainValueAsyncCallback() {
			@Override
			public void method(boolean succeeded, PointI point31, long requestedTime, double value, Metric metric) {
				app.runInUIThread(() -> onValueObtained(succeeded, point31, requestedTime, value));
			}
		};
		this.callback.swigReleaseOwnership();
		setIcons(widgetType);
		setText(NO_VALUE, null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			if (PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
				showForecastInfoToast();
			}
		};
	}

	private void onValueObtained(boolean success, @NonNull PointI requestedPoint31, long requestedTime, double value) {
		WeatherTileResourcesManager resourcesManager = weatherHelper.getWeatherResourcesManager();
		if (success && resourcesManager != null) {
			lastObtainingFailed = false;

			PointI point31 = getPoint31();
			if (point31 != null && getMetersBetweenPoints(point31, requestedPoint31) > MAX_METERS_TO_PREVIOUS_FORECAST) {
				return;
			}

			lastDisplayedForecastPoint31 = requestedPoint31;
			lastDisplayedForecastTime = requestedTime;

			double convertedValue = resourcesManager.getConvertedBandValue(band, value);
			String formattedValue = resourcesManager.getFormattedBandValue(band, convertedValue, true);
			updateContent(formattedValue);
		} else {
			lastObtainingFailed = true;
			updateContent(null);
		}
	}

	public void updateContent(@Nullable String formattedValue) {
		app.removeMessagesInUiThread(hideOldDataMessageId);
		if (!Algorithms.isEmpty(formattedValue)) {
			WeatherSource weatherSource = plugin.getWeatherSource();
			if (weatherSource == WeatherSource.ECMWF &&
					(widgetType == WidgetType.WEATHER_CLOUDS_WIDGET || widgetType == WidgetType.WEATHER_WIND_WIDGET) &&
					"0".equals(formattedValue)) {
				setText(NO_VALUE, weatherBand.getBandUnit().getUnit(app));
			} else {
				setText(formattedValue, weatherBand.getBandUnit().getUnit(app));
			}
		} else {
			setText(NO_VALUE, null);
		}
		mapActivity.getMapLayers().getMapInfoLayer().updateSideWidgets();
	}

	public void setDateTime(@Nullable Date date) {
		this.dateTime = date != null ? date.getTime() : null;
	}

	@Nullable
	public PointI getPoint31() {
		MapRendererView mapRenderer = getMyApplication().getOsmandMap().getMapView().getMapRenderer();
		if (mapRenderer != null) {
			RotatedTileBox tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
			int centerPixelX = tileBox.getCenterPixelX();
			int centerPixelY = tileBox.getCenterPixelY();
			return NativeUtilities.get31FromElevatedPixel(mapRenderer, centerPixelX, centerPixelY);
		}
		return null;
	}

	@Nullable
	public ZoomLevel getZoom() {
		MapRendererView mapRenderer = getMyApplication().getOsmandMap().getMapView().getMapRenderer();
		return mapRenderer != null ? mapRenderer.getZoomLevel() : null;
	}

	public long getDateTime() {
		long time = dateTime != null ? dateTime : System.currentTimeMillis();
		return WeatherUtils.roundForecastTimeToHour(time);
	}

	private boolean shouldObtainValue(@Nullable PointI point31, @Nullable ZoomLevel zoom, long dateTime) {
		if (point31 == null || zoom == null || dateTime == 0) {
			return false;
		}
		if (lastPotition31 == null || lastZoom == null || lastDateTime == 0) {
			return true;
		}
		return point31.getX() != lastPotition31.getX()
				|| point31.getY() != lastPotition31.getY()
				|| zoom.ordinal() != lastZoom.ordinal()
				|| dateTime != lastDateTime;
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
		PointI point31 = getPoint31();
		ZoomLevel zoom = getZoom();
		long dateTime = getDateTime();

		boolean scheduleDashShow = dateTime != lastDateTime;
		if (!scheduleDashShow && lastDisplayedForecastPoint31 != null && point31 != null) {
			double metersToPreviousForecastPoint = getMetersBetweenPoints(lastDisplayedForecastPoint31, point31);
			if (metersToPreviousForecastPoint > MAX_METERS_TO_PREVIOUS_FORECAST) {
				scheduleDashShow = true;
			}
		}
		if (scheduleDashShow) {
			if (!app.hasMessagesInUiThread(hideOldDataMessageId)) {
				app.runMessageInUiThread(hideOldDataMessageId, HIDE_OLD_DATA_DELAY, () -> {
					lastDisplayedForecastTime = 0;
					lastDisplayedForecastPoint31 = null;
					setText(NO_VALUE, null);
				});
			}
		}

		WeatherTileResourcesManager resourcesManager = weatherHelper.getWeatherResourcesManager();
		if (resourcesManager != null && shouldObtainValue(point31, zoom, dateTime)) {
			ValueRequest request = new ValueRequest();
			request.setClientId(TAG);
			request.setBand(band);
			request.setDateTime(dateTime);
			request.setLocalData(false);
			request.setPoint31(point31);
			request.setZoom(zoom);
			request.setAbortIfNotRecent(true);
			lastPotition31 = point31;
			lastZoom = zoom;
			lastDateTime = dateTime;
			resourcesManager.obtainValueAsync(request, callback.getBinding());
		}
	}

	private void showForecastInfoToast() {
		WeatherTileResourcesManager weatherResourcesManager = weatherHelper.getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return;
		}

		StringBuilder stringBuilder = new StringBuilder();

		if (lastDisplayedForecastTime != 0) {
			long forecastTime = lastDisplayedForecastTime / TRUNCATE_MINUTES * TRUNCATE_MINUTES;
			stringBuilder.append("For date: ")
					.append(timeFormat.format(new Date(forecastTime)));

			long lastDownload = getForecastDbLastDownload(lastDisplayedForecastTime);
			if (lastDownload != 0) {
				stringBuilder.append(". Downloaded: ")
						.append(timeFormat.format(new Date(lastDownload)));
			}
		}

		if (lastDateTime != 0) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append(". ");
			}
			String tilesStatus = getTilesStatus(weatherResourcesManager, lastDateTime);
			stringBuilder.append("Status: ")
					.append(tilesStatus);
		}

		if (stringBuilder.length() > 0) {
			app.showToastMessage(stringBuilder.toString());
		}
	}

	private long getForecastDbLastDownload(long forecastSystemTime) {
		File weatherForecastDir = app.getAppPath(IndexConstants.WEATHER_FORECAST_DIR);
		String forecastDbFileName = forecastNamingFormat.format(new Date(forecastSystemTime)) + IndexConstants.TIFF_DB_EXT;
		File usedForecastDb = new File(weatherForecastDir, forecastDbFileName);

		return usedForecastDb.exists() && usedForecastDb.canRead()
				? usedForecastDb.lastModified()
				: 0;
	}

	private double getMetersBetweenPoints(@NonNull PointI a, @NonNull PointI b) {
		return MapUtils.measuredDist31(a.getX(), a.getY(), b.getX(), b.getY());
	}

	@NonNull
	private String getTilesStatus(@NonNull WeatherTileResourcesManager weatherResourcesManager, long lastDateTime) {
		if (weatherResourcesManager.isEvaluatingTiles(lastDateTime)) {
			return "processing tiles";
		} else if (weatherResourcesManager.isDownloadingTiles(lastDateTime)) {
			return "downloading tiles";
		} else if (lastObtainingFailed) {
			return "error";
		}

		return "ready";
	}
}
