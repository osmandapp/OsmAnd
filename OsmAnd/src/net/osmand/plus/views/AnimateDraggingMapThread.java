package net.osmand.plus.views;

import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.core.util.Pair;

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
	private double targetFloatZoom = 0;

	private boolean isAnimatingZoom;
	private boolean isAnimatingMapMove;

	public AnimateDraggingMapThread(OsmandMapTileView tileView) {
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
			} catch (Exception e) {
			}
		}
	}

	public synchronized void startThreadAnimating(final Runnable runnable) {
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
							final boolean pendingRotation, final Float finalRotation, final boolean notifyListener) {
		stopAnimatingSync();

		final RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		final int startZoom = rb.getZoom();
		final double startZoomFP = rb.getZoomFloatPart();
		final float startRotation = rb.getRotate();

		final int zoom;
		final double zoomFP;
		final float rotation;
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

		final float mMoveX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		final float mMoveY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		boolean skipAnimation = !rb.containsLatLon(finalLat, finalLon);
		if (skipAnimation) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(zoom, zoomFP, notifyListener);
			tileView.rotateToAnimate(rotation);
			return;
		}

		startThreadAnimating(new Runnable() {

			@Override
			public void run() {
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

				animatingMoveInThread(mMoveX, mMoveY, NAV_ANIMATION_TIME, notifyListener, null);
				isAnimatingMapMove = false;
			}
		});
	}

	public void startMoving(final double finalLat, final double finalLon, final int endZoom, final boolean notifyListener) {
		startMoving(finalLat, finalLon, endZoom, notifyListener, null);
	}

	public void startMoving(final double finalLat, final double finalLon, final int endZoom,
							final boolean notifyListener, final Runnable finishAnimationCallback) {
		boolean wasAnimating = isAnimating();
		stopAnimatingSync();

		final RotatedTileBox rb = tileView.getCurrentRotatedTileBox().copy();
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();
		final int startZoom = rb.getZoom();
		final double startZoomFP = rb.getZoomFloatPart();
		float[] mSt = new float[2];
		final int moveZoom = calculateMoveZoom(rb, finalLat, finalLon, mSt);
		boolean skipAnimation = moveZoom == 0;
		// check if animation needed
		skipAnimation = skipAnimation || (Math.abs(moveZoom - startZoom) >= 3 || Math.abs(endZoom - moveZoom) > 3);
		if (skipAnimation || wasAnimating) {
			tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
			tileView.setFractionalZoom(endZoom, 0, notifyListener);
			if (finishAnimationCallback != null) {
				tileView.getApplication().runInUIThread(new Runnable() {
					@Override
					public void run() {
						finishAnimationCallback.run();
					}
				});
			}
			return;
		}
		final float mMoveX = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		final float mMoveY = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);

		final boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		final float animationTime = doNotUseAnimations ? 1 : Math.max(450, (Math.abs(mSt[0]) + Math.abs(mSt[1])) / 1200f * MOVE_MOVE_ANIMATION_TIME);

		startThreadAnimating(new Runnable() {

			@Override
			public void run() {
				isAnimatingMapMove = true;
				setTargetValues(endZoom, 0, finalLat, finalLon);

				if (moveZoom != startZoom) {
					animatingZoomInThread(startZoom, startZoomFP, moveZoom, startZoomFP, doNotUseAnimations ? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}

				if (!stopped) {
					animatingMoveInThread(mMoveX, mMoveY, animationTime, notifyListener, finishAnimationCallback);
				} else if (finishAnimationCallback != null) {
					tileView.getApplication().runInUIThread(new Runnable() {
						@Override
						public void run() {
							finishAnimationCallback.run();
						}
					});
				}
				if (!stopped) {
					tileView.setLatLonAnimate(finalLat, finalLon, notifyListener);
				}
				
				if (!stopped && (moveZoom != endZoom || startZoomFP != 0)) {
					animatingZoomInThread(moveZoom, startZoomFP, endZoom, 0, doNotUseAnimations ? 1 : ZOOM_MOVE_ANIMATION_TIME, notifyListener);
				}
				tileView.setFractionalZoom(endZoom, 0, notifyListener);

				pendingRotateAnimation();
				isAnimatingMapMove = false;
			}
		});
	}

	public int calculateMoveZoom(RotatedTileBox rb, final double finalLat, final double finalLon, float[] mSt) {
		if (rb == null) {
			rb = tileView.getCurrentRotatedTileBox().copy();
		}
		double startLat = rb.getLatitude();
		double startLon = rb.getLongitude();

		boolean skipAnimation = false;
		if (mSt == null) {
			mSt = new float[2];
		}
		mSt[0] = rb.getPixXFromLatLon(startLat, startLon) - rb.getPixXFromLatLon(finalLat, finalLon);
		mSt[1] = rb.getPixYFromLatLon(startLat, startLon) - rb.getPixYFromLatLon(finalLat, finalLon);
		while (Math.abs(mSt[0]) + Math.abs(mSt[1]) > 1200) {
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
									   boolean notify, final Runnable finishAnimationCallback) {
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
									   int zoomEnd, double zoomFloatEnd, float animationTime, boolean notifyListener) {
		try {
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
				float interpolation = interpolator.getInterpolation(normalizedTime);
				double curZoom = interpolation * (endZoom - beginZoom) + beginZoom;
				int baseZoom = (int) Math.round(curZoom - 0.5 * threshold);
				double zaAnimate = curZoom - baseZoom;
				tileView.zoomToAnimate(baseZoom, zaAnimate, notifyListener);
				try {
					Thread.sleep(DEFAULT_SLEEP_TO_REDRAW);
				} catch (InterruptedException e) {
					stopped = true;
				}
			}
			tileView.setFractionalZoom(zoomEnd, zoomFloatEnd, notifyListener);
		} finally {
			isAnimatingZoom = false;
		}
	}

	public boolean isAnimatingZoom() {
		return isAnimatingZoom;
	}

	public boolean isAnimatingMapMove() {
		return isAnimatingMapMove;
	}

	public void startZooming(final int zoomEnd, final double zoomPart, final boolean notifyListener) {
		boolean doNotUseAnimations = tileView.getSettings().DO_NOT_USE_ANIMATIONS.get();
		final float animationTime = doNotUseAnimations ? 0 : ZOOM_ANIMATION_TIME;
		startThreadAnimating(new Runnable() {
			@Override
			public void run() {
				setTargetValues(zoomEnd, zoomPart, tileView.getLatitude(), tileView.getLongitude());
				RotatedTileBox tb = tileView.getCurrentRotatedTileBox().copy();
				animatingZoomInThread(tb.getZoom(), tb.getZoomFloatPart(), zoomEnd, zoomPart, animationTime, notifyListener);

				pendingRotateAnimation();
			}
		}); //$NON-NLS-1$
	}


	public void startDragging(final float velocityX, final float velocityY,
							  float startX, float startY, final float endX, final float endY,
							  final boolean notifyListener) {
		final float animationTime = DRAGGING_ANIMATION_TIME;
		clearTargetValues();
		startThreadAnimating(new Runnable() {
			@Override
			public void run() {
				float curX = endX;
				float curY = endY;
				DecelerateInterpolator interpolator = new DecelerateInterpolator(1);

				long timeMillis = SystemClock.uptimeMillis();
				float normalizedTime;
				float prevNormalizedTime = 0f;
				while (!stopped) {
					normalizedTime = (SystemClock.uptimeMillis() - timeMillis) / animationTime;
					if (normalizedTime >= 1f) {
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

	private void clearTargetValues() {
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

	private void setTargetValues(int zoom, double zoomPart, double lat, double lon) {
		targetIntZoom = zoom;
		targetFloatZoom = zoomPart;
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


