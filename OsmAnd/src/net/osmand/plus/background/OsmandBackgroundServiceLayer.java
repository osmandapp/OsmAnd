package net.osmand.plus.background;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.Toast;

public class OsmandBackgroundServiceLayer extends OsmandMapLayer {
	
	private final MapActivity map;
	
	public OsmandBackgroundServiceLayer(MapActivity activity) {
		map = activity;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect,
			DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		AccessibleToast.makeText(map, R.string.bg_service_screen_lock_toast, Toast.LENGTH_SHORT).show();
		return true;
	}
	
	@Override
	public boolean onSingleTap(PointF point) {
		AccessibleToast.makeText(map, R.string.bg_service_screen_lock_toast, Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
}
