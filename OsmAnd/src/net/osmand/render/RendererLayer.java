package net.osmand.render;

import net.osmand.ResourceManager;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.views.OsmandMapLayer;
import net.osmand.views.OsmandMapTileView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;

public class RendererLayer implements OsmandMapLayer {

	private OsmandMapTileView view;
	private final static int startZoom = 15;
	private Rect pixRect;
	private RectF tileRect;
	private ResourceManager resourceManager;
	private Paint paint;
	private Paint paintImg;
	
	private RectF cacheLoc;
	private Rect srcImage = new Rect();
	private Bitmap bmp;
	private float bitmapZoom;
	

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
		pixRect = new Rect();
		tileRect = new RectF();
		resourceManager = ResourceManager.getResourceManager();
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(2);
		paint.setColor(Color.RED);
		
		paintImg = new Paint();
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getFloatZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getFloatZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getFloatZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getFloatZoom(), tileRect.right);
			resourceManager.updateRendererIfNeeded(topLatitude, leftLongitude, bottomLatitude, rightLongitude, view.getZoom());
			RenderMapsRepositories renderer = resourceManager.getRenderer();
			if (renderer != null) {
				// TODO check zoom, etc
				// TODO rotate !!!
				RectF newLoc = renderer.getCachedWaysLoc();
				double leftX = MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.left);
				double rightX = MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.right);
				double topY = MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.top);
				double bottomY = MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.bottom);
				if(newLoc != cacheLoc){
					// TODO recalculate bitmap
					if(bmp != null){
						bmp.recycle();
					}
					bmp = null;
					if (!renderer.getCache().isEmpty() && newLoc.width() != 0f && newLoc.height() != 0f) {
						bitmapZoom = view.getFloatZoom();
						bmp = Bitmap.createBitmap((int) ((rightX - leftX) * 256), (int) ((bottomY - topY) * 256), Config.ALPHA_8);
						
						Canvas cv = new Canvas(bmp);
						for (Way w : renderer.getCache()) {
							draw(w, cv, leftX, topY, view.getFloatZoom());
						}
					}
					cacheLoc = renderer.getCachedWaysLoc();
				}
				
				
				if(bmp != null){
					double leftX1 = MapUtils.getTileNumberX(bitmapZoom, newLoc.left);
					double rightX1 = MapUtils.getTileNumberX(bitmapZoom, newLoc.right);
					double topY1 = MapUtils.getTileNumberY(bitmapZoom, newLoc.top);
					double bottomY1 = MapUtils.getTileNumberY(bitmapZoom, newLoc.bottom);
					double bottomY2 = MapUtils.getTileNumberY(bitmapZoom, bottomLatitude);
					double leftX2 = MapUtils.getTileNumberX(bitmapZoom, leftLongitude);
					double rightX2 = MapUtils.getTileNumberX(bitmapZoom, rightLongitude);
					double topY2 = MapUtils.getTileNumberY(bitmapZoom, topLatitude);
					
					
					srcImage.set((int)((leftX2 - leftX1) * 256), (int) ((topY2 - topY1) * 256), 
							bmp.getWidth() - (int)((rightX1 - rightX2) * 256), 
							bmp.getHeight() - (int)((bottomY1 - bottomY2) * 256));
					if(srcImage.right < 0 || srcImage.bottom < 0 || srcImage.left > bmp.getWidth() || srcImage.top > bmp.getHeight()){
						// nothing to draw
					} else {
						if(srcImage.left < 0){
							pixRect.left -= srcImage.left;
							srcImage.left = 0;
						}
						if(srcImage.top < 0){
							pixRect.top -= srcImage.top;
							srcImage.top = 0;
						}
						if(srcImage.right > bmp.getWidth()){
							pixRect.right -= (srcImage.right - bmp.getWidth());
							srcImage.right = bmp.getWidth();
						}
						if(srcImage.bottom > bmp.getHeight()){
							pixRect.bottom -= (srcImage.bottom - bmp.getHeight());
							srcImage.bottom = bmp.getHeight();
						}
						canvas.drawBitmap(bmp, srcImage, pixRect, paintImg);
					}
					
				}
			}
		}
	}

	protected void draw(Way way, Canvas canvas, double leftTileX, double topTileY, float zoom) {
		Path path = null;
		for (Node node:way.getNodes()) {
			float x = (float) ((MapUtils.getTileNumberX(zoom, node.getLongitude()) - leftTileX) * 256f);
			float y = (float) ((MapUtils.getTileNumberY(zoom, node.getLatitude()) - topTileY) * 256f);
			if (path == null) {
				path = new Path();
				path.moveTo(x, y);
			} else {
				path.lineTo(x, y);
			}
		}
		if (path != null) {
			canvas.drawPath(path, paint);
		}
		
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
