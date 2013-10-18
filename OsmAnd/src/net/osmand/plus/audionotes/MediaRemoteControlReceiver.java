package net.osmand.plus.audionotes;

import net.osmand.plus.OsmandPlugin;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MediaRemoteControlReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Intent sent", Toast.LENGTH_LONG).show();
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
			if(plugin != null && intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) != null) {
				System.out.println("OsmAnd AV Button pressed " + intent.getIntExtra(Intent.EXTRA_KEY_EVENT, 0));
				// Toast.makeText(context, "Button pressed " + intent.getIntExtra(Intent.EXTRA_KEY_EVENT, 0), Toast.LENGTH_LONG).show();
			}
		}
	}
}