package net.osmand.plus.activities;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.List;

public class RestartActivity extends AppCompatActivity {

	private static final Log log = PlatformUtil.getLog(RestartActivity.class);

	private static final String RESTART_INTENT_KEY = "restart_intent_key";
	private static final String MAIN_PROCESS_PID_KEY = "main_process_pid_key";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Process.killProcess(intent.getIntExtra(MAIN_PROCESS_PID_KEY, -1));
		startActivity(intent.getParcelableExtra(RESTART_INTENT_KEY));

		finish();
		Runtime.getRuntime().exit(0);
	}

	public static void doRestartSilent(@NonNull Context ctx) {
		int currentPid = Process.myPid();
		Intent launchIntent = getRestartIntent(ctx);

		if (launchIntent == null) {
			log.error("Was not able to restart application, launch intent null");
			Process.killProcess(currentPid);
		} else {
			launchIntent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);

			Intent intent = new Intent(ctx, RestartActivity.class);
			intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(RESTART_INTENT_KEY, launchIntent);
			intent.putExtra(MAIN_PROCESS_PID_KEY, currentPid);
			ctx.startActivity(intent);
		}
	}

	@Nullable
	private static Intent getRestartIntent(@NonNull Context ctx) {
		String packageName = ctx.getPackageName();
		return ctx.getPackageManager().getLaunchIntentForPackage(packageName);
	}

	public static boolean isRestartProcess(@NonNull Context ctx) {
		int currentPid = Process.myPid();
		ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
		if (!Algorithms.isEmpty(processes)) {
			for (RunningAppProcessInfo processInfo : processes) {
				if (processInfo.pid == currentPid && processInfo.processName.endsWith(":restart")) {
					return true;
				}
			}
		}
		return false;
	}

	public static void doRestart(@NonNull Context ctx, boolean silent) {
		if (silent) {
			RestartActivity.doRestartSilent(ctx);
		} else {
			RestartActivity.doRestart(ctx);
		}
	}

	public static void doRestart(@NonNull Context ctx) {
		doRestart(ctx, ctx.getString(R.string.restart_is_required));
	}

	public static void doRestart(@NonNull Context ctx, @Nullable String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.shared_string_restart);
		builder.setMessage(message);
		builder.setNegativeButton(R.string.later, null);
		builder.setPositiveButton(R.string.restart_now, (dialog, which) -> doRestartSilent(ctx));
		builder.show();
	}

	public static void exitApp() {
		Process.killProcess(Process.myPid());
	}
}