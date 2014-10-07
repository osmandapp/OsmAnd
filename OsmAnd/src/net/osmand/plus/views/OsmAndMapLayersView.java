package net.osmand.plus.views;

import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

public class OsmAndMapLayersView extends View {
	
	private OsmandMapTileView mapView;

	public OsmAndMapLayersView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	public OsmAndMapLayersView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mapView = new OsmandMapTileView();
		mapView.initView(this);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		Boolean r = mapView.onTrackballEvent(event);
		if(r == null) {
			return super.onTrackballEvent(event);
		}
		return r;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Boolean r = mapView.onKeyDown(keyCode, event);
		if(r == null) {
			return super.onKeyDown(keyCode, event);
		}
		return r;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mapView.onTouchEvent(event);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		boolean nightMode = mapView.getApplication().getDaynightHelper().isNightMode();
		DrawSettings drawSettings = new DrawSettings(nightMode, false);
		mapView.drawOverMap(canvas, mapView.getCurrentRotatedTileBox().copy(), drawSettings);
	}
	

	public OsmandMapTileView getMapView() {
		return mapView;
	}

}
