package net.osmand.render;

import net.osmand.ResourceManager;
import net.osmand.RotatedTileBox;
import net.osmand.osm.MapUtils;
import net.osmand.views.OsmandMapLayer;
import net.osmand.views.OsmandMapTileView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class RendererLayer implements OsmandMapLayer {

	private OsmandMapTileView view;
	private final static int startZoom = 5;
	private Rect pixRect = new Rect();
	private RotatedTileBox rotatedTileBox = new RotatedTileBox(0, 0, 0, 0, 0, 0);
	private ResourceManager resourceManager;
	private Paint paintImg;
	
	private RectF destImage = new RectF();
	private boolean visible = false;
	

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		resourceManager = view.getApplication().getResourceManager();
		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
	}
	
	private void updateRotatedTileBox(){
		float ts = view.getTileSize();
		float xL = view.calcDiffTileX(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getXTile();
		float yT = view.calcDiffTileY(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getYTile();
		rotatedTileBox.set(xL, yT, ((float) pixRect.width()) / ts, ((float) pixRect.height()) / ts, view.getRotate(), view.getZoom());
	}
	
	

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, boolean nightMode) {
		if (view.getZoom() >= startZoom && visible) {
			if (!view.isZooming()){
				pixRect.set(0, 0, view.getWidth(), view.getHeight());
				updateRotatedTileBox();
				if(resourceManager.updateRenderedMapNeeded(rotatedTileBox)){
					// pixRect.set(-view.getWidth(), -view.getHeight() / 2, 2 * view.getWidth(), 3 * view.getHeight() / 2);
					pixRect.set(-view.getWidth()/3, -view.getHeight() / 4, 4 * view.getWidth() /3, 5 * view.getHeight() / 4);
					updateRotatedTileBox();
					resourceManager.updateRendererMap(rotatedTileBox);
				}
			}
			
			MapRenderRepositories renderer = resourceManager.getRenderer();
			Bitmap bmp = renderer.getBitmap();
			RotatedTileBox bmpLoc = renderer.getBitmapLocation();
			if (bmp != null && bmpLoc != null) {
				float rot = bmpLoc.getRotate();
				float mult = (float) MapUtils.getPowZoom(view.getZoom() - bmpLoc.getZoom());
				
				float tx = view.getXTile();
				float ty = view.getYTile();
				float dleftX1 = (bmpLoc.getLeftTileX() * mult - tx) ;
				float dtopY1 =  (bmpLoc.getTopTileY() * mult - ty);
				
				
				float cos = bmpLoc.getRotateCos();
				float sin = bmpLoc.getRotateSin();
				float x1 = MapUtils.calcDiffPixelX(sin, cos, dleftX1, dtopY1, view.getTileSize()) + view.getCenterPointX();
				float y1 = MapUtils.calcDiffPixelY(sin, cos, dleftX1, dtopY1, view.getTileSize()) + view.getCenterPointY();
				
				/*float drightX1 = (bmpLoc.getRightBottomTileX() * mult - tx) ;
				float dbottomY1 = (bmpLoc.getRightBottomTileY() * mult - ty);
				float x2 = MapUtils.calcDiffPixelX(sin, cos, drightX1, dbottomY1, view.getTileSize()) + view.getCenterPointX();
				float y2 = MapUtils.calcDiffPixelY(sin, cos, drightX1, dbottomY1, view.getTileSize()) + view.getCenterPointY();
				destImage.set(x1, y1, x2, y2);*/
				
				canvas.rotate(-rot, view.getCenterPointX(), view.getCenterPointY());
				destImage.set(x1, y1, x1 + bmpLoc.getTileWidth() * mult * view.getTileSize(), y1 + bmpLoc.getTileHeight() * mult * view.getTileSize());
				if(!bmp.isRecycled()){
					canvas.drawBitmap(bmp, null, destImage, paintImg);
				}
			}
		}
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		view.refreshMap();
	}
	
	public boolean isVisible() {
		return visible;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}
}
