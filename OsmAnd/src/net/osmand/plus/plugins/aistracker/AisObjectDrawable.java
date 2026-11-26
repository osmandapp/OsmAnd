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

import net.osmand.core.android.MapRendererView;
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

	private static final int NATIVE_LINE_WIDTH = 6;

	private final AisTrackerPlugin plugin;
	private final AisObject ais;
	private final AisImagesCache imagesCache;

	private Bitmap bitmap;
	private int bitmapColor;
	private int visualState;
	private MapMarker marker;
	private VectorLine directionLine;
	private VectorLine shapeLine;
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
		double density = tileBox.getPixDensity();
		double[][] outline = getShapeOutline();
		for (int i = 0; i + 1 < outline.length; i++) {
			canvas.drawLine((float) (x + outline[i][0] * density), (float) (y - outline[i][1] * density),
					(float) (x + outline[i + 1][0] * density), (float) (y - outline[i + 1][1] * density), paint);
		}
	}

	/**
	 * Vessel outline in meters from the AIS position: x to starboard, y towards the bow.
	 * The first point is repeated at the end to close the outline.
	 */
	@NonNull
	private double[][] getShapeOutline() {
		double a;
		double b;
		double c;
		double d;
		if (ais.getDimensionToBow() == 0 && ais.getDimensionToPort() == 0) {
			// Only the overall size is reported, so the shape is centered on the position
			a = ais.getDimensionToStern() * 0.5;
			b = a;
			c = ais.getDimensionToStarboard() * 0.5;
			d = c;
		} else {
			a = ais.getDimensionToBow();
			b = ais.getDimensionToStern();
			c = ais.getDimensionToPort();
			d = ais.getDimensionToStarboard();
		}
		double e = 0.5 * (c + d);
		return new double[][] {{-c, -b}, {-c, a - e}, {-c + e, a}, {d, a - e}, {d, -b}, {-c, -b}};
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
		return marker != null || directionLine != null || shapeLine != null;
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
		MapRendererView mapRenderer = mapView == null ? null : mapView.getMapRenderer();
		if (!visible || mapRenderer == null || position == null) {
			marker.setIsHidden(true);
			removeDirectionLine(vectorLinesCollection);
			removeShapeLine(vectorLinesCollection);
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

		updateShapeLine(position, zoom, vectorLinesCollection);

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
				builder.setLineWidth(NATIVE_LINE_WIDTH);
				builder.setPoints(initialPoints);
				directionLine = builder.buildAndAddToCollection(vectorLinesCollection);
			}
			if (directionLine != null) {
				int bitmapHeight = bitmap == null ? 48 : bitmap.getHeight();
				float points31InPixel = get31PointsInPixel(mapRenderer);
				// Same pixel distances as the Version 1 renderer in draw()
				float startOffset = bitmapHeight / 4f * points31InPixel;
				float length = startOffset + movement * bitmapHeight * points31InPixel;
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

	private float get31PointsInPixel(@NonNull MapRendererView mapRenderer) {
		long tileSize31 = 1L << (31 - mapRenderer.getZoomLevel().ordinal());
		return (float) (tileSize31 / mapRenderer.getTileSizeOnScreenInPixels());
	}

	private boolean shouldDrawNativeShape(int zoom) {
		return shouldDrawShape(zoom) && (visualState != 1 || ais.getHeading() != INVALID_HEADING);
	}

	private float getShapeBearing() {
		if (visualState == 1) {
			return ais.getHeading();
		}
		if (!needRotation()) {
			return 0;
		}
		return ais.getHeading() != INVALID_HEADING && ais.getHeading() != 0
				? ais.getHeading() : ais.getVesselRotation();
	}

	@NonNull
	private QVectorPointI getShapePoints(@NonNull AisLatLon position) {
		double bearing = Math.toRadians(getShapeBearing());
		double sin = Math.sin(bearing);
		double cos = Math.cos(bearing);
		double metersInLatitudeDegree = MapUtils.METERS_IN_DEGREE;
		double metersInLongitudeDegree = metersInLatitudeDegree * Math.cos(Math.toRadians(position.getLatitude()));
		QVectorPointI points = new QVectorPointI();
		for (double[] point : getShapeOutline()) {
			double east = point[0] * cos + point[1] * sin;
			double north = point[1] * cos - point[0] * sin;
			points.add(new PointI(
					MapUtils.get31TileNumberX(position.getLongitude() + east / metersInLongitudeDegree),
					MapUtils.get31TileNumberY(position.getLatitude() + north / metersInLatitudeDegree)));
		}
		return points;
	}

	private void updateShapeLine(@NonNull AisLatLon position, int zoom,
			@NonNull VectorLinesCollection vectorLinesCollection) {
		if (!shouldDrawNativeShape(zoom)) {
			removeShapeLine(vectorLinesCollection);
			return;
		}
		QVectorPointI points = getShapePoints(position);
		if (shapeLine == null) {
			VectorLineBuilder builder = new VectorLineBuilder();
			builder.setLineId(-ais.getMmsi());
			builder.setBaseOrder(baseOrder + 10);
			builder.setFillColor(NativeUtilities.createFColorARGB(Color.BLACK));
			builder.setLineWidth(NATIVE_LINE_WIDTH);
			builder.setPoints(points);
			shapeLine = builder.buildAndAddToCollection(vectorLinesCollection);
		} else {
			shapeLine.setPoints(points);
		}
		if (shapeLine != null) {
			shapeLine.setIsHidden(false);
		}
	}

	private void removeDirectionLine(@NonNull VectorLinesCollection vectorLinesCollection) {
		if (directionLine != null) {
			directionLine.setIsHidden(true);
			vectorLinesCollection.removeLine(directionLine);
			directionLine = null;
		}
	}

	private void removeShapeLine(@NonNull VectorLinesCollection vectorLinesCollection) {
		if (shapeLine != null) {
			shapeLine.setIsHidden(true);
			vectorLinesCollection.removeLine(shapeLine);
			shapeLine = null;
		}
	}

	public void clearAisRenderData(@NonNull MapMarkersCollection markersCollection,
			@NonNull VectorLinesCollection vectorLinesCollection) {
		if (marker != null) {
			markersCollection.removeMarker(marker);
			marker = null;
		}
		removeDirectionLine(vectorLinesCollection);
		removeShapeLine(vectorLinesCollection);
		renderKey = null;
		renderedVersion = 0;
		renderedZoom = -1;
	}
}
