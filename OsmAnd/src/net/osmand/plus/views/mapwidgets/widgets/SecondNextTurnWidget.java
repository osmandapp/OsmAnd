package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.SECOND_NEXT_TURN;

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

public class SecondNextTurnWidget extends NextTurnBaseWidget {

	private final NextDirectionInfo nextDirectionInfo = new NextDirectionInfo();

	public SecondNextTurnWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, customId, SECOND_NEXT_TURN, panel, true);
	}

	/**
	 * Do not delete to have pressed state. Uncomment to test rendering
	 */
	@NonNull
	protected OnClickListener getOnClickListener() {
		return new View.OnClickListener() {
//			int i = 0;
			@Override
			public void onClick(View v) {
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), true);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//					float a = 180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], true);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nexsweepAngletTurnInfo.turnType,nextTurnInfo.pathTransform);
//				showMiniMap = true;
			}
		};
	}

	@Override
	public void updateNavigationInfo(@Nullable DrawSettings drawSettings) {
		boolean followingMode = routingHelper.isFollowingMode() || locationProvider.getLocationSimulation().isRouteAnimating();
		StreetNameWidget.StreetNameWidgetParams params = new StreetNameWidget.StreetNameWidgetParams(mapActivity, true);
		CurrentStreetName streetName = params.streetName;
		TurnType turnType = null;
		boolean deviatedFromRoute = false;
		int turnImminent = 0;
		int nextTurnDistance = 0;
		if (routingHelper.isRouteCalculated() && followingMode) {
			deviatedFromRoute = routingHelper.isDeviatedFromRoute();
			NextDirectionInfo info = routingHelper.getNextRouteDirectionInfo(nextDirectionInfo, true);
			if (!deviatedFromRoute) {
				if (info != null) {
					info = routingHelper.getNextRouteDirectionInfoAfter(info, nextDirectionInfo, true);
				}
			}
			if (info != null && info.distanceTo > 0 && info.directionInfo != null) {
				streetName = TripUtils.getStreetName(info);
				if (verticalWidget && Algorithms.isEmpty(streetName.text)) {
					streetName.text = info.directionInfo.getDescriptionRoutePart(true);
				}
				turnType = info.directionInfo.getTurnType();
				turnImminent = info.imminent;
				nextTurnDistance = info.distanceTo;
			}
		}
		setStreetName(streetName);
		setTurnType(turnType);
		setTurnImminent(turnImminent, deviatedFromRoute);
		setTurnDistance(nextTurnDistance);
	}
}