package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.plugins.PluginsHelper;

public class TakeMediaNoteCommand extends KeyEventCommand {

	public static final String ID = "take_media_note";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		AudioVideoNotesPlugin plugin = getPlugin();
		if (plugin != null) {
			plugin.makeAction(requireMapActivity(), AV_DEFAULT_ACTION_CHOOSE);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Nullable
	private AudioVideoNotesPlugin getPlugin() {
		return PluginsHelper.getEnabledPlugin(AudioVideoNotesPlugin.class);
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_take_media_note);
	}
}
