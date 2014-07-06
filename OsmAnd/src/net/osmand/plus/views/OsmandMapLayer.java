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
	
	public abstract class MapLayerData<T> {
		public int ZOOM_THRESHOLD = 1;
		public RotatedTileBox queriedBox;
		protected T results;
		protected Task currentTask;
		protected Task pendingTask;
		
		public RotatedTileBox getQueriedBox() {
			return queriedBox;
		}
		
		public T getResults() {
			return results;
		}
		
		public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
			return queriedData != null && queriedData.containsTileBox(newBox) && Math.abs(queriedData.getZoom() - newBox.getZoom()) <= ZOOM_THRESHOLD;
		}
		
		public void queryNewData(RotatedTileBox tileBox) {
			if (!queriedBoxContains(queriedBox, tileBox) ) {
				Task ct = currentTask;
				if(ct == null || !queriedBoxContains(ct.getDataBox(), tileBox)) {
					RotatedTileBox original = tileBox.copy();
					RotatedTileBox extended = original.copy();
					extended.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
					Task task = new Task(original, extended);
					if (currentTask == null) {
						executeTaskInBackground(task);
					} else {
						pendingTask = task;
					}	
				}
				
			}
		}
		
		public void layerOnPostExecute() {
		}
		
		public boolean isInterrupted() {
			return pendingTask != null;
		}
		
		protected abstract T calculateResult(RotatedTileBox tileBox);
		
		public class Task extends AsyncTask<Object, Object, T> {
			private RotatedTileBox dataBox;
			private RotatedTileBox requestedBox;
			
			public Task(RotatedTileBox requestedBox, RotatedTileBox dataBox) {
				this.requestedBox = requestedBox;
				this.dataBox = dataBox;
			}
			
			public RotatedTileBox getOriginalBox() {
				return requestedBox;
			}
			
			public RotatedTileBox getDataBox() {
				return dataBox;
			}
			
			@Override
			protected T doInBackground(Object... params) {
				if (queriedBoxContains(queriedBox, dataBox) ) {
					return null;
				}
				return calculateResult(dataBox);
			}

			@Override
			protected void onPreExecute() {
				currentTask = this;
			}

			@Override
			protected void onPostExecute(T result) {
				if (result != null) {
					queriedBox = dataBox;
					results = result;
				}
				currentTask = null;
				if (pendingTask != null) {
					executeTaskInBackground(pendingTask);
					pendingTask = null;
				} else {
					layerOnPostExecute();
				}
			}
		}

		public void clearCache() {
			results = null;
			queriedBox = null;
			
		}
		
	}

}
