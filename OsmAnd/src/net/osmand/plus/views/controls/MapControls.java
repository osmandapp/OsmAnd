package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Handler;
import android.widget.FrameLayout;


public abstract class MapControls {

	protected MapActivity mapActivity;
	protected float scaleCoefficient;
	protected Handler showUIHandler;
	protected int shadowColor;
	private boolean visible;
	private long delayTime;

	public MapControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		this.mapActivity = mapActivity;
		this.showUIHandler = showUIHandler;
		this.scaleCoefficient = scaleCoefficient;
	}

	public void setShadowColor(int textColor, int shadowColor) {
		this.shadowColor = shadowColor;
	}

	public final void init(FrameLayout layout) {
		initControls(layout);
	}
	
	public final void show(FrameLayout layout) {
		visible = true;
		showControls(layout);
	}
	
	public final void showWithDelay(final FrameLayout layout, final long delay) {
		this.delayTime = System.currentTimeMillis() + delay;
		if(!visible) {
			visible = true;
			showControls(layout);
			runWithDelay(layout, delay);
			mapActivity.getMapView().refreshMap();
		}
	}

	private void runWithDelay(final FrameLayout layout, final long delay) {
		showUIHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				long ctime = System.currentTimeMillis();
				if(MapControls.this.delayTime <= ctime) {
					MapControls.this.delayTime = 0;
					visible = false;
					hideControls(layout);
					mapActivity.getMapView().refreshMap();
				} else {
					runWithDelay(layout, MapControls.this.delayTime - ctime);
				}
			}
		}, delay);
	}
	
	public final void hide(FrameLayout layout) {
		if(this.delayTime == 0) {
			visible = false;
			hideControls(layout);
		}
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	
	protected void initControls(FrameLayout layout) {
	}

	public abstract int getWidth();

	protected abstract void hideControls(FrameLayout layout);
	
	protected abstract void showControls(FrameLayout layout);
	

	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode);
	
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}
}