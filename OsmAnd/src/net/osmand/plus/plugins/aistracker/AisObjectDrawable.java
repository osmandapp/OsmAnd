package net.osmand.plus.plugins.aistracker;

import static net.osmand.shared.aistracker.AisObjectConstants.INVALID_COG;
import static net.osmand.shared.aistracker.AisObjectConstants.INVALID_HEADING;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.shared.aistracker.AisLatLon;
import net.osmand.shared.aistracker.AisObjType;
import net.osmand.shared.aistracker.AisObject;
import net.osmand.util.MapUtils;

/**
 * Native resources for one admitted AIS target.
 *
 * <p>The layer owns only a bounded number of these objects. Each drawable owns one marker for
 * its current visual state and creates a direction line only while it is actually needed.</p>
 */
public class AisObjectDrawable {

	private final AisTrackerPlugin plugin;
	private final AisObject ais;
	private final AisImagesCache imagesCache;

	private Bitmap bitmap;
	private int bitmapColor;
	private int visualState;
	private MapMarker marker;
	private VectorLine directionLine;
	private int baseOrder;
	private String renderKey;
	private long renderedVersion;
	private double renderedZoom = -1;
	private boolean cpaWarning;
	private boolean ownObject;

	public AisObjectDrawable(@NonNull AisTrackerPlugin plugin, @NonNull AisObject ais) {
		this.plugin = plugin;
		this.ais = new AisObject(ais);
		imagesCache = plugin.getAisImagesCache();
	}

	public void set(@NonNull AisObject ais, int visualState) {
		this.ais.set(ais);
		this.visualState = visualState;
		bitmap = null;
		bitmapColor = 0;
	}

	public void setOwnObject(boolean ownObject) {
		if (this.ownObject != ownObject) {
			this.ownObject = ownObject;
			bitmap = null;
			bitmapColor = 0;
		}
	}

	public long getRenderedVersion() {
		return renderedVersion;
	}

	public void setRenderedVersion(long renderedVersion) {
		this.renderedVersion = renderedVersion;
	}

	public double getRenderedZoom() {
		return renderedZoom;
	}

	public boolean isCpaWarningActive() {
		return cpaWarning;
	}

	public void setSoftwareCpaWarning(boolean cpaWarning) {
		this.cpaWarning = cpaWarning;
	}

	@NonNull
	public String getCurrentRenderKey() {
		int bitmapId = visualState == 0 ? selectBitmap(ais.getObjectClass())
				: visualState == 2 ? R.drawable.mm_ais_vessel_cross : 0;
		return visualState + ":" + bitmapId;
	}

	public boolean isRenderKeyChanged() {
		return renderKey != null && !renderKey.equals(getCurrentRenderKey());
	}

	public static int selectBitmap(@NonNull AisObjType type) {
		return switch (type) {
			case AIS_VESSEL, AIS_VESSEL_SPORT, AIS_VESSEL_FAST, AIS_VESSEL_PASSENGER,
				 AIS_VESSEL_FREIGHT, AIS_VESSEL_COMMERCIAL, AIS_VESSEL_AUTHORITIES, AIS_VESSEL_SAR,
				 AIS_VESSEL_OTHER, AIS_INVALID -> R.drawable.mm_ais_vessel;
			case AIS_LANDSTATION -> R.drawable.mm_ais_land;
			case AIS_AIRPLANE -> R.drawable.mm_ais_plane;
			case AIS_SART -> R.drawable.mm_ais_sar;
			case AIS_ATON -> R.drawable.mm_ais_aton;
			case AIS_ATON_VIRTUAL -> R.drawable.mm_ais_aton_virt;
		};
	}

	public static int selectColor(@NonNull AisObjType type) {
		return switch (type) {
			case AIS_VESSEL -> Color.GREEN;
			case AIS_VESSEL_SPORT -> Color.YELLOW;
			case AIS_VESSEL_FAST -> Color.BLUE;
			case AIS_VESSEL_PASSENGER -> Color.CYAN;
			case AIS_VESSEL_FREIGHT -> Color.GRAY;
			case AIS_VESSEL_COMMERCIAL -> Color.LTGRAY;
			case AIS_VESSEL_AUTHORITIES -> Color.rgb(0x55, 0x6b, 0x2f);
			case AIS_VESSEL_SAR -> Color.rgb(0xfa, 0x80, 0x72);
			case AIS_VESSEL_OTHER -> Color.rgb(0x00, 0xbf, 0xff);
			default -> 0;
		};
	}

	private float getMovement() {
		if (ais.isMovable() && ais.getSog() > 0) {
			if (ais.getSog() < 2) {
				return 0;
			}
			if (ais.getSog() < 5) {
				return 1;
			}
			if (ais.getSog() < 10) {
				return 2;
			}
			if (ais.getSog() < 25) {
				return 3;
			}
			return 5;
		}
		return 0;
	}

