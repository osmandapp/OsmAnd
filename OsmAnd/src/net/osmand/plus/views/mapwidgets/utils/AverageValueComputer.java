package net.osmand.plus.views.mapwidgets.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.simulation.SimulationProvider;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AverageValueComputer {

	public static final long BIGGEST_MEASURED_INTERVAL;
	public static final long ADD_POINT_INTERVAL_MILLIS = 1000;
	public static final long DEFAULT_INTERVAL_MILLIS = 30 * 60 * 1000L;
	public static final List<Long> MEASURED_INTERVALS;

	static {
		List<Long> modifiableIntervals = new ArrayList<>();
		modifiableIntervals.add(15 * 1000L);
		modifiableIntervals.add(30 * 1000L);
		modifiableIntervals.add(45 * 1000L);
		for (int i = 1; i <= 60; i++) {
			modifiableIntervals.add(i * 60 * 1000L);
		}
		MEASURED_INTERVALS = Collections.unmodifiableList(modifiableIntervals);
		BIGGEST_MEASURED_INTERVAL = MEASURED_INTERVALS.get(MEASURED_INTERVALS.size() - 1);
	}

	protected final OsmandApplication app;

	protected final OsmandSettings settings;

	protected final List<Location> locations = new LinkedList<>();

	public AverageValueComputer(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public void updateLocation(@Nullable Location location) {
		if (location != null) {
			long time = System.currentTimeMillis();
			boolean save = isEnabled() && SimulationProvider.isNotSimulatedLocation(location);
			if (save) {
				saveLocation(location, time);
			}
		}
	}

	protected void clearExpiredLocations(@NonNull List<Location> locations, long measuredInterval) {
		long expirationTime = System.currentTimeMillis() - measuredInterval;
		for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext(); ) {
			Location location = iterator.next();
			if (location.getTime() < expirationTime) {
				iterator.remove();
			} else {
				break;
			}
		}
	}

	protected abstract boolean isEnabled();

	protected abstract void saveLocation(@NonNull Location location, long time);
}
