package net.osmand;

import android.util.Log;

public class CorwinLogger {

	public static void log(String message) {
		Log.d("Corwin", message + " (" + Thread.currentThread().getName() + ")");
	}

	public static void log_error(String message) {
		Log.e("Corwin", message + " (" + Thread.currentThread().getName() + ")");
	}
}