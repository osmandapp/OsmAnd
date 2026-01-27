package net.osmand.plus.base;

import android.os.StrictMode;

public class EnableStrictMode {

	public EnableStrictMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.permitDiskReads()
				.permitDiskWrites()
				.penaltyLog()./*penaltyDeath().*/build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog()./*penaltyDeath().*/build());
	}
}