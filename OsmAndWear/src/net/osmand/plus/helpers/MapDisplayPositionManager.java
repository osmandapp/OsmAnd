package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapDisplayPositionManager {

	private OsmandMapTileView mapView;
	private final OsmandSettings settings;
	private List<IMapDisplayPositionProvider> externalProviders = new ArrayList<>();

	public MapDisplayPositionManager(@NonNull OsmandApplication app) {
		this.settings = app.getSettings();
	}

	public void setMapView(@Nullable OsmandMapTileView mapView) {
		this.mapView = mapView;
	}

	public void updateProviders(@NonNull IMapDisplayPositionProvider provider, boolean shouldRegister) {
		if (shouldRegister) {
			registerProvider(provider);
		} else {
			unregisterProvider(provider);
		}
	}

	public void registerProvider(@NonNull IMapDisplayPositionProvider provider) {
		if (!externalProviders.contains(provider)) {
			externalProviders = Algorithms.addToList(externalProviders, provider);
		}
	}

	public void unregisterProvider(@NonNull IMapDisplayPositionProvider provider) {
		externalProviders = Algorithms.removeFromList(externalProviders, provider);
	}

	public void updateMapDisplayPosition() {
		updateMapDisplayPosition(false);
	}

	public void updateMapDisplayPosition(boolean refreshMap) {
		if (mapView != null) {
			updateMapDisplayPositionImpl(refreshMap);
		}
	}

	private void updateMapDisplayPositionImpl(boolean shouldRefreshMap) {
		Integer position = getPositionFromProviders();
		if (position == null) {
			position = getPositionFromPreferences();
		}
		mapView.setMapPosition(position);
		refreshMapIfNeeded(shouldRefreshMap);
	}

	private void refreshMapIfNeeded(boolean shouldRefreshMap) {
		if (!shouldRefreshMap) {
			return;
		}
		if (mapView != null) {
			MapActivity mapActivity = mapView.getMapActivity();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		}
	}

	private Integer getPositionFromProviders() {
		for (IMapDisplayPositionProvider provider : externalProviders) {
			Integer position = provider.getMapDisplayPosition();
			if (position != null) {
				return position;
			}
		}
		return null;
	}

	private int getPositionFromPreferences() {
		if (useCenterByDefault() || (useAutomaticByDefault() && useCenterForAutomatic())) {
			return OsmandSettings.CENTER_CONSTANT;
		} else {
			return OsmandSettings.BOTTOM_CONSTANT;
		}
	}

	private boolean useCenterByDefault() {
		return settings.POSITION_PLACEMENT_ON_MAP.get() == OsmandSettings.POSITION_PLACEMENT_CENTER;
	}

	private boolean useAutomaticByDefault() {
		return settings.POSITION_PLACEMENT_ON_MAP.get() == OsmandSettings.POSITION_PLACEMENT_AUTOMATIC;
	}

	private boolean useCenterForAutomatic() {
		return settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_BEARING;
	}

	public static interface IMapDisplayPositionProvider {
		@Nullable Integer getMapDisplayPosition();
	}
}
