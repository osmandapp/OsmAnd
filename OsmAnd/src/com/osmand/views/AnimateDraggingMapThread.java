package com.osmand.views;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread implements Runnable {
	public interface AnimateDraggingCallback {
		
		public void dragTo(float curX, float curY, float newX, float newY);
		
		public void zoomTo(float zoom);
		
	}
	private int endZ;
	private float curZ;
	private int timeZ;
	
	private float curX;
	private float curY;
	private float vx;
	private float vy;
	private float ax;
	private float ay;
	private byte dirX;
	private byte dirY;
	private byte dirZ;
	private long time;
	private volatile boolean stopped;
	private final float  a = 0.001f;
	
	private volatile Thread currentThread = null;
	private AnimateDraggingCallback callback = null;

	
	@Override
	public void run() {
		currentThread = Thread.currentThread();
		try {
			while (!stopped && (vx > 0 || vy > 0 || curZ != endZ)) {
				// calculate sleep
				long sleep = 0;
				if(vx > 0 || vy > 0){
					sleep = (long) (40d / (Math.max(vx, vy) + 0.45));
				} else {
					sleep = 80;
				}
				Thread.sleep(sleep);
				long curT = System.currentTimeMillis();
				int dt = (int) (curT - time);
				float newX = vx > 0 ? curX + dirX * vx * dt : curX;
				float newY = vy > 0 ? curY + dirY * vy * dt : curY;
				
				float newZ = curZ;
				if(timeZ > 0){
					newZ = newZ + dirZ * (float)dt / timeZ;
					if(dirZ > 0 == newZ > endZ){
						newZ = endZ;
					}
				}
				if (!stopped && callback != null) {
					callback.dragTo(curX, curY, newX, newY);
					if(newZ > 0){
						callback.zoomTo(newZ);
					}
				}
				vx -= ax * dt;
				vy -= ay * dt;
				time = curT;
				curX = newX;
				curY = newY;
				curZ = newZ;
			}
			if(curZ != endZ && endZ > 0){
				callback.zoomTo(endZ);
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
		if(zoomStart < zoomEnd){
			dirZ = 1;
		} else {
			dirZ = -1;
		}
		curZ = zoomStart;
		endZ = zoomEnd;
		timeZ = 600;
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startDragging(float dTime, float  startX, float  startY, float  endX, float  endY){
		vx = Math.abs((endX - startX)/dTime);
		vy = Math.abs((endY - startY)/dTime);
		startDragging(vx, vy, startX, startY, endX, endY);
	}
	
	public void startDragging(float velocityX, float velocityY, float  startX, float  startY, float  endX, float  endY){
		stopAnimatingSync();
		vx = velocityX;
		vy = velocityY;
		dirX = (byte) (endX > startX ? 1 : -1);
		dirY = (byte) (endY > startY ? 1 : -1);
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

