package net.osmand.plus.plugins.weather;

import net.osmand.IndexConstants;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_Metric_t;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherTileResourcesManager.IObtainValueAsyncCallback;
import net.osmand.core.jni.WeatherTileResourcesManager.ValueRequest;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class WeatherWidget extends TextInfoWidget {

	private static final long TRUNCATE_MINUTES = 60 * 60 * 1000;

	private static final DateFormat forecastNamingFormat = new SimpleDateFormat("yyyyMMdd_HH00");
	private static final DateFormat timeFormat = new SimpleDateFormat("d MMM HH:mm");

	static {
		forecastNamingFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	protected final WeatherPlugin weatherPlugin;
	protected final IObtainValueAsyncCallback callback;
	protected final short band;

	private PointI lastPotition31;
	private ZoomLevel lastZoom;
	private long lastDateTime;

	private boolean lastObtainingFailed;
	private long lastSuccessfulObtainedRequestTime;

	public WeatherWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin,
	                     @NonNull WidgetType widgetType, short band) {
		super(mapActivity, widgetType);
		this.weatherPlugin = weatherPlugin;
		this.band = band;
		this.callback = new IObtainValueAsyncCallback() {
			@Override
			public void method(boolean succeeded, long requestedTime, double value, SWIGTYPE_p_std__shared_ptrT_Metric_t metric) {
				WeatherTileResourcesManager resourcesManager = weatherPlugin.getWeatherResourcesManager();
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
		setOnClickListener(v -> {
			if (PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
				showForecastInfoToast();
			}
		});
	}

	public abstract void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue);

	@Nullable
	public PointI getPoint31() {
		MapRendererView mapRenderer = getMyApplication().getOsmandMap().getMapView().getMapRenderer();
		return mapRenderer != null ? mapRenderer.getTarget() : null;
	}

	@Nullable
	public ZoomLevel getZoom() {
		MapRendererView mapRenderer = getMyApplication().getOsmandMap().getMapView().getMapRenderer();
		return mapRenderer != null ? mapRenderer.getZoomLevel() : null;
	}

	public long getDateTime() {
		return System.currentTimeMillis() / 60000 * 60000; // TODO: get correct time
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
		WeatherTileResourcesManager resourcesManager = weatherPlugin.getWeatherResourcesManager();
		if (resourcesManager != null && shouldObtainValue(point31, zoom, dateTime)) {
			ValueRequest request = new ValueRequest();
			request.setBand(band);
			request.setDateTime(dateTime);
			request.setLocalData(false);
			request.setPoint31(point31);
			request.setZoom(zoom);
			lastPotition31 = point31;
			lastZoom = zoom;
			lastDateTime = dateTime;
			resourcesManager.obtainValueAsync(request, callback.getBinding());
		}
	}

	private void showForecastInfoToast() {
		WeatherTileResourcesManager weatherResourcesManager = weatherPlugin.getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return;
		}

		StringBuilder stringBuilder = new StringBuilder();

		if (lastSuccessfulObtainedRequestTime != 0) {
			long forecastTime = lastSuccessfulObtainedRequestTime / TRUNCATE_MINUTES * TRUNCATE_MINUTES;
			stringBuilder.append("Weather forecast for: ")
					.append(timeFormat.format(new Date(forecastTime)));

			long lastDownload = getForecastDbLastDownload(lastSuccessfulObtainedRequestTime);
			if (lastDownload != 0) {
				stringBuilder.append("\n")
						.append("Weather forecast downloaded: ")
						.append(timeFormat.format(new Date(lastDownload)));
			}
		}

		if (lastDateTime != 0) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append("\n");
			}
			String tilesStatus = getTilesStatus(weatherResourcesManager, lastDateTime);
			stringBuilder.append("Tiles status: ")
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
		if (weatherResourcesManager.isTileProviderEvaluatingTilesToObtainValue(lastDateTime)) {
			return "processing tiles";
		} else if (weatherResourcesManager.isTileProviderDownloadingTilesToObtainValue(lastDateTime)) {
			return "downloading tiles";
		} else if (lastObtainingFailed) {
			return "error";
		}

		return "ready";
	}
}
