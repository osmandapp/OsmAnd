package net.osmand.plus.plugins.gtfs.selftest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;

/**
 * Debug-only entry point of the GTFS self-test:
 * {@code adb shell am broadcast -p net.osmand.dev -a net.osmand.dev.GTFS_SELFTEST --es scenarios all}.
 * Extras: {@code scenarios} (comma list or "all"), {@code out} (report directory), {@code options}
 * (comma list of key=value, for example constrained=1).
 * <p>
 * It is registered only in the debug manifest overlay (AndroidManifest-debug.xml), guarded by the DUMP
 * permission (the adb shell has it, other apps do not), and refuses to run in a non-debuggable build. The
 * work runs in {@link GtfsSelfTestService}, a foreground service, so it survives the screen turning off; if
 * the system refuses to start the service the receiver runs the job on its own thread under a wake lock.
 * Nothing here touches an activity or any view.
 */
public class GtfsSelfTestReceiver extends BroadcastReceiver {

	public static final String ACTION = "net.osmand.dev.GTFS_SELFTEST";
	public static final String EXTRA_SCENARIOS = "scenarios";
	public static final String EXTRA_OUT = "out";
	public static final String EXTRA_OPTIONS = "options";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null || !ACTION.equals(intent.getAction())) {
			return;
		}
		if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
			Log.w(GtfsSelfTestJob.TAG, "ignored: not a debuggable build");
			return;
		}
		Log.i(GtfsSelfTestJob.TAG, "broadcast received, starting the foreground service");
		try {
			Intent service = new Intent(context, GtfsSelfTestService.class);
			service.putExtras(intent.getExtras() != null ? intent.getExtras() : new android.os.Bundle());
			ContextCompat.startForegroundService(context, service);
		} catch (RuntimeException e) {
			Log.w(GtfsSelfTestJob.TAG, "foreground service refused (" + e + "), running on a plain thread", e);
			OsmandApplication app = (OsmandApplication) context.getApplicationContext();
			GtfsSelfTestJob.startThread(app, intent.getStringExtra(EXTRA_SCENARIOS), intent.getStringExtra(EXTRA_OUT),
					intent.getStringExtra(EXTRA_OPTIONS), "receiver-fallback");
		}
	}
}
