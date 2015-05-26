/**
 *
 */
package net.osmand.plus.osmo;

import gnu.trove.list.array.TIntArrayList;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListActivity;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.osmo.OsMoGroups.OsMoGroupsUIListener;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoGroup;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.util.MapUtils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 *
 */
public class OsMoGroupsActivity extends OsmandExpandableListActivity implements OsmAndCompassListener,
		OsmAndLocationListener, OsMoGroupsUIListener {

	public static final int CONNECT_TO = 1;
	protected static final int DELETE_ACTION_ID = 2;
	public static final int CREATE_GROUP = 3;
	protected static final int ON_OFF_ACTION_ID = 4;
	protected static final int SHARE_ID = 5;
	protected static final int SHOW_ON_MAP_ID = 6;
	public static final int SHARE_SESSION = 7;
	public static final int GROUP_INFO = 8;
	protected static final int SETTINGS_ID = 9;
	protected static final int SETTINGS_DEV_ID = 10;
	protected static final int TRACK_DEV_ID = 11;
	protected static final int LOGIN_ID = 12;
	public static final int LIST_REFRESH_MSG_ID = OsmAndConstants.UI_HANDLER_SEARCH + 30;
	public static final long RECENT_THRESHOLD = 60000;
	private boolean joinGroup;

	private OsMoPlugin osMoPlugin;
	private OsMoGroupsAdapter adapter;
	private Location mapLocation;
	private OsmandApplication app;
	private Handler uiHandler;

	private float width = 24;
	private float height = 24;
	private Path directionPath = new Path();
	private float lastCompass;
	private ActionMode actionMode;
	private Object selectedObject = null;
	private String operation;
	private Paint white;
	private View header;
	private View footer;
	
	
	@Override
	public void onCreate(Bundle icicle) {
		// This has to be called before setContentView and you must use the
		//TODO: remove this deprecated code with toolbar
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			setWindowOptionsDeprecated();
		}
		super.onCreate(icicle);
		app = (OsmandApplication) getApplication();
		osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		if(osMoPlugin == null) {
			osMoPlugin = OsmandPlugin.getPlugin(OsMoPlugin.class);
			OsmandPlugin.enablePlugin(this, app, osMoPlugin, true);
		}
		if(getIntent() != null) {
			if("http".equals(getIntent().getScheme())) {
				new OsMoIntentHandler(app, osMoPlugin).execute(getIntent());
			}
		}
		setContentView(R.layout.osmo_group_list);
		getSupportActionBar().setTitle(R.string.osmo);
		setSupportProgressBarIndeterminateVisibility(false);
		setupHeader();
		setupFooter();
		// getSupportActionBar().setIcon(R.drawable.tab_search_favorites_icon);

		
		adapter = new OsMoGroupsAdapter(osMoPlugin.getGroups(), osMoPlugin.getTracker(),
				osMoPlugin.getService());
		setListAdapter(adapter);
		
		
		uiHandler = new Handler();
		directionPath = createDirectionPath();
		
		white = new Paint();
		white.setStyle(Style.FILL_AND_STROKE);
		white.setColor(getResources().getColor(R.color.color_unknown));
		white.setAntiAlias(true);
		
		updateStatus();
		setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
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
		});
	}

	@SuppressLint("NewApi")
	private void setWindowOptionsDeprecated() {
		getWindow().setUiOptions(
				ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
	}

	private void setupHeader() {
		header = getLayoutInflater().inflate(R.layout.osmo_groups_list_header, null);
		getExpandableListView().addHeaderView(header);
		ImageView iv =  (ImageView) header.findViewById(R.id.share_my_location);
		iv.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				shareSession();
			}
		});
		CompoundButton trackr = (CompoundButton) header.findViewById(R.id.enable_tracker);
		trackr.setText(R.string.osmo_share_my_location);
		if(osMoPlugin != null && osMoPlugin.getTracker() != null){
			trackr.setChecked(osMoPlugin.getTracker().isEnabledTracker());
		}
		trackr.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					if (osMoPlugin != null && osMoPlugin.getTracker() != null){
						osMoPlugin.getTracker().enableTracker();
					}
					app.startNavigationService(NavigationService.USED_BY_LIVE);
					//interval setting not needed here, handled centrally in app.startNavigationService
					//app.getSettings().SERVICE_OFF_INTERVAL.set(0);
				} else {
					if (osMoPlugin != null && osMoPlugin.getTracker() != null){
						osMoPlugin.getTracker().disableTracker();
					}
					if (app.getNavigationService() != null) {
						app.getNavigationService().stopIfNeeded(app,NavigationService.USED_BY_LIVE);
					}
				}
				updateStatus();
			}
		});
		
		CompoundButton srvc = (CompoundButton) header.findViewById(R.id.enable_service);
		srvc.setChecked(osMoPlugin.getService().isEnabled());
		srvc.setText(R.string.osmo_start_service);
		srvc.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					osMoPlugin.getService().connect(true);
				} else {
					osMoPlugin.getTracker().disableTracker();
					osMoPlugin.getService().disconnect();
					if (app.getNavigationService() != null) {
						app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_LIVE);
					}
					if (getExpandableListView().getFooterViewsCount() > 0) {
						getExpandableListView().removeFooterView(footer);	
					}
				}
				setSupportProgressBarIndeterminateVisibility(true);
				header.postDelayed(new Runnable() {

					@Override
					public void run() {
						updateStatus();
						if (osMoPlugin.getService().isConnected()) {
							adapter.synchronizeGroups();
						} else {
							adapter.clear();
						}
						setSupportProgressBarIndeterminateVisibility(false);
					}
				}, 3000);
			}
		});
		
		
		
		TextView mtd = (TextView) header.findViewById(R.id.motd);
		SessionInfo si = osMoPlugin.getService().getCurrentSessionInfo();
		boolean visible = si != null && si.motd != null && si.motd.length() > 0;
		mtd.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible) {
			mtd.setText(si.motd);
			mtd.setLinksClickable(true);
			mtd.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		
	}

	private void setupFooter() {
		footer = getLayoutInflater().inflate(R.layout.osmo_groups_list_footer, null);
		TextView noConnectionTextView = (TextView) footer.findViewById(R.id.osmo_no_connection_msg);
		noConnectionTextView.setMovementMethod(LinkMovementMethod.getInstance());
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
		ImageView status = (ImageView) header.findViewById(R.id.osmo_status);
		final Drawable srcSmall = getResources().getDrawable(R.drawable.mon_osmo_conn_small);
		final Drawable srcSignalSmall = getResources().getDrawable(R.drawable.mon_osmo_conn_signal_small);
		final Drawable srcBig = getResources().getDrawable(R.drawable.mon_osmo_conn_big);
		final Drawable srcSignalBig = getResources().getDrawable(R.drawable.mon_osmo_conn_signal_big);
//		final Drawable srcinactive = getResources().getDrawable(R.drawable.mon_osmo_inactive);
		final Drawable srcSignalinactive = getResources().getDrawable(R.drawable.mon_osmo_signal_inactive);
//		final Drawable srcSignalinactive = app.getIconsCache().getPaintedContentIcon(R.drawable.mon_osmo_signal_inactive,
//				Color.LTGRAY);
		OsMoService service = osMoPlugin.getService();
		OsMoTracker tracker = osMoPlugin.getTracker();
		
		Drawable small = srcSignalinactive; //tracker.isEnabledTracker() ? srcSignalinactive : srcinactive;
		Drawable big = srcSignalinactive;// tracker.isEnabledTracker() ? srcSignalinactive : srcinactive;
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
		supportInvalidateOptionsMenu();
		if(service.isConnected()) {
			header.findViewById(R.id.motd).setVisibility(View.VISIBLE);
			header.findViewById(R.id.share_my_location_layout).setVisibility(View.VISIBLE);
			header.findViewById(R.id.share_my_location).setVisibility(tracker.isEnabledTracker() ? View.VISIBLE : View.INVISIBLE);
			if (service.isLoggedIn()) {
				getSupportActionBar().setTitle(app.getSettings().OSMO_USER_NAME.get());
			} else {
				getSupportActionBar().setTitle(R.string.anonymous_user);
			}
		} else {
			header.findViewById(R.id.motd).setVisibility(View.GONE);
			header.findViewById(R.id.share_my_location_layout).setVisibility(View.GONE);
			getSupportActionBar().setTitle(R.string.osmo);
		}
	}

	private Path createDirectionPath() {
		int h = 15;
		int w = 4;
		float sarrowL = 8; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float hpartArrowL = (harrowL - w) / 2;
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
		LatLon ml = getMyApplication().getSettings().getLastKnownMapLocation();
		mapLocation = new Location("map");
		mapLocation.setLatitude(ml.getLatitude());
		mapLocation.setLongitude(ml.getLongitude());
		if(!app.accessibilityEnabled()) {
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().registerOrUnregisterCompassListener(true);
		}
		app.getLocationProvider().addLocationListener(this);
		app.getLocationProvider().resumeAllUpdates();
		osMoPlugin.getGroups().addUiListeners(this);
		if(osMoPlugin.getService().isConnected()) {
			adapter.synchronizeGroups();
		}
		osMoPlugin.setGroupsActivity(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.getLocationProvider().pauseAllUpdates();
		if(!app.accessibilityEnabled()) {
			app.getLocationProvider().removeCompassListener(this);
		}
		app.getLocationProvider().removeLocationListener(this);
		osMoPlugin.getGroups().removeUiListener(this);
		osMoPlugin.setGroupsActivity(null);
	}
	
	private void enterSelectionMode(final Object o) {
		if(!checkOperationIsNotRunning()) {
			return;
		}
		actionMode = startSupportActionMode(new ActionMode.Callback() {
			private OsMoDevice device;
			private OsMoGroup group;
			private Menu menu;

			private MenuItem createActionModeMenuItem(final ActionMode actionMode, Menu m, int id, int titleRes, int icon, int menuItemType){
				final MenuItem menuItem = createMenuItem(m, id, titleRes, icon,
						menuItemType);
				menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						onActionItemClicked(actionMode, menuItem);
						return true;
					}
				});
				return menuItem;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectedObject = o;
				boolean portrait = AndroidUiHelper.isOrientationPortrait(OsMoGroupsActivity.this);
				if (portrait) {
					menu = getClearToolbar(true).getMenu();
				} else {
					getClearToolbar(false);
				}
				this.menu = menu;
				device = (OsMoDevice) (o instanceof OsMoDevice ? o : null);
				group = (OsMoGroup) (o instanceof OsMoGroup ? o : null);
				MenuItem mi = null;
				if (device != null) {
					mi = createActionModeMenuItem(actionMode, menu, ON_OFF_ACTION_ID, R.string.shared_string_ok, 0,
							MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
					mode.setTitle(device.getVisibleName());
				}
				if (device != null && device.getLastLocation() != null) {
					createActionModeMenuItem(actionMode, menu, SHOW_ON_MAP_ID, R.string.shared_string_show_on_map, R.drawable.ic_action_marker_dark,
							MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

				}
				createActionModeMenuItem(actionMode, menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark,
						// there is a bug in Android 4.2 layout
						device != null && device.getLastLocation() != null ? MenuItemCompat.SHOW_AS_ACTION_NEVER : MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				///
				if (device != null) {
					createActionModeMenuItem(actionMode, menu, SETTINGS_DEV_ID, R.string.shared_string_settings, R.drawable.ic_action_settings,
							// there is a bug in Android 4.2 layout
							device.getLastLocation() != null ? MenuItemCompat.SHOW_AS_ACTION_NEVER : MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				if (device != null && device.getLastLocation() != null) {
					MenuItem menuItem = createActionModeMenuItem(actionMode, menu, TRACK_DEV_ID, R.string.osmo_set_moving_target, R.drawable.ic_action_flage_dark,
												// there is a bug in Android 4.2 layout
							device.getLastLocation() != null ? MenuItemCompat.SHOW_AS_ACTION_NEVER : MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
					menuItem.setTitleCondensed(getString(R.string.osmo_follow));
				}
				if (group != null) {
					mode.setTitle(group.getVisibleName(OsMoGroupsActivity.this));
					createActionModeMenuItem(actionMode, menu, GROUP_INFO, R.string.osmo_group_info, R.drawable.ic_action_gabout_dark,
							MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				if ((group != null && !group.isMainGroup()) || (device != null && device.getGroup().isMainGroup())) {
					createActionModeMenuItem(actionMode, menu, DELETE_ACTION_ID, R.string.shared_string_delete,
							R.drawable.ic_action_delete_dark,
							MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}


				if (mi != null) {
					final LayoutInflater inflater = LayoutInflater.from(OsMoGroupsActivity.this);
					View view = inflater.inflate(R.layout.check_item_rel, null);
					final CompoundButton check = (CompoundButton) view.findViewById(R.id.check_item);
					check.setChecked((device != null && device.isActive() && device.isEnabled()) || (group != null && group.isActive() && group.isEnabled()));
					check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							onOffAction(check);
						}
					});
					MenuItemCompat.setActionView(mi, view);
				}
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectedObject = null;
				refreshList();
				if (AndroidUiHelper.isOrientationPortrait(OsMoGroupsActivity.this)){
					onCreateOptionsMenu(menu);
				}
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == TRACK_DEV_ID) {
					if (device != null && device.getLastLocation() != null) {
						TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
						targets.navigateToPoint(new LatLon(device.getLastLocation().getLatitude(), device
								.getLastLocation().getLongitude()), true, -1);
						OsMoPositionLayer.setFollowDestination(device);
						MapActivity.launchMapActivityMoveToTop(OsMoGroupsActivity.this);
					}
				} else if (item.getItemId() == SETTINGS_DEV_ID) {
					showSettingsDialog(device);
				} else if (item.getItemId() == DELETE_ACTION_ID) {
					Builder bld = new AlertDialog.Builder(OsMoGroupsActivity.this);
					String name = (selectedObject instanceof OsMoDevice) ? ((OsMoDevice) selectedObject).getVisibleName() :
							((OsMoGroup) selectedObject).getVisibleName(OsMoGroupsActivity.this);
					bld.setTitle(getString(
							selectedObject instanceof OsMoDevice ? R.string.delete_confirmation_msg :
									R.string.osmo_leave_confirmation_msg, name));
					bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Object obj = selectedObject;
							quitSelectionMode();
							deleteObject(obj);
						}
					});
					bld.setNegativeButton(R.string.shared_string_no, null);
					bld.show();
				} else if (item.getItemId() == GROUP_INFO) {
					showGroupInfo(group);
				} else if (item.getItemId() == SHARE_ID) {
					if (device != null) {
						shareTrackerId(device.getVisibleName(), device.getTrackerId());
					} else {
						shareOsMoGroup(group.getVisibleName(app), group.getGroupId());
					}
				} else if (item.getItemId() == SHOW_ON_MAP_ID) {
					if (device != null) {
						Location location = device.getLastLocation();
						MapActivity.getSingleMapViewTrackingUtilities().setMapLinkedToLocation(false);
						if (location != null) {
							app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), app.getSettings().getLastKnownMapZoom(),
									new PointDescription(PointDescription.POINT_TYPE_MARKER, device.getVisibleName()), false,
									device);
						}
						OsMoPositionLayer.setFollowTrackerId(device);
						MapActivity.launchMapActivityMoveToTop(OsMoGroupsActivity.this);
					}
				} else if (item.getItemId() == ON_OFF_ACTION_ID) {
					CompoundButton bt = ((CompoundButton) MenuItemCompat.getActionView(item).findViewById(R.id.check_item));
					onOffAction(bt);
				}
				return true;
			}

			private void onOffAction(CompoundButton bt) {
				if ((selectedObject instanceof OsMoDevice)) {
					OsMoDevice d = (OsMoDevice) selectedObject;
					if (bt.isChecked()) {
						osMoPlugin.getGroups().connectDevice(d);
					} else {
						osMoPlugin.getGroups().disconnectDevice(d);
					}
				} else {
					OsMoGroup g = (OsMoGroup) selectedObject;
					if (bt.isChecked()) {
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
		refreshList();
	}
	
	private StringBuilder setFields(StringBuilder bld, int field, String value) {
		bld.append(getString(field)).append(": ").append(value).append("\n");
		return bld;
	}
	
	

	protected void showGroupInfo(final OsMoGroup group) {
		Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(R.string.osmo_group);
		StringBuilder sb = new StringBuilder();
		if (group != null){
			setFields(sb, R.string.osmo_group_name, group.name);
			if(group.description != null) {
				setFields(sb, R.string.osmo_group_description, group.description);
			}
			if(group.expireTime != 0) {
				setFields(sb, R.string.osmo_expire_group, new Date(group.expireTime).toString());
			}
			if(group.policy != null) {
				setFields(sb, R.string.osmo_group_policy, group.policy);
			}
			setFields(sb, R.string.osmo_connect_to_group_id, group.groupId);
		}
		ScrollView sv = new ScrollView(this);
		TextView tv = new TextView(this);
		sv.addView(tv);
		tv.setPadding(5, 0, 5, 5);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(sb.toString());
		bld.setView(sv);
		bld.setPositiveButton(R.string.shared_string_ok, null);
		bld.setNegativeButton(R.string.osmo_invite, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				shareOsMoGroup(group.getVisibleName(app), group.getGroupId());
			}
		});
		bld.show();
		
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

	private void quitSelectionMode() {
		selectedObject = null;
		actionMode.finish();
		refreshList();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == CONNECT_TO) {
			connectToDevice();
			return true;
		} else if (item.getItemId() == SETTINGS_ID) {
			startActivity(new Intent(this, SettingsOsMoActivity.class));
			return true;
		} else if (item.getItemId() == LOGIN_ID) {
			loginDialog();
			return true;
		} else if (item.getItemId() == SHARE_SESSION) {
			shareSession();
			return true;
		} else if (item.getItemId() == CREATE_GROUP) {
			createGroup(true);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void loginDialog() {
		if (!osMoPlugin.getService().isLoggedIn()) {
			setSupportProgressBarIndeterminateVisibility(true);
			signinPost(false);
		} else {
			Builder bld = new AlertDialog.Builder(this);
			String text = getString(R.string.logged_as, app.getSettings().OSMO_USER_NAME.get());
			bld.setMessage(text);
			bld.setPositiveButton(R.string.shared_string_ok, null);
			bld.setNegativeButton(R.string.shared_string_logoff, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					logoff();
				}
			});
			bld.show();
		}
	}

	private void shareSession() {
		Builder bld = new AlertDialog.Builder(this);
		bld.setItems(new String[] {
				getString(R.string.osmo_share_current_session),
				getString(R.string.osmo_share_connect_device)
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					shareSessionUrl(osMoPlugin, OsMoGroupsActivity.this);
				} else {
					OsMoService service = osMoPlugin.getService();
					SessionInfo ci = service.getCurrentSessionInfo();
					shareTrackerId("", ci == null ? null : ci.trackerId);
				}
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();
	}
	
	public static void shareSessionUrl(OsMoPlugin osMoPlugin, Activity ctx) {
		String sessionURL = osMoPlugin.getTracker().getSessionURL();
		if(sessionURL == null ) {
			AccessibleToast.makeText(ctx, R.string.osmo_session_not_available, Toast.LENGTH_SHORT).show();
		} else {
			ShareDialog dlg = new ShareDialog(ctx);
			dlg.setTitle(ctx.getString(R.string.osmo_share_session));
			dlg.viewContent(sessionURL);
			dlg.shareURLOrText(sessionURL, ctx.getString(R.string.osmo_session_id_share, sessionURL), null);
			dlg.showDialog();
		}
	}

	private void shareTrackerId(String name, String trackerId) {
		if(trackerId == null) {
			AccessibleToast.makeText(this, R.string.osmo_auth_pending, Toast.LENGTH_SHORT).show();
		} else {
			ShareDialog dlg = new ShareDialog(this);
			String url = OsMoService.SHARE_TRACKER_URL + Uri.encode(trackerId);
			if(name != null && name.length() > 0){
				url += "&name="+Uri.encode(name);
			}
			dlg.setTitle(getString(R.string.osmo_tracker_id));
			dlg.viewContent(trackerId);
			dlg.shareURLOrText(url, getString(R.string.osmo_tracker_id_share, trackerId, name, url), null);
			dlg.showDialog();
		}
	}
	
	private void shareOsMoGroup(String name, String groupId) {
		ShareDialog dlg = new ShareDialog(this);
		String url = OsMoService.SHARE_GROUP_URL +Uri.encode(groupId)+"&name="+Uri.encode(name);
		dlg.setTitle(getString(R.string.osmo_group));
		dlg.viewContent(groupId);
		dlg.shareURLOrText(url, getString(R.string.osmo_group_share, groupId, name, url), null);
		dlg.showDialog();
	}
	
	private void signinPost() {
		signinPost(true);
	}
	
	
	static int getResIdFromAttribute(final Activity activity, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}
	
	private void signinPost(final boolean createGroup) {
		final Dialog dialog = new Dialog(this, 
				app.getSettings().isLightContent() ?
						R.style.OsmandLightTheme:
							R.style.OsmandDarkTheme);
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		Toolbar tb = new Toolbar(this);
		tb.setClickable(true);
		Drawable back = ((OsmandApplication)getApplication()).getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		tb.setNavigationIcon(back);
		tb.setTitle(R.string.osmo_sign_in);
		tb.setBackgroundColor(getResources().getColor( getResIdFromAttribute(this, R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});
		setSupportProgressBarIndeterminateVisibility(true);
		final WebView wv = new WebView(this);
		wv.loadUrl(OsMoService.SIGN_IN_URL + app.getSettings().OSMO_DEVICE_KEY.get());
		ScrollView scrollView = new ScrollView(this);
		int pad = (int) getResources().getDimension(R.dimen.list_content_padding);
//		scrollView.setPadding(pad, pad, pad, pad);
		ll.addView(tb);
		ll.addView(scrollView);
		scrollView.addView(wv);
		dialog.setContentView(ll);
		wv.setFocusable(true);
        wv.setFocusableInTouchMode(true);
		wv.requestFocus(View.FOCUS_DOWN);
		wv.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});
		
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (!createGroup) {
					updateStatus();
					setSupportProgressBarIndeterminateVisibility(false);
				}
			}
		});
		dialog.show();		
		wv.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, String url) {
				setSupportProgressBarIndeterminateVisibility(false);
				wv.requestFocus(View.FOCUS_DOWN);
			}
		    public boolean shouldOverrideUrlLoading(WebView view, String url){
				if (url.contains(OsMoService.SIGNED_IN_CONTAINS)) {
					Uri data = Uri.parse(url);
					String user = data.getQueryParameter("u");
					String pwd = data.getQueryParameter("p");
					app.getSettings().OSMO_USER_NAME.set(user);
					app.getSettings().OSMO_USER_PWD.set(pwd);
					osMoPlugin.getService().reconnectToServer();
					if (createGroup) {
						createGroupWithDelay(3000);
					} else {
						updateStatus();
					}
					dialog.dismiss();
					return true;
				}
		        return false; // then it is not handled by default action
		   }
		});
	}
	
	public void createGroupWithDelay(final int delay) {
		if(delay <= 0) {
			app.showToastMessage(R.string.osmo_not_signed_in);
			setSupportProgressBarIndeterminateVisibility(false);
			return;
		}
		setSupportProgressBarIndeterminateVisibility(true);
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if(osMoPlugin.getService().getRegisteredUserName() == null) {
					createGroupWithDelay(delay - 700);
				} else {
					setSupportProgressBarIndeterminateVisibility(false);
					createGroup(true);
				}
			}
		}, delay);
	}
	
	protected void signin() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.osmo_sign_in);
		String message = "";
		if(app.getSettings().OSMO_USER_PWD.get() != null) {
			message = getString(R.string.osmo_credentials_not_valid) +"\n";
		}
		message += getString(R.string.osmo_create_groups_confirm);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.osmo_sign_in, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				signinPost();
