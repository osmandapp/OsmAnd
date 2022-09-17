package net.osmand.plus.views;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BaseInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapAnimator;
import net.osmand.core.jni.MapAnimator.AnimatedValue;
import net.osmand.core.jni.MapAnimator.IAnimation;
import net.osmand.core.jni.MapAnimator.TimingFunction;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

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
	private static final float ROTATION_ANIMATION_TIME = 250f;
	private static final float ROTATION_MOVE_ANIMATION_TIME = 500f;

	private static final float TARGET_MOVE_VELOCITY_LIMIT = 3000f;
	private static final float TARGET_MOVE_DECELERATION = 10000f;
	private static final int SYMBOLS_UPDATE_INTERVAL = 2000;

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

	private SWIGTYPE_p_void userInteractionAnimationKey;
	private SWIGTYPE_p_void locationServicesAnimationKey;

	public AnimateDraggingMapThread(@NonNull OsmandMapTileView tileView) {
		this.app = tileView.getApplication();
		this.tileView = tileView;
	}

	@Nullable
	private MapRendererView getMapRenderer() {
		return tileView.getMapRenderer();
	}

	@Nullable
	private MapAnimator getAnimator() {
		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = mapRenderer != null ? mapRenderer.getAnimator() : null;
		if (mapRenderer != null) {
			if (userInteractionAnimationKey == null) {
				userInteractionAnimationKey = SwigUtilities.getOnSurfaceIconKey(1);
			}
			if (locationServicesAnimationKey == null) {
				locationServicesAnimationKey = SwigUtilities.getOnSurfaceIconKey(2);
			}
		}
		return animator;
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
		MapAnimator animator = getAnimator();
		if (animator != null) {
			animator.pause();
		}
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
				suspendSymbolsUpdate();
				runnable.run();
			} finally {
				currentThread = null;
				resumeSymbolsUpdate();
			}
		}, "Animating Thread");
		currentThread = t;
		t.start();
	}

	public void startMoving(double finalLat, double finalLon, Pair<Integer, Double> finalZoom,
	                        boolean pendingRotation, Float finalRotation, long movingTime,
	                        boolean joinAnimations, boolean notifyListener) {
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

		boolean skipAnimation = movingTime == 0 || !NativeUtilities.containsLatLon(mapRenderer, rb, finalLat, finalLon);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom, zoomFP, notifyListener);
			tileView.rotateToAnimate(rotation);
			return;
		}

		float animationDuration = Math.max(movingTime, NAV_ANIMATION_TIME);

		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			IAnimation targetAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Target);
			IAnimation zoomAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Zoom);

			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Target);

			boolean animateZoom = finalZoom != null && (zoom != startZoom || startZoomFP != 0);
			if (!animateZoom)
				zoomAnimation = null;
			if (zoomAnimation != null) {
				animator.cancelAnimation(zoomAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Zoom);
			}

			IAnimation azimuthAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Azimuth);
			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Azimuth);
			if (azimuthAnimation != null) {
				animator.cancelAnimation(azimuthAnimation);
			}

			boolean animateRotation = rotation != startRotation;
			if (animateRotation)
			{
				animator.animateAzimuthTo(-rotation, ROTATION_MOVE_ANIMATION_TIME / 1000f, TimingFunction.EaseOutQuadratic,
						locationServicesAnimationKey);
			}

			PointI start31 = mapRenderer.getTarget();
			PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
			if (finish31.getX() != start31.getX() || finish31.getY() != start31.getY()) {
				float duration = animationDuration / 1000f;
				if (targetAnimation != null)
				{
					animator.cancelAnimation(targetAnimation);
					if (joinAnimations) {
						duration = targetAnimation.getDuration() - targetAnimation.getTimePassed();
					}
				}
				animator.animateTargetTo(finish31, duration, TimingFunction.Linear, locationServicesAnimationKey);
			}

			if (animateZoom)
			{
				animator.animateZoomTo(zoom + (float) zoomFP, NAV_ANIMATION_TIME / 1000f,
						TimingFunction.EaseOutQuadratic, locationServicesAnimationKey);
			}
		}

		startThreadAnimating(() -> {
			isAnimatingMapMove = true;
			setTargetValues(zoom, zoomFP, finalLat, finalLon);
			boolean animateZoom = finalZoom != null && (zoom != startZoom || startZoomFP != 0);
			boolean animateRotation = rotation != startRotation;

			if (mapRenderer != null && animator != null) {
				if (animateZoom) {
					isAnimatingZoom = true;
				}
				animatingMapAnimator(mapRenderer, animator);
				if (animateZoom) {
					isAnimatingZoom = false;
				}
			} else {
				if (animateZoom) {
					animatingZoomInThread(startZoom, startZoomFP, zoom, zoomFP, NAV_ANIMATION_TIME, notifyListener);
				}

				if (pendingRotation) {
					pendingRotateAnimation();
				} else if (animateRotation) {
					animatingRotateInThread(rotation, ROTATION_MOVE_ANIMATION_TIME, notifyListener);
				}

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

		if (startAnimationCallback != null) {
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
				: Math.max(450f, normalizedAnimationLength * MOVE_MOVE_ANIMATION_TIME);

		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			IAnimation targetAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Target);
			IAnimation zoomAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Zoom);

			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Target);

			boolean animateZoom = endZoom != startZoom || startZoomFP != 0;
			if (!animateZoom)
				zoomAnimation = null;
			if (zoomAnimation != null) {
				animator.cancelAnimation(zoomAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Zoom);
			}

			PointI start31 = mapRenderer.getTarget();
			PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
			if (finish31.getX() != start31.getX() || finish31.getY() != start31.getY()) {
				float duration = animationTime / 1000f;
				if (targetAnimation != null)
				{
					animator.cancelAnimation(targetAnimation);
					duration = targetAnimation.getDuration() - targetAnimation.getTimePassed();
				}
				animator.animateTargetTo(finish31, duration, TimingFunction.Linear, locationServicesAnimationKey);
			}

			if (animateZoom)
			{
				animator.animateZoomTo((float) endZoom, ZOOM_MOVE_ANIMATION_TIME / 1000f,
						TimingFunction.EaseOutQuadratic, locationServicesAnimationKey);
			}
		}

		startThreadAnimating(() -> {
			isAnimatingMapMove = true;
			setTargetValues(endZoom, 0, finalLat, finalLon);

			boolean animateZoom = endZoom != startZoom || startZoomFP != 0;
			if (mapRenderer != null && animator != null) {
				if (animateZoom) {
					isAnimatingZoom = true;
				}
				animatingMapAnimator(mapRenderer, animator);
				if (animateZoom) {
					isAnimatingZoom = false;
				}
				if (finishAnimationCallback != null) {
					app.runInUIThread(finishAnimationCallback);
				}
				if (!stopped) {
					tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
				}
			} else {
				if (moveZoom != startZoom) {
					animatingZoomInThread(startZoom, startZoomFP, moveZoom, startZoomFP, doNotUseAnimations
							? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}

				if (!stopped) {
					if (mapRenderer != null) {
						PointI start31 = mapRenderer.getTarget();
						PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
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
			}
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

	private void animatingMapAnimator(@NonNull MapRendererView mapRenderer, @NonNull MapAnimator animator) {
		long startTime = SystemClock.uptimeMillis();
		long currTime = startTime;
		long prevTime = currTime;

		int targetIntZoom = this.targetIntZoom;
		double targetFloatZoom = this.targetFloatZoom;

		PointI initTarget31 = mapRenderer.getTarget();
		float initZoom = mapRenderer.getZoom();
		int zoomThreshold = ((int) (targetFloatZoom * 2));
		float initAzimuth = mapRenderer.getAzimuth();

		boolean animateTarget = false;
		boolean animateZoom = false;
		boolean animateAzimuth = false;

		mapRenderer.setSymbolsUpdateInterval(SYMBOLS_UPDATE_INTERVAL);
		if (!stopped) {
			animator.resume();
		}
		RotatedTileBox tb = tileView.getCurrentRotatedTileBox();
		while (!stopped) {
			currTime = SystemClock.uptimeMillis();
			boolean animationFinished = animator.update((currTime - prevTime) / 1000f);
			prevTime = currTime;

			PointI target31 = mapRenderer.getTarget();
			float azimuth = mapRenderer.getAzimuth();
			float zoom = mapRenderer.getZoom();

			if (!animateTarget) {
				animateTarget = initTarget31.getX() != target31.getX() || initTarget31.getY() != target31.getY();
			}
			if (!animateZoom) {
				animateZoom = initZoom != zoom;
			}
			if (!animateAzimuth) {
				animateAzimuth = initAzimuth != azimuth;
			}

			if (!stopped && animateTarget) {
				tb.setLatLonCenter(MapUtils.get31LatitudeY(target31.getY()), MapUtils.get31LongitudeX(target31.getX()));
			}
			if (!stopped && animateZoom) {
				int baseZoom = (int) Math.round(zoom - 0.5 * zoomThreshold);
				double zaAnimate = zoom - baseZoom;
				tb.setZoomAndAnimation(baseZoom, zaAnimate, tb.getZoomFloatPart());
			}
			if (!stopped && animateAzimuth) {
				tb.setRotate(-azimuth);
			}

			if (animationFinished) {
				break;
			}
			mapRenderer.requestRender();
			sleepToRedraw(true);
		}
		if (animateZoom) {
			mapRenderer.setZoom(targetIntZoom + (float) targetFloatZoom);
			tb.setZoomAndAnimation(targetIntZoom, 0, targetFloatZoom);
		}
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

		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.pause();
			animator.cancelAllAnimations();

			animator.animateZoomTo(zoomEnd + (float) zoomPart,
					animationTime / 1000f,
					TimingFunction.Linear,
					userInteractionAnimationKey);
		}

		startThreadAnimating(() -> {
			setTargetValues(zoomEnd, zoomPart, tileView.getLatitude(), tileView.getLongitude());
			if (mapRenderer != null && animator != null) {
				isAnimatingZoom = true;
				animatingMapAnimator(mapRenderer, animator);
				isAnimatingZoom = false;
			} else {
				RotatedTileBox tb = tileView.getCurrentRotatedTileBox().copy();
				animatingZoomInThread(tb.getZoom(), tb.getZoomFloatPart(), zoomEnd, zoomPart, animationTime, notifyListener);

				pendingRotateAnimation();
			}
		});
	}


	public void startDragging(float velocityX, float velocityY,
	                          float startX, float startY, float endX, float endY,
	                          boolean notifyListener) {
		clearTargetValues();

		/*
		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			velocityX = velocityX > 0
					? Math.min(velocityX, TARGET_MOVE_VELOCITY_LIMIT)
					: Math.max(velocityX, -TARGET_MOVE_VELOCITY_LIMIT);
			velocityY = velocityY > 0
					? Math.min(velocityY, TARGET_MOVE_VELOCITY_LIMIT)
					: Math.max(velocityY, -TARGET_MOVE_VELOCITY_LIMIT);

			MapRendererState state = mapRenderer.getState();

			// Taking into account current zoom, get how many 31-coordinates there are in 1 point
            long tileSize31 = (1L << (31 - state.getZoomLevel().ordinal()));
            double scale31 = tileSize31 / mapRenderer.tileSizeOnScreenInPixels;

			// Take into account current azimuth and reproject to map space (points)
			double angle = Math.toRadians(state.getAzimuth());
            double cosAngle = Math.cos(angle);
            double sinAngle = Math.sin(angle);

			double velocityInMapSpaceX = velocityX * cosAngle - velocityY * sinAngle;
			double velocityInMapSpaceY = velocityX * sinAngle + velocityY * cosAngle;

			// Rescale speed to 31 coordinates
			PointD velocity = new PointD(-velocityInMapSpaceX * scale31, -velocityInMapSpaceY * scale31);
			animator.animateTargetWith(velocity,
					new PointD(TARGET_MOVE_DECELERATION * scale31, TARGET_MOVE_DECELERATION * scale31),
					userInteractionAnimationKey);
		}
		*/

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

	private void suspendSymbolsUpdate() {
		MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.suspendSymbolsUpdate();
		}
	}

	private void resumeSymbolsUpdate() {
		MapRendererView mapRenderer = tileView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.resumeSymbolsUpdate();
		}
	}

	private void setTargetValues(int zoom, double zoomPart, double lat, double lon) {
		targetIntZoom = zoom;
		targetFloatZoom = zoomPart;
		targetLatitude = lat;
		targetLongitude = lon;
	}

	public void startRotate(float rotate) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapAnimator animator = mapRenderer.getAnimator();
			animator.pause();

			animator.cancelCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Azimuth);
			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Azimuth);
			animator.animateAzimuthTo(-rotate, ROTATION_ANIMATION_TIME / 1000f, TimingFunction.Linear,
					locationServicesAnimationKey);

			startThreadAnimating(() -> animatingMapAnimator(mapRenderer, animator));
		} else {
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