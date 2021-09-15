package net.osmand.plus.osmedit;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.MapTextLayer;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmEditsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
		ContextMenuLayer.IMoveObjectProvider, MapTextProvider<OpenstreetmapPoint> {
	private static final int startZoom = 10;
	private final OsmandApplication app;
	private final OsmEditingPlugin plugin;
	private final Context ctx;
	private final OpenstreetmapLocalUtil mOsmChangeUtil;
	private final OsmBugsLocalUtil mOsmBugsUtil;

	private ContextMenuLayer contextMenuLayer;
	private MapTextLayer mapTextLayer;

	public OsmEditsLayer(@NonNull Context context, OsmEditingPlugin plugin) {
		super(context);
		this.ctx = context;
		this.plugin = plugin;
		app = (OsmandApplication) context.getApplicationContext();
		mOsmChangeUtil = plugin.getPoiModificationLocalUtil();
		mOsmBugsUtil = plugin.getOsmNotesLocalUtil();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
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
			drawOsmbugsPoints(canvas, tileBox, fullObjectsLatLon);
			drawOpenstreetmapPoints(canvas, tileBox, fullObjectsLatLon);
			this.fullObjectsLatLon = fullObjectsLatLon;
		}
	}

	private void drawOsmbugsPoints(Canvas canvas, RotatedTileBox tileBox, List<LatLon> fullObjectsLatLon) {
		drawPoints(canvas, tileBox, plugin.getDBBug().getOsmbugsPoints(), fullObjectsLatLon);
	}

	private void drawOpenstreetmapPoints(Canvas canvas, RotatedTileBox tileBox, List<LatLon> fullObjectsLatLon) {
		List<OpenstreetmapPoint> objects = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmPoint> result = drawPoints(canvas, tileBox, objects, fullObjectsLatLon);
		mapTextLayer.putData(this, result);
	}

	private List<OsmPoint> drawPoints(Canvas canvas, RotatedTileBox tileBox,
	                                  List<? extends OsmPoint> objects, List<LatLon> fullObjectsLatLon) {
		float iconSize = getIconSize(app);
		List<OsmPoint> fullObjects = new ArrayList<>();
		for (OsmPoint o : objects) {
			if (contextMenuLayer.getMoveableObject() != o) {
				float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				if (tileBox.containsPoint(x, y, iconSize)) {
					drawPoint(canvas, o, x, y);
					fullObjects.add(o);
					fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
				}
			}
		}
		return fullObjects;
	}

	private void drawPoint(Canvas canvas, OsmPoint osmPoint, float x, float y) {
		float textScale = app.getSettings().TEXT_SCALE.get();
		int iconId = getIconId(osmPoint);
		BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;
		if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			backgroundType = BackgroundType.COMMENT;
		}
		PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx,
				ContextCompat.getColor(ctx, R.color.created_poi_icon_color), true, false,
				iconId, backgroundType);
		pointImageDrawable.setAlpha(0.8f);
		int offsetY = backgroundType.getOffsetY(ctx, textScale);
		pointImageDrawable.drawPoint(canvas, x, y - offsetY, textScale, false);
	}

	public int getIconId(OsmPoint osmPoint) {
		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			OpenstreetmapPoint osmP = (OpenstreetmapPoint) osmPoint;
			int iconResId = 0;
			String poiTranslation = osmP.getEntity().getTag(Entity.POI_TYPE_TAG);
			if (poiTranslation != null && ctx != null) {
				Map<String, PoiType> poiTypeMap = app.getPoiTypes().getAllTranslatedNames(false);
				PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
				if (poiType != null) {
					String id = null;
					if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
						id = poiType.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
						id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
					}
					if (id != null) {
						iconResId = RenderingIcons.getBigIconResourceId(id);
					}
				}
			}
			if (iconResId == 0) {
				iconResId = R.drawable.ic_action_info_dark;
			}
			return iconResId;
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			return R.drawable.mm_special_symbol_plus;
		} else {
			return 0;
		}
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
		int compare = getScaledTouchRadius(app, getRadiusPoi(tileBox));
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
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
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
	public boolean showMenuAction(@Nullable Object o) {
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
				Entity entity = objectInMotion.getEntity();
				entity.setLatitude(position.getLatitude());
				entity.setLongitude(position.getLongitude());
				new SaveOsmChangeAsyncTask(mOsmChangeUtil, callback, objectInMotion).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else if (o instanceof OsmNotesPoint) {
				OsmNotesPoint objectInMotion = (OsmNotesPoint) o;
				objectInMotion.setLatitude(position.getLatitude());
				objectInMotion.setLongitude(position.getLongitude());
				new SaveOsmNoteAsyncTask(objectInMotion.getText(), ctx, callback, plugin, mOsmBugsUtil)
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, objectInMotion);
			}
		}
	}

	@Override
	public LatLon getTextLocation(OpenstreetmapPoint o) {
		return o.getLocation();
	}

	@Override
	public int getTextShift(OpenstreetmapPoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(OpenstreetmapPoint o) {
		return o.getName();
	}

	@Override
	public boolean isTextVisible() {
		return app.getSettings().SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	static class SaveOsmChangeAsyncTask extends AsyncTask<Void, Void, Entity> {
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
		protected Entity doInBackground(Void... params) {
			Entity entity = objectInMotion.getEntity();
			return mOpenstreetmapUtil.commitEntityImpl(objectInMotion.getAction(), entity,
					mOpenstreetmapUtil.getEntityInfo(entity.getId()), "", false, null);
		}

		@Override
		protected void onPostExecute(Entity newEntity) {
			if (mCallback != null) {
				mCallback.onApplyMovedObject(newEntity != null, objectInMotion);
			}
		}
	}

	private static class SaveOsmNoteAsyncTask extends AsyncTask<OsmNotesPoint, Void, OsmNotesPoint> {
		private final String mText;
		private final Context mContext;
		@Nullable
		private final ContextMenuLayer.ApplyMovedObjectCallback mCallback;
		private final OsmEditingPlugin plugin;
		private final OsmBugsUtil mOsmbugsUtil;

		public SaveOsmNoteAsyncTask(String text,
		                            @NonNull Context context,
		                            @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback,
		                            OsmEditingPlugin plugin, OsmBugsUtil osmbugsUtil) {
			mText = text;
			mContext = context;
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
				Toast.makeText(mContext, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
			}
			if (mCallback != null) {
				mCallback.onApplyMovedObject(point != null, point);
			}
		}
	}
}
