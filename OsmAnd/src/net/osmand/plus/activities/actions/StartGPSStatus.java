package net.osmand.plus.activities.actions;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class StartGPSStatus extends OsmAndAction {

	public enum GpsStatusApps {
		GPS_STATUS("GPS Status & Toolbox", "com.eclipsim.gpsstatus2", "", "com.eclipsim.gpsstatus2.GPSStatus"),
		GPS_TEST("GPS Test", "com.chartcross.gpstest", "com.chartcross.gpstestplus", ""),
		INVIU_GPS("inViu GPS-details ", "de.enaikoon.android.inviu.gpsdetails", "", ""),
		ANDROI_TS_GPS_TEST("AndroiTS GPS Test", "com.androits.gps.test.free", "com.androits.gps.test.pro", ""),
		SAT_STAT("SatStat (F-droid)", "com.vonglasow.michael.satstat", "", "");
		
		public final String stringRes;
		public final String appName;
		public final String paidAppName;
		public final String activity;

		GpsStatusApps(String res, String appName, String paidAppName, String activity) {
			this.stringRes = res;
			this.appName = appName;
			this.paidAppName = paidAppName;
			this.activity = activity;
		}
		
		public boolean installed(Activity a) {
			return installed(a, appName, paidAppName);
		}
		
		public boolean installed(Activity a, String... appName) {
			boolean installed = false;
			PackageManager packageManager = a.getPackageManager();
			for (String app: appName) {
				try{
					installed = packageManager.getPackageInfo(app, 0) != null;
					break;
				} catch ( NameNotFoundException e){
					installed = false;
				}	
			}			
			return installed;
		}
	}
	
	public StartGPSStatus(MapActivity mapActivity) {
		super(mapActivity);
	}
	
	@Override
	public void run() {
		String appName = getSettings().GPS_STATUS_APP.get();
		GpsStatusApps[] values = GpsStatusApps.values();
		for(GpsStatusApps g : values) {
			if(appName.length() > 0 && g.appName.equals(appName)) {
				if(g.installed(mapActivity)) {
					runChosenGPSStatus(g);
					return;
				} else {
					getSettings().GPS_STATUS_APP.set("");
				}
			}
		}
		showDialog();
	}
	
	@Override
	public int getDialogID() {
		return OsmAndDialogs.DIALOG_START_GPS;
	}
	
	@Override
	public Dialog createDialog(Activity activity, Bundle args) {
		GpsStatusApps[] values = GpsStatusApps.values();
		String[] res = new String[values.length];
		int i = 0;
		for(GpsStatusApps g : values) {
			res[i++] = g.stringRes;
		}
		OsmandMapTileView view = mapActivity.getMapView();
		AlertDialog.Builder builder = new AccessibleAlertBuilder(mapActivity);
		LinearLayout ll = new LinearLayout(view.getContext());
		final ListView lv = new ListView(view.getContext());
		lv.setPadding(7, 3, 7, 0);
		final CheckBox cb = new CheckBox(view.getContext());
		cb.setText(R.string.remember_choice);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(7, 10, 7, 0);
		cb.setLayoutParams(lp);
		
		final int layout;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			layout = R.layout.list_menu_item;
		} else {
			layout = R.layout.list_menu_item_native;
		}
		final ArrayAdapter<GpsStatusApps> adapter = new ArrayAdapter<GpsStatusApps>(mapActivity, layout, GpsStatusApps.values()) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = mapActivity.getLayoutInflater().inflate(layout, null);
	            TextView tv = (TextView)v.findViewById(R.id.title);
	            tv.setText(getItem(position).stringRes);		
	            v.findViewById(R.id.check_item).setVisibility(View.INVISIBLE);
				return v;
			}
		};
		lv.setAdapter(adapter);
		
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.addView(lv);
		ll.addView(cb);
		final AlertDialog dlg = builder.create();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				boolean remember = cb.isChecked();
				GpsStatusApps item = adapter.getItem(position);
				if(remember) {
					getSettings().GPS_STATUS_APP.set(item.appName);
				}
				dlg.dismiss();
				runChosenGPSStatus(item);
			}
		});
		dlg.setView(ll);
		return dlg;
	}

	private void runChosenGPSStatus(final GpsStatusApps g) {
		if (g.installed(mapActivity)) {
			Intent intent = null;
			// if (g.activity.length() == 0) {
				PackageManager pm = mapActivity.getPackageManager();
				try {
					String appName = !g.paidAppName.equals("") &&
							g.installed(mapActivity, g.paidAppName) ? g.paidAppName : g.appName;
					intent = pm.getLaunchIntentForPackage(appName);
				} catch (RuntimeException e) {
				}
//			} else {
//				intent = new Intent();
//				intent.setComponent(new ComponentName(g.appName, g.activity));
//			}
			if(intent == null) {
				return;
			}
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			mapActivity.startActivity(intent);
		} else {
			if (Version.isMarketEnabled(getMyApplication())) {
				AlertDialog.Builder builder = new AccessibleAlertBuilder(mapActivity);
				builder.setMessage(mapActivity. getString(R.string.gps_status_app_not_found));
				builder.setPositiveButton(mapActivity.getString(R.string.default_buttons_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(getMyApplication()) + g.appName));
						try {
							mapActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(mapActivity.getString(R.string.default_buttons_no), null);
				builder.show();
			} else {
				Toast.makeText(mapActivity, R.string.gps_status_app_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	
	
}
