/**
 *
 */
package net.osmand.plus.osmo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListActivity;
import net.osmand.plus.osmo.OsMoGroups.OsMoGroupsUIListener;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoGroup;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.plus.osmo.OsMoTracker.OsmoTrackerListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Typeface;
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
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 *
 */
public class OsMoGroupsActivity extends OsmandExpandableListActivity implements OsmAndCompassListener,
		OsmAndLocationListener, OsmoTrackerListener, OsMoGroupsUIListener {

	public static final int CONNECT_TO = 1;
	protected static final int DELETE_ACTION_ID = 2;
	public static final int CREATE_GROUP = 3;
	protected static final int ON_OFF_ACTION_ID = 4;
	protected static final int SHARE_ID = 5;
	protected static final int SHOW_ON_MAP_ID = 6;
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
	private Object selectedObject = null;
	private String operation;

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
				updateStatus();
			}
		});
		TextView mtd = (TextView) findViewById(R.id.motd);
		SessionInfo si = osMoPlugin.getService().getCurrentSessionInfo();
		boolean visible = si != null && si.motd != null && si.motd.length() > 0;
		mtd.setVisibility(visible? View.VISIBLE:View.GONE);
		if(visible) {
			mtd.setText(si.motd);
		}
		
		updateStatus();
		getExpandableListView().setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				OsMoGroup gr = adapter.getGroup(groupPosition);
				if(gr.isMainGroup()) {
					if(parent.isGroupExpanded(groupPosition)) {
						parent.collapseGroup(groupPosition);
					} else {
						parent.expandGroup(groupPosition);
					}
				} else {
					if(selectedObject == gr) {
						quitSelectionMode();
						if(parent.isGroupExpanded(groupPosition)) {
							parent.collapseGroup(groupPosition);
						} else {
							parent.expandGroup(groupPosition);
						}
					} else {
						enterSelectionMode(gr);
					}
				}
				if(groupPosition == 0) {
					return true;
				}
				return false;
			}
		});
	}

	long lastUpdateTime;
	private Drawable blinkImg;
	private void blink(final ImageView status, Drawable bigger, final Drawable smaller ) {
		blinkImg = smaller;
		status.setImageDrawable(bigger);
		status.invalidate();
		uiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				blinkImg = null;
				status.setImageDrawable(smaller);
				status.invalidate();
			}
		}, 500);
	}

	private void updateStatus() {
		ImageView status = (ImageView) findViewById(R.id.osmo_status);
		final Drawable srcSmall = getResources().getDrawable(R.drawable.mon_osmo_conn_small);
		final Drawable srcSignalSmall = getResources().getDrawable(R.drawable.mon_osmo_conn_signal_small);
		final Drawable srcBig = getResources().getDrawable(R.drawable.mon_osmo_conn_big);
		final Drawable srcSignalBig = getResources().getDrawable(R.drawable.mon_osmo_conn_signal_big);
		final Drawable srcinactive = getResources().getDrawable(R.drawable.monitoring_rec_inactive);
		Drawable small = srcinactive;
		Drawable big = srcinactive;
		OsMoService service = osMoPlugin.getService();
		OsMoTracker tracker = osMoPlugin.getTracker();
		long last = service.getLastCommandTime();
		if (service.isActive()) {
			small = tracker.isEnabledTracker() ? srcSignalSmall : srcSmall;
			big = tracker.isEnabledTracker() ? srcSignalBig : srcBig;
		}
		if (blinkImg != small) {
			status.setImageDrawable(small);
		}
		if (last != lastUpdateTime) {
			lastUpdateTime = last;
			blink(status, big, small);
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
		osMoPlugin.getTracker().setUITrackerListener(this);
		osMoPlugin.getGroups().setUiListener(this);
		adapter.synchronizeGroups();
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.getLocationProvider().pauseAllUpdates();
		app.getLocationProvider().removeCompassListener(this);
		app.getLocationProvider().removeLocationListener(this);
		osMoPlugin.getTracker().setUITrackerListener(null);
		osMoPlugin.getGroups().setUiListener(null);
	}
	
	private void enterSelectionMode(final Object o) {
		if(!checkOperationIsNotRunning()) {
			return;
		}
		actionMode = startActionMode(new Callback() {
			private OsMoDevice device;
			private OsMoGroup group;

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectedObject = o;
				device =  (OsMoDevice) (o instanceof OsMoDevice ?o : null); 
				group =  (OsMoGroup) (o instanceof OsMoGroup ?o : null);
				createMenuItem(menu, DELETE_ACTION_ID, R.string.default_buttons_delete, R.drawable.ic_action_delete_light, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				if(device != null && device.getLastLocation() != null) {
					createMenuItem(menu, SHOW_ON_MAP_ID, R.string.show_poi_on_map, R.drawable.ic_action_marker_light, R.drawable.ic_action_marker_dark,
							MenuItem.SHOW_AS_ACTION_IF_ROOM);
				} else if(group != null) {
					createMenuItem(menu, SHOW_ON_MAP_ID, R.string.show_poi_on_map, R.drawable.ic_action_marker_light, R.drawable.ic_action_marker_dark,
							MenuItem.SHOW_AS_ACTION_IF_ROOM);
				}
				createMenuItem(menu, SHARE_ID, R.string.share_fav, R.drawable.ic_action_gshare_light, R.drawable.ic_action_gshare_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				MenuItem mi = createMenuItem(menu, ON_OFF_ACTION_ID, R.string.default_buttons_ok, 0, 0,
						MenuItem.SHOW_AS_ACTION_ALWAYS);
				
		        final LayoutInflater inflater = LayoutInflater.from(OsMoGroupsActivity.this);
		        View view = inflater.inflate(R.layout.check_item_rel, null);
		        final CompoundButton check = (CompoundButton) view.findViewById(R.id.check_item);
				check.setChecked((device != null && device.isEnabled()) || (group != null && group.isEnabled()));
		        check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						onOffAction(check);						
					}
				});
		        mi.setActionView(view);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectedObject = null;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if(item.getItemId() == DELETE_ACTION_ID ) {
					Builder bld = new AlertDialog.Builder(OsMoGroupsActivity.this);
					String name = (selectedObject instanceof OsMoDevice)? ((OsMoDevice) selectedObject).getVisibleName() :
						((OsMoGroup) selectedObject).getVisibleName(OsMoGroupsActivity.this);
					bld.setTitle(getString(R.string.delete_confirmation_msg, name));
					bld.setPositiveButton(R.string .default_buttons_yes, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Object obj = selectedObject;
							quitSelectionMode();
							deleteObject(obj);
						}
					});
					bld.setNegativeButton(R.string.default_buttons_no, null);
					bld.show();
				} else if(item.getItemId() == SHARE_ID) {
				} else if(item.getItemId() == SHOW_ON_MAP_ID) {
					if(device != null) {
						Location location = device.getLastLocation();
						app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), app.getSettings().getMapZoomToShow(), 
								null, device.getVisibleName(), device); 
						MapActivity.launchMapActivityMoveToTop(OsMoGroupsActivity.this);
					}
				} else if(item.getItemId() == ON_OFF_ACTION_ID) {
					CompoundButton bt = ((CompoundButton) item.getActionView().findViewById(R.id.check_item));
					onOffAction(bt);
				}
				return true;
			}

			private void onOffAction(CompoundButton bt) {
				if((selectedObject instanceof OsMoDevice)) {
					OsMoDevice d = (OsMoDevice) selectedObject;
					if(bt.isChecked()) {
						osMoPlugin.getGroups().connectDevice(d);
					} else {
						osMoPlugin.getGroups().disconnectDevice(d);
					}
				} else {
					OsMoGroup g = (OsMoGroup) selectedObject;
					if(bt.isChecked()) {
						String operation = osMoPlugin.getGroups().connectGroup(g);
						startLongRunningOperation(operation);
					} else {
						String operation = osMoPlugin.getGroups().disconnectGroup(g);
						startLongRunningOperation(operation);
					}	
				}
				quitSelectionMode();
			}
		});

	}

	protected void deleteObject(Object selectedObject) {
		if(selectedObject instanceof OsMoDevice) {
			OsMoDevice d = (OsMoDevice)selectedObject;
			osMoPlugin.getGroups().deleteDevice(d);
			adapter.update(d.getGroup());
			adapter.notifyDataSetChanged();
		} else {
			if (!checkOperationIsNotRunning()) {
				return;
			}
			String operation = osMoPlugin.getGroups().leaveGroup((OsMoGroup) selectedObject);
			startLongRunningOperation(operation);
			adapter.update((OsMoGroup) selectedObject);
			adapter.notifyDataSetChanged();
		}
		
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		OsMoDevice model = adapter.getChild(groupPosition, childPosition);
		if (model != selectedObject) {
			enterSelectionMode(model);
		} else {
			quitSelectionMode();
		}
		return true;
	}

	private void quitSelectionMode() {
		selectedObject = null;
		actionMode.finish();
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == CONNECT_TO) {
			connectToDevice();
			return true;
		} else if (item.getItemId() == CREATE_GROUP) {
			createGroup();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void createGroup() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.osmo_create_group);
		final View v = getLayoutInflater().inflate(R.layout.osmo_create_group, getExpandableListView(), false);
		final EditText policy = (EditText) v.findViewById(R.id.Policy);
		final EditText description = (EditText) v.findViewById(R.id.Description);
		final EditText name = (EditText) v.findViewById(R.id.Name);
		final Spinner expire = (Spinner) v.findViewById(R.id.ExpireGroup);
		final int[] days = new int[]{1,2,3,7,14};
		String[] ds = new String[days.length];
		for(int k = 0; k < days.length; k++) {
			ds[k] = days[k] + " " + getString(R.string.int_days);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ds);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		expire.setAdapter(adapter);
		expire.setSelection(3);
		
		builder.setView(v);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(!checkOperationIsNotRunning()) {
					return ;
				}
				long eTime = days[expire.getSelectedItemPosition()] * 60 * 60 * 24 * 1000 + System.currentTimeMillis();
				String op = osMoPlugin.getGroups().createGroup(name.getText().toString(), eTime,
						description.getText().toString(), policy.getText().toString());
				startLongRunningOperation(op);
			}
		});
		builder.create().show();
		AndroidUtils.softKeyboardDelayed(name);
		
	}

	private void connectToDevice() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.osmo_connect_to_device);
		final View v = getLayoutInflater().inflate(R.layout.osmo_connect_to_device, getExpandableListView(), false);
		final RadioButton device = (RadioButton) v.findViewById(R.id.ConnectToDevice);
		//final RadioButton group = (RadioButton) v.findViewById(R.id.ConnectToGroup);
		final TextView labelTracker = (TextView ) v.findViewById(R.id.LabelTrackerId);
		final TextView labelName = (TextView ) v.findViewById(R.id.LabelName);
		final EditText tracker = (EditText) v.findViewById(R.id.TrackerId);
		final EditText name = (EditText) v.findViewById(R.id.Name);
		device.setChecked(true);
		device.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					labelTracker.setText(R.string.osmo_connect_to_device_tracker_id);
					labelName.setText(R.string.osmo_connect_to_device_name);
				} else {
					labelTracker.setText(R.string.osmo_connect_to_group_id);
					labelName.setText(R.string.osmo_group_name);
				}
			}
		});
		
		builder.setView(v);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String nameUser = name.getText().toString();
				final String id = tracker.getText().toString();
				if(device.isChecked()) {
					OsMoDevice dev = osMoPlugin.getGroups().addConnectedDevice(id, nameUser, 0);
					adapter.update(dev.group);
					adapter.notifyDataSetChanged();
				} else {
					if(!checkOperationIsNotRunning()) {
						return;
					}
					String op = osMoPlugin.getGroups().joinGroup(id, nameUser);
					startLongRunningOperation(op);
				}
			}
		});
		builder.create().show();
		AndroidUtils.softKeyboardDelayed(tracker);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		createMenuItem(menu, CONNECT_TO, R.string.osmo_connect, 
				0, 0,/*R.drawable.ic_action_marker_light,*/
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		createMenuItem(menu, CREATE_GROUP, R.string.osmo_create_group, 
				0, 0,/*R.drawable.ic_action_marker_light,*/
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	public void startLongRunningOperation(String operation) {
		this.operation = operation;
		setSupportProgressBarIndeterminateVisibility(true);
		uiHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(OsMoGroupsActivity.this, R.string.osmo_server_operation_failed, Toast.LENGTH_LONG).show();
				hideProgressBar();
			}
		}, 15000);
	}

	public void hideProgressBar() {
		OsMoGroupsActivity.this.operation = null;
		setSupportProgressBarIndeterminateVisibility(false);
	}
	
	@Override
	public void groupsListChange(final String operation, final OsMoGroup group) {
		uiHandler.post(new Runnable() {
			
			@Override
			public void run() {
				String top = OsMoGroupsActivity.this.operation;
				if(operation == top || (operation != null && operation.equals(top))) {
					hideProgressBar();
				}
				adapter.update(group);				
				adapter.notifyDataSetChanged();
				updateStatus();
			}
		});
	}
	
	public boolean checkOperationIsNotRunning() {
		if(operation != null) {
			Toast.makeText(this, R.string.wait_current_task_finished, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	class OsMoGroupsAdapter extends OsmandBaseExpandableListAdapter {

		private List<OsMoGroup> sortedGroups = new ArrayList<OsMoGroup>();
		private Map<OsMoGroup, List<OsMoDevice>> users = new LinkedHashMap<OsMoGroup, List<OsMoDevice>>();
		private OsMoGroups grs;
		private OsMoTracker tracker;

		public OsMoGroupsAdapter(OsMoGroups grs, OsMoTracker tracker) {
			this.grs = grs;
			this.tracker = tracker;
			synchronizeGroups();
		}

		public void update(OsMoGroup group) {
			if(group.isDeleted()) {
				sortedGroups.remove(group);
				users.remove(group);
			} else {
				List<OsMoDevice> us = !group.isEnabled() && !group.isMainGroup() ? new ArrayList<OsMoDevice>(0) : group.getGroupUsers();
				final Collator ci = Collator.getInstance();
				Collections.sort(us, new Comparator<OsMoDevice>() {

					@Override
					public int compare(OsMoDevice lhs, OsMoDevice rhs) {
						return ci.compare(lhs.getVisibleName(), rhs.getVisibleName());
					}
				});
				users.put(group, us);
				if(!sortedGroups.contains(group)) {
					sortedGroups.add(group);
				}
			}
		}
		


		public void synchronizeGroups() {
			users.clear();
			sortedGroups.clear();
			final Collator clt = Collator.getInstance();
			for (OsMoGroup key : grs.getGroups()) {
				sortedGroups.add(key);
				update(key);
			}
			Collections.sort(sortedGroups, new Comparator<OsMoGroup>() {
				@Override
				public int compare(OsMoGroup lhs, OsMoGroup rhs) {
					if (lhs.isMainGroup()) {
						return -1;
					}
					if (rhs.isMainGroup()) {
						return 1;
					}
					return clt.compare(lhs.getVisibleName(OsMoGroupsActivity.this),
							rhs.getVisibleName(OsMoGroupsActivity.this));
				}
			});
			notifyDataSetChanged();
		}

		@Override
		public OsMoDevice getChild(int groupPosition, int childPosition) {
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
		public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.osmo_group_item, parent, false);
				//fixBackgroundRepeat(row);
			}
			
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final OsMoGroup model = getGroup(groupPosition);
			if(selectedObject == model) {
				row.setBackgroundColor(getResources().getColor(R.color.row_selection_color));
			} else {
				row.setBackgroundColor(Color.TRANSPARENT);
			}
			label.setText(model.getVisibleName(OsMoGroupsActivity.this));
			if(model.isMainGroup() || model.isActive()) {
				label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				label.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
			}
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
			final OsMoDevice model = (OsMoDevice) getChild(groupPosition, childPosition);
			row.setTag(model);
			if(selectedObject == model) {
				row.setBackgroundColor(getResources().getColor(R.color.row_selection_color));
			} else {
				row.setBackgroundColor(Color.TRANSPARENT);
			}
			TextView label = (TextView) row.findViewById(R.id.osmo_label);
			TextView labelTime = (TextView) row.findViewById(R.id.osmo_label_time);
			ImageView icon = (ImageView) row.findViewById(R.id.osmo_user_icon);
			LatLon lnLocation = mapLocation;
			Location location = model.getLastLocation();
			if(model.getTrackerId().equals(osMoPlugin.getService().getMyGroupTrackerId())) {
				location = tracker.getLastSendLocation();
			}
			//Location location = tracker.getLastLocation(model.trackerId);
			if (location == null || lnLocation == null) {
				if(model.isEnabled()) {
				icon.setImageResource(
						//model.isActive() ? R.drawable.mon_osmo_conn_signal_small :
						 R.drawable.mon_osmo_conn_small);
				} else {
					icon.setImageResource(R.drawable.monitoring_rec_inactive);
					
				}
				label.setText(model.getVisibleName());
			} else {
				DirectionDrawable draw = new DirectionDrawable();
				float[] mes = new float[2];
				net.osmand.Location.distanceBetween(location.getLatitude(), location.getLongitude(),
						lnLocation.getLatitude(), lnLocation.getLongitude(), mes);
				draw.setAngle(mes[1] - lastCompass + 180);
				draw.setRecent(Math.abs(location.getTime() - System.currentTimeMillis()) < RECENT_THRESHOLD);
				icon.setImageDrawable(draw);
				int dist = (int) mes[0];
				long seconds = Math.max(0, (System.currentTimeMillis() - location.getTime()) / 1000);
				String time = "";
				if (seconds < 60) {
					seconds = (seconds / 5) * 5;
					time = seconds + " " + getString(R.string.seconds_ago);
					
				} else if (seconds / 60 < 100) {
					time = (seconds / 60) + " " + getString(R.string.minutes_ago);
				} else {
					time = (seconds / (60 * 60)) + " " + getString(R.string.hours_ago);
				}
				String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
				String visibleName = model.getVisibleName();
				String firstPart = distance + visibleName;
				label.setText(firstPart, TextView.BufferType.SPANNABLE);
				((Spannable) label.getText()).setSpan(
						new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0,
						distance.length() - 1, 0);
				labelTime.setText(time, TextView.BufferType.SPANNABLE);
				((Spannable) labelTime.getText()).setSpan(
						new ForegroundColorSpan(getResources().getColor(seconds < 60 ? R.color.color_ok : R.color.color_unknown)), 0,
						time.length(), 0);
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
					updateStatus();
				}
			});
			msg.what = LIST_REFRESH_MSG_ID;
			uiHandler.sendMessageDelayed(msg, 100);
		}
	}
	
	@Override
	public void locationChange(String trackerId, Location loc) {
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
