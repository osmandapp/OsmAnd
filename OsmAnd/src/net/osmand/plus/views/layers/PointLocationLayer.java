package net.osmand.plus.views.layers;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;
import static net.osmand.plus.views.AnimateMapMarkersThread.ROTATE_ANIMATION_TIME;
import static net.osmand.plus.views.layers.PointLocationLayer.MarkerState.MOVE;
import static net.osmand.plus.views.layers.PointLocationLayer.MarkerState.STAY;
import static net.osmand.util.MapUtils.HIGH_LATLON_PRECISION;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AnimatedValue;
import net.osmand.core.jni.FColorRGB;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.Model3D;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.Model3dHelper;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.AnimateMapMarkersThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class PointLocationLayer extends OsmandMapLayer
		implements OsmAndLocationListener, OsmAndCompassListener, IContextMenuProvider {

	private static final int MODEL_3D_MAX_SIZE_DP = 6;
	protected static final float BEARING_SPEED_THRESHOLD = 0.1f;
	protected static final int MIN_ZOOM = 3;
	protected static final int RADIUS = 7;

	private Paint headingPaint;
	private Paint area;
	private Paint aroundArea;

	private ApplicationMode appMode;
	private boolean carView;
	private float textScale = 1f;
	@ColorInt
	private int profileColor;

	private String navigationIconName;
	@Nullable
	private Model3D navigationModel;
	private boolean brokenNavigationModel;
	@Nullable
	private LayerDrawable navigationIcon;

	private String locationIconName;
	@Nullable
	private Model3D locationModel;
	private boolean brokenLocationModel;
	@Nullable
	private LayerDrawable locationIcon;

	private Bitmap headingIcon;
	private int headingIconId;

	private final OsmAndLocationProvider locationProvider;
	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private final OsmandSettings settings;
	private final Model3dHelper model3dHelper;
	private boolean nighMode;
	private boolean locationOutdated;
	private Location prevLocation;

	private static final int MARKER_ID_MY_LOCATION = 1;
	private static final int MARKER_ID_NAVIGATION = 2;
	private static final int MARKER_ID_MY_LOCATION_HEADING = 3;
	private static final int MARKER_ID_NAVIGATION_HEADING = 4;
	private CoreMapMarker locationMarker;
	private CoreMapMarker locationMarkerWithHeading;
	private CoreMapMarker navigationMarker;
	private CoreMapMarker navigationMarkerWithHeading;

	private boolean markersInvalidated = true;
	private boolean showHeadingCached = false;
	private Float lastBearingCached;
	private Float lastHeadingCached;
	private MarkerState currentMarkerState = STAY;
	private LatLon lastMarkerLocation;

	public enum MarkerState {
		STAY,
		MOVE,
		NONE,
	}

	private static class CoreMapMarker {
		private MapMarker marker;
		private SWIGTYPE_p_void onSurfaceIconKey;
		private SWIGTYPE_p_void onSurfaceHeadingIconKey;

		public static CoreMapMarker createAndAddToCollection(@NonNull Context ctx, @Nullable MapMarkersCollection markersCollection,
		                                                     int id, int baseOrder, @Nullable LayerDrawable icon, @Nullable Model3D model3D,
		                                                     @DrawableRes int headingIconId, float scale, @ColorInt int profileColor, boolean withHeading) {
			if (icon == null && model3D == null) {
				return null;
			}

			CoreMapMarker marker = new CoreMapMarker();
			MapMarkerBuilder myLocMarkerBuilder = new MapMarkerBuilder();
			myLocMarkerBuilder.setMarkerId(id);
			myLocMarkerBuilder.setBaseOrder(baseOrder);
			myLocMarkerBuilder.setIsAccuracyCircleSupported(true);
			myLocMarkerBuilder.setAccuracyCircleBaseColor(NativeUtilities.createFColorRGB(profileColor));
			myLocMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical);
			myLocMarkerBuilder.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
			myLocMarkerBuilder.setIsHidden(true);

			if (model3D != null) {
				myLocMarkerBuilder.setModel3D(model3D);
				myLocMarkerBuilder.setModel3DMaxSizeInPixels((int) (AndroidUtils.dpToPx(ctx, MODEL_3D_MAX_SIZE_DP) * scale));
			} else {
				int width = (int) (icon.getIntrinsicWidth() * scale);
				int height = (int) (icon.getIntrinsicHeight() * scale);
				int locationX = width / 2;
				int locationY = height / 2;

				Bitmap markerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(markerBitmap);
				AndroidUtils.drawScaledLayerDrawable(canvas, icon, locationX, locationY, scale);

				marker.onSurfaceIconKey = SwigUtilities.getOnSurfaceIconKey(1);
				myLocMarkerBuilder.addOnMapSurfaceIcon(marker.onSurfaceIconKey,
						NativeUtilities.createSkImageFromBitmap(markerBitmap));
			}

			if (withHeading) {
				Bitmap headingBitmap = AndroidUtils.createScaledBitmapWithTint(ctx, headingIconId, scale, profileColor);
				if (headingBitmap != null) {
					marker.onSurfaceHeadingIconKey = SwigUtilities.getOnSurfaceIconKey(2);
				}
			}
			marker.marker = myLocMarkerBuilder.buildAndAddToCollection(markersCollection);
			return marker.marker != null ? marker : null;
		}

		public void setVisibility(boolean visible) {
			if (marker == null) {
				return;
			}
			marker.setIsHidden(!visible);
			marker.setIsAccuracyCircleVisible(visible);
		}
	}

	public PointLocationLayer(@NonNull Context context) {
		super(context);
		this.mapViewTrackingUtilities = getApplication().getMapViewTrackingUtilities();
		this.locationProvider = getApplication().getLocationProvider();
		this.settings = getApplication().getSettings();
		this.model3dHelper = getApplication().getModel3dHelper();
	}

	private void initLegacyRenderer() {
		headingPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		area = new Paint();
		aroundArea = new Paint();
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
	}

	private void initCoreRenderer() {
		markersInvalidated = true;
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity == null) {
			clearMapMarkersCollections();
		} else {
			initCoreRenderer();
		}
	}

	@Override
	public void onMapRendererChange(@Nullable MapRendererView currentMapRenderer, @Nullable MapRendererView newMapRenderer) {
		super.onMapRendererChange(currentMapRenderer, newMapRenderer);
		if (newMapRenderer != null) {
			initCoreRenderer();
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		initCoreRenderer();
		initLegacyRenderer();
		updateParams(view.getSettings().getApplicationMode(), false, locationProvider.getLastKnownLocation() == null);
		locationProvider.addLocationListener(this);
		locationProvider.addCompassListener(this);
	}

	public LatLon getLastMarkerLocation() {
		return lastMarkerLocation;
	}

	@Override
	public boolean areMapRendererViewEventsAllowed() {
		return true;
	}

	@Override
	public void onUpdateFrame(MapRendererView mapRenderer) {
		super.onUpdateFrame(mapRenderer);
		if (isMapLinkedToLocation() && !isMovingToMyLocation()) {
			Location location = getPointLocation();
			PointI target31 = mapRenderer.getTarget();
			updateMarker(location, target31, 0);
		}
		lastMarkerLocation = getCurrentMarkerLocation();
	}

	private boolean setMarkerState(MarkerState markerState, boolean showHeading, boolean forceUpdate) {
		if (currentMarkerState == markerState && !forceUpdate) {
			return false;
		}
		currentMarkerState = markerState;
		updateMarkerState(showHeading);
		return true;
	}

	@Nullable
	private CoreMapMarker recreateMarker(LayerDrawable icon, Model3D model3d, int id, @ColorInt int profileColor, boolean withHeading) {
		if (view == null || (icon == null && model3d == null)) {
			return null;
		}
		if (mapMarkersCollection == null) {
			mapMarkersCollection = new MapMarkersCollection();
			mapMarkersCollection.setPriority(Long.MAX_VALUE);
		}
		return CoreMapMarker.createAndAddToCollection(getContext(), mapMarkersCollection, id,
				getPointsOrder(), icon, model3d, headingIconId, getTextScale(), profileColor, withHeading);
	}

	private void setMarkerProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && mapMarkersCollection != null) {
			mapRenderer.addSymbolsProvider(mapMarkersCollection);
		}
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		recreateMarkerCollection();
	}

	private boolean recreateMarkerCollection() {
		if (view == null || !hasMapRenderer()) {
			return false;
		}
		clearMapMarkersCollections();
		locationMarker = recreateMarker(locationIcon, locationModel, MARKER_ID_MY_LOCATION, profileColor, false);
		locationMarkerWithHeading = recreateMarker(locationIcon, locationModel, MARKER_ID_MY_LOCATION_HEADING, profileColor, true);
		navigationMarker = recreateMarker(navigationIcon, navigationModel, MARKER_ID_NAVIGATION, profileColor, false);
		navigationMarkerWithHeading = recreateMarker(navigationIcon, navigationModel, MARKER_ID_NAVIGATION_HEADING, profileColor, true);
		setMarkerProvider();
		return true;
	}

	private void updateMarkerState(boolean showHeading) {
		if (navigationMarker == null || locationMarker == null
				|| navigationMarkerWithHeading == null || locationMarkerWithHeading == null) {
			return;
		}
		FColorRGB circleColor = new FColorRGB();
		PointI circleLocation31 = new PointI();
		float circleRadius = 0.0f;
		boolean withCircle = false;
		float sectorDirection = 0.0f;
		float sectorRadius = 0.0f;
		switch (currentMarkerState) {
			case MOVE -> {
				navigationMarker.setVisibility(!showHeading);
				locationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(showHeading);
				locationMarkerWithHeading.setVisibility(false);
				circleColor = showHeading
						? navigationMarkerWithHeading.marker.getAccuracyCircleBaseColor()
						: navigationMarker.marker.getAccuracyCircleBaseColor();
				circleLocation31 = showHeading
						? navigationMarkerWithHeading.marker.getPosition()
						: navigationMarker.marker.getPosition();
				circleRadius = (float) (showHeading
						? navigationMarkerWithHeading.marker.getAccuracyCircleRadius()
						: navigationMarker.marker.getAccuracyCircleRadius());
				withCircle = true;
				sectorDirection = showHeading
						? navigationMarkerWithHeading.marker.getOnMapSurfaceIconDirection(navigationMarkerWithHeading.onSurfaceHeadingIconKey)
						: 0.0f;
				sectorRadius = (float) (showHeading
						? Math.max(headingIcon.getWidth(), headingIcon.getHeight()) / 2
						: 0);
			}
			case STAY -> {
				navigationMarker.setVisibility(false);
				locationMarker.setVisibility(!showHeading);
				navigationMarkerWithHeading.setVisibility(false);
				locationMarkerWithHeading.setVisibility(showHeading);
				circleColor = showHeading
						? locationMarkerWithHeading.marker.getAccuracyCircleBaseColor()
						: locationMarker.marker.getAccuracyCircleBaseColor();
				circleLocation31 = showHeading
						? locationMarkerWithHeading.marker.getPosition()
						: locationMarker.marker.getPosition();
				circleRadius = (float) (showHeading
						? locationMarkerWithHeading.marker.getAccuracyCircleRadius()
						: locationMarker.marker.getAccuracyCircleRadius());
				withCircle = true;
				sectorDirection = showHeading
						? locationMarkerWithHeading.marker.getOnMapSurfaceIconDirection(locationMarkerWithHeading.onSurfaceHeadingIconKey)
						: 0.0f;
				sectorRadius = (float) (showHeading
						? Math.max(headingIcon.getWidth(), headingIcon.getHeight()) / 2
						: 0);
			}
			default -> {
				navigationMarker.setVisibility(false);
				locationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(false);
				locationMarkerWithHeading.setVisibility(false);
			}
		}
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (withCircle) {
				mapRenderer.setMyLocationCircleColor(circleColor.withAlpha(0.2f));
				mapRenderer.setMyLocationCirclePosition(circleLocation31);
				mapRenderer.setMyLocationCircleRadius(circleRadius);
				mapRenderer.setMyLocationSectorDirection(sectorDirection);
				mapRenderer.setMyLocationSectorRadius(sectorRadius);
			} else {
				mapRenderer.setMyLocationCircleRadius(0.0f);
				mapRenderer.setMyLocationSectorRadius(0.0f);
			}
		}
	}

	@Nullable
	private CoreMapMarker getCurrentMarker() {
		CoreMapMarker locMarker;
		boolean showHeading = showHeadingCached;
		switch (currentMarkerState) {
			case MOVE -> locMarker = showHeading ? navigationMarkerWithHeading : navigationMarker;
			case STAY -> locMarker = showHeading ? locationMarkerWithHeading : locationMarker;
			default -> {
				return null;
			}
		}
		return locMarker;
	}

	private boolean containsLatLon(double lat, double lon) {
		MapRendererView mapRenderer = getMapRenderer();
		if (view == null || mapRenderer == null) {
			return false;
		}
		RotatedTileBox tb = view.getRotatedTileBox();
		PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(mapRenderer, tb, lat, lon);
		double tx = pixel.x;
		double ty = pixel.y;
		return tx >= 0 && tx <= tb.getPixWidth() && ty >= 0 && ty <= tb.getPixHeight();
	}

	private void updateMarker(@Nullable Location location, @Nullable PointI target31, long animationDuration) {
		if (location != null) {
			boolean animateBearing = isAnimateMyLocation();
			if (target31 == null && !containsLatLon(location.getLatitude(), location.getLongitude())) {
				animationDuration = 0;
				animateBearing = false;
			}
			updateMarkerPosition(location, target31, animationDuration);
			if (location.hasBearing()) {
				float bearing = location.getBearing() - 90.0f;
				Float cachedBearing = lastBearingCached;
				boolean updateBearing = cachedBearing == null || Math.abs(bearing - cachedBearing) > 0.1;
				if (updateBearing) {
					lastBearingCached = bearing;
					updateMarkerBearing(bearing, animateBearing);
				}
			}
		}
		Float heading = locationProvider.getHeading();
		if (heading != null && showHeadingCached) {
			Float cachedHeading = lastHeadingCached;
			boolean updateHeading = cachedHeading == null || Math.abs(heading - cachedHeading) > 0.1;
			if (location == null) {
				location = getPointLocation();
			}
			if (updateHeading && location != null && containsLatLon(location.getLatitude(), location.getLongitude())) {
				lastHeadingCached = heading;
				updateMarkerHeading(heading);
			}
		} else {
			lastHeadingCached = null;
		}
	}

	private void updateMarkerPosition(@NonNull Location location, @Nullable PointI target31, long animationDuration) {
		CoreMapMarker locMarker = getCurrentMarker();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && view != null && locMarker != null && locMarker.marker != null) {
			if (target31 == null) {
				target31 = new PointI(MapUtils.get31TileNumberX(location.getLongitude()),
						MapUtils.get31TileNumberY(location.getLatitude()));
			}
			AnimateMapMarkersThread animationThread = view.getAnimatedMapMarkersThread();
			animationThread.cancelCurrentAnimation(locMarker.marker, AnimatedValue.Target);
			if (animationDuration > 0) {
				animationThread.animatePositionTo(locMarker.marker, target31, animationDuration);
			} else {
				locMarker.marker.setPosition(target31);
				mapRenderer.setMyLocationCirclePosition(locMarker.marker.getPosition());
			}
			float circleRadius = location.getAccuracy();
			boolean withCircle = shouldShowLocationRadius(currentMarkerState);
			locMarker.marker.setAccuracyCircleRadius(circleRadius);
			locMarker.marker.setIsAccuracyCircleVisible(withCircle);
			if (withCircle) {
				mapRenderer.setMyLocationCircleRadius(circleRadius);
			} else {
				mapRenderer.setMyLocationCircleRadius(0.0f);
			}
		}
	}

	private void updateMarkerBearing(float bearing, boolean animateRotation) {
		MapRendererView mapRenderer = getMapRenderer();
		CoreMapMarker locMarker = getCurrentMarker();
		if (mapRenderer != null && view != null && locMarker != null && locMarker.marker != null) {
			AnimateMapMarkersThread animationThread = view.getAnimatedMapMarkersThread();
			animationThread.cancelCurrentAnimation(locMarker.marker, AnimatedValue.Azimuth);
			boolean hasModel = locMarker.marker.getModel3D() != null;
			long bearingRotationDuration = animateRotation && (hasModel || locMarker.onSurfaceIconKey != null)
					? ROTATE_ANIMATION_TIME : 0;
			if (locMarker.marker.getModel3D() != null) {
				if (bearingRotationDuration > 0) {
					animationThread.animateModel3dDirectionTo(locMarker.marker, bearing, bearingRotationDuration);
				} else {
					locMarker.marker.setModel3DDirection(bearing);
				}
			} else {
				if (bearingRotationDuration > 0) {
					animationThread.animateDirectionTo(locMarker.marker, locMarker.onSurfaceIconKey,
							bearing, bearingRotationDuration);
				} else if (locMarker.onSurfaceIconKey != null) {
					locMarker.marker.setOnMapSurfaceIconDirection(locMarker.onSurfaceIconKey, bearing);
				}
			}
		}
	}

	private void updateMarkerHeading(float heading) {
		MapRendererView mapRenderer = getMapRenderer();
		CoreMapMarker locMarker = getCurrentMarker();
		if (mapRenderer != null && view != null && locMarker != null && locMarker.marker != null) {
			if (locMarker.onSurfaceHeadingIconKey != null) {
				locMarker.marker.setOnMapSurfaceIconDirection(locMarker.onSurfaceHeadingIconKey, heading);
				mapRenderer.setMyLocationSectorDirection(heading);
			}
		}
	}

	@Nullable
	private PointI getCurrentMarkerPosition() {
		CoreMapMarker locMarker;
		boolean showHeading = showHeadingCached;
		switch (currentMarkerState) {
			case MOVE -> locMarker = showHeading ? navigationMarkerWithHeading : navigationMarker;
			case STAY -> locMarker = showHeading ? locationMarkerWithHeading : locationMarker;
			default -> {
				return null;
			}
		}
		return locMarker != null && locMarker.marker != null ? locMarker.marker.getPosition() : null;
	}

	@Nullable
	private LatLon getCurrentMarkerLocation() {
		PointI pos31 = getCurrentMarkerPosition();
		return pos31 != null
				? new LatLon(MapUtils.get31LatitudeY(pos31.getY()), MapUtils.get31LongitudeX(pos31.getX()))
				: null;
	}

	private boolean shouldShowHeading(@NonNull MarkerState markerState) {
		return !locationOutdated && mapViewTrackingUtilities.isShowViewAngle() && !isLocationSnappedToRoad()
				&& settings.VIEW_ANGLE_VISIBILITY.getModeValue(appMode).isVisible(markerState);
	}

	private boolean shouldShowLocationRadius(@NonNull MarkerState markerState) {
		return !isLocationSnappedToRoad() && settings.LOCATION_RADIUS_VISIBILITY.getModeValue(appMode).isVisible(markerState);
	}

	private boolean shouldShowBearing(@Nullable Location location) {
		return getBearingToShow(location) != null;
	}

	@Nullable
	private Float getBearingToShow(@Nullable Location location) {
		if (!locationOutdated && location != null) {
			// Issue 5538: Some devices return positives for hasBearing() at rest, hence add 0.0 check:
			boolean hasBearing = location.hasBearing() && location.getBearing() != 0.0f;
			boolean bearingValid = hasBearing || isUseRouting() && lastBearingCached != null;
			boolean speedValid = !location.hasSpeed() || location.getSpeed() > BEARING_SPEED_THRESHOLD;

			if (bearingValid && (speedValid || isLocationSnappedToRoad())) {
				return hasBearing ? location.getBearing() : lastBearingCached;
			}
		}
		return null;
	}

	private boolean isUseRouting() {
		RoutingHelper routingHelper = getApplication().getRoutingHelper();
		return routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()
				|| routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated();
	}

	private boolean isLocationSnappedToRoad() {
		OsmandApplication app = getApplication();
		Location projection = app.getOsmandMap().getMapLayers().getRouteLayer().getLastRouteProjection();
		return app.getSettings().SNAP_TO_ROAD.get() && Algorithms.objectEquals(projection, getPointLocation());
	}

	@Nullable
	public Location getPointLocation() {
		Location location = null;
		OsmandApplication app = getApplication();
		if (app.getRoutingHelper().isFollowingMode() && app.getSettings().SNAP_TO_ROAD.get()) {
			location = app.getOsmandMap().getMapLayers().getRouteLayer().getLastRouteProjection();
		}
		return location != null ? location : locationProvider.getLastStaleKnownLocation();
	}


	private boolean isLocationVisible(@NonNull RotatedTileBox tb, @NonNull Location l) {
		return tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}

	private void drawMarkers(@NonNull Canvas canvas, @NonNull RotatedTileBox box, @NonNull Location lastKnownLocation) {
		int locationX;
		int locationY;
		if (isMapLinkedToLocation()
				&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(lastKnownLocation)
				&& !isMovingToMyLocation()) {
			locationX = box.getCenterPixelX();
			locationY = box.getCenterPixelY();
		} else {
			locationX = box.getPixXFromLonNoRot(lastKnownLocation.getLongitude());
			locationY = box.getPixYFromLatNoRot(lastKnownLocation.getLatitude());
		}
		if (shouldShowLocationRadius(currentMarkerState)) {
			drawLocationAccuracy(canvas, box, lastKnownLocation, locationX, locationY);
		}
		// draw bearing/direction/location
		if (isLocationVisible(box, lastKnownLocation)) {
			if (shouldShowHeading(currentMarkerState)) {
				drawLocationHeading(canvas, locationX, locationY);
			}
			Float bearing = getBearingToShow(lastKnownLocation);
			if (bearing != null) {
				canvas.rotate(bearing - 90, locationX, locationY);
				if (navigationIcon != null) {
					AndroidUtils.drawScaledLayerDrawable(canvas, navigationIcon, locationX, locationY, textScale);
				}
			} else if (locationIcon != null) {
				AndroidUtils.drawScaledLayerDrawable(canvas, locationIcon, locationX, locationY, textScale);
			}
		}
	}

	private void drawLocationAccuracy(@NonNull Canvas canvas, @NonNull RotatedTileBox box,
	                                  @NonNull Location location, int locationX, int locationY) {
		double dist = box.getDistance(0, box.getPixHeight() / 2, box.getPixWidth(), box.getPixHeight() / 2);
		int radius = (int) (((double) box.getPixWidth()) / dist * location.getAccuracy());
		if (radius > RADIUS * box.getDensity()) {
			int allowedRad = Math.min(box.getPixWidth() / 2, box.getPixHeight() / 2);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), area);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), aroundArea);
		}
	}

	private void drawLocationHeading(@NonNull Canvas canvas, int locationX, int locationY) {
		Float heading = locationProvider.getHeading();
		if (heading != null) {
			canvas.save();
			canvas.rotate(heading - 180, locationX, locationY);
			canvas.drawBitmap(headingIcon, locationX - headingIcon.getWidth() / 2f,
					locationY - headingIcon.getHeight() / 2f, headingPaint);
			canvas.restore();
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		Location lastKnownLocation = locationProvider.getLastStaleKnownLocation();
		if (view == null || tileBox.getZoom() < MIN_ZOOM || lastKnownLocation == null) {
			clearMapMarkersCollections();
			return;
		}
		MapRendererView mapRenderer = getMapRenderer();
		boolean markersRecreated = false;
		if (mapRenderer != null && (markersInvalidated || mapMarkersCollection == null)) {
			markersRecreated = recreateMarkerCollection();
			markersInvalidated = false;
		}
		boolean showHeading = shouldShowHeading(currentMarkerState) && locationProvider.getHeading() != null;
		boolean showBearing = shouldShowBearing(lastKnownLocation);
		boolean stateUpdated = setMarkerState(showBearing ? MOVE : STAY, showHeading, markersRecreated);
		if (mapRenderer != null) {
			if (showHeading != showHeadingCached) {
				showHeadingCached = showHeading;
				if (!stateUpdated) {
					updateMarkerState(showHeading);
					stateUpdated = true;
				}
			}
			if (markersRecreated || stateUpdated) {
				lastBearingCached = null;
				lastHeadingCached = null;
				if (!isMapLinkedToLocation()) {
					updateMarker(lastKnownLocation, null, 0);
				}
			}
		}
		boolean nightMode = settings != null && settings.isNightMode();
		updateParams(view.getSettings().getApplicationMode(), nightMode, locationProvider.getLastKnownLocation() == null);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		Location lastKnownLocation = locationProvider.getLastStaleKnownLocation();
		if (view == null || tileBox.getZoom() < MIN_ZOOM || lastKnownLocation == null) {
			return;
		}
		if (!hasMapRenderer()) {
			drawMarkers(canvas, tileBox, lastKnownLocation);
		}
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		locationProvider.removeLocationListener(this);
		locationProvider.removeCompassListener(this);
	}

	@Override
	public void updateLocation(Location location) {
		if (view == null || view.getZoom() < MIN_ZOOM || location == null) {
			return;
		}
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && (!isMapLinkedToLocation() || isMovingToMyLocation())) {
			boolean dataChanged = !MapUtils.areLatLonEqual(prevLocation, location, HIGH_LATLON_PRECISION);
			if (dataChanged) {
				long movingTime = prevLocation != null ? location.getTime() - prevLocation.getTime() : 0;
				boolean animatePosition = settings.ANIMATE_MY_LOCATION.get();
				Integer interpolationPercent = settings.LOCATION_INTERPOLATION_PERCENT.get();
				if (prevLocation != null && getApplication().getRoutingHelper().isFollowingMode() && interpolationPercent > 0 && animatePosition) {
					List<Location> predictedLocations = RoutingHelperUtils.predictLocations(prevLocation, location,
							movingTime / 1000.0, getApplication().getRoutingHelper().getRoute(), interpolationPercent);
					if (!predictedLocations.isEmpty()) {
						// At the moment we get the first predicted location, but there may be several of them
						Location predictedLocation = predictedLocations.get(0);
						updateMarker(predictedLocation, null, isAnimateMyLocation() ? movingTime : 0);
					}
				} else {
					updateMarker(location, null, isAnimateMyLocation() ? movingTime : 0);
				}
				prevLocation = location;
			}
		}
	}

	@Override
	public void updateCompassValue(float value) {
		updateMarker(null, null, 0);
	}

	private boolean isAnimateMyLocation() {
		return settings.ANIMATE_MY_LOCATION.get();
	}

	private boolean isMapLinkedToLocation() {
		return mapViewTrackingUtilities.isMapLinkedToLocation();
	}

	private boolean isMovingToMyLocation() {
		return mapViewTrackingUtilities.isMovingToMyLocation();
	}

	private void setLocationModel() {
		locationModel = model3dHelper.getModel(locationIconName, model -> {
			locationModel = model;
			if (locationModel != null) {
				locationModel.setMainColor(NativeUtilities.createFColorARGB(profileColor));
			}
			brokenLocationModel = model == null;
			markersInvalidated = true;
			return true;
		});
		if (locationModel != null) {
			locationModel.setMainColor(NativeUtilities.createFColorARGB(profileColor));
		}
		locationIcon = null;
	}

	private void updateParams(ApplicationMode appMode, boolean nighMode, boolean locationOutdated) {
		boolean hasMapRenderer = hasMapRenderer();
		Context ctx = getContext();
		int profileColor = locationOutdated ?
				ContextCompat.getColor(ctx, ProfileIconColors.getOutdatedLocationColor(nighMode)) :
				appMode.getProfileColor(nighMode);
		String locationIconName = getLocationIconName(appMode);
		String navigationIconName = getNavigationIconName(appMode);
		float textScale = getTextScale();
		boolean carView = getApplication().getOsmandMap().getMapView().isCarView();
		boolean locationIconChanged = !locationIconName.equals(this.locationIconName);
		boolean navigationIconChanged = !navigationIconName.equals(this.navigationIconName);
		if (appMode != this.appMode || this.nighMode != nighMode || this.locationOutdated != locationOutdated
				|| this.profileColor != profileColor
				|| locationIconChanged
				|| navigationIconChanged
				|| this.textScale != textScale
				|| this.carView != carView) {
			this.appMode = appMode;
			this.profileColor = profileColor;
			this.nighMode = nighMode;
			this.locationOutdated = locationOutdated;
			this.locationIconName = locationIconName;
			this.navigationIconName = navigationIconName;
			this.textScale = textScale;
			this.carView = carView;

			if (navigationIconChanged && brokenNavigationModel) {
				brokenNavigationModel = false;
			}
			if (locationIconChanged && brokenLocationModel) {
				brokenLocationModel = false;
			}

			if (LocationIcon.isModel(navigationIconName)) {
				navigationModel = model3dHelper.getModel(navigationIconName, model -> {
					navigationModel = model;
					if (navigationModel != null) {
						navigationModel.setMainColor(NativeUtilities.createFColorARGB(profileColor));
					}
					brokenNavigationModel = model == null;
					markersInvalidated = true;
					if (LocationIcon.isModel(locationIconName)) {
						setLocationModel();
					}
					return true;
				});
				if (navigationModel != null) {
					navigationModel.setMainColor(NativeUtilities.createFColorARGB(profileColor));
				}
				navigationIcon = null;
			} else {
				int navigationIconId = LocationIcon.fromName(navigationIconName, false).getIconId();
				navigationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, navigationIconId);
				if (navigationIcon != null) {
					DrawableCompat.setTint(navigationIcon.getDrawable(1), profileColor);
				}
				navigationModel = null;
			}

			LocationIcon locationIconType = LocationIcon.fromName(locationIconName, true);
			if (LocationIcon.isModel(locationIconName)) {
				if (!LocationIcon.isModel(navigationIconName) || navigationModel != null) {
					setLocationModel();
				}
			} else {
				locationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, locationIconType.getIconId());
				if (locationIcon != null) {
					DrawableCompat.setTint(DrawableCompat.wrap(locationIcon.getDrawable(1)), profileColor);
				}
				locationModel = null;
			}
			headingIconId = locationIconType.getHeadingIconId();
			headingIcon = getScaledBitmap(headingIconId);

			if (!hasMapRenderer) {
				headingPaint.setColorFilter(new PorterDuffColorFilter(profileColor, PorterDuff.Mode.SRC_IN));
				area.setColor(ColorUtilities.getColorWithAlpha(profileColor, 0.16f));
				aroundArea.setColor(profileColor);
			}
			markersInvalidated = true;
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (result.getTileBox().getZoom() >= 3 && !excludeUntouchableObjects) {
			getMyLocationFromPoint(result);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return getMyLocation();
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
				getContext().getString(R.string.shared_string_my_location), "");
	}

	private LatLon getMyLocation() {
		Location location = locationProvider.getLastKnownLocation();
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		} else {
			return null;
		}
	}

	private void getMyLocationFromPoint(@NonNull MapSelectionResult result) {
		LatLon location = getMyLocation();
		if (location != null && view != null) {
			PointF point = result.getPoint();
			RotatedTileBox tileBox = result.getTileBox();
			int ex = (int) point.x;
			int ey = (int) point.y;
			PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tileBox, location);
			int rad = (int) (18 * tileBox.getDensity());
			if (Math.abs(pixel.x - ex) <= rad && (ey - pixel.y) <= rad && (pixel.y - ey) <= 2.5 * rad) {
				result.collect(location, this);
			}
		}
	}

	@NonNull
	private String getLocationIconName(@NonNull ApplicationMode appMode) {
		boolean hasMapRenderer = hasMapRenderer();
		String locationIconName = appMode.getLocationIcon();
		if (hasMapRenderer && LocationIcon.isModelRepresented(locationIconName)) {
			locationIconName = LocationIcon.fromName(locationIconName).getRepresented3DModelKey();
		}
		boolean forceUseDefault = LocationIcon.isModel(locationIconName)
				&& (!hasMapRenderer || brokenLocationModel && locationIconName.equals(this.locationIconName));
		return forceUseDefault
				? getDefaultIcon(locationIconName, LocationIcon.STATIC_DEFAULT.name())
				: locationIconName;
	}

	@NonNull
	private String getNavigationIconName(@NonNull ApplicationMode appMode) {
		boolean hasMapRenderer = hasMapRenderer();
		String navigationIconName = appMode.getNavigationIcon();
		if (hasMapRenderer && LocationIcon.isModelRepresented(navigationIconName)) {
			navigationIconName = LocationIcon.fromName(navigationIconName).getRepresented3DModelKey();
		}
		boolean forceUseDefault = LocationIcon.isModel(navigationIconName)
				&& (!hasMapRenderer || brokenNavigationModel && navigationIconName.equals(this.navigationIconName));
		return forceUseDefault
				? getDefaultIcon(navigationIconName, LocationIcon.MOVEMENT_DEFAULT.name())
				: navigationIconName;
	}

	private String getDefaultIcon(@NonNull String iconName, @NonNull String defaultIconName) {
		String defaultIcon = defaultIconName;
		if (LocationIcon.isDefaultModel(iconName)) {
			String iconForDefaultModel = LocationIcon.getIconForDefaultModel(iconName);
			if (!Algorithms.isEmpty(iconForDefaultModel)) {
				defaultIcon = iconForDefaultModel;
			}
		}
		return defaultIcon;
	}
}
