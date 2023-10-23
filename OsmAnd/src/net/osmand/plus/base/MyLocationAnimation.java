package net.osmand.plus.base;

import android.os.Handler;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapRendererState;
import net.osmand.core.jni.PointD;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.AutoZoomMap;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.AnimateDraggingMapThread.FinishAnimationCallback;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Zoom;
import net.osmand.plus.views.Zoom.ComplexZoom;
import net.osmand.util.MapUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import static net.osmand.plus.views.AnimateDraggingMapThread.SKIP_ANIMATION_DP_THRESHOLD;

public class MyLocationAnimation implements OsmAndLocationListener {

	private static final float ELEVATION_DEGREES_PER_MS = 12 / 1000f; // 12 degrees per second
	private static final float MAX_ZOOM_PER_MS = 1 / 1000f;

	private static final long MAX_SECOND_TO_START_2D_TILT = 15;
	private static final long MOVE_ANIMATION_TIME = 500;
	private static final long MIN_SECONDS_TO_FOCUS_LOCATION_AFTER_2D_TILT = 3;
	private static final long MIN_SECONDS_BETWEEN_3D_AND_2D_TILT = 3;
	// Delay after passing focus locations to animate auto zoom, auto tilt and auto rotation
	private static final long MILLIS_TO_RESUME_ANIMATIONS = 5000;

	public static final int AUTO_ZOOM_DEFAULT_CHANGE_ZOOM = 4500;



	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;
	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final AnimateDraggingMapThread animateDraggingThread;
	private final LocationChangeObserver locationChangeObserver;

	@Nullable
	private Location myLocation;
	private long finishMovingTime;
	@Nullable
	private Location originalFocusLocation;
	@Nullable
	private Location focusLocationToAnimate;
	@Nullable
	private PointI focusLocationToAnimate31;

	private long timeToAllow2dTilt = 0;

	public MyLocationAnimation(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.mapView = app.getOsmandMap().getMapView();
		this.mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		this.animateDraggingThread = mapView.getAnimatedDraggingThread();
		this.locationChangeObserver = new LocationChangeObserver(
				this::getOriginalFocusLocation,
				newFocusLocation -> {
					checkFocusLocationIsNew(newFocusLocation);
					return true;
				});

		// todo start properly
		locationChangeObserver.startObserving();

		app.getLocationProvider().addLocationListener(this);
	}

	@Override
	public void updateLocation(@Nullable Location location) {
		Location previousMyLocation = myLocation;
		myLocation = location;

		if (location != null && mapViewTrackingUtilities.isMapLinkedToLocation()) {
			long animationDuration = getAnimationDuration(location, previousMyLocation);
			boolean animateMyLocation = settings.ANIMATE_MY_LOCATION.get()
					&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(location)
					&& !mapViewTrackingUtilities.isMovingToMyLocation();

			if (animateMyLocation) {
				// Temp fix to avoid late tilting to 2d after starting moving
				if (timeToAllow2dTilt == 0) {
					timeToAllow2dTilt = System.currentTimeMillis() + 5000;
				}
				animateMyLocation(location, previousMyLocation);
			} else {
				timeToAllow2dTilt = 0;
				setMyLocation(location, animationDuration);
			}

			mapView.refreshMap();
		}
	}

	private long getAnimationDuration(@NonNull Location myLocation, @Nullable Location previousMyLocation) {
		if (previousMyLocation == null) {
			return 0;
		}

		boolean rotateByBearing = settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING;
		boolean checkDistance = rotateByBearing && (!myLocation.hasBearing() || myLocation.getBearing() == 0.0f);
		if (checkDistance) {
			RotatedTileBox tileBox = mapView.getRotatedTileBox();
			double distInPixels = tileBox.getPixDensity() * MapUtils.getDistance(previousMyLocation, myLocation);
			double distInDp = distInPixels / tileBox.getDensity();
			if (distInDp > SKIP_ANIMATION_DP_THRESHOLD) {
				return 0;
			}
		}

		return myLocation.getTime() - previousMyLocation.getTime();
	}

