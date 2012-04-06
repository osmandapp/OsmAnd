package net.osmand.plus.render;

import net.osmand.access.AccessibleToast;
import net.osmand.osm.MapUtils;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.RotatedTileBox;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class MapVectorLayer extends BaseMapLayer {

	private OsmandMapTileView view;
	private Rect pixRect = new Rect();
	private RotatedTileBox rotatedTileBox = new RotatedTileBox(0, 0, 0, 0, 0, 0);
	private ResourceManager resourceManager;
	private Paint paintImg;
	
	private RectF destImage = new RectF();
	private final MapTileLayer tileLayer;
	private boolean visible = false;
	
	public MapVectorLayer(MapTileLayer tileLayer){
		this.tileLayer = tileLayer;
	}
	

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
		paintImg.setAlpha(getAlpha());
	}
	
	private void updateRotatedTileBox(){
		float ts = view.getTileSize();
		float xL = view.calcDiffTileX(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getXTile();
		float yT = view.calcDiffTileY(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getYTile();
		rotatedTileBox.set(xL, yT, ((float) pixRect.width()) / ts, ((float) pixRect.height()) / ts, view.getRotate(), view.getZoom());
	}
	
	public boolean isVectorDataVisible() {
		return visible &&  view.getZoom() >= view.getSettings().LEVEL_TO_SWITCH_VECTOR_RASTER.get();
	}
	
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		if(!visible){
			resourceManager.getRenderer().clearCache();
		}
	}
	
	@Override
	public int getMaximumShownMapZoom() {
		return 23;
	}
	
	@Override
	public int getMinimumShownMapZoom() {
		return 1;
	}
	

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings drawSettings) {
		if(!visible){
			return;
		}
		if(!isVectorDataVisible() && tileLayer != null){
			tileLayer.drawTileMap(canvas, tilesRect);
			resourceManager.getRenderer().interruptLoadingMap();
		} else {
			if (!view.isZooming()) {
				pixRect.set(0, 0, view.getWidth(), view.getHeight());
				updateRotatedTileBox();
				//TODO passing of nithMode and appMode could be probably something more general? These are
				//renderer properties, so, we should check if renderer properties are changed somehow...
				if (resourceManager.updateRenderedMapNeeded(rotatedTileBox,drawSettings)) {
					// pixRect.set(-view.getWidth(), -view.getHeight() / 2, 2 * view.getWidth(), 3 * view.getHeight() / 2);
					pixRect.set(-view.getWidth() / 3, -view.getHeight() / 4, 4 * view.getWidth() / 3, 5 * view.getHeight() / 4);
					updateRotatedTileBox();
					resourceManager.updateRendererMap(rotatedTileBox);
				}

			}
			
			MapRenderRepositories renderer = resourceManager.getRenderer();
			drawRenderedMap(canvas, renderer.getBitmap(), renderer.getBitmapLocation());
			drawRenderedMap(canvas, renderer.getPrevBitmap(), renderer.getPrevBmpLocation());
		}
	}


	private void drawRenderedMap(Canvas canvas, Bitmap bmp, RotatedTileBox bmpLoc) {
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
			
			canvas.rotate(-rot, view.getCenterPointX(), view.getCenterPointY());
			destImage.set(x1, y1, x1 + bmpLoc.getTileWidth() * mult * view.getTileSize(), y1 + bmpLoc.getTileHeight() * mult
					* view.getTileSize());
			if(!bmp.isRecycled()){
				canvas.drawBitmap(bmp, null, destImage, paintImg);
			}
			canvas.rotate(rot, view.getCenterPointX(), view.getCenterPointY());
		}
	}

	
	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);
		if (paintImg != null) {
			paintImg.setAlpha(alpha);
		}
	}	
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		return false;
	}
}
