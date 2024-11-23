package net.osmand.plus.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AnimatedValue;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkersAnimator;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListIAnimation;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.TimingFunction;
import net.osmand.core.jni.Utilities;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

public class AnimateMapMarkersThread {

	protected static final Log LOG = PlatformUtil.getLog(AnimateMapMarkersThread.class);

	private static final int DEFAULT_SLEEP_TO_REDRAW = 15;
	public static final long ROTATE_ANIMATION_TIME = 1000;

	private final OsmandApplication app;
	private final OsmandMapTileView tileView;

	private volatile boolean stopped;
	private volatile Thread currentThread;

	public AnimateMapMarkersThread(@NonNull OsmandMapTileView tileView) {
		this.app = tileView.getApplication();
		this.tileView = tileView;
	}

	@Nullable
	private MapRendererView getMapRenderer() {
		return tileView.getMapRenderer();
	}

	@Nullable
	private MapMarkersAnimator getAnimator() {
		MapRendererView mapRenderer = getMapRenderer();
		return mapRenderer != null ? mapRenderer.getMapMarkersAnimator() : null;
	}

	/**
	 * Stop animating async
	 */
	public void stopAnimating() {
		stopped = true;
	}

	public boolean isAnimating() {
		return currentThread != null && !stopped;
	}

	/**
	 * Stop animating sync
	 */
	public void stopAnimatingSync() {
		// wait until current thread != null
		MapRendererView renderer = getMapRenderer();
		if (renderer != null) {
			renderer.pauseMapMarkersAnimation();
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
				runnable.run();
			} finally {
				currentThread = null;
			}
		}, "Animating Map markers Thread");
		currentThread = t;
		t.start();
	}

	public void cancelAnimations(@NonNull MapMarker mapMarker) {
		MapMarkersAnimator animator = getAnimator();
		if (animator != null) {
			animator.cancelAnimations(mapMarker);
		}
	}

	public void cancelCurrentAnimation(@NonNull MapMarker mapMarker, @NonNull AnimatedValue animatedValue) {
		MapMarkersAnimator animator = getAnimator();
		if (animator != null) {
			animator.cancelCurrentAnimation(mapMarker, animatedValue);
		}
	}

	public QListIAnimation getAnimations(@NonNull MapMarker mapMarker) {
		MapMarkersAnimator animator = getAnimator();
		if (animator != null) {
			return animator.getAnimations(mapMarker);
		}
		return null;
	}

	public boolean hasAminations(@NonNull MapMarker mapMarker) {
		QListIAnimation animations = getAnimations(mapMarker);
		return animations != null && !animations.isEmpty();
	}

	public void animatePositionTo(@NonNull MapMarker mapMarker, @NonNull PointI target31, long animationDuration) {
		MapRendererView mapRenderer = getMapRenderer();
		MapMarkersAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.animatePositionTo(mapMarker, target31, animationDuration / 1000f, TimingFunction.Linear);
			startThreadAnimating(() -> animatingMapMarkersAnimator(mapRenderer));
		}
	}

	public void animateDirectionTo(@NonNull MapMarker mapMarker, @NonNull SWIGTYPE_p_void iconKey, float direction, long animationDuration) {
		MapRendererView mapRenderer = getMapRenderer();
		MapMarkersAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.animateDirectionTo(mapMarker, iconKey, (float) Utilities.normalizedAngleDegrees(direction),
					animationDuration / 1000f, TimingFunction.Linear);
			startThreadAnimating(() -> animatingMapMarkersAnimator(mapRenderer));
		}
	}

	public void animateModel3dDirectionTo(@NonNull MapMarker mapMarker, float direction, long animationDuration) {
		MapRendererView mapRenderer = getMapRenderer();
		MapMarkersAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.animateModel3DDirectionTo(mapMarker, (float) Utilities.normalizedAngleDegrees(direction),
					animationDuration / 1000f, TimingFunction.Linear);
			startThreadAnimating(() -> animatingMapMarkersAnimator(mapRenderer));
		}
	}

	public void animatePositionAndDirectionTo(@NonNull MapMarker mapMarker, @NonNull PointI target31, long positionAnimationDuration,
	                                          @NonNull SWIGTYPE_p_void iconKey, float direction, long directionAnimationDuration) {
		MapRendererView mapRenderer = getMapRenderer();
		MapMarkersAnimator animator = getAnimator();
		if (mapRenderer != null && animator != null) {
			animator.animatePositionTo(mapMarker, target31, positionAnimationDuration / 1000f, TimingFunction.Linear);
			animator.animateDirectionTo(mapMarker, iconKey, (float) Utilities.normalizedAngleDegrees(direction),
					directionAnimationDuration / 1000f, TimingFunction.Linear);
			startThreadAnimating(() -> animatingMapMarkersAnimator(mapRenderer));
		}
	}
	private void animatingMapMarkersAnimator(@NonNull MapRendererView mapRenderer) {
		MapRendererView renderer = getMapRenderer();
		if (renderer != null) {
			renderer.resumeMapMarkersAnimation();
		}
		while (!stopped) {
			mapRenderer.requestRender();
			sleepToRedraw(true);
			if (mapRenderer.isMapMarkersAnimationFinished()) {
				break;
			}
		}
		tileView.refreshMap();
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
}
