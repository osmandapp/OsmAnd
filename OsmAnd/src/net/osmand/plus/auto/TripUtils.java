package net.osmand.plus.auto;

import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_IN;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_TURN_NOW;
import static net.osmand.plus.utils.OsmAndFormatterParams.DEFAULT;
import static net.osmand.plus.utils.OsmAndFormatterParams.USE_LOWER_BOUNDS;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.router.TurnType;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.util.Algorithms;

public class TripUtils {

	public static boolean shouldKeepLeft(@Nullable TurnType type) {
		return type != null && TurnType.isLeftTurn(type.getValue());
	}

	public static boolean shouldKeepRight(@Nullable TurnType type) {
		return type != null && TurnType.isRightTurn(type.getValue());
	}

	@NonNull
	public static String nextTurnsToString(@NonNull Context ctx, @NonNull TurnType type, @Nullable TurnType nextTurnType) {
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
	public static String getFormattedDistanceStr(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		return OsmAndFormatter.getFormattedDistance((float) meters, app, USE_LOWER_BOUNDS, mc);
	}

	@NonNull
	public static Distance getFormattedDistance(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		FormattedValue formattedValue = OsmAndFormatter.getFormattedDistanceValue((float) meters, app, USE_LOWER_BOUNDS, mc);

		return Distance.create(formattedValue.valueSrc, getDistanceUnit(formattedValue.unitId));
	}

	@NonNull
	public static Distance getDistance(@NonNull OsmandApplication app, double meters) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		FormattedValue formattedValue = OsmAndFormatter.getFormattedDistanceValue((float) meters, app, DEFAULT, mc);

		return Distance.create(formattedValue.valueSrc, getDistanceUnit(formattedValue.unitId));
	}

	@Distance.Unit
	public static int getDistanceUnit(@StringRes int unitId) {
		if (unitId == R.string.m) {
			return Distance.UNIT_METERS;
		} else if (unitId == R.string.yard) {
			return Distance.UNIT_YARDS;
		} else if (unitId == R.string.foot) {
			return Distance.UNIT_FEET;
		} else if (unitId == R.string.mile || unitId == R.string.nm) {
			return Distance.UNIT_MILES_P1;
		} else if (unitId == R.string.km) {
			return Distance.UNIT_KILOMETERS_P1;
		}
		return Distance.UNIT_METERS;
	}

	public static int getManeuverType(@NonNull TurnType turnType) {
		return switch (turnType.getValue()) {
			case TurnType.C -> Maneuver.TYPE_STRAIGHT; // continue (go straight)
			case TurnType.TL -> Maneuver.TYPE_TURN_NORMAL_LEFT; // turn left
			case TurnType.TSLL -> Maneuver.TYPE_TURN_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL -> Maneuver.TYPE_TURN_SHARP_LEFT; // turn sharply left
			case TurnType.TR -> Maneuver.TYPE_TURN_NORMAL_RIGHT; // turn right
			case TurnType.TSLR -> Maneuver.TYPE_TURN_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR -> Maneuver.TYPE_TURN_SHARP_RIGHT; // turn sharply right
			case TurnType.KL -> Maneuver.TYPE_KEEP_LEFT; // keep left
			case TurnType.KR -> Maneuver.TYPE_KEEP_RIGHT; // keep right
			case TurnType.TU -> Maneuver.TYPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU -> Maneuver.TYPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR -> Maneuver.TYPE_UNKNOWN; // Off route
			case TurnType.RNDB -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW; // Roundabout
			case TurnType.RNLB -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW; // Roundabout left
			default -> Maneuver.TYPE_UNKNOWN;
		};
	}

