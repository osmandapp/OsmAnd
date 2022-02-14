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

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.FColorRGB;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SWIGTYPE_p_void;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.Utilities;
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
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import org.apache.commons.logging.Log;

import java.nio.ByteBuffer;
import java.util.List;

public class PointLocationLayer extends OsmandMapLayer implements IContextMenuProvider {
	private static final Log LOG = PlatformUtil.getLog(PointLocationLayer.class);

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
	private MapMarkersCollection markersCollection;
	private MapMarker navigationMarker;
	private static final int MARKER_ID_NAVIGATION = 2;
	//navigation pin marker
	private MapMarker myLocationMarker;
	private static final int MARKER_ID_MY_LOCATION = 1;
	private SWIGTYPE_p_void onSurfaceIconKey = null;
	private SWIGTYPE_p_void onSurfaceHeadingIconKey = null;

	private byte[] getBitmapAsByteArray(@NonNull Bitmap bitmap) {
		int size = bitmap.getRowBytes() * bitmap.getHeight();
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		bitmap.copyPixelsToBuffer(byteBuffer);
		return byteBuffer.array();
	}

	private MapMarker updateMarker(MapMarker oldMarker, LayerDrawable icon, int id) {
		if (view == null) {
			return null;
		}

		final MapRendererView mapGpuRenderer = view.getMapRenderer();
		if (mapGpuRenderer == null) { return null; }

		boolean newCollection = false;
		if (markersCollection == null) {
			markersCollection = new MapMarkersCollection();
			newCollection = true;
		} else if (oldMarker != null) {
			mapGpuRenderer.suspendSymbolsUpdate();
			oldMarker.setIsHidden(true);
			oldMarker.setIsAccuracyCircleVisible(false);
			markersCollection.removeMarker(oldMarker);
			mapGpuRenderer.resumeSymbolsUpdate();
			//collection.getMarkers();
		}

		MapMarkerBuilder myLocMarkerBuilder = new MapMarkerBuilder();
		myLocMarkerBuilder.setMarkerId(id);
		myLocMarkerBuilder.setIsAccuracyCircleSupported(true);
		myLocMarkerBuilder.setAccuracyCircleBaseColor(new FColorRGB((color >> 16 & 0xff)/255.0f,
																	((color >> 8) & 0xff)/255.0f,
																	((color) & 0xff)/255.0f));
		myLocMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top);
		myLocMarkerBuilder.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
		myLocMarkerBuilder.setBaseOrder(-206000);
		myLocMarkerBuilder.setIsHidden(true);

		int width = (int) (icon.getIntrinsicWidth());  // * textScale);
		int height = (int) (icon.getIntrinsicHeight() );  //* textScale);
		//width += width % 2 == 1 ? 1 : 0;
		//height += height % 2 == 1 ? 1 : 0;
		//icon.setBounds(0, 0, width, height);
		if (onSurfaceIconKey != null) {
			myLocMarkerBuilder.removeOnMapSurfaceIcon(onSurfaceIconKey);
		}
		if (onSurfaceHeadingIconKey != null) {
			myLocMarkerBuilder.removeOnMapSurfaceIcon(onSurfaceHeadingIconKey);
		}

		Bitmap myLocationBitmap = AndroidUtils.createScaledBitmap(icon, width, height);
		if (myLocationBitmap != null) {
			SWIGTYPE_p_sk_spT_SkImage_const_t swigImg = SwigUtilities.createSkImageARGB888With(
					myLocationBitmap.getWidth(), myLocationBitmap.getHeight(), getBitmapAsByteArray(myLocationBitmap));
			onSurfaceIconKey = SwigUtilities.getOnSurfaceIconKey(1);
			myLocMarkerBuilder.addOnMapSurfaceIcon(onSurfaceIconKey, swigImg);
		}
//		if (headingIcon != null) {
//			SWIGTYPE_p_sk_spT_SkImage_const_t swigImg = SwigUtilities.createSkImageARGB888With(
//					headingIcon.getWidth(), headingIcon.getHeight(), getBitmapAsByteArray(headingIcon));
//			onSurfaceHeadingIconKey = SwigUtilities.getOnSurfaceIconKey(2);
//			myLocMarkerBuilder.addOnMapSurfaceIcon(onSurfaceHeadingIconKey, swigImg);
//		}

