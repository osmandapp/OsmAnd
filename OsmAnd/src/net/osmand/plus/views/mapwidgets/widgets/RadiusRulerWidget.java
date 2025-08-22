package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.Set;

public class RadiusRulerWidget extends SimpleWidget {

	private RadiusRulerMode cachedRadiusRulerMode;

	public RadiusRulerWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, RADIUS_RULER, customId, widgetsPanel);
		cachedRadiusRulerMode = settings.RADIUS_RULER_MODE.get();

		updateIcons();
		setText(NO_VALUE, null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> switchRadiusRulerMode();
	}

	private void switchRadiusRulerMode() {
		RadiusRulerMode radiusRulerMode = settings.RADIUS_RULER_MODE.get();
		cachedRadiusRulerMode = radiusRulerMode.next();
		updateIcons();
		settings.RADIUS_RULER_MODE.set(cachedRadiusRulerMode);
		mapActivity.refreshMap();
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		Location currentLocation = locationProvider.getLastKnownLocation();
		LatLon centerLocation = mapActivity.getMapLocation();

		RadiusRulerMode radiusRulerMode = settings.RADIUS_RULER_MODE.get();
		if (radiusRulerMode != cachedRadiusRulerMode) {
			cachedRadiusRulerMode = radiusRulerMode;
			updateIcons();
		}

		if (currentLocation != null && centerLocation != null) {
			if (mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				setDistanceText(0);
			} else {
				double currentLat = currentLocation.getLatitude();
				double currentLon = currentLocation.getLongitude();
				float distance = ((float) MapUtils.getDistance(centerLocation, currentLat, currentLon));
				setDistanceText(distance);
			}
		} else {
			setText(NO_VALUE, null);
		}
	}

	private void updateIcons() {
		if (cachedRadiusRulerMode == RadiusRulerMode.FIRST || cachedRadiusRulerMode == RadiusRulerMode.SECOND) {
			setIcons(RADIUS_RULER);
		} else {
			setIcons(R.drawable.widget_hidden_day, R.drawable.widget_hidden_night);
		}
	}

	private void setDistanceText(float dist) {
		FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(dist, app);
		setText(formattedDistance.value, formattedDistance.unit);
	}

	@Nullable
	@Override
	public OsmandPreference<?> getWidgetSettingsPrefToReset(@NonNull ApplicationMode appMode) {
		MapWidgetRegistry mapWidgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> widgetInfos = mapWidgetRegistry
				.getWidgetsForPanel(mapActivity, appMode, ENABLED_MODE, Collections.singletonList(WidgetsPanel.LEFT));
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			MapWidget widget = widgetInfo.widget;
			boolean anotherRulerWidgetPresent = widget instanceof RadiusRulerWidget && !widget.equals(this);
			if (anotherRulerWidgetPresent) {
				return null;
			}
		}
		return settings.RADIUS_RULER_MODE;
	}
}