	private void setMyLocation(@NonNull Location myLocation, long animationDuration) {
		boolean pendingRotation = isPendingRotation();
		Float rotation = getRotationToAnimateFromRotationMode(myLocation);
		ComplexZoom zoom = calculateZoomFromSpeed(myLocation);

		if (mapView.hasMapRenderer()) {
			if (settings.DO_NOT_USE_ANIMATIONS.get()) {
				animationDuration = 0;
			} else {
				animationDuration = mapViewTrackingUtilities.isMovingToMyLocation()
						? (long) Math.min(animationDuration * 0.7, MOVE_ANIMATION_TIME)
						: MOVE_ANIMATION_TIME;
			}

			Pair<Integer, Double> pair = zoom == null ? null : new Pair<>(zoom.base, (double) zoom.floatPart);
			animateDraggingThread.startMoving(
					myLocation.getLatitude(), myLocation.getLongitude(), pair,
					false, rotation, animationDuration, false, () -> mapViewTrackingUtilities.setIsMovingToMyLocation(false));
		} else {
			if (zoom != null) {
				animateDraggingThread.startZooming(zoom.base, zoom.floatPart, null, false);
			}
			if (rotation != null) {
				mapView.setRotate(rotation, false);
			}
			mapView.setLatLon(myLocation.getLatitude(), myLocation.getLongitude());
		}
	}


	boolean animatingTilt = false;
	private float userElevationAngle = 0.0f;
	private long timeToUnfocusLocation = 0;

	private void animateMyLocation(@NonNull Location myLocation, @Nullable Location previousMyLocation) {
		MapRendererView mapRenderer = mapView.getMapRenderer();
		boolean hasMapRenderer = mapRenderer != null;

		long animationDuration = getAnimationDuration(myLocation, previousMyLocation);

		if (!hasMapRenderer) {
			ComplexZoom zoom = null;
			if (shouldAutoZoomBySpeed(null)) {
				zoom = calculateZoomFromSpeed(myLocation);
			}
			Float rotation = getRotationToAnimateFromRotationMode(myLocation);
			animateDraggingThread.startMoving(myLocation.getLatitude(), myLocation.getLongitude(),
					null, isPendingRotation(), rotation, animationDuration, false, () -> mapViewTrackingUtilities.setIsMovingToMyLocation(false));
			return;
		}

//		animateDraggingThread.stopAnimatingSync();

		focusLocationToAnimate = null;
		focusLocationToAnimate31 = null;
		if (settings.AUTO_TILT_MAP.get() || settings.AUTO_ZOOM_MAP_TO_FOCUS.get()) {
			Location originalFocusLocation = getOriginalFocusLocation();
			checkFocusLocationIsNew(originalFocusLocation);
			if (originalFocusLocation != null) {
				focusLocationToAnimate = getFocusLocationForAnimation(myLocation, originalFocusLocation);
				focusLocationToAnimate31 = NativeUtilities.getPoint31FromLatLon(focusLocationToAnimate.getLatitude(), focusLocationToAnimate.getLongitude());
			}
		}
		PointI focusPixel = getFocusPixel();

		StateToFocus stateToFocus = focusLocationToAnimate31 != null && settings.AUTO_ZOOM_MAP_TO_FOCUS.get()
				? calculateZoomAndRotationToFocus(mapRenderer, myLocation, focusLocationToAnimate31, focusPixel)
				: null;

		long delay = 0;
		boolean passedFocusLocation = false;
		boolean unfocusedLocation = false;
		if (timeToUnfocusLocation != 0) {
			passedFocusLocation = true;
			long currentTimeMillis = System.currentTimeMillis();
			if (timeToUnfocusLocation < currentTimeMillis + animationDuration) {
				delay = Math.max(0, timeToUnfocusLocation - currentTimeMillis);
				unfocusedLocation = true;
				timeToUnfocusLocation = 0;
			}
		}

		double secondsToFocusLocation = Double.NaN;
		if (focusLocationToAnimate != null && myLocation.hasSpeed() && myLocation.getSpeed() > 0.0f) {
			double distance = MapUtils.getDistance(myLocation, focusLocationToAnimate);
			secondsToFocusLocation = distance / myLocation.getSpeed();
		}

		animateDraggingThread.startMoving(myLocation.getLatitude(), myLocation.getLongitude(), null,
				false, null, animationDuration, false, () -> {
					mapViewTrackingUtilities.setIsMovingToMyLocation(false);
				});
		finishMovingTime = System.currentTimeMillis() + animationDuration;

		if (shouldTiltTo2D(stateToFocus, secondsToFocusLocation)) {
			userElevationAngle = mapView.getElevationAngle();
			animateTilt(90, focusLocationToAnimate31, focusPixel);
		} else if (unfocusedLocation && shouldRestore3DTilt(secondsToFocusLocation)) {
			float targetElevationAngle = userElevationAngle;
			app.runMessageInUiThread(123456, delay, () -> animateTilt(targetElevationAngle, focusLocationToAnimate31, focusPixel));
			userElevationAngle = 0.0f;
		} else if (!passedFocusLocation || unfocusedLocation) {
			if (delay == 0) {
				zoomIfNeeded(stateToFocus);
			} else {
				app.runMessageInUiThread(1223344, delay, () -> zoomIfNeeded(stateToFocus));
			}
		}

		if (isRotationToFocusEnabled() && stateToFocus != null && stateToFocus.rotation != null) {
			animateDraggingThread.startRotate(stateToFocus.rotation, finishMovingTime - System.currentTimeMillis());
		} else {
			Float rotation = getRotationToAnimateFromRotationMode(myLocation);
			if (rotation != null) {
				animateDraggingThread.startRotate(rotation, 1000);
			}
		}
	}

