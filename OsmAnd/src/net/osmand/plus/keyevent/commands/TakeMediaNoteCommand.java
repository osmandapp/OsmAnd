package net.osmand.plus.keyevent.commands;

import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;

import android.view.KeyEvent;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;

public class TakeMediaNoteCommand extends KeyEventCommand {

	public static final String ID = "take_media_note";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getEnabledPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			plugin.makeAction(requireMapActivity(), AV_DEFAULT_ACTION_CHOOSE);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
