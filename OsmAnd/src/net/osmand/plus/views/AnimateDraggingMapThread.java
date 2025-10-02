package net.osmand.plus.views;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.*;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView.TouchListener;
import net.osmand.plus.views.Zoom.ComplexZoom;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.EnumSet;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen.
 */
public class AnimateDraggingMapThread implements TouchListener {

	protected static final Log log = PlatformUtil.getLog(AnimateDraggingMapThread.class);

	private static final float DRAGGING_ANIMATION_TIME = 1200f;
	public static final float ZOOM_ANIMATION_TIME = 250f;
	private static final float ZOOM_MOVE_ANIMATION_TIME = 350f;
	private static final float MOVE_MOVE_ANIMATION_TIME = 900f;
	public static final float NAV_ANIMATION_TIME = 1000f;
	private static final int DEFAULT_SLEEP_TO_REDRAW = 15;
	private static final float ROTATION_ANIMATION_TIME = 250f;
	private static final float ROTATION_MOVE_ANIMATION_TIME = 1000f;
	private static final float SKIP_ANIMATION_TIMEOUT = 10000f;
	public static final float SKIP_ANIMATION_DP_THRESHOLD = 20f;
	public static final float TILT_ANIMATION_TIME = 400f;

	public static final int TARGET_NO_ROTATION = -720;

	private static final float TARGET_MOVE_VELOCITY_LIMIT = 4000f;
	private static final float TARGET_MOVE_DECELERATION = 8000f;

	private static final float MIN_INTERPOLATION_TO_JOIN_ANIMATION = 0.8f;
	private static final float MAX_OX_OY_SUM_DELTA_TO_ANIMATE = 2400f;

	private final OsmandApplication app;
	private final OsmandMapTileView tileView;

	private volatile boolean stopped;
	private volatile Thread currentThread;

	private float targetRotate = TARGET_NO_ROTATION;
	private double targetLatitude;
	private double targetLongitude;
	private int targetIntZoom;
	private double targetFloatZoom;

	private boolean animatingMapZoom;
	private boolean animatingMapMove;
	private boolean animatingMapRotation;
	private boolean animatingMapTilt;
	private boolean userAnimationsActive;
	private volatile boolean inconsistentMapTarget;
	private volatile boolean targetChanged;
	private volatile int targetPixelX;
	private volatile int targetPixelY;
	private volatile boolean animationsDisabled;

	private float interpolation;

	private SWIGTYPE_p_void userInteractionAnimationKey;
	private SWIGTYPE_p_void locationServicesAnimationKey;

	public AnimateDraggingMapThread(@NonNull OsmandMapTileView tileView) {
		this.app = tileView.getApplication();
		this.tileView = tileView;
		this.tileView.addTouchListener(this);
	}

	@Nullable
	private MapRendererView getMapRenderer() {
		return tileView.getMapRenderer();
	}

