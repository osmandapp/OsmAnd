package net.osmand.plus.mapcontextmenu.other;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ShareSheetReceiver extends BroadcastReceiver {

	public static final String KEY_SHARE_ACTION_ID = "share_action_id";
	public static final String KEY_SHARE_SMS = "key_share_sms";
	public static final String KEY_SHARE_ADDRESS = "key_share_address";
	public static final String KEY_SHARE_TITLE = "key_share_title";
	public static final String KEY_SHARE_COORDINATES = "key_share_coordinates";
	public static final String KEY_SHARE_GEOURL = "key_share_geourl";

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}

		int actionId = extras.getInt(KEY_SHARE_ACTION_ID, -1);
		if (actionId == -1) {
			return;
		}

		String sms = extras.getString(KEY_SHARE_SMS, "");
		String address = extras.getString(KEY_SHARE_ADDRESS, "");
		String title = extras.getString(KEY_SHARE_TITLE, "");
		String coordinates = extras.getString(KEY_SHARE_COORDINATES, "");
		String geoUrl = extras.getString(KEY_SHARE_GEOURL, "");

		ShareMenu.ShareItem item = ShareMenu.ShareItem.values()[actionId];
		ShareMenu.startAction(context, null, item, sms, address, title, coordinates, geoUrl);
	}
}