package net.osmand.plus.audionotes;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

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
			DataTileManager<Recording> recs = plugin.getRecordings();
			final QuadRect latlon = tileBox.getLatLonBounds();
			List<Recording> objects = recs.getObjects(latlon. top, latlon.left, latlon.bottom, latlon.right);
			for (Recording o : objects) {
				int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
				Bitmap b;
				if (o.isPhoto()) {
					b = photo;
				} else if (o.isAudio()) {
					b = audio;
				} else {
					b = video;

				}
				canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight(), paintIcon);
			}
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
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if (o instanceof Recording) {
			final Recording r = (Recording) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					if (itemId == R.string.recording_context_menu_play ||
							itemId == R.string.recording_context_menu_show) {
						plugin.playRecording(view.getContext(), r);
					} else if (itemId == R.string.recording_context_menu_delete) {
						deleteRecording(r);
					}
					return true;
				}


			};
			if(r.isPhoto()) {
				adapter.item(R.string.recording_context_menu_show).iconColor(
						R.drawable.ic_action_view).listen(listener).reg();
			} else {
				adapter.item(R.string.recording_context_menu_play).iconColor(
						R.drawable.ic_action_play_dark).listen(listener).reg();
			}
			adapter.item(R.string.recording_context_menu_delete).iconColor(R.drawable.ic_action_delete_dark
					).listen(listener).reg();
		}
	}
	
	private void deleteRecording(final Recording r) {
		AccessibleAlertBuilder bld = new AccessibleAlertBuilder(activity);
		bld.setMessage(R.string.recording_delete_confirm);
		bld.setPositiveButton(R.string.shared_string_yes, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				plugin.deleteRecording(r);				
			}
		});
		bld.setNegativeButton(R.string.shared_string_no, null);
		bld.show();
		
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
			if(rec.getName(activity).isEmpty()) {
				return new PointDescription(rec.getSearchHistoryType(), view.getResources().getString(R.string.recording_default_name));
			}
			return new PointDescription(rec.getSearchHistoryType(), ((Recording)o).getName(activity));
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects) {
		getRecordingsFromPoint(point, tileBox, objects);
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
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		ArrayList<Recording> o = new ArrayList<Recording>();
		getRecordingsFromPoint(point, tileBox, o);
		if(o.size() > 0){
			StringBuilder b = new StringBuilder();
			for(Recording r : o) {
				b.append(getObjectDescription(r)).append('\n');
			}
			AccessibleToast.makeText(activity, b.toString().trim(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof Recording){
			return new LatLon(((Recording)o).getLatitude(), ((Recording)o).getLongitude());
		}
		return null;
	}


}