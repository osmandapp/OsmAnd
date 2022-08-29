package net.osmand.plus.views;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BaseInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen.
 */
public class AnimateDraggingMapThread {

	protected static final Log log = PlatformUtil.getLog(AnimateDraggingMapThread.class);

	private static final float DRAGGING_ANIMATION_TIME = 1200f;
	private static final float ZOOM_ANIMATION_TIME = 250f;
	private static final float ZOOM_MOVE_ANIMATION_TIME = 350f;
	private static final float MOVE_MOVE_ANIMATION_TIME = 900f;
	private static final float NAV_ANIMATION_TIME = 1000f;
	private static final int DEFAULT_SLEEP_TO_REDRAW = 15;

	private static final float MIN_INTERPOLATION_TO_JOIN_ANIMATION = 0.8f;
	private static final float MAX_OX_OY_SUM_DELTA_TO_ANIMATE = 2400f;

	private final OsmandApplication app;
	private final OsmandMapTileView tileView;

	private volatile boolean stopped;
	private volatile Thread currentThread;

	private float targetRotate = -720;
	private double targetLatitude;
	private double targetLongitude;
	private int targetIntZoom;
	private double targetFloatZoom;

	private boolean isAnimatingZoom;
	private boolean isAnimatingMapMove;
	private boolean isAnimatingMapTilt;

	private float interpolation;

	public AnimateDraggingMapThread(@NonNull OsmandMapTileView tileView) {
		this.app = tileView.getApplication();
		this.tileView = tileView;
	}

	private void pendingRotateAnimation() {
		boolean conditionToCountinue;
		if (targetRotate != -720) {
			do {
				conditionToCountinue = false;
				float rotationDiff = MapUtils.unifyRotationDiff(tileView.getRotate(), targetRotate);
				float absDiff = Math.abs(rotationDiff);
				if (absDiff > 0) {
					sleepToRedraw(false);
					if (absDiff < 1) {
						tileView.rotateToAnimate(targetRotate);
					} else {
						conditionToCountinue = true;
						tileView.rotateToAnimate(rotationDiff / 5 + tileView.getRotate());
					}
				}
			} while (conditionToCountinue);
			targetRotate = -720;
		}
	}


	/**
	 * Stop dragging async
	 */
	public void stopAnimating() {
		stopped = true;
	}

	public boolean isAnimating() {
		return currentThread != null && !stopped;
	}

	/**
	 * Stop dragging sync
	 */
	public void stopAnimatingSync() {
		// wait until current thread != null
		stopped = true;
		Thread tt;
		while ((tt = currentThread) != null) {
			try {
				tt.join();
			} catch (Exception ignored) {
			}
		}
	}

	public synchronized void startThreadAnimating(@NonNull Runnable runnable) {
		stopAnimatingSync();
		stopped = false;
		Thread t = new Thread(() -> {
			try {
				suspendUpdate();
				runnable.run();
			} finally {
				currentThread = null;
				resumeUpdate();
			}
		}, "Animating Thread");
		currentThread = t;
		t.start();
	}

