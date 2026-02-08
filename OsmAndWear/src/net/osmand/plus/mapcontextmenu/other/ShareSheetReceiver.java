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
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return;
		}
		int actionId = bundle.getInt(KEY_SHARE_ACTION_ID, -1);
		if (actionId >= 0 && actionId < ShareItem.values().length) {
			String sms = bundle.getString(KEY_SHARE_SMS, "");
			String address = bundle.getString(KEY_SHARE_ADDRESS, "");
			String title = bundle.getString(KEY_SHARE_TITLE, "");
			String coordinates = bundle.getString(KEY_SHARE_COORDINATES, "");
			String geoUrl = bundle.getString(KEY_SHARE_GEOURL, "");

			ShareItem item = ShareItem.values()[actionId];
			ShareMenu.startAction(context, item, sms, address, title, coordinates, geoUrl);
		}
	}
}