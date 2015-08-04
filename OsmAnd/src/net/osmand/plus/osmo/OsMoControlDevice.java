package net.osmand.plus.osmo;

import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.Version;
import net.osmand.plus.TargetPointsHelper.TargetPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Vibrator;

public class OsMoControlDevice implements OsMoReactor {

	private OsMoService service;
	private OsmandApplication app;
	private OsMoTracker tracker;
	private OsMoPlugin plugin;

	public OsMoControlDevice(OsmandApplication app, OsMoPlugin plugin, 
			OsMoService service, OsMoTracker tracker) {
		this.app = app;
		this.plugin = plugin;
		this.service = service;
		this.tracker = tracker;
		service.registerReactor(this);
	}
	
	public JSONObject getBatteryLevel() throws JSONException {
	    Intent batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	    int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	    int temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
	    int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
				
	    JSONObject postjson = new JSONObject();
            postjson.put("batteryprocent",  (level == -1 || scale == -1)? 50.0f : ((float)level / (float)scale) * 100.0f);
            postjson.put("temperature", temperature);
            postjson.put("voltage", voltage);
            postjson.put("plugged", plugged);

	    return postjson; 
	}

	@Override
	public boolean acceptCommand(String command, String id, String data, JSONObject obj, OsMoThread tread) {
		if(command.equals("REMOTE_CONTROL")) {
			if(data.equals("PP")) {
				   service.pushCommand("PP");
			} else if(data.equals("FF")) {
				Location ll = app.getLocationProvider().getLastKnownLocation();
				if(ll == null) {
					service.pushCommand("FF|0");
				} else {
					service.pushCommand("FF|"+OsMoTracker.formatLocation(ll));
				}
			} else if(data.equals("BATTERY_INFO")) {
				try {
				   service.pushCommand("BATTERY_INFO|"+getBatteryLevel().toString());
				} catch(JSONException e) {
				   e.printStackTrace();
				}
			} else if(data.equals("VIBRATE")) {
				Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
				 // Vibrate for 500 milliseconds
				 v.vibrate(1000);
			} else if(data.equals("STOP_TRACKING")) {
				tracker.disableTracker();
				if (app.getNavigationService() != null) {
					app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_LIVE);
				}
			} else if(data.equals("START_TRACKING")) {
				tracker.enableTrackerCmd();
				app.startNavigationService(NavigationService.USED_BY_LIVE);
				//interval setting not needed here, handled centrally in app.startNavigationService
				//app.getSettings().SERVICE_OFF_INTERVAL.set(0);
			} else if(data.equals("OSMAND_INFO")) {
				JSONObject robj = new JSONObject();
				try {
					robj.put("full_version", Version.getFullVersion(app));
					robj.put("version", Version.getAppVersion(app));
					TargetPointsHelper tg = app.getTargetPointsHelper();
					if(tg.getPointToNavigate() != null) {
						addPoint(robj, "target_", tg.getPointToNavigate().point, tg.getPointToNavigate().getOriginalPointDescription());
					}
					List<TargetPoint> intermediatePoints = tg.getIntermediatePoints();
					if (intermediatePoints.size() > 0) {
						JSONArray ar = new JSONArray();
						robj.put("intermediates", ar);
						for (int i = 0; i < intermediatePoints.size(); i++) {
							JSONObject js = new JSONObject();
							ar.put(js);
							addPoint(js, "", intermediatePoints.get(i).point, intermediatePoints.get(i).getOriginalPointDescription());
						}
					}
					service.pushCommand("OSMAND_INFO|"+robj.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return true;
		} else if(command.equals("TP")) {
			plugin.getDownloadGpxTask(true).execute(obj);
		} else if (command.equals("PP")) {
			service.pushCommand("PP");
		}
		return false;
	}

	private void addPoint(JSONObject robj, String prefix, LatLon pointToNavigate, PointDescription pointNavigateDescription) throws JSONException {
		robj.put(prefix+"lat", pointToNavigate.getLatitude());
		robj.put(prefix+"lon", pointToNavigate.getLongitude());
		if(pointNavigateDescription != null) {
			robj.put(prefix+"name", pointNavigateDescription.getName());
		}
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		return null;
	}

	@Override
	public void onConnected() {
	}

	@Override
	public void onDisconnected(String msg) {
	}

}
