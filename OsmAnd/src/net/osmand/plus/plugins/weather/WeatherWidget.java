package net.osmand.plus.plugins.weather;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_Metric_t;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.WeatherTileResourcesManager.IObtainValueAsyncCallback;
import net.osmand.core.jni.WeatherTileResourcesManager.ValueRequest;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public abstract class WeatherWidget extends TextInfoWidget {

	protected final WeatherPlugin weatherPlugin;
	protected final IObtainValueAsyncCallback callback;
	protected final short band;

	private PointI lastPotition31;
	private ZoomLevel lastZoom;
	private long lastDateTime;

	public WeatherWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin,
	                     @NonNull WidgetType widgetType, short band) {
		super(mapActivity, widgetType);
		this.weatherPlugin = weatherPlugin;
		this.band = band;
		this.callback = new IObtainValueAsyncCallback() {
			@Override
			public void method(boolean succeeded, double value, SWIGTYPE_p_std__shared_ptrT_Metric_t metric) {
				WeatherTileResourcesManager resourcesManager = app.getWeatherHelper().getWeatherResourcesManager();
				if (succeeded && resourcesManager != null) {
					value = resourcesManager.getConvertedBandValue(band, value);
					String formattedValue = resourcesManager.getFormattedBandValue(band, value, true);
					onValueObtained(true, value, formattedValue);
				} else {
					onValueObtained(false, value, null);
				}
			}
		};
		this.callback.swigReleaseOwnership();
		setIcons(widgetType);
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
		WeatherTileResourcesManager resourcesManager = app.getWeatherHelper().getWeatherResourcesManager();
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
}
