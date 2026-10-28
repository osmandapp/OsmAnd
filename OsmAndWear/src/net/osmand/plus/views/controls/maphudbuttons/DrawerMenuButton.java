package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.DASHBOARD;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.configure.buttons.DrawerMenuButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class DrawerMenuButton extends MapButton {

	private final DrawerMenuButtonState buttonState;

	public DrawerMenuButton(@NonNull Context context) {
		this(context, null);
	}

	public DrawerMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DrawerMenuButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getDrawerMenuButtonState();

		setOnClickListener(v -> {
			MapActivity.clearPrevActivityIntent();
			if (settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get()) {
				mapActivity.getDashboard().setDashboardVisibility(true, DASHBOARD, AndroidUtils.getCenterViewCoordinates(v));
			} else {
				mapActivity.openDrawer();
			}
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected boolean shouldShow() {
		return showBottomButtons && mapActivity.isDrawerAvailable();
	}
}