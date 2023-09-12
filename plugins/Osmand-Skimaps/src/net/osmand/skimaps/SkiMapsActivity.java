package net.osmand.skimaps;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import net.osmand.skimapsPlugin.R;

public class SkiMapsActivity extends Activity {

	private static final String OSMAND_COMPONENT = "net.osmand";
	private static final String OSMAND_COMPONENT_PLUS = "net.osmand.plus";
	private static final String OSMAND_ACTIVITY = "net.osmand.plus.activities.MapActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
		descriptionTextView.setText(Html.fromHtml(getString(R.string.plugin_description)));

		Intent intentPlus = new Intent();
		intentPlus.setComponent(new ComponentName(OSMAND_COMPONENT_PLUS, OSMAND_ACTIVITY));
		intentPlus.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		ResolveInfo resolved = getPackageManager().resolveActivity(intentPlus, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolved != null) {
			logEvent(this, "open_osmand_plus");
			stopService(intentPlus);
			startActivity(intentPlus);
			finish();
		} else {
			Intent intentNormal = new Intent();
			intentNormal.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			intentNormal.setComponent(new ComponentName(OSMAND_COMPONENT, OSMAND_ACTIVITY));
			resolved = getPackageManager().resolveActivity(intentNormal, PackageManager.MATCH_DEFAULT_ONLY);
			if (resolved != null) {
				logEvent(this, "open_osmand");
				stopService(intentNormal);
				startActivity(intentNormal);
				finish();
			} else {
				logEvent(this, "open_dialog");
				findViewById(R.id.buyButton).setOnClickListener(v -> {
					String appName = OSMAND_COMPONENT;
					logEvent(SkiMapsActivity.this, "open_play_store_" + appName);
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName));
					try {
						//stopService(intent);
						startActivity(intent);
						finish();
					} catch (ActivityNotFoundException e) {
						// ignore
					}
				});
			}
		}
	}

	public void logEvent(Activity ctx, String event) {
		try {
			// not implemented yet
		} catch (Exception e) {
			//ignore
		}
	}
}