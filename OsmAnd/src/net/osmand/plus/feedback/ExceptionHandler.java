package net.osmand.plus.feedback;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import java.lang.Thread.UncaughtExceptionHandler;

class ExceptionHandler implements UncaughtExceptionHandler {

	private final OsmandApplication app;
	private final PendingIntent pendingIntent;
	private final UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

	public ExceptionHandler(@NonNull OsmandApplication app) {
		this.app = app;
		Context context = app.getBaseContext();
		Intent intent = new Intent(context, app.getAppCustomization().getMapActivity());
		pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE);
	}

	@Override
	public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
		try {
			app.getFeedbackHelper().saveException(thread, throwable);
			if (app.getRoutingHelper().isFollowingMode()) {
				AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
				manager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingIntent);
				System.exit(2);
			}
			if (exceptionHandler != null) {
				exceptionHandler.uncaughtException(thread, throwable);
			}
		} catch (Exception e) {
			// swallow all exceptions
			android.util.Log.e(PlatformUtil.TAG, "Exception while handle other exception", e);
		}
	}
}
