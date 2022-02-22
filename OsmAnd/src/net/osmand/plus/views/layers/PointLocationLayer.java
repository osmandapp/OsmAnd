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
import android.graphics.RectF;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.Utilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
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
	private int color;
	private LayerDrawable navigationIcon;
	private int navigationIconId;
	private LayerDrawable locationIcon;
	private int locationIconId;
	private Bitmap headingIcon;
	private int headingIconId;
	private OsmAndLocationProvider locationProvider;
	private final MapViewTrackingUtilities mapViewTrackingUtilities;
	private boolean nm;
	private boolean locationOutdated;

	// location pin marker
	private static final int MARKER_ID_MY_LOCATION = 1;
	private static final int MARKER_ID_NAVIGATION = 2;
	private static final int MARKER_ID_MY_LOCATION_HEADING = 3;
	private static final int MARKER_ID_NAVIGATION_HEADING = 4;
	private MapMarkersCollection markersCollection;
	private MyLocMapMarker myLocationMarker;
	private MyLocMapMarker myLocationMarkerWithHeading;
	private MyLocMapMarker navigationMarker;
	private MyLocMapMarker navigationMarkerWithHeading;
	private SWIGTYPE_p_void onSurfaceIconKey = null;
	private SWIGTYPE_p_void onSurfaceHeadingIconKey = null;
	private boolean markersNeedInvalidate = true;
	private boolean hasHeading = false;
	private boolean hasHeadingCached = true;
	private Location lastKnownLocation;
	private float lastHeading = 0.0f;

	private enum MarkerState {
		Stay,
		Move,
		None,
	}

	private class MyLocMapMarker {
		public MapMarker marker;
		public SWIGTYPE_p_void onSurfaceIconKey = null;

		public void reset() {
			if (marker != null) {
				marker.delete();
				marker = null;
			}
		}

		public void setVisibility(boolean visible) {
			if (marker == null) {
				return;
			}
			marker.setIsHidden(!visible);
			marker.setIsAccuracyCircleVisible(visible);
		}
	}

	private MarkerState currentMarkerState = MarkerState.Stay;

	private void setMarkerState(MarkerState markerState) {
		if (currentMarkerState == markerState) {
			return;
		}
		currentMarkerState = markerState;
		updateMarkerState();
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


	private MyLocMapMarker recreateMarker(MyLocMapMarker currentMarker, LayerDrawable icon, int id, int baseColor, boolean addHeading) {
		if (view == null || icon == null) {
			return null;
		}

		if (markersCollection == null) {
			markersCollection = new MapMarkersCollection();
		} else if (currentMarker != null) {
			markersCollection.removeMarker(currentMarker.marker);
			currentMarker.reset();
		}
		currentMarker = new MyLocMapMarker();


		MapMarkerBuilder myLocMarkerBuilder = new MapMarkerBuilder();
		myLocMarkerBuilder.setMarkerId(id);
		myLocMarkerBuilder.setIsAccuracyCircleSupported(true);
		myLocMarkerBuilder.setAccuracyCircleBaseColor(NativeUtilities.createFColorRGB(baseColor));
		myLocMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical);
		myLocMarkerBuilder.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
		myLocMarkerBuilder.setIsHidden(true);

		float scale = getTextScale();
		Bitmap markerBitmap = AndroidUtils.createScaledBitmap(icon, scale);
		if (markerBitmap != null) {
			currentMarker.onSurfaceIconKey = SwigUtilities.getOnSurfaceIconKey(1);
			myLocMarkerBuilder.addOnMapSurfaceIcon(currentMarker.onSurfaceIconKey,
					NativeUtilities.createSkImageFromBitmap(markerBitmap));
		}

		if (addHeading) {
			Bitmap headingBitmap = AndroidUtils.createScaledBitmapWithTint(view.getContext(), headingIconId, scale, baseColor);
			if (headingBitmap != null) {
				onSurfaceHeadingIconKey = SwigUtilities.getOnSurfaceIconKey(2);
				myLocMarkerBuilder.addOnMapSurfaceIcon(onSurfaceHeadingIconKey,
						NativeUtilities.createSkImageFromBitmap(headingBitmap));
			}
		}
		currentMarker.marker = myLocMarkerBuilder.buildAndAddToCollection(markersCollection);

		return currentMarker;
	}

	private void resetMarkerProvider() {
		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && markersCollection != null) {
			mapRenderer.removeSymbolsProvider(markersCollection);
			markersCollection.delete();
			markersCollection = null;
			if (myLocationMarker != null) {
				myLocationMarker.reset();
			}
			if (navigationMarker != null) {
				navigationMarker.reset();
			}
			if (myLocationMarkerWithHeading != null) {
				myLocationMarkerWithHeading.reset();
			}
			if (navigationMarkerWithHeading != null) {
				navigationMarkerWithHeading.reset();
			}
		}
	}

	private void setMarkerProvider() {
		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && markersCollection != null) {
			mapRenderer.addSymbolsProvider(markersCollection);
		}
	}

	private void invalidateMarkerCollection() {
		if (view == null || !view.hasMapRenderer()) {
			return;
		}

		resetMarkerProvider();
		myLocationMarker = recreateMarker(myLocationMarker, locationIcon, MARKER_ID_MY_LOCATION, color, false);
		myLocationMarkerWithHeading = recreateMarker(myLocationMarkerWithHeading, locationIcon, MARKER_ID_MY_LOCATION_HEADING, color, true);
		navigationMarker = recreateMarker(navigationMarker, navigationIcon, MARKER_ID_NAVIGATION, color, false);
		navigationMarkerWithHeading = recreateMarker(navigationMarkerWithHeading, navigationIcon, MARKER_ID_NAVIGATION_HEADING, color, true);
		setMarkerProvider();
		updateMarkerState();

		getApplication().runInUIThread(() -> updateMarkerData(lastKnownLocation, locationProvider.getHeading()));
	}

	private void updateMarkerState() {
		if (navigationMarker == null || myLocationMarker == null
				|| navigationMarkerWithHeading == null || myLocationMarkerWithHeading == null) {
			return;
		}

		switch (currentMarkerState) {
			case Move:
				navigationMarker.setVisibility(!hasHeading);
				myLocationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(hasHeading);
				myLocationMarkerWithHeading.setVisibility(false);
				break;
			case Stay:
				navigationMarker.setVisibility(false);
				myLocationMarker.setVisibility(!hasHeading);
				navigationMarkerWithHeading.setVisibility(false);
				myLocationMarkerWithHeading.setVisibility(hasHeading);
				break;
			case None:
			default:
				navigationMarker.setVisibility(false);
				myLocationMarker.setVisibility(false);
				navigationMarkerWithHeading.setVisibility(false);
				myLocationMarkerWithHeading.setVisibility(false);
		}
	}

	private void updateMarkerData(@Nullable Location location, @Nullable Float heading) {
		MyLocMapMarker locMarker = null;
		SWIGTYPE_p_void bearingIconKey = null;
		switch (currentMarkerState) {
			case Move:
				locMarker = hasHeading ? navigationMarkerWithHeading : navigationMarker;
				bearingIconKey = locMarker.onSurfaceIconKey;
				break;
			case Stay:
				locMarker = hasHeading ? myLocationMarkerWithHeading : myLocationMarker;
				break;
			case None:
			default:
				return;
		}

		if (locMarker.marker != null) {
			if (location != null) {  // location
				final PointI target31 = Utilities.convertLatLonTo31(
						new net.osmand.core.jni.LatLon(location.getLatitude(), location.getLongitude()));
				locMarker.marker.setPosition(target31);
				if (bearingIconKey != null) {  // bearing
					locMarker.marker.setOnMapSurfaceIconDirection(bearingIconKey, location.getBearing() - 90.0f);
				}
				locMarker.marker.setAccuracyCircleRadius(location.getAccuracy());
			}
			if (heading != null) {  // heading
				locMarker.marker.setOnMapSurfaceIconDirection(onSurfaceHeadingIconKey, heading);
			}
			if (locMarker.marker.isHidden()) {
				locMarker.marker.setIsHidden(false);
			}
		}
	}

	public PointLocationLayer(@NonNull Context context) {
		super(context);
		this.mapViewTrackingUtilities = getApplication().getMapViewTrackingUtilities();
		locationProvider = getApplication().getLocationProvider();
	}

	private void initUI() {
		headingPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		bitmapPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		area = new Paint();
		aroundArea = new Paint();
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
		updateIcons(view.getSettings().getApplicationMode(),
				false, locationProvider.getLastKnownLocation() == null);
	}

	private void initGpuRenderer() {
		if (view == null || !view.hasMapRenderer()) {
			return;
		}

		lastKnownLocation = locationProvider.getLastStaleKnownLocation();
		markersNeedInvalidate = true;
		hasHeadingCached = false;
		setMarkerState(MarkerState.Stay);
		updateIcons(view.getSettings().getApplicationMode(), getApplication().getDaynightHelper().isNightMode(),
				locationProvider.getLastKnownLocation() == null);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null && view != null && view.hasMapRenderer()) {
			initGpuRenderer();
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		locationProvider.addLocationListener(this);
		locationProvider.addCompassListener(this);

		if (view.hasMapRenderer()) {
			initGpuRenderer();
		} else {
			initUI();
		}
	}

	private RectF getHeadingRect(int locationX, int locationY) {
		int rad = (int) (view.getDensity() * 60);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}

	private void invalidateMarkerState(@NonNull Location lastKnownLocation) {
		boolean isBearing = lastKnownLocation.hasBearing() && (lastKnownLocation.getBearing() != 0.0f)
				&& (!lastKnownLocation.hasSpeed() || lastKnownLocation.getSpeed() > BEARING_SPEED_THRESHOLD);
		if (isBearing && !locationOutdated) {  // navigation
			setMarkerState(MarkerState.Move);
		} else {  // location
			setMarkerState(MarkerState.Stay);
		}
	}

	private void drawMarkersCpu(Canvas canvas, RotatedTileBox box, Location lastKnownLocation) {
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
			if (!locationOutdated && heading != null && mapViewTrackingUtilities.isShowViewAngle()) {
				canvas.save();
				canvas.rotate(heading - 180, locationX, locationY);
				canvas.drawBitmap(headingIcon, locationX - headingIcon.getWidth() / 2,
						locationY - headingIcon.getHeight() / 2, headingPaint);
				canvas.restore();
			}
			// Issue 5538: Some devices return positives for hasBearing() at rest, hence add 0.0 check:
			boolean isBearing = lastKnownLocation.hasBearing() && (lastKnownLocation.getBearing() != 0.0)
					&& (!lastKnownLocation.hasSpeed() || lastKnownLocation.getSpeed() > 0.1);
			if (!locationOutdated && isBearing) {
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
					canvas.drawBitmap(bitmap, locationX - width / 2, locationY - height / 2, bitmapPaint);
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
					canvas.drawBitmap(bitmap, locationX - width / 2, locationY - height / 2, bitmapPaint);
				}
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (view == null || tileBox.getZoom() < MIN_ZOOM_MARKER_VISIBILITY || lastKnownLocation == null) {
			return;
		}

		boolean nm = settings != null && settings.isNightMode();
		updateIcons(view.getSettings().getApplicationMode(), nm, locationProvider.getLastKnownLocation() == null);

		if (view.hasMapRenderer()) {
			if (markersNeedInvalidate) {
				invalidateMarkerCollection();
				markersNeedInvalidate = false;
			}

			hasHeading = !locationOutdated && locationProvider.getHeading() != null && mapViewTrackingUtilities.isShowViewAngle();
			if (hasHeading != hasHeadingCached) {
				hasHeadingCached = hasHeading;
				updateMarkerState();
			}

			invalidateMarkerState(lastKnownLocation);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (view == null || tileBox.getZoom() < MIN_ZOOM_MARKER_VISIBILITY || lastKnownLocation == null) {
			return;
		}

		// legacy rendering
		if (!view.hasMapRenderer()) {
			drawMarkersCpu(canvas, tileBox, lastKnownLocation);
		}
	}

	private boolean isLocationVisible(RotatedTileBox tb, Location l) {
		return l != null && tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}

	@Override
	public void destroyLayer() {
		if (view == null) {
			return;
		}

		locationProvider.removeLocationListener(this);
		locationProvider.removeCompassListener(this);
		resetMarkerProvider();
	}

	private void updateIcons(ApplicationMode appMode, boolean nighMode, boolean locationOutdated) {
		Context ctx = view.getContext();
		int color = locationOutdated ?
				ContextCompat.getColor(ctx, ProfileIconColors.getOutdatedLocationColor(nighMode)) :
				appMode.getProfileColor(nighMode);
		int locationIconId = appMode.getLocationIcon().getIconId();
		int navigationIconId = appMode.getNavigationIcon().getIconId();
		int headingIconId = appMode.getLocationIcon().getHeadingIconId();
		float textScale = getTextScale();
		boolean carView = getApplication().getOsmandMap().getMapView().isCarView();
		if (appMode != this.appMode || this.nm != nighMode || this.locationOutdated != locationOutdated
				|| this.color != color
				|| this.locationIconId != locationIconId
				|| this.headingIconId != headingIconId
				|| this.navigationIconId != navigationIconId
				|| this.textScale != textScale
				|| this.carView != carView) {
			this.appMode = appMode;
			this.color = color;
			this.nm = nighMode;
			this.locationOutdated = locationOutdated;
			this.locationIconId = locationIconId;
			this.headingIconId = headingIconId;
			this.navigationIconId = navigationIconId;
			this.textScale = textScale;
			this.carView = carView;
			navigationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, navigationIconId);
			if (navigationIcon != null) {
				DrawableCompat.setTint(navigationIcon.getDrawable(1), color);
			}
			headingIcon = getScaledBitmap(headingIconId);
			locationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, locationIconId);
			if (locationIcon != null) {
				DrawableCompat.setTint(DrawableCompat.wrap(locationIcon.getDrawable(1)), color);
			}

			if (!view.hasMapRenderer()) {
				headingPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
				area.setColor(ColorUtilities.getColorWithAlpha(color, 0.16f));
				aroundArea.setColor(color);
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
