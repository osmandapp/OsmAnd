package net.osmand.plus.osmo;

import java.io.IOException;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MonitoringInfoControl;
import net.osmand.plus.views.MonitoringInfoControl.MonitoringInfoControlServices;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class OsMoPlugin extends OsmandPlugin implements MonitoringInfoControlServices {

	private OsmandApplication app;
	public static final String ID = "osmand.osmo";
	private static final Log log = PlatformUtil.getLog(OsMoPlugin.class);
	private OsMoService service;

	public OsMoPlugin(final OsmandApplication app) {
		service = new OsMoService();
		this.app = app;
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		if (service.isActive()) {
			try {
				service.sendCoordinate(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
						(float) location.getAltitude(), location.getSpeed(), location.getBearing());
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static String getUUID(Context ctx) {
		return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
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
	
	private void connect(final boolean enable) {
		new AsyncTask<Void, Void, String>() {
			private Exception e;
			@Override
			protected String doInBackground(Void... params) {
				try {
					String response;
					if (enable) {
						response = service.activate(getUUID(app));
					} else {
						response = service.deactivate();
					}
					return response;
				} catch (Exception e) {
					this.e = e;
					return null;
				}
			}
			protected void onPostExecute(String result) {
				if(e != null) {
					app.showToastMessage(app.getString(R.string.osmo_io_error) + ": " + e.getMessage());
					log.error(e.getMessage(), e);
				} else {
					app.showToastMessage(result + "");
				}
			};
			
		}.execute();
	}
	
	private void sendCoordinate(final double lat, final double lon) {
		new AsyncTask<Void, Void, String>() {

			private Exception e;

			@Override
			protected String doInBackground(Void... params) {
				try {
					return service.sendCoordinate(lat, lon, 0, 0, 0,
							0);
				} catch (Exception e) {
					this.e = e;
					return null;
				}
			}
			protected void onPostExecute(String result) {
				if(e != null) {
					app.showToastMessage(app.getString(R.string.osmo_io_error) + ": " + e.getMessage());
					log.error(e.getMessage(), e);
				} else {
					app.showToastMessage(result + "");
				}
			};
			
		}.execute();
	}

	@Override
	public void addMonitorActions(ContextMenuAdapter qa, MonitoringInfoControl li, final OsmandMapTileView view) {
		final boolean off = !service.isActive();
		qa.item(off ? R.string.osmo_mode_off : R.string.osmo_mode_on)
				.icon(off ? R.drawable.monitoring_rec_inactive : R.drawable.monitoring_rec_big)
				.listen(new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						connect(off);
					}

					

				}).reg();
		qa.item("Test (send)").icons(R.drawable.ic_action_grefresh_dark, R.drawable.ic_action_grefresh_light)
				.listen(new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						final double lat = view.getLatitude();
						final double lon = view.getLongitude();
						sendCoordinate(lat, lon);
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
}
