package net.osmand.turnScreenOn.listener;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;

public class LockHelperEventListener implements OnLockListener {
	private TurnScreenApp app;
	private OsmAndAidlHelper osmAndAidlHelper;
	
	public LockHelperEventListener(TurnScreenApp app) {
		this.app = app;
		osmAndAidlHelper = app.getOsmAndAidlHelper();
	}

	@Override
	public void onLock() {
		osmAndAidlHelper.changeMapActivityKeyguardFlags(false);
	}

	@Override
	public void onUnlock() {
		osmAndAidlHelper.changeMapActivityKeyguardFlags(true);
	}
}