	private boolean shouldAutoZoomBySpeed(@Nullable StateToFocus state) {
		if (!settings.AUTO_ZOOM_MAP.get()) {
			return false;
		}

		if (state != null) {
			int minZoomBaseToFocus = settings.AUTO_ZOOM_MAP_SCALE.get().minZoomBaseToFocus;
			return state.zoom.base < minZoomBaseToFocus;
		} else {
			return true;
		}
	}

	private boolean shouldZoomToFocus(@NonNull StateToFocus state) {
		if (!settings.AUTO_ZOOM_MAP_TO_FOCUS.get()) {
			return false;
		}

		AutoZoomMap autoZoomScale = settings.AUTO_ZOOM_MAP_SCALE.get();
		return state.zoom.base >= autoZoomScale.minZoomBaseToFocus
				&& state.zoom.base <= autoZoomScale.maxZoomBaseToFocus;
	}

	private boolean shouldTiltTo2D(@Nullable StateToFocus stateToFocus,
	                               double secondsToFocusLocation) {
		if (!settings.AUTO_TILT_MAP.get()
				|| animatingTilt
				|| System.currentTimeMillis() < timeToAllow2dTilt
				|| timeToUnfocusLocation != 0) {
			return false;
		}

		float elevationAngle = mapView.getElevationAngle();
		if (elevationAngle >= 85f) {
			return false;
		}

		if (!Double.isNaN(secondsToFocusLocation)) {
			float elevationAngleDelta = 90 - elevationAngle;
			float secondToTilt = (elevationAngleDelta / ELEVATION_DEGREES_PER_MS) / 1000;

			// Do not tilt if there is not enough time before focus location
			if (secondToTilt + MIN_SECONDS_TO_FOCUS_LOCATION_AFTER_2D_TILT > secondsToFocusLocation) {
				return false;
			}

			// Tilt if focus location is close enough
			if (secondsToFocusLocation <= MAX_SECOND_TO_START_2D_TILT) {
				return true;
			}
		}


		if (stateToFocus != null) {
			int maxZoomBaseToFocus = settings.AUTO_ZOOM_MAP_SCALE.get().maxZoomBaseToFocus;
			if (stateToFocus.zoom.base > maxZoomBaseToFocus) {
				return true;
			}
		}

		return false;
	}

