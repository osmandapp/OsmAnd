package com.osmand.views;

import com.osmand.osm.MapUtils;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread implements Runnable {
	public interface AnimateDraggingCallback {
		
		public void dragTo(float curX, float curY, float newX, float newY, boolean notify);
		
		public void zoomTo(float zoom, boolean notify);
		
		
	}
	
	private boolean animateDrag = true;
	private float curX;
	private float curY;
	private float vx;
	private float vy;
	private float ax;
	private float ay;
	private byte dirX;
	private byte dirY;
	private final float  a = 0.001f;
	
	private long time;
	private volatile boolean stopped;
	
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
	
	private volatile Thread currentThread = null;
	private AnimateDraggingCallback callback = null;
	private boolean notifyListener;

	
	@Override
	public void run() {
		currentThread = Thread.currentThread();
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
					conditionToCountinue = vx > 0 || vy > 0;
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
		} catch (InterruptedException e) {
		}
		currentThread = null;
	}
	

	/**
	 * Stop dragging async
	 */
	public void stopAnimating(){
		stopped = true;
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
	
	public void startZooming(int zoomStart, int zoomEnd){
		stopAnimatingSync();
		this.notifyListener = false;
		if(zoomStart < zoomEnd){
			dirZ = 1;
		} else {
			dirZ = -1;
		}
		curZ = zoomStart;
		endZ = zoomEnd;
		timeZEnd = 600;
		phaseOfMoving = 2;
		animateDrag = false;
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startMoving(double curLat, double curLon, double finalLat, double finalLon, int curZoom, int endZoom, int tileSize, boolean notifyListener){
		stopAnimatingSync();
		this.notifyListener = notifyListener;
		intZ = curZoom;
		moveX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
		moveY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		// todo calculate right with rotated map!!!
		while (Math.abs(moveX) + Math.abs(moveY) > 1200 && intZ > 4) {
			intZ--;
			moveX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
			moveY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		}
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
		
		timeZInt = Math.abs(curZoom - intZ) * 300;
		timeZEnd = 500;
		timeMove = (int) (Math.abs(moveX) + Math.abs(moveY) * 4);
		if(timeMove > 2000){
			timeMove = 2000;
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
	
	public void startDragging(float velocityX, float velocityY, float  startX, float  startY, float  endX, float  endY){
		stopAnimatingSync();
		this.notifyListener = true;
		vx = velocityX;
		vy = velocityY;
		dirX = (byte) (endX > startX ? 1 : -1);
		dirY = (byte) (endY > startY ? 1 : -1);
		animateDrag = true;
		ax = vx * a;
		ay = vy * a;
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public AnimateDraggingCallback getCallback() {
		return callback;
	}
	
	public void setCallback(AnimateDraggingCallback callback) {
		this.callback = callback;
	}
}

