package com.osmand.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.osm.MapUtils;

public class OsmBugsLayer implements OsmandMapLayer {

	private static final Log log = LogUtil.getLog(OsmBugsLayer.class); 
	private final static int startZoom = 8;
	private final int SEARCH_LIMIT = 100;
	
	private OsmandMapTileView view;
	private Handler handlerToLoop;
	private Rect pixRect = new Rect();
	private RectF tileRect = new RectF();
	
	private List<OpenStreetBug> objects = new ArrayList<OpenStreetBug>();
	private Paint pointClosedUI;
	private Paint pointOpenedUI;
	private Pattern patternToParse = Pattern.compile("putAJAXMarker\\((\\d*), ((\\d|\\.)*), ((\\d|\\.)*), '([^']*)', (\\d)\\);");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm aaa", Locale.US);
	
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	private int czoom;
	private final Activity activity;
	
	public OsmBugsLayer(Activity activity){
		this.activity = activity;
		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		synchronized (this) {
			if (handlerToLoop == null) {
				new Thread("Open street bugs layer") {
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
		pixRect.set(0, 0, view.getWidth(), view.getHeight());
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
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);

			
			// request to load
			requestToLoad(topLatitude, leftLongitude, bottomLatitude, rightLongitude, view.getZoom());
			for (OpenStreetBug o : objects) {
				int x = view.getMapXForPoint(o.getLongitude());
				int y = view.getMapYForPoint(o.getLatitude());
				canvas.drawCircle(x, y, getRadiusBug(view.getZoom()), o.isOpened()? pointOpenedUI: pointClosedUI);
			}

		}
	}
	
	public int getRadiusBug(int zoom){
		if(zoom < startZoom){
			return 0;
		} else if(zoom <= 12){
			return 6;
		} else if(zoom <= 15){
			return 10;
		} else if(zoom == 16){
			return 13;
		} else if(zoom == 17){
			return 15;
		} else {
			return 18;
		}
	}
	
	public void requestToLoad(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude, final int zoom){
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
						&& cBottomLatitude <= bottomLatitude;
		if(!inside || (czoom != zoom && objects.size() >= SEARCH_LIMIT)){
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
		if (objects != null && !objects.isEmpty()) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusBug(view.getZoom()) * 3 / 2;
			try {
				for (final OpenStreetBug n : objects) {
					int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						Builder builder = new AlertDialog.Builder(view.getContext());
				    	builder.setItems(new String[]{"Add comment", "Close bug"}, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if(which == 0){
									commentBug(view.getContext(), activity.getLayoutInflater(), n);
								} else if(which == 1){
									closeBug(view.getContext(), activity.getLayoutInflater(), n);
								}
							}
				    	});
						builder.create().show();
						return true;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		if (objects != null && !objects.isEmpty()) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusBug(view.getZoom()) * 3 / 2;
			try {
				for (OpenStreetBug n : objects) {
					int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						String format = "Bug : " + n.getName();
						Toast.makeText(view.getContext(), format, Toast.LENGTH_SHORT).show();
						return true;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
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
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?");
		b.append("lat=").append(latitude);
		b.append("&lon=").append(longitude);
		text = text + " [" + authorName +" "+ dateFormat.format(new Date())+ "]";
		b.append("&text=").append(text);
		b.append("&name=").append(authorName);
		return editingPOI(b.toString(), "creating bug");
	}
	
	public boolean addingComment(long id, String text, String authorName){
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?");
		b.append("id=").append(id);
		text = text + " [" + authorName +" "+ dateFormat.format(new Date())+ "]";
		b.append("&text=").append(text);
		b.append("&name=").append(authorName);
		return editingPOI(b.toString(), "adding comment");
	}
	
	public boolean closingBug(long id){
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/closePOIexec?");
		b.append("id=").append(id);
		return editingPOI(b.toString(),"closing bug");
	}
	
	
	private boolean editingPOI(String urlStr, String debugAction){
		urlStr = urlStr.replace(" ", "%20");
		try {
			URL url = new URL(urlStr);
			URLConnection connection = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while(reader.readLine() != null){
			}
			log.debug("Action " + debugAction + " successfull");
			return true;
		} catch (IOException e) {
			log.error("Error " +debugAction, e);
		} catch (RuntimeException e) {
			log.error("Error "+debugAction, e);
		} 
		return false;
	}
	
	
	protected List<OpenStreetBug> loadingBugs(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude){
		List<OpenStreetBug> bugs = new ArrayList<OpenStreetBug>();
		StringBuilder b = new StringBuilder();
		b.append("http://openstreetbugs.schokokeks.org/api/0.1/getBugs?");
		b.append("b=").append(bottomLatitude);
		b.append("&t=").append(topLatitude);
		b.append("&l=").append(leftLongitude);
		b.append("&r=").append(rightLongitude);
		try {
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
					bug.setLatitude(Double.parseDouble(matcher.group(4)));
					bug.setName(matcher.group(6).replace("<hr />", "\n"));
					bug.setOpened(matcher.group(7).equals("0"));
					bugs.add(bug);
				}
			}
		} catch (IOException e) {
			log.warn("Error loading bugs", e);
		} catch (NumberFormatException e) {
			log.warn("Error loading bugs", e);
		} catch (RuntimeException e) {
			log.warn("Error loading bugs", e);
		} 
		
		return bugs;
	}
	

	public void openBug(final Context ctx, LayoutInflater layoutInflater, final OsmandMapTileView mapView){
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle("Input text of bug");
		final View view = layoutInflater.inflate(R.layout.open_bug, null);
		builder.setView(view);
		((EditText)view.findViewById(R.id.AuthorName)).setText(OsmandSettings.getUserName(ctx));
		builder.setNegativeButton("Cancel", null);
		builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = ((EditText)view.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)view.findViewById(R.id.AuthorName)).getText().toString();
				OsmandSettings.setUserName(ctx, author);
				boolean bug = createNewBug(mapView.getLatitude(), mapView.getLongitude(), 
						text, author);
		    	if (bug) {
		    		Toast.makeText(ctx, "Bug was successfully created", Toast.LENGTH_LONG).show();
					clearCache();
					if (mapView.getLayers().contains(OsmBugsLayer.this)) {
						mapView.refreshMap();
					}
				} else {
					Toast.makeText(ctx, "Exception occured : bug was not created", Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.show();
	}
	
	public void commentBug(final Context ctx, LayoutInflater layoutInflater, final OpenStreetBug bug){
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle("Adding comment to bug");
		final View view = layoutInflater.inflate(R.layout.open_bug, null);
		builder.setView(view);
		((EditText)view.findViewById(R.id.AuthorName)).setText(OsmandSettings.getUserName(ctx));
		builder.setNegativeButton("Cancel", null);
		builder.setPositiveButton("Add comment", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = ((EditText)view.findViewById(R.id.BugMessage)).getText().toString();
				String author = ((EditText)view.findViewById(R.id.AuthorName)).getText().toString();
				OsmandSettings.setUserName(ctx, author);
				boolean added = addingComment(bug.getId(), text, author);
		    	if (added) {
		    		Toast.makeText(ctx, "Comment was successfully added", Toast.LENGTH_LONG).show();
					clearCache();
					if (OsmBugsLayer.this.view.getLayers().contains(OsmBugsLayer.this)) {
						OsmBugsLayer.this.view.refreshMap();
					}
				} else {
					Toast.makeText(ctx, "Exception occured : comment was not added", Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.show();
	}
	
	public void closeBug(final Context ctx, LayoutInflater layoutInflater, final OpenStreetBug bug){
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle("Closing bug");
		builder.setNegativeButton("Cancel", null);
		builder.setPositiveButton("Close bug", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean closed = closingBug(bug.getId());
		    	if (closed) {
		    		Toast.makeText(ctx, "Bug was successfully added", Toast.LENGTH_LONG).show();
					clearCache();
					if (OsmBugsLayer.this.view.getLayers().contains(OsmBugsLayer.this)) {
						OsmBugsLayer.this.view.refreshMap();
					}
				} else {
					Toast.makeText(ctx, "Exception occured : bug was not closed", Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.show();
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
