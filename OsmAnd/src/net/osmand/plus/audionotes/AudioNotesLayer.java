package net.osmand.plus.audionotes;

import android.graphics.Canvas;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AudioNotesLayer extends OsmandMapLayer implements
		IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int startZoom = 10;
	private MapActivity activity;
	private AudioVideoNotesPlugin plugin;
	private OsmandMapTileView view;
	private ContextMenuLayer contextMenuLayer;

	public AudioNotesLayer(MapActivity activity, AudioVideoNotesPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r = 0;
		if (tb.getZoom() < startZoom) {
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof Recording) {
			Recording objectInMotion = (Recording) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			float textScale = activity.getMyApplication().getSettings().TEXT_SCALE.get();
			drawRecording(canvas, objectInMotion, pf.x, pf.y, textScale);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			OsmandApplication app = activity.getMyApplication();
			float textScale = app.getSettings().TEXT_SCALE.get();
			float iconSize = getIconSize(app);
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			DataTileManager<Recording> recs = plugin.getRecordings();
			final QuadRect latlon = tileBox.getLatLonBounds();
			List<Recording> objects = recs.getObjects(latlon.top, latlon.left, latlon.bottom, latlon.right);
			List<Recording> fullObjects = new ArrayList<>();
			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			List<LatLon> smallObjectsLatLon = new ArrayList<>();
			for (Recording o : objects) {
				if (o != contextMenuLayer.getMoveableObject()) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(activity,
								ContextCompat.getColor(activity, R.color.audio_video_icon_color), true);
						pointImageDrawable.setAlpha(0.8f);
						pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
						smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					} else {
						fullObjects.add(o);
						fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					}
				}
			}
			for (Recording o : fullObjects) {
				float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				drawRecording(canvas, o, x, y, textScale);
			}
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
		}
	}

	private void drawRecording(Canvas canvas, Recording o, float x, float y, float textScale) {
		int iconId;
		if (o.isPhoto()) {
			iconId = R.drawable.mx_special_photo_camera;
		} else if (o.isAudio()) {
			iconId = R.drawable.mx_special_microphone;
		} else {
			iconId = R.drawable.mx_special_video_camera;
		}
		PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(activity,
				ContextCompat.getColor(activity, R.color.audio_video_icon_color), true, iconId);
		pointImageDrawable.setAlpha(0.8f);
		pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Recording) {
			Recording rec = (Recording) o;
			if (rec.getFile().exists()) {
				String recName = rec.getName(activity, true);
				if (Algorithms.isEmpty(recName)) {
					return new PointDescription(rec.getSearchHistoryType(), view.getResources().getString(R.string.recording_default_name));
				}
				return new PointDescription(rec.getSearchHistoryType(), recName);
			} else {
				plugin.deleteRecording(rec, true);
			}
		}
		return null;
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
		return o instanceof Recording;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects, boolean unknownLocation) {
		if (tileBox.getZoom() >= startZoom) {
			getRecordingsFromPoint(point, tileBox, objects);
		}
	}

	public void getRecordingsFromPoint(PointF point, RotatedTileBox tileBox, List<? super Recording> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getScaledTouchRadius(activity.getMyApplication(), getRadiusPoi(tileBox));
		int radius = compare * 3 / 2;
		for (Recording n : plugin.getAllRecordings()) {
			int x = (int) tileBox.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, compare)) {
				compare = radius;
				am.add(n);
			}
		}
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Recording) {
			return new LatLon(((Recording) o).getLatitude(), ((Recording) o).getLongitude());
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof Recording;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position, @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {
		boolean result = false;
		if (o instanceof Recording) {
			result = ((Recording) o).setLocation(position);
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, o);
		}
	}
}