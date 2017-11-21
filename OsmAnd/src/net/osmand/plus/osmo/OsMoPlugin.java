package net.osmand.plus.osmo;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class OsMoPlugin extends OsmandPlugin implements OsMoReactor {

	private OsmandApplication app;
	public static final String ID = "osmand.osmo";
	private OsMoService service;
	private OsMoTracker tracker;
	private OsMoGroups groups;
	private TextInfoWidget osmoControl;
	private OsMoPositionLayer olayer;
	protected MapActivity mapActivity;
	protected Activity groupsActivity;
	protected OsMoControlDevice deviceControl;

	private final static Log LOG = PlatformUtil.getLog(OsMoPlugin.class);


	public OsMoPlugin(final OsmandApplication app) {
		this.app = app;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.osmo_monitoring;
	}

	@Override
	public boolean init(final OsmandApplication app, Activity activity) {
		if (service == null) {
			service = new OsMoService(app, this);
			tracker = new OsMoTracker(service, app.getSettings().OSMO_SAVE_TRACK_INTERVAL,
					app.getSettings().OSMO_SEND_LOCATIONS_STATE);
			deviceControl = new OsMoControlDevice(app, this, service, tracker);
			groups = new OsMoGroups(this, service, tracker, app);
		}
		ApplicationMode.regWidgetVisibility("osmo_control", (ApplicationMode[]) null);
		if (app.getSettings().OSMO_AUTO_CONNECT.get() ||
				(System.currentTimeMillis() - app.getSettings().OSMO_LAST_PING.get() < 5 * 60 * 1000)) {
			service.connect(true);
		}
		return true;
	}


	public Activity getGroupsActivity() {
		return groupsActivity;
	}

	public void setGroupsActivity(Activity groupsActivity) {
		this.groupsActivity = groupsActivity;
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_LIVE);
		}
		tracker.disableTracker();
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
	public int getLogoResourceId() {
		return R.drawable.ic_osmo_dark;
	}


	@Override
	public String getHelpFileName() {
		return "feature_articles/osmo-plugin.html";
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (isActive()) {
			if (olayer == null) {
				registerLayers(activity);
			}
			if (osmoControl == null) {
				registerSideWidget(activity);
			}
		} else {
			MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
			if (layer != null && osmoControl != null) {
				layer.removeSideWidget(osmoControl);
				osmoControl = null;
				layer.recreateControls();
			}
			if (olayer != null) {
				activity.getMapView().removeLayer(olayer);
				olayer = null;
			}
		}
	}

	@Override
	public void registerLayers(MapActivity activity) {
		registerSideWidget(activity);
		if (olayer != null) {
			activity.getMapView().removeLayer(olayer);
		}
		olayer = new OsMoPositionLayer(activity, this);
		activity.getMapView().addLayer(olayer, 5.5f);
	}

	private void registerSideWidget(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		if (layer != null) {
			osmoControl = createOsMoControl(activity);
			layer.registerSideWidget(osmoControl, R.drawable.ic_osmo_dark, R.string.osmo_control, "osmo_control",
					false, 31);
			layer.recreateControls();
		}
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		groups.removeUiListener(olayer);
		mapActivity = null;
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		if (olayer != null) {
			groups.addUiListeners(olayer);
		}
		mapActivity = activity;
	}

	/**
	 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer that shows a monitoring state (recorded/stopped)
	 */
	private TextInfoWidget createOsMoControl(final MapActivity map) {
		final TextInfoWidget osmoControl = new TextInfoWidget(map) {
			long lastUpdateTime;
			private int blinkImg;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				String txt = "OsMo";
				String subtxt = "";
				SessionInfo si = getService().getCurrentSessionInfo();
				if (si != null) {
					String uname = si.username;
					if (uname != null && uname.length() > 0) {
						if (uname.length() > 7) {
							for (int k = 4; k < uname.length(); k++) {
								if (!Character.isLetterOrDigit(uname.charAt(k)) &&
										uname.charAt(k) != '.' && uname.charAt(k) != '-') {
									uname = uname.substring(0, k);
									break;
								}
							}
							if (uname.length() > 12) {
								uname = uname.substring(0, 12);
							}
						}
						if (uname.length() > 4) {
							txt = "";
							subtxt = uname;
						} else {
							txt = uname;
						}
					}
				}
				boolean night = drawSettings != null && drawSettings.isNightMode();
				int srcSignalinactive = !night ? R.drawable.widget_osmo_inactive_day : R.drawable.widget_osmo_inactive_night;
				int small = srcSignalinactive; //tracker.isEnabledTracker() ? srcSignalinactive : srcinactive;
				int big = srcSignalinactive; // tracker.isEnabledTracker() ? srcSignalinactive : srcinactive;
				long last = service.getLastCommandTime();
				if (service.isActive()) {
					if (tracker.isEnabledTracker()) {
						small = night ? R.drawable.widget_osmo_connected_location_night : R.drawable.widget_osmo_connected_location_day;
						big = night ? R.drawable.widget_osmo_connected_location_night : R.drawable.widget_osmo_connected_location_day;
					} else {
						small = night ? R.drawable.widget_osmo_connected_night : R.drawable.widget_osmo_connected_day;
						big = night ? R.drawable.widget_osmo_connected_night : R.drawable.widget_osmo_connected_day;
					}
				}
				setText(txt, subtxt);
				if (blinkImg != small) {
					setImageDrawable(small);
				}
				if (last != lastUpdateTime) {
					lastUpdateTime = last;
					blink(big, small);
				}

				return true;
			}

			private void blink(int bigger, final int smaller) {
				blinkImg = smaller;
				setImageDrawable(bigger);
				map.getMyApplication().runInUIThread(new Runnable() {
					@Override
					public void run() {
						blinkImg = 0;
						setImageDrawable(smaller);
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
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsOsMoActivity.class;
	}

	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, ContextMenuAdapter helper) {
		helper.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.osmo_groups, mapActivity)
				.setIcon(R.drawable.ic_osmo_dark)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						Intent intent = new Intent(mapActivity, OsMoGroupsActivity.class);
						mapActivity.startActivity(intent);
						return true;
					}
				})
				.setPosition(6)
				.createItem());
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

	public AsyncTask<WptPt, String, String> getSaveGpxTask(final String name,
														   final long timestamp,
														   final boolean generateToast,
														   final boolean isGroupConnect) {
		return new AsyncTask<WptPt, String, String>() {

			protected void onProgressUpdate(String... values) {
				if (values != null && generateToast) {
					String t = "";
					for (String s : values) {
						t += s + "\n";
					}
					app.showToastMessage(t.trim());
				}
			}

			@Override
			protected String doInBackground(WptPt... params) {
				final File fl = app.getAppPath(IndexConstants.GPX_INDEX_DIR + "/osmo");
				if (!fl.exists()) {
					fl.mkdirs();
				}
				File ps = new File(fl, name + ".gpx");
				String errors = "";
				boolean changed = false;
				if (!ps.exists() || (ps.lastModified() / 1000) != (timestamp / 1000)) {
					changed = true;
					GPXFile g;
					if (isGroupConnect) {
						g = new GPXFile();
					} else {
						g = GPXUtilities.loadGPXFile(app, ps);
					}
					for (WptPt point : params) {
						if (point.deleted) {
							for (WptPt pointInTrack : g.getPoints()) {
								if (pointInTrack.getExtensionsToRead().get("u").equals(
										point.getExtensionsToRead().get("u"))) {
									g.deleteWptPt(pointInTrack);
								}
							}
						} else {
							g.addPoint(point);
						}
					}
					errors = GPXUtilities.writeGpxFile(ps, g, app);
					ps.setLastModified(timestamp);
					if (errors == null) {
						errors = "";
					}
					if (generateToast) {
						publishProgress(app.getString(R.string.osmo_gpx_points_downloaded, name));
					}
				}
				SelectedGpxFile byPath = app.getSelectedGpxHelper().getSelectedFileByPath(ps.getAbsolutePath());
				if (byPath == null || changed) {
					GPXFile selectGPXFile = GPXUtilities.loadGPXFile(app, ps);
					if (byPath != null) {
						app.getSelectedGpxHelper().selectGpxFile(selectGPXFile, false, false);
					}
					app.getSelectedGpxHelper().setGpxFileToDisplay(selectGPXFile);
				}
				return errors;
			}

			@Override
			protected void onPostExecute(String result) {
				if (result.length() > 0 && generateToast) {
					app.showToastMessage(app.getString(R.string.osmo_io_error) + result);
				}
			}

		};
	}

	public AsyncTask<JSONObject, String, String> getDownloadGpxTask(final boolean makeVisible) {

		return new AsyncTask<JSONObject, String, String>() {

			@Override
			protected String doInBackground(JSONObject... params) {
				final File fl = app.getAppPath(IndexConstants.GPX_INDEX_DIR + "/osmo");
				if (!fl.exists()) {
					fl.mkdirs();
				}
				String errors = "";
				for (JSONObject obj : params) {
					try {
						File f = new File(fl, obj.getString("name") + ".gpx");
						long timestamp = obj.getLong("created") * 1000;
						int color = 0;
						if (obj.has("color")) {
							try {
								color = Algorithms.parseColor(obj.getString("color"));
							} catch (RuntimeException e) {
								LOG.warn("", e);
							}
						}
						boolean visible = obj.has("visible");
						boolean changed = false;
						if (!f.exists() || (f.lastModified() != timestamp)) {
							long len = !f.exists() ? -1 : f.length();
							boolean sizeEqual = obj.has("size") && obj.getLong("size") == len;
							boolean modifySupported = f.setLastModified(timestamp - 1);
							if (!sizeEqual || modifySupported) {
								changed = true;
								String url = obj.getString("url");
								LOG.info("Download gpx " + url);
								DownloadFileHelper df = new DownloadFileHelper(app);
								InputStream is = df.getInputStreamToDownload(new URL(url), false);
								int av = is.available();
								if (av > 0 && !modifySupported && len == av) {
									// ignore
									is.close();
								} else {
									redownloadFile(f, timestamp, color, is);
									publishProgress(app.getString(R.string.osmo_gpx_track_downloaded, obj.getString("name")));
								}
							}
						}
						if (visible && (changed || makeVisible)) {
							GPXFile selectGPXFile = GPXUtilities.loadGPXFile(app, f);
							if (color != 0) {
								selectGPXFile.setColor(color);
							}
							app.getSelectedGpxHelper().setGpxFileToDisplay(selectGPXFile);
						}
					} catch (JSONException | IOException e) {
						e.printStackTrace();
						errors += e.getMessage() + "\n";
					}
				}
				return errors;
			}

			private void redownloadFile(File f, long timestamp, int color, InputStream is)
					throws IOException {
				FileOutputStream fout = new FileOutputStream(f);
				byte[] buf = new byte[1024];
				int k;
				while ((k = is.read(buf)) >= 0) {
					fout.write(buf, 0, k);
				}
				fout.close();
				is.close();
				if (!f.setLastModified(timestamp)) {
					LOG.error("Timestamp updates are not supported");
				}
			}

			protected void onProgressUpdate(String... values) {
				if (values != null) {
					String t = "";
					for (String s : values) {
						t += s + "\n";
					}
					app.showToastMessage(t.trim());
					refreshMap();
				}
			}

			@Override
			protected void onPostExecute(String result) {
				if (result.length() > 0) {
					app.showToastMessage(app.getString(R.string.osmo_io_error) + result);
				}
			}

		};

	}

	public void refreshMap() {
		if (mapActivity != null) {
			mapActivity.getMapView().refreshMap();
		}
	}

	@Override
	public boolean acceptCommand(String command, String id, String data, JSONObject obj, OsMoThread tread) {
		return false;
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		return null;
	}

	@Override
	public void onConnected() {
		if (groupsActivity instanceof OsMoGroupsActivity) {
			((OsMoGroupsActivity) groupsActivity).handleConnect();
		}
	}

	@Override
	public void onDisconnected(String msg) {
		if (groupsActivity instanceof OsMoGroupsActivity) {
			((OsMoGroupsActivity) groupsActivity).handleDisconnect(msg);
		}
	}

	public boolean useHttps() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashOsMoFragment.FRAGMENT_DATA;
	}
}
