package net.osmand.plus.osmedit;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmBugsUtil.OsmBugResult;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class OsmBugsLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final Log log = PlatformUtil.getLog(OsmBugsLayer.class);
	private final OsmEditingPlugin plugin;

	private OsmandMapTileView view;

	private Paint paintIcon;
	private Bitmap unresolvedNote;
	private Bitmap resolvedNote;
	private Bitmap unresolvedNoteSmall;
	private Bitmap resolvedNoteSmall;

	private final MapActivity activity;
	private OsmBugsLocalUtil local;
	private MapLayerData<List<OpenStreetNote>> data;

	private int startZoom;

	public OsmBugsLayer(MapActivity activity, OsmEditingPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
		local = plugin.getOsmNotesLocalUtil();
	}

	public OsmBugsUtil getOsmbugsUtil(OpenStreetNote bug) {
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		if ((bug != null && bug.isLocal()) || settings.OFFLINE_EDITION.get()
				|| !settings.isInternetConnectionAvailable(true)) {
			return local;
		} else {
			return plugin.getOsmNotesRemoteUtil();
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
		startZoom = activity.getMyApplication().getSettings().SHOW_OSM_BUGS_MIN_ZOOM.get();
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			data.queryNewData(tileBox);
			List<OpenStreetNote> objects = data.getResults();

			if (objects != null) {
				float iconSize = resolvedNote.getWidth() * 3 / 2.5f;
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				List<OpenStreetNote> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				boolean showClosed = activity.getMyApplication().getSettings().SHOW_CLOSED_OSM_BUGS.get();
				for (OpenStreetNote o : objects) {
					if (!o.isOpened() && !showClosed) {
						continue;
					}
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
						smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					} else {
						fullObjects.add(o);
						fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					}
				}
				for (OpenStreetNote o : fullObjects) {
					if (!o.isOpened() && !showClosed) {
						continue;
					}
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
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
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

	public void getBugFromPoint(RotatedTileBox tb, PointF point, List<? super OpenStreetNote> res) {
		List<OpenStreetNote> objects = data.getResults();
		if (objects != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rad = getRadiusBug(tb);
			int radius = rad * 3 / 2;
			int small = rad * 3 / 4;
			boolean showClosed = activity.getMyApplication().getSettings().SHOW_CLOSED_OSM_BUGS.get();
			try {
				for (int i = 0; i < objects.size(); i++) {
					OpenStreetNote n = objects.get(i);
					if (!n.isOpened() && !showClosed) {
						continue;
					}
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
		if (data != null) {
			data.clearCache();
		}
	}

	private static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
		int tok;
		String text = "";
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG && parser.getName().equals(key)) {
				break;
			} else if (tok == XmlPullParser.TEXT) {
				text += parser.getText();
			}

		}
		return text;
	}


	protected List<OpenStreetNote> loadingBugs(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;

		String SITE_API;

		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			SITE_API = "https://api.openstreetmap.org/";
		} else {
			SITE_API = "http://api.openstreetmap.org/";
		}

		List<OpenStreetNote> bugs = new ArrayList<>();
		StringBuilder b = new StringBuilder();
		b.append(SITE_API).append("api/0.6/notes?bbox="); //$NON-NLS-1$
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
						commentIndex++;
					} else if (parser.getName().equals("user") && current != null) {
						if (commentIndex == current.users.size()) {
							current.users.add(readText(parser, "user"));
						}
					} else if (parser.getName().equals("date") && current != null) {
						if (commentIndex == current.dates.size()) {
							current.dates.add(readText(parser, "date"));
						}
					} else if (parser.getName().equals("text") && current != null) {
						if (commentIndex == current.comments.size()) {
							current.comments.add(readText(parser, "text"));
						}
					}
				}
			}
			reader.close();
			for (OpenStreetNote note : bugs) {
				note.acquireDescriptionAndType();
			}
		} catch (IOException | RuntimeException | XmlPullParserException e) {
			log.warn("Error loading bugs", e); //$NON-NLS-1$
		}
		return bugs;
	}

	private void asyncActionTask(final OpenStreetNote bug, final OsmNotesPoint point, final String text, final Action action) {
		AsyncTask<Void, Void, OsmBugResult> task = new AsyncTask<Void, Void, OsmBugResult>() {
			private OsmBugsUtil osmbugsUtil;

			@Override
			protected OsmBugResult doInBackground(Void... params) {
				if (bug != null) {
					osmbugsUtil = getOsmbugsUtil(bug);
					OsmNotesPoint pnt = new OsmNotesPoint();
					pnt.setId(bug.getId());
					pnt.setLatitude(bug.getLatitude());
					pnt.setLongitude(bug.getLongitude());
					return osmbugsUtil.commit(pnt, text, action);
				} else if (point != null) {
					osmbugsUtil = local;
					return osmbugsUtil.modify(point, text);
				}
				return null;
			}

			protected void onPostExecute(OsmBugResult obj) {
				if (obj != null && obj.warning == null) {
					if (local == osmbugsUtil) {
						Toast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
						if (obj.local != null) {
							PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_OSM_BUG, obj.local.getText());
							activity.getContextMenu().show(new LatLon(obj.local.getLatitude(), obj.local.getLongitude()), pd, obj.local);
						}
					} else {
						if (action == Action.REOPEN) {
							Toast.makeText(activity, R.string.osn_add_dialog_success, Toast.LENGTH_LONG).show();
						} else if (action == Action.MODIFY) {
							Toast.makeText(activity, R.string.osb_comment_dialog_success, Toast.LENGTH_LONG).show();
						} else if (action == Action.DELETE) {
							Toast.makeText(activity, R.string.osn_close_dialog_success, Toast.LENGTH_LONG).show();
						} else if (action == Action.CREATE) {
							Toast.makeText(activity, R.string.osn_add_dialog_success, Toast.LENGTH_LONG).show();
						}

					}
					clearCache();
				} else {
					int r = R.string.osb_comment_dialog_error;
					if (action == Action.REOPEN) {
						r = R.string.osn_add_dialog_error;
						reopenBug(bug, text);
					} else if (action == Action.DELETE) {
						r = R.string.osn_close_dialog_error;
						closeBug(bug, text);
					} else if (action == Action.CREATE) {
						r = R.string.osn_add_dialog_error;
						openBug(bug.getLatitude(), bug.getLongitude(), text);
					} else if (action == null) {
						r = R.string.osn_modify_dialog_error;
						modifyBug(point);
					} else {
						commentBug(bug, text);
					}
					Toast.makeText(activity, activity.getResources().getString(r) + "\n" + obj, Toast.LENGTH_LONG).show();
				}
			}
		};
		executeTaskInBackground(task);
	}


	public void openBug(final double latitude, final double longitude, String message) {
		OpenStreetNote bug = new OpenStreetNote();
		bug.setLatitude(latitude);
		bug.setLongitude(longitude);
		showBugDialog(bug, Action.CREATE, message);
	}

	public void openBug(final double latitude, final double longitude, String message, boolean autofill) {
		OpenStreetNote bug = new OpenStreetNote();
		bug.setLatitude(latitude);
		bug.setLongitude(longitude);

		if (autofill) asyncActionTask(bug, null, message, Action.CREATE);
		else showBugDialog(bug, Action.CREATE, message);
	}

	public void closeBug(final OpenStreetNote bug, String txt) {
		showBugDialog(bug, Action.DELETE, txt);
	}

	public void reopenBug(final OpenStreetNote bug, String txt) {
		showBugDialog(bug, Action.REOPEN, txt);
	}

	public void commentBug(final OpenStreetNote bug, String txt) {
		showBugDialog(bug, Action.MODIFY, txt);
	}

	public void modifyBug(final OsmNotesPoint point) {
		showBugDialog(point);
	}

	private void showBugDialog(final OsmNotesPoint point) {
		String text = point.getText();
		createBugDialog(true, text, R.string.osn_modify_dialog_title, null, null, point);
	}

	private void showBugDialog(final OpenStreetNote bug, final Action action, String text) {
		int title;
		if (action == Action.DELETE) {
			title = R.string.osn_close_dialog_title;
		} else if (action == Action.MODIFY) {
			title = R.string.osn_comment_dialog_title;
		} else if (action == Action.REOPEN) {
			title = R.string.osn_reopen_dialog_title;
		} else {
			title = R.string.osn_add_dialog_title;
		}

		OsmBugsUtil util = getOsmbugsUtil(bug);
		final boolean offline = util instanceof OsmBugsLocalUtil;

		createBugDialog(offline, text, title, action, bug, null);
	}

	private void createBugDialog(final boolean offline, String text, int posButtonTitleRes, final Action action, final OpenStreetNote bug, final OsmNotesPoint point) {
		@SuppressLint("InflateParams")
		final View view = LayoutInflater.from(activity).inflate(R.layout.open_bug, null);
		if (offline) {
			view.findViewById(R.id.user_name_field).setVisibility(View.GONE);
			view.findViewById(R.id.userNameEditTextLabel).setVisibility(View.GONE);
			view.findViewById(R.id.password_field).setVisibility(View.GONE);
			view.findViewById(R.id.passwordEditTextLabel).setVisibility(View.GONE);
		} else {
			((EditText) view.findViewById(R.id.user_name_field)).setText(getUserName());
			((EditText) view.findViewById(R.id.password_field)).setText(((OsmandApplication) activity.getApplication()).getSettings().USER_PASSWORD.get());
		}
		if (!Algorithms.isEmpty(text)) {
			((EditText) view.findViewById(R.id.message_field)).setText(text);
		}
		AndroidUtils.softKeyboardDelayed(view.findViewById(R.id.message_field));

		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.shared_string_commit);
		builder.setView(view);
		builder.setPositiveButton(posButtonTitleRes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = offline ? getMessageText(view) : getTextAndUpdateUserPwd(view);
				activity.getContextMenu().close();
				if (bug != null) {
					asyncActionTask(bug, null, text, action);
				} else if (point != null) {
					asyncActionTask(null, point, text, null);
				}
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
	}

	private String getUserName() {
		return ((OsmandApplication) activity.getApplication()).getSettings().USER_NAME.get();
	}

	private String getTextAndUpdateUserPwd(final View view) {
		String text = getMessageText(view);
		String author = ((EditText) view.findViewById(R.id.user_name_field)).getText().toString();
		String pwd = ((EditText) view.findViewById(R.id.password_field)).getText().toString();
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_NAME.set(author);
		((OsmandApplication) OsmBugsLayer.this.activity.getApplication()).getSettings().USER_PASSWORD.set(pwd);
		return text;
	}

	private String getMessageText(final View view) {
		return ((EditText) view.findViewById(R.id.message_field)).getText().toString();
	}

	public void refreshMap() {
		if (view != null && view.getLayers().contains(OsmBugsLayer.this)) {
			view.refreshMap();
		}
	}



	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof OpenStreetNote) {
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
	public boolean isObjectClickable(Object o) {
		return o instanceof OpenStreetNote;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if (tileBox.getZoom() >= startZoom) {
			getBugFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OpenStreetNote) {
			return new LatLon(((OpenStreetNote) o).getLatitude(), ((OpenStreetNote) o).getLongitude());
		}
		return null;
	}

	public static class OpenStreetNote implements Serializable {
		private boolean local;
		private static final long serialVersionUID = -7848941747811172615L;
		private double latitude;
		private double longitude;
		private String description;
		private String typeName;
		private List<String> dates = new ArrayList<>();
		private List<String> comments = new ArrayList<>();
		private List<String> users = new ArrayList<>();
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