	private boolean needRotation() {
		return ais.isMovable() && ((ais.getCog() != INVALID_COG && ais.getCog() != 0)
				|| (ais.getHeading() != INVALID_HEADING && ais.getHeading() != 0));
	}

	private void prepareBitmap() {
		if (visualState == 1) {
			bitmap = null;
		} else {
			bitmap = imagesCache.getBitmap(visualState == 2
					? R.drawable.mm_ais_vessel_cross
					: selectBitmap(ais.getObjectClass()));
		}
		if (ownObject) {
			bitmapColor = Color.BLACK;
		} else if (visualState == 2) {
			bitmapColor = 0;
		} else {
			bitmapColor = selectColor(ais.getObjectClass());
		}
	}

	private void updatePaint(@NonNull Paint paint, boolean cpaWarning) {
		prepareBitmap();
		int color = cpaWarning && !ownObject ? Color.RED : bitmapColor;
		paint.setColorFilter(color == 0 ? null : new LightingColorFilter(color, 0));
	}

	private void drawRestCircle(float x, float y, @NonNull Paint paint, @NonNull Canvas canvas) {
		Paint localPaint = new Paint(paint);
		localPaint.setColorFilter(null);
		localPaint.setStyle(Paint.Style.FILL);
		localPaint.setColor(Color.DKGRAY);
		canvas.drawCircle(x, y, 22, localPaint);
		localPaint.setColor(cpaWarning && !ownObject ? Color.RED : bitmapColor);
		canvas.drawCircle(x, y, 18, localPaint);
	}

	private boolean shouldDrawShape(int zoom) {
		return zoom >= AisTrackerLayer.START_ZOOM_SHOW_SHAPE
				&& ais.getDimensionToBow() + ais.getDimensionToStern() > 0
				&& ais.getDimensionToPort() + ais.getDimensionToStarboard() > 0
				&& visualState != 2;
	}

	private void drawShape(float x, float y, @NonNull RotatedTileBox tileBox,
			@NonNull Paint paint, @NonNull Canvas canvas) {
		if (!shouldDrawShape(tileBox.getZoom())) {
			return;
		}
		float a;
		float b;
		float c;
		float d;
		double density = tileBox.getPixDensity();
		if (ais.getDimensionToBow() == 0 && ais.getDimensionToPort() == 0) {
			a = (float) (ais.getDimensionToStern() * density * 0.5);
			b = a;
			c = (float) (ais.getDimensionToStarboard() * density * 0.5);
			d = c;
		} else {
			a = (float) (ais.getDimensionToBow() * density);
			b = (float) (ais.getDimensionToStern() * density);
			c = (float) (ais.getDimensionToPort() * density);
			d = (float) (ais.getDimensionToStarboard() * density);
		}
		float e = 0.5f * (c + d);
		canvas.drawLine(x - c, y + b, x - c, y - a + e, paint);
		canvas.drawLine(x - c, y - a + e, x - c + e, y - a, paint);
		canvas.drawLine(x - c + e, y - a, x + d, y - a + e, paint);
		canvas.drawLine(x + d, y - a + e, x + d, y + b, paint);
		canvas.drawLine(x + d, y + b, x - c, y + b, paint);
	}

	public void draw(@NonNull Paint paint, @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		updatePaint(paint, cpaWarning);
		AisLatLon position = ais.getPosition();
		if (position == null) {
			return;
		}
		canvas.save();
		canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		float x = tileBox.getPixXFromLonNoRot(position.getLongitude());
		float y = tileBox.getPixYFromLatNoRot(position.getLatitude());
		if (visualState == 1) {
			drawRestCircle(x, y, paint, canvas);
			if (ais.getHeading() != INVALID_HEADING) {
				canvas.rotate(ais.getHeading(), x, y);
				drawShape(x, y, tileBox, paint, canvas);
			}
		} else if (bitmap != null) {
			float rotation = needRotation() ? ais.getVesselRotation() : 0;
			canvas.rotate(rotation, x, y);
			canvas.drawBitmap(bitmap, x - bitmap.getWidth() / 2f, y - bitmap.getHeight() / 2f, paint);
			float movement = getMovement();
			if (tileBox.getZoom() >= AisTrackerLayer.START_ZOOM_SHOW_DIRECTION && movement > 0 && visualState != 2) {
				float startY = y - bitmap.getHeight() / 4f;
				canvas.drawLine(x, startY, x, startY - bitmap.getHeight() * movement, paint);
			}
			if (needRotation() && ais.getHeading() != INVALID_HEADING
					&& ais.getHeading() != 0 && ais.getHeading() != rotation) {
				canvas.rotate(ais.getHeading() - rotation, x, y);
			}
			drawShape(x, y, tileBox, paint, canvas);
		}
		canvas.restore();
	}

