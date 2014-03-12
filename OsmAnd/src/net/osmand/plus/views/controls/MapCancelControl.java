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

public class MapCancelControl extends MapControls {
	private Button cancelButton;
	
	
	public MapCancelControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		Context ctx = mapActivity;
		cancelButton = new Button(ctx);
		cancelButton.setContentDescription(ctx.getString(R.string.cancel_navigation));
		cancelButton.setBackgroundResource(R.drawable.map_btn_cancel);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		params.leftMargin = ctx.getResources().getDrawable(R.drawable.map_btn_hmenu).getMinimumWidth();
		parent.addView(cancelButton, params);
		cancelButton.setEnabled(true);
		
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapActions().stopNavigationWithoutConfirm();
			}
		});
		mapActivity.accessibleContent.add(cancelButton);
	}

	@Override
	public void hideControls(FrameLayout layout) {
		layout.removeView(cancelButton);
		mapActivity.accessibleContent.remove(cancelButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
}
