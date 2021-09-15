package net.osmand.plus.auto;

import static net.osmand.plus.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.OsmAndFormatter.YARDS_IN_ONE_METER;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.router.TurnType;

import java.util.TimeZone;

public class TripHelper {

	public static final float TURN_IMAGE_SIZE_DP = 128f;
	public static final float TURN_LANE_IMAGE_SIZE = 64f;
	public static final float TURN_LANE_IMAGE_MIN_DELTA = 36f;
	public static final float TURN_LANE_IMAGE_MARGIN = 4f;

	private final OsmandApplication app;

	private Destination lastDestination;
	private TravelEstimate lastDestinationTravelEstimate;
	private Step lastStep;
	private TravelEstimate lastStepTravelEstimate;
	private CharSequence lastCurrentRoad;

	public TripHelper(@NonNull OsmandApplication app) {
		this.app = app;
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
	public Trip buildTrip(float density) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		OsmandSettings settings = app.getSettings();

		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPoint pointToNavigate = targetPointsHelper.getPointToNavigate();
		Trip.Builder tripBuilder = new Trip.Builder();
		if (pointToNavigate != null) {
			Pair<Destination, TravelEstimate> dest = getDestination(pointToNavigate);
			tripBuilder.addDestination(dest.first, dest.second);
			lastDestination = dest.first;
			lastDestinationTravelEstimate = dest.second;
		} else {
			lastDestination = null;
			lastDestinationTravelEstimate = null;
		}
		boolean routeBeingCalculated = routingHelper.isRouteBeingCalculated();
		tripBuilder.setLoading(routeBeingCalculated);
		if (!routeBeingCalculated) {
			Step.Builder stepBuilder = new Step.Builder();
			Maneuver.Builder turnBuilder;
			TurnType turnType = null;
			boolean leftSide = settings.DRIVING_REGION.get().leftHandDriving;
			boolean deviatedFromRoute = routingHelper.isDeviatedFromRoute();
			int turnImminent = 0;
			int nextTurnDistance = 0;
			RouteCalculationResult.NextDirectionInfo nextDirInfo;
			RouteCalculationResult.NextDirectionInfo calc = new RouteCalculationResult.NextDirectionInfo();
			if (deviatedFromRoute) {
				turnType = TurnType.valueOf(TurnType.OFFR, leftSide);
				nextTurnDistance = (int) routingHelper.getRouteDeviation();
			} else {
				nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, true);
				if (nextDirInfo != null && nextDirInfo.distanceTo > 0 && nextDirInfo.directionInfo != null) {
					turnType = nextDirInfo.directionInfo.getTurnType();
					nextTurnDistance = nextDirInfo.distanceTo;
					turnImminent = nextDirInfo.imminent;
				}
			}
			if (turnType != null) {
				TurnDrawable drawable = new TurnDrawable(app, false);
				int height = (int) (TURN_IMAGE_SIZE_DP * density);
				int width = (int) (TURN_IMAGE_SIZE_DP * density);
				drawable.setBounds(0, 0, width, height);
				drawable.setTurnType(turnType);
				drawable.setTurnImminent(turnImminent, deviatedFromRoute);
				Bitmap turnBitmap = drawableToBitmap(drawable, width, height);
				turnBuilder = new Maneuver.Builder(getManeuverType(turnType));
				if (turnType.isRoundAbout()) {
					turnBuilder.setRoundaboutExitNumber(turnType.getExitOut());
				}
				turnBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithBitmap(turnBitmap)).build());
			} else {
				turnBuilder = new Maneuver.Builder(Maneuver.TYPE_UNKNOWN);
			}
			Maneuver maneuver = turnBuilder.build();
			String cue = turnType != null ? RouteCalculationResult.toString(turnType, app, true) : "";
			stepBuilder.setManeuver(maneuver);
			stepBuilder.setCue(cue);

			nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, false);
			if (nextDirInfo != null && nextDirInfo.directionInfo != null && nextDirInfo.directionInfo.getTurnType() != null) {
				int[] lanes = nextDirInfo.directionInfo.getTurnType().getLanes();
				int locimminent = nextDirInfo.imminent;
				// Do not show too far
				if ((nextDirInfo.distanceTo > 800 && nextDirInfo.directionInfo.getTurnType().isSkipToSpeak()) || nextDirInfo.distanceTo > 1200) {
					lanes = null;
				}
				//int dist = nextDirInfo.distanceTo;
				if (lanes != null) {
					for (int lane : lanes) {
						int firstTurnType = TurnType.getPrimaryTurn(lane);
						int secondTurnType = TurnType.getSecondaryTurn(lane);
						int thirdTurnType = TurnType.getTertiaryTurn(lane);
						Lane.Builder laneBuilder = new Lane.Builder();
						laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(firstTurnType, leftSide)), (lane & 1) == 1));
						if (secondTurnType > 0) {
							laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(secondTurnType, leftSide)), false));
						}
						if (thirdTurnType > 0) {
							laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(thirdTurnType, leftSide)), false));
						}
						stepBuilder.addLane(laneBuilder.build());
					}
					LanesDrawable lanesDrawable = new LanesDrawable(app, 1f,
							TURN_LANE_IMAGE_SIZE * density,
							TURN_LANE_IMAGE_MIN_DELTA * density,
							TURN_LANE_IMAGE_MARGIN * density,
							TURN_LANE_IMAGE_SIZE * density);
					lanesDrawable.lanes = lanes;
					lanesDrawable.imminent = locimminent == 0;
					lanesDrawable.isNightMode = app.getDaynightHelper().isNightMode();
					lanesDrawable.updateBounds(); // prefer 500 x 74 dp
					Bitmap lanesBitmap = drawableToBitmap(lanesDrawable, lanesDrawable.getIntrinsicWidth(), lanesDrawable.getIntrinsicHeight());
					stepBuilder.setLanesImage(new CarIcon.Builder(IconCompat.createWithBitmap(lanesBitmap)).build());
				}
			}

			int leftTurnTimeSec = routingHelper.getLeftTimeNextTurn();
			long turnArrivalTime = System.currentTimeMillis() + leftTurnTimeSec * 1000L;
			Distance stepDistance = getDistance(nextTurnDistance);
			DateTimeWithZone stepDateTime = DateTimeWithZone.create(turnArrivalTime, TimeZone.getDefault());
			TravelEstimate.Builder stepTravelEstimateBuilder = new TravelEstimate.Builder(stepDistance, stepDateTime);
			stepTravelEstimateBuilder.setRemainingTimeSeconds(leftTurnTimeSec);
			Step step = stepBuilder.build();
			TravelEstimate stepTravelEstimate = stepTravelEstimateBuilder.build();
			tripBuilder.addStep(step, stepTravelEstimate);
			lastStep = step;
			lastStepTravelEstimate = stepTravelEstimate;
			if (!deviatedFromRoute) {
				nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, true);
				CurrentStreetName currentName = routingHelper.getCurrentName(nextDirInfo);
				tripBuilder.setCurrentRoad(currentName.text);
				lastCurrentRoad = currentName.text;
			} else {
				lastCurrentRoad = null;
			}
		} else {
			lastStep = null;
			lastStepTravelEstimate = null;
		}
		return tripBuilder.build();
	}

	@NonNull
	public Pair<Destination, TravelEstimate> getDestination(@NonNull TargetPoint pointToNavigate) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		Destination.Builder destBuilder = new Destination.Builder();
		destBuilder.setName(pointToNavigate.getOnlyName());
		destBuilder.setImage(new CarIcon.Builder(IconCompat.createWithResource(app,
				R.drawable.ic_action_point_destination)).build());

		Distance distance = getDistance(routingHelper.getLeftDistance());
		int leftTimeSec = routingHelper.getLeftTime();
		DateTimeWithZone dateTime = DateTimeWithZone.create(System.currentTimeMillis() + leftTimeSec * 1000L, TimeZone.getDefault());
		TravelEstimate.Builder travelEstimateBuilder = new TravelEstimate.Builder(distance, dateTime);
		travelEstimateBuilder.setRemainingTimeSeconds(leftTimeSec);
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

	private Distance getDistance(double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		int displayUnit;
		float mainUnitInMeters;
		if (mc == net.osmand.plus.helpers.enums.MetricsConstants.KILOMETERS_AND_METERS) {
			displayUnit = Distance.UNIT_KILOMETERS;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.NAUTICAL_MILES) {
			displayUnit = Distance.UNIT_MILES;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			displayUnit = Distance.UNIT_MILES;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}
		if (meters >= 100 * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters + 0.5, displayUnit);
		} else if (meters > 9.99f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else if (meters > 0.999f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {
			return Distance.create(meters / mainUnitInMeters, displayUnit);
		} else {
			if (mc == net.osmand.plus.helpers.enums.MetricsConstants.KILOMETERS_AND_METERS || mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_METERS) {
				return Distance.create(meters + 0.5,  Distance.UNIT_METERS);
			} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_FEET) {
				return Distance.create(meters * FEET_IN_ONE_METER + 0.5, Distance.UNIT_FEET);
			} else if (mc == net.osmand.plus.helpers.enums.MetricsConstants.MILES_AND_YARDS) {
				return Distance.create(meters * YARDS_IN_ONE_METER + 0.5, Distance.UNIT_YARDS);
			}
			return Distance.create(meters + 0.5,  Distance.UNIT_METERS);
		}
	}

	private int getManeuverType(@NonNull TurnType turnType) {
		switch (turnType.getValue()) {
			case TurnType.C:
				return Maneuver.TYPE_STRAIGHT; // continue (go straight)
			case TurnType.TL:
				return Maneuver.TYPE_TURN_NORMAL_LEFT; // turn left
			case TurnType.TSLL:
				return Maneuver.TYPE_TURN_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL:
				return Maneuver.TYPE_TURN_SHARP_LEFT; // turn sharply left
			case TurnType.TR:
				return Maneuver.TYPE_TURN_NORMAL_RIGHT; // turn right
			case TurnType.TSLR:
				return Maneuver.TYPE_TURN_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR:
				return Maneuver.TYPE_TURN_SHARP_RIGHT; // turn sharply right
			case TurnType.KL:
				return Maneuver.TYPE_KEEP_LEFT; // keep left
			case TurnType.KR:
				return Maneuver.TYPE_KEEP_RIGHT; // keep right
			case TurnType.TU:
				return Maneuver.TYPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU:
				return Maneuver.TYPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR:
				return Maneuver.TYPE_UNKNOWN; // Off route
			case TurnType.RNDB:
				return Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW; // Roundabout
			case TurnType.RNLB:
				return Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW; // Roundabout left
			default:
				return Maneuver.TYPE_UNKNOWN;
		}
	}

	private int getLaneDirection(@NonNull TurnType turnType) {
		switch (turnType.getValue()) {
			case TurnType.C:
				return LaneDirection.SHAPE_STRAIGHT; // continue (go straight)
			case TurnType.TL:
				return LaneDirection.SHAPE_NORMAL_LEFT; // turn left
			case TurnType.TSLL:
				return LaneDirection.SHAPE_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL:
				return LaneDirection.SHAPE_SHARP_LEFT; // turn sharply left
			case TurnType.TR:
				return LaneDirection.SHAPE_NORMAL_RIGHT; // turn right
			case TurnType.TSLR:
				return LaneDirection.SHAPE_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR:
				return LaneDirection.SHAPE_SHARP_RIGHT; // turn sharply right
			case TurnType.KL:
				return LaneDirection.SHAPE_SLIGHT_LEFT; // keep left
			case TurnType.KR:
				return LaneDirection.SHAPE_SLIGHT_RIGHT; // keep right
			case TurnType.TU:
				return LaneDirection.SHAPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU:
				return LaneDirection.SHAPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR:
				return LaneDirection.SHAPE_UNKNOWN; // Off route
			case TurnType.RNDB:
				return LaneDirection.SHAPE_UNKNOWN; // Roundabout
			case TurnType.RNLB:
				return LaneDirection.SHAPE_UNKNOWN; // Roundabout left
			default:
				return LaneDirection.SHAPE_UNKNOWN;
		}
	}
}
