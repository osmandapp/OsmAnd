package net.osmand.plus.osmo;

import java.text.SimpleDateFormat;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MonitoringInfoControl;
import net.osmand.plus.views.MonitoringInfoControl.MonitoringInfoControlServices;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.MapUtils;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;

public class OsMoPlugin extends OsmandPlugin implements MonitoringInfoControlServices {

	private OsmandApplication app;
	public static final String ID = "osmand.osmo";
	private OsMoService service;
	private OsMoTracker tracker;
	private OsMoGroups groups;
	private BaseMapWidget osmoControl;
	private OsMoPositionLayer olayer;
	private Object followTrackerId;

	// 2014-05-27 23:11:40
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public OsMoPlugin(final OsmandApplication app) {
		service = new OsMoService(app);
		tracker = new OsMoTracker(service, app.getSettings().OSMO_SAVE_TRACK_INTERVAL,
				app.getSettings().OSMO_AUTO_SEND_LOCATIONS);
		this.app = app;
		ApplicationMode.regWidget("osmo_control", (ApplicationMode[])null);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		service.connect(true);
		groups = new OsMoGroups(service, tracker, app.getSettings());
		return true;
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		service.disconnect();
	}

	@Override
	public void updateLocation(Location location) {
		tracker.sendCoordinate(location);
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmo_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmo_plugin_name);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		// registerLayers(activity);
		super.updateLayers(mapView, activity);
		MonitoringInfoControl lock = activity.getMapLayers().getMapInfoLayer().getMonitoringInfoControl();
		if (lock != null && !lock.getMonitorActions().contains(this)) {
			lock.addMonitorActions(this);
		}
		
		
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude,
			ContextMenuAdapter adapter, final Object selectedObj) {
		if(selectedObj instanceof OsMoDevice) {
			adapter.item(R.string.osmo_center_location).icons(R.drawable.ic_action_gloc_dark, 
					R.drawable.ic_action_gloc_light).listen(new OnContextMenuClick() {
						
						@Override
						public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
							OsMoDevice o = (OsMoDevice) selectedObj;
							double lat = o.getLastLocation() == null ? latitude : o.getLastLocation().getLatitude();
							double lon = o.getLastLocation() == null ? longitude : o.getLastLocation().getLongitude();
							mapActivity.getMapView().setLatLon(lat, lon);
							OsMoPositionLayer.setFollowTrackerId(o);
						}
					}).position(0).reg();
			if(OsMoPositionLayer.getFollowDestinationId() != null) {
				adapter.item(R.string.osmo_cancel_moving_target).icons(R.drawable.ic_action_close_dark, 
						R.drawable.ic_action_close_light).listen(new OnContextMenuClick() {

							@Override
							public void onContextMenuClick(int itemId, int pos, boolean isChecked,
									DialogInterface dialog) {
								OsMoPositionLayer.setFollowDestination(null);
							}
							
						}).position(0).reg();
			}
			adapter.item(R.string.osmo_set_moving_target).icons(R.drawable.ic_action_flag_dark, 
					R.drawable.ic_action_flag_light).listen(new OnContextMenuClick() {
						
						@Override
						public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
							OsMoDevice o = (OsMoDevice) selectedObj;
							if(o.getLastLocation() != null) {
								TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
								targets.navigateToPoint(new LatLon(o.getLastLocation().getLatitude(), o.getLastLocation().getLongitude()), true, -1);
							}
							OsMoPositionLayer.setFollowDestination(o);
						}
					}).position(1).reg();
		}
		super.registerMapContextMenuActions(mapActivity, latitude, longitude, adapter, selectedObj);
	}
	
	@Override
	public void registerLayers(MapActivity activity) {
		super.registerLayers(activity);
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		osmoControl = createOsMoControl(activity, layer.getPaintText(), layer.getPaintSubText());
		layer.getMapInfoControls().registerSideWidget(osmoControl,
				R.drawable.mon_osmo_conn_big, R.string.osmo_control, "osmo_control", false, 18);
		layer.recreateControls();
		
		if(olayer != null) {
			activity.getMapView().removeLayer(olayer);
		}
		olayer = new OsMoPositionLayer(activity, this);
		activity.getMapView().addLayer(olayer, 5.5f);
	}
	
	@Override
	public void mapActivityPause(MapActivity activity) {
		groups.setUiListener(null);
	}
	
	@Override
	public void mapActivityResume(MapActivity activity) {
		if(olayer != null) {
			groups.setUiListener(olayer);
		}
	}
	
	/**
	 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer that shows a monitoring state (recorded/stopped)
	 */
	private BaseMapWidget createOsMoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final Drawable srcSmall = map.getResources().getDrawable(R.drawable.mon_osmo_conn_small);
		final Drawable srcSignalSmall = map.getResources().getDrawable(R.drawable.mon_osmo_conn_signal_small);
		final Drawable srcBig = map.getResources().getDrawable(R.drawable.mon_osmo_conn_big);
		final Drawable srcSignalBig = map.getResources().getDrawable(R.drawable.mon_osmo_conn_signal_big);
		final Drawable srcinactive = map.getResources().getDrawable(R.drawable.monitoring_rec_inactive);
		final TextInfoWidget osmoControl = new TextInfoWidget(map, 0, paintText, paintSubText) {
			long lastUpdateTime;
			private Drawable blinkImg;
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean visible = true;
				String txt = "OsMo";
				String subtxt = "";
				SessionInfo si = getService().getCurrentSessionInfo();
				if (si != null) {
					String uname = si.username;
					if (uname != null && uname.length() > 0) {
						if (uname.length() > 7 && uname.indexOf(' ') != -1) {
							uname = uname.substring(0, uname.indexOf(' '));
						}
						if (uname.length() > 4 && uname.indexOf(' ') != -1) {
							txt = "";
							subtxt = uname;
						} else {
							txt = uname;
						}
					}
				}
				Drawable small = srcinactive;
				Drawable big = srcinactive;
				long last = service.getLastCommandTime();
				if (service.isActive()) {
					small = tracker.isEnabledTracker() ? srcSignalSmall : srcSmall;
					big = tracker.isEnabledTracker() ? srcSignalBig : srcBig;
				}
				setText(txt, subtxt);
				if(blinkImg != small) {
					setImageDrawable(small);
				}
				if (last != lastUpdateTime) {
					lastUpdateTime = last;
					blink(big, small);
				}
				
				updateVisibility(visible);
				return true;
			}
			
			private void blink(Drawable bigger, final Drawable smaller ) {
				blinkImg = smaller;
				setImageDrawable(bigger);
				invalidate();
				postDelayed(new Runnable() {
					@Override
					public void run() {
						blinkImg = null;
						setImageDrawable(smaller);
						invalidate();
					}
				}, 500);
			}
		};
		osmoControl.updateInfo(null);

		osmoControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(map, OsMoGroupsActivity.class);
				map.startActivity(intent);
			}
		});
		return osmoControl;
	}

	@Override
	public void addMonitorActions(ContextMenuAdapter qa, MonitoringInfoControl li, final OsmandMapTileView view) {
		qa.item("Test (send)").icons(R.drawable.ic_action_grefresh_dark, R.drawable.ic_action_grefresh_light)
				.listen(new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						final double lat = view.getLatitude();
						final double lon = view.getLongitude();
						tracker.sendCoordinate(lat, lon);
					}
				}).reg();
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsOsMoActivity.class));
				return true;
			}
		});
		grp.setSummary(R.string.osmo_settings_descr);
		grp.setTitle(R.string.osmo_settings);
		grp.setKey("osmo_settings");
		screen.addPreference(grp);
	}
	
	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, ContextMenuAdapter helper) {
		helper.item(R.string.osmo_groups).icons(R.drawable.ic_action_eye_dark, R.drawable.ic_action_eye_light).position(6)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						Intent intent = new Intent(mapActivity, OsMoGroupsActivity.class);
						mapActivity.startActivity(intent);
					}
				}).reg();
	}
	
	public Object getFollowTrackerId() {
		return followTrackerId;
	}
	
	public void setMapFollowTrackerId(OsMoDevice d) {
		if(olayer != null) {
			OsMoPositionLayer.setFollowTrackerId(d);
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	public OsMoGroups getGroups() {
		return groups;
	}

	public OsMoTracker getTracker() {
		return tracker;
	}

	public OsMoService getService() {
		return service;
	}

	

}
