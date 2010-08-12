package com.osmand.render;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;

import com.osmand.ResourceManager;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.Way;
import com.osmand.views.OsmandMapLayer;
import com.osmand.views.OsmandMapTileView;

public class RendererLayer implements OsmandMapLayer {

	private OsmandMapTileView view;
	private final static int startZoom = 15;
	private Rect pixRect;
	private RectF tileRect;
	private ResourceManager resourceManager;
	private Paint paint;

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
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
			
			resourceManager.updateRendererIfNeeded(topLatitude, leftLongitude, bottomLatitude, rightLongitude, view.getZoom());
			RenderMapsRepositories renderer = resourceManager.getRenderer();
			if(renderer != null) {
				for (Way way:renderer.getCache()) {
					draw(way,canvas);
				}
			}

		}
		
	}

	private void draw(Way way, Canvas canvas) {
		
		Path path = null;
		for (Node node:way.getNodes()) {
			int x = view.getMapXForPoint(node.getLongitude());
			int y = view.getMapYForPoint(node.getLatitude());
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
