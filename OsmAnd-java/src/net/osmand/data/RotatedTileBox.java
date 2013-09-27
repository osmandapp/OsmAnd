package net.osmand.data;
import net.osmand.util.MapUtils;

public class RotatedTileBox {

	/// primary fields
	private double lat;
	private double lon;
	private float rotate;
	private int zoom;
	private float zoomScale;
	private int cx;
	private int cy;
	private int pixWidth;
	private int pixHeight;

	// derived
	// all geometry math is done in tileX, tileY of phisycal given zoom
	// zoomFactor is conversion factor, from dtileX * zoomFactor = dPixelX
	private float zoomFactor;
	private float rotateCos;
	private float rotateSin;
	private float oxTile;
	private float oyTile;
	private QuadRect tileBounds;
	private QuadRect latLonBounds;
	private QuadPoint tileLT;
	private QuadPoint tileRT;
	private QuadPoint tileRB;
	private QuadPoint tileLB;


	public RotatedTileBox(int pixWidth, int pixHeight, float centerX, float centerY,
	                      double lat, double lon, int zoom, float zoomScale, float rotate) {
		init(pixWidth, pixHeight, centerX, centerY, lat, lon, zoom, zoomScale, rotate);
	}

	public RotatedTileBox(RotatedTileBox r){
		// TODO
	}

	private void init(int pixWidth, int pixHeight, float centerX, float centerY, double lat, double lon,
	                  int zoom, float zoomScale, float rotate) {
		this.pixWidth = pixWidth;
		this.pixHeight = pixHeight;
		this.lat = lat;
		this.lon = lon;
		this.zoom = zoom;
		this.zoomScale = zoomScale;
		this.rotate = rotate;
		cx = (int) (pixWidth * centerX);
		cy = (int) (pixHeight * centerY);
		// derived
		calculateDerivedFields();

	}

	private void calculateDerivedFields() {
		zoomFactor = (float) Math.pow(2, zoomScale) * 256;
		float rad = (float) Math.toRadians(this.rotate);
		rotateCos = (float) Math.cos(rad);
		rotateSin = (float) Math.sin(rad);
		oxTile = (float) MapUtils.getTileNumberX(zoom, lon);
		oyTile = (float) MapUtils.getTileNumberY(zoom, lat);
		while(rotate < 0){
			rotate += 360;
		}
		while(rotate > 360){
			rotate -= 360;
		}
		calculateTileRectangle();
	}


	protected float getTileXFromPixel(int x, int y) {
		int dx = x - cx;
		int dy = y - cy;
		float dtilex;
		if(isMapRotateEnabled()){
			dtilex = (rotateCos * (float) dx + rotateSin * (float) dy);
		} else {
			dtilex = (float) dx;
		}
		return dtilex / zoomFactor + oxTile;
	}

	protected float getTileYFromPixel(int x, int y) {
		int dx = x - cx;
		int dy = y - cy;
		float dtiley;
		if(isMapRotateEnabled()){
			dtiley = (-rotateSin * (float) dx + rotateCos * (float) dy);
		} else {
			dtiley = (float) dy;
		}
		return dtiley / zoomFactor + oyTile;
	}


	public void calculateTileRectangle() {
		float x1 = getTileXFromPixel(0, 0);
		float x2 = getTileXFromPixel(pixWidth, 0);
		float x3 = getTileXFromPixel(pixWidth, pixHeight);
		float x4 = getTileXFromPixel(0, pixHeight);
		float y1 = getTileYFromPixel(0, 0);
		float y2 = getTileYFromPixel(pixWidth, 0);
		float y3 = getTileYFromPixel(pixWidth, pixHeight);
		float y4 = getTileYFromPixel(0, pixHeight);
		tileLT = new QuadPoint(x1, y1);
		tileRT = new QuadPoint(x2, y2);
		tileRB = new QuadPoint(x3, y3);
		tileLB = new QuadPoint(x4, y4);
		float l = Math.min(Math.min(x1, x2), Math.min(x3, x4)) ;
		float r = Math.max(Math.max(x1, x2), Math.max(x3, x4)) ;
		float t = Math.min(Math.min(y1, y2), Math.min(y3, y4)) ;
		float b = Math.max(Math.max(y1, y2), Math.max(y3, y4)) ;
		tileBounds = new QuadRect(l, t, r, b);
		float top = (float) MapUtils.getLatitudeFromTile(zoom, tileBounds.top);
		float left = (float) MapUtils.getLongitudeFromTile(zoom, tileBounds.left);
		float bottom = (float) MapUtils.getLatitudeFromTile(zoom, tileBounds.bottom);
		float right = (float) MapUtils.getLongitudeFromTile(zoom, tileBounds.right);
		latLonBounds = new QuadRect(left, top, right, bottom);
	}


	public int getPixWidth() {
		return pixWidth;
	}

	public int getPixHeight() {
		return pixHeight;
	}


	public int getPixXFromLatLon(double latitude, double longitude) {
		float xTile = (float) MapUtils.getTileNumberX(zoom, longitude);
		float yTile = (float) MapUtils.getTileNumberY(zoom, latitude);
		return getPixXFromTile(xTile, yTile);
	}

	protected int getPixXFromTile(float xTile, float yTile) {
		float rotX;
		final float dTileX = xTile - oxTile;
		final float dTileY = yTile - oyTile;
		if(isMapRotateEnabled()){
			rotX = (rotateCos * dTileX - rotateSin * dTileY);
		} else {
			rotX = dTileX;
		}
		float dx = rotX * zoomFactor;
		return (int) (dx + cx);
	}


	public int getPixYFromLatLon(double latitude, double longitude) {
		float xTile = (float) MapUtils.getTileNumberX(zoom, longitude);
		float yTile = (float) MapUtils.getTileNumberY(zoom, latitude);
		return getPixYFromTile(xTile, yTile);
	}

	protected int getPixYFromTile(float xTile, float yTile) {
		final float dTileX = xTile - oxTile;
		final float dTileY = yTile - oyTile;
		float rotY;
		if(isMapRotateEnabled()){
			rotY = (rotateSin * dTileX + rotateCos * dTileY);
		} else {
			rotY = dTileY;
		}
		float dy = rotY * zoomFactor;
		return (int) (dy + cy);
	}


	private boolean isMapRotateEnabled() {
		return rotate != 0;
	}

	public QuadRect getLatLonBounds() {
		return latLonBounds;
	}
	
	public float getRotateCos() {
		return rotateCos;
	}
	
	public float getRotateSin() {
		return rotateSin;
	}

	public float getZoom() {
		return zoom;
	}
	
	public int getIntZoom() {
		return Math.round(zoom);
	}

	public float getRotate() {
		return rotate;
	}

	public boolean containsTileBox(RotatedTileBox box) {
		QuadPoint temp = new QuadPoint();
		if(box.zoom != zoom){
			throw new UnsupportedOperationException();
		}
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
	
}
