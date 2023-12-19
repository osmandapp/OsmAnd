package net.osmand.data;

import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class RotatedTileBox {

	/// primary fields
	private double lat;
	private double lon;
	private float height;
	private float rotate;
	private float density;
	private int zoom;
	private double mapDensity = 1;
	private double zoomAnimation;
	private double zoomFloatPart;
	private int cx;
	private int cy;
	private int pixWidth;
	private int pixHeight;
	private float ratiocx;
	private float ratiocy;

	// derived
	// all geometry math is done in tileX, tileY of phisycal given zoom
	// zoomFactor is conversion factor, from dtileX * zoomFactor = dPixelX
	private double zoomFactor;
	private double rotateCos;
	private double rotateSin;
	private double oxTile;
	private double oyTile;
	private QuadRect tileBounds;
	private QuadRect latLonBounds;
	private List<LatLon> rotatedLatLonBounds;
	private QuadPointDouble tileLT;
	private QuadPointDouble tileRT;
	private QuadPointDouble tileRB;
	private QuadPointDouble tileLB;


	private RotatedTileBox(){
	}

	public RotatedTileBox(RotatedTileBox r){
		this.pixWidth = r.pixWidth;
		this.pixHeight = r.pixHeight;
		this.lat = r.lat;
		this.lon = r.lon;
		this.zoom = r.zoom;
		this.mapDensity = r.mapDensity;
		this.zoomFloatPart = r.zoomFloatPart;
		this.zoomAnimation = r.zoomAnimation;
		this.rotate = r.rotate;
		this.density = r.density;
		this.cx = r.cx;
		this.cy = r.cy;
		this.ratiocx = r.ratiocx;
		this.ratiocy = r.ratiocy;
		copyDerivedFields(r);
	}

	private void copyDerivedFields(RotatedTileBox r) {
		zoomFactor = r.zoomFactor;
		rotateCos = r.rotateCos;
		rotateSin = r.rotateSin;
		oxTile = r.oxTile;
		oyTile = r.oyTile;
		if (r.tileBounds != null && r.latLonBounds != null && !Algorithms.isEmpty(r.rotatedLatLonBounds)) {
			tileBounds = new QuadRect(r.tileBounds);
			latLonBounds = new QuadRect(r.latLonBounds);
			tileLT = new QuadPointDouble(r.tileLT);
			tileRT = new QuadPointDouble(r.tileRT);
			tileRB = new QuadPointDouble(r.tileRB);
			tileLB = new QuadPointDouble(r.tileLB);

			rotatedLatLonBounds = new ArrayList<>();
			for (LatLon latLon : r.rotatedLatLonBounds) {
				rotatedLatLonBounds.add(new LatLon(latLon.getLatitude(), latLon.getLongitude()));
			}
		}
	}

	public void calculateDerivedFields() {
		zoomFactor = Math.pow(2, zoomAnimation + zoomFloatPart) * 256 * mapDensity;
		double rad = Math.toRadians(this.rotate);
		rotateCos = Math.cos(rad);
		rotateSin = Math.sin(rad);
		oxTile = MapUtils.getTileNumberX(zoom, lon);
		oyTile = MapUtils.getTileNumberY(zoom, lat);
		while(rotate < 0){
			rotate += 360;
		}
		while(rotate > 360){
			rotate -= 360;
		}
		tileBounds = null;
		// lazy
		// calculateTileRectangle();
	}

	public double getLatFromPixel(float x, float y) {
		return MapUtils.getLatitudeFromTile(zoom, getTileYFromPixel(x, y));
	}

	public double getLonFromPixel(float x, float y) {
		return MapUtils.getLongitudeFromTile(zoom, getTileXFromPixel(x, y));
	}

	public LatLon getLatLonFromPixel(float x, float y) {
		return new LatLon(getLatFromPixel(x, y), getLonFromPixel(x, y));
	}

	public LatLon getCenterLatLon() {
		return new LatLon(lat, lon);
	}

	public QuadPoint getCenterPixelPoint() {
		return new QuadPoint(cx, cy);
	}

	public int getCenterPixelX(){
		return cx;
	}

	public int getCenterPixelY(){
		return cy;
	}

	public void setDensity(float density) {
		this.density = density;
	}

	public double getCenterTileX(){
		return oxTile;
	}
	
	public int getCenter31X(){
		return MapUtils.get31TileNumberX(lon);
	}
	
	public int getCenter31Y(){
		return MapUtils.get31TileNumberY(lat);
	}

	public double getCenterTileY(){
		return oyTile;
	}

	protected double getTileXFromPixel(float x, float y) {
		float dx = x - cx;
		float dy = y - cy;
		double dtilex;
		if(isMapRotateEnabled()){
			dtilex = (rotateCos * (float) dx + rotateSin * (float) dy);
		} else {
			dtilex = (float) dx;
		}
		return dtilex / zoomFactor + oxTile;
	}

	protected double getTileYFromPixel(float x, float y) {
		float dx = x - cx;
		float dy = y - cy;
		double dtiley;
		if(isMapRotateEnabled()){
			dtiley = (-rotateSin * (float) dx + rotateCos * (float) dy);
		} else {
			dtiley = (float) dy;
		}
		return dtiley / zoomFactor + oyTile;
	}


	public QuadRect getTileBounds() {
		checkTileRectangleCalculated();
		return tileBounds;
	}

	public void calculateTileRectangle() {
		double x1 = getTileXFromPixel(0, 0);
		double x2 = getTileXFromPixel(pixWidth, 0);
		double x3 = getTileXFromPixel(pixWidth, pixHeight);
		double x4 = getTileXFromPixel(0, pixHeight);
		double y1 = getTileYFromPixel(0, 0);
		double y2 = getTileYFromPixel(pixWidth, 0);
		double y3 = getTileYFromPixel(pixWidth, pixHeight);
		double y4 = getTileYFromPixel(0, pixHeight);
		tileLT = new QuadPointDouble(x1, y1);
		tileRT = new QuadPointDouble(x2, y2);
		tileRB = new QuadPointDouble(x3, y3);
		tileLB = new QuadPointDouble(x4, y4);
		double l = Math.min(Math.min(x1, x2), Math.min(x3, x4));
		double r = Math.max(Math.max(x1, x2), Math.max(x3, x4));
		double t = Math.min(Math.min(y1, y2), Math.min(y3, y4));
		double b = Math.max(Math.max(y1, y2), Math.max(y3, y4));
		QuadRect bounds = new QuadRect((float) l, (float) t, (float) r, (float) b);
		float top = (float) MapUtils.getLatitudeFromTile(zoom, alignTile(bounds.top));
		float left = (float) MapUtils.getLongitudeFromTile(zoom, alignTile(bounds.left));
		float bottom = (float) MapUtils.getLatitudeFromTile(zoom, alignTile(bounds.bottom));
		float right = (float) MapUtils.getLongitudeFromTile(zoom, alignTile(bounds.right));
		tileBounds = bounds;
		latLonBounds = new QuadRect(left, top, right, bottom);

		rotatedLatLonBounds = new ArrayList<>();
		rotatedLatLonBounds.add(new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y1)), MapUtils.getLongitudeFromTile(zoom, alignTile(x1))));
		rotatedLatLonBounds.add(new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y2)), MapUtils.getLongitudeFromTile(zoom, alignTile(x2))));
		rotatedLatLonBounds.add(new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y3)), MapUtils.getLongitudeFromTile(zoom, alignTile(x3))));
		rotatedLatLonBounds.add(new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y4)), MapUtils.getLongitudeFromTile(zoom, alignTile(x4))));
	}
	
	private double alignTile(double tile) {
		if (tile < 0) {
			return 0;
		}
		if (tile >= MapUtils.getPowZoom(zoom)) {
			return MapUtils.getPowZoom(zoom) - .000001;
		}
		return tile;
	}

	public double getPixDensity() {
		final double dist = getDistance(0, getPixHeight() / 2, getPixWidth(), getPixHeight() / 2);
		return getPixWidth() / dist;
	}

	public int getPixWidth() {
		return pixWidth;
	}

	public int getPixHeight() {
		return pixHeight;
	}

	public float getPixXFrom31(int x31, int y31) {
		double zm = MapUtils.getPowZoom(31 - zoom);
		double xTile = ((double) x31) / zm;
		double yTile = ((double) y31) / zm;
		return getPixXFromTile(xTile, yTile);
	}

	public float getPixYFrom31(int x31, int y31) {
		double zm = MapUtils.getPowZoom(31 - zoom);
		double xTile = ((double) x31) / zm;
		double yTile = ((double) y31) / zm;
		return getPixYFromTile(xTile, yTile);
	}

	public float getPixXFromLatLon(double latitude, double longitude) {
		double xTile = MapUtils.getTileNumberX(zoom, longitude);
		double yTile = MapUtils.getTileNumberY(zoom, latitude);
		return getPixXFromTile(xTile, yTile);
	}
	
	public float getPixXFromTile(double tileX, double tileY, float zoom) {
		double pw = MapUtils.getPowZoom(zoom - this.zoom);
		double xTile = tileX / pw;
		double yTile = tileY / pw;
		return getPixXFromTile(xTile, yTile);
	}

	protected float getPixXFromTile(double xTile, double yTile) {
		double rotX;
		final double dTileX = xTile - oxTile;
		final double dTileY = yTile - oyTile;
		if(isMapRotateEnabled()){
			rotX = (rotateCos * dTileX - rotateSin * dTileY);
		} else {
			rotX = dTileX;
		}
		double dx = rotX * zoomFactor;
		return (float) (dx + cx);
	}


	public float getPixYFromLatLon(double latitude, double longitude) {
		double  xTile = MapUtils.getTileNumberX(zoom, longitude);
		double  yTile = MapUtils.getTileNumberY(zoom, latitude);
		return getPixYFromTile(xTile, yTile);
	}
	
	public float getPixYFromTile(double tileX, double tileY, float zoom) {
		double pw = MapUtils.getPowZoom(zoom - this.zoom);
		double  xTile = (tileX / pw);
		double  yTile = (tileY / pw);
		return getPixYFromTile(xTile, yTile);
	}

	protected float getPixYFromTile(double  xTile, double  yTile) {
		final double dTileX = xTile - oxTile;
		final double dTileY = yTile - oyTile;
		double rotY;
		if(isMapRotateEnabled()){
			rotY = (rotateSin * dTileX + rotateCos * dTileY);
		} else {
			rotY = dTileY;
		}
		double dy = rotY * zoomFactor;
		return (float) (dy + cy);
	}

	public int getPixXFromLonNoRot(double longitude) {
		double dTilex = MapUtils.getTileNumberX(zoom, longitude) - oxTile;
		return (int) (dTilex * zoomFactor + cx);
	}

	public int getPixXFromTileXNoRot(double tileX) {
		double dTilex = tileX - oxTile;
		return (int) (dTilex * zoomFactor + cx);
	}

	public int getPixYFromLatNoRot(double latitude) {
		double dTileY = MapUtils.getTileNumberY(zoom, latitude) - oyTile;
		return (int) (dTileY * zoomFactor + cy);
	}

	public int getPixYFromTileYNoRot(double tileY) {
		double dTileY  = tileY - oyTile;
		return (int) ((dTileY * zoomFactor) + cy);
	}


	private boolean isMapRotateEnabled() {
		return rotate != 0;
	}

	public QuadRect getLatLonBounds() {
		checkTileRectangleCalculated();
		return latLonBounds;
	}
	
	public double getRotateCos() {
		return rotateCos;
	}
	
	public double getRotateSin() {
		return rotateSin;
	}

	public double getFullZoom() {
		return getZoom() + getZoomFloatPart() + getZoomAnimation();
	}

	public int getZoom() {
		return zoom;
	}

	public int getDefaultRadiusPoi() {
		int radius;
		double zoom = getZoom();
		if (zoom <= 15) {
			radius = 10;
		} else if (zoom <= 16) {
			radius = 14;
		} else if (zoom <= 17) {
			radius = 16;
		} else {
			radius = 18;
		}
		return (int) (radius * getDensity());
	}

	// Change lat/lon center
	public void setLatLonCenter(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
		calculateDerivedFields();
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setRotate(float rotate) {
		this.rotate = rotate;
		calculateDerivedFields();
	}

	public void increasePixelDimensions(int dwidth, int dheight) {
		this.pixWidth += 2 * dwidth;
		this.pixHeight += 2 * dheight;
		this.cx += dwidth;
		this.cy += dheight;
		calculateDerivedFields();
	}

	public void setPixelDimensions(int width, int height) {
		setPixelDimensions(width, height, 0.5f, 0.5f);
	}

	public void setPixelDimensions(int width, int height, float ratiocx, float ratiocy) {
		this.pixHeight = height;
		this.pixWidth = width;
		this.cx = (int) (pixWidth * ratiocx);
		this.cy = (int) (pixHeight * ratiocy);
		this.ratiocx = ratiocx;
		this.ratiocy = ratiocy;
		calculateDerivedFields();
	}

	public boolean isZoomAnimated() {
		return zoomAnimation != 0;
	}

	public double getZoomAnimation() {
		return zoomAnimation;
	}
	
	public double getZoomFloatPart() {
		return zoomFloatPart;
	}

	public void setZoomAndAnimation(int zoom, double zoomAnimation, double zoomFloatPart) {
		this.zoomAnimation = zoomAnimation;
		this.zoomFloatPart = zoomFloatPart;
		this.zoom = zoom;
		calculateDerivedFields();
	}
	
	public void setZoomAndAnimation(int zoom, double zoomAnimation) {
		this.zoomAnimation = zoomAnimation;
		this.zoom = zoom;
		calculateDerivedFields();
	}

	public void setCenterLocation(float ratiocx, float ratiocy) {
		this.cx = (int) (pixWidth * ratiocx);
		this.cy = (int) (pixHeight * ratiocy);
		this.ratiocx = ratiocx;
		this.ratiocy = ratiocy;
		calculateDerivedFields();
	}

	public boolean isCenterShifted() {
		return ratiocx != 0.5f || ratiocy != 0.5f;
	}

	public LatLon getLeftTopLatLon() {
		checkTileRectangleCalculated();
		return new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(tileLT.y)),
				MapUtils.getLongitudeFromTile(zoom, alignTile(tileLT.x)));

	}
	
	public QuadPointDouble getLeftTopTile(double zoom) {
		checkTileRectangleCalculated();
		return new QuadPointDouble((tileLT.x * MapUtils.getPowZoom(zoom - this.zoom)),
				(tileLT.y *  MapUtils.getPowZoom(zoom - this.zoom)));
	}

	
	public QuadPointDouble getRightBottomTile(float zoom) {
		checkTileRectangleCalculated();
		return new QuadPointDouble((tileRB.x * MapUtils.getPowZoom(zoom - this.zoom)),
				(tileRB.y * MapUtils.getPowZoom(zoom - this.zoom)));
	}
	

	private void checkTileRectangleCalculated() {
		if(tileBounds == null){
			calculateTileRectangle();
		}
	}

	public LatLon getRightBottomLatLon() {
		checkTileRectangleCalculated();
		return new LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(tileRB.y)),
				MapUtils.getLongitudeFromTile(zoom, alignTile(tileRB.x)));
	}

	public void setMapDensity(double mapDensity) {
		this.mapDensity = mapDensity;
		calculateDerivedFields();
	}
	
	public double getMapDensity() {
		return mapDensity;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
		calculateDerivedFields();
	}

	public float getHeight() {
		return height;
	}

	public float getRotate() {
		return rotate;
	}

	public float getDensity() {
		return density;
	}

	public RotatedTileBox copy() {
		return new RotatedTileBox(this);
	}



	public boolean containsTileBox(RotatedTileBox box) {
		checkTileRectangleCalculated();
		// thread safe
		box = box.copy();
		box.checkTileRectangleCalculated();
		if(!containsTilePoint(box.tileLB)){
			return false;
		}
		if(!containsTilePoint(box.tileLT)){
			return false;
		}
		if(!containsTilePoint(box.tileRB)){
			return false;
		}
		if(!containsTilePoint(box.tileRT)){
			return false;
		}
		return true;
	}

	public boolean containsTilePoint(QuadPoint qp) {
		double tx = getPixXFromTile(qp.x, qp.y);
		double ty = getPixYFromTile(qp.x, qp.y);
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight;
	}
	
	public boolean containsTilePoint(QuadPointDouble qp) {
		double tx = getPixXFromTile(qp.x, qp.y);
		double ty = getPixYFromTile(qp.x, qp.y);
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight;
	}

	public boolean containsRectInRotatedRect(double left, double top, double right, double bottom) {
		List<LatLon> rect = new ArrayList<>();
		rect.add(new LatLon(top, left));
		rect.add(new LatLon(top, right));
		rect.add(new LatLon(bottom, right));
		rect.add(new LatLon(bottom, left));
		rect.add(rect.get(0));

		checkTileRectangleCalculated();
		List<LatLon> rotatedLatLonRect = new ArrayList<>(this.rotatedLatLonBounds);
		rotatedLatLonRect.add(rotatedLatLonRect.get(0));

		return Algorithms.isFirstPolygonInsideSecond(rect, rotatedLatLonRect);
	}

	public boolean containsLatLon(double lat, double lon) {
		double tx = getPixXFromLatLon(lat, lon);
		double ty = getPixYFromLatLon(lat, lon);
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight;
	}
	
	public boolean containsLatLon(LatLon latLon) {
		double tx = getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		double ty = getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight;
	}

	public boolean containsPoint(float tx, float ty, float outMargin) {
		return tx >= -outMargin && tx <= pixWidth + outMargin && ty >= -outMargin && ty <= pixHeight + outMargin;
	}

	public double getDistance(int pixX, int pixY, int pixX2, int pixY2) {
		final double lat1 = getLatFromPixel(pixX, pixY);
		final double lon1 = getLonFromPixel(pixX, pixY);
		final double lat2 = getLatFromPixel(pixX2, pixY2);
		final double lon2 = getLonFromPixel(pixX2, pixY2);
		return MapUtils.getDistance(lat1,lon1, lat2, lon2);
	}

	public boolean isLatLonNearPixel(LatLon latLon, float centerPixX, float centerPixY, float radius) {
		return isLatLonNearPixel(latLon.getLatitude(), latLon.getLongitude(), centerPixX, centerPixY, radius);
	}

	public boolean isLatLonNearPixel(double lat, double lon, float centerPixX, float centerPixY, float radius) {
		QuadRect pixelArea = new QuadRect(
				centerPixX - radius,
				centerPixY - radius,
				centerPixX + radius,
				centerPixY + radius
		);
		return isLatLonInsidePixelArea(lat, lon, pixelArea);
	}

	public boolean isLatLonInsidePixelArea(LatLon latLon, QuadRect pixelArea) {
		return isLatLonInsidePixelArea(latLon.getLatitude(), latLon.getLongitude(), pixelArea);
	}

	public boolean isLatLonInsidePixelArea(double lat, double lon, QuadRect pixelArea) {
		float pixX = getPixXFromLatLon(lat, lon);
		float pixY = getPixYFromLatLon(lat, lon);
		return pixelArea.contains(pixX, pixY, pixX, pixY);
	}

	public static class RotatedTileBoxBuilder {

		private RotatedTileBox tb;
		private boolean pixelDimensionsSet = false;
		private boolean locationSet = false;
		private boolean zoomSet = false;

		public RotatedTileBoxBuilder() {
			tb = new RotatedTileBox();
			tb.density = 1;
			tb.rotate = 0;
		}

		public RotatedTileBoxBuilder density(float d) {
			tb.density = d;
			return this;
		}

		public RotatedTileBoxBuilder setMapDensity(double mapDensity) {
			tb.mapDensity = mapDensity;
			return this;
		}

		public RotatedTileBoxBuilder setZoom(int zoom) {
			tb.zoom = zoom;
			zoomSet = true;
			return this;
		}

		public RotatedTileBoxBuilder setZoomFloatPart(double zoomFloatPart) {
			tb.zoomFloatPart = zoomFloatPart;
			return this;
		}
		
		public RotatedTileBoxBuilder setLocation(double lat, double lon) {
			tb.lat = lat;
			tb.lon = lon;
			locationSet = true;
			return this;
		}

		public RotatedTileBoxBuilder setRotate(float degrees) {
			tb.rotate = degrees;
			return this;
		}

		public RotatedTileBoxBuilder setPixelDimensions(int pixWidth, int pixHeight, float centerX, float centerY) {
			tb.pixWidth = pixWidth;
			tb.pixHeight = pixHeight;
			tb.cx = (int) (pixWidth * centerX);
			tb.cy = (int) (pixHeight * centerY);
			tb.ratiocx = centerX;
			tb.ratiocy = centerY;
			pixelDimensionsSet = true;
			return this;
		}

		public RotatedTileBoxBuilder setPixelDimensions(int pixWidth, int pixHeight) {
			return setPixelDimensions(pixWidth, pixHeight, 0.5f, 0.5f);
		}

		public RotatedTileBox build() {
			if(!pixelDimensionsSet) {
				throw new IllegalArgumentException("Please specify pixel dimensions");
			}
			if(!zoomSet) {
				throw new IllegalArgumentException("Please specify zoom");
			}
			if(!locationSet) {
				throw new IllegalArgumentException("Please specify location");
			}

			final RotatedTileBox local = tb;
			local.calculateDerivedFields();
			tb = null;
			return local;
		}
	}

	public double getLongitude() {
		return lon;
	}

	public double getLatitude() {
		return lat;
	}

	@Override
	public String toString() {
		return "RotatedTileBox [lat=" + lat + ", lon=" + lon + ", rotate="
				+ rotate + ", density=" + density + ", zoom=" + zoom
				+ ", mapDensity=" + mapDensity + ", zoomAnimation="
				+ zoomAnimation + ", zoomFloatPart=" + zoomFloatPart + ", cx="
				+ cx + ", cy=" + cy + ", pixWidth=" + pixWidth + ", pixHeight="
				+ pixHeight + "]";
	}

	
	
}
