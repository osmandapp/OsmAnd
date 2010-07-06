package com.osmand.views;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread implements Runnable {
	public interface AnimateDraggingCallback {
		public void dragTo(float curX, float curY, float newX, float newY);
	}
	
	private float curX;
	private float curY;
	private float vx;
	private float vy;
	private float ax;
	private float ay;
	private byte dirX;
	private byte dirY;
	private long time;
	private volatile boolean stopped;
	private final float  a = 0.001f;
	
	private volatile Thread currentThread = null;
	private AnimateDraggingCallback callback = null;

	
	@Override
	public void run() {
		currentThread = Thread.currentThread();
		try {
			while (!stopped && (vx > 0 || vy > 0)) {
				Thread.sleep((long) (40d / (Math.max(vx, vy) + 0.45)));
				long curT = System.currentTimeMillis();
				int dt = (int) (curT - time);
				float newX = vx > 0 ? curX + dirX * vx * dt : curX;
				float newY = vy > 0 ? curY + dirY * vy * dt : curY;
				if (!stopped && callback != null) {
					callback.dragTo(curX, curY, newX, newY);
				}
				vx -= ax * dt;
				vy -= ay * dt;
				time = curT;
				curX = newX;
				curY = newY;
			}
		} catch (InterruptedException e) {
		}
		currentThread = null;
	}
	

	/**
	 * Stop dragging async
	 */
	public void stopDragging(){
		stopped = true;
	}
	
	/**
	 * Stop dragging sync
	 */
	public void stopDraggingSync(){
		// wait until current thread != null
		// TODO implement better method for waintg
		stopped = true;
		while(currentThread != null){}
	}
	
	public void startDragging(float dTime, float  startX, float  startY, float  endX, float  endY){
		vx = Math.abs((endX - startX)/dTime);
		vy = Math.abs((endY - startY)/dTime);
		startDragging(vx, vy, startX, startY, endX, endY);
	}
	
	public void startDragging(float velocityX, float velocityY, float  startX, float  startY, float  endX, float  endY){
		stopDraggingSync();
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

