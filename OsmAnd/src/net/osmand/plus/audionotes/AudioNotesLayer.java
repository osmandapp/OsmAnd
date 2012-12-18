package net.osmand.plus.audionotes;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
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
	private Bitmap bmp;

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
		
		bmp = BitmapFactory.decodeResource(view.getResources(), R.drawable.device_access_video);

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
			int r = getRadiusPoi(view.getZoom());
			for (Recording o : objects) {
				int x = view.getRotatedMapXForPoint(o.getLatitude(), o.getLongitude());
				int y = view.getRotatedMapYForPoint(o.getLatitude(), o.getLongitude());
				canvas.drawCircle(x, y, r, pointAltUI);
				canvas.drawCircle(x, y, r, point);
				canvas.drawBitmap(bmp, x - bmp.getWidth() / 2, y - bmp.getHeight() / 2, paintIcon);
			}
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if (o instanceof Recording) {
			final Recording r = (Recording) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.recording_context_menu_play) {
						playRecording(r);
					} else if (itemId == R.string.recording_context_menu_delete) {
						deleteRecording(r);
					}
				}


			};
			adapter.registerItem(R.string.recording_context_menu_play, 0, listener, -1);
			adapter.registerItem(R.string.recording_context_menu_delete, 0, listener, -1);
		}
	}
	
	private void playRecording(final Recording r) {
		final MediaPlayer player = new MediaPlayer();
		final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(view.getContext());
		dlg.setMessage(view.getContext().getString(R.string.recording_playing, r.getDescription(view.getContext())));
		dlg.setPositiveButton(R.string.recording_open_external_player, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface v, int w) {
				if(player.isPlaying()) {
					player.stop();
				}
				Intent vint = new Intent(Intent.ACTION_VIEW);
		    	vint.setDataAndType(Uri.fromFile(r.file), "video/*");
		    	vint.setFlags(0x10000000);
				try {
					view.getContext().startActivity(vint);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		dlg.setNegativeButton(R.string.default_buttons_cancel, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(player.isPlaying()) {
					player.stop();
				}				
				
			}
			
		});
		try {
			player.setDataSource(r.file.getAbsolutePath());
			player.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					dlg.show();
					player.start();
				}
			});
			player.prepareAsync();
		} catch (Exception e) {
			AccessibleToast.makeText(activity, R.string.recording_can_not_be_played, Toast.LENGTH_SHORT).show();
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
		List<Recording> objects = plugin.getRecordings().getAllObjects();
		for (int i = 0; i < objects.size(); i++) {
			Recording n = objects.get(i);
			int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
			int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
			if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
				compare = radius;
				am.add(n);
			}
		}
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