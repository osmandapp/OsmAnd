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
	
	@Override
	public void setOnClickListener(OnClickListener l) {
		super.setOnClickListener(l);
		this.onClickListener = l;
	}
	

	private void init() {
		getHolder().addCallback(this);	
	}
	

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(mapView != null) {
			mapView.refreshMap();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(mapView != null) {
			mapView.refreshMap();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		mapView.setView(this);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if(mapView == null) {
			return super.onTrackballEvent(event);
		}
		Boolean r = mapView.onTrackballEvent(event);
		if(r == null) {
			return super.onTrackballEvent(event);
		}
		return r;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(mapView == null) {
			return super.onKeyDown(keyCode, event);
		}
		Boolean r = mapView.onKeyDown(keyCode, event);
		if(r == null) {
			return super.onKeyDown(keyCode, event);
		}
		return r;
	}
	
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if(mapView == null) {
			return super.onGenericMotionEvent(event);
		}
		return mapView.onGenericMotionEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(mapView == null) {
			return super.onTouchEvent(event);
		}
		return mapView.onTouchEvent(event);
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}
}