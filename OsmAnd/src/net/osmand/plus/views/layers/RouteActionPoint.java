package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;

import net.osmand.Location;

public class RouteActionPoint {

	@NonNull
	public final Location location;

	public final int index;
	public final double normalizedOffset;

	public RouteActionPoint(@NonNull Location location, int index, double normalizedOffset) {
		this.location = location;
		this.index = index;
		this.normalizedOffset = normalizedOffset;
	}
}
