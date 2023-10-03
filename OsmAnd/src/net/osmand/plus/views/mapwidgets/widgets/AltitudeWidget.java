package net.osmand.plus.views.mapwidgets.widgets;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

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
		Double altitude = getAltitudeInMeters();
		if (altitude != null) {
			int newAltitude = (int) (double) altitude;
			if (isUpdateNeeded() || cachedAltitude != (int) newAltitude) {
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

	private Double getAltitudeInMeters() {
		switch (widgetType) {
			case ALTITUDE_MY_LOCATION: {
				Location location = locationProvider.getLastKnownLocation();
				if (location != null && location.hasAltitude()) {
					return location.getAltitude();
				}
				break;
			}
			case ALTITUDE_MAP_CENTER: {
				MapRendererView mapRenderer = mapView.getMapRenderer();
				if (mapRenderer != null) {
					RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
					PointI screenPoint = new PointI(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
					return NativeUtilities.getAltitudeForPixelPoint(mapRenderer, screenPoint);
				}
				break;
			}
		}
		return null;
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}