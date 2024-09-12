package net.osmand.plus.auto;

import static androidx.car.app.navigation.model.TravelEstimate.REMAINING_TIME_UNKNOWN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_IN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_NOW;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
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
	private final RoutingHelper routingHelper;

	private Destination lastDestination;
	private TravelEstimate lastDestinationTravelEstimate;
	private Step lastStep;
	private TravelEstimate lastStepTravelEstimate;
	private CharSequence lastCurrentRoad;

	public TripHelper(@NonNull OsmandApplication app) {
		this.app = app;
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
		AnnounceTimeDistances atd = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
		if (nextDirInfo != null && atd != null) {
			float speed = atd.getSpeed(currentLocation);
			int dist = nextDirInfo.distanceTo;
			nextNextDirInfo = routingHelper.getNextRouteDirectionInfoAfter(nextDirInfo, new NextDirectionInfo(), true);
			if (atd.isTurnStateActive(speed, dist, STATE_TURN_IN)) {
				if (nextNextDirInfo != null && nextNextDirInfo.directionInfo != null &&
						(atd.isTurnStateActive(speed, nextNextDirInfo.distanceTo, STATE_TURN_NOW)
								|| !atd.isTurnStateNotPassed(speed, nextNextDirInfo.distanceTo, STATE_TURN_IN))) {
					nextTurnType = nextNextDirInfo.directionInfo.getTurnType();
				}
			}
		}
		stepBuilder.setManeuver(maneuver);
		stepBuilder.setCue(getNextCue(nextDirInfo, turnType, nextTurnType));

		nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, false);
		if (nextDirInfo != null && nextDirInfo.directionInfo != null && nextDirInfo.directionInfo.getTurnType() != null) {
			int[] lanes = nextDirInfo.directionInfo.getTurnType().getLanes();
			int locimminent = nextDirInfo.imminent;
			// Do not show too far
			if ((nextDirInfo.distanceTo > 800 && nextDirInfo.directionInfo.getTurnType().isSkipToSpeak())
					|| nextDirInfo.distanceTo > 1200
					|| (nextTurnDistance != nextDirInfo.distanceTo && nextDirInfo.distanceTo > 150)) {
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
		Distance stepDistance = getFormattedDistance(app, nextTurnDistance);
		DateTimeWithZone stepDateTime = DateTimeWithZone.create(turnArrivalTime, TimeZone.getDefault());
		TravelEstimate.Builder stepTravelEstimateBuilder = new TravelEstimate.Builder(stepDistance, stepDateTime);
		stepTravelEstimateBuilder.setRemainingTimeSeconds(leftTurnTimeSec >= 0 ? leftTurnTimeSec : REMAINING_TIME_UNKNOWN);
		Step step = stepBuilder.build();
		TravelEstimate stepTravelEstimate = stepTravelEstimateBuilder.build();
		tripBuilder.addStep(step, stepTravelEstimate);

		lastStep = step;
		lastStepTravelEstimate = stepTravelEstimate;

		// Next next turn
		if (nextNextDirInfo != null && nextNextDirInfo.distanceTo > 0 && nextNextDirInfo.imminent >= 0 && nextNextDirInfo.directionInfo != null) {
			Step.Builder nextStepBuilder = new Step.Builder();
			Maneuver.Builder nextTurnBuilder;
			nextTurnType = nextNextDirInfo.directionInfo.getTurnType();
			if (nextTurnType != null) {
				TurnDrawable drawable = new TurnDrawable(app, false);
				int height = (int) (NEXT_TURN_IMAGE_SIZE_DP * density);
				int width = (int) (NEXT_TURN_IMAGE_SIZE_DP * density);
				drawable.setBounds(0, 0, width, height);
				drawable.setTurnType(nextTurnType);
				drawable.setTurnImminent(nextNextDirInfo.imminent, deviatedFromRoute);
				Bitmap turnBitmap = drawableToBitmap(drawable, width, height);
				nextTurnBuilder = new Maneuver.Builder(getManeuverType(nextTurnType));
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
			Distance nextStepDistance = getFormattedDistance(app, nextNextDirInfo.distanceTo);
			DateTimeWithZone nextStepDateTime = DateTimeWithZone.create(nextTurnArrivalTime, TimeZone.getDefault());
			TravelEstimate.Builder nextStepTravelEstimateBuilder = new TravelEstimate.Builder(nextStepDistance, nextStepDateTime);
			nextStepTravelEstimateBuilder.setRemainingTimeSeconds(leftNextTurnTimeSec >= 0 ? leftNextTurnTimeSec : REMAINING_TIME_UNKNOWN);

			nextStepBuilder.setCue(getNextNextCue(nextNextDirInfo));

			Step nextStep = nextStepBuilder.build();
			TravelEstimate nextStepTravelEstimate = nextStepTravelEstimateBuilder.build();
			tripBuilder.addStep(nextStep, nextStepTravelEstimate);
		}
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

	private boolean shouldKeepLeft(@Nullable TurnType t) {
		return t != null && (t.getValue() == TurnType.TL || t.getValue() == TurnType.TSHL
				|| t.getValue() == TurnType.TSLL || t.getValue() == TurnType.TU || t.getValue() == TurnType.KL);
	}

	private boolean shouldKeepRight(@Nullable TurnType t) {
		return t != null && (t.getValue() == TurnType.TR || t.getValue() == TurnType.TSHR
				|| t.getValue() == TurnType.TSLR || t.getValue() == TurnType.TRU || t.getValue() == TurnType.KR);
	}

	@NonNull
	private String getNextCue(@Nullable NextDirectionInfo info, @Nullable TurnType type, @Nullable TurnType nextTurnType) {
		String cue = getCue(info);
		String turnName = type != null ? nextTurnsToString(app, type, nextTurnType) : "";

		if (type != null && type.isRoundAbout() && !Algorithms.isEmpty(cue)) {
			return app.getString(R.string.ltr_or_rtl_combine_via_comma, turnName, cue);
		}
		return !Algorithms.isEmpty(cue) ? cue : turnName;
	}

	@NonNull
	private String getNextNextCue(@NonNull NextDirectionInfo directionInfo) {
		String cue = getCue(directionInfo);
		String distance = getFormattedDistanceStr(app, directionInfo.distanceTo);

		return Algorithms.isEmpty(cue) ? distance : app.getString(R.string.ltr_or_rtl_combine_via_comma, distance, cue);
	}

	@Nullable
	private String getCue(@Nullable NextDirectionInfo info) {
		String name = defineStreetName(info);
		String ref = info != null && info.directionInfo != null ? info.directionInfo.getRef() : null;
		return !Algorithms.isEmpty(name) ? name : ref;
	}

	private String nextTurnsToString(@NonNull Context ctx, @NonNull TurnType type, @Nullable TurnType nextTurnType) {
		if (type.isRoundAbout()) {
			if (shouldKeepLeft(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_roundabout_kl, type.getExitOut());
			} else if (shouldKeepRight(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_roundabout_kr, type.getExitOut());
			} else {
				return ctx.getString(R.string.route_roundabout_exit, type.getExitOut());
			}
		} else if (type.getValue() == TurnType.TU || type.getValue() == TurnType.TRU) {
			if (shouldKeepLeft(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tu_kl);
			} else if (shouldKeepRight(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tu_kr);
			} else {
				return ctx.getString(R.string.auto_25_chars_route_tu);
			}
		} else if (type.getValue() == TurnType.C) {
			return ctx.getString(R.string.route_head);
		} else if (type.getValue() == TurnType.TSLL) {
			return ctx.getString(R.string.auto_25_chars_route_tsll);
		} else if (type.getValue() == TurnType.TL) {
			if (shouldKeepLeft(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tl_kl);
			} else if (shouldKeepRight(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tl_kr);
			} else {
				return ctx.getString(R.string.auto_25_chars_route_tl);
			}
		} else if (type.getValue() == TurnType.TSHL) {
			return ctx.getString(R.string.auto_25_chars_route_tshl);
		} else if (type.getValue() == TurnType.TSLR) {
			return ctx.getString(R.string.auto_25_chars_route_tslr);
		} else if (type.getValue() == TurnType.TR) {
			if (shouldKeepLeft(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tr_kl);
			} else if (shouldKeepRight(nextTurnType)) {
				return ctx.getString(R.string.auto_25_chars_route_tr_kr);
			} else {
				return ctx.getString(R.string.auto_25_chars_route_tr);
			}
		} else if (type.getValue() == TurnType.TSHR) {
			return ctx.getString(R.string.auto_25_chars_route_tshr);
		} else if (type.getValue() == TurnType.KL) {
			return ctx.getString(R.string.auto_25_chars_route_kl);
		} else if (type.getValue() == TurnType.KR) {
			return ctx.getString(R.string.auto_25_chars_route_kr);
		}
		return "";
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

		Distance distance = getDistance(app, routingHelper.getLeftDistance());
		int leftTimeSec = routingHelper.getLeftTime();
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

	private static String getFormattedDistanceStr(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		return OsmAndFormatter.getFormattedDistance((float) meters, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS, mc);
	}

	private static Distance getFormattedDistance(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		OsmAndFormatter.FormattedValue formattedValue = OsmAndFormatter.getFormattedDistanceValue((float) meters, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS, mc);

		return Distance.create(formattedValue.valueSrc, getDistanceUnit(formattedValue.unitId));
	}

	public static Distance getDistance(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		OsmAndFormatter.FormattedValue formattedValue = OsmAndFormatter.getFormattedDistanceValue((float) meters, app, OsmAndFormatter.OsmAndFormatterParams.DEFAULT, mc);

		return Distance.create(formattedValue.valueSrc, getDistanceUnit(formattedValue.unitId));
	}

	@Distance.Unit
	private static int getDistanceUnit(@StringRes int unitId) {
		if (unitId == R.string.m) {
			return Distance.UNIT_METERS;
		} else if (unitId == R.string.yard) {
			return Distance.UNIT_YARDS;
		} else if (unitId == R.string.foot) {
			return Distance.UNIT_FEET;
		} else if (unitId == R.string.mile || unitId == R.string.nm) {
			return Distance.UNIT_MILES;
		} else if (unitId == R.string.km) {
			return Distance.UNIT_KILOMETERS;
		}
		return Distance.UNIT_METERS;
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

	@Nullable
	private String defineStreetName() {
		NextDirectionInfo directionInfo = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		return defineStreetName(directionInfo);
	}

	@Nullable
	private String defineStreetName(@Nullable NextDirectionInfo nextDirInfo) {
		if (nextDirInfo != null) {
			CurrentStreetName currentStreetName = routingHelper.getCurrentName(nextDirInfo);
			String streetName = currentStreetName.text;

			if (!Algorithms.isEmpty(streetName)) {
				String exitRef = currentStreetName.exitRef;
				return Algorithms.isEmpty(exitRef)
						? streetName
						: app.getString(R.string.ltr_or_rtl_combine_via_comma, exitRef, streetName);
			}
		}
		return null;
	}
}
