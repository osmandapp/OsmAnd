package net.osmand.base;

import android.os.StrictMode;

public class EnableStrictMode {

	public EnableStrictMode(){
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().
				penaltyLog()./*penaltyDeath().*/build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog()./*penaltyDeath().*/build());
	}
}
