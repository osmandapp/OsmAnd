package net.osmand.plus.audionotes;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class AudioNotesLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int startZoom = 10;
	private MapActivity activity;
	private AudioVideoNotesPlugin plugin;
	private DisplayMetrics dm;
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
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

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
	
	public int getRadiusPoi(int zoom){
		int r = 0;
		if(zoom < startZoom){
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * dm.density);
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings) {
		if (view.getZoom() >= startZoom) {
			DataTileManager<Recording> recs = plugin.getRecordings();
			List<Recording> objects = recs.getObjects(latlonRect.top, latlonRect.left, latlonRect.bottom, latlonRect.right);
			for (Recording o : objects) {
				int x = view.getRotatedMapXForPoint(o.getLatitude(), o.getLongitude());
				int y = view.getRotatedMapYForPoint(o.getLatitude(), o.getLongitude());
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
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.recording_context_menu_play ||
							itemId == R.string.recording_context_menu_show) {
						plugin.playRecording(view.getContext(), r);
					} else if (itemId == R.string.recording_context_menu_delete) {
						deleteRecording(r);
					}
				}


			};
			if(r.isPhoto()) {
				adapter.registerItem(R.string.recording_context_menu_show, R.drawable.list_activities_play_note, listener, -1);
			} else {
				adapter.registerItem(R.string.recording_context_menu_play, R.drawable.list_activities_play_note, listener, -1);
			}
			adapter.registerItem(R.string.recording_context_menu_delete, R.drawable.list_activities_remove_note, listener, -1);
		}
	}
	
	private void deleteRecording(final Recording r) {
		AccessibleAlertBuilder bld = new AccessibleAlertBuilder(activity);
		bld.setMessage(R.string.recording_delete_confirm);
		bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				plugin.deleteRecording(r);				
			}
		});
		bld.setNegativeButton(R.string.default_buttons_no, null);
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
	public String getObjectName(Object o) {
		if(o instanceof Recording){
			if(((Recording)o).name == null) {
				return view.getResources().getString(R.string.recording_default_name);
			}
			return ((Recording)o).name; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> objects) {
		getRecordingsFromPoint(point, objects);
	}
	
	public void getRecordingsFromPoint(PointF point, List<? super Recording> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(view.getZoom());
		int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
		for (Recording n : plugin.getAllRecordings()) {
			int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
			int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
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
	public boolean onSingleTap(PointF point) {
		ArrayList<Recording> o = new ArrayList<Recording>();
		getRecordingsFromPoint(point, o);
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