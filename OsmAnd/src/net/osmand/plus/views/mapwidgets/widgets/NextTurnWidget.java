package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.SMALL_NEXT_TURN;

import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.TripUtils;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

public class NextTurnWidget extends NextTurnBaseWidget {

	private final NextDirectionInfo nextDirectionInfo = new NextDirectionInfo();

	public NextTurnWidget(@NonNull MapActivity mapActivity, @Nullable String customId,
			@Nullable WidgetsPanel panel, boolean horizontalMini) {
		super(mapActivity, customId, horizontalMini ? SMALL_NEXT_TURN : NEXT_TURN, panel, horizontalMini);
	}

	/**
	 * Uncomment to test rendering
	 */
	@NonNull
	protected OnClickListener getOnClickListener() {
		return new View.OnClickListener() {
//			int i = 0;
//			boolean leftSide = false;
			@Override
			public void onClick(View v) {
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), leftSide);
//					float a = leftSide?  -180 + (i % (l + exits) - l + 1) * 50:  180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], leftSide);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nextTurnInfo.turnType,nextTurnInfo.pathTransform);
				if (routingHelper.isOnRoute()) {
					routingHelper.getVoiceRouter().announceCurrentDirection(null);
				}
			}
		};
	}

	@Override
	public void updateNavigationInfo(@Nullable DrawSettings drawSettings) {
		boolean followingMode = routingHelper.isFollowingMode()
				|| locationProvider.getLocationSimulation().isRouteAnimating();
		StreetNameWidget.StreetNameWidgetParams params = new StreetNameWidget.StreetNameWidgetParams(mapActivity, true);
		CurrentStreetName streetName = params.streetName;
		TurnType turnType = null;
		boolean deviatedFromRoute = false;
		int turnImminent = 0;
		int nextTurnDistance = 0;
		if (routingHelper.isRouteCalculated() && followingMode) {
			deviatedFromRoute = routingHelper.isDeviatedFromRoute();

			if (deviatedFromRoute) {
				turnType = TurnType.valueOf(TurnType.OFFR, settings.DRIVING_REGION.get().leftHandDriving);
				setDeviatePath((int) routingHelper.getRouteDeviation());
			} else {
				NextDirectionInfo info = routingHelper.getNextRouteDirectionInfo(nextDirectionInfo, true);
				if (info != null && info.distanceTo >= 0 && info.directionInfo != null) {
					streetName = TripUtils.getStreetName(info);
					if (verticalWidget && Algorithms.isEmpty(streetName.text)) {
						streetName.text = info.directionInfo.getDescriptionRoutePart(true);
					}
					turnType = info.directionInfo.getTurnType();
					nextTurnDistance = info.distanceTo;
					turnImminent = info.imminent;
				}
			}
		}
		setStreetName(streetName);
		setTurnType(turnType);
		setTurnImminent(turnImminent, deviatedFromRoute);
		setTurnDistance(nextTurnDistance);
	}
}