	private boolean shouldRestore3DTilt(double secondsToFocusLocation) {
		float secondsToTiltTo2dAnd3D = (userElevationAngle / ELEVATION_DEGREES_PER_MS) / 1000 * 2;
		float secondsToFitTiltTo2dAnd3d = secondsToTiltTo2dAnd3D
				+ MIN_SECONDS_BETWEEN_3D_AND_2D_TILT
				+ MAX_SECOND_TO_START_2D_TILT;
		return settings.AUTO_TILT_MAP.get()
				&& !animatingTilt
				&& userElevationAngle != 0.0f
				&& secondsToFitTiltTo2dAnd3d < secondsToFocusLocation;
	}

	private float getTiltDuration(float targetElevationAngle) {
		float elevationAngle = mapView.getElevationAngle();
		return Math.abs(targetElevationAngle - elevationAngle) / ELEVATION_DEGREES_PER_MS;
	}

	private void zoomIfNeeded(@Nullable StateToFocus stateToFocus) {
		long animationDuration = finishMovingTime - System.currentTimeMillis();
		if (animationDuration == 0) {
			return;
		}

		if (shouldAutoZoomBySpeed(stateToFocus)) {
			autoZoomBySpeed(animationDuration);
		} else if (stateToFocus != null && shouldZoomToFocus(stateToFocus)) {
			animateStateToFocus(stateToFocus, animationDuration);
		}
	}

	private void autoZoomBySpeed(long animationDuration) {

	}

	private void animateTilt(float elevationAngle, @Nullable PointI focusLocation31, @NonNull PointI focusPixel) {
		FinishAnimationCallback finishAnimationCallback = (boolean cancelled) -> {
			animatingTilt = false;
			if (cancelled) {
				// todo
			} else {
				long duration = finishMovingTime - System.currentTimeMillis();
				if (duration < 0) {
					return;
				}

				MapRendererView mapRenderer = mapView.getMapRenderer();
				if (mapRenderer == null) {
					return;
				}

				StateToFocus newStateToFocus = null;
				if (this.myLocation != null && this.focusLocationToAnimate31 != null) {
					newStateToFocus = calculateZoomAndRotationToFocus(mapRenderer, this.myLocation, this.focusLocationToAnimate31, getFocusPixel());
				}

				zoomIfNeeded(newStateToFocus);
				// todo rotate to focus, if enabled
			}
		};
		if (focusLocation31 != null) {
			animateDraggingThread.startTilting(elevationAngle, ELEVATION_DEGREES_PER_MS, null);
			animateDraggingThread.startFittingLocationToPixel(focusLocation31, focusPixel, isRotationToFocusEnabled(), getTiltDuration(elevationAngle), finishAnimationCallback);
		} else {
			animateDraggingThread.startTilting(elevationAngle, ELEVATION_DEGREES_PER_MS, finishAnimationCallback);
		}
	}

	private void animateStateToFocus(@NonNull StateToFocus stateToFocus, float animationDuration) {
		animateDraggingThread.startZooming(stateToFocus.zoom.base, stateToFocus.zoom.floatPart, animationDuration, null, false, false);
		if (stateToFocus.rotation != null) {
			animateDraggingThread.startRotate(stateToFocus.rotation, animationDuration);
		}
	}

	@Nullable
	private Location getOriginalFocusLocation() {
		NextDirectionInfo nextDirectionInfo = app.getRoutingHelper().getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		if (nextDirectionInfo == null || nextDirectionInfo.distanceTo <= 0 || nextDirectionInfo.directionInfo == null) {
			return null;
		}
		return app.getRoutingHelper().getLocationFromRouteDirection(nextDirectionInfo.directionInfo);
	}

	@NonNull
	private Location getFocusLocationForAnimation(@NonNull Location myLocation, @NonNull Location originalFocusLocation) {
		if (isRotationToFocusEnabled() && settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			return originalFocusLocation;
		}

		int rotationMode = settings.ROTATE_MAP.get();
		float rotation = 0.0f;
		if (rotationMode == OsmandSettings.ROTATE_MAP_COMPASS) {
			Float heading = app.getLocationProvider().getHeading();
			rotation = heading == null ? mapView.getRotate() : heading;
		} else if (rotationMode == OsmandSettings.ROTATE_MAP_BEARING) {
			rotation = myLocation.hasBearing() ? -myLocation.getBearing() : mapView.getRotate();
		} else if (rotationMode == OsmandSettings.ROTATE_MAP_MANUAL) {
			rotation = mapView.getRotate();
		}

		double distance = MapUtils.getDistance(myLocation, originalFocusLocation);
		LatLon latLon = MapUtils.rhumbDestinationPoint(myLocation.getLatitude(), myLocation.getLongitude(), distance, rotation);
		return new Location("", latLon.getLatitude(), latLon.getLongitude());
	}

