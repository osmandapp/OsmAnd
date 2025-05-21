package net.osmand.plus.auto;

import static androidx.car.app.navigation.model.TravelEstimate.REMAINING_TIME_UNKNOWN;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.*;
import androidx.car.app.navigation.model.Trip.Builder;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import java.util.TimeZone;

public class TripHelper {

	public static final float TURN_IMAGE_SIZE_DP = 128f;
	public static final float NEXT_TURN_IMAGE_SIZE_DP = 48f;
	public static final float TURN_LANE_IMAGE_SIZE = 64f;
	public static final float TURN_LANE_IMAGE_MIN_DELTA = 36f;
	public static final float TURN_LANE_IMAGE_MARGIN = 4f;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;

	private Destination lastDestination;
	private TravelEstimate lastDestinationTravelEstimate;
	private Step lastStep;
	private TravelEstimate lastStepTravelEstimate;
	private CharSequence lastCurrentRoad;
	private AnnounceTimeDistances timeDistances;

	public TripHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.routingHelper = app.getRoutingHelper();
	}

	public Destination getLastDestination() {
		return lastDestination;
	}

	public TravelEstimate getLastDestinationTravelEstimate() {
		return lastDestinationTravelEstimate;
	}

	public Step getLastStep() {
		return lastStep;
	}

	public TravelEstimate getLastStepTravelEstimate() {
		return lastStepTravelEstimate;
	}

	public CharSequence getLastCurrentRoad() {
		return lastCurrentRoad;
	}

	@NonNull
	public Trip buildTrip(Location currentLocation, float density) {
		Trip.Builder tripBuilder = new Trip.Builder();
		updateDestination(tripBuilder);

		boolean routeBeingCalculated = routingHelper.isRouteBeingCalculated();
		tripBuilder.setLoading(routeBeingCalculated);
		if (!routeBeingCalculated) {
			setupTrip(tripBuilder, currentLocation, density);
		} else {
			lastStep = null;
			lastStepTravelEstimate = null;
		}
		return tripBuilder.build();
	}

	private void setupTrip(@NonNull Builder tripBuilder, Location currentLocation, float density) {
		Step.Builder stepBuilder = new Step.Builder();
		Maneuver.Builder turnBuilder;
		TurnType turnType = null;
		TurnType nextTurnType = null;
		boolean leftSide = app.getSettings().DRIVING_REGION.get().leftHandDriving;
		boolean deviatedFromRoute = routingHelper.isDeviatedFromRoute();
		int turnImminent = 0;
		int nextTurnDistance = 0;
		NextDirectionInfo nextDirInfo = null;
		NextDirectionInfo nextNextDirInfo = null;
		NextDirectionInfo calc = new NextDirectionInfo();
		if (deviatedFromRoute) {
			turnType = TurnType.valueOf(TurnType.OFFR, leftSide);
			nextTurnDistance = (int) routingHelper.getRouteDeviation();
		} else {
			nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, true);
			if (nextDirInfo != null && nextDirInfo.distanceTo >= 0 && nextDirInfo.directionInfo != null) {
				turnType = nextDirInfo.directionInfo.getTurnType();
				nextTurnDistance = nextDirInfo.distanceTo;
				turnImminent = nextDirInfo.imminent;
			}

		}
		if (turnType != null) {
			int height = (int) (TURN_IMAGE_SIZE_DP * density);
			int width = (int) (TURN_IMAGE_SIZE_DP * density);
			Bitmap bitmap = createTurnBitmap(turnType, deviatedFromRoute, turnImminent, width, height);

			turnBuilder = new Maneuver.Builder(TripUtils.getManeuverType(turnType));
			if (turnType.isRoundAbout()) {
				turnBuilder.setRoundaboutExitNumber(turnType.getExitOut());
			}
			turnBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build());
		} else {
			turnBuilder = new Maneuver.Builder(Maneuver.TYPE_UNKNOWN);
		}
		Maneuver maneuver = turnBuilder.build();
		AnnounceTimeDistances atd = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
		if (nextDirInfo != null && atd != null) {
			float speed = atd.getSpeed(currentLocation);
			nextNextDirInfo = routingHelper.getNextRouteDirectionInfoAfter(nextDirInfo, new NextDirectionInfo(), true);
			nextTurnType = TripUtils.getNextTurnType(atd, nextNextDirInfo, speed, nextDirInfo.distanceTo);
		}
		stepBuilder.setManeuver(maneuver);
		stepBuilder.setCue(TripUtils.getNextTurnDescription(app, nextDirInfo, turnType, nextTurnType));

		nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, false);
		if (nextDirInfo != null && nextDirInfo.directionInfo != null && nextDirInfo.directionInfo.getTurnType() != null) {
			int[] lanes = nextDirInfo.directionInfo.getTurnType().getLanes();
			int locimminent = nextDirInfo.imminent;
			if (timeDistances == null || timeDistances.getAppMode() != routingHelper.getAppMode()) {
				timeDistances = new AnnounceTimeDistances(routingHelper.getAppMode(), app);
			}
			// Do not show too far
			// (nextTurnDistance != nextDirInfo.distanceTo && nextDirInfo.distanceTo > 150))
			if (nextDirInfo.directionInfo.getTurnType() == null ||
					timeDistances.tooFarToDisplayLanes(nextDirInfo.directionInfo.getTurnType(), nextDirInfo.distanceTo)) {
				lanes = null;
			}
			//int dist = nextDirInfo.distanceTo;
			if (lanes != null) {
				Bitmap lanesBitmap = createLanesBitmap(stepBuilder, lanes, locimminent, leftSide, density);
				stepBuilder.setLanesImage(new CarIcon.Builder(IconCompat.createWithBitmap(lanesBitmap)).build());
			}
		}

		if (deviatedFromRoute) {
			lastCurrentRoad = null;
		} else {
			String streetName = defineStreetName();
			if (!Algorithms.isEmpty(streetName)) {
				stepBuilder.setRoad(streetName);
				tripBuilder.setCurrentRoad(streetName);
			}
			lastCurrentRoad = streetName;
		}

		int leftTurnTimeSec = routingHelper.getLeftTimeNextTurn();
		long turnArrivalTime = System.currentTimeMillis() + leftTurnTimeSec * 1000L;
		Distance stepDistance = TripUtils.getFormattedDistance(app, nextTurnDistance);
		DateTimeWithZone stepDateTime = DateTimeWithZone.create(turnArrivalTime, TimeZone.getDefault());
		TravelEstimate.Builder stepTravelEstimateBuilder = new TravelEstimate.Builder(stepDistance, stepDateTime);
		stepTravelEstimateBuilder.setRemainingTimeSeconds(leftTurnTimeSec >= 0 ? leftTurnTimeSec : REMAINING_TIME_UNKNOWN);
		Step step = stepBuilder.build();
		TravelEstimate stepTravelEstimate = stepTravelEstimateBuilder.build();
		tripBuilder.addStep(step, stepTravelEstimate);

		lastStep = step;
		lastStepTravelEstimate = stepTravelEstimate;

		setupNextNextTurn(tripBuilder, nextNextDirInfo, currentLocation, deviatedFromRoute, leftTurnTimeSec, density);
	}

	private void setupNextNextTurn(@NonNull Builder tripBuilder, @Nullable NextDirectionInfo nextNextDirInfo,
	                               @Nullable Location currentLocation, boolean deviatedFromRoute,
	                               int leftTurnTimeSec, float density) {
		if (nextNextDirInfo != null && nextNextDirInfo.distanceTo > 0
				&& nextNextDirInfo.imminent >= 0 && nextNextDirInfo.directionInfo != null) {
			Step.Builder nextStepBuilder = new Step.Builder();
			Maneuver.Builder nextTurnBuilder;
			TurnType nextTurnType = nextNextDirInfo.directionInfo.getTurnType();
			if (nextTurnType != null) {
				int width = (int) (NEXT_TURN_IMAGE_SIZE_DP * density);
				int height = (int) (NEXT_TURN_IMAGE_SIZE_DP * density);
				Bitmap turnBitmap = createTurnBitmap(nextTurnType, deviatedFromRoute, nextNextDirInfo.imminent, width, height);

				nextTurnBuilder = new Maneuver.Builder(TripUtils.getManeuverType(nextTurnType));
				if (nextTurnType.isRoundAbout()) {
					nextTurnBuilder.setRoundaboutExitNumber(nextTurnType.getExitOut());
				}
				nextTurnBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithBitmap(turnBitmap)).build());
			} else {
				nextTurnBuilder = new Maneuver.Builder(Maneuver.TYPE_UNKNOWN);
			}
			Maneuver nextManeuver = nextTurnBuilder.build();
			nextStepBuilder.setManeuver(nextManeuver);

			int leftNextTurnTimeSec = leftTurnTimeSec + nextNextDirInfo.directionInfo.getExpectedTime();
			long nextTurnArrivalTime = System.currentTimeMillis() + leftNextTurnTimeSec * 1000L;
			Distance nextStepDistance = TripUtils.getFormattedDistance(app, nextNextDirInfo.distanceTo);
			DateTimeWithZone nextStepDateTime = DateTimeWithZone.create(nextTurnArrivalTime, TimeZone.getDefault());
			TravelEstimate.Builder nextStepTravelEstimateBuilder = new TravelEstimate.Builder(nextStepDistance, nextStepDateTime);
			nextStepTravelEstimateBuilder.setRemainingTimeSeconds(leftNextTurnTimeSec >= 0 ? leftNextTurnTimeSec : REMAINING_TIME_UNKNOWN);

			TurnType nextNextTurnType = null;
			AnnounceTimeDistances atd = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
			if (atd != null) {
				float speed = atd.getSpeed(currentLocation);
				NextDirectionInfo info = routingHelper.getNextRouteDirectionInfoAfter(nextNextDirInfo, new NextDirectionInfo(), true);
				nextNextTurnType = TripUtils.getNextTurnType(atd, info, speed, nextNextDirInfo.distanceTo);
			}
			nextStepBuilder.setCue(TripUtils.getSecondNextTurnDescription(app, nextNextDirInfo, nextTurnType, nextNextTurnType));

			Step nextStep = nextStepBuilder.build();
			TravelEstimate nextStepTravelEstimate = nextStepTravelEstimateBuilder.build();
			tripBuilder.addStep(nextStep, nextStepTravelEstimate);
		}
	}

	@NonNull
	private Bitmap createLanesBitmap(@NonNull Step.Builder builder, int[] lanes, int imminent, boolean leftSide, float density) {
		for (int lane : lanes) {
			int firstTurnType = TurnType.getPrimaryTurn(lane);
			int secondTurnType = TurnType.getSecondaryTurn(lane);
			int thirdTurnType = TurnType.getTertiaryTurn(lane);
			Lane.Builder laneBuilder = new Lane.Builder();
			laneBuilder.addDirection(LaneDirection.create(TripUtils.getLaneDirection(TurnType.valueOf(firstTurnType, leftSide)), (lane & 1) == 1));
			if (secondTurnType > 0) {
				laneBuilder.addDirection(LaneDirection.create(TripUtils.getLaneDirection(TurnType.valueOf(secondTurnType, leftSide)), false));
			}
			if (thirdTurnType > 0) {
				laneBuilder.addDirection(LaneDirection.create(TripUtils.getLaneDirection(TurnType.valueOf(thirdTurnType, leftSide)), false));
			}
			builder.addLane(laneBuilder.build());
		}
		LanesDrawable lanesDrawable = new LanesDrawable(app, 1f,
				TURN_LANE_IMAGE_SIZE * density,
				TURN_LANE_IMAGE_MIN_DELTA * density,
				TURN_LANE_IMAGE_MARGIN * density,
				TURN_LANE_IMAGE_SIZE * density);
		lanesDrawable.lanes = lanes;
		lanesDrawable.imminent = imminent == 0;
		lanesDrawable.isNightMode = app.getDaynightHelper().isNightMode();
		lanesDrawable.updateBounds(); // prefer 500 x 74 dp
		return drawableToBitmap(lanesDrawable, lanesDrawable.getIntrinsicWidth(), lanesDrawable.getIntrinsicHeight());
	}

	@NonNull
	private Bitmap createTurnBitmap(@NonNull TurnType turnType, boolean deviatedFromRoute, int imminent, int width, int height) {
		TurnDrawable drawable = new TurnDrawable(app, false);
		drawable.setBounds(0, 0, width, height);
		drawable.setTurnType(turnType);
		drawable.setTurnImminent(imminent, deviatedFromRoute);
		drawable.updateColors(app.getDaynightHelper().isNightMode());
		return drawableToBitmap(drawable, width, height);
	}

	private void updateDestination(@NonNull Builder builder) {
		TargetPointsHelper helper = app.getTargetPointsHelper();
		TargetPoint pointToNavigate = helper.getPointToNavigate();
		if (pointToNavigate != null) {
			Pair<Destination, TravelEstimate> dest = getDestination(pointToNavigate);
			builder.addDestination(dest.first, dest.second);
			lastDestination = dest.first;
			lastDestinationTravelEstimate = dest.second;
		} else {
			lastDestination = null;
			lastDestinationTravelEstimate = null;
		}
	}

	@NonNull
	public Pair<Destination, TravelEstimate> getDestination(@NonNull TargetPoint pointToNavigate) {
		Destination.Builder destBuilder = new Destination.Builder();
		String name = pointToNavigate.getOnlyName();
		if (Algorithms.isEmpty(name)) {
			name = app.getString(R.string.route_descr_destination);
		}
		destBuilder.setName(name);
		destBuilder.setImage(new CarIcon.Builder(IconCompat.createWithResource(app,
				R.drawable.ic_action_point_destination)).build());

		int leftTimeSec = 0;
		int leftDistance = 0;
		if (settings.USE_LEFT_DISTANCE_TO_INTERMEDIATE.get()) {
			leftDistance = routingHelper.getLeftDistanceNextIntermediate();
			leftTimeSec = routingHelper.getLeftTimeNextIntermediate();
		}
		if (leftDistance == 0) {
			leftTimeSec = routingHelper.getLeftTime();
			leftDistance = routingHelper.getLeftDistance();
		}
		Distance distance =  TripUtils.getDistance(app, leftDistance);
		DateTimeWithZone dateTime = DateTimeWithZone.create(System.currentTimeMillis() + leftTimeSec * 1000L, TimeZone.getDefault());
		TravelEstimate.Builder travelEstimateBuilder = new TravelEstimate.Builder(distance, dateTime);
		travelEstimateBuilder.setRemainingTimeSeconds(leftTimeSec >= 0 ? leftTimeSec : REMAINING_TIME_UNKNOWN);
		Destination destination = destBuilder.build();
		TravelEstimate travelEstimate = travelEstimateBuilder.build();
		return new Pair<>(destination, travelEstimate);
	}

	@NonNull
	private Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	@Nullable
	private String defineStreetName() {
		NextDirectionInfo directionInfo = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		return TripUtils.defineStreetName(app, directionInfo);
	}
}