//				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
//						OsMoService.SIGN_IN_URL + app.getSettings().OSMO_DEVICE_KEY.get()));
//				startActivity(browserIntent);
			}
		});
		
		//builder.setNegativeButton(R.string.shared_string_no, null);
		builder.show();
	}

	private void createGroup(boolean check) {
		if(osMoPlugin.getService().getRegisteredUserName() == null && check) {
			signin();
			return;
		}
		final Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.osmo_create_group);
		final View v = getLayoutInflater().inflate(R.layout.osmo_create_group, getExpandableListView(), false);
		final EditText policy = (EditText) v.findViewById(R.id.Policy);
		final EditText description = (EditText) v.findViewById(R.id.Description);
		final EditText name = (EditText) v.findViewById(R.id.Name);
		final TextView lengthAlert = (TextView) v.findViewById(R.id.textLengthAlert);
		final CheckBox onlyByInvite = (CheckBox) v.findViewById(R.id.OnlyByInvite);

		final TextView warnCreateDesc = (TextView) v.findViewById(R.id.osmo_group_create_dinfo);
		View.OnClickListener click = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int vls = warnCreateDesc.getVisibility();
				warnCreateDesc.setVisibility(vls == View.VISIBLE? View.GONE : View.VISIBLE);
			}
		};
		ImageButton info = (ImageButton) v.findViewById(R.id.info);
		info.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_gabout_dark));
		info.setOnClickListener(click);
		warnCreateDesc.setOnClickListener(click);
		
		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(!checkOperationIsNotRunning()) {
					return ;
				}
				joinGroup = true;
				String op = osMoPlugin.getGroups().createGroup(name.getText().toString(), onlyByInvite.isChecked(),
						description.getText().toString(), policy.getText().toString());
				startLongRunningOperation(op);
			}
		});
		final AlertDialog dialog = builder.create();

		name.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() > 2) {
					lengthAlert.setVisibility(View.GONE);
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				} else {
					lengthAlert.setVisibility(View.VISIBLE);
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		dialog.show();
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
		
		final View mgv = v.findViewById(R.id.MyGroupName);
		final EditText nickname = (EditText) v.findViewById(R.id.NickName);
		nickname.setText(app.getSettings().OSMO_USER_NAME.get());
		
		device.setChecked(true);
		device.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					labelTracker.setText(R.string.osmo_connect_to_device_tracker_id);
					labelName.setText(R.string.osmo_connect_to_device_name);
					name.setVisibility(View.VISIBLE);
					labelName.setVisibility(View.VISIBLE);
					mgv.setVisibility(View.GONE);
				} else {
					labelTracker.setText(R.string.osmo_connect_to_group_id);
					labelName.setText(R.string.osmo_group_name);
					name.setHint(R.string.osmo_use_server_name);
					name.setVisibility(View.GONE);
					labelName.setVisibility(View.GONE);
					mgv.setVisibility(View.VISIBLE);
				}
			}
		});
		
		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String nameUser = name.getText().toString();
				final String id = tracker.getText().toString();
				String nick = nickname.getText().toString().isEmpty() ? "user" : nickname.getText().toString();

				if(id.length() == 0) {
					app.showToastMessage(R.string.osmo_specify_tracker_id);
					connectToDevice();
					return;
				}
				if(device.isChecked()) {
					OsMoDevice dev = osMoPlugin.getGroups().addConnectedDevice(id, nameUser, 
							ColorDialogs.getRandomColor());
					adapter.update(dev.group);
					adapter.notifyDataSetChanged();
				} else {
					if(!checkOperationIsNotRunning()) {
						return;
					}
					joinGroup = true;
					String op = osMoPlugin.getGroups().joinGroup(id, nameUser, nick);
					if(app.getSettings().OSMO_USER_PWD.get() == null) {
						app.getSettings().OSMO_USER_NAME.set(nick);
					}
					startLongRunningOperation(op);
				}
			}
		});
		builder.create().show();
		AndroidUtils.softKeyboardDelayed(tracker);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		OsMoService service = osMoPlugin.getService();
		MenuItem log = menu.findItem(LOGIN_ID);
		if (log != null) {
			log.setVisible(service.isConnected());
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		Menu oldMenu = menu;
		addLoginActionMenu(oldMenu);
		createMenuItem(oldMenu, SETTINGS_ID, R.string.shared_string_settings, R.drawable.ic_action_settings,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		boolean portrait = AndroidUiHelper.isOrientationPortrait(this);
		if (selectedObject == null) {
			if (portrait) {
				menu = getClearToolbar(true).getMenu();
			} else {
				getClearToolbar(false);
			}
			createMenuItem(menu, CONNECT_TO, R.string.osmo_connect, 0, 0,/* R.drawable.ic_action_marker_light, */
					MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			// createMenuItem(menu, SHARE_SESSION, R.string.osmo_share_session,
			// R.drawable.ic_action_gshare_dark,
			// MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, CREATE_GROUP, R.string.osmo_create_group, 0, 0,
			// R.drawable.ic_group_add,
					MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
		return super.onCreateOptionsMenu(menu);
	}

	private void addLoginActionMenu(Menu oldMenu) {
		OsMoService service = osMoPlugin.getService();
		String text;
		if (service.isLoggedIn()) {
			text = getString(R.string.logged_as, app.getSettings().OSMO_USER_NAME.get());
		} else {
			text = getString(R.string.anonymous_user);
		}
		MenuItem menuItem = oldMenu.add(0, LOGIN_ID, 0, text);
		menuItem.setIcon(R.drawable.ic_person);
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}

	public void startLongRunningOperation(String operation) {
		this.operation = operation;
		setSupportProgressBarIndeterminateVisibility(true);
		uiHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if(OsMoGroupsActivity.this.operation != null) {
					Toast.makeText(OsMoGroupsActivity.this, R.string.osmo_server_operation_failed, Toast.LENGTH_LONG).show();
				}
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
				if(joinGroup && (operation != null && operation.startsWith("GROUP_CONNECT"))) {
					showGroupInfo(group);
					joinGroup = false;
				}
				if(group != null) {
					adapter.update(group);
					adapter.notifyDataSetChanged();
				} else if (operation.startsWith("GROUP_GET_ALL")
						|| operation.startsWith("DEVICE_GET_ALL")
						|| operation.startsWith("SUBSCRIBE")
						|| operation.startsWith("UNSUBSCRIBE")) {
					adapter.synchronizeGroups();
				}
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
		private OsMoService srv;

		public OsMoGroupsAdapter(OsMoGroups grs, OsMoTracker tracker, OsMoService srv) {
			this.grs = grs;
			this.tracker = tracker;
			this.srv = srv;
			if(srv.isConnected()) {
				synchronizeGroups();
			}
		}

		public void update(OsMoGroup group) {
			if(group.isDeleted()) {
				sortedGroups.remove(group);
				users.remove(group);
			} else {
				List<OsMoDevice> us = !group.isEnabled() && !group.isMainGroup() ? new ArrayList<OsMoDevice>(0) : 
					group.getVisibleGroupUsers(srv.getMyGroupTrackerId());
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
		
		public void clear() {
			users.clear();
			sortedGroups.clear();
			notifyDataSetChanged();
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
			boolean light = getMyApplication().getSettings().isLightContent();
			adjustIndicator(groupPosition, isExpanded, row, light);
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
			View v = (View) row.findViewById(R.id.settings);
			if(model.isMainGroup()) {
				v.setVisibility(View.GONE);
			} else {
//				(ImageView) v.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_settings));

				if((selectedObject == model) != ((CheckBox) v).isChecked()) {
					((CheckBox) v).setChecked(selectedObject == model);
				}
				v.setVisibility(View.VISIBLE);
				v.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (model != selectedObject) {
							enterSelectionMode(model);
						} else {
							quitSelectionMode();
						}
					}
				});
			}
			CompoundButton ci = (CompoundButton) row.findViewById(R.id.check_item);
			if(model.isMainGroup()) {
				ci.setVisibility(View.GONE);
			} else {
				ci.setVisibility(View.VISIBLE);
				ci.setOnCheckedChangeListener(null);
				ci.setChecked(model.isEnabled() && model.isActive());
				ci.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(isChecked) {
							String operation = osMoPlugin.getGroups().connectGroup(model);
							startLongRunningOperation(operation);
						} else {
							String operation = osMoPlugin.getGroups().disconnectGroup(model);
							startLongRunningOperation(operation);
						}	
					}
				});
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
			final OsMoDevice model = getChild(groupPosition, childPosition);
			row.setTag(model);
			if(selectedObject == model) {
				row.setBackgroundColor(getResources().getColor(R.color.row_selection_color));
			} else {
				if (app.getSettings().isLightContent()){
					row.setBackgroundResource(R.drawable.expandable_list_item_background_light);
				} else {
					row.setBackgroundResource(R.drawable.expandable_list_item_background_dark);
				}
			}
			TextView label = (TextView) row.findViewById(R.id.osmo_label);
			TextView labelTime = (TextView) row.findViewById(R.id.osmo_label_time);
			ImageView icon = (ImageView) row.findViewById(R.id.osmo_user_icon);
			Location location = model.getLastLocation();
			if(model.getTrackerId().equals(osMoPlugin.getService().getMyGroupTrackerId())) {
				location = tracker.getLastSendLocation();
			}
			int color = getResources().getColor(R.color.color_unknown);
			int activeColor = model.getColor();
			if (activeColor == 0) {
				activeColor = ColorDialogs.getRandomColor();
				osMoPlugin.getGroups().setGenColor(model, activeColor);
			}
			//Location location = tracker.getLastLocation(model.trackerId);
			if(!model.isEnabled()) {
				icon.setVisibility(View.INVISIBLE);
				label.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				label.setText(model.getVisibleName());
				labelTime.setText("");
			} else if (location == null || mapLocation == null) {
				label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				icon.setVisibility(View.VISIBLE);
				NonDirectionDrawable draw ;
				if(icon.getDrawable() instanceof NonDirectionDrawable) {
					draw = (NonDirectionDrawable) icon.getDrawable(); 
				} else {
					draw = new NonDirectionDrawable();
				}
				draw.setColor(model.isEnabled() ? activeColor : color);
				icon.setImageDrawable(draw);
				label.setText(model.getVisibleName());
				labelTime.setText("");
			} else {
				label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				icon.setVisibility(View.VISIBLE);
				DirectionDrawable draw;
				if (icon.getDrawable() instanceof DirectionDrawable) {
					draw = (DirectionDrawable) icon.getDrawable();
				} else {
					draw = new DirectionDrawable();
				}
				float[] mes = new float[2];
				net.osmand.Location.distanceBetween(location.getLatitude(), location.getLongitude(),
						mapLocation.getLatitude(), mapLocation.getLongitude(), mes);
				//TODO: Hardy: Check: The arrow direction below may only be correct for the default display's standard orientation
				//      i.e. still needs to be corrected for .ROTATION_90/180/170
				//	Keep in mind: getRotation was introduced from Android 2.2
				draw.setAngle(mes[1] - lastCompass + 180);
				long now = System.currentTimeMillis();
				final boolean recent = Math.abs( now - location.getTime() ) < RECENT_THRESHOLD;
				draw.setColor(recent ? activeColor : color);
				icon.setImageDrawable(draw);
				int dist = (int) mes[0];
				long seconds = Math.max(0, (now - location.getTime()) / 1000);
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
						new ForegroundColorSpan(getResources().getColor(
								seconds < 60 ? R.color.color_ok : R.color.color_unknown)), 0, time.length(), 0);
			}

			return row;
		}


		
	}

	@Override
	public void updateLocation(Location location) {
		MapViewTrackingUtilities mv = MapActivity.getSingleMapViewTrackingUtilities();
		if(mv != null && mv.isMapLinkedToLocation() && location != null) {
			Location lt = mapLocation;
			mapLocation = location;
			if(lt == null || location.distanceTo(lt) > 8) {
				refreshList();
			}
		}
	}
	
	

	@Override
	public void updateCompassValue(float value) {
		float vl = lastCompass;
		lastCompass = value;
		if(Math.abs(MapUtils.degreesDiff(vl, value)) > 15) {
			refreshList();
		}
	}

	private void refreshList() {
		if (!uiHandler.hasMessages(LIST_REFRESH_MSG_ID)) {
			Message msg = Message.obtain(uiHandler, new Runnable() {
				@Override
				public void run() {
					adapter.notifyDataSetInvalidated();
					updateStatus();
				}
			});
			msg.what = LIST_REFRESH_MSG_ID;
			uiHandler.sendMessageDelayed(msg, 100);
		}
	}
	
	
	
	@Override
	public void deviceLocationChanged(OsMoDevice device) {
		refreshList();		
	}
	

	private void showSettingsDialog(final OsMoDevice device) {
		Builder bld = new AlertDialog.Builder(OsMoGroupsActivity.this);
		bld.setTitle(R.string.osmo_edit_device);
		final LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.osmo_edit_device, null);
        final EditText name = (EditText) view.findViewById(R.id.Name);
        if(device.getColor() == 0) {
        	osMoPlugin.getGroups().setDeviceProperties(device, device.getVisibleName(), 
        			ColorDialogs.getRandomColor());	
        }
        int devColor = device.getColor();        
        bld.setView(view);
        name.setText(device.getVisibleName());
        
        final Spinner colorSpinner = (Spinner) view.findViewById(R.id.ColorSpinner);
        final TIntArrayList list = new TIntArrayList();
        ColorDialogs.setupColorSpinner(this, devColor, colorSpinner, list);
		
		bld.setPositiveButton(R.string .shared_string_yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				osMoPlugin.getGroups().setDeviceProperties(device, 
						name.getText().toString(), list.get(colorSpinner.getSelectedItemPosition()));
			}
		});
		bld.setNegativeButton(R.string.shared_string_no, null);
		bld.show();
	}

	
	
	class NonDirectionDrawable extends Drawable {
		Paint paintRouteDirection;
		
		public void setColor(int color) {
			paintRouteDirection.setColor(color);
		}

		public NonDirectionDrawable() {
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			paintRouteDirection.setAntiAlias(true);
			
		}
		
		@Override
		public void draw(Canvas canvas) {
			canvas.drawCircle(width/2, height/2, (width + height) / 6, white);
			canvas.drawCircle(width/2, height/2, (width + height) / 7, paintRouteDirection);
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

	class DirectionDrawable extends Drawable {
		Paint paintRouteDirection;

		private float angle;

		public DirectionDrawable() {
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			paintRouteDirection.setAntiAlias(true);
		}

		public void setColor(int color) {
			paintRouteDirection.setColor(color);
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

	private void logoff() {
		if (osMoPlugin.getService().isLoggedIn()) {
			app.getSettings().OSMO_USER_NAME.set("");
			app.getSettings().OSMO_USER_PWD.set("");
			osMoPlugin.getService().reconnectToServer();
			updateStatus();
			osMoPlugin.getGroups().clearGroups();
			adapter.synchronizeGroups();
		}
	}
	
	private void showHint() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.anonymous_user);
		String message = getString(R.string.anonymous_user_hint);
		builder.setMessage(message);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.show();
	}

	

	public void handleConnect() {
		app.runInUIThread(new Runnable() {
			
			@Override
			public void run() {
				if (getExpandableListView().getFooterViewsCount() > 0) {
					getExpandableListView().removeFooterView(footer);	
				}
				updateStatus();
			}
		});
	}
	
	public void handleDisconnect(final String msg) {
		app.runInUIThread(new Runnable() {
			
			@Override
			public void run() {
				if (!TextUtils.isEmpty(msg)) {
					CompoundButton srvc = (CompoundButton) header.findViewById(R.id.enable_service);
					if (srvc.isChecked()) {
						if (getExpandableListView().getFooterViewsCount() == 0) {
							getExpandableListView().addFooterView(footer);	
						}
						adapter.clear();
					}
					updateStatus();
				}
			}
		});
	}

}