	private long lastTimeManualZooming;
	private long lastTimeAutoZooming;

	@Nullable
	private ComplexZoom calculateZoomFromSpeed(@NonNull Location myLocation) {
		if (!myLocation.hasSpeed()) {
			return null;
		}

		RotatedTileBox tb = mapView.getCurrentRotatedTileBox();

		long now = System.currentTimeMillis();
		float zdelta = defineZoomFromSpeed(tb, myLocation.getSpeed());
		if (Math.abs(zdelta) >= 0.5/*?Math.sqrt(0.5)*/) {
			// prevent ui hysteresis (check time interval for autozoom)
			if (zdelta >= 2) {
				// decrease a bit
				zdelta -= 1;
			} else if (zdelta <= -2) {
				// decrease a bit
				zdelta += 1;
			}
			double targetZoom = Math.min(tb.getZoom() + tb.getZoomFloatPart() + zdelta, settings.AUTO_ZOOM_MAP_SCALE.get().maxZoomFromSpeed);
			boolean isUserZoomed = lastTimeManualZooming > lastTimeAutoZooming;
			int threshold = settings.AUTO_FOLLOW_ROUTE.get();
			if ((now - lastTimeAutoZooming > AUTO_ZOOM_DEFAULT_CHANGE_ZOOM && !isUserZoomed)
					|| (now - lastTimeManualZooming > Math.max(threshold, AUTO_ZOOM_DEFAULT_CHANGE_ZOOM) && isUserZoomed)) {
				lastTimeAutoZooming = now;
//					double settingsZoomScale = Math.log(mapView.getSettingsMapDensity()) / Math.log(2.0f);
//					double zoomScale = Math.log(tb.getMapDensity()) / Math.log(2.0f);
//					double complexZoom = tb.getZoom() + zoomScale + zdelta;
				// round to 0.33
				targetZoom = Math.round(targetZoom * 3) / 3f;
				int newIntegerZoom = (int) Math.round(targetZoom);
				float zPart = (float) (targetZoom - newIntegerZoom);
				return newIntegerZoom > 0 ? new ComplexZoom(newIntegerZoom, zPart) : null;
			}
		}

		return null;
	}

