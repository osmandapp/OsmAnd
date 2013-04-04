package net.osmand.plus.render;

import net.osmand.util.MapUtils;
import net.osmand.plus.RotatedTileBox;
import net.osmand.plus.resources.ResourceManager;
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
		float mult = (float) Math.pow(2, view.getFloatZoom() - view.getZoom());
		float xL = (view.calcDiffTileX(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getXTile());
		float yT = (view.calcDiffTileY(pixRect.left - view.getCenterPointX(), pixRect.top - view.getCenterPointY()) + view.getYTile()) ;
		float ts = view.getSourceTileSize(); 
		rotatedTileBox.set(xL * mult, yT * mult, ((float) pixRect.width()) / ts , ((float) pixRect.height()) / ts, view.getRotate(), view.getFloatZoom());
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
		if (!visible) {
			return;
		}
		if (!isVectorDataVisible() && tileLayer != null) {
			tileLayer.drawTileMap(canvas, tilesRect);
			resourceManager.getRenderer().interruptLoadingMap();
		} else {
			if (!view.isZooming()) {
				pixRect.set(0, 0, view.getWidth(), view.getHeight());
				updateRotatedTileBox();
				if (resourceManager.updateRenderedMapNeeded(rotatedTileBox, drawSettings)) {
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


	private boolean drawRenderedMap(Canvas canvas, Bitmap bmp, RotatedTileBox bmpLoc) {
		boolean shown = false;
		if (bmp != null && bmpLoc != null) {
			float rot = bmpLoc.getRotate();
			float mult = (float) MapUtils.getPowZoom(view.getZoom() - bmpLoc.getZoom());
			float fmult = (float) MapUtils.getPowZoom(view.getFloatZoom() - bmpLoc.getZoom());
			
			float tx = view.getXTile() / mult;
			float ty = view.getYTile() / mult;
			float dleftX1 = bmpLoc.getLeftTileX() - tx;
			float dtopY1 = bmpLoc.getTopTileY() - ty;
			
			float ts = view.getSourceTileSize() * fmult;
			
			float cos = bmpLoc.getRotateCos();
			float sin = bmpLoc.getRotateSin();
			float x1 = MapUtils.calcDiffPixelX(sin, cos, dleftX1, dtopY1, ts) + view.getCenterPointX();
			float y1 = MapUtils.calcDiffPixelY(sin, cos, dleftX1, dtopY1, ts) + view.getCenterPointY();
			
			canvas.rotate(-rot, view.getCenterPointX(), view.getCenterPointY());
			destImage.set(x1, y1, x1 + bmpLoc.getTileWidth() * ts, y1 + 
					bmpLoc.getTileHeight() * ts);
			if(!bmp.isRecycled()){
				canvas.drawBitmap(bmp, null, destImage, paintImg);
				shown = true;
			}
			canvas.rotate(rot, view.getCenterPointX(), view.getCenterPointY());
		}
		return shown;
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
