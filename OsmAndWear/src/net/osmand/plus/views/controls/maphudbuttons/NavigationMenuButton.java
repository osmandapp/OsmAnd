package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapActionsHelper;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.NavigationMenuButtonState;

public class NavigationMenuButton extends MapButton {

	private final RoutingHelper routingHelper;
	private final NavigationMenuButtonState buttonState;

	public NavigationMenuButton(@NonNull Context context) {
		this(context, null);
	}

	public NavigationMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NavigationMenuButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		routingHelper = app.getRoutingHelper();
		buttonState = app.getMapButtonsHelper().getNavigationMenuButtonState();

		setOnClickListener(v -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			MapActionsHelper controlsHelper = app.getOsmandMap().getMapLayers().getMapActionsHelper();
			if (controlsHelper != null) {
				controlsHelper.doRoute();
			}
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected void updateColors(boolean nightMode) {
		if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()
				|| routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated()
				|| MapActivity.getMapRouteInfoMenu().getRoutePlanningBtnImage() != 0) {
			setIconColor(ColorUtilities.getColor(app, R.color.color_myloc_distance));
		} else {
			setIconColor(ColorUtilities.getMapButtonIconColor(app, nightMode));
		}
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	@Override
	protected boolean shouldShow() {
		return showBottomButtons;
	}
}