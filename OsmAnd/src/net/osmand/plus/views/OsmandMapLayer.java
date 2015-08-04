package net.osmand.plus.views;

import gnu.trove.list.array.TIntArrayList;

import java.util.List;
import java.util.Map;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.util.MapAlgorithms;
import android.graphics.Canvas;
import android.graphics.Path;
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
			this(nightMode, false);
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
	
	
	private boolean isIn(int x, int y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}
	
	public int calculateSplitPaths(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys,
			TIntArrayList results) {
		int px = xs.get(0);
		int py = ys.get(0);
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int cnt = 0;
		boolean pin = isIn(px, py, 0, 0, w, h);
		Path path = null;
		for(int i = 1; i < xs.size(); i++) {
			int x = xs.get(i);
			int y = ys.get(i);
			boolean in = isIn(x, y, 0, 0, w, h);
			boolean draw = false;
			if(pin && in) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(x, y,
						px, py, 0, w, h, 0);
				if (intersection != -1) {
					draw = true;
				}
			}
			if (draw) {
				path = new Path();
				results.add(px);
				results.add(py);
				results.add(x);
				results.add(y);
			}
			pin = in;
			px = x;
			py = y;
		}
		return cnt;
	}
	
	public int calculatePath(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys, Path path) {
		boolean start = false;
		int px = xs.get(0);
		int py = ys.get(0);
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int cnt = 0;
		boolean pin = isIn(px, py, 0, 0, w, h);
		for(int i = 1; i < xs.size(); i++) {
			int x = xs.get(i);
			int y = ys.get(i);
			boolean in = isIn(x, y, 0, 0, w, h);
			boolean draw = false;
			if(pin && in) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(x, y,
						px, py, 0, w, h, 0);
				if (intersection != -1) {
					px = (int) (intersection >> 32);
					py = (int) (intersection & 0xffffffff);
					draw = true;
				}
			}
			if (draw) {
				if (!start) {
					cnt++;
					path.moveTo(px, py);
				}
				path.lineTo(x, y);
				start = true;
			} else{
				start = false;
			}
			pin = in;
			px = x;
			py = y;
		}
		return cnt;
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
