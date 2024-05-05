package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.utils.GlideUtils.areAltitudesEqual;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.OnResultCallback;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.utils.GlideUtils;
import net.osmand.plus.views.mapwidgets.widgetstates.GlideTargetWidgetState;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import java.util.Objects;

public class GlideTargetWidget extends GlideBaseWidget {

	private final GlideTargetWidgetState widgetState;
	private Location cachedCurrentLocation = null;
	private Double cachedCurrentAltitude =null;
	private LatLon cachedTargetLocation = null;
	private Double cachedTargetAltitude = null;
	private String cachedFormattedRatio = null;

	private boolean forceUpdate; // becomes 'true' when widget state switched

	public GlideTargetWidget(@NonNull MapActivity mapActivity, @NonNull GlideTargetWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, WidgetType.GLIDE_TARGET, customId, widgetsPanel);
		this.widgetState = widgetState;
		updateInfo(null);

		setOnClickListener(getOnClickListener());
		updateWidgetName();
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			forceUpdate = true;
			widgetState.changeToNextState();
			updateInfo(null);
			updateWidgetName();
		};
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		if (isInTargetAltitudeState()) {
			updateTargetAltitude();
		} else {
			updateRequiredRatioToTarget();
		}
	}

	@Nullable
	protected String getWidgetName() {
		if (widgetState != null) {
			return getString(isInTargetAltitudeState() ? R.string.target_elevation : R.string.glide_ratio_to_target);
		}
		return widgetType != null ? getString(widgetType.titleId) : null;
	}

	private void updateTargetAltitude() {
		LatLon targetLocation = getTargetLocation();
		boolean locationChanged = !MapUtils.areLatLonEqual(targetLocation, cachedTargetLocation);

		boolean metricSystemChanged = isUpdateNeeded();
		boolean updateNeeded = locationChanged || metricSystemChanged;

		if (!forceUpdate && !updateNeeded && !isTimeToUpdate(LONG_UPDATE_INTERVAL_MILLIS)) {
			// Avoid too frequent calculations
			return;
		}

		cachedTargetLocation = targetLocation;

		calculateAltitude(targetLocation, targetAltitude -> {
			markUpdated();
			if (forceUpdate || metricSystemChanged || !areAltitudesEqual(cachedTargetAltitude, targetAltitude)) {
				cachedTargetAltitude = targetAltitude;
				if (cachedTargetAltitude == null) {
					setText(NO_VALUE, null);
				} else {
					String formattedAltitude = OsmAndFormatter.getFormattedAlt(cachedTargetAltitude, app);
					int index = formattedAltitude.lastIndexOf(' ');
					if (index == -1) {
						setText(formattedAltitude, null);
					} else {
						setText(formattedAltitude.substring(0, index), formattedAltitude.substring(index + 1));
					}
				}
			}
		});
	}

	private void updateRequiredRatioToTarget() {
		Location currentLocation = getCurrentLocation();
		boolean currentLocationChanged = !MapUtils.areLatLonEqual(currentLocation, cachedCurrentLocation);

		Double currentAltitude = currentLocation != null && currentLocation.hasAltitude()
				? currentLocation.getAltitude()
				: null;
		boolean currentAltitudeChanged = !areAltitudesEqual(currentAltitude, cachedCurrentAltitude);

		LatLon targetLocation = getTargetLocation();
		boolean targetLocationChanged = !MapUtils.areLatLonEqual(targetLocation, cachedTargetLocation);

		boolean anyChanged = currentLocationChanged || currentAltitudeChanged || targetLocationChanged;
		if (!forceUpdate && !anyChanged && !isTimeToUpdate(LONG_UPDATE_INTERVAL_MILLIS)) {
			// Avoid too frequent calculations
			return;
		}

		markUpdated();
		cachedCurrentLocation = currentLocation;
		cachedCurrentAltitude = currentAltitude;
		cachedTargetLocation = targetLocation;

		calculateAltitude(targetLocation, targetAltitude -> {
			markUpdated();
			if (forceUpdate || anyChanged || !areAltitudesEqual(cachedTargetAltitude, targetAltitude)) {
				cachedTargetAltitude = targetAltitude;
				String ratio = calculateFormattedRatio(currentLocation, currentAltitude, targetLocation, targetAltitude);
				if (forceUpdate || !Objects.equals(cachedFormattedRatio, ratio)) {
					cachedFormattedRatio = ratio;
					if (cachedFormattedRatio != null) {
						setText(cachedFormattedRatio, null);
					} else {
						setText(NO_VALUE, null);
					}
				}
			}
		});
	}

	@Nullable
	private Location getCurrentLocation() {
		return locationProvider.getLastKnownLocation();
	}

	@Nullable
	private LatLon getTargetLocation() {
		MapMarker mapMarker = app.getMapMarkersHelper().getFirstMapMarker();
		if (mapMarker != null) {
			return new LatLon(mapMarker.getLatitude(), mapMarker.getLongitude());
		}
		return null;
	}

	private void calculateAltitude(@Nullable LatLon location, @NonNull OnResultCallback<Double> callback) {
		MapRendererView mapRenderer = mapActivity.getMapView().getMapRenderer();
		if (mapRenderer == null || location == null) {
			callback.onResult(null);
		} else {
			NativeUtilities.getAltitudeForLatLon(mapRenderer, location, callback);
		}
	}

	@Nullable
	private String calculateFormattedRatio(Location l1, Double a1, LatLon l2, Double a2) {
		if (CollectionUtils.anyIsNull(l1, a1, l2, a2)) {
			return null;
		}
		LatLon l1LatLon = new LatLon(l1.getLatitude(), l1.getLongitude());
		return GlideUtils.calculateFormattedRatio(app, l1LatLon, l2, a1, a2);
	}

	public boolean isInTargetAltitudeState() {
		return widgetState.getPreference().get();
	}
}