	public void startMoving(double finalLat, double finalLon, Pair<Integer, Double> finalZoom,
	                        boolean pendingRotation, Float finalRotation, long movingTime, boolean notifyListener) {
		stopAnimatingSync();

		RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		int startZoom = rb.getZoom();
		double startZoomFP = rb.getZoomFloatPart();
		float startRotation = rb.getRotate();

		int zoom;
		double zoomFP;
		float rotation;
		if (finalZoom != null && finalZoom.first != null && finalZoom.second != null) {
			zoom = finalZoom.first;
			zoomFP = finalZoom.second;
		} else {
			zoom = startZoom;
			zoomFP = startZoomFP;
		}
		if (finalRotation != null) {
			rotation = finalRotation;
		} else {
			rotation = startRotation;
		}

		MapRendererView mapRenderer = tileView.getMapRenderer();
		PointF startPoint = NativeUtilities.getPixelFromLatLon(mapRenderer, rb, startLat, startLon);
		PointF finalPoint = NativeUtilities.getPixelFromLatLon(mapRenderer, rb, finalLat, finalLon);
		float mMoveX = startPoint.x - finalPoint.x;
		float mMoveY = startPoint.y - finalPoint.y;

		boolean skipAnimation = !NativeUtilities.containsLatLon(mapRenderer, rb, finalLat, finalLon);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom, zoomFP, notifyListener);
			tileView.rotateToAnimate(rotation);
			return;
		}

		float animationDuration = Math.max(movingTime, NAV_ANIMATION_TIME);
		startThreadAnimating(() -> {
			isAnimatingMapMove = true;
			setTargetValues(zoom, zoomFP, finalLat, finalLon);

			boolean animateZoom = finalZoom != null && (zoom != startZoom || startZoomFP != 0);
			if (animateZoom) {
				animatingZoomInThread(startZoom, startZoomFP, zoom, zoomFP, NAV_ANIMATION_TIME, notifyListener);
			}

			boolean animateRotation = rotation != startRotation;
			if (pendingRotation) {
				pendingRotateAnimation();
			} else if (animateRotation) {
				animatingRotateInThread(rotation, 500f, notifyListener);
			}

			if (mapRenderer != null) {
				PointI start31 = mapRenderer.getState().getTarget31();
				PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, rb, finalLat, finalLon, false);
				animatingMoveInThread(start31.getX(), start31.getY(), finish31.getX(), finish31.getY(),
						animationDuration, notifyListener, null);
			} else {
				animatingMoveInThread(mMoveX, mMoveY, animationDuration, notifyListener, null);
			}
			isAnimatingMapMove = false;
		});
	}

	public void startMoving(double finalLat, double finalLon, int endZoom, boolean notifyListener) {
		startMoving(finalLat, finalLon, endZoom, notifyListener, false, null, null);
	}

	public void startMoving(double finalLat, double finalLon, int endZoom, boolean notifyListener, boolean allowAnimationJoin,
	                        @Nullable Runnable startAnimationCallback, @Nullable Runnable finishAnimationCallback) {
		boolean wasAnimating = isAnimating();
		stopAnimatingSync();

		if (finishAnimationCallback != null) {
			app.runInUIThread(startAnimationCallback);
		}

		RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		int startZoom = rb.getZoom();
		double startZoomFP = rb.getZoomFloatPart();
		float[] mSt = new float[2];
		int moveZoom = calculateMoveZoom(rb, finalLat, finalLon, mSt);
		boolean skipAnimation = moveZoom == 0;
		// check if animation needed
		skipAnimation = skipAnimation || (Math.abs(moveZoom - startZoom) >= 3 || Math.abs(endZoom - moveZoom) > 3);
		boolean joinAnimation = allowAnimationJoin && interpolation >= MIN_INTERPOLATION_TO_JOIN_ANIMATION;
		if (skipAnimation || wasAnimating && !joinAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(endZoom, 0, notifyListener);
			if (finishAnimationCallback != null) {
				app.runInUIThread(finishAnimationCallback);
			}
			return;
		}
		MapRendererView mapRenderer = tileView.getMapRenderer();
		PointF startPoint = NativeUtilities.getPixelFromLatLon(mapRenderer, rb, startLat, startLon);
		PointF finalPoint = NativeUtilities.getPixelFromLatLon(mapRenderer, rb, finalLat, finalLon);
		float mMoveX = startPoint.x - finalPoint.x;
		float mMoveY = startPoint.y - finalPoint.y;

		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		float normalizedAnimationLength = (Math.abs(mSt[0]) + Math.abs(mSt[1])) / MAX_OX_OY_SUM_DELTA_TO_ANIMATE;
		float animationTime = doNotUseAnimations
				? 1
				: Math.max(450, normalizedAnimationLength * MOVE_MOVE_ANIMATION_TIME);

		startThreadAnimating(() -> {
			isAnimatingMapMove = true;
			setTargetValues(endZoom, 0, finalLat, finalLon);

			if (moveZoom != startZoom) {
				animatingZoomInThread(startZoom, startZoomFP, moveZoom, startZoomFP, doNotUseAnimations
						? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
			}

			if (!stopped) {
				if (mapRenderer != null) {
					PointI start31 = mapRenderer.getState().getTarget31();
					PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, rb, finalLat, finalLon, false);
					animatingMoveInThread(start31.getX(), start31.getY(), finish31.getX(), finish31.getY(),
							animationTime, notifyListener, finishAnimationCallback);
				} else {
					animatingMoveInThread(mMoveX, mMoveY, animationTime, notifyListener, finishAnimationCallback);
				}
			} else if (finishAnimationCallback != null) {
				app.runInUIThread(finishAnimationCallback);
			}
			if (!stopped) {
				tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			}

			if (!stopped && (moveZoom != endZoom || startZoomFP != 0)) {
				animatingZoomInThread(moveZoom, startZoomFP, endZoom, 0, doNotUseAnimations
						? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
			}
			tileView.setFractionalZoom(endZoom, 0, notifyListener);

			pendingRotateAnimation();
			isAnimatingMapMove = false;
		});
	}

	public int calculateMoveZoom(RotatedTileBox rb, double finalLat, double finalLon, float[] mSt) {
		if (rb == null) {
			rb = tileView.getCurrentRotatedTileBox().copy();
		}
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();

		boolean skipAnimation = false;
		if (mSt == null) {
			mSt = new float[2];
		}
		PointF startPoint = NativeUtilities.getPixelFromLatLon(tileView.getMapRenderer(), rb, startLat, startLon);
		PointF finalPoint = NativeUtilities.getPixelFromLatLon(tileView.getMapRenderer(), rb, finalLat, finalLon);
		mSt[0] = startPoint.x - finalPoint.x;
		mSt[1] = startPoint.y - finalPoint.y;
		while (Math.abs(mSt[0]) + Math.abs(mSt[1]) > MAX_OX_OY_SUM_DELTA_TO_ANIMATE) {
			rb.setZoom(rb.getZoom() - 1);
			if (rb.getZoom() <= 4) {
				skipAnimation = true;
			}
			mSt[0] = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
			mSt[1] = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		}
		return skipAnimation ? 0 : rb.getZoom();
	}

	private void animatingRotateInThread(float rotate, float animationTime, boolean notify) {
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		float startRotate = tileView.getRotate();
		float rotationDiff = MapUtils.unifyRotationDiff(startRotate, rotate);
		if (Math.abs(rotationDiff) > 1) {
			long timeMillis = SystemClock.uptimeMillis();
			float normalizedTime;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
				if (normalizedTime > 1f) {
					tileView.rotateToAnimate(rotate);
					break;
				}
				interpolation = interpolator.getInterpolation(normalizedTime);
				tileView.rotateToAnimate(rotationDiff * interpolation + startRotate);
				sleepToRedraw(true);
			}
			resetInterpolation();
		} else {
			tileView.rotateToAnimate(rotate);
		}
	}

	private void animatingMoveInThread(float moveX, float moveY, float animationTime,
									   boolean notify, Runnable finishAnimationCallback) {
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

		float cX = 0;
		float cY = 0;
		long timeMillis = SystemClock.uptimeMillis();
		float normalizedTime;
		while (!stopped) {
			normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
			if (normalizedTime > 1f) {
				break;
			}
			interpolation = interpolator.getInterpolation(normalizedTime);
			float nX = interpolation * moveX;
			float nY = interpolation * moveY;
			tileView.dragToAnimate(cX, cY, nX, nY, notify);
			cX = nX;
			cY = nY;
			sleepToRedraw(true);
		}
		resetInterpolation();
		if (finishAnimationCallback != null) {
			app.runInUIThread(finishAnimationCallback);
		}
	}

	private void animatingMoveInThread(int startX31, int startY31, int finalX31, int finalY31,
	                                   float animationTime, boolean notify, Runnable finishAnimationCallback) {
		BaseInterpolator interpolator = new LinearInterpolator();

		int moveX = finalX31 - startX31;
		int moveY = finalY31 - startY31;

		long timeMillis = SystemClock.uptimeMillis();
		float normalizedTime;
		while (!stopped) {
			normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
			if (normalizedTime > 1f) {
				break;
			}
			interpolation = interpolator.getInterpolation(normalizedTime);
			int nX = (int) (interpolation * moveX);
			int nY = (int) (interpolation * moveY);
			tileView.dragToAnimate(startX31 + nX, startY31 + nY, notify);
			sleepToRedraw(true);
		}
		resetInterpolation();
		if (finishAnimationCallback != null) {
			app.runInUIThread(finishAnimationCallback);
		}
	}

	private void animatingZoomInThread(int zoomStart, double zoomFloatStart,
									   int zoomEnd, double zoomFloatEnd, float animationTime, boolean notifyListener) {
		try {
			RotatedTileBox tb = tileView.getCurrentRotatedTileBox().copy();
			int centerPixelX = tb.getCenterPixelX();
			int centerPixelY = tb.getCenterPixelY();
			isAnimatingZoom = true;
			// could be 0 ]-0.5,0.5], -1 ]-1,0], 1 ]0, 1]  
			int threshold = ((int) (zoomFloatEnd * 2));
			double beginZoom = zoomStart + zoomFloatStart;
			double endZoom = zoomEnd + zoomFloatEnd;

			animationTime *= Math.abs(endZoom - beginZoom);
			// AccelerateInterpolator interpolator = new AccelerateInterpolator(1);
			LinearInterpolator interpolator = new LinearInterpolator();

			long timeMillis = SystemClock.uptimeMillis();
			float normalizedTime;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
				if (normalizedTime > 1f) {
					break;
				}
				interpolation = interpolator.getInterpolation(normalizedTime);
				double curZoom = interpolation * (endZoom - beginZoom) + beginZoom;
				int baseZoom = (int) Math.round(curZoom - 0.5 * threshold);
				double zaAnimate = curZoom - baseZoom;
				tileView.zoomToAnimate(baseZoom, zaAnimate, centerPixelX, centerPixelY, notifyListener);
				sleepToRedraw(true);
			}
			tileView.setFractionalZoom(zoomEnd, zoomFloatEnd, notifyListener);
		} finally {
			resetInterpolation();
			isAnimatingZoom = false;
		}
	}

	public boolean isAnimatingZoom() {
		return isAnimatingZoom;
	}

	public boolean isAnimatingMapMove() {
		return isAnimatingMapMove;
	}

	public boolean isAnimatingMapTilt() {
		return isAnimatingMapTilt;
	}

	public void startZooming(int zoomEnd, double zoomPart, boolean notifyListener) {
		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		float animationTime = doNotUseAnimations ? 0 : ZOOM_ANIMATION_TIME;
		startThreadAnimating(() -> {
			setTargetValues(zoomEnd, zoomPart, tileView.getLatitude(), tileView.getLongitude());
			RotatedTileBox tb = tileView.getCurrentRotatedTileBox().copy();
			animatingZoomInThread(tb.getZoom(), tb.getZoomFloatPart(), zoomEnd, zoomPart, animationTime, notifyListener);

			pendingRotateAnimation();
		});
	}


	public void startDragging(float velocityX, float velocityY,
	                          float startX, float startY, float endX, float endY,
	                          boolean notifyListener) {
		clearTargetValues();
		startThreadAnimating(() -> {
			float curX = endX;
			float curY = endY;
			DecelerateInterpolator interpolator = new DecelerateInterpolator(1);

			long timeMillis = SystemClock.uptimeMillis();
			float normalizedTime;
			float prevNormalizedTime = 0f;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / DRAGGING_ANIMATION_TIME;
				if (normalizedTime >= 1f) {
					break;
				}
				interpolation = interpolator.getInterpolation(normalizedTime);

				float newX = velocityX * (1 - interpolation) * (normalizedTime - prevNormalizedTime) + curX;
				float newY = velocityY * (1 - interpolation) * (normalizedTime - prevNormalizedTime) + curY;

				tileView.dragToAnimate(curX, curY, newX, newY, notifyListener);
				curX = newX;
				curY = newY;
				prevNormalizedTime = normalizedTime;
				sleepToRedraw(true);
			}

			resetInterpolation();
			pendingRotateAnimation();
		});
	}

	public void animateElevationAngleChange(float elevationAngle) {
		stopAnimatingSync();

		float initialElevationAngle = tileView.getElevationAngle();
		float elevationAngleDiff = elevationAngle - initialElevationAngle;

		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		float animationTime = doNotUseAnimations ? 1 : Math.abs(elevationAngleDiff) * 5;

		startThreadAnimating(() -> {
			isAnimatingMapTilt = true;

			LinearInterpolator interpolator = new LinearInterpolator();
			long animationStartTime = SystemClock.uptimeMillis();
			float normalizedTime;
			while (!stopped) {
				normalizedTime = (SystemClock.uptimeMillis() - animationStartTime) / animationTime;
				if (normalizedTime > 1) {
					if (tileView.getElevationAngle() != elevationAngle) {
						tileView.setElevationAngle(elevationAngle);
					}
					break;
				}

				interpolation = interpolator.getInterpolation(normalizedTime);
				float newElevationAngle = initialElevationAngle + elevationAngleDiff * interpolation;

				tileView.setElevationAngle(newElevationAngle);
				tileView.setLatLonAnimate(tileView.getLatitude(), tileView.getLongitude(), false);

				sleepToRedraw(true);
			}

			pendingRotateAnimation();
			resetInterpolation();
			isAnimatingMapTilt = false;
		});

	}

	private void sleepToRedraw(boolean stopIfInterrupted) {
		try {
			Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
		} catch (InterruptedException e) {
			if (stopIfInterrupted) {
				stopped = true;
			}
		}
	}

	private void clearTargetValues() {
		targetIntZoom = 0;
	}

	private void suspendUpdate() {
		MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.suspendSymbolsUpdate();
		}
	}

	private void resumeUpdate() {
		MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			while (!mapRenderer.resumeSymbolsUpdate());
		}
	}

	private void setTargetValues(int zoom, double zoomPart, double lat, double lon) {
		targetIntZoom = zoom;
		targetFloatZoom = zoomPart;
		targetLatitude = lat;
		targetLongitude = lon;
	}

	public void startRotate(float rotate) {
		if (!isAnimating()) {
			clearTargetValues();
			// stopped = false;
			// do we need to kill and recreate the thread? wait would be enough as now it
			// also handles the rotation?
			startThreadAnimating(() -> {
				targetRotate = rotate;
				pendingRotateAnimation();
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

	private void resetInterpolation() {
		interpolation = 0;
	}
}