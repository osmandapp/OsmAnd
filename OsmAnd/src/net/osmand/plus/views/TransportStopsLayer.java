package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.R;
import net.osmand.plus.resources.TransportIndexRepository;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class TransportStopsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 12;
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<TransportStop> objects = new ArrayList<TransportStop>();
	private DisplayMetrics dm;
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(view.getResources().getColor(R.color.transport_stop));
		pointAltUI.setAntiAlias(true);
	}
	
	public void getFromPoint(RotatedTileBox tb,PointF point, List<? super TransportStop> res) {
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rp = getRadiusPoi(tb);
			int radius = rp * 3 / 2;
			int small = rp;
			try {
				for (int i = 0; i < objects.size(); i++) {
					TransportStop n = objects.get(i);
					if (n.getLocation() == null){
						continue;
					}
					int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						radius = small;
						res.add(n);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}
	


	
	
	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		ArrayList<TransportStop> stops = new ArrayList<TransportStop >(); 
		getFromPoint(tileBox, point, stops);
		if(!stops.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for (TransportStop n : stops) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(getStopDescription(n, true));
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	private String getStopDescription(TransportStop n, boolean useName) {
		StringBuilder text = new StringBuilder(250);
		text.append(view.getContext().getString(R.string.transport_Stop))
				.append(" : ").append(n.getName(view.getSettings().usingEnglishNames())); //$NON-NLS-1$
		text.append("\n").append(view.getContext().getString(R.string.transport_Routes)).append(" : "); //$NON-NLS-1$ //$NON-NLS-2$
		List<TransportIndexRepository> reps = view.getApplication().getResourceManager().searchTransportRepositories(
				n.getLocation().getLatitude(), n.getLocation().getLongitude());

		for (TransportIndexRepository t : reps) {
			if (t.acceptTransportStop(n)) {
				List<String> l;
				if (!useName) {
					l = t.getRouteDescriptionsForStop(n, "{1} {0}"); //$NON-NLS-1$
				} else if (view.getSettings().usingEnglishNames()) {
					l = t.getRouteDescriptionsForStop(n, "{1} {0} - {3}"); //$NON-NLS-1$
				} else {
					l = t.getRouteDescriptionsForStop(n, "{1} {0} - {2}"); //$NON-NLS-1$
				}
				if (l != null) {
					for (String s : l) {
						text.append("\n").append(s); //$NON-NLS-1$
					}
				}
			}
		}
		return text.toString();
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		final float zoom = tb.getZoom() + tb.getZoomScale();
		int r;
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 8;
		} else if(zoom <= 16){
			r = 10;
		} else if(zoom <= 17){
			r = 14;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tb,
			DrawSettings settings) {
		if (tb.getZoom() >= startZoom) {
			objects.clear();
			final QuadRect latLonBounds = tb.getLatLonBounds();
			view.getApplication().getResourceManager().searchTransportAsync(latLonBounds.top, latLonBounds.left,
					latLonBounds.bottom, latLonBounds.right, tb.getZoom(), objects);
			int r = 3 * getRadiusPoi(tb) / 4;
			for (TransportStop o : objects) {
				int x = tb.getPixXFromLonNoRot(o.getLocation().getLongitude());
				int y = tb.getPixYFromLatNoRot(o.getLocation().getLatitude());
				canvas.drawRect(x - r, y - r, x + r, y + r, pointAltUI);
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof TransportStop){
			return getStopDescription((TransportStop) o, false);
		}
		return null;
	}
	
	private void showDescriptionDialog(TransportStop a) {
		Builder bs = new AlertDialog.Builder(view.getContext());
		bs.setTitle(a.getName(view.getSettings().usingEnglishNames()));
		bs.setMessage(getStopDescription(a, true));
		bs.show();
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof TransportStop){
			return ((TransportStop)o).getName(); 
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getFromPoint(tileBox, point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof TransportStop){
			return ((TransportStop)o).getLocation();
		}
		return null;
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof TransportStop){
			final TransportStop a = (TransportStop) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					showDescriptionDialog(a);
				}
			};
			adapter.item(R.string.poi_context_menu_showdescription)
			.icons( R.drawable.ic_action_note_dark, R.drawable.ic_action_note_light).listen(listener).reg();
		}
	}



}
