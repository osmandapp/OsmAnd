package net.osmand.plus.views.layers;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.Utilities;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.List;

public class PointLocationLayer extends OsmandMapLayer implements IContextMenuProvider, OsmAndLocationProvider.OsmAndLocationListener, OsmAndLocationProvider.OsmAndCompassListener {
	private static final Log LOG = PlatformUtil.getLog(PointLocationLayer.class);

	protected final static float BEARING_SPEED_THRESHOLD = 0.1f;
	protected final static int MIN_ZOOM_MARKER_VISIBILITY = 3;
	protected final static int RADIUS = 7;

	private Paint headingPaint;
	private Paint bitmapPaint;
	private Paint area;
	private Paint aroundArea;

	private OsmandMapTileView view;

	private ApplicationMode appMode;
	private boolean carView = false;
	private float textScale = 1f;
	@ColorInt
	private int profileColor;
	private LayerDrawable navigationIcon;
	private int navigationIconId;
	private LayerDrawable locationIcon;
	private int locationIconId;
	private Bitmap headingIcon;
	private int headingIconId;
	private final OsmAndLocationProvider locationProvider;
	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private boolean nm;
	private boolean locationOutdated;

	private static final int MARKER_ID_MY_LOCATION = 1;
	private static final int MARKER_ID_NAVIGATION = 2;
	private static final int MARKER_ID_MY_LOCATION_HEADING = 3;
	private static final int MARKER_ID_NAVIGATION_HEADING = 4;
	private MapMarkersCollection markersCollection;
	private CoreMapMarker locationMarker;
	private CoreMapMarker locationMarkerWithHeading;
	private CoreMapMarker navigationMarker;
	private CoreMapMarker navigationMarkerWithHeading;

	private boolean markersNeedInvalidate = true;
	private boolean showHeadingCached = false;
	private Location lastKnownLocation;
	private float lastHeading = 0.0f;
	private MarkerState currentMarkerState = MarkerState.Stay;

	private enum MarkerState {
		Stay,
		Move,
		None,
	}

	private static class CoreMapMarker {
		private MapMarker marker;
		private SWIGTYPE_p_void onSurfaceIconKey;
		private SWIGTYPE_p_void onSurfaceHeadingIconKey;

		public static CoreMapMarker createAndAddToCollection(@NonNull Context ctx, @NonNull MapMarkersCollection markersCollection,
		                                                     int id, @NonNull Drawable icon, @DrawableRes int headingIconId,
		                                                     float scale, @ColorInt int profileColor, boolean withHeading) {
			CoreMapMarker marker = new CoreMapMarker();
			MapMarkerBuilder myLocMarkerBuilder = new MapMarkerBuilder();
			myLocMarkerBuilder.setMarkerId(id);
			myLocMarkerBuilder.setIsAccuracyCircleSupported(true);
			myLocMarkerBuilder.setAccuracyCircleBaseColor(NativeUtilities.createFColorRGB(profileColor));
			myLocMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical);
			myLocMarkerBuilder.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
			myLocMarkerBuilder.setIsHidden(true);

			Bitmap markerBitmap = AndroidUtils.createScaledBitmap(icon, scale);
			if (markerBitmap != null) {
				marker.onSurfaceIconKey = SwigUtilities.getOnSurfaceIconKey(1);
				myLocMarkerBuilder.addOnMapSurfaceIcon(marker.onSurfaceIconKey,
						NativeUtilities.createSkImageFromBitmap(markerBitmap));
			}

			if (withHeading) {
				Bitmap headingBitmap = AndroidUtils.createScaledBitmapWithTint(ctx, headingIconId, scale, profileColor);
				if (headingBitmap != null) {
					marker.onSurfaceHeadingIconKey = SwigUtilities.getOnSurfaceIconKey(2);
					myLocMarkerBuilder.addOnMapSurfaceIcon(marker.onSurfaceHeadingIconKey,
							NativeUtilities.createSkImageFromBitmap(headingBitmap));
				}
			}
			marker.marker = myLocMarkerBuilder.buildAndAddToCollection(markersCollection);
			return  marker.marker != null ? marker : null;
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
		locationProvider = getApplication().getLocationProvider();
	}

	@Nullable
	public MapRendererView getMapRenderer() {
		return view != null ? view.getMapRenderer() : null;
	}

	private void initLegacyRenderer() {
		headingPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		bitmapPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		area = new Paint();
		aroundArea = new Paint();
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
	}

