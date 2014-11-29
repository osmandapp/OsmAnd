package net.osmand.plus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class OsmAndMapSurfaceView extends SurfaceView implements Callback {
	
	private OsmandMapTileView mapView;
	private OnClickListener onClickListener;

	public OsmAndMapSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	public OsmAndMapSurfaceView(Context context) {
		super(context);
		init();
	}

	private void init() {
		getHolder().addCallback(this);	
		mapView = new OsmandMapTileView();
		mapView.initView(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mapView.refreshMap();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mapView.refreshMap();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
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
	public void setOnClickListener(OnClickListener l) {
		super.setOnClickListener(l);
		this.onClickListener = l;
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
		if(onClickListener != null) {
			return super.onTouchEvent(event);
		}
		return mapView.onTouchEvent(event);
	}
	

	public OsmandMapTileView getMapView() {
		return mapView;
	}

}
