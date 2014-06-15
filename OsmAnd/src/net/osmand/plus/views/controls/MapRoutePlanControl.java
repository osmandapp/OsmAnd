package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapRoutePlanControl extends MapControls {
	private Button routePlanButton;
	
	
	public MapRoutePlanControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		routePlanButton = addButton(parent, R.string.info_button, R.drawable.map_btn_info);
		routePlanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapActivity.getRoutingHelper().setRoutePlanningMode(true);
				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				mapActivity.refreshMap();
			}
		});
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, routePlanButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
	
	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_info);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
}
