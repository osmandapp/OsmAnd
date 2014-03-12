package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;

public class SmallMapMenuControls extends MapControls {
	private Button backToMenuButton;
	
	
	public SmallMapMenuControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		Context ctx = mapActivity;
		backToMenuButton = new Button(ctx);
		backToMenuButton.setContentDescription(ctx.getString(R.string.backToMenu));
		backToMenuButton.setBackgroundResource(R.drawable.map_btn_hmenu);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		parent.addView(backToMenuButton, params);
		backToMenuButton.setEnabled(true);
		
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapActions().openOptionsMenuAsList();
			}
		});
		mapActivity.accessibleContent.add(backToMenuButton);
	}

	@Override
	public void hideControls(FrameLayout layout) {
		layout.removeView(backToMenuButton);
		mapActivity.accessibleContent.remove(backToMenuButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
}
