package net.osmand.render;

import net.osmand.ResourceManager;
import net.osmand.osm.MapUtils;
import net.osmand.views.OsmandMapLayer;
import net.osmand.views.OsmandMapTileView;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class RendererLayer implements OsmandMapLayer {

	private OsmandMapTileView view;
	private final static int startZoom = 15;
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
			double topLatitude = MapUtils.getLatitudeFromTile(view.getFloatZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getFloatZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getFloatZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getFloatZoom(), tileRect.right);
			resourceManager.updateRendererIfNeeded(topLatitude, leftLongitude, bottomLatitude, rightLongitude, view.getZoom());
			RenderMapsRepositories renderer = resourceManager.getRenderer();
			renderer.setResources(view.getResources());
			if (renderer != null && renderer.getBitmap() != null) {
				RectF newLoc = renderer.getCachedWaysLoc();
				double leftX1 = MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.left);
				double rightX1 = MapUtils.getTileNumberX(view.getFloatZoom(), newLoc.right);
				double topY1 = MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.top);
				double bottomY1 = MapUtils.getTileNumberY(view.getFloatZoom(), newLoc.bottom);

				float x1 = (float) ((leftX1 - view.getXTile()) * view.getTileSize() + view.getCenterPointX());
				float y1 = (float) ((topY1 - view.getYTile()) * view.getTileSize() + view.getCenterPointY());
				float x2 = (float) ((rightX1 - view.getXTile()) * view.getTileSize() + view.getCenterPointX());
				float y2 = (float) ((bottomY1 - view.getYTile()) * view.getTileSize() + view.getCenterPointY());

				destImage.set(x1, y1, x2, y2);
				canvas.drawBitmap(renderer.getBitmap(), null, destImage, paintImg);
			}
		}
	}

	
	public void setVisible(boolean visible) {
		this.visible = visible;
		view.setShowMapTiles(!visible);
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
