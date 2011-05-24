package net.osmand.plus.views;

import net.osmand.LogUtil;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.os.SystemClock;
import android.util.FloatMath;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread implements Runnable {
	
	protected static final Log log = LogUtil.getLog(AnimateDraggingMapThread.class);
	
	public interface AnimateDraggingCallback {
		
		public void dragTo(float curX, float curY, float newX, float newY, boolean notify);
		
		public void setLatLon(double latitude, double longitude, boolean notify);
		
		public void zoomTo(float zoom, boolean notify);
		
		public void rotateTo(float rotate);
		
		public float getRotate();
		
	}
	
	private final static float DRAGGING_ANIMATION_TIME = 1900f;
	private final static float ZOOM_ANIMATION_TIME = 800f;
	private final static int DEFAULT_SLEEP_TO_REDRAW = 55;
	
	private volatile boolean stopped;
	private volatile Thread currentThread = null;
	private AnimateDraggingCallback callback = null;
	private boolean notifyListener;
	
	private float targetRotate = 0;
	
	
	private boolean animateDrag = true;
	private float curX;
	private float curY;
	private float vx;
	private float vy;
	private float ax;
	private float ay;
	private byte dirX;
	private byte dirY;
	private final float  a = 0.0014f;
	
	private long time;
	
	
	// 0 - zoom out, 1 - moving, 2 - zoom in
	private byte phaseOfMoving ;
	private int endZ;
	private byte dirZ;
	private int intZ;
	private byte dirIntZ;
	private float curZ;
	private int timeZEnd;
	private int timeZInt;
	private int timeMove;
	private float moveX;
	private float moveY;
	private double moveLat;
	private double moveLon;
	

	private double targetLatitude = 0;
	private double targetLongitude = 0;
	private int targetZoom = 0;
	
	@Override
	public void run() {
		currentThread = Thread.currentThread();
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		try {
			
			boolean conditionToCountinue = true;
			while (!stopped && conditionToCountinue) {
				// calculate sleep
				long sleep = 0;
				if(animateDrag){
//					sleep = (long) (40d / (Math.max(vx, vy) + 0.45));
					sleep = 80;
				} else {
					sleep = 80;
				}
				Thread.sleep(sleep);
				long curT = System.currentTimeMillis();
				int dt = (int) (curT - time);
				float newX = animateDrag && vx > 0 ? curX + dirX * vx * dt : curX;
				float newY = animateDrag && vy > 0 ? curY + dirY * vy * dt : curY;
				
				float newZ = curZ;
				if(!animateDrag){
					if (phaseOfMoving == 0 || phaseOfMoving == 2) {
						byte dir = phaseOfMoving == 2 ? dirZ : dirIntZ;
						int time = phaseOfMoving == 2 ? timeZEnd : timeZInt;
						float end = phaseOfMoving == 2 ? endZ : intZ;
						if (time > 0) {
							newZ = newZ + dir * (float) dt / time;
						}
						if (dir > 0 == newZ > end) {
							newZ = end;
						}
					} else {
						if(timeMove > 0){
							newX = newX + moveX * (float) dt / timeMove;
							newY = newY + moveY * (float) dt / timeMove;
							
							if(moveX > 0 == newX > moveX){
								newX = moveX;
							}
							if(moveY > 0 == newY > moveY){
								newY = moveY;
							}
						}
					}
				}
				if (!stopped && callback != null) {
					if (animateDrag || phaseOfMoving == 1) {
						callback.dragTo(curX, curY, newX, newY, notifyListener);
					} else {
						callback.zoomTo(newZ, notifyListener);
					}
				}
				time = curT;
				if(animateDrag){
					vx -= ax * dt;
					vy -= ay * dt;
					curX = newX;
					curY = newY;
					conditionToCountinue = vx > 0.5 || vy > 0.5;
				} else {
					if(phaseOfMoving == 0){
						curZ = newZ;
						if(curZ == intZ){
							curX = 0;
							curY = 0;
							phaseOfMoving ++;
						}
					} else if(phaseOfMoving == 2){
						curZ = newZ;
						conditionToCountinue = curZ != endZ;
					} else  {
						curX = newX;
						curY = newY;
						if(curX == moveX && curY == moveY){
							phaseOfMoving ++;
							callback.setLatLon(moveLat, moveLon, notifyListener);
						}
					}
				}
			}
			if(curZ != ((int) Math.round(curZ))){
				if(Math.abs(curZ - endZ) > 3){
					callback.zoomTo(Math.round(curZ), notifyListener);
				} else {
					callback.zoomTo(endZ, notifyListener);
				}
			}
			//rotate after animation
			pendingRotateAnimation();
		} catch (InterruptedException e) {
		}
		currentThread = null;
	}
	
	
	private void pendingRotateAnimation() throws InterruptedException{
		boolean conditionToCountinue = true;
		while (conditionToCountinue && callback != null) {
			conditionToCountinue = false;
			float rotationDiff = targetRotate - callback.getRotate();
			if (Math.abs((rotationDiff + 360) % 360) < Math.abs((rotationDiff - 360) % 360)) {
				rotationDiff = (rotationDiff + 360) % 360;
			} else {
				rotationDiff = (rotationDiff - 360) % 360;
			}
			float absDiff = Math.abs(rotationDiff);
			if (absDiff > 0) {
				Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
				if (absDiff < 1) {
					callback.rotateTo(targetRotate);
				} else {
					conditionToCountinue = true;
					callback.rotateTo(((absDiff / 10) * Math.signum(rotationDiff) + callback.getRotate()) % 360);
				}
			}
		}
	}
	

	/**
	 * Stop dragging async
	 */
	public void stopAnimating(){
		stopped = true;
	}
	
	public boolean isAnimating(){
		return currentThread != null;
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
	
	public void startMoving(double curLat, double curLon, double finalLat, double finalLon, int curZoom, int endZoom, int tileSize, float rotate, boolean notifyListener){
		stopAnimatingSync();
		targetLatitude = finalLat;
		targetLongitude = finalLon;
		targetZoom = endZoom;
		
		this.notifyListener = notifyListener;
		curZ = curZoom;
		intZ = curZoom;
		float mX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
		float mY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		while (Math.abs(mX) + Math.abs(mY) > 1200 && intZ > 4) {
			intZ--;
			mX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
			mY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		}
		float rad = (float) Math.toRadians(rotate);
		moveX = FloatMath.cos(rad) * mX - FloatMath.sin(rad) * mY; 
		moveY = FloatMath.sin(rad) * mX + FloatMath.cos(rad) * mY;
		moveLat = finalLat;
		moveLon = finalLon;
		if(curZoom < intZ){
			dirIntZ = 1;
		} else {
			dirIntZ = -1;
		}
		
		if(intZ < endZoom){
			dirZ = 1;
		} else {
			dirZ = -1;
		}
		
		endZ = endZoom;
		
//		timeZInt = Math.abs(curZoom - intZ) * 300;
//		if (timeZInt > 900) {
//			
//		}
		timeZInt = 600;
		timeZEnd = 500;
		timeMove = (int) (Math.abs(moveX) + Math.abs(moveY) * 3);
		if(timeMove > 2200){
			timeMove = 2200;
		}
		animateDrag = false;
		phaseOfMoving = (byte) (intZ == curZoom ? 1 : 0);
		curX = 0;
		curY = 0;

		
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	
	public void startZooming(final int zoomStart, final int zoomEnd){
		stopAnimatingSync();
		
		stopped = false;
		final boolean notifyListener = true;
		final float animationTime = ZOOM_ANIMATION_TIME;
		
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				currentThread = Thread.currentThread();
				float curZoom = zoomStart;
				AccelerateInterpolator interpolator = new AccelerateInterpolator(1);
				
				long timeMillis = SystemClock.uptimeMillis();
				float normalizedTime = 0f;
				while(!stopped){
					normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime; 
					if(normalizedTime > 1f){
						break;
					}
					float interpolation = interpolator.getInterpolation(normalizedTime);
					curZoom = interpolation * (zoomEnd - zoomStart) + zoomStart;
					callback.zoomTo(curZoom, notifyListener);
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
						callback.zoomTo(curZoom, notifyListener);
					} else {
						callback.zoomTo(zoomEnd, notifyListener);
					}
				}
				try {
					pendingRotateAnimation();
				} catch (InterruptedException e) {
				}
				currentThread = null;
			}
		},"Animatable zooming"); //$NON-NLS-1$
		thread.start();
	}
	
	
	public void startDragging(final float velocityX, final float velocityY, float startX, float startY, 
			final float  endX, final float  endY){
		stopAnimatingSync();
		this.notifyListener = true;
		stopped = false;
		final float animationTime = DRAGGING_ANIMATION_TIME;
		
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				currentThread = Thread.currentThread();
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
					
					callback.dragTo(curX, curY, newX, newY, notifyListener);
					curX = newX;
					curY = newY;
					prevNormalizedTime = normalizedTime;
					try {
						Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
					} catch (InterruptedException e) {
						stopped = true;
					}
				}
				try {
					pendingRotateAnimation();
				} catch (InterruptedException e) {
				}
				currentThread = null;
			}
		},"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startRotate(float rotate) {
		this.targetRotate = rotate;
		if (!isAnimating()) {
			// stopped = false;
			// do we need to kill and recreate the thread? wait would be enough as now it
			// also handles the rotation?
			Thread thread = new Thread(this, "Animatable dragging"); //$NON-NLS-1$
			thread.start();
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
	
	public AnimateDraggingCallback getCallback() {
		return callback;
	}
	
	public void setCallback(AnimateDraggingCallback callback) {
		this.callback = callback;
	}
}

