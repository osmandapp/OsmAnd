package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.AudioVideoNoteMenuBuilder;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class AudioVideoNoteMenuController extends MenuController {
	private Recording recording;

	private DateFormat dateFormat;
	private AudioVideoNotesPlugin plugin;

	public AudioVideoNoteMenuController(OsmandApplication app, MapActivity mapActivity, final Recording recording) {
		super(new AudioVideoNoteMenuBuilder(app, recording), mapActivity);
		this.recording = recording;
		plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		dateFormat = android.text.format.DateFormat.getMediumDateFormat(mapActivity);
		titleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					plugin.playRecording(getMapActivity(), recording);
				}
			}
		};
		if (!recording.isPhoto()) {
			titleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_play);
			titleButtonController.leftIconId = R.drawable.ic_play_dark;
			String durationStr = recording.getPlainDuration();
			titleButtonController.needRightText = true;
			titleButtonController.rightTextCaption = "— " + durationStr;
		} else {
			titleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_show);
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public Drawable getLeftIcon() {
		if (recording.isPhoto()) {
			return getIcon(R.drawable.ic_action_photo_dark, R.color.osmand_orange_dark, R.color.osmand_orange);
		} else if (recording.isAudio()) {
			return getIcon(R.drawable.ic_action_micro_dark, R.color.osmand_orange_dark, R.color.osmand_orange);
		} else {
			return getIcon(R.drawable.ic_action_video_dark, R.color.osmand_orange_dark, R.color.osmand_orange);
		}
	}

	@Override
	public String getNameStr() {
		File file = recording.getFile();
		if (file != null) {
			Date date = new Date(recording.getFile().lastModified());
			return dateFormat.format(date);
		} else {
			return recording.getName(getMapActivity());
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
