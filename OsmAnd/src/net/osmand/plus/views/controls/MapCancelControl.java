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

public class MapCancelControl extends MapControls {
	private Button cancelButton;
	
	
	public MapCancelControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		cancelButton = addButton(parent, R.string.cancel_navigation, R.drawable.map_btn_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				if(mapActivity.getRoutingHelper().isFollowingMode()) {
					mapActivity.getMapActions().stopNavigationActionConfirm(mapActivity.getMapView());
				} else {
					mapActivity.getMapActions().stopNavigationWithoutConfirm();
				}
			}
		});
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, cancelButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
	
	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_cancel);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
}
