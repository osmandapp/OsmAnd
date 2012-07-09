package net.osmand.plus.monitoring;

import net.osmand.LogUtil;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoControl;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;

import org.apache.commons.logging.Log;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;

public class OsmandMonitoringPlugin extends OsmandPlugin {
	private static final String ID = "osmand.monitoring";
	private OsmandSettings settings;
	private OsmandApplication app;
	private static final Log log = LogUtil.getLog(OsmandMonitoringPlugin.class);
	private MonitoringLayer monitoringLayer;
	
	public OsmandMonitoringPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		//settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, true);
		//settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		//settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
		return true;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_monitoring_description);
	}
	
	@Override
	public String getName() {
		return app.getString(R.string.monitor_preferences);
	}
	
	@Override
	public void registerLayers(MapActivity activity) {
		monitoringLayer = new MonitoringLayer(activity);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if ((!settings.SAVE_TRACK_TO_GPX.get())
				&& (mapView.getLayers().contains(monitoringLayer))) {
			mapView.removeLayer(monitoringLayer);
		} else {
			if (monitoringLayer == null)
				registerLayers(activity);
			mapView.addLayer(monitoringLayer, 16);
		}
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
			final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getMapActions().addWaypoint(latitude, longitude);
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_waypoint, R.drawable.list_activities_gpx_waypoint, listener, -1);
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference offlineData = screen.findPreference("local_indexes");
		if(offlineData == null) {
			log.error("OsmandMonitoringPlugin: Index settings preference not found !");
		} else {
			offlineData.setSummary(offlineData.getSummary() + " "+ app.getString(R.string.gpx_index_settings_descr));
		}
		PreferenceScreen grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setTitle(R.string.monitor_preferences);
		grp.setSummary(R.string.monitor_preferences_descr);
		grp.setKey("monitor_settings");
		((PreferenceCategory) screen.findPreference("profile_dep_cat")).addPreference(grp);

		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.save_track_to_gpx);
		grp.addPreference(cat);

		cat.addPreference(activity.createCheckBoxPreference(
				settings.SAVE_TRACK_TO_GPX, R.string.save_track_to_gpx,
				R.string.save_track_to_gpx_descrp));
		cat.addPreference(activity.createTimeListPreference(
				settings.SAVE_TRACK_INTERVAL, new int[] { 1, 2, 3, 5, 10, 15,
						20, 30 }, new int[] { 1, 2, 3, 5 }, 1,
				R.string.save_track_interval,
				R.string.save_track_interval_descr));

		Preference pref = new Preference(activity);
		pref.setTitle(R.string.save_current_track);
		pref.setSummary(R.string.save_current_track_descr);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SavingTrackHelper helper = app.getSavingTrackHelper();
				if (helper.hasDataToSave()) {
					saveCurrentTracks(helper, activity);
				} else {
					helper.close();
				}
				return true;
			}
		});
		cat.addPreference(pref);
		
		CheckBoxPreference infoControlPreference = activity
				.createCheckBoxPreference(settings.SHOW_MONITORING_CONTROL,
						R.string.monitoring_info_control,
						R.string.monitoring_info_control_desc);
		cat.addPreference(infoControlPreference);

		cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.live_monitoring);
		grp.addPreference(cat);

		cat.addPreference(activity.createCheckBoxPreference(
				settings.LIVE_MONITORING, R.string.live_monitoring,
				R.string.live_monitoring_descr));
		cat.addPreference(activity.createEditTextPreference(
				settings.LIVE_MONITORING_URL, R.string.live_monitoring_url,
				R.string.live_monitoring_url_descr));
		cat.addPreference(activity.createTimeListPreference(
				settings.LIVE_MONITORING_INTERVAL, new int[] { 1, 2, 3, 5, 10,
						15, 20, 30 }, new int[] { 1, 2, 3, 5 }, 1,
				R.string.live_monitoring_interval,
				R.string.live_monitoring_interval_descr));		

	}
	
	private void saveCurrentTracks(final SavingTrackHelper helper, final SettingsActivity activity) {
		activity.progressDlg = ProgressDialog.show(activity,
				activity.getString(R.string.saving_gpx_tracks),
				activity.getString(R.string.saving_gpx_tracks), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(activity.progressDlg);
		impl.setRunnable("SavingGPX", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
							helper.saveDataToGpx();
							helper.close();
						} finally {
							if (activity.progressDlg != null) {
								activity.progressDlg.dismiss();
								activity.progressDlg = null;
							}
						}
					}
				});
		impl.run();
	}
		
	private class MonitoringLayer extends OsmandMapLayer {
		
		private final MapActivity map;
		private Paint paintText;
		private Paint paintSubText;
		
		private TextInfoControl monitoringControl;
		private Drawable monitoring_rec;
		private Drawable monitoring;
		
		public MonitoringLayer(MapActivity map) {
			this.map = map;
		}

		@Override
		public void initLayer(OsmandMapTileView view) {
			
			paintText = new Paint();
			paintText.setStyle(Style.FILL_AND_STROKE);
			paintText.setColor(Color.BLACK);
			paintText.setTextSize(23 * MapInfoLayer.scaleCoefficient);
			paintText.setAntiAlias(true);
			paintText.setStrokeWidth(4);
			paintSubText = new Paint();
			paintSubText.setStyle(Style.FILL_AND_STROKE);
			paintSubText.setColor(Color.BLACK);
			paintSubText.setTextSize(15 * MapInfoLayer.scaleCoefficient);
			paintSubText.setAntiAlias(true);
			
			MapInfoLayer mapInfoLayer = map.getMapLayers().getMapInfoLayer();
			if ((mapInfoLayer != null) && (monitoringControl == null)) {
				mapInfoLayer.addRightStack(createMonitoringControl(map));
			}
		}

		@Override
		public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect,
				DrawSettings settings) {
		}

		@Override
		public void destroyLayer() {
		}

		@Override
		public boolean drawInScreenPixels() {
			return false;
		}
		
		/**
		 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer 
		 * that shows a monitoring state (recorded/stopped)
		 */
		private MapInfoControl createMonitoringControl(final MapActivity mapActivity) {	
			monitoringControl = new TextInfoControl(mapActivity, 0, paintText, paintSubText) {
				@Override
				public boolean updateInfo() {
					setText(getMonitoringControlTxt(), null);
					setImageDrawable(getMonitoringControlImg(mapActivity));
					return true;
				}		
			};
			monitoringControl.updateInfo();
			monitoringControl.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean isTrackMonitored = settings.SAVE_TRACK_TO_GPX.get();
					settings.SAVE_TRACK_TO_GPX.set(!isTrackMonitored);
					monitoringControl.updateInfo();
				}
			});	
			return monitoringControl;
		}
		
		private String getMonitoringControlTxt() {
			if (!settings.SHOW_MONITORING_CONTROL.get()) 
				return null;
			if(settings.SAVE_TRACK_TO_GPX.get()) 
				return "pause ";
			return "record";
		}
		
		private Drawable getMonitoringControlImg(MapActivity mapActivity) {
			if (monitoring_rec == null)
				monitoring_rec = mapActivity.getMapView().getResources().getDrawable(R.drawable.monitoring_rec_big);			
			if (monitoring == null)
				monitoring = mapActivity.getMapView().getResources().getDrawable(R.drawable.monitoring_rec_inactive);			
			if (settings.SAVE_TRACK_TO_GPX.get())
				return monitoring_rec;
			return monitoring;
		}
	}
}