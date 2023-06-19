package net.osmand.plus.views.controls.maphudbuttons;

import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.layers.MapControlsLayer;

import androidx.annotation.NonNull;

public class ZoomInButton extends MapButton {

	public ZoomInButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id) {
		super(mapActivity, view, id);
		setIconId(R.drawable.ic_zoom_in);
		setRoundTransparentBackground();
		setOnClickListener(v -> {
			if (mapActivity.getContextMenu().zoomInPressed()) {
				return;
			}
			app.getOsmandMap().changeZoom(1, System.currentTimeMillis());
		});
		setOnLongClickListener(MapControlsLayer.getOnClickMagnifierListener(mapActivity.getMapView()));
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && widgetsVisibilityHelper.shouldShowZoomButtons();
	}
}