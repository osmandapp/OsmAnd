package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONFIGURE_MAP;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.ConfigureMapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class ConfigureMapButton extends MapButton {

	private final ConfigureMapButtonState buttonState;

	public ConfigureMapButton(@NonNull Context context) {
		this(context, null);
	}

	public ConfigureMapButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ConfigureMapButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getConfigureMapButtonState();

		setOnClickListener(v -> {
			MapActivity.clearPrevActivityIntent();
			mapActivity.getDashboard().setDashboardVisibility(true, CONFIGURE_MAP, AndroidUtils.getCenterViewCoordinates(v));
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected void updateColors(boolean nightMode) {
		setIconColor(settings.getApplicationMode().getProfileColor(nightMode));
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	@Override
	protected boolean shouldShow() {
		return !routeDialogOpened && visibilityHelper.shouldShowTopButtons();
	}
}