package net.osmand.plus.views;

import net.osmand.LogUtil;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.os.SystemClock;
import android.util.FloatMath;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread {
	
	protected static final Log log = LogUtil.getLog(AnimateDraggingMapThread.class);
	
	private final static float DRAGGING_ANIMATION_TIME = 1900f;
	private final static float ZOOM_ANIMATION_TIME = 800f;
	private final static float ZOOM_MOVE_ANIMATION_TIME = 650f;
	private final static float MOVE_MOVE_ANIMATION_TIME = 1000f;
	private final static int DEFAULT_SLEEP_TO_REDRAW = 55;
	
	private volatile boolean stopped;
	private volatile Thread currentThread = null;
	private final OsmandMapTileView tileView;
	
	private float targetRotate = 0;
	private double targetLatitude = 0;
	private double targetLongitude = 0;
	private int targetZoom = 0;
	
	
	public AnimateDraggingMapThread(OsmandMapTileView tileView){
		this.tileView = tileView;
	}
	
	
	
	private void pendingRotateAnimation() {
		boolean conditionToCountinue = false;
		do {
			conditionToCountinue = false;
			float rotationDiff = MapUtils.unifyRotationDiff(tileView.getRotate(), targetRotate);
			float absDiff = Math.abs(rotationDiff);
			if (absDiff > 0) {
				try {
					Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
				} catch (InterruptedException e) {
					//do nothing
				}
				if (absDiff < 1) {
					tileView.rotateToAnimate(targetRotate);
				} else {
					conditionToCountinue = true;
					tileView.rotateToAnimate(rotationDiff / 5 + tileView.getRotate());
				}
			}
		} while (conditionToCountinue && tileView.isMapRotateEnabled());
	}
	

	/**
	 * Stop dragging async
	 */
	public void stopAnimating(){
		stopped = true;
	}
	
	public boolean isAnimating(){
		return currentThread != null && !stopped;
	}
	
	/**
	 * Stop dragging sync
	 */
	public void stopAnimatingSync(){
		// wait until current thread != null
		stopped = true;
		while(currentThread != null){
			try {
				currentThread.join();
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void startThreadAnimating(final Runnable runnable){
		stopAnimatingSync();
		stopped = false;
		currentThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					currentThread = null;
				}
			}
		}, "Animating Thread");
		currentThread.start();
		
	}
	
	public void startMoving(final double finalLat, final double finalLon, final int endZoom, final boolean notifyListener){
		stopAnimatingSync();
		
		double startLat = tileView.getLatitude();
		double startLon = tileView.getLongitude();
		float rotate = tileView.getRotate();
		final int startZoom = tileView.getZoom();
		int tileSize = tileView.getSourceTileSize();
		
		
		int mZoom = startZoom;
		boolean skipAnimation = false;
		float mStX = (float) ((MapUtils.getTileNumberX(mZoom, startLon) - MapUtils.getTileNumberX(mZoom, finalLon)) * tileSize);
		float mStY = (float) ((MapUtils.getTileNumberY(mZoom, startLat) - MapUtils.getTileNumberY(mZoom, finalLat)) * tileSize);
		while (Math.abs(mStX) + Math.abs(mStY) > 1200) {
			mZoom--;
			if(mZoom <= 4){
				skipAnimation = true;
			}
			mStX = (float) ((MapUtils.getTileNumberX(mZoom, startLon) - MapUtils.getTileNumberX(mZoom, finalLon)) * tileSize);
			mStY = (float) ((MapUtils.getTileNumberY(mZoom, startLat) - MapUtils.getTileNumberY(mZoom, finalLat)) * tileSize);
		}
		final int moveZoom = mZoom;
		// check if animation needed
		skipAnimation = skipAnimation || (Math.abs(moveZoom - startZoom) >= 3 || Math.abs(endZoom - moveZoom) > 3);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.zoomToAnimate(endZoom, notifyListener);
			return;
		}
		float rad = (float) Math.toRadians(rotate);
		final float mMoveX = FloatMath.cos(rad) * mStX - FloatMath.sin(rad) * mStY; 
		final float mMoveY = FloatMath.sin(rad) * mStX + FloatMath.cos(rad) * mStY;
		
		final float animationTime = Math.max(450, (Math.abs(mStX) + Math.abs(mStY)) / 1200f * MOVE_MOVE_ANIMATION_TIME);
		
		startThreadAnimating(new Runnable() {
			
			@Override
			public void run() {
				setTargetValues(endZoom, finalLat, finalLon);
				if(moveZoom != startZoom){
					animatingZoomInThread(startZoom, moveZoom, ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				
				if(!stopped){
					animatingMoveInThread(mMoveX, mMoveY, animationTime, notifyListener);
				}
				if(!stopped){
					tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
				}
				
				if (!stopped && moveZoom != endZoom) {
					animatingZoomInThread(moveZoom, endZoom, ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				
				pendingRotateAnimation();
			}
		});
	}
	
	private void animatingMoveInThread(float moveX, float moveY, float animationTime,
			boolean notify){
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		
		float cX = 0;
		float cY = 0;
		long timeMillis = SystemClock.uptimeMillis();
		float normalizedTime = 0f;
		while(!stopped){
			normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime; 
			if(normalizedTime > 1f){
				break;
			}
			float interpolation = interpolator.getInterpolation(normalizedTime);
			float nX = interpolation * moveX;
			float nY = interpolation * moveY;
			tileView.dragToAnimate(cX, cY, nX, nY, notify);
			cX = nX;
			cY = nY;
			try {
				Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
			} catch (InterruptedException e) {
				stopped = true;
			}
		}
		
	}
	
	private void animatingZoomInThread(int zoomStart, int zoomEnd, float animationTime, boolean notifyListener){
		float curZoom = zoomStart;
		animationTime *= Math.abs(zoomEnd - zoomStart);
		// AccelerateInterpolator interpolator = new AccelerateInterpolator(1);
		LinearInterpolator interpolator = new LinearInterpolator();
		
		long timeMillis = SystemClock.uptimeMillis();
		float normalizedTime = 0f;
		while(!stopped){
			normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime; 
			if(normalizedTime > 1f){
				break;
			}
			float interpolation = interpolator.getInterpolation(normalizedTime);
			curZoom = interpolation * (zoomEnd - zoomStart) + zoomStart;
			tileView.zoomToAnimate(curZoom, notifyListener);
			try {
				Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
			} catch (InterruptedException e) {
				stopped = true;
			}
		}
		
		if(curZoom != ((int) Math.round(curZoom))){
			if(Math.abs(curZoom - zoomEnd) > 2){
				if(zoomStart > zoomEnd){
					curZoom = (float) Math.floor(curZoom);
				} else {
					curZoom = (float) Math.ceil(curZoom);
				}
				tileView.zoomToAnimate(curZoom, notifyListener);
			} else {
				tileView.zoomToAnimate(zoomEnd, notifyListener);
			}
		}
	}
	
	
	public void startZooming(final int zoomEnd, final boolean notifyListener){
		final float animationTime = ZOOM_ANIMATION_TIME;
		startThreadAnimating(new Runnable(){
			@Override
			public void run() {
				final int zoomStart = tileView.getZoom();
				setTargetValues(zoomEnd, tileView.getLatitude(), tileView.getLongitude());
				animatingZoomInThread(zoomStart, zoomEnd, animationTime, notifyListener);
				pendingRotateAnimation();
			}
		}); //$NON-NLS-1$
	}
	
	
	public void startDragging(final float velocityX, final float velocityY, float startX, float startY, 
			final float endX, final float endY, final boolean notifyListener){
		final float animationTime = DRAGGING_ANIMATION_TIME;
		clearTargetValues();
		startThreadAnimating(new Runnable(){
			@Override
			public void run() {
				float curX = endX;
				float curY = endY;
				DecelerateInterpolator interpolator = new DecelerateInterpolator(1);
				
				long timeMillis = SystemClock.uptimeMillis();
				float normalizedTime = 0f;
				float prevNormalizedTime = 0f;
				while(!stopped){
					normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime; 
					if(normalizedTime >= 1f){
						break;
					}
					float interpolation = interpolator.getInterpolation(normalizedTime);
					
					float newX = velocityX * (1 - interpolation) * (normalizedTime - prevNormalizedTime) + curX;
					float newY = velocityY * (1 - interpolation) * (normalizedTime - prevNormalizedTime) + curY;
					
					tileView.dragToAnimate(curX, curY, newX, newY, notifyListener);
					curX = newX;
					curY = newY;
					prevNormalizedTime = normalizedTime;
					try {
						Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
					} catch (InterruptedException e) {
						stopped = true;
					}
				}
				pendingRotateAnimation();
			}
		}); //$NON-NLS-1$
	}
	
	private void clearTargetValues(){
		targetZoom = 0;
	}
	
	private void setTargetValues(int zoom, double lat, double lon){
		targetZoom = zoom;
		targetLatitude = lat;
		targetLongitude = lon;
	}
	
	public void startRotate(final float rotate) {
		if (!isAnimating()) {
			clearTargetValues();
			// stopped = false;
			// do we need to kill and recreate the thread? wait would be enough as now it
			// also handles the rotation?
			startThreadAnimating(new Runnable() {
				@Override
				public void run() {
					targetRotate = rotate;
					pendingRotateAnimation();
				}
			});
		} else {
			this.targetRotate = rotate;
		}
	}
	
	public int getTargetZoom() {
		return targetZoom;
	}
	
	public double getTargetLatitude() {
		return targetLatitude;
	}
	
	public double getTargetLongitude() {
		return targetLongitude;
	}
	
}

