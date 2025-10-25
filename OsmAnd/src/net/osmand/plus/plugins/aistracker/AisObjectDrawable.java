package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_AIRPLANE;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.CPA_UPDATE_TIMEOUT_IN_SECONDS;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_COG;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_HEADING;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getCpa;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
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
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.plugins.aistracker.AisTrackerHelper.Cpa;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

public class AisObjectDrawable {

	private final AisObject ais;
	private final AisImagesCache imagesCache;

	private Bitmap bitmap = null;
	private boolean bitmapValid = false;
	private int bitmapColor;
	private long lastCpaUpdate = 0;
	private MapMarker activeMarker;
	private MapMarker restMarker;
	private MapMarker lostMarker;
	private VectorLine directionLine;

	public AisObjectDrawable(@NonNull AisObject ais) {
		this.ais = ais;
		this.imagesCache = ais.getPlugin().getAisImagesCache();
	}

	@NonNull
	public AisTrackerPlugin getPlugin() {
		return ais.getPlugin();
	}

	private void invalidateBitmap() {
		this.bitmapValid = false;
	}

	public void set(@NonNull AisObject ais) {
		this.ais.set(ais);

		this.invalidateBitmap();
		this.bitmapColor = 0;
	}

	private void activateCpaWarning() {
		bitmapColor = Color.RED;
	}

