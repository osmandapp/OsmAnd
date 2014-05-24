/**
 *
 */
package net.osmand.plus.osmo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListActivity;
import net.osmand.plus.osmo.OsMoGroups.OsMoGroup;
import net.osmand.plus.osmo.OsMoGroups.OsMoUser;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.plus.osmo.OsMoTracker.OsmoTrackerListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.view.ActionMode.Callback;

/**
 *
 */
public class OsMoGroupsActivity extends OsmandExpandableListActivity implements OsmAndCompassListener,
		OsmAndLocationListener, OsmoTrackerListener {

	public static final int CONNECT_TO_DEVICE = 1;
	protected static final int DELETE_ACTION_ID = 2;
	public static final int CREATE_GROUP = 3;
	public static final int JOIN_GROUP = 4;
	private static final int LIST_REFRESH_MSG_ID = OsmAndConstants.UI_HANDLER_SEARCH + 30;
	private static final long RECENT_THRESHOLD = 60000;

	private OsMoPlugin osMoPlugin;
	private OsMoGroupsAdapter adapter;
	private LatLon mapLocation;
	private OsmandApplication app;
	private Handler uiHandler;

	private float width = 24;
	private float height = 24;
	private Path directionPath = new Path();
	private Location lastLocation;
	private float lastCompass;
	private ActionMode actionMode;
	private boolean selectionMode;
	private Set<OsMoUser> selectedObjects = new HashSet<OsMoGroups.OsMoUser>();

	@Override
	public void onCreate(Bundle icicle) {
		// This has to be called before setContentView and you must use the
		// class in com.actionbarsherlock.view and NOT android.view
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		super.onCreate(icicle);
		setContentView(R.layout.osmo_groups_list);
		getSupportActionBar().setTitle(R.string.osmo_activity);
		setSupportProgressBarIndeterminateVisibility(false);
		// getSupportActionBar().setIcon(R.drawable.tab_search_favorites_icon);

		osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		adapter = new OsMoGroupsAdapter(osMoPlugin.getGroups(), osMoPlugin.getTracker());
		getExpandableListView().setAdapter(adapter);
		app = (OsmandApplication) getApplication();
		
		uiHandler = new Handler();
		directionPath = createDirectionPath();
		CompoundButton trackr = (CompoundButton) findViewById(R.id.enable_tracker);
		trackr.setChecked(osMoPlugin.getTracker().isEnabledTracker());
		trackr.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					osMoPlugin.getTracker().enableTracker();
				} else {
					osMoPlugin.getTracker().disableTracker();
				}
			}
		});
		TextView mtd = (TextView) findViewById(R.id.motd);
		SessionInfo si = osMoPlugin.getService().getCurrentSessionInfo();
		boolean visible = si != null && si.motd != null && si.motd.length() > 0;
		mtd.setVisibility(visible? View.VISIBLE:View.GONE);
		if(visible) {
			mtd.setText(si.motd);
		}
	}

	private Path createDirectionPath() {
		int h = 15;
		int w = 4;
		float sarrowL = 8; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float hpartArrowL = (float) (harrowL - w) / 2;
		Path path = new Path();
		path.moveTo(width / 2, height - (height - h) / 3);
		path.rMoveTo(w / 2, 0);
		path.rLineTo(0, -h);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(-harrowL / 2, -harrowL / 2); // center
		path.rLineTo(-harrowL / 2, harrowL / 2);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(0, h);

		Matrix pathTransform = new Matrix();
		WindowManager mgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		pathTransform.postScale(dm.density, dm.density);
		path.transform(pathTransform);
		width *= dm.density;
		height *= dm.density;
		return path;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().registerOrUnregisterCompassListener(true);
		app.getLocationProvider().addLocationListener(this);
		app.getLocationProvider().resumeAllUpdates();
		osMoPlugin.getTracker().setTrackerListener(this);
		adapter.synchronizeGroups();
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.getLocationProvider().pauseAllUpdates();
		app.getLocationProvider().removeCompassListener(this);
		app.getLocationProvider().removeLocationListener(this);
		osMoPlugin.getTracker().setTrackerListener(null);
	}
	
	private void enterSelectionMode() {
		actionMode = startActionMode(new Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				createMenuItem(menu, DELETE_ACTION_ID, R.string.default_buttons_delete, R.drawable.ic_action_delete_light, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectionMode = false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if(item.getItemId() == DELETE_ACTION_ID) {
				}
				return true;
			}
		});

	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (!selectionMode) {
			enterSelectionMode();
		}
		OsMoUser user = adapter.getChild(groupPosition, childPosition);
		selectedObjects.add(user);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == CONNECT_TO_DEVICE) {
			connectToDevice();
			return true;
		} else if (item.getItemId() == JOIN_GROUP) {
			// shareFavourites();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void connectToDevice() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.osmo_connect_to_device);
		final View v = getLayoutInflater().inflate(R.layout.osmo_connect_to_device, getExpandableListView(), false);
		final EditText tracker = (EditText) v.findViewById(R.id.TrackerId);
		final EditText name = (EditText) v.findViewById(R.id.Name);
		builder.setView(v);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String nameUser = name.getText().toString();
				adapter.registerUser(tracker.getText().toString(), nameUser);
			}
		});
		builder.create().show();
		tracker.requestFocus();
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		createMenuItem(menu, CONNECT_TO_DEVICE, R.string.osmo_new_device, R.drawable.ic_action_marker_light,
				R.drawable.ic_action_marker_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		createMenuItem(menu, JOIN_GROUP, R.string.osmo_join_group, R.drawable.ic_action_markers_light,
				R.drawable.ic_action_markers_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public void showProgressBar() {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

	class OsMoGroupsAdapter extends OsmandBaseExpandableListAdapter {

		private List<OsMoGroup> sortedGroups = new ArrayList<OsMoGroups.OsMoGroup>();
		private Map<OsMoGroup, List<OsMoUser>> users = new LinkedHashMap<OsMoGroups.OsMoGroup, List<OsMoUser>>();
		private OsMoGroups grs;
		private OsMoTracker tracker;

		public OsMoGroupsAdapter(OsMoGroups grs, OsMoTracker tracker) {
			this.grs = grs;
			this.tracker = tracker;
			synchronizeGroups();
		}

		public void registerUser(String trackerId, String nameUser) {
			addUser(grs.registerUser(trackerId, nameUser));
		}

		public void sort(Comparator<OsMoUser> comparator) {
			for (List<OsMoUser> ps : users.values()) {
				Collections.sort(ps, comparator);
			}
		}

		public void addUser(OsMoUser p) {
			if (!users.containsKey(p.getGroup())) {
				users.put(p.getGroup(), new ArrayList<OsMoUser>());
				sortedGroups.add(p.getGroup());
			}
			users.get(p.getGroup()).add(p);
			notifyDataSetChanged();
		}

		public void synchronizeGroups() {
			users.clear();
			sortedGroups.clear();
			final Collator clt = Collator.getInstance();
			for (OsMoGroup key : grs.getGroups().values()) {
				sortedGroups.add(key);
				final ArrayList<OsMoUser> list = new ArrayList<OsMoUser>(key.users.values());
				Collections.sort(list, new Comparator<OsMoGroups.OsMoUser>() {

					@Override
					public int compare(OsMoUser lhs, OsMoUser rhs) {
						return clt.compare(lhs.getVisibleName(), rhs.getVisibleName());
					}
				});
				users.put(key, list);
			}
			Collections.sort(sortedGroups, new Comparator<OsMoGroups.OsMoGroup>() {

				@Override
				public int compare(OsMoGroup lhs, OsMoGroup rhs) {
					if (lhs.isMainGroup()) {
						return 1;
					}
					if (rhs.isMainGroup()) {
						return -1;
					}
					return clt.compare(lhs.getVisibleName(OsMoGroupsActivity.this),
							rhs.getVisibleName(OsMoGroupsActivity.this));
				}
			});
			notifyDataSetChanged();
		}

		@Override
		public OsMoUser getChild(int groupPosition, int childPosition) {
			return users.get(sortedGroups.get(groupPosition)).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return users.get(sortedGroups.get(groupPosition)).size();
		}

		@Override
		public OsMoGroup getGroup(int groupPosition) {
			return sortedGroups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return sortedGroups.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.osmo_group_item, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final OsMoGroup model = getGroup(groupPosition);
			label.setText(model.getVisibleName(OsMoGroupsActivity.this));
			final CompoundButton ch = (CompoundButton) row.findViewById(R.id.check_item);
			ch.setVisibility(View.VISIBLE);
			ch.setChecked(model.active);
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					osMoPlugin.getGroups().joinGroup(model.groupId);
				}
			});
			return row;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.osmo_group_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.osmo_label);
			ImageView icon = (ImageView) row.findViewById(R.id.osmo_user_icon);
			final CompoundButton ch = (CompoundButton) row.findViewById(R.id.check_item);
			ch.setVisibility(View.VISIBLE);

			final OsMoUser model = (OsMoUser) getChild(groupPosition, childPosition);
			row.setTag(model);

			LatLon lnLocation = mapLocation;
			Location location = tracker.getLastLocation(model.trackerId);
			if (location == null || lnLocation == null) {
				icon.setImageResource(R.drawable.list_favorite);
				label.setText(model.getVisibleName());
			} else {
				DirectionDrawable draw = new DirectionDrawable();
				float[] mes = new float[2];
				net.osmand.Location.distanceBetween(location.getLatitude(), location.getLongitude(),
						lnLocation.getLatitude(), lnLocation.getLongitude(), mes);
				draw.setAngle(mes[1] - lastCompass + 180);
				draw.setRecent(Math.abs(location.getTime() - System.currentTimeMillis()) < RECENT_THRESHOLD);
				icon.setImageDrawable(draw);
				icon.setImageResource(R.drawable.list_favorite);
				int dist = (int) mes[0];
				long seconds = Math.max(0, (System.currentTimeMillis() - location.getTime()) / 1000);
				String time = "  (";
				if (seconds < 100) {
					time = seconds + " " + getString(R.string.seconds_ago);
				} else if (seconds / 60 < 100) {
					time = (seconds / 60) + " " + getString(R.string.minutes_ago);
				} else {
					time = (seconds / (60 * 60)) + " " + getString(R.string.hours_ago);
				}
				time += ")";
				String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
				String visibleName = model.getVisibleName();
				String firstPart = distance + visibleName;
				final String fullText = firstPart + time;
				label.setText(fullText, TextView.BufferType.SPANNABLE);
				((Spannable) label.getText()).setSpan(
						new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0,
						distance.length() - 1, 0);
				((Spannable) label.getText()).setSpan(
						new ForegroundColorSpan(getResources().getColor(R.color.color_unknown)), firstPart.length(),
						fullText.length() - 1, 0);
			}

			return row;
		}
	}

	@Override
	public void updateLocation(Location location) {
		lastLocation = location;
		adapter.notifyDataSetInvalidated();
	}

	@Override
	public void updateCompassValue(float value) {
		lastCompass = value;
		refreshList();
	}

	private void refreshList() {
		if (!uiHandler.hasMessages(LIST_REFRESH_MSG_ID)) {
			Message msg = Message.obtain(uiHandler, new Runnable() {
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
			msg.what = LIST_REFRESH_MSG_ID;
			uiHandler.sendMessageDelayed(msg, 100);
		}
	}
	
	@Override
	public void locationChange(String trackerId) {
		refreshList();		
	}

	class DirectionDrawable extends Drawable {
		Paint paintRouteDirection;

		private float angle;

		public DirectionDrawable() {
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			paintRouteDirection.setAntiAlias(true);
		}

		public void setRecent(boolean recent) {
			if (recent) {
				paintRouteDirection.setColor(getResources().getColor(R.color.color_ok));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			}
		}

		public void setAngle(float angle) {
			this.angle = angle;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.rotate(angle, width / 2, height / 2);
			canvas.drawPath(directionPath, paintRouteDirection);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);

		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}
	}

}
