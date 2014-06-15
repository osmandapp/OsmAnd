package net.osmand.plus.views.controls;

import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapMenuControls extends MapControls {
	private Button backToMenuButton;
	private Drawable modeShadow;
	private Drawable cacheAppModeIcon = null;
	private ApplicationMode cacheApplicationMode = null;
	
	
	public MapMenuControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
	}
	
	private void initBackToMenuButton(final OsmandMapTileView view, FrameLayout parent) {
		backToMenuButton = addButton(parent, R.string.backToMenu, R.drawable.map_btn_menu);
		modeShadow = view.getResources().getDrawable(R.drawable.zoom_background);
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// double lat = activity.getMapView().getLatitude();
				// double lon = activity.getMapView().getLongitude();
				// MainMenuActivity.backToMainMenuDialog(activity, new LatLon(lat, lon));
				mapActivity.getMapActions().openOptionsMenuAsList();
				notifyClicked();
			}
		});
	}

	@Override
	public void showControls(FrameLayout layout) {
		initBackToMenuButton(mapActivity.getMapView(), layout);
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, backToMenuButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		drawApplicationMode(canvas, nightMode != null && nightMode.isNightMode());
	}
	
	
	private void drawApplicationMode(Canvas canvas, boolean nightMode) {
		OsmandMapTileView view = mapActivity.getMapView();
		ApplicationMode  appMode = view.getSettings().getApplicationMode();
		if(appMode != cacheApplicationMode){
			modeShadow.setBounds(backToMenuButton.getLeft() + (int) (2 * scaleCoefficient), backToMenuButton.getTop() - (int) (24 * scaleCoefficient),
					backToMenuButton.getRight() - (int) (4 * scaleCoefficient), backToMenuButton.getBottom());
			cacheAppModeIcon = view.getResources().getDrawable(appMode.getSmallIcon(nightMode));
			int l = modeShadow.getBounds().left + (modeShadow.getBounds().width() - cacheAppModeIcon.getMinimumWidth()) / 2;
			int t = (int) (modeShadow.getBounds().top + 2 * scaleCoefficient);
			cacheAppModeIcon.setBounds(l, t, l + cacheAppModeIcon.getMinimumWidth(), t + cacheAppModeIcon.getMinimumHeight());	
		}
		modeShadow.draw(canvas);
		if(cacheAppModeIcon != null){
			cacheAppModeIcon.draw(canvas);
		}
		
	}
	
	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (modeShadow.getBounds().contains((int) point.x, (int) point.y)) {
			onApplicationModePress();
			return true;
		}
		return false;
	}
	
	
	
	private void onApplicationModePress() {
		final QuickAction mQuickAction = new QuickAction(backToMenuButton);
		List<ApplicationMode> vls = ApplicationMode.values(mapActivity.getMyApplication().getSettings());
		final ApplicationMode[] modes = vls.toArray(new ApplicationMode[vls.size()]);
		int[] icons = new int[vls.size()];
		int[] values = new int[vls.size()];
		for(int k = 0; k < modes.length; k++) {
			icons[k] = modes[k].getSmallIcon(false);
			values[k] = modes[k].getStringResource();
		}
		for (int i = 0; i < modes.length; i++) {
			final ActionItem action = new ActionItem();
			action.setTitle(mapActivity.getResources().getString(values[i]));
			action.setIcon(mapActivity.getResources().getDrawable(icons[i]));
			final int j = i;
			action.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().getSettings().APPLICATION_MODE.set(modes[j]);
					mQuickAction.dismiss();
				}
			});
			mQuickAction.addActionItem(action);
		}
		mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);
		mQuickAction.show();
	}

	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_menu);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
	


}
