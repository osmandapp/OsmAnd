package net.osmand.plus.audionotes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;

import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AudioNotesLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int startZoom = 10;
	private MapActivity activity;
	private AudioVideoNotesPlugin plugin;
	private Paint pointAltUI;
	private Paint paintIcon;
	private Paint point;
	private OsmandMapTileView view;
	private Bitmap audio;
	private Bitmap video;
	private Bitmap photo;
	private Bitmap pointSmall;

	public AudioNotesLayer(MapActivity activity, AudioVideoNotesPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		pointAltUI = new Paint();
		pointAltUI.setColor(0xa0FF3344);
		pointAltUI.setStyle(Style.FILL);
		
		audio = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_note_audio);
		video = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_note_video);
		photo = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_note_photo);

		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_note_small);

		paintIcon = new Paint();

		point = new Paint();
		point.setColor(Color.GRAY);
		point.setAntiAlias(true);
		point.setStyle(Style.STROKE);
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		if(tb.getZoom()  < startZoom){
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			float iconSize = audio.getWidth() * 3 / 2.5f;
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			DataTileManager<Recording> recs = plugin.getRecordings();
			final QuadRect latlon = tileBox.getLatLonBounds();
			List<Recording> objects = recs.getObjects(latlon.top, latlon.left, latlon.bottom, latlon.right);
			List<Recording> fullObjects = new ArrayList<>();
			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			List<LatLon> smallObjectsLatLon = new ArrayList<>();
			for (Recording o : objects) {
				float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
					smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
				} else {
					fullObjects.add(o);
					fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
				}
			}
			for (Recording o : fullObjects) {
				float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				Bitmap b;
				if (o.isPhoto()) {
					b = photo;
				} else if (o.isAudio()) {
					b = audio;
				} else {
					b = video;
				}
				canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
			}
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof Recording){
			return ((Recording)o).getDescription(view.getContext());
		}
		return null;
	}
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof Recording){
			Recording rec = (Recording) o;
			String recName = rec.getName(activity, true);
			if(Algorithms.isEmpty(recName)) {
				return new PointDescription(rec.getSearchHistoryType(), view.getResources().getString(R.string.recording_default_name));
			}
			return new PointDescription(rec.getSearchHistoryType(), recName);
		}
		return null;
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
		return o instanceof Recording;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects) {
		if (tileBox.getZoom() >= startZoom) {
			getRecordingsFromPoint(point, tileBox, objects);
		}
	}
	
	public void getRecordingsFromPoint(PointF point, RotatedTileBox tileBox, List<? super Recording> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(tileBox);
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
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof Recording){
			return new LatLon(((Recording)o).getLatitude(), ((Recording)o).getLongitude());
		}
		return null;
	}


}