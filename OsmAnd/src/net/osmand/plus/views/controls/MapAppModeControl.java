package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int id = settings.getApplicationMode().getIconId();
		if(cachedId != id && settingsAppModeButton.getBackground().getBounds().width() > 0) {
			cachedId = id;
			drawable = mapActivity.getResources().getDrawable(id);
			drawable.setBounds(settingsAppModeButton.getBackground().getBounds());
		}
		drawable.draw(canvas);
		
	}
}
