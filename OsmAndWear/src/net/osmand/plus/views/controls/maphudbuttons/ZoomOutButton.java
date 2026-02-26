package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.ZoomOutButtonState;

public class ZoomOutButton extends MapButton {

	private final ZoomOutButtonState buttonState;

	public ZoomOutButton(@NonNull Context context) {
		this(context, null);
	}

	public ZoomOutButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomOutButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getZoomOutButtonState();

		setOnClickListener(v -> {
			if (!mapActivity.getContextMenu().zoomOutPressed()) {
				getMapView().zoomOutAndAdjustTiltAngle();
			}
		});
		setOnLongClickListener(v -> {
			ConfigureMapDialogs.showMapMagnifierDialog(getMapView());
			return true;
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected boolean shouldShow() {
		return !routeDialogOpened && visibilityHelper.shouldShowZoomButtons();
	}
}