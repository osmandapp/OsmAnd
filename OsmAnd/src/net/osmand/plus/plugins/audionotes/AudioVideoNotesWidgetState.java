package net.osmand.plus.plugins.audionotes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

public class AudioVideoNotesWidgetState extends WidgetState {

	private final CommonPreference<Integer> defaultActionSetting;

	private static final int AV_WIDGET_STATE_ASK = R.id.av_notes_widget_state_ask;
	private static final int AV_WIDGET_STATE_AUDIO = R.id.av_notes_widget_state_audio;
	private static final int AV_WIDGET_STATE_VIDEO = R.id.av_notes_widget_state_video;
	private static final int AV_WIDGET_STATE_PHOTO = R.id.av_notes_widget_state_photo;

	AudioVideoNotesWidgetState(OsmandApplication ctx, CommonPreference<Integer> defaultActionSetting) {
		super(ctx);
		this.defaultActionSetting = defaultActionSetting;
	}

	@Override
	public int getMenuTitleId() {
		Integer action = defaultActionSetting.get();
		switch (action) {
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO:
				return R.string.av_def_action_audio;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO:
				return R.string.av_def_action_video;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE:
				return R.string.av_def_action_picture;
			default:
				return R.string.map_widget_av_notes;
		}
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		Integer action = defaultActionSetting.get();
		switch (action) {
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO:
				return R.drawable.ic_action_micro_dark;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO:
				return R.drawable.ic_action_video_dark;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE:
				return R.drawable.ic_action_photo_dark;
			default:
				return R.drawable.ic_action_photo_dark;
		}
	}

	@Override
	public int getMenuItemId() {
		Integer action = defaultActionSetting.get();
		switch (action) {
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO:
				return AV_WIDGET_STATE_AUDIO;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO:
				return AV_WIDGET_STATE_VIDEO;
			case AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE:
				return AV_WIDGET_STATE_PHOTO;
			default:
				return AV_WIDGET_STATE_ASK;
		}
	}

	@Override
	public int[] getMenuTitleIds() {
		return new int[] {R.string.av_def_action_choose, R.string.av_def_action_audio, R.string.av_def_action_video, R.string.av_def_action_picture};
	}

	@Override
	public int[] getMenuIconIds() {
		return new int[] {R.drawable.ic_action_photo_dark, R.drawable.ic_action_micro_dark, R.drawable.ic_action_video_dark, R.drawable.ic_action_photo_dark};
	}

	@Override
	public int[] getMenuItemIds() {
		return new int[] {AV_WIDGET_STATE_ASK, AV_WIDGET_STATE_AUDIO, AV_WIDGET_STATE_VIDEO, AV_WIDGET_STATE_PHOTO};
	}

	@Override
	public void changeState(int stateId) {
		if (stateId == AV_WIDGET_STATE_AUDIO) {
			defaultActionSetting.set(AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO);
		} else if (stateId == AV_WIDGET_STATE_VIDEO) {
			defaultActionSetting.set(AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO);
		} else if (stateId == AV_WIDGET_STATE_PHOTO) {
			defaultActionSetting.set(AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE);
		} else {
			defaultActionSetting.set(AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE);
		}
	}
}
