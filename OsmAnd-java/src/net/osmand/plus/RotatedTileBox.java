package net.osmand.plus;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;

public class RotatedTileBox {
	private float leftTileX;
	private float topTileY;
	private float tileWidth;
	private float tileHeight;
	private float rotate;
	private float zoom;
	private float rotateCos;
	private float rotateSin;

	public RotatedTileBox(float leftTileX, float topTileY, float tileWidth, float tileHeight, float rotate, int zoom) {
		set(leftTileX, topTileY, tileWidth, tileHeight, rotate, zoom);
	}
	
	public RotatedTileBox(RotatedTileBox r){
		set(r.leftTileX, r.topTileY, r.tileWidth, r.tileHeight, r.rotate, r.zoom);
	}

	private void init() {
		float rad = (float) Math.toRadians(this.rotate);
		rotateCos = (float) Math.cos(rad);
		rotateSin = (float) Math.sin(rad);
	}
	
	public void set(float leftTileX, float topTileY, float tileWidth, float tileHeight, float rotate, float zoom) {
		this.leftTileX = leftTileX;
		if(rotate < 0){
			rotate += 360;
		} else if(rotate > 360){
			rotate -= 360;
		}
		this.rotate = rotate;
		this.tileHeight = tileHeight;
		this.tileWidth = tileWidth;
		this.topTileY = topTileY;
		this.zoom = zoom;
		init();
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

	public float getTileHeight() {
		return tileHeight;
	}

	public float getTileWidth() {
		return tileWidth;
	}

	public float getLeftTileX() {
		return leftTileX;
	}

	public float getTopTileY() {
		return topTileY;
	}

	public boolean containsTileBox(RotatedTileBox box) {
		QuadPoint temp = new QuadPoint();
		if(box.zoom != zoom){
			throw new UnsupportedOperationException();
		}
		box.calcPointTile(0, 0, temp);
		if(!containsPoint(temp.x, temp.y)){
			return false;
		}
		box.calcPointTile(box.tileWidth, 0, temp);
		if(!containsPoint(temp.x, temp.y)){
			return false;
		}
		box.calcPointTile(0, box.tileHeight, temp);
		if(!containsPoint(temp.x, temp.y)){
			return false;
		}
		box.calcPointTile(box.tileWidth, box.tileHeight, temp);
		if(!containsPoint(temp.x, temp.y)){
			return false;
		}
		return true;
	}
	
	public QuadRect calculateLatLonBox(QuadRect rectF) {
		float tx = calcPointTileX(tileWidth, 0);
		float tx2 = calcPointTileX(tileWidth, tileHeight);
		float tx3 = calcPointTileX(0, tileHeight);
		float minTileX = Math.min(Math.min(leftTileX, tx), Math.min(tx2, tx3)) ;
		float maxTileX = Math.max(Math.max(leftTileX, tx), Math.max(tx2, tx3)) ;
		int max = (int) MapUtils.getPowZoom(zoom);
		if(minTileX < 0){
			minTileX = 0;
		}
		if(maxTileX > max){
			maxTileX = max;
		}
		
		rectF.left = (float) MapUtils.getLongitudeFromTile(zoom, minTileX);
		rectF.right = (float) MapUtils.getLongitudeFromTile(zoom, maxTileX);
		
		float ty = calcPointTileY(tileWidth, 0);
		float ty2 = calcPointTileY(tileWidth, tileHeight);
		float ty3 = calcPointTileY(0, tileHeight);
		
		float minTileY = Math.min(Math.min(topTileY, ty), Math.min(ty2, ty3)) ;
		float maxTileY = Math.max(Math.max(topTileY, ty), Math.max(ty2, ty3)) ;
		if(minTileY < 0){
			minTileY = 0;
		}
		if(maxTileY > max){
			maxTileY = max;
		}
		
		rectF.top = (float) MapUtils.getLatitudeFromTile(zoom, minTileY);
		rectF.bottom = (float) MapUtils.getLatitudeFromTile(zoom, maxTileY);
		
		return rectF;
	}
	
	public boolean containsPoint(float tileX, float tileY) {
		tileX -= leftTileX;
		tileY -= topTileY;
		double tx = rotateCos * tileX - rotateSin * tileY;
		double ty = rotateSin * tileX + rotateCos * tileY;
		return tx >= 0 && tx <= tileWidth && ty >= 0 && ty <= tileHeight;
	}
	
	protected QuadPoint calcPointTile(float dx, float dy, QuadPoint p){
		float tx = rotateCos * dx + rotateSin * dy + leftTileX;
		float ty = - rotateSin * dx + rotateCos * dy + topTileY;
		p.set(tx, ty);
		return p;
	}
	
	protected float calcPointTileX(float dx, float dy){
		return rotateCos * dx + rotateSin * dy + leftTileX;
	}
	
	protected float calcPointTileY(float dx, float dy){
		return - rotateSin * dx + rotateCos * dy + topTileY;
	}

	public float getRightBottomTileX() {
		return calcPointTileX(tileWidth, tileHeight);
	}

	public float getRightBottomTileY() {
		return calcPointTileY(tileWidth, tileHeight);
	}

	

}