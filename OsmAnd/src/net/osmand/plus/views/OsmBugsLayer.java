package net.osmand.plus.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.LogUtil;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class OsmBugsLayer implements OsmandMapLayer, ContextMenuLayer.IContextMenuProvider {

	private static final Log log = LogUtil.getLog(OsmBugsLayer.class); 
	private final static int startZoom = 8;
	private final int SEARCH_LIMIT = 100;
	
	private OsmandMapTileView view;
	private Handler handlerToLoop;
	
	private List<OpenStreetBug> objects = new ArrayList<OpenStreetBug>();
	private Paint pointClosedUI;
	private Paint pointOpenedUI;
	private Pattern patternToParse = Pattern.compile("putAJAXMarker\\((\\d*), (-?(?:\\d|\\.)+), (-?(?:\\d|\\.)+), '([^']*)', (\\d)\\);"); //$NON-NLS-1$
//	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm aaa", Locale.US); //$NON-NLS-1$
	
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	private int czoom;
	private final Activity activity;
	private DisplayMetrics dm;
	
	public OsmBugsLayer(Activity activity){
		this.activity = activity;
		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		synchronized (this) {
			if (handlerToLoop == null) {
				new Thread("Open street bugs layer") { //$NON-NLS-1$
					@Override
					public void run() {
						Looper.prepare();
						handlerToLoop = new Handler();
						Looper.loop();
					}
				}.start();
			}
			
		}
		pointOpenedUI = new Paint();
		pointOpenedUI.setColor(Color.RED);
		pointOpenedUI.setAlpha(200);
		pointOpenedUI.setAntiAlias(true);
		pointClosedUI = new Paint();
		pointClosedUI.setColor(Color.GREEN);
		pointClosedUI.setAlpha(200);
		pointClosedUI.setAntiAlias(true);
	}

	@Override
	public void destroyLayer() {
		synchronized (this) {
			if(handlerToLoop != null){
				handlerToLoop.post(new Runnable(){
					@Override
					public void run() {
						Looper.myLooper().quit();
					}
				});
				handlerToLoop = null;
			}
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, boolean nightMode) {
		if (view.getZoom() >= startZoom) {
			// request to load
			requestToLoad(latLonBounds.top, latLonBounds.left, latLonBounds.bottom, latLonBounds.right, view.getZoom());
			for (OpenStreetBug o : objects) {
				int x = view.getMapXForPoint(o.getLongitude());
				int y = view.getMapYForPoint(o.getLatitude());
				canvas.drawCircle(x, y, getRadiusBug(view.getZoom()), o.isOpened()? pointOpenedUI: pointClosedUI);
			}

		}
	}
	
	public int getRadiusBug(int zoom) {
		int z;
		if (zoom < startZoom) {
			z = 0;
		} else if (zoom <= 12) {
			z = 8;
		} else if (zoom <= 15) {
			z = 10;
		} else if (zoom == 16) {
			z = 13;
		} else if (zoom == 17) {
			z = 15;
		} else {
			z = 16;
		}
		return (int) (z * dm.density);
	}
	
	public void requestToLoad(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude, final int zoom){
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
						&& cBottomLatitude <= bottomLatitude;
		if((!inside || (czoom != zoom && objects.size() >= SEARCH_LIMIT)) && handlerToLoop != null){
			handlerToLoop.removeMessages(1);
			final double nTopLatitude = topLatitude + (topLatitude -bottomLatitude);
			final double nBottomLatitude = bottomLatitude - (topLatitude -bottomLatitude);
			final double nLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
			final double nRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
			Message msg = Message.obtain(handlerToLoop, new Runnable() {
				@Override
				public void run() {
					if(handlerToLoop != null && !handlerToLoop.hasMessages(1)){
						boolean inside = cTopLatitude >= nTopLatitude && cLeftLongitude <= nLeftLongitude && cRightLongitude >= nRightLongitude
										&& cBottomLatitude <= nBottomLatitude;
						if (!inside || czoom != zoom) {
							objects = loadingBugs(nTopLatitude, nLeftLongitude, nBottomLatitude, nRightLongitude);
							cTopLatitude = nTopLatitude;
							cLeftLongitude = nLeftLongitude;
							cRightLongitude = nRightLongitude;
							cBottomLatitude = nBottomLatitude;
							czoom = zoom;
							view.refreshMap();
						}
					}
				}
			});
			msg.what = 1;
			handlerToLoop.sendMessage(msg);
		}
	}
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public OpenStreetBug getBugFromPoint(PointF point){
		OpenStreetBug result = null;
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusBug(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					OpenStreetBug n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						radius = Math.max(Math.abs(x - ex), Math.abs(y - ey));
						result = n;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		return result;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		OpenStreetBug bug = getBugFromPoint(point);
		if(bug != null){
			String format = view.getContext().getString(R.string.osb_bug_name)+ " : " + bug.getName(); //$NON-NLS-1$
			Toast.makeText(view.getContext(), format, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}
	
	


	public void clearCache() {
		objects.clear();
		cTopLatitude = 0;
		cBottomLatitude = 0;
		cLeftLongitude = 0;
		cRightLongitude = 0;
	}
	
	public boolean createNewBug(double latitude, double longitude, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?"); //$NON-NLS-1$
		b.append("lat=").append(latitude); //$NON-NLS-1$
		b.append("&lon=").append(longitude); //$NON-NLS-1$
		text = text + " [" + authorName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		b.append("&name=").append(URLEncoder.encode(authorName)); //$NON-NLS-1$
		return editingPOI(b.toString(), "creating bug"); //$NON-NLS-1$
	}
	
	public boolean addingComment(long id, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?"); //$NON-NLS-1$
		b.append("id=").append(id); //$NON-NLS-1$
		text = text + " [" + authorName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		b.append("&name=").append(URLEncoder.encode(authorName)); //$NON-NLS-1$
		return editingPOI(b.toString(), "adding comment"); //$NON-NLS-1$
	}
	
	public boolean closingBug(long id){
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/closePOIexec?"); //$NON-NLS-1$
		b.append("id=").append(id); //$NON-NLS-1$
		return editingPOI(b.toString(),"closing bug"); //$NON-NLS-1$
	}
	
	
	private boolean editingPOI(String urlStr, String debugAction){
		try {
			log.debug("Action " + debugAction + " " + urlStr); //$NON-NLS-1$ //$NON-NLS-2$
			URL url = new URL(urlStr);
			URLConnection connection = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while(reader.readLine() != null){
			}
			log.debug("Action " + debugAction + " successfull"); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} catch (IOException e) {
			log.error("Error " +debugAction, e); //$NON-NLS-1$
		} catch (RuntimeException e) {
			log.error("Error "+debugAction, e); //$NON-NLS-1$
		} 
		return false;
	}
	
	protected List<OpenStreetBug> loadingBugs(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude){
		List<OpenStreetBug> bugs = new ArrayList<OpenStreetBug>();
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/getBugs?"); //$NON-NLS-1$
		b.append("b=").append(bottomLatitude); //$NON-NLS-1$
		b.append("&t=").append(topLatitude); //$NON-NLS-1$
		b.append("&l=").append(leftLongitude); //$NON-NLS-1$
		b.append("&r=").append(rightLongitude); //$NON-NLS-1$
		try {
			log.info("Loading bugs " + b.toString()); //$NON-NLS-1$
			URL url = new URL(b.toString());
			URLConnection connection = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String st = null;
			while((st = reader.readLine()) != null){
				Matcher matcher = patternToParse.matcher(st);
				if(matcher.find()){
					OpenStreetBug bug = new OpenStreetBug();
					bug.setId(Long.parseLong(matcher.group(1)));
					bug.setLongitude(Double.parseDouble(matcher.group(2)));
					bug.setLatitude(Double.parseDouble(matcher.group(3)));
					bug.setName(matcher.group(4).replace("<hr />", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
					bug.setOpened(matcher.group(5).equals("0")); //$NON-NLS-1$
					bugs.add(bug);
				}
			}
		} catch (IOException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (RuntimeException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} 
		
		return bugs;
	}
	
	private void openBugAlertDialog(final Context ctx, final LayoutInflater layoutInflater, final OsmandMapTileView mapView, final double latitude, final double longitude, String message, String authorName){
		final View openBug = layoutInflater.inflate(R.layout.open_bug, null);
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.osb_add_dialog_title);
		builder.setView(openBug);
		((EditText)openBug.findViewById(R.id.BugMessage)).setText(message);
		((EditText)openBug.findViewById(R.id.AuthorName)).setText(authorName);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = ((EditText)openBug.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)openBug.findViewById(R.id.AuthorName)).getText().toString();
				// do not set name as author it is ridiculous in that case
				OsmandSettings.setUserNameForOsmBug(ctx, author);
				boolean bug = createNewBug(latitude, longitude, text, author);
		    	if (bug) {
		    		Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_add_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					if (mapView.getLayers().contains(OsmBugsLayer.this)) {
						mapView.refreshMap();
					}
				} else {
					Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_add_dialog_error), Toast.LENGTH_LONG).show();
					openBugAlertDialog(ctx, layoutInflater, mapView, latitude, longitude, text, author);
				}
			}
		});
		builder.show();
	}
	

	public void openBug(final Context ctx, LayoutInflater layoutInflater, final OsmandMapTileView mapView,  final double latitude, final double longitude){
		openBugAlertDialog(ctx, layoutInflater, mapView, latitude, longitude, "", OsmandSettings.getUserNameForOsmBug(OsmandSettings.getPrefs(ctx)));
	}
	
	public void commentBug(final Context ctx, LayoutInflater layoutInflater, final OpenStreetBug bug){
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.osb_comment_dialog_title);
		final View view = layoutInflater.inflate(R.layout.open_bug, null);
		builder.setView(view);
		((EditText)view.findViewById(R.id.AuthorName)).setText(OsmandSettings.getUserNameForOsmBug(OsmandSettings.getPrefs(ctx)));
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.osb_comment_dialog_add_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = ((EditText)view.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)view.findViewById(R.id.AuthorName)).getText().toString();
				OsmandSettings.setUserNameForOsmBug(ctx, author);
				boolean added = addingComment(bug.getId(), text, author);
		    	if (added) {
		    		Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_comment_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					if (OsmBugsLayer.this.view.getLayers().contains(OsmBugsLayer.this)) {
						OsmBugsLayer.this.view.refreshMap();
					}
				} else {
					Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_comment_dialog_error), Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.show();
	}
	
	public void closeBug(final Context ctx, LayoutInflater layoutInflater, final OpenStreetBug bug){
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.osb_close_dialog_title);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.osb_close_dialog_close_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean closed = closingBug(bug.getId());
		    	if (closed) {
		    		Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_close_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					if (OsmBugsLayer.this.view.getLayers().contains(OsmBugsLayer.this)) {
						OsmBugsLayer.this.view.refreshMap();
					}
				} else {
					Toast.makeText(ctx, ctx.getResources().getString(R.string.osb_close_dialog_error), Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.show();
	}
	
	
	@Override
	public OnClickListener getActionListener(List<String> actionsList, Object o) {
		final OpenStreetBug bug = (OpenStreetBug) o;
		actionsList.add(view.getContext().getString(R.string.osb_comment_menu_item));
		actionsList.add(view.getContext().getString(R.string.osb_close_menu_item));
		return new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					commentBug(view.getContext(), activity.getLayoutInflater(), bug);
				} else if (which == 1) {
					closeBug(view.getContext(), activity.getLayoutInflater(), bug);
				}
			}
		};
	}


	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OpenStreetBug){
			return view.getContext().getString(R.string.osb_bug_name) + " : " + ((OpenStreetBug)o).getName(); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public Object getPointObject(PointF point) {
		return getBugFromPoint(point);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof OpenStreetBug){
			return new LatLon(((OpenStreetBug)o).getLatitude(), ((OpenStreetBug)o).getLongitude());
		}
		return null;
	}


	
	public static class OpenStreetBug {
		private double latitude;
		private double longitude;
		private String name;
		private long id;
		private boolean opened;
		public double getLatitude() {
			return latitude;
		}
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}
		public double getLongitude() {
			return longitude;
		}
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public boolean isOpened() {
			return opened;
		}
		public void setOpened(boolean opened) {
			this.opened = opened;
		}
		
		
	}

}
