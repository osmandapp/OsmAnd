package net.osmand.plus.views;

import net.osmand.OsmAndFormatter;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.activities.MapActivity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;

public class MapControlsLayer implements OsmandMapLayer {

	private static final int SHOW_SEEKBAR_MSG_ID = 2;
	private static final int SHOW_SEEKBAR_DELAY = 7000;
	private static final int SHOW_SEEKBAR_SECOND_DELAY = 25000;
	

	private OsmandMapTileView view;
	private DisplayMetrics dm;
	private final MapActivity activity;
	private Handler showUIHandler;
	
	private Button zoomInButton;
	private int ZOOM_IN_LEFT_MARGIN = 2;
	private Button zoomOutButton;
	private TextPaint zoomTextPaint;
	
	private Button backToMenuButton;
	
	
	// transparency bar
	private SeekBar transparencyBar;
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
		showUIHandler = new Handler();
		
		initZoomButtons(view, parent);

		initBackToMenuButton(view, parent);
		
		initTransparencyBar(view, parent);
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
		
		String zoomText = view.getZoom()+"";
		float length = zoomTextPaint.measureText(zoomText);
		canvas.drawText(zoomText, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - (ZOOM_IN_LEFT_MARGIN * dm.density) ) / 2, zoomInButton.getTop() + 4 * dm.density,
				zoomTextPaint);  
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}

	
	private void initBackToMenuButton(final OsmandMapTileView view, FrameLayout parent) {
		android.widget.FrameLayout.LayoutParams params;
		backToMenuButton = new Button(view.getContext());
		backToMenuButton.setBackgroundResource(R.drawable.map_btn_menu);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		parent.addView(backToMenuButton, params);
		backToMenuButton.setEnabled(true);
		
		
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.backToMainMenu();
			}
		});
	}
	
	private void initZoomButtons(final OsmandMapTileView view, FrameLayout parent) {
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * dm.density);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);
		
		ImageView zoomShadow = new ImageView(view.getContext());
		zoomShadow.setBackgroundResource(R.drawable.zoom_background);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(minimumWidth + (int) (ZOOM_IN_LEFT_MARGIN * dm.density), 
				minimumHeight + (int) (18 * dm.density), Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		parent.addView(zoomShadow, params);
		
		zoomInButton = new Button(view.getContext());
		zoomInButton.setBackgroundResource(R.drawable.map_zoom_in);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		parent.addView(zoomInButton, params);
		
		
		zoomOutButton = new Button(view.getContext());
		zoomOutButton.setBackgroundResource(R.drawable.map_zoom_out);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		
		params.setMargins(0, 0, minimumWidth , 0);
		parent.addView(zoomOutButton, params);
		
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
		
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(view.getZoom() - 1);
				
			}
		});
	}
	



	/////////////////  Transparency bar /////////////////////////
	private void initTransparencyBar(final OsmandMapTileView view, FrameLayout parent) {
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		android.widget.FrameLayout.LayoutParams params;
		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setVisibility(View.GONE);
		transparencyBar.setMax(255);
		params = new FrameLayout.LayoutParams((int) (dm.density * 100), LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER);
		params.setMargins(0, 0, 0, minimumHeight + 3);
		parent.addView(transparencyBar, params);
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
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				transparencyBar.setVisibility(View.GONE);
			}

		});
		msg.what = SHOW_SEEKBAR_MSG_ID;
		showUIHandler.removeMessages(SHOW_SEEKBAR_MSG_ID);
		showUIHandler.sendMessageDelayed(msg, delay);
	}
	
	
	
	
	/////////////////////// Ruler ///////////////////
	// cache values for ruler
	int rulerDistPix = 0;
	String rulerDistName = null;
	int rulerBaseLine = 50;
	float rulerTextLen = 0;
	// cache properties
	int rulerCZoom = 0;
	double rulerCTileX = 0;
	double rulerCTileY = 0;
	
	private void drawRuler(Canvas canvas) {
		// occupy length over screen
		double screenPercent = 0.2;
		
				
		// update cache
		if (view.isZooming()) {
			rulerDistName = null;
		} else if(view.getZoom() != rulerCZoom || 
				Math.abs(view.getXTile() - rulerCTileX) +  Math.abs(view.getYTile() - rulerCTileY) > 1){
			rulerCZoom = view.getZoom();
			rulerCTileX = view.getXTile();
			rulerCTileY = view.getYTile();
			double latitude = view.getLatitude();
			double tileNumberLeft = rulerCTileX - ((double) view.getWidth()) / (2d * view.getTileSize());
			double tileNumberRight = rulerCTileX + ((double) view.getWidth()) / (2d * view.getTileSize());
			double dist = MapUtils.getDistance(latitude, MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberLeft), latitude,
					MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberRight));

			dist *= screenPercent;
			int baseDist = 5;
			byte pointer = 0;
			while (dist > baseDist) {
				if (pointer++ % 3 == 2) {
					baseDist = baseDist * 5 / 2;
				} else {
					baseDist *= 2;
				}
			}

			rulerDistPix = (int) (view.getWidth() * screenPercent / dist * baseDist);
			rulerDistName = OsmAndFormatter.getFormattedDistance(baseDist, view.getContext());
			rulerBaseLine = (int) (view.getHeight() - 50 * dm.density);
			rulerTextLen = zoomTextPaint.measureText(rulerDistName);
		} 
		if (rulerDistName != null) {
			int w2 = (int) (view.getWidth() - 5 * dm.density);
			canvas.drawLine(w2 - rulerDistPix, rulerBaseLine, w2, rulerBaseLine, zoomTextPaint);
			canvas.drawLine(w2 - rulerDistPix, rulerBaseLine, w2 - rulerDistPix, rulerBaseLine - 10 * dm.density, zoomTextPaint);
			canvas.drawLine(w2, rulerBaseLine, w2, rulerBaseLine - 10 * dm.density, zoomTextPaint);
			canvas.drawText(rulerDistName, w2 - (rulerDistPix + rulerTextLen)/2 + 1, rulerBaseLine - 5 * dm.density, zoomTextPaint);
		}
	}
	

}