	private void deactivateCpaWarning() {
		if (bitmapColor == Color.RED) {
			setColor(ais.isVesselAtRest());
		}
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
			case AIS_VESSEL_AUTHORITIES ->
					Color.argb(0xff, 0x55, 0x6b, 0x2f); // 0x556b2f: darkolivegreen
			case AIS_VESSEL_SAR -> Color.argb(0xff, 0xfa, 0x80, 0x72); // 0xfa8072: salmon
			case AIS_VESSEL_OTHER -> Color.argb(0xff, 0x00, 0xbf, 0xff); // 0x00bfff: deepskyblue
			default -> 0; // default icon
		};
	}

	/*
	for AIS objects that are moving, return a value that is taken as multiple of bitmap
	height to draw a line to indicate the speed,
	otherwise return 0 (no movement)
	*/
	private float getMovement() {
		if (ais.getSog() > 0.0d) {
			if (ais.isMovable()) {
				if (this.ais.getSog() <  2.0d) { return 0.0f; }
				if (this.ais.getSog() <  5.0d) { return 1.0f; }
				if (this.ais.getSog() < 10.0d) { return 2.0f; }
				if (this.ais.getSog() < 25.0d) { return 3.0f; }
				return 5.0f;
			}
		}
		return 0.0f;
	}

	private boolean needRotation() {
		return (((ais.getCog() != INVALID_COG) && (this.ais.getCog() != 0)) ||
				((ais.getHeading() != INVALID_HEADING) && (ais.getHeading() != 0))) && ais.isMovable();
	}

	/* return true if the vessel gets too close with the own position in the future
	 * (danger of collusion);
	 * this situation occurs if all of the following conditions hold:
	 *  (1) the calculated TCPA is in the future (>0)
	 *  (2) the calculated CPA is not bigger than the configured warning distance
	 *  (3) the calculated TCPA is not bigger than the configured warning time
	 *  (4) the time when the own course crosses the course of the other vessel
	 *      is not in the past
	 *  (5) the time when the course of the other vessel crosses the own course
	 *      is not in the past  */
	private boolean checkCpaWarning() {
		Location ownPosition = getPlugin().getOwnPosition();
		int cpaWarningTime = getPlugin().getCpaWarningTime();
		float cpaWarningDistance = getPlugin().getCpaWarningDistance();
		if (ais.isMovable() && (ais.getObjectClass() != AIS_AIRPLANE) && (cpaWarningTime > 0) && (ais.getSog() > 0.0d)) {
			Cpa cpa = ais.getCpa();
			if (checkForCpaTimeout() && (ownPosition != null)) {
				Location aisPosition = ais.getCurrentLocation();
				if (aisPosition != null) {
					getCpa(ownPosition, aisPosition, cpa);
					lastCpaUpdate = System.currentTimeMillis();
				}
			}
			if (cpa.isValid()) {
				double tcpa = cpa.getTcpa();
				if (tcpa > 0.0f) {
					return ((cpa.getCpaDist() <= cpaWarningDistance) &&
							((tcpa * 60.0d) <= cpaWarningTime) &&
							(cpa.getCrossingTime1() >= 0.0d) &&
							(cpa.getCrossingTime2() >= 0.0d));
				}
			}
		}
		return false;
	}

	private boolean checkForCpaTimeout() {
		return ((System.currentTimeMillis() - this.lastCpaUpdate) / 1000) > CPA_UPDATE_TIMEOUT_IN_SECONDS;
	}
	
	private void setBitmap() {
		invalidateBitmap();
		boolean vesselAtRest = ais.isVesselAtRest();
		if (ais.isLost(getPlugin().getVesselLostTimeoutInMinutes()) && !vesselAtRest) {
			if (ais.isMovable()) {
				this.bitmap = imagesCache.getBitmap(R.drawable.mm_ais_vessel_cross);
				this.bitmapValid = true;
			}
		} else {
			int bitmapId = selectBitmap(ais.getObjectClass());
			if (bitmapId >= 0) {
				this.bitmap = imagesCache.getBitmap(bitmapId);
				this.bitmapValid = true;
			}
		}
		this.setColor(vesselAtRest);
	}

	private void setColor(boolean vesselAtRest) {
		if (ais.isLost(getPlugin().getVesselLostTimeoutInMinutes()) && !vesselAtRest) {
			if (ais.isMovable()) {
				this.bitmapColor = 0; // default icon
			}
		} else {
			this.bitmapColor = selectColor(ais.getObjectClass());
		}
	}

	private void updateBitmap(@NonNull Paint paint) {
		if (ais.isLost(getPlugin().getVesselLostTimeoutInMinutes())) {
			setBitmap();
		} else {
			if (!this.bitmapValid) {
				setBitmap();
			}
			if (checkCpaWarning()) {
				activateCpaWarning();
			} else {
				deactivateCpaWarning();
			}
		}
		if (this.bitmapColor != 0) {
			paint.setColorFilter(new LightingColorFilter(this.bitmapColor, 0));
		} else {
			paint.setColorFilter(null);
		}
	}

	private void drawCircle(float locationX, float locationY,
							@NonNull Paint paint, @NonNull Canvas canvas) {
		Paint localPaint = new Paint(paint);
		localPaint.setColorFilter(null);
		localPaint.setColor(Color.DKGRAY);
		canvas.drawCircle(locationX, locationY, 22.0f, localPaint);
		localPaint.setColor(this.bitmapColor);
		canvas.drawCircle(locationX, locationY, 18.0f, localPaint);
	}

	public void draw(@NonNull Paint paint, @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		updateBitmap(paint);
		LatLon position = ais.getPosition();
		if (this.bitmap != null && position != null) {
			canvas.save();
			canvas.rotate(tileBox.getRotate(), (float)tileBox.getCenterPixelX(), (float)tileBox.getCenterPixelY());
			float speedFactor = getMovement();
			int locationX = tileBox.getPixXFromLonNoRot(position.getLongitude());
			int locationY = tileBox.getPixYFromLatNoRot(position.getLatitude());
			float fx =  locationX - this.bitmap.getWidth() / 2.0f;
			float fy =  locationY - this.bitmap.getHeight() / 2.0f;
			boolean vesselAtRest = ais.isVesselAtRest();
			if (!vesselAtRest && this.needRotation()) {
				float rotation = ais.getVesselRotation();
				canvas.rotate(rotation, locationX, locationY);
			}
			if (vesselAtRest) {
				drawCircle(locationX, locationY, paint, canvas);
			} else {
				canvas.drawBitmap(this.bitmap, Math.round(fx), Math.round(fy), paint);
			}
			if ((tileBox.getZoom() >= AisTrackerLayer.START_ZOOM_SHOW_DIRECTION) && (speedFactor > 0) &&
					(!ais.isLost(getPlugin().getVesselLostTimeoutInMinutes())) && !vesselAtRest) {
				float lineLength = (float)this.bitmap.getHeight() * speedFactor;
				float lineStartY = locationY - this.bitmap.getHeight() / 4.0f;
				float lineEndY = lineStartY - lineLength;
				canvas.drawLine((float) locationX, lineStartY, (float) locationX, lineEndY, paint);
			}
			canvas.restore();
		}
	}

	public void createAisRenderData(int baseOrder, @NonNull Paint paint,
									@NonNull MapMarkersCollection markersCollection,
									@NonNull VectorLinesCollection vectorLinesCollection,
									@NonNull SingleSkImage restImage) {
		updateBitmap(paint);

		Bitmap lostBitmap = imagesCache.getBitmap(R.drawable.mm_ais_vessel_cross);
		if (bitmap == null || lostBitmap == null) {
			return;
		}

		SingleSkImage activeImage = NativeUtilities.createSkImageFromBitmap(bitmap);
		SingleSkImage lostImage = NativeUtilities.createSkImageFromBitmap(lostBitmap);

		MapMarkerBuilder markerBuilder = new MapMarkerBuilder();
		markerBuilder.setBaseOrder(baseOrder);
		markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), activeImage);
		markerBuilder.setIsHidden(true);
		activeMarker = markerBuilder.buildAndAddToCollection(markersCollection);

		markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), restImage);
		restMarker = markerBuilder.buildAndAddToCollection(markersCollection);

		markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), lostImage);
		lostMarker = markerBuilder.buildAndAddToCollection(markersCollection);

		VectorLineBuilder lineBuilder = new VectorLineBuilder();
		lineBuilder.setLineId(ais.getMmsi());
		// To simplify algorithm draw line from the center of icon and increase order to draw it behind the icon
		lineBuilder.setBaseOrder(baseOrder + 10);
		lineBuilder.setIsHidden(true);
		lineBuilder.setFillColor(NativeUtilities.createFColorARGB(0xFF000000));
		// Create line with non empty vector, otherwise render symbol is not created, TODO: FIX IN ENGINE
		lineBuilder.setPoints(new QVectorPointI(2));
		lineBuilder.setLineWidth(6);
		directionLine = lineBuilder.buildAndAddToCollection(vectorLinesCollection);
	}

	public boolean hasAisRenderData() {
		return activeMarker != null && restMarker != null && lostMarker != null && directionLine != null;
	}

	public void updateAisRenderData(@Nullable OsmandMapTileView mapView, @NonNull Paint paint) {
		// Call updateBitmap to update marker color
		updateBitmap(paint);

		if (!hasAisRenderData()) {
			return;
		}

		int currentZoom = mapView != null ? mapView.getZoom() : 0;
		if (currentZoom < AisTrackerLayer.START_ZOOM) {
			activeMarker.setIsHidden(true);
			restMarker.setIsHidden(true);
			lostMarker.setIsHidden(true);
			directionLine.setIsHidden(true);
			return;
		}

		boolean vesselAtRest = ais.isVesselAtRest();
		float speedFactor = getMovement();
		boolean lostTimeout = ais.isLost(getPlugin().getVesselLostTimeoutInMinutes()) && !vesselAtRest;
		boolean drawDirectionLine = (speedFactor > 0) && (!lostTimeout) && !vesselAtRest;

		activeMarker.setIsHidden(vesselAtRest || lostTimeout);
		restMarker.setIsHidden(!vesselAtRest);
		lostMarker.setIsHidden(!lostTimeout);
		directionLine.setIsHidden(drawDirectionLine);

		float rotation = (ais.getVesselRotation() + 180f) % 360f;
		if (!vesselAtRest && needRotation()) {
			activeMarker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rotation);
			lostMarker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rotation);
		}

		ColorARGB iconColor = bitmapColor == 0 ? NativeUtilities.createColorARGB(0xFFFFFFFF)
				: NativeUtilities.createColorARGB(bitmapColor);

		activeMarker.setOnSurfaceIconModulationColor(iconColor);
		restMarker.setOnSurfaceIconModulationColor(iconColor);

		LatLon location = ais.getPosition();
		if (location != null) {
			PointI markerLocation = new PointI(
					MapUtils.get31TileNumberX(location.getLongitude()),
					MapUtils.get31TileNumberY(location.getLatitude())
			);

			activeMarker.setPosition(markerLocation);
			restMarker.setPosition(markerLocation);
			lostMarker.setPosition(markerLocation);

			int inverseZoom = mapView.getMaxZoom() - mapView.getZoom();
			float lineLength = speedFactor * (float)MapUtils.getPowZoom(inverseZoom) * bitmap.getHeight() * 0.75f;

			double theta = Math.toRadians(rotation);
			float dx = (float) (-Math.sin(theta) * lineLength);
			float dy = (float) (Math.cos(theta) * lineLength);

			PointI directionLineEnd = new PointI(
					(int) (markerLocation.getX() + Math.ceil(dx)),
					(int) (markerLocation.getY() + Math.ceil(dy))
			);

			QVectorPointI points = new QVectorPointI();
			points.add(markerLocation);
			points.add(directionLineEnd);

			directionLine.setPoints(points);
			directionLine.setIsHidden(!drawDirectionLine);
		}
	}

	public void clearAisRenderData(@NonNull MapMarkersCollection markersCollection,
								   @NonNull VectorLinesCollection vectorLinesCollection) {
		markersCollection.removeMarker(activeMarker);
		markersCollection.removeMarker(restMarker);
		markersCollection.removeMarker(lostMarker);
		vectorLinesCollection.removeLine(directionLine);
		activeMarker = null;
		restMarker = null;
		lostMarker = null;
		directionLine = null;
	}
}
