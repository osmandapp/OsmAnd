package net.osmand.plus.plugins.audionotes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaRemoteControlReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// DO NOTHING 	https://github.com/osmandapp/Osmand/issues/1262
//		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
//			AudioVideoNotesPlugin plugin = PluginsHelper.getEnabledPlugin(AudioVideoNotesPlugin.class);
//			if(plugin != null && intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) != null && 
//					plugin.getActivity() != null) {
//
//				System.out.println("OsmAnd AV Button pressed " + intent.getIntExtra(Intent.EXTRA_KEY_EVENT, 0));
				// Toast.makeText(context, "Button pressed " + intent.getIntExtra(Intent.EXTRA_KEY_EVENT, 0), Toast.LENGTH_LONG).show();
//				plugin.defaultAction(plugin.getActivity());
//			}
//		}
	}
}