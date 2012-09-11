package net.osmand.plus.osmedit;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.LogUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DialogProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class OsmBugsLayer extends OsmandMapLayer implements IContextMenuProvider, DialogProvider {

	private static final Log log = LogUtil.getLog(OsmBugsLayer.class); 
	private final static int startZoom = 8;
	private final int SEARCH_LIMIT = 100;
	
	private final OsmBugsUtil osmbugsUtil;
	private OsmandMapTileView view;
	private Handler handlerToLoop;
	
	private List<OpenStreetBug> objects = new ArrayList<OpenStreetBug>();
	private Paint pointClosedUI;
	private Paint pointOpenedUI;
	private Pattern patternToParse = Pattern.compile("putAJAXMarker\\((\\d*), (-?(?:\\d|\\.)+), (-?(?:\\d|\\.)+), '([^']*)', (\\d)\\);"); //$NON-NLS-1$
//	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm aaa", Locale.US); //$NON-NLS-1$
	
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	private int czoom;
	private final MapActivity activity;
	private DisplayMetrics dm;
	
	private static final String KEY_AUTHOR = "author";
	private static final String KEY_MESSAGE = "message";
	protected static final String KEY_LATITUDE = "latitude";
	protected static final String KEY_LONGITUDE = "longitude";
	protected static final String KEY_BUG = "bug";
	private static final int DIALOG_OPEN_BUG = 300;
	private static final int DIALOG_COMMENT_BUG = 301;
	private static final int DIALOG_CLOSE_BUG = 302;
	private Bundle dialogBundle = new Bundle();
	
	public OsmBugsLayer(MapActivity activity){
		this.activity = activity;

		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)){
			this.osmbugsUtil = new OsmBugsLocalUtil(activity);
		} else {
			this.osmbugsUtil = new OsmBugsRemoteUtil();
		}
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
		pointOpenedUI.setColor(activity.getResources().getColor(R.color.osmbug_opened));
		pointOpenedUI.setAntiAlias(true);
		pointClosedUI = new Paint();
		pointClosedUI.setColor(activity.getResources().getColor(R.color.osmbug_closed));
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
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
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
							refreshMap();
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
	
	public void getBugFromPoint(PointF point, List<? super OpenStreetBug> res){
		if (objects != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusBug(view.getZoom()) * 3 / 2;
			int small = getRadiusBug(view.getZoom()) * 3 / 4;
			try {
				for (int i = 0; i < objects.size(); i++) {
					OpenStreetBug n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
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
	public boolean onSingleTap(PointF point) {
		ArrayList<OpenStreetBug> list = new ArrayList<OpenStreetBug>();
		getBugFromPoint(point, list);
		if(!list.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(OpenStreetBug o : list) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(activity.getString(R.string.osb_bug_name)+ " : " + o.getName()); //$NON-NLS-1$
			}
			AccessibleToast.makeText(activity, res.toString(), Toast.LENGTH_LONG).show();
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
	
	private void openBugAlertDialog(final double latitude, final double longitude, String message, String authorName){
		dialogBundle.putDouble(KEY_LATITUDE, latitude);
		dialogBundle.putDouble(KEY_LONGITUDE, longitude);
		dialogBundle.putString(KEY_MESSAGE, message);
		dialogBundle.putString(KEY_AUTHOR, authorName);
		activity.showDialog(DIALOG_OPEN_BUG);
	}
	
	private void prepareOpenBugDialog(Dialog dlg, Bundle args) {
		((EditText)dlg.findViewById(R.id.BugMessage)).setText(args.getString(KEY_MESSAGE));
		((EditText)dlg.findViewById(R.id.AuthorName)).setText(args.getString(KEY_AUTHOR));
	}
	
	private Dialog createOpenBugDialog(final Bundle args) {
		final View openBug = activity.getLayoutInflater().inflate(R.layout.open_bug, null);
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.osb_add_dialog_title);
		builder.setView(openBug);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final double latitude = args.getDouble(KEY_LATITUDE);
				final double longitude = args.getDouble(KEY_LONGITUDE);

				String text = ((EditText)openBug.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)openBug.findViewById(R.id.AuthorName)).getText().toString();
				// do not set name as author it is ridiculous in that case
				((OsmandApplication) activity.getApplication()).getSettings().USER_OSM_BUG_NAME.set(author);
				boolean bug = osmbugsUtil.createNewBug(latitude, longitude, text, author);
		    	if (bug) {
		    		AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_add_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_add_dialog_error), Toast.LENGTH_LONG).show();
					openBugAlertDialog(latitude, longitude, text, author);
				}
			}
		});
		return builder.create();
	}
	

	public void openBug(final double latitude, final double longitude){
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		openBugAlertDialog(latitude, longitude, "", settings.USER_OSM_BUG_NAME.get());
	}
	
	public void commentBug(final OpenStreetBug bug){
		dialogBundle.putSerializable(KEY_BUG, bug);
		activity.showDialog(DIALOG_COMMENT_BUG);
	}
	
	private Dialog createCommentBugDialog(final Bundle args) {
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.osb_comment_dialog_title);
		final View view = activity.getLayoutInflater().inflate(R.layout.open_bug, null);
		builder.setView(view);
		((EditText)view.findViewById(R.id.AuthorName)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_OSM_BUG_NAME.get());
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.osb_comment_dialog_add_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OpenStreetBug bug = (OpenStreetBug) args.getSerializable(KEY_BUG);
				String text = ((EditText)view.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)view.findViewById(R.id.AuthorName)).getText().toString();
				((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_OSM_BUG_NAME.set(author);
				boolean added = osmbugsUtil.addingComment(bug.getId(), text, author);
		    	if (added) {
		    		AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_error), Toast.LENGTH_LONG).show();
				}
			}
		});
		return builder.create();
	}
	
	public void refreshMap(){
		if (view != null && view.getLayers().contains(OsmBugsLayer.this)) {
			view.refreshMap();
		}
	}
	
	public void closeBug(final OpenStreetBug bug){
		dialogBundle.putSerializable(KEY_BUG, bug);
		activity.showDialog(DIALOG_CLOSE_BUG);
	}
	
	private Dialog createCloseBugDialog(final Bundle args) {
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.osb_close_dialog_title);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.osb_close_dialog_close_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OpenStreetBug bug = (OpenStreetBug) args.getSerializable(KEY_BUG);
				boolean closed = osmbugsUtil.closingBug(bug.getId(), "", ((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_OSM_BUG_NAME.get());
		    	if (closed) {
		    		AccessibleToast.makeText(activity, activity.getString(R.string.osb_close_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getString(R.string.osb_close_dialog_error), Toast.LENGTH_LONG).show();
				}
			}
		});
		return builder.create();
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof OpenStreetBug) {
			final OpenStreetBug bug = (OpenStreetBug) o;
			OnContextMenuClick listener = new OnContextMenuClick() {
				
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.osb_comment_menu_item) {
						commentBug(bug);
					} else if (itemId == R.string.osb_close_menu_item) {
						closeBug(bug);
					}
				}
			};
			adapter.registerItem(R.string.osb_comment_menu_item, R.drawable.list_activities_add_comment, listener, -1);
			adapter.registerItem(R.string.osb_close_menu_item, R.drawable.list_activities_close_bug, listener, -1);
		}
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OpenStreetBug){
			return activity.getString(R.string.osb_bug_name) + " : " + ((OpenStreetBug)o).getName(); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof OpenStreetBug){
			return ((OpenStreetBug)o).getName(); 
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> res) {
		getBugFromPoint(point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof OpenStreetBug){
			return new LatLon(((OpenStreetBug)o).getLatitude(), ((OpenStreetBug)o).getLongitude());
		}
		return null;
	}

	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_OPEN_BUG:
				return createOpenBugDialog(args);
			case DIALOG_COMMENT_BUG:
				return createCommentBugDialog(args);
			case DIALOG_CLOSE_BUG:
				return createCloseBugDialog(args);
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_OPEN_BUG: 
				prepareOpenBugDialog(dialog, args);
		}
	}
	
	public static class OpenStreetBug implements Serializable {
		private static final long serialVersionUID = -7848941747811172615L;
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
