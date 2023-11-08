package net.osmand.plus.views.controls.maphudbuttons;

import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.MapControlsLayer;

import androidx.annotation.NonNull;

public class ZoomOutButton extends MapButton {

	public ZoomOutButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id) {
		super(mapActivity, view, id);
		setIconId(R.drawable.ic_zoom_out);
		setRoundTransparentBackground();
		setOnClickListener(v -> {
			if (mapActivity.getContextMenu().zoomOutPressed()) {
				return;
			}
			app.getOsmandMap().changeZoom(-1, System.currentTimeMillis());
		});
		setOnLongClickListener(MapControlsLayer.getOnClickMagnifierListener(mapActivity.getMapView()));
		updateIcon(app.getDaynightHelper().isNightModeForMapControls());
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && widgetsVisibilityHelper.shouldShowZoomButtons();
	}
}