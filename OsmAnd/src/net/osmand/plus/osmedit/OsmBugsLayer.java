package net.osmand.plus.osmedit;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DialogProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class OsmBugsLayer extends OsmandMapLayer implements IContextMenuProvider, DialogProvider {

	private static final Log log = PlatformUtil.getLog(OsmBugsLayer.class); 
	private final static int startZoom = 8;
	
	private OsmandMapTileView view;
	
	private Paint pointClosedUI;
	private Paint pointOpenedUI;
	private Paint pointNotSubmitedUI;
	
	private final MapActivity activity;

	private static final String KEY_AUTHOR = "author";
	private static final String KEY_MESSAGE = "message";
	protected static final String KEY_LATITUDE = "latitude";
	protected static final String KEY_LONGITUDE = "longitude";
	protected static final String KEY_BUG = "bug";
	private static final int DIALOG_OPEN_BUG = 300;
	private static final int DIALOG_COMMENT_BUG = 301;
	private static final int DIALOG_CLOSE_BUG = 302;
	private static Bundle dialogBundle = new Bundle();
	private OsmBugsLocalUtil local;
	private OsmBugsRemoteUtil remote;
	private MapLayerData<List<OpenStreetNote>> data;
	
	public OsmBugsLayer(MapActivity activity){
		this.activity = activity;
		local = new OsmBugsLocalUtil(activity);
		remote = new OsmBugsRemoteUtil(activity.getMyApplication());
	}
	
	public OsmBugsUtil getOsmbugsUtil(OpenStreetNote bug) {
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		if ((bug != null && bug.isLocal() )|| settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)){
			return local;
		} else {
			return remote;
		}
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		pointOpenedUI = new Paint();
		pointOpenedUI.setColor(activity.getResources().getColor(R.color.osmbug_opened));
		pointOpenedUI.setAntiAlias(true);
		pointNotSubmitedUI = new Paint();
		pointNotSubmitedUI.setColor(activity.getResources().getColor(R.color.osmbug_not_submitted));
		pointNotSubmitedUI.setAntiAlias(true);
		pointClosedUI = new Paint();
		pointClosedUI.setColor(activity.getResources().getColor(R.color.osmbug_closed));
		pointClosedUI.setAntiAlias(true);
		
		data = new OsmandMapLayer.MapLayerData<List<OpenStreetNote>>() {

			{
				ZOOM_THRESHOLD = 1;
			}
			
			@Override
			protected List<OpenStreetNote> calculateResult(RotatedTileBox tileBox) {
				QuadRect bounds = tileBox.getLatLonBounds();
				return loadingBugs(bounds.top, bounds.left, bounds.bottom, bounds.right);
			}
		};
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			data.queryNewData(tileBox);
			List<OpenStreetNote> objects = data.getResults();
			if (objects != null) {
				for (OpenStreetNote o : objects) {
					int x = tileBox.getPixXFromLonNoRot(o.getLongitude());
					int y = tileBox.getPixYFromLatNoRot(o.getLatitude());
					canvas.drawCircle(x, y, getRadiusBug(tileBox), o.isLocal() ? pointNotSubmitedUI
							: (o.isOpened() ? pointOpenedUI : pointClosedUI));
				}
			}
		}
	}
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		
	}
	
	public int getRadiusBug(RotatedTileBox tb) {
		int z;
		final float zoom = tb.getZoom() + tb.getZoomScale();
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
		return (int) (z * tb.getDensity());
	}
	
	
	
	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}
	
	public void getBugFromPoint(RotatedTileBox tb, PointF point, List<? super OpenStreetNote> res){
		List<OpenStreetNote> objects = data.getResults();
		if (objects != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rad = getRadiusBug(tb);
			int radius = rad * 3 / 2;
			int small = rad * 3 / 4;
			try {
				for (int i = 0; i < objects.size(); i++) {
					OpenStreetNote n = objects.get(i);
					int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
					int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
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
		ArrayList<OpenStreetNote> list = new ArrayList<OpenStreetNote>();
		getBugFromPoint(tileBox, point, list);
		if(!list.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(OpenStreetNote o : list) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(activity.getString(R.string.osb_bug_name)+ " : " + o.getCommentDescription()); //$NON-NLS-1$
			}
			AccessibleToast.makeText(activity, res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	public void clearCache() {
		if(data != null) {
			data.clearCache();
		}
	}
	
	private static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
		int tok;
		String text = "";
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if(tok == XmlPullParser.END_TAG && parser.getName().equals(key)){
				break;
			} else if(tok == XmlPullParser.TEXT){
				text += parser.getText();
			}
			
		}
		return text;
	}
	
	
	protected List<OpenStreetNote> loadingBugs(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude){
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;
		
		String SITE_API;
		
		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			SITE_API = "https://api.openstreetmap.org/";
		}
		else {
			SITE_API = "http://api.openstreetmap.org/";
		}
	
		List<OpenStreetNote> bugs = new ArrayList<OpenStreetNote>();
		StringBuilder b = new StringBuilder();
		b.append(SITE_API + "api/0.6/notes?bbox="); //$NON-NLS-1$
		b.append(leftLongitude); //$NON-NLS-1$
		b.append(",").append(bottomLatitude); //$NON-NLS-1$
		b.append(",").append(rightLongitude); //$NON-NLS-1$
		b.append(",").append(topLatitude); //$NON-NLS-1$
		try {
			log.info("Loading bugs " + b); //$NON-NLS-1$
			URL url = new URL(b.toString());
			URLConnection connection = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(reader);
			int tok;
			OpenStreetNote current = null;
			int commentIndex = 0;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					if (parser.getName().equals("note")) {
						current = new OpenStreetNote();
						commentIndex = -1;
						current.setLongitude(Double.parseDouble(parser.getAttributeValue("", "lon")));
						current.setLatitude(Double.parseDouble(parser.getAttributeValue("", "lat")));
						current.setOpened(true);
						bugs.add(current);
					} else if (parser.getName().equals("status") && current != null) {
						current.setOpened("open".equals(readText(parser, "status")));
					} else if (parser.getName().equals("id") && current != null) {
						current.id = Long.parseLong(readText(parser, "id"));
					} else if (parser.getName().equals("comment")) {
						commentIndex ++;
					} else if (parser.getName().equals("user") && current != null) {
						if(commentIndex == current.users.size()) {
							current.users.add(readText(parser, "user"));
						}
					} else if (parser.getName().equals("date") && current != null) {
						if(commentIndex == current.dates.size()) {
							current.dates.add(readText(parser, "date"));
						}
					} else if (parser.getName().equals("text") && current != null) {
						if(commentIndex == current.comments.size()) {
							current.comments.add(readText(parser, "text"));
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (RuntimeException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} 
		for(OsmNotesPoint p : local.getOsmbugsPoints() ) {
			if(p.getId() < 0 ) {
				OpenStreetNote bug = new OpenStreetNote();
				bug.setId(p.getId());
				bug.setLongitude(p.getLongitude());
				bug.setLatitude(p.getLatitude());
				bug.dates.add("");
				bug.users.add(activity.getMyApplication().getSettings().USER_NAME.get());
				bug.comments.add(p.getText()); 
				bug.setOpened(p.getAction() == Action.CREATE || p.getAction() == Action.MODIFY);
				bug.setLocal(true);
				bugs.add(bug);
			}
		}
		return bugs;
	}
	
	private void openBugAlertDialog(final double latitude, final double longitude, String message){
		dialogBundle.putDouble(KEY_LATITUDE, latitude);
		dialogBundle.putDouble(KEY_LONGITUDE, longitude);
		dialogBundle.putString(KEY_MESSAGE, message);
		OsmandSettings settings = activity.getMyApplication().getSettings();
		dialogBundle.putString(KEY_AUTHOR, settings.USER_NAME.get());
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
		((EditText)openBug.findViewById(R.id.Password)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_PASSWORD.get());
		((EditText)openBug.findViewById(R.id.AuthorName)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_NAME.get());
		AndroidUtils.softKeyboardDelayed((EditText)openBug.findViewById(R.id.BugMessage));
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final double latitude = args.getDouble(KEY_LATITUDE);
				final double longitude = args.getDouble(KEY_LONGITUDE);
				final String text = getTextAndUpdateUserPwd(openBug);
				createNewBugAsync(latitude, longitude, text);
			}

		});
		return builder.create();
	}
	
	private void createNewBugAsync(final double latitude, final double longitude, final String text) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return getOsmbugsUtil(null).createNewBug(latitude, longitude, text);
			}

			protected void onPostExecute(String result) {
				if (result == null) {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_add_dialog_success),
							Toast.LENGTH_LONG).show();
					clearCache();
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_add_dialog_error) + "\n" + result,
							Toast.LENGTH_LONG).show();
					openBugAlertDialog(latitude, longitude, text);
				}
			};
		};
		executeTaskInBackground(task);
	}
	
	private void addingCommentAsync(final OpenStreetNote bug, final String text) {
		AsyncTask<Void,Void,String> task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return getOsmbugsUtil(bug).addingComment(bug.getId(), text);
			}
			protected void onPostExecute(String warn) {
				if (warn == null) {
		    		AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_error) + "\n" + warn, Toast.LENGTH_LONG).show();
				}
			};
		};
		executeTaskInBackground(task);
	}
	

	public void openBug(final double latitude, final double longitude){
		openBugAlertDialog(latitude, longitude, "");
	}
	
	public void commentBug(final OpenStreetNote bug){
		dialogBundle.putSerializable(KEY_BUG, bug);
		activity.showDialog(DIALOG_COMMENT_BUG);
	}
	
	private Dialog createCommentBugDialog(final Bundle args) {
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.osb_comment_dialog_title);
		final View view = activity.getLayoutInflater().inflate(R.layout.open_bug, null);
		builder.setView(view);
		((EditText)view.findViewById(R.id.AuthorName)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_NAME.get());
		((EditText)view.findViewById(R.id.Password)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_PASSWORD.get());
		AndroidUtils.softKeyboardDelayed((EditText)view.findViewById(R.id.BugMessage));
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.osb_comment_dialog_add_button, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OpenStreetNote bug = (OpenStreetNote) args.getSerializable(KEY_BUG);
				String text = getTextAndUpdateUserPwd(view);
				addingCommentAsync(bug, text);
			}
		});
		return builder.create();
	}
	
	private String getTextAndUpdateUserPwd(final View view) {
		String text = ((EditText)view.findViewById(R.id.BugMessage)).getText().toString();
		String author = ((EditText)view.findViewById(R.id.AuthorName)).getText().toString();
		String pwd = ((EditText)view.findViewById(R.id.Password)).getText().toString();
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_NAME.set(author);
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_PASSWORD.set(pwd);
		return text;
	}
	
	public void refreshMap(){
		if (view != null && view.getLayers().contains(OsmBugsLayer.this)) {
			view.refreshMap();
		}
	}
	
	public void closeBug(final OpenStreetNote bug){
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
				OpenStreetNote bug = (OpenStreetNote) args.getSerializable(KEY_BUG);
				String us = activity.getMyApplication().getSettings().USER_NAME.get();
				String pwd = activity.getMyApplication().getSettings().USER_PASSWORD.get();
				if(us.length() == 0 || pwd.length() == 0) {
					AccessibleToast.makeText(activity, activity.getString(R.string.osb_author_or_password_not_specified),
							Toast.LENGTH_SHORT).show();
				}
				closingAsync(bug, "");
			}
		});
		return builder.create();
	}
	
	private void closingAsync(final OpenStreetNote bug, final String text) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return getOsmbugsUtil(bug).closingBug(bug.getId(), "");
			}

			protected void onPostExecute(String closed) {
				if (closed == null) {
					AccessibleToast.makeText(activity, activity.getString(R.string.osb_close_dialog_success), Toast.LENGTH_LONG).show();
					clearCache();
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getString(R.string.osb_close_dialog_error) + "\n" + closed,
							Toast.LENGTH_LONG).show();
				}
			};
		};
		executeTaskInBackground(task);
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof OpenStreetNote) {
			final OpenStreetNote bug = (OpenStreetNote) o;
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
			adapter.item(R.string.osb_comment_menu_item).icons(
					R.drawable.ic_action_note_dark,  R.drawable.ic_action_note_light
					).listen(listener).reg();
			adapter.item(R.string.osb_close_menu_item).icons(
					R.drawable.ic_action_remove_dark,R.drawable.ic_action_remove_light
					).listen(listener).reg();
		}
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OpenStreetNote){
			return activity.getString(R.string.osb_bug_name) + " : " + ((OpenStreetNote)o).getCommentDescription(); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof OpenStreetNote){
			return ((OpenStreetNote)o).getCommentDescription(); 
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getBugFromPoint(tileBox, point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof OpenStreetNote){
			return new LatLon(((OpenStreetNote)o).getLatitude(), ((OpenStreetNote)o).getLongitude());
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
				break;
			case DIALOG_COMMENT_BUG: 
				((EditText)dialog.findViewById(R.id.BugMessage)).setText("");
				break;
		}
	}
	
	public static class OpenStreetNote implements Serializable {
		private boolean local;
		private static final long serialVersionUID = -7848941747811172615L;
		private double latitude;
		private double longitude;
		private String name;
		private List<String> dates = new ArrayList<String>();
		private List<String> comments = new ArrayList<String>();
		private List<String> users = new ArrayList<String>();
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
		
		public String getCommentDescription() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < comments.size(); i++) {
				if (i < dates.size()) {
					sb.append(dates.get(i)).append(" ");
				}
				if (i < users.size()) {
					sb.append(users.get(i)).append(" : ");
				}
				sb.append(comments.get(i)).append("\n");
			}
			return sb.toString();
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
		
		public boolean isLocal() {
			return local;
		}
		
		public void setLocal(boolean local) {
			this.local = local;
		}
	}

}
