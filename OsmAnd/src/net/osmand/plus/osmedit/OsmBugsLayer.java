package net.osmand.plus.osmedit;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.osmedit.dialogs.BugBottomSheetDialog;
import net.osmand.plus.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

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

import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;
import static net.osmand.plus.osmedit.OsmBugsUtil.*;

public class OsmBugsLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final Log log = PlatformUtil.getLog(OsmBugsLayer.class);
	private final OsmEditingPlugin plugin;

	private OsmandMapTileView view;

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
		OsmandSettings settings = activity.getMyApplication().getSettings();
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
		OsmandApplication app = activity.getMyApplication();
		startZoom = app.getSettings().SHOW_OSM_BUGS_MIN_ZOOM.get();
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			data.queryNewData(tileBox);
			List<OpenStreetNote> objects = data.getResults();

			if (objects != null) {
				float textScale = app.getSettings().TEXT_SCALE.get();
				float iconSize = getIconSize(app);
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				List<OpenStreetNote> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				boolean showClosed = app.getSettings().SHOW_CLOSED_OSM_BUGS.get();
				for (OpenStreetNote o : objects) {
					if (!o.isOpened() && !showClosed) {
						continue;
					}
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						int backgroundColorRes;
						if (o.isOpened()) {
							backgroundColorRes = R.color.osm_bug_unresolved_icon_color;
						} else {
							backgroundColorRes = R.color.osm_bug_resolved_icon_color;
						}
						PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(activity,
								ContextCompat.getColor(activity, backgroundColorRes), true,
								false, DEFAULT_UI_ICON_ID, BackgroundType.COMMENT);
						pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
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
					int iconId;
					int backgroundColorRes;
					if (o.isOpened()) {
						iconId = R.drawable.mx_special_symbol_remove;
						backgroundColorRes = R.color.osm_bug_unresolved_icon_color;
					} else {
						iconId = R.drawable.mx_special_symbol_check_mark;
						backgroundColorRes = R.color.osm_bug_resolved_icon_color;
					}
					BackgroundType backgroundType = BackgroundType.COMMENT;
					PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(activity,
							ContextCompat.getColor(activity, backgroundColorRes), true, false, iconId,
							backgroundType);
					int offsetY = backgroundType.getOffsetY(activity, textScale);
					pointImageDrawable.drawPoint(canvas, x, y - offsetY, textScale, false);
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
			final int rad = getScaledTouchRadius(activity.getMyApplication(), getRadiusBug(tb));
			int radius = rad * 3;
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

		String SITE_API = activity.getMyApplication().getSettings().getOsmUrl();

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
						if (current != null) {
							current.comments.add(commentIndex, new Comment());
						}
					} else if (parser.getName().equals("user") && current != null) {
						current.comments.get(commentIndex).user = readText(parser, "user");
					} else if (parser.getName().equals("date") && current != null) {
						current.comments.get(commentIndex).date = readText(parser, "date");
					} else if (parser.getName().equals("text") && current != null) {
						current.comments.get(commentIndex).text = readText(parser, "text");
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

	public void openBug(final double latitude, final double longitude, String message, boolean autofill) {
		OpenStreetNote bug = new OpenStreetNote();
		bug.setLatitude(latitude);
		bug.setLongitude(longitude);
		if (autofill) {
			executeTaskInBackground(new HandleOsmNoteAsyncTask(getOsmbugsUtil(bug), local, bug, null, message,
					Action.CREATE, getHandleBugListener()));
		} else {
			showBugDialog(bug, Action.CREATE, message);
		}
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
		createBugDialog(true, text, R.string.context_menu_item_modify_note, R.string.osn_modify_dialog_title,
				null, null, point);
	}

	private void showBugDialog(final OpenStreetNote bug, final Action action, String text) {
		int posButtonTextId;
		int titleTextId;
		if (action == Action.DELETE) {
			posButtonTextId = R.string.osn_close_dialog_title;
			titleTextId = R.string.osm_edit_close_note;
		} else if (action == Action.MODIFY) {
			posButtonTextId = R.string.osn_comment_dialog_title;
			titleTextId = R.string.osm_edit_comment_note;
		} else if (action == Action.REOPEN) {
			posButtonTextId = R.string.osn_reopen_dialog_title;
			titleTextId = R.string.osn_reopen_dialog_title;
		} else {
			posButtonTextId = R.string.osn_add_dialog_title;
			titleTextId = R.string.context_menu_item_open_note;
		}

		OsmBugsUtil util = getOsmbugsUtil(bug);
		final boolean offline = util instanceof OsmBugsLocalUtil;

		createBugDialog(offline, text, titleTextId, posButtonTextId, action, bug, null);
	}

	private void createBugDialog(final boolean offline, String text, int titleTextId, int posButtonTextId,
	                             final Action action, final OpenStreetNote bug, OsmNotesPoint point) {
		if (offline) {
			activity.getContextMenu().close();
			BugBottomSheetDialog.showInstance(activity.getSupportFragmentManager(), getOsmbugsUtil(bug), local, text,
					titleTextId, posButtonTextId, action, bug, point, getHandleBugListener());
		} else {
			OsmNotesPoint notesPoint = new OsmNotesPoint();
			notesPoint.setAction(action);
			notesPoint.setId(bug.getId());
			notesPoint.setLatitude(bug.getLatitude());
			notesPoint.setLongitude(bug.getLongitude());
			SendOsmNoteBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), new OsmPoint[]{notesPoint});
		}
	}

	HandleOsmNoteAsyncTask.HandleBugListener getHandleBugListener() {
		return new HandleOsmNoteAsyncTask.HandleBugListener() {
			@Override
			public void onOsmBugHandled(OsmBugResult obj, Action action, OpenStreetNote bug,
			                            OsmNotesPoint point, String text) {
				if (activity == null || activity.isFinishing()) {
					return;
				}
				if (obj != null && obj.warning == null) {
					if (local == getOsmbugsUtil(bug)) {
						Toast.makeText(activity, R.string.osm_changes_added_to_local_edits, Toast.LENGTH_LONG).show();
						if (obj.local != null) {
							PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_OSM_BUG, obj.local.getText());
							activity.getContextMenu().show(new LatLon(obj.local.getLatitude(), obj.local.getLongitude()),
									pd, obj.local);
							activity.getMapLayers().getContextMenuLayer().updateContextMenu();
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
						openBug(bug.getLatitude(), bug.getLongitude(), text, false);
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
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
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
		private List<Comment> comments = new ArrayList<>();
		private long id;
		private boolean opened;

		private void acquireDescriptionAndType() {
			if (comments.size() > 0) {
				Comment comment = comments.get(0);
				description = comment.text;
				typeName = comment.date + " " + comment.user;
				if (description != null && description.length() < 100) {
					comments.remove(comment);
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
				Comment comment = comments.get(i);
				if (!comment.date.isEmpty()) {
					sb.append(comment.date).append(" ");
					needLineFeed = true;
				}
				if (!comment.user.isEmpty()) {
					sb.append(comment.user).append(":");
					needLineFeed = true;
				}
				if (needLineFeed) {
					sb.append("\n");
				}
				sb.append(comment.text);
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

	class Comment implements Serializable {

		private String date = "";
		private String text = "";
		private String user = "";

		public String getDate() {
			return date;
		}

		public String getText() {
			return text;
		}

		public String getUser() {
			return user;
		}
	}
}