		MapMarker marker = myLocMarkerBuilder.buildAndAddToCollection(markersCollection);
		if (newCollection) {
			mapGpuRenderer.addSymbolsProvider(markersCollection);
		}
		return marker;
	}

	private void invalidateMarkers() {
		if (view == null) {
			return;
		}

		if (view.hasGpuRenderer()) {
			myLocationMarker = updateMarker(myLocationMarker, locationIcon, MARKER_ID_MY_LOCATION);
			navigationMarker = updateMarker(navigationMarker, navigationIcon, MARKER_ID_NAVIGATION);
		}
	}

	public PointLocationLayer(@NonNull Context context) {
		super(context);
		this.mapViewTrackingUtilities = getApplication().getMapViewTrackingUtilities();
	}

	private void initUI() {
		headingPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		bitmapPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		area = new Paint();
		aroundArea = new Paint();
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
		locationProvider = view.getApplication().getLocationProvider();
		updateIcons(view.getSettings().getApplicationMode(), false, locationProvider.getLastKnownLocation() == null);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		initUI();

		final MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.setMapRendererContextListener(() -> {
				invalidateMarkers();
			});
		}
	}

	private RectF getHeadingRect(int locationX, int locationY){
		int rad = (int) (view.getDensity() * 60);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		if (view == null || box.getZoom() < 3) {
			return;
		}
		// draw
		boolean nm = nightMode != null && nightMode.isNightMode();
		Location lastKnownLocation = locationProvider.getLastStaleKnownLocation();
		updateIcons(view.getSettings().getApplicationMode(), nm,
				view.getApplication().getLocationProvider().getLastKnownLocation() == null);
		if (lastKnownLocation == null) {
			return;
		}

		// rendering
		final MapRendererView mapGpuRenderer = view.getMapRenderer();
		if (mapGpuRenderer != null) {
/////////////////////////////////////////////////GPU////////////////////////////////////////////////
			if (isLocationVisible(box, lastKnownLocation)) {
				// ToDo heading
				Bitmap headingImage = null;
				Float heading = locationProvider.getHeading();
				if (!locationOutdated && heading != null && mapViewTrackingUtilities.isShowViewAngle()) {
					headingImage = headingIcon;
				}
				final PointI target31 = Utilities.convertLatLonTo31(
						new net.osmand.core.jni.LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
				boolean isBearing = lastKnownLocation.hasBearing() && (lastKnownLocation.getBearing() != 0.0)
						&& (!lastKnownLocation.hasSpeed() || lastKnownLocation.getSpeed() > 0.1);
				if (!locationOutdated && isBearing) {
					// ToDo navigationIcon
					mapGpuRenderer.suspendSymbolsUpdate();
					myLocationMarker.setIsHidden(true);
					navigationMarker.setIsHidden(false);
					navigationMarker.setPosition(target31);
					boolean hasAccuracy = lastKnownLocation.hasAccuracy();
					if (hasAccuracy) {
						navigationMarker.setAccuracyCircleRadius(lastKnownLocation.getAccuracy());
					}
					navigationMarker.setIsAccuracyCircleVisible(hasAccuracy);
					myLocationMarker.setIsAccuracyCircleVisible(!hasAccuracy);

					if (heading != null) {
						navigationMarker.setOnMapSurfaceIconDirection(onSurfaceIconKey, heading);
						if (mapViewTrackingUtilities.isShowViewAngle()) {
							//navigationMarker.setOnMapSurfaceIconDirection(onSurfaceHeadingIconKey, heading);
						}
					}
					mapGpuRenderer.resumeSymbolsUpdate();
				} else {
					// ToDo location
					mapGpuRenderer.suspendSymbolsUpdate();
					navigationMarker.setIsHidden(true);
					myLocationMarker.setIsHidden(false);
					myLocationMarker.setPosition(target31);
					boolean hasAccuracy = lastKnownLocation.hasAccuracy();
					if (hasAccuracy) {
						myLocationMarker.setAccuracyCircleRadius(lastKnownLocation.getAccuracy());
					}
					myLocationMarker.setIsAccuracyCircleVisible(hasAccuracy);
					navigationMarker.setIsAccuracyCircleVisible(!hasAccuracy);

					if (heading != null) {
						myLocationMarker.setOnMapSurfaceIconDirection(onSurfaceIconKey, heading);
						if (mapViewTrackingUtilities.isShowViewAngle()) {
							//myLocationMarker.setOnMapSurfaceIconDirection(onSurfaceHeadingIconKey, heading);
						}
					}
					mapGpuRenderer.resumeSymbolsUpdate();
				}
			}
		} else {
/////////////////////////////////////////////////CPU////////////////////////////////////////////////
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
	}

	private boolean isLocationVisible(RotatedTileBox tb, Location l) {
		return l != null && tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}

	@Override
	public void destroyLayer() {
		if (view == null) { return; }
		final MapRendererView mapGpuRenderer = view.getMapRenderer();
		if (mapGpuRenderer != null) {
			mapGpuRenderer.suspendSymbolsUpdate();
			navigationMarker.setIsHidden(true);
			navigationMarker.setIsAccuracyCircleVisible(false);
			markersCollection.removeMarker(navigationMarker);
			myLocationMarker.setIsHidden(true);
			myLocationMarker.setIsAccuracyCircleVisible(false);
			markersCollection.removeMarker(myLocationMarker);
			mapGpuRenderer.resumeSymbolsUpdate();
			//collection.getMarkers();
			markersCollection.removeAllMarkers();
			mapGpuRenderer.removeSymbolsProvider(markersCollection);
		}
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
			headingPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
			locationIcon = (LayerDrawable) AppCompatResources.getDrawable(ctx, locationIconId);
			if (locationIcon != null) {
				DrawableCompat.setTint(DrawableCompat.wrap(locationIcon.getDrawable(1)), color);
			}
			area.setColor(ColorUtilities.getColorWithAlpha(color, 0.16f));
			aroundArea.setColor(color);

			invalidateMarkers();
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