	private float defineZoomFromSpeed(RotatedTileBox tb, float speed) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double visibleDist = tb.getDistance(tb.getCenterPixelX(), 0, tb.getCenterPixelX(), tb.getCenterPixelY());
		float time = 75f; // > 83 km/h show 75 seconds
		if (speed < 83f / 3.6) {
			time = 60f;
		}
		time /= settings.AUTO_ZOOM_MAP_SCALE.get().coefficient;
		double distToSee = speed * time;
		// check if 17, 18 is correct?
		return (float) (Math.log(visibleDist / distToSee) / Math.log(2.0f));
	}

	public void setUserZoomedTime(long time) {
		lastTimeManualZooming = time;
	}

	private boolean isPendingRotation() {
		int rotationMode = settings.ROTATE_MAP.get();
		return rotationMode == OsmandSettings.ROTATE_MAP_NONE
				|| rotationMode == OsmandSettings.ROTATE_MAP_COMPASS
				|| rotationMode == OsmandSettings.ROTATE_MAP_MANUAL;
	}

	@Nullable
	private Float getRotationToAnimateFromRotationMode(@NonNull Location myLocation) {
		int rotationMode = settings.ROTATE_MAP.get();
		if (rotationMode == OsmandSettings.ROTATE_MAP_NONE) {
			return 0.0f;
		} else if (rotationMode == OsmandSettings.ROTATE_MAP_BEARING) {
			if (myLocation.hasBearing() && myLocation.getBearing() != 0.0f) {
				return -myLocation.getBearing();
			}
		}

		return null;
	}

	private boolean isRotationToFocusEnabled() {
		return settings.AUTO_ROTATE_MAP_TO_FOCUS.get()
				&& settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING;
	}

	private void checkFocusLocationIsNew(@Nullable Location focusLocation) {
		boolean currentFocusLocationIsPassed = !MapUtils.areLatLonEqual(originalFocusLocation, focusLocation);
		if (currentFocusLocationIsPassed) {
			originalFocusLocation = focusLocation;
			timeToUnfocusLocation = System.currentTimeMillis() + MILLIS_TO_RESUME_ANIMATIONS;
		}
	}

	@Nullable
	private StateToFocus calculateZoomAndRotationToFocus(@NonNull MapRendererView mapRenderer,
					  									 @NonNull Location myLocation,
														 @NonNull PointI focusLocation31,
														 @NonNull PointI focusPixel) {
		PointI myLocation31 = NativeUtilities.getPoint31FromLatLon(myLocation.getLatitude(), myLocation.getLongitude());
		MapRendererState state = mapRenderer.getState();
		if (!mapRenderer.setMapTargetForState(state, state.getFixedPixel(), myLocation31)) {
			return null;
		}

		// TODO heights
		float myLocationHeight = 0.0f;
		float focusLocationHeight = 0.0f;
		PointD deltaZoomAndRotation = new PointD();
		boolean canFocus = mapRenderer.getZoomAndRotationAfterPinch(state,
				myLocation31, myLocationHeight, state.getFixedPixel(),
				focusLocation31, focusLocationHeight, focusPixel,
				deltaZoomAndRotation);

		if (!canFocus) {
			return null;
		}

		Zoom zoom = new Zoom(
				mapView.getZoom(),
				mapView.getZoomFloatPart() + mapView.getZoomAnimation(),
				mapView.getMinZoom(),
				mapView.getMaxZoom());
		float deltaZoom = (float) deltaZoomAndRotation.getX();
		zoom.applyZoomDelta(deltaZoom);
		ComplexZoom zoomToFocus = new ComplexZoom(zoom.getBaseZoom(), zoom.getZoomFloatPart());

		Float rotationToFocus = null;
		if (isRotationToFocusEnabled()) {
			float deltaRotation = (float) -deltaZoomAndRotation.getY();
			rotationToFocus = MapUtils.unifyRotationTo360(mapView.getRotate() + deltaRotation);
		}

		return new StateToFocus(zoomToFocus, rotationToFocus);
	}

	@NonNull
	private PointI getFocusPixel() {
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		return new PointI(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 3);
	}

	private static class LocationChangeObserver {

		interface LocationGetter {

			@Nullable
			Location getLocation();
		}

		private static final int UPDATE_INTERVAL_MILLIS = 250;

		private final Handler handler;
		private final Runnable handlerCallback;

		private boolean observing = false;

		public LocationChangeObserver(@NonNull LocationGetter locationGetter, @NonNull CallbackWithObject<Location> onLocationChanged) {
			this.handler = new Handler();
			this.handlerCallback = new Runnable() {

				private Location previousLocation = null;

				@Override
				public void run() {
					Location newLocation = locationGetter.getLocation();
					if (!MapUtils.areLatLonEqual(previousLocation, newLocation)) {
						onLocationChanged.processResult(newLocation);
					}
					previousLocation = newLocation;
					handler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
				}
			};
		}

		public void startObserving() {
			if (!observing) {
				observing = handler.post(handlerCallback);
			}
		}

		public void stopObserving() {
			if (observing) {
				handler.removeCallbacks(handlerCallback);
			}
		}
	}

	private static class StateToFocus {

		@NonNull
		public final ComplexZoom zoom;
		@Nullable
		public final Float rotation;

		public StateToFocus(@NonNull ComplexZoom zoom, @Nullable Float rotation) {
			this.zoom = zoom;
			this.rotation = rotation;
		}
	}
}