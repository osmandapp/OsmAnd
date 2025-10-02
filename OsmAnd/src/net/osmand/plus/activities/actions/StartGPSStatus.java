package net.osmand.plus.activities.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class StartGPSStatus {

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapActivity mapActivity;

	public StartGPSStatus(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.settings = app.getSettings();
	}
	
	public void run() {
		String appName = settings.GPS_STATUS_APP.get();
		GpsStatusApps[] values = GpsStatusApps.values();
		for (GpsStatusApps g : values) {
			if (appName.length() > 0 && g.appName.equals(appName)) {
				if (g.installed(mapActivity)) {
					runChosenGPSStatus(g);
					return;
				} else {
					settings.GPS_STATUS_APP.set("");
				}
			}
		}
		showDialog();
	}

	public void showDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.gps_status);
		LinearLayout ll = new LinearLayout(mapActivity);
		ListView lv = new ListView(mapActivity);
		int dp24 = AndroidUtils.dpToPx(mapActivity, 24f);
		int dp12 = AndroidUtils.dpToPx(mapActivity, 12f);
		int dp8 = AndroidUtils.dpToPx(mapActivity, 8f);
		lv.setPadding(0, dp8, 0, dp8);
		AppCompatCheckBox cb = new AppCompatCheckBox(mapActivity);
		cb.setText(R.string.shared_string_remember_my_choice);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(lp, dp24, dp8, dp8, dp24);
		cb.setLayoutParams(lp);
		cb.setPadding(dp8, 0, 0, 0);
		int textColorPrimary = ColorUtilities.getPrimaryTextColor(mapActivity, isNightMode());
		int selectedModeColor = settings.getApplicationMode().getProfileColor(isNightMode());
		cb.setTextColor(textColorPrimary);
		UiUtilities.setupCompoundButton(isNightMode(), selectedModeColor, cb);

		final int layout = R.layout.list_menu_item_native;
		ArrayAdapter<GpsStatusApps> adapter = new ArrayAdapter<GpsStatusApps>(mapActivity, layout, GpsStatusApps.values()) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Context themedCtx = UiUtilities.getThemedContext(mapActivity, isNightMode());
				LayoutInflater inflater = UiUtilities.getInflater(themedCtx, isNightMode());
				View v = inflater.inflate(layout, parent, false);
				TextView tv = v.findViewById(R.id.title);
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
		AlertDialog dlg = builder.create();
		lv.setOnItemClickListener((parent, view, position, id) -> {
			boolean remember = cb.isChecked();
			GpsStatusApps item = adapter.getItem(position);
			if (remember) {
				settings.GPS_STATUS_APP.set(item.appName);
			}
			dlg.dismiss();
			runChosenGPSStatus(item);
		});
		dlg.setView(ll);
		dlg.show();
	}

	private void runChosenGPSStatus(GpsStatusApps g) {
		if (g.installed(mapActivity)) {
			Intent intent = null;
			PackageManager pm = mapActivity.getPackageManager();
			try {
				String appName = !g.paidAppName.isEmpty() &&
						g.installed(mapActivity, g.paidAppName) ? g.paidAppName : g.appName;
				intent = pm.getLaunchIntentForPackage(appName);
			} catch (RuntimeException e) {
			}
			if (intent == null) {
				return;
			}
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			AndroidUtils.startActivityIfSafe(mapActivity, intent);
		} else if (Version.isMarketEnabled()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
			builder.setMessage(mapActivity.getString(R.string.gps_status_app_not_found));
			builder.setPositiveButton(mapActivity.getString(R.string.shared_string_yes), (dialog, which) -> {
				Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, g.appName));
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				AndroidUtils.startActivityIfSafe(mapActivity, intent);
			});
			builder.setNegativeButton(mapActivity.getString(R.string.shared_string_no), null);
			builder.show();
		} else {
			app.showToastMessage(R.string.gps_status_app_not_found);
		}
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
	}
}