	private void initCoreRenderer() {
		markersNeedInvalidate = true;
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			lastKnownLocation = locationProvider.getLastStaleKnownLocation();
			initCoreRenderer();
			locationProvider.addLocationListener(this);
			locationProvider.addCompassListener(this);
		} else {
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			resetMarkerProvider();
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		if (view.hasMapRenderer()) {
			initCoreRenderer();
		} else {
			initLegacyRenderer();
		}
		updateParams(view.getSettings().getApplicationMode(), false, locationProvider.getLastKnownLocation() == null);
	}

	private boolean setMarkerState(MarkerState markerState, boolean showHeading, boolean forceUpdate) {
		if (currentMarkerState == markerState && !forceUpdate) {
			return false;
		}
		currentMarkerState = markerState;
		updateMarkerState(showHeading);
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		lastKnownLocation = location;
		if (view != null && view.hasMapRenderer()) {
			getApplication().runInUIThread(() -> updateMarkerData(lastKnownLocation, null));
		}
	}

	@Override
	public void updateCompassValue(float value) {
		if (Math.abs(MapUtils.degreesDiff(value, lastHeading)) > MapViewTrackingUtilities.COMPASS_HEADING_THRESHOLD) {
			lastHeading = value;
			if (view != null && view.hasMapRenderer()) {
				getApplication().runInUIThread(() -> updateMarkerData(null, lastHeading));
			}
		}
	}

	@Nullable
	private CoreMapMarker recreateMarker(Drawable icon, int id, @ColorInt int profileColor, boolean withHeading) {
		if (view == null || icon == null) {
			return null;
		}
		if (markersCollection == null) {
			markersCollection = new MapMarkersCollection();
		}
		return CoreMapMarker.createAndAddToCollection(view.getContext(),
				markersCollection, id, icon, headingIconId, getTextScale(), profileColor, withHeading);
	}

