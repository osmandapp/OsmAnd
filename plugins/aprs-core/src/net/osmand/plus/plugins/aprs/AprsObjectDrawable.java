package net.osmand.plus.plugins.aprs;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

public class AprsObjectDrawable {

	/** Transparent margin so OpenGL on-surface icons are not clipped at the top edge. */
	private static final float NATIVE_TOP_GUARD_RATIO = 0.20f;

	private final AprsPlugin plugin;
	private AprsStation station;
	private Bitmap bitmap;
	private Bitmap nativeBitmap;
	private MapMarker marker;
	private float lastRotation = Float.NaN;
	private double lastLoggedLat = Double.NaN;
	private double lastLoggedLon = Double.NaN;

	public AprsObjectDrawable(@NonNull AprsPlugin plugin, @NonNull AprsStation station) {
		this.plugin = plugin;
		this.station = station;
		refreshBitmap();
	}

	public void set(@NonNull AprsStation station) {
		this.station = station;
		refreshBitmap();
	}

	@NonNull
	public AprsStation getStation() {
		return station;
	}

	public double getQrvFrequencyMhz() {
		return station.getQrvFrequencyMhz();
	}

	private void refreshBitmap() {
		Bitmap raw = plugin.getSymbolResolver().getSymbolBitmap(station.getSymbolTable(), station.getSymbolCode());
		if (raw == null) {
			bitmap = null;
			nativeBitmap = null;
			return;
		}
		bitmap = raw;
		nativeBitmap = padBitmapForNativeAnchor(raw);
	}

	private boolean isDirectedMovingSymbol() {
		return AprsSymbolResolver.isDirectedMovingSymbol(station.getSymbolTable(), station.getSymbolCode());
	}

	private float getCanvasRotationDeg() {
		if (station.getCourse() < 0 || !isDirectedMovingSymbol()) {
			return Float.NaN;
		}
		return station.getCourse();
	}

	/**
	 * Hessu/aprs-symbols assets point north at 0 degrees. Use course directly.
	 * AIS vessel bitmaps point the opposite way and need +180; APRS symbols do not.
	 */
	private float getNativeRotationDeg() {
		if (station.getCourse() < 0 || !isDirectedMovingSymbol()) {
			return Float.NaN;
		}
		return station.getCourse();
	}

	/**
	 * Native markers anchor at the bitmap centre. Add bottom padding so the centre
	 * sits on the bottom-centre of the visible symbol, plus a top guard band against clipping.
	 */
	@NonNull
	private Bitmap padBitmapForNativeAnchor(@NonNull Bitmap src) {
		int topGuard = Math.round(src.getHeight() * NATIVE_TOP_GUARD_RATIO);
		int bottomPad = src.getHeight();
		int outH = topGuard + src.getHeight() + bottomPad;
		Bitmap out = Bitmap.createBitmap(src.getWidth(), outH, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(out);
		canvas.drawBitmap(src, 0, topGuard, null);
		return out;
	}

	public void createRenderData(int baseOrder, @NonNull MapMarkersCollection collection) {
		if (nativeBitmap == null) {
			return;
		}
		SingleSkImage image = NativeUtilities.createSkImageFromBitmap(nativeBitmap);
		MapMarkerBuilder builder = new MapMarkerBuilder();
		builder.setBaseOrder(baseOrder);
		builder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), image);
		builder.setIsHidden(true);
		marker = builder.buildAndAddToCollection(collection);
	}

	public boolean hasRenderData() {
		return marker != null;
	}

	public void updateRenderData(@Nullable OsmandMapTileView mapView) {
		if (marker == null || !station.hasPosition()) {
			if (marker != null) {
				marker.setIsHidden(true);
			}
			return;
		}
		int zoom = mapView != null ? mapView.getZoom() : 0;
		if (zoom < AprsLayer.START_ZOOM) {
			marker.setIsHidden(true);
			return;
		}
		marker.setIsHidden(false);
		PointI loc = new PointI(
				MapUtils.get31TileNumberX(station.getLongitude()),
				MapUtils.get31TileNumberY(station.getLatitude())
		);
		marker.setPosition(loc);

		if (Math.abs(station.getLatitude() - lastLoggedLat) > 1e-6
				|| Math.abs(station.getLongitude() - lastLoggedLon) > 1e-6) {
			Log.d("APRS_MOVE", station.getCallsign() + " -> "
					+ station.getLatitude() + "," + station.getLongitude()
					+ " course=" + station.getCourse()
					+ " at t=" + System.currentTimeMillis());
			lastLoggedLat = station.getLatitude();
			lastLoggedLon = station.getLongitude();
		}

		float rot = getNativeRotationDeg();
		if (!Float.isNaN(rot)) {
			if (Float.isNaN(lastRotation) || Math.abs(lastRotation - rot) > 0.5f) {
				marker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rot);
				lastRotation = rot;
			}
		}
	}

	public void clearRenderData(@NonNull MapMarkersCollection collection) {
		if (marker != null) {
			collection.removeMarker(marker);
			marker = null;
		}
	}

	public void draw(@NonNull Paint paint, @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		if (bitmap == null || !station.hasPosition() || tileBox.getZoom() < AprsLayer.START_ZOOM) {
			return;
		}
		canvas.save();
		canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		int x = tileBox.getPixXFromLonNoRot(station.getLongitude());
		int y = tileBox.getPixYFromLatNoRot(station.getLatitude());
		float anchorX = x;
		float anchorY = y;
		float rot = getCanvasRotationDeg();
		if (!Float.isNaN(rot)) {
			Matrix matrix = new Matrix();
			matrix.postTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight());
			matrix.postRotate(rot);
			matrix.postTranslate(anchorX, anchorY);
			canvas.drawBitmap(bitmap, matrix, paint);
		} else {
			float fx = anchorX - bitmap.getWidth() / 2f;
			float fy = anchorY - bitmap.getHeight();
			canvas.drawBitmap(bitmap, Math.round(fx), Math.round(fy), paint);
		}
		canvas.restore();
	}
}
