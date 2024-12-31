package net.osmand.plus.measurementtool;

import static net.osmand.plus.routing.GpxApproximator.DEFAULT_POINT_APPROXIMATION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationsHolder;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

public class GpxApproximationParams {

	private ApplicationMode appMode = ApplicationMode.CAR;
	private int distanceThreshold = DEFAULT_POINT_APPROXIMATION;
	private List<LocationsHolder> locationsHolders;


	public void setTrackPoints(@NonNull List<List<WptPt>> points) {
		List<LocationsHolder> locationHolders = new ArrayList<>();
		for (List<WptPt> segment : points) {
			locationHolders.add(new LocationsHolder(SharedUtil.jWptPtList(segment)));
		}
		this.locationsHolders = locationHolders;
	}

	public boolean setAppMode(@Nullable ApplicationMode appMode) {
		if (appMode != null && this.appMode != appMode) {
			this.appMode = appMode;
			return true;
		}
		return false;
	}

	public boolean setDistanceThreshold(int newThreshold) {
		if (this.distanceThreshold != newThreshold) {
			this.distanceThreshold = newThreshold;
			return true;
		}
		return false;
	}

	public ApplicationMode getAppMode() {
		return appMode;
	}

	public int getDistanceThreshold() {
		return distanceThreshold;
	}

	public List<LocationsHolder> getLocationsHolders() {
		return locationsHolders;
	}
}
