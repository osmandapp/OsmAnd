package net.osmand.plus.views;

import android.os.SystemClock;
import android.support.v4.util.Pair;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread {
	
	protected static final Log log = PlatformUtil.getLog(AnimateDraggingMapThread.class);
	
	private final static float DRAGGING_ANIMATION_TIME = 1200f;
	private final static float ZOOM_ANIMATION_TIME = 250f;
	private final static float ZOOM_MOVE_ANIMATION_TIME = 350f;
	private final static float MOVE_MOVE_ANIMATION_TIME = 900f;
	private final static float NAV_ANIMATION_TIME = 1000f;
	private final static int DEFAULT_SLEEP_TO_REDRAW = 15;
	
	private volatile boolean stopped;
	private volatile Thread currentThread = null;
	private final OsmandMapTileView tileView;
	
	private float targetRotate = -720;
	private double targetLatitude = 0;
	private double targetLongitude = 0;
	private int targetIntZoom = 0;
	private int targetFloatZoom = 0;

	private boolean isAnimatingZoom;
	
	
	public AnimateDraggingMapThread(OsmandMapTileView tileView){
		this.tileView = tileView;
	}
	
	
	
	private void pendingRotateAnimation() {
		boolean conditionToCountinue = false;
		if (targetRotate != -720) {
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
			targetRotate = -720;
		}
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
		Thread tt = null;
		while((tt = currentThread) != null){
			try {
				tt.join();
			} catch (Exception e) {
			}
		}
	}
	
	public synchronized void startThreadAnimating(final Runnable runnable){
		stopAnimatingSync();
		stopped = false;
		final Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try { 
					suspendUpdate();
					runnable.run();
				} finally {
					currentThread = null;
					resumeUpdate();
				}
			}
		}, "Animating Thread");
		currentThread = t;		
		t.start();
	}

	public void startMoving(final double finalLat, final double finalLon, final Pair<Integer, Double> finalZoom,
							final Float finalRotation, final boolean notifyListener) {
		stopAnimatingSync();
		final RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		final int startZoom = rb.getZoom();
		final double startZoomFP = rb.getZoomFloatPart();
		final float startRotaton = rb.getRotate();

		final int zoom;
		final double zoomFP;
		final float rotation;
		if (finalZoom != null) {
			zoom = finalZoom.first;
			zoomFP = finalZoom.second;
		} else {
			zoom = startZoom;
			zoomFP = startZoomFP;
		}
		if (finalRotation != null) {
			rotation = finalRotation;
		} else {
			rotation = startRotaton;
		}

		final float mMoveX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		final float mMoveY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		boolean skipAnimation = !rb.containsLatLon(finalLat, finalLon);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom, 0, notifyListener);
			tileView.rotateToAnimate(rotation);
			return;
		}

		startThreadAnimating(new Runnable() {

			@Override
			public void run() {
				setTargetValues(zoom, finalLat, finalLon);
				boolean animateZoom = finalZoom != null && (zoom != startZoom || startZoomFP != 0);
				boolean animateRotation = rotation != startRotaton;
				if (animateZoom) {
					animatingZoomInThread(startZoom, startZoomFP, zoom, zoomFP, NAV_ANIMATION_TIME, notifyListener);
				}

				if (animateRotation) {
					animatingRotateInThread(rotation, 500f, notifyListener);
				}

				if (!stopped){
					animatingMoveInThread(mMoveX, mMoveY, NAV_ANIMATION_TIME, notifyListener, null);
				}
			}
		});
	}

	public void startMoving(final double finalLat, final double finalLon, final int endZoom, final boolean notifyListener) {
		startMoving(finalLat, finalLon, endZoom, notifyListener, null);
	}

	public void startMoving(final double finalLat, final double finalLon, final int endZoom,
							final boolean notifyListener, final Runnable finishAminationCallback) {
		stopAnimatingSync();
		final RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		float rotate = rb.getRotate();
		final int startZoom = rb.getZoom();
		final double startZoomFP = rb.getZoomFloatPart();
		
		boolean skipAnimation = false;
		float mStX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		float mStY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		while (Math.abs(mStX) + Math.abs(mStY) > 1200) {
			rb.setZoom(rb.getZoom() - 1);
			if(rb.getZoom() <= 4){
				skipAnimation = true;
			}
			mStX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
			mStY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		}
		final int moveZoom = rb.getZoom();
		// check if animation needed
		skipAnimation = skipAnimation || (Math.abs(moveZoom - startZoom) >= 3 || Math.abs(endZoom - moveZoom) > 3);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(endZoom, 0, notifyListener);
			return;
		}
		final float mMoveX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		final float mMoveY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		
		final float animationTime = Math.max(450, (Math.abs(mStX) + Math.abs(mStY)) / 1200f * MOVE_MOVE_ANIMATION_TIME);
		
		startThreadAnimating(new Runnable() {
			
			@Override
			public void run() {
				setTargetValues(endZoom, finalLat, finalLon);
				if(moveZoom != startZoom){
					animatingZoomInThread(startZoom, startZoomFP, moveZoom, startZoomFP,ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				
				if(!stopped){
					animatingMoveInThread(mMoveX, mMoveY, animationTime, notifyListener, finishAminationCallback);
				}
				if(!stopped){
					tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
				}
				
				if (!stopped && (moveZoom != endZoom || startZoomFP != 0)) {
					animatingZoomInThread(moveZoom, startZoomFP, endZoom, 0, ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				tileView.setFractionalZoom(endZoom, 0, notifyListener);
				
				pendingRotateAnimation();
			}
		});
	}

	private void animatingRotateInThread(float rotate, float animationTime, boolean notify){
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		float startRotate = tileView.getRotate();
		float rotationDiff = MapUtils.unifyRotationDiff(startRotate, rotate);
		if (tileView.isMapRotateEnabled() && Math.abs(rotationDiff) > 1) {
			long timeMillis = SystemClock.uptimeMillis();
			float normalizedTime;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
				if (normalizedTime > 1f) {
					tileView.rotateToAnimate(rotate);
					break;
				}
				float interpolation = interpolator.getInterpolation(normalizedTime);
				tileView.rotateToAnimate(rotationDiff * interpolation + startRotate);
				try {
					Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
				} catch (InterruptedException e) {
					stopped = true;
				}
			}
		} else {
			tileView.rotateToAnimate(rotate);
		}
	}

	private void animatingMoveInThread(float moveX, float moveY, float animationTime,
			boolean notify, final Runnable finishAnimationCallback){
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		
		float cX = 0;
		float cY = 0;
		long timeMillis = SystemClock.uptimeMillis();
		float normalizedTime = 0f;
		while (!stopped){
			normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime; 
			if (normalizedTime > 1f) {
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
		if (finishAnimationCallback != null) {
			tileView.getApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					finishAnimationCallback.run();
				}
			});
		}
	}
	
	private void animatingZoomInThread(int zoomStart, double zoomFloatStart, 
			int zoomEnd, double zoomFloatEnd, float animationTime, boolean notifyListener){
		try {
			isAnimatingZoom = true;
			// could be 0 ]-0.5,0.5], -1 ]-1,0], 1 ]0, 1]  
			int threshold = ((int)(zoomFloatEnd * 2));
			double beginZoom = zoomStart + zoomFloatStart;
			double endZoom =  zoomEnd + zoomFloatEnd;
			
			double curZoom = beginZoom;
			animationTime *= Math.abs(endZoom - beginZoom);
			// AccelerateInterpolator interpolator = new AccelerateInterpolator(1);
			LinearInterpolator interpolator = new LinearInterpolator();

			long timeMillis = SystemClock.uptimeMillis();
			float normalizedTime = 0f;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
				if (normalizedTime > 1f) {
					break;
				}
				float interpolation = interpolator.getInterpolation(normalizedTime);
				curZoom = interpolation * (endZoom - beginZoom) + beginZoom;
				int baseZoom = (int) Math.round(curZoom - 0.5 * threshold);
				double zaAnimate = curZoom - baseZoom; 
				tileView.zoomToAnimate(baseZoom, zaAnimate, notifyListener);
				try {
					Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
				} catch (InterruptedException e) {
					stopped = true;
				}
			}
			tileView.setFractionalZoom(zoomEnd, zoomFloatEnd,  notifyListener);
		} finally {
			isAnimatingZoom = false;
		}
	}
	
	public boolean isAnimatingZoom() {
		return isAnimatingZoom;
	}

	public void startZooming(final int zoomEnd, final double zoomPart, final boolean notifyListener){
		final float animationTime = ZOOM_ANIMATION_TIME;
		startThreadAnimating(new Runnable(){
			@Override
			public void run() {
				RotatedTileBox tb = tileView.getCurrentRotatedTileBox();
				setTargetValues(zoomEnd, tileView.getLatitude(), tileView.getLongitude());
				animatingZoomInThread(tb.getZoom(), tb.getZoomFloatPart(), zoomEnd, zoomPart, animationTime, notifyListener);
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
		targetIntZoom = 0;
	}
	
	private void suspendUpdate() {
		final MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.suspendSymbolsUpdate();
		}
	}
	
	private void resumeUpdate() {
		final MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.resumeSymbolsUpdate();
		}
	}
	
	private void setTargetValues(int zoom, double lat, double lon){
		targetIntZoom = zoom;
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
	

	public int getTargetIntZoom() {
		return targetIntZoom;
	}

	public double getTargetLatitude() {
		return targetLatitude;
	}
	
	public double getTargetLongitude() {
		return targetLongitude;
	}
	
}


