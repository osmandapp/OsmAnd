package net.osmand.skimaps;

import net.osmand.skimapsPlugin.R;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

public class SkiMapsActivity extends Activity {
	private static final String OSMAND_COMPONENT = "net.osmand"; //$NON-NLS-1$
	private static final String OSMAND_COMPONENT_PLUS = "net.osmand.plus"; //$NON-NLS-1$
	private static final String OSMAND_ACTIVITY = "net.osmand.plus.activities.MapActivity"; //$NON-NLS-1$
	
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent intentPlus = new Intent();
		intentPlus.setComponent(new ComponentName(OSMAND_COMPONENT_PLUS, OSMAND_ACTIVITY));
		intentPlus.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		ResolveInfo resolved = getPackageManager().resolveActivity(intentPlus, PackageManager.MATCH_DEFAULT_ONLY);
		if(resolved != null) {
			stopService(intentPlus);
			startActivity(intentPlus);
		} else {
			Intent intentNormal = new Intent();
			intentNormal.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			intentNormal.setComponent(new ComponentName(OSMAND_COMPONENT, OSMAND_ACTIVITY));
			resolved = getPackageManager().resolveActivity(intentNormal, PackageManager.MATCH_DEFAULT_ONLY);
			if (resolved != null) {
				stopService(intentNormal);
				startActivity(intentNormal);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.osmand_app_not_found));
				builder.setPositiveButton(getString(R.string.shared_string_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + OSMAND_COMPONENT_PLUS));
						try {
							stopService(intent);
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(getString(R.string.shared_string_no), null);
				builder.show();
			}
		}
	}
    
}