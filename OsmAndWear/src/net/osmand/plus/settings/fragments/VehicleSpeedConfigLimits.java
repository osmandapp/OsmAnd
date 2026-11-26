package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GeneralRouter;

public class VehicleSpeedConfigLimits {

	// max speed limit in km/h
	public static final float MAX_DEFAULT_SPEED_LIMIT = 300;
	public static final float MAX_CAR_SPEED_LIMIT = 195;
	public static final float MAX_BICYCLE_SPEED_LIMIT = 50;
	public static final float MAX_PEDESTRIAN_SPEED_LIMIT = 15;
	public static final float MAX_HORSE_SPEED_LIMIT = 45;
	public static final float MAX_BOAT_SPEED_LIMIT = 100f;
	public static final float MAX_SKI_SPEED_LIMIT = 225;
	public static final float MAX_PUBLIC_TRANSPORT_SPEED_LIMIT = 142.5f;
	public static final float MAX_MOPED_SPEED_LIMIT = 70;
	public static final float MAX_TRAIN_SPEED_LIMIT = 255;
	public static final float MAX_STRAIGHT_SPEED_LIMIT = 1080;
	public static final float MAX_DIRECT_TO_SPEED_LIMIT = 1080;

	public static float getMaxSpeedConfigLimit(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		String routingProfile = mode.getRoutingProfile();
		RouteService routeService = mode.getRouteService();
		if (routeService == RouteService.OSMAND) {
			Float maxSpeedLimit = getMaxSpeedConfigLimit(routingProfile);
			if (maxSpeedLimit != null) {
				return maxSpeedLimit / 3.6f;
			}
		} else if (routeService == RouteService.STRAIGHT) {
			return MAX_STRAIGHT_SPEED_LIMIT / 3.6f;
		} else if (routeService == RouteService.DIRECT_TO) {
			return MAX_DIRECT_TO_SPEED_LIMIT / 3.6f;
		}
		return getRoutingDefaultMaxSpeed(app, mode);
	}

	@Nullable
	private static Float getMaxSpeedConfigLimit(@NonNull String routingProfile) {
		switch (routingProfile) {
			case "car":
				return MAX_CAR_SPEED_LIMIT;
			case "bicycle":
				return MAX_BICYCLE_SPEED_LIMIT;
			case "pedestrian":
				return MAX_PEDESTRIAN_SPEED_LIMIT;
			case "horsebackriding":
				return MAX_HORSE_SPEED_LIMIT;
			case "boat":
				return MAX_BOAT_SPEED_LIMIT;
			case "ski":
				return MAX_SKI_SPEED_LIMIT;
			case "public_transport":
				return MAX_PUBLIC_TRANSPORT_SPEED_LIMIT;
			case "moped":
				return MAX_MOPED_SPEED_LIMIT;
			case "train":
				return MAX_TRAIN_SPEED_LIMIT;
		}
		return null;
	}

	private static float getRoutingDefaultMaxSpeed(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		GeneralRouter router = app.getRouter(mode);
		return router == null ? Math.max(MAX_DEFAULT_SPEED_LIMIT / 3.6f, mode.getDefaultSpeed()) : router.getMaxSpeed() * 1.5f;
	}
}