	private void resetMarkerProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && markersCollection != null) {
			mapRenderer.removeSymbolsProvider(markersCollection);
		}
		markersCollection = null;
	}

	private void setMarkerProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && markersCollection != null) {
			mapRenderer.addSymbolsProvider(markersCollection);
		}
	}

	private boolean invalidateMarkerCollection() {
		if (view == null || !view.hasMapRenderer()) {
			return false;
		}
		resetMarkerProvider();
		locationMarker = recreateMarker(locationIcon, MARKER_ID_MY_LOCATION, profileColor, false);
		locationMarkerWithHeading = recreateMarker(locationIcon, MARKER_ID_MY_LOCATION_HEADING, profileColor, true);
		navigationMarker = recreateMarker(navigationIcon, MARKER_ID_NAVIGATION, profileColor, false);
		navigationMarkerWithHeading = recreateMarker(navigationIcon, MARKER_ID_NAVIGATION_HEADING, profileColor, true);
		setMarkerProvider();
		return true;
	}

	private void updateMarkerState(boolean showHeading) {
		if (navigationMarker == null || locationMarker == null
				|| navigationMarkerWithHeading == null || locationMarkerWithHeading == null) {
			return;
		}
		switch (currentMarkerState) {
			case Move:
				navigationMarker.setVisibility(!showHeading);
				locationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(showHeading);
				locationMarkerWithHeading.setVisibility(false);
				break;
			case Stay:
				navigationMarker.setVisibility(false);
				locationMarker.setVisibility(!showHeading);
				navigationMarkerWithHeading.setVisibility(false);
				locationMarkerWithHeading.setVisibility(showHeading);
				break;
			case None:
			default:
				navigationMarker.setVisibility(false);
				locationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(false);
				locationMarkerWithHeading.setVisibility(false);
		}
	}

	private void updateMarkerData(@Nullable Location location, @Nullable Float heading) {
		CoreMapMarker locMarker;
		boolean showHeading = showHeadingCached;
		switch (currentMarkerState) {
			case Move:
				locMarker = showHeading ? navigationMarkerWithHeading : navigationMarker;
				break;
			case Stay:
				locMarker = showHeading ? locationMarkerWithHeading : locationMarker;
				break;
			case None:
			default:
				return;
		}
		MapRendererView mapRenderer = getMapRenderer();
		if (locMarker != null && locMarker.marker != null && mapRenderer != null) {
			mapRenderer.suspendSymbolsUpdate();
			if (location != null) {  // location
				final PointI target31 = Utilities.convertLatLonTo31(
						new net.osmand.core.jni.LatLon(location.getLatitude(), location.getLongitude()));
				locMarker.marker.setPosition(target31);
				if (locMarker.onSurfaceIconKey != null) {  // bearing
					locMarker.marker.setOnMapSurfaceIconDirection(locMarker.onSurfaceIconKey, location.getBearing() - 90.0f);
				}
				locMarker.marker.setAccuracyCircleRadius(location.getAccuracy());
			}
			if (locMarker.onSurfaceHeadingIconKey != null && heading != null) {  // heading
				locMarker.marker.setOnMapSurfaceIconDirection(locMarker.onSurfaceHeadingIconKey, heading);
			}
			if (locMarker.marker.isHidden()) {
				locMarker.marker.setIsHidden(false);
			}
			mapRenderer.resumeSymbolsUpdate();
		}
	}

	private boolean shouldShowHeading() {
		return !locationOutdated && mapViewTrackingUtilities.isShowViewAngle();
	}

	private boolean shouldShowBearing(@Nullable Location location) {
		return !locationOutdated && location != null
				// Issue 5538: Some devices return positives for hasBearing() at rest, hence add 0.0 check:
				&& location.hasBearing() && (location.getBearing() != 0.0f)
				&& (!location.hasSpeed() || location.getSpeed() > BEARING_SPEED_THRESHOLD);
	}

	private boolean isLocationVisible(@NonNull RotatedTileBox tb, @NonNull Location l) {
		return tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}

	private void drawMarkers(@NonNull Canvas canvas, @NonNull RotatedTileBox box, @NonNull Location lastKnownLocation) {
		int locationX;
		int locationY;
		if (mapViewTrackingUtilities.isMapLinkedToLocation()
				&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(lastKnownLocation)
				&& !mapViewTrackingUtilities.isMovingToMyLocation()) {
			locationX = box.getCenterPixelX();
			locationY = box.getCenterPixelY();
		} else {
			locationX = box.getPixXFromLonNoRot(lastKnownLocation.getLongitude());
			locationY = box.getPixYFromLatNoRot(lastKnownLocation.getLatitude());
		}

		final double dist = box.getDistance(0, box.getPixHeight() / 2, box.getPixWidth(), box.getPixHeight() / 2);
		int radius = (int) (((double) box.getPixWidth()) / dist * lastKnownLocation.getAccuracy());
		if (radius > RADIUS * box.getDensity()) {
			int allowedRad = Math.min(box.getPixWidth() / 2, box.getPixHeight() / 2);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), area);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), aroundArea);
		}
		// draw bearing/direction/location
		if (isLocationVisible(box, lastKnownLocation)) {
			Float heading = locationProvider.getHeading();
			if (shouldShowHeading() && heading != null) {
				canvas.save();
				canvas.rotate(heading - 180, locationX, locationY);
				canvas.drawBitmap(headingIcon, locationX - headingIcon.getWidth() / 2f,
						locationY - headingIcon.getHeight() / 2f, headingPaint);
				canvas.restore();
			}
			if (shouldShowBearing(lastKnownLocation)) {
				float bearing = lastKnownLocation.getBearing();
				canvas.rotate(bearing - 90, locationX, locationY);
				int width = (int) (navigationIcon.getIntrinsicWidth() * textScale);
				int height = (int) (navigationIcon.getIntrinsicHeight() * textScale);
				width += width % 2 == 1 ? 1 : 0;
				height += height % 2 == 1 ? 1 : 0;
				if (textScale == 1) {
					navigationIcon.setBounds(locationX - width / 2, locationY - height / 2,
							locationX + width / 2, locationY + height / 2);
					navigationIcon.draw(canvas);
				} else {
					navigationIcon.setBounds(0, 0, width, height);
					Bitmap bitmap = AndroidUtils.createScaledBitmap(navigationIcon, width, height);
					canvas.drawBitmap(bitmap, locationX - width / 2f, locationY - height / 2f, bitmapPaint);
				}
			} else {
				int width = (int) (locationIcon.getIntrinsicWidth() * textScale);
				int height = (int) (locationIcon.getIntrinsicHeight() * textScale);
				width += width % 2 == 1 ? 1 : 0;
				height += height % 2 == 1 ? 1 : 0;
				if (textScale == 1) {
					locationIcon.setBounds(locationX - width / 2, locationY - height / 2,
							locationX + width / 2, locationY + height / 2);
					locationIcon.draw(canvas);
				} else {
					locationIcon.setBounds(0, 0, width, height);
					Bitmap bitmap = AndroidUtils.createScaledBitmap(locationIcon, width, height);
					canvas.drawBitmap(bitmap, locationX - width / 2f, locationY - height / 2f, bitmapPaint);
				}
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (view == null || tileBox.getZoom() < MIN_ZOOM_MARKER_VISIBILITY || lastKnownLocation == null) {
			return;
		}
		boolean nightMode = settings != null && settings.isNightMode();
		updateParams(view.getSettings().getApplicationMode(), nightMode, locationProvider.getLastKnownLocation() == null);

		if (view.hasMapRenderer()) {
			boolean markersInvalidated = false;
			if (markersNeedInvalidate) {
				markersInvalidated = invalidateMarkerCollection();
				this.markersNeedInvalidate = false;
			}
			boolean showHeading = shouldShowHeading();
			boolean showBearing = shouldShowBearing(lastKnownLocation);
			boolean stateUpdated = setMarkerState(showBearing ?
					MarkerState.Move : MarkerState.Stay, showHeading, markersInvalidated);
			if (showHeading != showHeadingCached) {
				showHeadingCached = showHeading;
				if (!stateUpdated) {
					updateMarkerState(showHeading);
				}
			}
			if (markersInvalidated) {
				updateMarkerData(lastKnownLocation, locationProvider.getHeading());
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		Location lastKnownLocation = this.lastKnownLocation;
		if (view != null && !view.hasMapRenderer()
				&& tileBox.getZoom() >= MIN_ZOOM_MARKER_VISIBILITY && lastKnownLocation != null) {
			drawMarkers(canvas, tileBox, lastKnownLocation);
		}
	}

	@Override
	public void destroyLayer() {
	}

	private void updateParams(ApplicationMode appMode, boolean nighMode, boolean locationOutdated) {
		Context ctx = view.getContext();
		int profileColor = locationOutdated ?
				ContextCompat.getColor(ctx, ProfileIconColors.getOutdatedLocationColor(nighMode)) :
				appMode.getProfileColor(nighMode);
		int locationIconId = appMode.getLocationIcon().getIconId();
		int navigationIconId = appMode.getNavigationIcon().getIconId();
		int headingIconId = appMode.getLocationIcon().getHeadingIconId();
		float textScale = getTextScale();
		boolean carView = getApplication().getOsmandMap().getMapView().isCarView();
		if (appMode != this.appMode || this.nm != nighMode || this.locationOutdated != locationOutdated
				|| this.profileColor != profileColor
				|| this.locationIconId != locationIconId
				|| this.headingIconId != headingIconId
				|| this.navigationIconId != navigationIconId
				|| this.textScale != textScale
				|| this.carView != carView) {
			this.appMode = appMode;
			this.profileColor = profileColor;
			this.nm = nighMode;
			this.locationOutdated = locationOutdated;
			this.locationIconId = locationIconId;
			this.headingIconId = headingIconId;
			this.navigationIconId = navigationIconId;
			this.textScale = textScale;
			this.carView = carView;
			navigationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, navigationIconId);
			if (navigationIcon != null) {
				DrawableCompat.setTint(navigationIcon.getDrawable(1), profileColor);
			}
			headingIcon = getScaledBitmap(headingIconId);
			locationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, locationIconId);
			if (locationIcon != null) {
				DrawableCompat.setTint(DrawableCompat.wrap(locationIcon.getDrawable(1)), profileColor);
			}
			if (!view.hasMapRenderer()) {
				headingPaint.setColorFilter(new PorterDuffColorFilter(profileColor, PorterDuff.Mode.SRC_IN));
				area.setColor(ColorUtilities.getColorWithAlpha(profileColor, 0.16f));
				aroundArea.setColor(profileColor);
			}
			markersNeedInvalidate = true;
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= 3) {
			getMyLocationFromPoint(tileBox, point, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return getMyLocation();
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
				view.getContext().getString(R.string.shared_string_my_location), "");
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	private LatLon getMyLocation() {
		Location location = locationProvider.getLastKnownLocation();
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		} else {
			return null;
		}
	}

	private void getMyLocationFromPoint(RotatedTileBox tb, PointF point, List<? super LatLon> myLocation) {
		LatLon location = getMyLocation();
		if (location != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int x = (int) tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			int y = (int) tb.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
			int rad = (int) (18 * tb.getDensity());
			if (Math.abs(x - ex) <= rad && (ey - y) <= rad && (y - ey) <= 2.5 * rad) {
				myLocation.add(location);
			}
		}
	}
}
