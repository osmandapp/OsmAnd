package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;

public class StartGPSStatus extends OsmAndAction {

	public enum GpsStatusApps {
		GPC_CONNECTED("GPS Connected", "org.bruxo.gpsconnected", "", ""),
		GPS_STATUS("GPS Status & Toolbox", "com.eclipsim.gpsstatus2", "", "com.eclipsim.gpsstatus2.GPSStatus"),
		GPS_TEST("GPS Test", "com.chartcross.gpstest", "com.chartcross.gpstestplus", ""),
		GPSTEST("GPSTest", "com.android.gpstest", "", ""),
		SAT_STAT("SatStat (F-droid)", "com.vonglasow.michael.satstat", "", ""),
		GPSTESTSS("GPSTest (F-droid)", "com.android.gpstest.osmdroid", "", "");

		
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
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.gps_status);
		LinearLayout ll = new LinearLayout(activity);
		final ListView lv = new ListView(activity);
		final int dp24 = AndroidUtils.dpToPx(mapActivity, 24f);
		final int dp12 = AndroidUtils.dpToPx(mapActivity, 12f);
		final int dp8 = AndroidUtils.dpToPx(mapActivity, 8f);
		lv.setPadding(0, dp8, 0, dp8);
		final AppCompatCheckBox cb = new AppCompatCheckBox(activity);
		cb.setText(R.string.shared_string_remember_my_choice);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(lp, dp24, dp8, dp8, dp24);
		cb.setLayoutParams(lp);
		cb.setPadding(dp8, 0, 0, 0);
		int textColorPrimary = ContextCompat.getColor(activity, isNightMode() ? R.color.text_color_primary_dark : R.color.text_color_primary_light);
		int selectedModeColor = getSettings().getApplicationMode().getProfileColor(isNightMode());
		cb.setTextColor(textColorPrimary);
		UiUtilities.setupCompoundButton(isNightMode(), selectedModeColor, cb);
		
		final int layout = R.layout.list_menu_item_native;
		final ArrayAdapter<GpsStatusApps> adapter = new ArrayAdapter<GpsStatusApps>(mapActivity, layout, GpsStatusApps.values()) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = mapActivity.getLayoutInflater().inflate(layout, null);
	            TextView tv = (TextView)v.findViewById(R.id.title);
				tv.setPadding(dp12, 0, dp24, 0);
	            tv.setText(getItem(position).stringRes);		
	            v.findViewById(R.id.toggle_item).setVisibility(View.INVISIBLE);
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
					String appName = !g.paidAppName.isEmpty() &&
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
			if (Version.isMarketEnabled()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
				builder.setMessage(mapActivity. getString(R.string.gps_status_app_not_found));
				builder.setPositiveButton(mapActivity.getString(R.string.shared_string_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.getUrlWithUtmRef(getMyApplication(), g.appName)));
						try {
							mapActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(mapActivity.getString(R.string.shared_string_no), null);
				builder.show();
			} else {
				Toast.makeText(mapActivity, R.string.gps_status_app_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	
	
}
