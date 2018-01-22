package net.osmand.plus.osmedit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.edit.Node;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.List;

public class OsmEditsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
		ContextMenuLayer.IMoveObjectProvider {
	private static final int startZoom = 10;
	private final OsmEditingPlugin plugin;
	private final MapActivity activity;
	private final OpenstreetmapLocalUtil mOsmChangeUtil;
	private final OsmBugsLocalUtil mOsmBugsUtil;
	private Bitmap poi;
	private Bitmap bug;
	private OsmandMapTileView view;
	private Paint paintIcon;

	private ContextMenuLayer contextMenuLayer;

	public OsmEditsLayer(MapActivity activity, OsmEditingPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
		mOsmChangeUtil = plugin.getPoiModificationLocalUtil();
		mOsmBugsUtil = plugin.getOsmNotesLocalUtil();
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		poi = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_poi);
		bug = poi;
		paintIcon = new Paint();

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof OsmPoint) {
			OsmPoint objectInMotion = (OsmPoint) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			drawPoint(canvas, objectInMotion, pf.x, pf.y);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			drawPoints(canvas, tileBox, plugin.getDBBug().getOsmbugsPoints(), fullObjectsLatLon);
			drawPoints(canvas, tileBox, plugin.getDBPOI().getOpenstreetmapPoints(), fullObjectsLatLon);
			this.fullObjectsLatLon = fullObjectsLatLon;
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, List<? extends OsmPoint> objects,
							List<LatLon> fullObjectsLatLon) {
		for (OsmPoint o : objects) {
			if (contextMenuLayer.getMoveableObject() != o) {
				float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				drawPoint(canvas, o, x, y);
				fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
			}
		}
	}

	private void drawPoint(Canvas canvas, OsmPoint o, float x, float y) {
		Bitmap b;
		if (o.getGroup() == OsmPoint.Group.POI) {
			b = poi;
		} else if (o.getGroup() == OsmPoint.Group.BUG) {
			b = bug;
		} else {
			b = poi;
		}
		canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}


	public void getOsmEditsFromPoint(PointF point, RotatedTileBox tileBox, List<? super OsmPoint> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(tileBox);
		int radius = compare * 3 / 2;
		compare = getFromPoint(tileBox, am, ex, ey, compare, radius, plugin.getDBBug().getOsmbugsPoints());
		getFromPoint(tileBox, am, ex, ey, compare, radius, plugin.getDBPOI().getOpenstreetmapPoints());
	}

	private int getFromPoint(RotatedTileBox tileBox, List<? super OsmPoint> am, int ex, int ey, int compare,
							 int radius, List<? extends OsmPoint> pnts) {
		for (OsmPoint n : pnts) {
			int x = (int) tileBox.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, compare)) {
				compare = radius;
				am.add(n);
			}
		}
		return compare;
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius;
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		if (tb.getZoom() < startZoom) {
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof OsmPoint;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= startZoom) {
			getOsmEditsFromPoint(point, tileBox, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OsmPoint) {
			return new LatLon(((OsmPoint) o).getLatitude(), ((OsmPoint) o).getLongitude());
		}
		return null;
	}


	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof OsmPoint) {
			OsmPoint point = (OsmPoint) o;
			String name = "";
			String type = "";
			if (point.getGroup() == OsmPoint.Group.POI) {
				name = ((OpenstreetmapPoint) point).getName();
				type = PointDescription.POINT_TYPE_OSM_NOTE;
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				name = ((OsmNotesPoint) point).getText();
				type = PointDescription.POINT_TYPE_OSM_BUG;
			}
			return new PointDescription(type, name);
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof OsmPoint;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position, @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {
		if (o instanceof OsmPoint) {
			if (o instanceof OpenstreetmapPoint) {
				OpenstreetmapPoint objectInMotion = (OpenstreetmapPoint) o;
				Node node = objectInMotion.getEntity();
				node.setLatitude(position.getLatitude());
				node.setLongitude(position.getLongitude());
				new SaveOsmChangeAsyncTask(mOsmChangeUtil, callback, objectInMotion).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else if (o instanceof OsmNotesPoint) {
				OsmNotesPoint objectInMotion = (OsmNotesPoint) o;
				objectInMotion.setLatitude(position.getLatitude());
				objectInMotion.setLongitude(position.getLongitude());
				new SaveOsmNoteAsyncTask(objectInMotion.getText(), activity, callback, plugin, mOsmBugsUtil)
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, objectInMotion);
			}
		}
	}

	static class SaveOsmChangeAsyncTask extends AsyncTask<Void, Void, Node> {
		private final OpenstreetmapLocalUtil mOpenstreetmapUtil;
		@Nullable
		private final ContextMenuLayer.ApplyMovedObjectCallback mCallback;
		private final OpenstreetmapPoint objectInMotion;

		SaveOsmChangeAsyncTask(OpenstreetmapLocalUtil openstreetmapUtil,
							   @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback, OpenstreetmapPoint objectInMotion) {
			this.mOpenstreetmapUtil = openstreetmapUtil;
			this.mCallback = callback;
			this.objectInMotion = objectInMotion;
		}

		@Override
		protected Node doInBackground(Void... params) {
			Node node = objectInMotion.getEntity();
			return mOpenstreetmapUtil.commitNodeImpl(objectInMotion.getAction(), node,
					mOpenstreetmapUtil.getEntityInfo(node.getId()), "", false, null);
		}

		@Override
		protected void onPostExecute(Node newNode) {
			if (mCallback != null) {
				mCallback.onApplyMovedObject(newNode != null, objectInMotion);
			}
		}
	}

	private static class SaveOsmNoteAsyncTask extends AsyncTask<OsmNotesPoint, Void, OsmNotesPoint> {
		private final String mText;
		private final MapActivity mActivity;
		@Nullable
		private final ContextMenuLayer.ApplyMovedObjectCallback mCallback;
		private final OsmEditingPlugin plugin;
		private OsmBugsUtil mOsmbugsUtil;

		public SaveOsmNoteAsyncTask(String text,
									MapActivity activity,
									@Nullable ContextMenuLayer.ApplyMovedObjectCallback callback,
									OsmEditingPlugin plugin, OsmBugsUtil osmbugsUtil) {
			mText = text;
			mActivity = activity;
			mCallback = callback;
			this.plugin = plugin;
			mOsmbugsUtil = osmbugsUtil;
		}

		@Override
		protected OsmNotesPoint doInBackground(OsmNotesPoint... params) {
			OsmNotesPoint mOsmNotesPoint = params[0];
			OsmPoint.Action action = mOsmNotesPoint.getAction();
			plugin.getDBBug().deleteAllBugModifications(mOsmNotesPoint);
			OsmBugsUtil.OsmBugResult result = mOsmbugsUtil.commit(mOsmNotesPoint, mText, action);
			return result == null ? null : result.local;
		}

		@Override
		protected void onPostExecute(OsmNotesPoint point) {
			if (point != null) {
				Toast.makeText(mActivity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
			}
			if (mCallback != null) {
				mCallback.onApplyMovedObject(point != null, point);
			}
		}
	}
}
