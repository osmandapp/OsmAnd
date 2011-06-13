package net.osmand.plus.views;

import net.osmand.plus.R;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.activities.MapActivity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;

public class MapControlsLayer implements OsmandMapLayer {

	private static final int SHOW_SEEKBAR_MSG_ID = 2;
	private static final int SHOW_SEEKBAR_DELAY = 7000;
	private static final int SHOW_SEEKBAR_SECOND_DELAY = 25000;
	

	private OsmandMapTileView view;
	private DisplayMetrics dm;
	private Button zoomInButton;
	private Button zoomOutButton;
	private Button backToMenuButton;
	private final MapActivity activity;
	
	private SeekBar transparencyBar;
	private Handler showBarHandler;
	private CommonPreference<Integer> settingsToTransparency;
	private BaseMapLayer[] transparencyLayers;

	public MapControlsLayer(MapActivity activity){
		this.activity = activity;
	}
	
	
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		
		FrameLayout parent = (FrameLayout) view.getParent();
		
		zoomInButton = new Button(view.getContext());
		zoomInButton.setBackgroundResource(R.drawable.map_zoom_in);
		android.widget.FrameLayout.LayoutParams params = 
			new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		parent.addView(zoomInButton, params);
		parent.layout(0, 0, 0, 0);
		
		zoomOutButton = new Button(view.getContext());
		zoomOutButton.setBackgroundResource(R.drawable.map_zoom_out);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		params.setMargins(0, 0, minimumWidth , 0);
		parent.addView(zoomOutButton, params);
		
		backToMenuButton = new Button(view.getContext());
		backToMenuButton.setBackgroundResource(R.drawable.map_btn_menu);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		parent.addView(backToMenuButton, params);
		backToMenuButton.setEnabled(true);
		
		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setVisibility(View.GONE);
		transparencyBar.setMax(255);
		params = new FrameLayout.LayoutParams((int) (dm.density * 100), LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER);
		params.setMargins(0, 0, 0, minimumHeight + 3);
		parent.addView(transparencyBar, params);
		
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(view.isZooming()){
					activity.changeZoom(view.getZoom() + 2);
				} else {
					activity.changeZoom(view.getZoom() + 1);
				}
				
			}
		});
		
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.backToMainMenu();
			}
		});
		
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(view.getZoom() - 1);
				
			}
		});
		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(settingsToTransparency != null && transparencyLayers != null){
					settingsToTransparency.set(progress);
					for(BaseMapLayer base : transparencyLayers){
						base.setAlpha(progress);
					}
					MapControlsLayer.this.view.refreshMap();
					showAndHideTransparencyBar(settingsToTransparency, transparencyLayers, SHOW_SEEKBAR_SECOND_DELAY);
				}
			}
		});
	}
	
	public void showAndHideTransparencyBar(CommonPreference<Integer> transparenPreference,
			BaseMapLayer[] layerToChange) {
		showAndHideTransparencyBar(transparenPreference, layerToChange, SHOW_SEEKBAR_DELAY);
	}
	private void showAndHideTransparencyBar(CommonPreference<Integer> transparenPreference,
			BaseMapLayer[] layerToChange, int delay) {
		transparencyBar.setVisibility(View.VISIBLE);
		transparencyBar.setProgress(transparenPreference.get());
		this.transparencyLayers = layerToChange;
		this.settingsToTransparency = transparenPreference;
		if (showBarHandler == null) {
			showBarHandler = new Handler();
		}
		Message msg = Message.obtain(showBarHandler, new Runnable() {
			@Override
			public void run() {
				transparencyBar.setVisibility(View.GONE);
			}

		});
		msg.what = SHOW_SEEKBAR_MSG_ID;
		showBarHandler.removeMessages(SHOW_SEEKBAR_MSG_ID);
		showBarHandler.sendMessageDelayed(msg, delay);
	}
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, boolean nightMode) {
		BaseMapLayer mainLayer = view.getMainLayer();
		boolean zoomInEnabled = mainLayer != null && view.getZoom() < mainLayer.getMaximumShownMapZoom();
		boolean zoomOutEnabled = mainLayer != null && view.getZoom() > mainLayer.getMinimumShownMapZoom();
		if(zoomInButton.isEnabled() != zoomInEnabled){
			zoomInButton.setEnabled(zoomInEnabled);
		}
		if(zoomOutButton.isEnabled() != zoomOutEnabled){
			zoomOutButton.setEnabled(zoomOutEnabled);
		}
		
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}

}
