package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapAppModeControl extends MapControls {
	private Button settingsAppModeButton;
	private OsmandSettings settings;
	private Drawable drawable;
	private int cachedId;
	
	
	public MapAppModeControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		settings = mapActivity.getMyApplication().getSettings();
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		settingsAppModeButton = addButton(parent, R.string.routing_preferences_descr, R.drawable.map_btn_plain);
		settingsAppModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
			}
		});
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, settingsAppModeButton);
		layout.removeView(settingsAppModeButton);
		mapActivity.accessibleContent.remove(settingsAppModeButton);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int id = settings.getApplicationMode().getIconId();
		if(cachedId != id && settingsAppModeButton.getLeft() > 0) {
			cachedId = id;
//			drawable = mapActivity.getResources().getDrawable(id);
//			int h = drawable.getMinimumHeight() / 2;
//			int w = drawable.getMinimumWidth() / 2;
//			int cx = (settingsAppModeButton.getLeft()  + settingsAppModeButton.getRight()) / 2;
//			int cy = (settingsAppModeButton.getTop()  + settingsAppModeButton.getBottom()) / 2;
//			drawable.setBounds(cx - w, cy - h, cx + w, cy + h);
			settingsAppModeButton.setBackgroundDrawable(new LayerDrawable(new Drawable[] {
					mapActivity.getResources().getDrawable(R.drawable.map_btn_plain), drawable }));
		}
		if(drawable != null) {
			drawable.draw(canvas);
		}
		
	}
}
