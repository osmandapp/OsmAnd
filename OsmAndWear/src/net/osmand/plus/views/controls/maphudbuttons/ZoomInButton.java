package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.ZoomInButtonState;

public class ZoomInButton extends MapButton {

	private final ZoomInButtonState buttonState;

	public ZoomInButton(@NonNull Context context) {
		this(context, null);
	}

	public ZoomInButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomInButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getZoomInButtonState();

		setOnClickListener(v -> {
			if (mapActivity.getContextMenu().zoomInPressed()) {
				return;
			}
			getMapView().zoomInAndAdjustTiltAngle();
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