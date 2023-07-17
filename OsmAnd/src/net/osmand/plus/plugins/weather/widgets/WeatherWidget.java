package net.osmand.plus.plugins.weather.widgets;

import net.osmand.IndexConstants;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_Metric_t;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherTileResourcesManager.IObtainValueAsyncCallback;
import net.osmand.core.jni.WeatherTileResourcesManager.ValueRequest;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WeatherWidget extends TextInfoWidget {

	public static final String TAG = WeatherWidget.class.getSimpleName();

	private static final long TRUNCATE_MINUTES = 60 * 60 * 1000;

	private static final DateFormat forecastNamingFormat = new SimpleDateFormat("yyyyMMdd_HH00");
	private static final DateFormat timeFormat = new SimpleDateFormat("d MMM HH:mm");

	static {
		forecastNamingFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final WeatherHelper weatherHelper;
	private final IObtainValueAsyncCallback callback;
	private final WeatherBand weatherBand;
	private final short band;

	private PointI lastPotition31;
	private ZoomLevel lastZoom;
	private Long dateTime;
	private long lastDateTime;

	private boolean lastObtainingFailed;
	private long lastSuccessfulObtainedRequestTime;

	public WeatherWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, short band) {
		super(mapActivity, widgetType);
		this.band = band;
		this.weatherHelper = app.getWeatherHelper();
		this.weatherBand = weatherHelper.getWeatherBand(band);
		this.callback = new IObtainValueAsyncCallback() {
			@Override
			public void method(boolean succeeded, long requestedTime, double value, SWIGTYPE_p_std__shared_ptrT_Metric_t metric) {
				WeatherTileResourcesManager resourcesManager = weatherHelper.getWeatherResourcesManager();
				if (succeeded && resourcesManager != null) {
					lastObtainingFailed = false;
					lastSuccessfulObtainedRequestTime = requestedTime;
					value = resourcesManager.getConvertedBandValue(band, value);
					String formattedValue = resourcesManager.getFormattedBandValue(band, value, true);
					onValueObtained(true, value, formattedValue);
				} else {
					lastObtainingFailed = true;
					onValueObtained(false, value, null);
				}
			}
		};
		this.callback.swigReleaseOwnership();
		setIcons(widgetType);
		setText(NO_VALUE, null);
		setOnClickListener(v -> {
			if (PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
				showForecastInfoToast();
			}
		});
	}

	public void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue) {
		app.runInUIThread(() -> {
			if (succeeded && !Algorithms.isEmpty(formattedValue)) {
				setText(formattedValue, weatherBand.getBandUnit().getSymbol());
			} else {
				setText(NO_VALUE, null);
			}
			mapActivity.getMapLayers().getMapInfoLayer().updateSideWidgets();
		});
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
		return WeatherHelper.roundForecastTimeToHour(time);
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
	public void updateInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
		PointI point31 = getPoint31();
		ZoomLevel zoom = getZoom();
		long dateTime = getDateTime();
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

		if (lastSuccessfulObtainedRequestTime != 0) {
			long forecastTime = lastSuccessfulObtainedRequestTime / TRUNCATE_MINUTES * TRUNCATE_MINUTES;
			stringBuilder.append("For date: ")
					.append(timeFormat.format(new Date(forecastTime)));

			long lastDownload = getForecastDbLastDownload(lastSuccessfulObtainedRequestTime);
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