	public static int getLaneDirection(@NonNull TurnType turnType) {
		return switch (turnType.getValue()) {
			case TurnType.C -> LaneDirection.SHAPE_STRAIGHT; // continue (go straight)
			case TurnType.TL -> LaneDirection.SHAPE_NORMAL_LEFT; // turn left
			case TurnType.TSLL -> LaneDirection.SHAPE_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL -> LaneDirection.SHAPE_SHARP_LEFT; // turn sharply left
			case TurnType.TR -> LaneDirection.SHAPE_NORMAL_RIGHT; // turn right
			case TurnType.TSLR -> LaneDirection.SHAPE_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR -> LaneDirection.SHAPE_SHARP_RIGHT; // turn sharply right
			case TurnType.KL -> LaneDirection.SHAPE_SLIGHT_LEFT; // keep left
			case TurnType.KR -> LaneDirection.SHAPE_SLIGHT_RIGHT; // keep right
			case TurnType.TU -> LaneDirection.SHAPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU -> LaneDirection.SHAPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR -> LaneDirection.SHAPE_UNKNOWN; // Off route
			case TurnType.RNDB -> LaneDirection.SHAPE_UNKNOWN; // Roundabout
			case TurnType.RNLB -> LaneDirection.SHAPE_UNKNOWN; // Roundabout left
			default -> LaneDirection.SHAPE_UNKNOWN;
		};
	}

	@Nullable
	public static TurnType getNextTurnType(@NonNull AnnounceTimeDistances atd, @Nullable NextDirectionInfo info, float speed, int distance) {
		if (atd.isTurnStateActive(speed, distance, STATE_TURN_IN)) {
			if (info != null && info.directionInfo != null &&
					(atd.isTurnStateActive(speed, info.distanceTo, STATE_TURN_NOW)
							|| !atd.isTurnStateNotPassed(speed, info.distanceTo, STATE_TURN_IN))) {
				return info.directionInfo.getTurnType();
			}
		}
		return null;
	}

	@NonNull
	public static String getNextTurnDescription(@NonNull OsmandApplication app, @Nullable NextDirectionInfo info,
	                                            @Nullable TurnType type, @Nullable TurnType nextTurnType) {
		String description = getTurnDescription(app, info);
		String turnName = type != null ? nextTurnsToString(app, type, nextTurnType) : "";

		if (type != null && type.isRoundAbout() && !Algorithms.isEmpty(description)) {
			return app.getString(R.string.ltr_or_rtl_combine_via_comma, turnName, description);
		}
		return !Algorithms.isEmpty(description) ? description : turnName;
	}

	@NonNull
	public static String getSecondNextTurnDescription(@NonNull OsmandApplication app, @NonNull NextDirectionInfo info,
	                                                  @Nullable TurnType turnType, @Nullable TurnType nextTurnType) {
		String description = getTurnDescription(app, info);
		String distance = getFormattedDistanceStr(app, info.distanceTo);

		if (Algorithms.isEmpty(description)) {
			description = turnType != null ? nextTurnsToString(app, turnType, nextTurnType) : null;
		}
		return Algorithms.isEmpty(description) ? distance : app.getString(R.string.ltr_or_rtl_combine_via_comma, distance, description);
	}

	@Nullable
	public static String getTurnDescription(@NonNull OsmandApplication app, @Nullable NextDirectionInfo info) {
		String name = defineStreetName(app, info);
		String ref = info != null && info.directionInfo != null ? info.directionInfo.getRef() : null;
		return !Algorithms.isEmpty(name) ? name : ref;
	}

	@Nullable
	public static String defineStreetName(@NonNull OsmandApplication app, @Nullable NextDirectionInfo info) {
		if (info != null) {
			CurrentStreetName streetName = new CurrentStreetName(info);

			String name = streetName.text;
			if (!Algorithms.isEmpty(name)) {
				String ref = streetName.exitRef;
				return Algorithms.isEmpty(ref) ? name : app.getString(R.string.ltr_or_rtl_combine_via_comma, ref, name);
			}
		}
		return null;
	}

	@NonNull
	public static CurrentStreetName getStreetName(@NonNull OsmandApplication app, @NonNull NextDirectionInfo info, @NonNull RouteDirectionInfo routeInfo) {
		return new CurrentStreetName(info, true);
	}
}
