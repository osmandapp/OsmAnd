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
		
		backToMenuButton = addButton(parent, R.string.backToMenu, R.drawable.map_btn_hmenu);
		/*
		Context ctx = mapActivity;
		backToMenuButton = new ImageButton(ctx);
		backToMenuButton.setContentDescription(ctx.getString(R.string.backToMenu));
		backToMenuButton.setImageResource(R.drawable.map_btn_hmenu);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		parent.addView(backToMenuButton, params);
		backToMenuButton.setEnabled(true);
		
		
		mapActivity.accessibleContent.add(backToMenuButton);*/
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_hmenu);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
}