	public boolean createAisRenderData(int baseOrder,
			@NonNull MapMarkersCollection markersCollection, @NonNull SingleSkImage restImage) {
		this.baseOrder = baseOrder;
		prepareBitmap();
		SingleSkImage image = visualState == 1
				? restImage
				: bitmap == null ? null : NativeUtilities.createSkImageFromBitmap(bitmap);
		if (image == null) {
			return false;
		}
		MapMarkerBuilder builder = new MapMarkerBuilder();
		builder.setMarkerId(ais.getMmsi());
		builder.setBaseOrder(baseOrder);
		builder.setUpdateAfterCreated(true);
		builder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), image);
		marker = builder.buildAndAddToCollection(markersCollection);
		renderKey = getCurrentRenderKey();
		return marker != null;
	}

	public boolean hasAisRenderData() {
		return marker != null;
	}

	public boolean hasAnyAisRenderData() {
		return marker != null || directionLine != null;
	}

	public void updateAisRenderData(@Nullable OsmandMapTileView mapView, boolean cpaWarning,
			boolean visible, @NonNull VectorLinesCollection vectorLinesCollection) {
		if (marker == null) {
			return;
		}
		prepareBitmap();
		this.cpaWarning = cpaWarning;
		int zoom = mapView == null ? 0 : mapView.getZoom();
		AisLatLon position = ais.getPosition();
		if (!visible || mapView == null || position == null) {
			marker.setIsHidden(true);
			removeDirectionLine(vectorLinesCollection);
			return;
		}

		PointI location = new PointI(MapUtils.get31TileNumberX(position.getLongitude()),
				MapUtils.get31TileNumberY(position.getLatitude()));
		float rotation = visualState != 1 && needRotation()
				? (ais.getVesselRotation() + 180f) % 360f : 0;
		int color = cpaWarning && !ownObject ? Color.RED : bitmapColor;
		ColorARGB iconColor = NativeUtilities.createColorARGB(color == 0 ? Color.WHITE : color);
		marker.setOnSurfaceIconModulationColor(iconColor);
		marker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rotation);
		marker.setPosition(location);
		marker.setIsHidden(false);

		float movement = getMovement();
		boolean drawDirection = zoom >= AisTrackerLayer.START_ZOOM_SHOW_DIRECTION
				&& movement > 0 && visualState == 0;
		if (!drawDirection) {
			removeDirectionLine(vectorLinesCollection);
		} else {
			if (directionLine == null) {
				QVectorPointI initialPoints = new QVectorPointI();
				initialPoints.add(location);
				initialPoints.add(new PointI(location.getX() + 1, location.getY() + 1));
				VectorLineBuilder builder = new VectorLineBuilder();
				builder.setLineId(ais.getMmsi());
				builder.setBaseOrder(baseOrder + 10);
				builder.setFillColor(NativeUtilities.createFColorARGB(Color.BLACK));
				builder.setLineWidth(6);
				builder.setPoints(initialPoints);
				directionLine = builder.buildAndAddToCollection(vectorLinesCollection);
			}
			if (directionLine != null) {
				int bitmapHeight = bitmap == null ? 48 : bitmap.getHeight();
				int inverseZoom = mapView.getMaxZoom() - zoom;
				float zoomFactor = (float) MapUtils.getPowZoom(inverseZoom);
				// Start slightly below the visible vessel tip. AIS icons contain transparent
				// padding, so using the geometric icon edge leaves a noticeable gap.
				float startOffset = bitmapHeight * 0.34f * zoomFactor;
				float length = Math.max(movement * bitmapHeight * 0.75f * zoomFactor,
						startOffset + bitmapHeight * 0.25f * zoomFactor);
				double theta = Math.toRadians(rotation);
				int startDx = (int) Math.ceil(-Math.sin(theta) * startOffset);
				int startDy = (int) Math.ceil(Math.cos(theta) * startOffset);
				int dx = (int) Math.ceil(-Math.sin(theta) * length);
				int dy = (int) Math.ceil(Math.cos(theta) * length);
				QVectorPointI points = new QVectorPointI();
				points.add(new PointI(location.getX() + startDx, location.getY() + startDy));
				points.add(new PointI(location.getX() + dx, location.getY() + dy));
				directionLine.setPoints(points);
				directionLine.setIsHidden(false);
			}
		}
		renderedZoom = mapView.getZoom() + mapView.getZoomFloatPart();
	}

	private void removeDirectionLine(@NonNull VectorLinesCollection vectorLinesCollection) {
		if (directionLine != null) {
			directionLine.setIsHidden(true);
			vectorLinesCollection.removeLine(directionLine);
			directionLine = null;
		}
	}

	public void clearAisRenderData(@NonNull MapMarkersCollection markersCollection,
			@NonNull VectorLinesCollection vectorLinesCollection) {
		if (marker != null) {
			markersCollection.removeMarker(marker);
			marker = null;
		}
		removeDirectionLine(vectorLinesCollection);
		renderKey = null;
		renderedVersion = 0;
		renderedZoom = -1;
	}
}