	@Nullable
	private MapAnimator getAnimator() {
		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = mapRenderer != null ? mapRenderer.getMapAnimator() : null;
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
		if (targetRotate != TARGET_NO_ROTATION) {
			do {
				animatingMapRotation = true;
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
			targetRotate = TARGET_NO_ROTATION;
			animatingMapRotation = false;
		}
	}

	/**
	 * Make map target in sync with current map location coordinates and elevation
	 */
	public void resetMapTarget() {
		MapRendererView renderer = getMapRenderer();
		if (renderer != null && inconsistentMapTarget) {
			inconsistentMapTarget = false;
			if (targetChanged) {
				targetChanged = false;
				renderer.resetMapTargetPixelCoordinates(new PointI(targetPixelX, targetPixelY));
			} else {
				renderer.resetMapTarget();
			}
			tileView.setCurrentZoom();
		}
	}

	public void invalidateMapTarget() {
		inconsistentMapTarget = true;
	}

	/**
	 * Block animations
	 */
	private void blockAnimations() {
		stopAnimatingSync();
		animationsDisabled = true;
	}

	/**
	 * Allow animations
	 */
	private void allowAnimations() {
		animationsDisabled = false;
	}

	public void toggleAnimations() {
		boolean mapActivityActive = app.getSettings().MAP_ACTIVITY_ENABLED;
		boolean carSessionActive = false;
		NavigationSession navigationSession = app.getCarNavigationSession();
		if (navigationSession != null) {
			SurfaceRenderer surfaceRenderer = navigationSession.getNavigationCarSurface();
			if (!app.useOpenGlRenderer() || (surfaceRenderer != null && surfaceRenderer.hasOffscreenRenderer()))
				carSessionActive = true;
		}
		if (mapActivityActive || carSessionActive) {
			allowAnimations();
		} else {
			blockAnimations();
		}
	}

	/**
	 * Stop dragging async
	 */
	public void stopAnimating() {
		stopped = true;
		resetMapTarget();
	}

	public boolean isAnimating() {
		return currentThread != null && !stopped;
	}

	/**
	 * Stop dragging sync
	 */
	public void stopAnimatingSync() {
		// wait until current thread != null
		MapRendererView renderer = getMapRenderer();
		if (renderer != null) {
			renderer.pauseMapAnimation();
		}
		stopped = true;
		resetMapTarget();
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
		}, "Animating Map Thread");
		currentThread = t;
		t.start();
	}

	public void animateToPreview(double finalLat, double finalLon, @NonNull Zoom zoom,
								 float finalRotation, float elevationAngle, long animationDuration, boolean notifyListener) {
		if (!animationsDisabled) {
			stopAnimatingSync();
		}
		boolean skipAnimation = animationDuration == 0;
		final MapRendererView mapRenderer = tileView.getMapRenderer();
		MapAnimator animator = getAnimator();
		if (skipAnimation || animationsDisabled || mapRenderer == null || animator == null) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom.getBaseZoom(), zoom.getZoomFloatPart(), notifyListener);
			tileView.setElevationAngle(elevationAngle);
			tileView.rotateToAnimate(finalRotation);
		} else {
			EnumSet<AnimatedValue> set = EnumSet.of(AnimatedValue.Target, AnimatedValue.ElevationAngle, AnimatedValue.Azimuth, AnimatedValue.Zoom);
			for (AnimatedValue a : set) {
				IAnimation animation = animator.getCurrentAnimation(locationServicesAnimationKey, a);
				if (animation != null) {
					animator.cancelAnimation(animation);
				}
				animation = animator.getCurrentAnimation(userInteractionAnimationKey, a);
				if (animation != null) {
					animator.cancelAnimation(animation);
				}
			}
			float fullDuration = animationDuration / 1000f ;
			float rotateDuration = fullDuration / 4;
			float duration = fullDuration - rotateDuration;
			PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
			startThreadAnimating(() -> {
				animator.animateAzimuthTo(finalRotation, rotateDuration, TimingFunction.Linear, userInteractionAnimationKey);
				animatingMapRotation = true;
				animatingMapAnimator();
				animatingMapRotation = false;

				animatingMapZoom = true;
				animatingMapMove = true;
				animatingMapTilt = true;
				animator.animateTargetTo(finish31, duration, TimingFunction.EaseInQuadratic, userInteractionAnimationKey);
				animator.animateZoomTo(zoom.getBaseZoom() + zoom.getZoomFloatPart(), duration,
						TimingFunction.EaseOutQuadratic, userInteractionAnimationKey);
				animator.animateElevationAngleTo(elevationAngle, duration,
						TimingFunction.Linear, userInteractionAnimationKey);
				setTargetValues(zoom.getBaseZoom(), zoom.getZoomFloatPart(), finalLat, finalLon);
				animatingMapAnimator();
				animatingMapZoom = false;
				animatingMapMove = false;
				animatingMapTilt = false;
				animatingMapRotation = false;
			});
		}

	}

	public void startMoving(double finalLat, double finalLon, @Nullable Pair<ComplexZoom, Float> zoomParams,
							boolean pendingRotation, Float finalRotation, float elevationAngle, long movingTime,
							boolean notifyListener, @Nullable Runnable finishAnimationCallback) {
		if (animationsDisabled)
			return;

		stopAnimatingSync();

		RotatedTileBox rb = tileView.getRotatedTileBox();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		int startZoom = rb.getZoom();
		double startZoomFP = rb.getZoomFloatPart();
		float startRotation = rb.getRotate();
		float startElevationAngle = tileView.getElevationAngle();

		int zoom;
		double zoomFP;
		float rotation;
		if (zoomParams != null) {
			zoom = zoomParams.first.base;
			zoomFP = zoomParams.first.floatPart;
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
		float mMoveX;
		float mMoveY;
		if (mapRenderer == null) {
			PointF startPoint = NativeUtilities.getPixelFromLatLon(null, rb, startLat, startLon);
			PointF finalPoint = NativeUtilities.getPixelFromLatLon(null, rb, finalLat, finalLon);
			mMoveX = startPoint.x - finalPoint.x;
			mMoveY = startPoint.y - finalPoint.y;
		} else {
			mMoveX = 0;
			mMoveY = 0;
		}

		boolean skipAnimation = movingTime == 0 || movingTime > SKIP_ANIMATION_TIMEOUT
				|| !NativeUtilities.containsLatLon(mapRenderer, rb, finalLat, finalLon);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom, zoomFP, notifyListener);
			if (elevationAngle != 0 && elevationAngle != startElevationAngle) {
				tileView.setElevationAngle(elevationAngle);
			}
			tileView.rotateToAnimate(rotation);
			if (finishAnimationCallback != null) {
				finishAnimationCallback.run();
			}
			return;
		}

		float animationDuration = Math.max(movingTime, NAV_ANIMATION_TIME / 4);

		boolean animateZoom = zoomParams != null && (zoom != startZoom || zoomFP != startZoomFP);
		boolean animateElevation = elevationAngle != 0 && elevationAngle != startElevationAngle;
		boolean allowRotationAfterReset = app.getMapViewTrackingUtilities().allowRotationAfterReset();
		float rotationDiff = finalRotation != null
				? Math.abs(MapUtils.unifyRotationDiff(rotation, startRotation)) : 0;
		boolean animateRotation = rotationDiff > 0.1 && allowRotationAfterReset;
		boolean animateTarget;

		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			IAnimation targetAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Target);
			IAnimation zoomAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Zoom);
			IAnimation elevatonAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.ElevationAngle);

			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Target);

			if (!animateZoom)
				zoomAnimation = null;
			if (zoomAnimation != null) {
				animator.cancelAnimation(zoomAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Zoom);
			}

			if (!animateElevation)
				elevatonAnimation = null;
			if (elevatonAnimation != null) {
				animator.cancelAnimation(elevatonAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.ElevationAngle);
			}

			IAnimation azimuthAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Azimuth);
			if (finalRotation != null) {
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Azimuth);
				if (azimuthAnimation != null) {
					animator.cancelAnimation(azimuthAnimation);
				}
			}

			if (animateRotation) {
				animator.animateAzimuthTo(-rotation, Math.max(animationDuration, ROTATION_MOVE_ANIMATION_TIME) / 1000f,
						TimingFunction.Linear,
						locationServicesAnimationKey);
			}

			PointI start31 = mapRenderer.getTarget();
			PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
			animateTarget = Math.abs(finish31.getX() - start31.getX()) > 5 || Math.abs(finish31.getY() - start31.getY()) > 5;
			if (animateTarget) {
				float duration = animationDuration / 1000f;
				if (targetAnimation != null) {
					animator.cancelAnimation(targetAnimation);
				}
				animator.animateTargetTo(finish31, duration, TimingFunction.Linear, locationServicesAnimationKey);
			}

			if (animateZoom) {
				animator.animateZoomTo(zoom + (float) zoomFP, zoomParams.second / 1000f,
						TimingFunction.EaseOutQuadratic, locationServicesAnimationKey);
			}
			if (!animateZoom) {
				tileView.setFractionalZoom(zoom, zoomFP, notifyListener);
			}
			if (animateElevation) {
				animator.animateElevationAngleTo(elevationAngle, TILT_ANIMATION_TIME / 1000f,
						TimingFunction.Linear, locationServicesAnimationKey);
			}
			if (!animateRotation && finalRotation != null && allowRotationAfterReset) {
				tileView.rotateToAnimate(rotation);
			}
			if (!animateTarget) {
				tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			}
		}

		startThreadAnimating(() -> {
			animatingMapMove = true;
			if (mapRenderer != null) {
				setTargetValues(0, 0, finalLat, finalLon);
			} else {
				setTargetValues(zoom, zoomFP, finalLat, finalLon);
			}

			if (mapRenderer != null) {
				if (animateZoom) {
					animatingMapZoom = true;
				}
				if (animateElevation) {
					animatingMapTilt = true;
				}
				if (animateRotation) {
					targetRotate = rotation;
					animatingMapRotation = true;
				}
				animatingMapAnimator();
				if (animateZoom) {
					animatingMapZoom = false;
				}
				if (animateElevation) {
					animatingMapTilt = false;
				}
				if (animateRotation) {
					animatingMapRotation = false;
					targetRotate = TARGET_NO_ROTATION;
				}
				if (!stopped && finishAnimationCallback != null) {
					finishAnimationCallback.run();
				}
			} else {
				if (animateZoom) {
					animatingZoomInThread(startZoom, startZoomFP, zoom, zoomFP, zoomParams.second, notifyListener);
				}

				if (pendingRotation) {
					pendingRotateAnimation();
				} else if (animateRotation) {
					animatingRotateInThread(rotation, ROTATION_MOVE_ANIMATION_TIME, notifyListener);
				}

				animatingMoveInThread(mMoveX, mMoveY, animationDuration, notifyListener, null);
			}
			animatingMapMove = false;
		});
	}

	public void startMoving(double finalLat, double finalLon) {
		startMoving(finalLat, finalLon, tileView.getZoom());
	}

	public void startMoving(double finalLat, double finalLon, int finalIntZoom) {
		startMoving(finalLat, finalLon, finalIntZoom, 0.0f);
	}

	public void startMoving(double finalLat, double finalLon, int finalIntZoom, float finalZoomFloatPart) {
		Runnable startAnimationCallback = () -> app.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
		startMoving(finalLat, finalLon, finalIntZoom, finalZoomFloatPart, true,
				false, startAnimationCallback, null);
	}

	public void startMoving(double finalLat, double finalLon, int endZoom, float endZoomFloatPart,
							boolean notifyListener, boolean allowAnimationJoin,
							@Nullable Runnable startAnimationCallback, @Nullable Runnable finishAnimationCallback) {
		if (animationsDisabled)
			return;

		boolean wasAnimating = isAnimating();
		stopAnimatingSync();

		if (startAnimationCallback != null) {
			startAnimationCallback.run();
		}

		RotatedTileBox rb = tileView.getRotatedTileBox();
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
			tileView.setFractionalZoom(endZoom, endZoomFloatPart, notifyListener);
			if (finishAnimationCallback != null) {
				finishAnimationCallback.run();
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

			boolean animateZoom = Math.abs(endZoom + endZoomFloatPart - startZoom - startZoomFP) > 0.001f;
			if (!animateZoom) {
				zoomAnimation = null;
			}
			if (zoomAnimation != null) {
				animator.cancelAnimation(zoomAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Zoom);
			}

			PointI start31 = mapRenderer.getTarget();
			PointI finish31 = NativeUtilities.calculateTarget31(mapRenderer, finalLat, finalLon, false);
			if (finish31.getX() != start31.getX() || finish31.getY() != start31.getY()) {
				float duration = animationTime / 1000f;
				if (targetAnimation != null) {
					animator.cancelAnimation(targetAnimation);
					duration = targetAnimation.getDuration() - targetAnimation.getTimePassed();
				}
				if (animateZoom) {
					animator.animateZoomToAndPan(endZoom + endZoomFloatPart, finish31, duration, TimingFunction.EaseOutQuadratic, locationServicesAnimationKey);
				} else {
					animator.animateTargetTo(finish31, duration, TimingFunction.Linear, locationServicesAnimationKey);
				}
			} else if (animateZoom) {
				animator.animateZoomTo(endZoom + endZoomFloatPart, ZOOM_MOVE_ANIMATION_TIME / 1000f,
						TimingFunction.EaseOutQuadratic, locationServicesAnimationKey);
			}
		}

		startThreadAnimating(() -> {
			animatingMapMove = true;
			setTargetValues(endZoom, endZoomFloatPart, finalLat, finalLon);

			boolean animateZoom = endZoom != startZoom || startZoomFP != endZoomFloatPart;
			if (mapRenderer != null) {
				if (targetChanged)
					invalidateMapTarget();
				if (animateZoom) {
					animatingMapZoom = true;
				}
				animatingMapAnimator();
				if (animateZoom) {
					animatingMapZoom = false;
				}
				if (finishAnimationCallback != null) {
					finishAnimationCallback.run();
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
					animatingMoveInThread(mMoveX, mMoveY, animationTime, notifyListener, finishAnimationCallback);
				}
				if (finishAnimationCallback != null) {
					finishAnimationCallback.run();
				}
				if (!stopped) {
					tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
				}

				if (!stopped && (moveZoom != endZoom || startZoomFP != endZoomFloatPart)) {
					animatingZoomInThread(moveZoom, startZoomFP, endZoom, 0, doNotUseAnimations
							? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				tileView.setFractionalZoom(endZoom, endZoomFloatPart, notifyListener);

				pendingRotateAnimation();
			}
			animatingMapMove = false;
		});
	}

	public int calculateMoveZoom(RotatedTileBox rb, double finalLat, double finalLon, float[] mSt) {
		if (rb == null) {
			rb = tileView.getRotatedTileBox();
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

	private void animatingMapAnimator() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		boolean userAnimationsActive = false;
		MapAnimator animator = getAnimator();
		if (animator != null) {
			QListIAnimation userAnimations = animator.getAnimations(userInteractionAnimationKey);
			userAnimationsActive = !userAnimations.isEmpty();
			this.userAnimationsActive = userAnimationsActive;
			if (userAnimationsActive) {
				tileView.applyMaximumFrameRate(mapRenderer);
			}
		}
		int targetIntZoom = this.targetIntZoom;
		double targetFloatZoom = this.targetFloatZoom;

		PointI initFlatTarget31 = mapRenderer.getState().getTarget31();
		float initZoom = mapRenderer.getZoom();
		float initAzimuth = mapRenderer.getAzimuth();
		float initElevationAngle = mapRenderer.getElevationAngle();

		boolean animateTarget = false;
		boolean animateZoom = false;
		boolean animateAzimuth = false;
		boolean animateElevationAngle = false;

		if (!stopped) {
			mapRenderer.resumeMapAnimation();
		}
		RotatedTileBox tb = tileView.getCurrentRotatedTileBox();
		while (!stopped) {
			mapRenderer.requestRender();
			sleepToRedraw(true);
			mapRenderer = getMapRenderer();
			if (mapRenderer == null) {
				break;
			}
			PointI target31 = mapRenderer.getTarget();
			PointI flatTarget31 = mapRenderer.getState().getTarget31();
			float azimuth = mapRenderer.getAzimuth();
			float zoom = mapRenderer.getZoom();
			float elevationAngle = mapRenderer.getElevationAngle();

			if (!animateTarget) {
				animateTarget = initFlatTarget31.getX() != flatTarget31.getX()
						|| initFlatTarget31.getY() != flatTarget31.getY();
			}
			if (!animateZoom) {
				animateZoom = initZoom != zoom;
			}
			if (!animateAzimuth) {
				animateAzimuth = initAzimuth != azimuth;
			}
			if (!animateElevationAngle) {
				animateElevationAngle = initElevationAngle != elevationAngle;
			}

			if (!stopped && animateTarget) {
				tb.setLatLonCenter(MapUtils.get31LatitudeY(target31.getY()),
						MapUtils.get31LongitudeX(target31.getX()));
			}
			if (!stopped && animateZoom) {
				int zoomBase = Math.round(zoom);
				double zoomAnimation = zoom - zoomBase - tb.getZoomFloatPart();
				tb.setZoomAndAnimation(zoomBase, zoomAnimation, tb.getZoomFloatPart());
			}
			if (!stopped && animateAzimuth) {
				tb.setRotate(-azimuth);
			}
			if (!stopped && animateElevationAngle) {
				tileView.setElevationAngle(elevationAngle);
			}

			if (mapRenderer.isMapAnimationFinished()) {
				break;
			}
		}
		this.userAnimationsActive = false;
		if (userAnimationsActive && mapRenderer != null) {
			tileView.applyMaximumFrameRate(mapRenderer);
		}
		if (animateZoom && mapRenderer != null) {
			if (targetIntZoom > 0) {
				mapRenderer.setZoom(targetIntZoom + (float) targetFloatZoom);
				tb.setZoomAndAnimation(targetIntZoom, 0, targetFloatZoom);
			} else {
				tb.setZoomAndAnimation(tb.getZoom(), 0, tb.getZoomFloatPart() + tb.getZoomAnimation());
			}
		}
		tileView.refreshMap();
	}

	private void animatingRotateInThread(float rotate, float animationTime, boolean notify) {
		AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		float startRotate = tileView.getRotate();
		float rotationDiff = MapUtils.unifyRotationDiff(startRotate, rotate);
		if (Math.abs(rotationDiff) > 1) {
			animatingMapRotation = true;
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
			animatingMapRotation = false;
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

	private void animatingZoomInThread(int zoomStart, double zoomFloatStart,
									   int zoomEnd, double zoomFloatEnd, float animationTime, boolean notifyListener) {
		try {
			RotatedTileBox tb = tileView.getRotatedTileBox();
			int centerPixelX = tb.getCenterPixelX();
			int centerPixelY = tb.getCenterPixelY();
			animatingMapZoom = true;
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
			animatingMapZoom = false;
		}
	}

	public boolean isAnimatingMapZoom() {
		return animatingMapZoom;
	}

	public boolean isAnimatingMapMove() {
		return animatingMapMove;
	}

	public boolean isAnimatingMapRotation() {
		return animatingMapRotation;
	}

	public boolean isAnimatingMapTilt() {
		return animatingMapTilt;
	}

	public boolean isUserAnimationsActive() {
		return userAnimationsActive;
	}

	public void startZooming(int zoomEnd, double zoomPart, @Nullable LatLon zoomingLatLon, boolean notifyListener) {
		if (animationsDisabled)
			return;

		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		float animationTime = doNotUseAnimations ? 0 : ZOOM_ANIMATION_TIME;
		double targetLat = tileView.getLatitude();
		double targetLon = tileView.getLongitude();

		resetMapTarget();
		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.pause();

			float duration = animationTime / 1000f;

			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Target);
			IAnimation targetAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Target);
			if (targetAnimation != null) {
				if (zoomingLatLon == null) {
					targetLat = targetLatitude;
					targetLon = targetLongitude;
				} else {
					animator.cancelAnimation(targetAnimation);
				}
				duration = Math.min(duration, targetAnimation.getDuration() - targetAnimation.getTimePassed());
			}

			IAnimation zoomAnimation = animator.getCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Zoom);
			if (zoomAnimation != null) {
				animator.cancelAnimation(zoomAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Zoom);
			}

			if (zoomingLatLon != null) {
				if (!targetChanged) {
					// Remember last target position before it is changed with animation
					PointI targetPixelPosition = mapRenderer.getTargetScreenPosition();
					targetPixelX = targetPixelPosition.getX();
					targetPixelY = targetPixelPosition.getY();
					targetChanged = true;
				}

				PointI zoomPosition31 = NativeUtilities.calculateTarget31(mapRenderer, zoomingLatLon.getLatitude(), zoomingLatLon.getLongitude(), false);
				PointI zoomPixel = new PointI();
				mapRenderer.getElevatedPointFromLocation(zoomPosition31, zoomPixel, false);
				mapRenderer.setMapTarget(zoomPixel, zoomPosition31);
			}

			if (duration > 0) {
				animator.animateZoomTo(zoomEnd + (float) zoomPart,
						duration,
						TimingFunction.Linear,
						userInteractionAnimationKey);
			} else {
				tileView.setFractionalZoom(zoomEnd, zoomPart, notifyListener);
			}
		}

		double finalLat = targetLat;
		double finalLon = targetLon;
		startThreadAnimating(() -> {
			setTargetValues(zoomEnd, zoomPart, finalLat, finalLon);
			if (mapRenderer != null) {
				if (targetChanged) {
					invalidateMapTarget();
				}

				animatingMapZoom = true;
				animatingMapAnimator();
				animatingMapZoom = false;

				if (!stopped && zoomingLatLon != null) {
					tileView.setLatLonAnimate(zoomingLatLon.getLatitude(), zoomingLatLon.getLongitude(), notifyListener);
				}
			} else {
				RotatedTileBox tb = tileView.getRotatedTileBox();
				animatingZoomInThread(tb.getZoom(), tb.getZoomFloatPart(), zoomEnd, zoomPart, animationTime, notifyListener);

				pendingRotateAnimation();
			}
		});
	}

	public void startDragging(float velocityX, float velocityY,
							  float startX, float startY, float endX, float endY,
							  boolean notifyListener) {
		if (animationsDisabled)
			return;

		clearTargetValues();

		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			float newVelocityX = velocityX > 0
					? Math.min(velocityX * 3, TARGET_MOVE_VELOCITY_LIMIT)
					: Math.max(velocityX * 3, -TARGET_MOVE_VELOCITY_LIMIT);
			float newVelocityY = velocityY > 0
					? Math.min(velocityY * 3, TARGET_MOVE_VELOCITY_LIMIT)
					: Math.max(velocityY * 3, -TARGET_MOVE_VELOCITY_LIMIT);

			float azimuth = mapRenderer.getAzimuth();
			int zoom = mapRenderer.getZoomLevel().ordinal();

			// Taking into account current zoom, get how many 31-coordinates there are in 1 point
			long tileSize31 = (1L << (31 - zoom));
			double scale31 = tileSize31 / mapRenderer.getTileSizeOnScreenInPixels();

			// Take into account current azimuth and reproject to map space (points)
			double angle = Math.toRadians(azimuth);
			double cosAngle = Math.cos(angle);
			double sinAngle = Math.sin(angle);

			double velocityInMapSpaceX = newVelocityX * cosAngle - newVelocityY * sinAngle;
			double velocityInMapSpaceY = newVelocityX * sinAngle + newVelocityY * cosAngle;

			// Rescale speed to 31 coordinates
			PointD velocity = new PointD(-velocityInMapSpaceX * scale31, -velocityInMapSpaceY * scale31);
			animator.animateFlatTargetWith(velocity,
					new PointD(TARGET_MOVE_DECELERATION * scale31, TARGET_MOVE_DECELERATION * scale31),
					userInteractionAnimationKey);
		}

		startThreadAnimating(() -> {
			if (mapRenderer != null) {
				if (animator != null) {
					invalidateMapTarget();
				}

				animatingMapAnimator();

				resetMapTarget();
				PointI target31 = mapRenderer.getTarget();
				tileView.setTarget31(target31.getX(), target31.getY(), notifyListener);
			} else {
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
			}
		});
	}

	public void startTilting(float elevationAngle, float elevationTime) {
		if (animationsDisabled)
			return;

		stopAnimatingSync();

		float initialElevationAngle = tileView.getElevationAngle();
		float elevationAngleDiff = elevationAngle - initialElevationAngle;

		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		float animationTime = doNotUseAnimations ? 1 : (elevationTime > 0.0f ? elevationTime : Math.abs(elevationAngleDiff) * 5);

		MapRendererView mapRenderer = getMapRenderer();
		MapAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.pause();

			float duration = animationTime / 1000f;
			IAnimation elevationAnimation = animator.getCurrentAnimation(userInteractionAnimationKey, AnimatedValue.ElevationAngle);
			if (elevationAnimation != null) {
				animator.cancelAnimation(elevationAnimation);
				animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.ElevationAngle);
			}

			animator.animateElevationAngleTo(elevationAngle,
					duration,
					TimingFunction.Linear,
					userInteractionAnimationKey);
		}

		startThreadAnimating(() -> {
			animatingMapTilt = true;
			if (mapRenderer != null) {
				animatingMapAnimator();
				if (mapRenderer.isMapAnimationFinished() && tileView.getElevationAngle() != elevationAngle) {
					tileView.setElevationAngle(elevationAngle);
				}
			} else {
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
			}
			animatingMapTilt = false;
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
		if (animationsDisabled)
			return;

		resetMapTarget();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapAnimator animator = mapRenderer.getMapAnimator();
			animator.pause();

			animator.cancelCurrentAnimation(locationServicesAnimationKey, AnimatedValue.Azimuth);
			animator.cancelCurrentAnimation(userInteractionAnimationKey, AnimatedValue.Azimuth);
			animator.animateAzimuthTo(-rotate, ROTATION_ANIMATION_TIME / 1000f, TimingFunction.Linear,
					locationServicesAnimationKey);

			startThreadAnimating(() -> {
				targetRotate = rotate;
				animatingMapRotation = true;
				animatingMapAnimator();
				animatingMapRotation = false;
				targetRotate = TARGET_NO_ROTATION;
			});
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

	public float getTargetRotate() {
		return targetRotate;
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

	@Override
	public void onTouchEvent(@NonNull MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			stopAnimating();
		}
	}
}