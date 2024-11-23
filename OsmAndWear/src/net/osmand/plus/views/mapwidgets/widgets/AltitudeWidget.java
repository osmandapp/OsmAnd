package net.osmand.plus.views.mapwidgets.widgets;


import net.osmand.Location;
import net.osmand.OnResultCallback;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AltitudeWidget extends SimpleWidget {

	private final OsmandMapTileView mapView;
	private int cachedAltitude;

	public AltitudeWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, widgetsPanel);
		this.mapView = mapActivity.getMapView();
		setIcons(widgetType);
		setText(NO_VALUE, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		getAltitudeInMeters(this::updateAltitudeText);
	}

	private void updateAltitudeText(@Nullable Double altitude) {
		if (altitude != null) {
			int newAltitude = (int) (double) altitude;
			if (isUpdateNeeded() || cachedAltitude != newAltitude) {
				cachedAltitude = newAltitude;
				String formattedAltitude = OsmAndFormatter.getFormattedAlt(cachedAltitude, app);
				int index = formattedAltitude.lastIndexOf(' ');
				if (index == -1) {
					setText(formattedAltitude, null);
				} else {
					setText(formattedAltitude.substring(0, index), formattedAltitude.substring(index + 1));
				}
			}
		} else if (cachedAltitude != 0) {
			cachedAltitude = 0;
			setText(NO_VALUE, null);
		}

	}

	private void getAltitudeInMeters(@NonNull OnResultCallback<Double> callback) {
		switch (widgetType) {
			case ALTITUDE_MY_LOCATION: {
				Location location = locationProvider.getLastKnownLocation();
				if (location != null && location.hasAltitude()) {
					callback.onResult(location.getAltitude());
				} else {
					callback.onResult(null);
				}
				break;
			}
			case ALTITUDE_MAP_CENTER: {
				MapRendererView mapRenderer = mapView.getMapRenderer();
				if (mapRenderer != null) {
					RotatedTileBox tileBox = mapView.getRotatedTileBox();
					int centerX = tileBox.getCenterPixelX();
					int centerY = tileBox.getCenterPixelY();
					LatLon centerLatLon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, centerX, centerY);
					NativeUtilities.getAltitudeForLatLon(mapRenderer, centerLatLon, callback);
				} else {
					callback.onResult(null);
				}
				break;
			}
			default: {
				callback.onResult(null);
				break;
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}