package net.osmand.plus.osmo;

import net.osmand.Location;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MonitoringInfoControl;
import net.osmand.plus.views.MonitoringInfoControl.MonitoringInfoControlServices;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

public class OsMoPlugin extends OsmandPlugin implements MonitoringInfoControlServices {

	private OsmandApplication app;
	public static final String ID = "osmand.osmo";
	private OsMoService service;
	private OsMoTracker tracker;
	private OsMoGroups groups;

	public OsMoPlugin(final OsmandApplication app) {
		service = new OsMoService(app);
		tracker = new OsMoTracker(service);
		tracker.enableTracker();
		groups = new OsMoGroups(service, tracker, app.getSettings());
		this.app = app;
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		if (service.isConnected()) {
			tracker.sendCoordinate(location);
		}
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
	
	public AsyncTask<Void, Void, Exception> 
	getRegisterDeviceTask(final Context uiContext, final Runnable postAction) {
		return new AsyncTask<Void, Void, Exception>() {
			private ProgressDialog dlg;
			protected void onPreExecute() {
				dlg = ProgressDialog.show(uiContext, "", app.getString(R.string.osmo_register_device));
			};
			@Override
			protected void onPostExecute(Exception result) {
				if(dlg.isShowing()) {
					dlg.dismiss();
				}
				if (result != null) {
					result.printStackTrace();
					app.showToastMessage(app.getString(R.string.osmo_io_error) + result.getMessage());
				} else if(postAction != null) {
					postAction.run();
				}
			}

			@Override
			protected Exception doInBackground(Void... params) {
				return service.registerOsmoDeviceKey();
			}
		};
	}
	
	

	@Override
	public void addMonitorActions(ContextMenuAdapter qa, MonitoringInfoControl li, final OsmandMapTileView view) {
		final boolean off = !service.isConnected();
		
		qa.item(off ? R.string.osmo_mode_off : R.string.osmo_mode_on)
				.icon(off ? R.drawable.monitoring_rec_inactive : R.drawable.monitoring_rec_big)
				.listen(new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if(app.getSettings().OSMO_DEVICE_KEY.get().length() == 0) {
							getRegisterDeviceTask(view.getContext(), new Runnable() {
								public void run() {
									service.connect(true);
								}
							}); 
							return;	
						}
						if(off) {
							service.connect(true);
						} else {
							service.disconnect();
						}
					}

					

				}).reg();
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
	public String getId() {
		return ID;
	}
	
	public OsMoGroups getGroups() {
		return groups;
	}
	
	
	public OsMoTracker getTracker() {
		return tracker;
	}
	
}
