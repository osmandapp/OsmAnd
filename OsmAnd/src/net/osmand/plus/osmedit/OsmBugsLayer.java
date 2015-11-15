package net.osmand.plus.osmedit;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DialogProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmBugsUtil.Action;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class OsmBugsLayer extends OsmandMapLayer implements IContextMenuProvider, DialogProvider {

	private static final Log log = PlatformUtil.getLog(OsmBugsLayer.class); 
	private final static int startZoom = 8;
	private final OsmEditingPlugin plugin;

	private OsmandMapTileView view;

	private Paint paintIcon;
	private Bitmap unresolvedNote;
	private Bitmap resolvedNote;
	private Bitmap unresolvedNoteSmall;
	private Bitmap resolvedNoteSmall;

	private final MapActivity activity;
	protected static final String KEY_BUG = "bug";
	protected static final String KEY_ACTION = "action";
	private static final int DIALOG_BUG = 305;
	private static Bundle dialogBundle = new Bundle();
	private OsmBugsLocalUtil local;
	private OsmBugsRemoteUtil remote;
	private MapLayerData<List<OpenStreetNote>> data;
	
	public OsmBugsLayer(MapActivity activity, OsmEditingPlugin plugin){
		this.activity = activity;
		this.plugin = plugin;
		local = new OsmBugsLocalUtil(activity, plugin.getDBBug());
		remote = new OsmBugsRemoteUtil(activity.getMyApplication());
	}
	
	public OsmBugsUtil getOsmbugsUtil(OpenStreetNote bug) {
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		if ((bug != null && bug.isLocal()) || settings.OFFLINE_EDITION.get()
				|| !settings.isInternetConnectionAvailable(true)) {
			return local;
		} else {
			return remote;
		}
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		paintIcon = new Paint();
		resolvedNote = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_osm_resolved);
		unresolvedNote = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_osm_unresolved);
		resolvedNoteSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_osm_resolved_small);
		unresolvedNoteSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_osm_unresolved_small);

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
		return true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			data.queryNewData(tileBox);
			List<OpenStreetNote> objects = data.getResults();

			if (objects != null) {
				float iconSize = resolvedNote.getWidth() * 3 / 2.5f;
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				List<OpenStreetNote> fullObjects = new ArrayList<>();
				for (OpenStreetNote o : objects) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						Bitmap b;
						if (o.isOpened()) {
							b = unresolvedNoteSmall;
						} else {
							b = resolvedNoteSmall;
						}
						canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
					} else {
						fullObjects.add(o);
					}
				}
				for (OpenStreetNote o : fullObjects) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					Bitmap b;
					if (o.isOpened()) {
						b = unresolvedNote;
					} else {
						b = resolvedNote;
					}
					canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
				}
			}
		}
	}
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		
	}
	
	public int getRadiusBug(RotatedTileBox tb) {
		int z;
		final double zoom = tb.getZoom();
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
			URLConnection connection = NetworkUtils.getHttpURLConnection(b.toString());
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
			for (OpenStreetNote note : bugs) {
				note.acquireDescriptionAndType();
			}
		} catch (IOException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (RuntimeException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		} 
		return bugs;
	}
	

	
	private void createNewBugAsync(final double latitude, final double longitude, final String text) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			private OsmBugsUtil osmbugsUtil;

			@Override
			protected String doInBackground(Void... params) {
				osmbugsUtil = getOsmbugsUtil(null);
				return osmbugsUtil.createNewBug(latitude, longitude, text);
			}

			protected void onPostExecute(String result) {
				if (result == null) {
					if(local == osmbugsUtil) { 
						AccessibleToast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
						List<OsmNotesPoint> points = plugin.getDBBug().getOsmbugsPoints();
						if(points.size() > 0) {
							OsmPoint point = points.get(points.size() - 1);
							activity.getContextMenu().showOrUpdate(new LatLon(latitude, longitude), plugin.getOsmEditsLayer(activity).getObjectName(point), point);
						}
					} else {
						AccessibleToast.makeText(activity, R.string.osn_add_dialog_success, Toast.LENGTH_LONG).show();
						activity.getContextMenu().close();
					}
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osn_add_dialog_error) + "\n" + result,
							Toast.LENGTH_LONG).show();
					openBug(latitude, longitude, text);
				}
			}
		};
		executeTaskInBackground(task);
	}
	
	private void addingCommentAsync(final OpenStreetNote bug, final String text) {
		AsyncTask<Void,Void,String> task = new AsyncTask<Void, Void, String>() {
			private OsmBugsUtil osmbugsUtil;
			@Override
			protected String doInBackground(Void... params) {
				osmbugsUtil = getOsmbugsUtil(bug);
				return osmbugsUtil.addingComment(bug.getLatitude(), bug.getLongitude(), bug.getId(), text);
			}
			protected void onPostExecute(String warn) {
				if (warn == null) {
					if(local == osmbugsUtil) { 
						AccessibleToast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
					} else {
						AccessibleToast.makeText(activity, R.string.osb_comment_dialog_success, Toast.LENGTH_LONG).show();
					}
					clearCache();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_error) + "\n" + warn, Toast.LENGTH_LONG).show();
				}
			}
		};
		executeTaskInBackground(task);
	}
	
	private void reopeningtAsync(final OpenStreetNote bug, final String text) {
		AsyncTask<Void,Void,String> task = new AsyncTask<Void, Void, String>() {
			private OsmBugsUtil osmbugsUtil;
			@Override
			protected String doInBackground(Void... params) {
				osmbugsUtil = getOsmbugsUtil(bug);
				return osmbugsUtil.reopenBug(bug.getLatitude(), bug.getLongitude(), bug.getId(), text);
			}
			protected void onPostExecute(String warn) {
				if (warn == null) {
					if(local == osmbugsUtil) { 
						AccessibleToast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
					} else {
						AccessibleToast.makeText(activity, R.string.osb_comment_dialog_success, Toast.LENGTH_LONG).show();
					}
					clearCache();
				} else {
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.osb_comment_dialog_error) + "\n" + warn, Toast.LENGTH_LONG).show();
				}
			}
		};
		executeTaskInBackground(task);
	}
	

	public void openBug(final double latitude, final double longitude, String message){
		OpenStreetNote bug = new OpenStreetNote();
		bug.setLatitude(latitude);
		bug.setLongitude(longitude);
		bug.comments.add(message);
		dialogBundle = new Bundle();
		dialogBundle.putSerializable(KEY_BUG, bug);
		dialogBundle.putSerializable(KEY_ACTION, OsmBugsUtil.Action.CREATE.name());
		activity.showDialog(DIALOG_BUG);
	}
	
	public void closeBug(final OpenStreetNote bug){
		dialogBundle = new Bundle();
		dialogBundle.putSerializable(KEY_BUG, bug);
		dialogBundle.putSerializable(KEY_ACTION, OsmBugsUtil.Action.CLOSE.name());
		activity.showDialog(DIALOG_BUG);
	}
	
	public void reopenBug(final OpenStreetNote bug){
		dialogBundle = new Bundle();
		dialogBundle.putSerializable(KEY_BUG, bug);
		dialogBundle.putSerializable(KEY_ACTION, OsmBugsUtil.Action.REOPEN.name());
		activity.showDialog(DIALOG_BUG);
	}
	
	public void commentBug(final OpenStreetNote bug){
		dialogBundle = new Bundle();
		dialogBundle.putSerializable(KEY_BUG, bug);
		dialogBundle.putSerializable(KEY_ACTION, OsmBugsUtil.Action.MODIFY.name());
		activity.showDialog(DIALOG_BUG);
	}
	
	private Dialog createBugDialog(final Bundle args) {
		final OpenStreetNote bug = (OpenStreetNote) args.getSerializable(KEY_BUG);
		final Action action = OsmBugsUtil.Action.valueOf((String) args.getSerializable(KEY_ACTION));
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		int title ;
		if(action == Action.CLOSE) {
			title = R.string.osn_close_dialog_title;
		} else if(action == Action.MODIFY) {
			title = R.string.osn_comment_dialog_title;
		} else if(action == Action.REOPEN) {
			title = R.string.osn_reopen_dialog_title;
		} else {
			title = R.string.osn_add_dialog_title;
		}
		builder.setTitle(title);
		final View view = activity.getLayoutInflater().inflate(R.layout.open_bug, null);
		builder.setView(view);
		
		if(action == Action.CREATE && bug.comments.size() > 0) {
			((EditText)view.findViewById(R.id.messageEditText)).setText(bug.comments.get(0));
		}
		OsmBugsUtil util = getOsmbugsUtil(bug);
		final boolean offline =  util instanceof OsmBugsLocalUtil;
		if(offline) {
			view.findViewById(R.id.userNameEditText).setVisibility(View.GONE);
			view.findViewById(R.id.userNameEditTextLabel).setVisibility(View.GONE);
			view.findViewById(R.id.passwordEditText).setVisibility(View.GONE);
			view.findViewById(R.id.passwordEditTextLabel).setVisibility(View.GONE);
		} else {
			((EditText)view.findViewById(R.id.userNameEditText)).setText(getUserName());
			((EditText)view.findViewById(R.id.passwordEditText)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_PASSWORD.get());
		}
		AndroidUtils.softKeyboardDelayed((EditText) view.findViewById(R.id.messageEditText));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(title, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (bug != null) {
					String text = offline ? getMessageText(view) : getTextAndUpdateUserPwd(view);
					// server validation will handle it
//					if (us.length() == 0 || pwd.length() == 0) {
//						AccessibleToast.makeText(activity, activity.getString(R.string.osb_author_or_password_not_specified),
//								Toast.LENGTH_SHORT).show();
//					}
					if(action == Action.CLOSE) {
						closingAsync(bug, text);
					} else if(action == Action.MODIFY) {
						addingCommentAsync(bug, text);
					} else if(action == Action.REOPEN) {
						reopeningtAsync(bug, text);
					} else {
						createNewBugAsync(bug.getLatitude(), bug.getLongitude(), text);
					}
					
					activity.getContextMenu().close();
				}
			}
		});
		return builder.create();
	}

	private String getUserName() {
		return ((OsmandApplication) activity.getApplication()).getSettings().USER_NAME.get();
	}
	
	private String getTextAndUpdateUserPwd(final View view) {
		String text = getMessageText(view);
		String author = ((EditText)view.findViewById(R.id.userNameEditText)).getText().toString();
		String pwd = ((EditText)view.findViewById(R.id.passwordEditText)).getText().toString();
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_NAME.set(author);
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_PASSWORD.set(pwd);
		return text;
	}

	private String getMessageText(final View view) {
		return ((EditText)view.findViewById(R.id.messageEditText)).getText().toString();
	}
	
	public void refreshMap(){
		if (view != null && view.getLayers().contains(OsmBugsLayer.this)) {
			view.refreshMap();
		}
	}
	
	
	private void closingAsync(final OpenStreetNote bug, final String text) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			private OsmBugsUtil osmbugsUtil;

			@Override
			protected String doInBackground(Void... params) {
				osmbugsUtil = getOsmbugsUtil(bug);
				return osmbugsUtil.closingBug(bug.getLatitude(), bug.getLongitude(), bug.getId(), text);
			}

			protected void onPostExecute(String closed) {
				if (closed == null) {
					if(local == osmbugsUtil) { 
						AccessibleToast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
					} else {
						AccessibleToast.makeText(activity, R.string.osn_close_dialog_success, Toast.LENGTH_LONG).show();
					}
					clearCache();
					refreshMap();
				} else {
					AccessibleToast.makeText(activity, activity.getString(R.string.osn_close_dialog_error) + "\n" + closed,
							Toast.LENGTH_LONG).show();
				}
			}
		};
		executeTaskInBackground(task);
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OpenStreetNote){
			return activity.getString(R.string.osn_bug_name) + ": " + ((OpenStreetNote)o).getCommentDescription(); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof OpenStreetNote){
			OpenStreetNote bug = (OpenStreetNote) o;
			String name = bug.description != null ? bug.description : "";
			String typeName = bug.typeName != null ? bug.typeName : activity.getString(R.string.osn_bug_name);
			return new PointDescription(PointDescription.POINT_TYPE_OSM_NOTE, typeName, name);
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
			case DIALOG_BUG:
				return createBugDialog(args);
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case DIALOG_BUG:
//				((EditText)dialog.findViewById(R.id.messageEditText)).setText("");
				break;
		}
	}
	
	public static class OpenStreetNote implements Serializable {
		private boolean local;
		private static final long serialVersionUID = -7848941747811172615L;
		private double latitude;
		private double longitude;
		private String description;
		private String typeName;
		private List<String> dates = new ArrayList<String>();
		private List<String> comments = new ArrayList<String>();
		private List<String> users = new ArrayList<String>();
		private long id;
		private boolean opened;

		private void acquireDescriptionAndType() {
			if (comments.size() > 0) {
				StringBuilder sb = new StringBuilder();
				if (dates.size() > 0) {
					sb.append(dates.get(0)).append(" ");
				}
				if (users.size() > 0) {
					sb.append(users.get(0));
				}
				description = comments.get(0);
				typeName = sb.toString();
			}
			if (description != null && description.length() < 100) {
				if (comments.size() > 0) {
					comments.remove(0);
				}
				if (dates.size() > 0) {
					dates.remove(0);
				}
				if (users.size() > 0) {
					users.remove(0);
				}
			}
		}

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

		public String getDescription() {
			return description;
		}

		public String getTypeName() {
			return typeName;
		}

		public String getCommentDescription() {
			StringBuilder sb = new StringBuilder();
			for (String s : getCommentDescriptionList()) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(s);
			}
			return sb.toString();
		}

		public List<String> getCommentDescriptionList() {
			List<String> res = new ArrayList<>(comments.size());
			for (int i = 0; i < comments.size(); i++) {
				StringBuilder sb = new StringBuilder();
				boolean needLineFeed = false;
				if (i < dates.size()) {
					sb.append(dates.get(i)).append(" ");
					needLineFeed = true;
				}
				if (i < users.size()) {
					sb.append(users.get(i)).append(":");
					needLineFeed = true;
				}
				if (needLineFeed) {
					sb.append("\n");
				}
				sb.append(comments.get(i));
				res.add(sb.toString());
			}
			return res;
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
