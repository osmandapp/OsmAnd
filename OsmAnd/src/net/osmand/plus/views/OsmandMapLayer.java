package net.osmand.plus.views;

import java.util.Map;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.view.MotionEvent;

public abstract class OsmandMapLayer {
	
	
	public abstract void initLayer(OsmandMapTileView view);
	
	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings);

	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}
	
	public abstract void destroyLayer();
	
	public void onRetainNonConfigurationInstance(Map<String, Object> map) {}
	
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}
	
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}
	
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) { return false;}
	
	public <Params> void executeTaskInBackground(AsyncTask<Params, ?, ?> task, Params... params){
		task.execute(params);
	}
	
	/**
	 * This method returns whether canvas should be rotated as 
	 * map rotated before {@link #onDraw(android.graphics.Canvas, net.osmand.data.RotatedTileBox, net.osmand.plus.views.OsmandMapLayer.DrawSettings)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public abstract boolean drawInScreenPixels();
	
	public static class DrawSettings {
		private final boolean nightMode;
		private final boolean updateVectorRendering;

		public DrawSettings(boolean nightMode) {
			this(nightMode,false);
		}

		public DrawSettings(boolean nightMode, boolean updateVectorRendering) {
			this.nightMode = nightMode;
			this.updateVectorRendering = updateVectorRendering;
		}
		
		public boolean isUpdateVectorRendering() {
			return updateVectorRendering;
		}
		public boolean isNightMode() {
			return nightMode;
		}
	}

}
