package net.osmand.plus.liveupdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);
	@Override
	public void onReceive(Context context, Intent intent) {
		LOG.debug("onReceive");
	}
}
