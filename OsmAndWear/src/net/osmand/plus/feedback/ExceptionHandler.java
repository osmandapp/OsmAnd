package net.osmand.plus.feedback;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.RestartActivity;

import java.lang.Thread.UncaughtExceptionHandler;

class ExceptionHandler implements UncaughtExceptionHandler {

	private final OsmandApplication app;
	private final UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

	public ExceptionHandler(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
		try {
			app.getFeedbackHelper().saveException(thread, throwable);
			if (app.getRoutingHelper().isFollowingMode()) {
				RestartActivity.doRestartSilent(app);
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
