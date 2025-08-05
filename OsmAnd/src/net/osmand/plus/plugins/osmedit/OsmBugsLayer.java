package net.osmand.plus.plugins.osmedit;

import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.Pair;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.osmedit.asynctasks.HandleOsmNoteAsyncTask;
import net.osmand.plus.plugins.osmedit.asynctasks.HandleOsmNoteAsyncTask.HandleBugListener;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.dialogs.BugBottomSheetDialog;
import net.osmand.plus.plugins.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsUtil;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.OsmBugsTileProvider;
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

	private final Context ctx;
	private final OsmBugsLocalUtil local;
	private MapLayerData<List<OpenStreetNote>> data;

	private int startZoom;

	//OpenGL
	private OsmBugsTileProvider osmBugsTileProvider;
	private float textScale = 1f;
	private boolean showClosed;

	public OsmBugsLayer(@NonNull Context context, @NonNull OsmEditingPlugin plugin) {
		super(context);
		this.ctx = context;
		this.plugin = plugin;
		local = plugin.getOsmNotesLocalUtil();
	}

	public OsmBugsUtil getOsmBugsUtil(OpenStreetNote bug) {
		OsmandSettings settings = getApplication().getSettings();
		if ((bug != null && bug.isLocal()) || plugin.OFFLINE_EDITION.get()
				|| !settings.isInternetConnectionAvailable(true)) {
			return local;
		} else {
			return plugin.getOsmNotesRemoteUtil();
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		data = new OsmandMapLayer.MapLayerData<>() {

			{
				ZOOM_THRESHOLD = 1;
			}

			@Override
			protected Pair<List<OpenStreetNote>, List<OpenStreetNote>> calculateResult(@NonNull QuadRect bounds, int zoom) {
				List<OpenStreetNote> notes = loadingBugs(bounds.top, bounds.left, bounds.bottom, bounds.right);
				return new Pair<>(notes, notes);
			}
		};
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearOsmBugsTileProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		OsmandApplication app = getApplication();
		startZoom = plugin.SHOW_OSM_BUGS_MIN_ZOOM.get();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (tileBox.getZoom() >= startZoom) {
				float textScale = app.getOsmandMap().getTextScale();
				boolean textScaleChanged = this.textScale != textScale;
				boolean showClosed = plugin.SHOW_CLOSED_OSM_BUGS.get();
				this.textScale = textScale;
				if (osmBugsTileProvider == null || textScaleChanged || mapActivityInvalidated || this.showClosed != showClosed) {
					clearOsmBugsTileProvider();
					osmBugsTileProvider = new OsmBugsTileProvider(ctx, data, getPointsOrder(), showClosed, startZoom, textScale);
					osmBugsTileProvider.drawSymbols(mapRenderer);
					this.showClosed = showClosed;
				}
			} else {
				clearOsmBugsTileProvider();
			}
			mapActivityInvalidated = false;
			return;
		}

		if (tileBox.getZoom() >= startZoom) {
			// request to load
			data.queryNewData(tileBox);
			List<OpenStreetNote> objects = data.getResults();

			if (objects != null) {
				float textScale = getTextScale();
				float iconSize = getIconSize(app);
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				List<OpenStreetNote> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				boolean showClosed = plugin.SHOW_CLOSED_OSM_BUGS.get();
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
						PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
								ContextCompat.getColor(ctx, backgroundColorRes), true,
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

					PointImageDrawable pointImageDrawable = createOsmBugDrawable(o.isOpened());
					int offsetY = pointImageDrawable.getBackgroundType().getOffsetY(ctx, textScale);
					pointImageDrawable.drawPoint(canvas, x, y - offsetY, textScale, false);
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
	}

	@NonNull
	public PointImageDrawable createOsmBugDrawable(boolean opened) {
		int iconId;
		int backgroundColorRes;
		if (opened) {
			iconId = R.drawable.mx_special_symbol_remove;
			backgroundColorRes = R.color.osm_bug_unresolved_icon_color;
		} else {
			iconId = R.drawable.mx_special_symbol_check_mark;
			backgroundColorRes = R.color.osm_bug_resolved_icon_color;
		}
		int color = ContextCompat.getColor(ctx, backgroundColorRes);
		BackgroundType backgroundType = BackgroundType.COMMENT;
		return PointImageUtils.getOrCreate(ctx, color, true, false, iconId, backgroundType);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	public int getRadiusBug(RotatedTileBox tb) {
		int z;
		double zoom = tb.getZoom();
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
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	public void collectBugsFromPoint(@NonNull MapSelectionResult result) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		List<OpenStreetNote> objects = data.getResults();

		if (view != null && !Algorithms.isEmpty(objects) && tileBox.getZoom() >= startZoom) {
			MapRendererView mapRenderer = getMapRenderer();
			float radius = getScaledTouchRadius(getApplication(), getRadiusBug(tileBox)) * TOUCH_RADIUS_MULTIPLIER;
			QuadRect screenArea = new QuadRect(
					point.x - radius,
					point.y - radius / 3f,
					point.x + radius,
					point.y + radius * 2f
			);
			List<PointI> touchPolygon31 = null;
			if (mapRenderer != null) {
				touchPolygon31 = NativeUtilities.getPolygon31FromScreenArea(mapRenderer, screenArea);
				if (touchPolygon31 == null) {
					return;
				}
			}

			boolean showClosed = plugin.SHOW_CLOSED_OSM_BUGS.get();
			try {
				for (int i = 0; i < objects.size(); i++) {
					OpenStreetNote note = objects.get(i);
					if (!note.isOpened() && !showClosed) {
						continue;
					}

					double lat = note.getLatitude();
					double lon = note.getLongitude();

					boolean add = mapRenderer != null
							? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
							: tileBox.isLatLonInsidePixelArea(lat, lon, screenArea);
					if (add) {
						result.collect(note, this);
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
		StringBuilder text = new StringBuilder();
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG && parser.getName().equals(key)) {
				break;
			} else if (tok == XmlPullParser.TEXT) {
				text.append(parser.getText());
			}

		}
		return text.toString();
	}

	protected List<OpenStreetNote> loadingBugs(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {

		String SITE_API = plugin.getOsmUrl();

		List<OpenStreetNote> bugs = new ArrayList<>();
		StringBuilder b = new StringBuilder();
		b.append(SITE_API).append("api/0.6/notes?bbox=");
		b.append(leftLongitude);
		b.append(",").append(bottomLatitude);
		b.append(",").append(rightLongitude);
		b.append(",").append(topLatitude);
		try {
			log.info("Loading bugs " + b);
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
			log.warn("Error loading bugs", e);
		}
		return bugs;
	}

	public void openBug(@NonNull MapActivity mapActivity, double latitude, double longitude, String message, boolean autofill) {
		OpenStreetNote bug = new OpenStreetNote();
		bug.setLatitude(latitude);
		bug.setLongitude(longitude);
		if (autofill) {
			OsmAndTaskManager.executeTask(new HandleOsmNoteAsyncTask(getOsmBugsUtil(bug), local, bug, null, message,
					Action.CREATE, getHandleBugListener(mapActivity)));
		} else {
			showBugDialog(mapActivity, bug, Action.CREATE, message);
		}
	}

	public void closeBug(@NonNull MapActivity mapActivity, OpenStreetNote bug, String txt) {
		showBugDialog(mapActivity, bug, Action.DELETE, txt);
	}

	public void reopenBug(@NonNull MapActivity mapActivity, OpenStreetNote bug, String txt) {
		showBugDialog(mapActivity, bug, Action.REOPEN, txt);
	}

	public void commentBug(@NonNull MapActivity mapActivity, OpenStreetNote bug, String txt) {
		showBugDialog(mapActivity, bug, Action.MODIFY, txt);
	}

	public void modifyBug(@NonNull MapActivity mapActivity, OsmNotesPoint point) {
		showBugDialog(mapActivity, point);
	}

	private void showBugDialog(@NonNull MapActivity mapActivity, OsmNotesPoint point) {
		String text = point.getText();
		createBugDialog(mapActivity, true, text, R.string.context_menu_item_modify_note, R.string.osn_modify_dialog_title,
				null, null, point);
	}

	private void showBugDialog(@NonNull MapActivity mapActivity, OpenStreetNote bug, Action action, String text) {
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

		OsmBugsUtil util = getOsmBugsUtil(bug);
		boolean offline = util instanceof OsmBugsLocalUtil;

		createBugDialog(mapActivity, offline, text, titleTextId, posButtonTextId, action, bug, null);
	}

	private void createBugDialog(@NonNull MapActivity mapActivity, boolean offline, String text,
								 int titleTextId, int posButtonTextId, Action action,
								 OpenStreetNote bug, OsmNotesPoint point) {
		if (mapActivity.isFinishing()) {
			return;
		}
		if (offline) {
			mapActivity.getContextMenu().close();
			BugBottomSheetDialog.showInstance(mapActivity.getSupportFragmentManager(), getOsmBugsUtil(bug), local, text,
					titleTextId, posButtonTextId, action, bug, point, getHandleBugListener(mapActivity));
		} else {
			OsmNotesPoint notesPoint = new OsmNotesPoint();
			notesPoint.setAction(action);
			notesPoint.setId(bug.getId());
			notesPoint.setLatitude(bug.getLatitude());
			notesPoint.setLongitude(bug.getLongitude());
			SendOsmNoteBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager(), new OsmPoint[]{notesPoint});
		}
	}

	HandleBugListener getHandleBugListener(@NonNull MapActivity mapActivity) {
		return (obj, action, bug, point, text) -> {
			if (mapActivity.isFinishing()) {
				return;
			}
			OsmandApplication app = getApplication();
			if (obj != null && obj.warning == null) {
				if (local == getOsmBugsUtil(bug)) {
					app.showToastMessage(R.string.osm_changes_added_to_local_edits);
					if (obj.local != null) {
						PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_OSM_BUG, obj.local.getText());
						mapActivity.getContextMenu().show(new LatLon(obj.local.getLatitude(), obj.local.getLongitude()),
								pd, obj.local);
						mapActivity.getMapLayers().getContextMenuLayer().updateContextMenu();
					}
				} else {
					if (action == Action.REOPEN) {
						app.showToastMessage(R.string.osn_add_dialog_success);
					} else if (action == Action.MODIFY) {
						app.showToastMessage(R.string.osb_comment_dialog_success);
					} else if (action == Action.DELETE) {
						app.showToastMessage(R.string.osn_close_dialog_success);
					} else if (action == Action.CREATE) {
						app.showToastMessage(R.string.osn_add_dialog_success);
					}
				}
				clearCache();
			} else {
				int r = R.string.osb_comment_dialog_error;
				if (action == Action.REOPEN) {
					r = R.string.osn_add_dialog_error;
					reopenBug(mapActivity, bug, text);
				} else if (action == Action.DELETE) {
					r = R.string.osn_close_dialog_error;
					closeBug(mapActivity, bug, text);
				} else if (action == Action.CREATE) {
					r = R.string.osn_add_dialog_error;
					openBug(mapActivity, bug.getLatitude(), bug.getLongitude(), text, false);
				} else if (action == null) {
					r = R.string.osn_modify_dialog_error;
					modifyBug(mapActivity, point);
				} else {
					commentBug(mapActivity, bug, text);
				}
				app.showToastMessage(ctx.getResources().getString(r) + "\n" + obj);
			}
		};
	}

	private String getMessageText(View view) {
		return ((EditText) view.findViewById(R.id.message_field)).getText().toString();
	}

	public void refreshMap() {
		if (view != null && view.getLayers().contains(this)) {
			view.refreshMap();
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof OpenStreetNote bug) {
			String name = bug.description != null ? bug.description : "";
			String typeName = bug.typeName != null ? bug.typeName : ctx.getString(R.string.osn_bug_name);
			return new PointDescription(PointDescription.POINT_TYPE_OSM_NOTE, typeName, name);
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (result.getTileBox().getZoom() >= startZoom) {
			collectBugsFromPoint(result);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OpenStreetNote) {
			return new LatLon(((OpenStreetNote) o).getLatitude(), ((OpenStreetNote) o).getLongitude());
		}
		return null;
	}

	private void clearOsmBugsTileProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && osmBugsTileProvider != null) {
			osmBugsTileProvider.clearSymbols(mapRenderer);
			osmBugsTileProvider = null;
		}
	}

	public static class OpenStreetNote implements Serializable {
		private boolean local;
		private static final long serialVersionUID = -7848941747811172615L;
		private double latitude;
		private double longitude;
		private String description;
		private String typeName;
		private final List<Comment> comments = new ArrayList<>();
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

	static class Comment implements Serializable {

		private static final long serialVersionUID = -5959877367366644911L;

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
