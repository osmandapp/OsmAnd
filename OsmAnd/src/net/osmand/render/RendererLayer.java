package net.osmand.render;

import net.osmand.ResourceManager;
import net.osmand.osm.MapUtils;
import net.osmand.views.OsmandMapLayer;
import net.osmand.views.OsmandMapTileView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.FloatMath;

public class RendererLayer implements OsmandMapLayer {

	private OsmandMapTileView view;
	private final static int startZoom = 5;
	private Rect pixRect = new Rect();
	private RectF tileRect = new RectF();
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

	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom && visible) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			if (view.getFloatZoom() == view.getZoom()
					&& resourceManager.updateRenderedMapNeeded(tileRect, view.getZoom(), view.getRotate())) {
				pixRect.set(-view.getWidth(), -view.getHeight()/2, 2*view.getWidth(), 3*view.getHeight()/2);
				view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
						view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
				float xL = view.calcDiffTileX(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getXTile();
				float xR = view.calcDiffTileX(pixRect.right - view.getCenterPointX(), pixRect.bottom - view.getCenterPointY()) + view.getXTile();
				float yT = view.calcDiffTileY(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY())+ view.getYTile();
				float yB = view.calcDiffTileY(pixRect.right - view.getCenterPointX(), pixRect.bottom - view.getCenterPointY()) + view.getYTile();
				RectF verticesRect = new RectF(xL, yT, xR, yB);
				resourceManager.updateRendererMap(verticesRect, tileRect, view.getZoom(), view.getRotate());
			}
			
			MapRenderRepositories renderer = resourceManager.getRenderer();
			Bitmap bmp = renderer.getBitmap();
			if (renderer != null &&  bmp != null) {
				RectF newLoc = renderer.getCachedWaysLoc();
				float rot = renderer.getCachedRotate();
				float leftX1 = (float) MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.left);
				float rightX1 = (float) MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.right);
				float topY1 = (float) MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.top);
				float bottomY1 = (float) MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.bottom);

				float x1 = calcDiffPixelX(rot, leftX1 - view.getXTile(), topY1 - view.getYTile()) + view.getCenterPointX();
				float y1 = calcDiffPixelY(rot, leftX1 - view.getXTile(), topY1 - view.getYTile()) + view.getCenterPointY();
				float x2 = calcDiffPixelX(rot, rightX1 - view.getXTile(), bottomY1 - view.getYTile()) + view.getCenterPointX();
				float y2 = calcDiffPixelY(rot, rightX1 - view.getXTile(), bottomY1 - view.getYTile()) + view.getCenterPointY();
				canvas.rotate(-rot, view.getCenterPointX(), view.getCenterPointY());
				destImage.set(x1, y1, x2, y2);
				if(!bmp.isRecycled()){
					canvas.drawBitmap(bmp, null, destImage, paintImg);
				}
			}
		}
	}

	
	public float calcDiffPixelX(float rotate, float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dTileX - FloatMath.sin(rad) * dTileY) * view.getTileSize();
	}
	
	public float calcDiffPixelY(float rotate, float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.sin(rad) * dTileX + FloatMath.cos(rad) * dTileY) * view.getTileSize() ;
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
