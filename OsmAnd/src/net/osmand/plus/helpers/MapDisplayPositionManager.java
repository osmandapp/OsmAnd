package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapDisplayPositionManager {

	private OsmandMapTileView mapView;
	private final OsmandSettings settings;
	private List<IMapDisplayPositionProvider> displayPositionProviders = new ArrayList<>();
	private List<IMapRatioShifter> mapRatioShifters = new ArrayList<>();

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
		if (!displayPositionProviders.contains(provider)) {
			displayPositionProviders = Algorithms.addToList(displayPositionProviders, provider);
		}
	}

	public void unregisterProvider(@NonNull IMapDisplayPositionProvider provider) {
		displayPositionProviders = Algorithms.removeFromList(displayPositionProviders, provider);
	}

	public void registerMapRatioShifter(@NonNull IMapRatioShifter shifter) {
		if (!mapRatioShifters.contains(shifter)) {
			mapRatioShifters = Algorithms.addToList(mapRatioShifters, shifter);
		}
	}

	public void unregisterMapRatioShifter(@NonNull IMapRatioShifter shifter) {
		mapRatioShifters = Algorithms.removeFromList(mapRatioShifters, shifter);
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
		MapPosition positionFromProviders = getPositionFromProviders();
		if (positionFromProviders != null) {
			mapView.setMapPosition(positionFromProviders);
		} else {
			MapPosition position = getPositionFromPreferences();
			float shifterRatioY = getShiftedRatioY(position.getRatioY());
			if (shifterRatioY != 0.0f) {
				mapView.setCustomMapRatioY(shifterRatioY);
			} else {
				mapView.setMapPosition(position);
			}
		}
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

	@Nullable
	private MapPosition getPositionFromProviders() {
		for (IMapDisplayPositionProvider provider : displayPositionProviders) {
			MapPosition position = provider.getMapDisplayPosition();
			if (position != null) {
				return position;
			}
		}
		return null;
	}

	@NonNull
	private MapPosition getPositionFromPreferences() {
		if (useCenterByDefault() || (useAutomaticByDefault() && useCenterForAutomatic())) {
			return MapPosition.CENTER;
		} else {
			return MapPosition.BOTTOM;
		}
	}

	private float getShiftedRatioY(float originalRatioY) {
		for (IMapRatioShifter shifter : mapRatioShifters) {
			float shiftedMapRatioY = shifter.getShiftedMapRatioY(originalRatioY);
			if (shiftedMapRatioY != 0.0f) {
				return shiftedMapRatioY;
			}
		}
		return 0.0f;
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

	public interface IMapDisplayPositionProvider {
		@Nullable
		MapPosition getMapDisplayPosition();
	}
	
	public interface IMapRatioShifter {
		
		float getShiftedMapRatioY(float originalRatioY);
	}
}
