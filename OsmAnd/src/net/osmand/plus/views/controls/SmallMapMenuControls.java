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

public class SmallMapMenuControls extends MapControls {
	private Button backToMenuButton;
	
	public SmallMapMenuControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		backToMenuButton = addButton(parent, R.string.backToMenu, R.drawable.map_btn_menu);
		mapActivity.accessibleContent.add(backToMenuButton);
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapActivity.getMapActions().openOptionsMenuAsList();
			}
		});
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, backToMenuButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}

	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_menu);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
}
