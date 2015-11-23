package net.osmand.plus.audionotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.mapcontextmenu.MenuController;

import java.lang.ref.WeakReference;

public class AudioVideoNoteMenuController extends MenuController {
	private Recording recording;

	private AudioVideoNotesPlugin plugin;

	public AudioVideoNoteMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final Recording recording) {
		super(new AudioVideoNoteMenuBuilder(app, recording), pointDescription, mapActivity);
		this.recording = recording;
		plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					plugin.playRecording(getMapActivity(), getRecording());
				}
			}
		};
		if (!recording.isPhoto()) {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_play);
			leftTitleButtonController.leftIconId = R.drawable.ic_play_dark;
			String durationStr = recording.getPlainDuration();
			leftTitleButtonController.needRightText = true;
			leftTitleButtonController.rightTextCaption = "— " + durationStr;
		} else {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_show);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_view;
		}

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				AccessibleAlertBuilder bld = new AccessibleAlertBuilder(getMapActivity());
				bld.setMessage(R.string.recording_delete_confirm);
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (plugin != null) {
							plugin.deleteRecording(getRecording());
							getMapActivity().getContextMenu().close();
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.show();
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof Recording) {
			this.recording = (Recording) object;
		}
	}

	public Recording getRecording() {
		return recording;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public Drawable getLeftIcon() {
		if (recording.isPhoto()) {
			return getIcon(R.drawable.ic_action_photo_dark, R.color.audio_video_icon_color);
		} else if (recording.isAudio()) {
			return getIcon(R.drawable.ic_action_micro_dark, R.color.audio_video_icon_color);
		} else {
			return getIcon(R.drawable.ic_action_video_dark, R.color.audio_video_icon_color);
		}
	}

	@Override
	public String getNameStr() {
		return recording.getName(getMapActivity(), false);
	}

	@Override
	public String getTypeStr() {
		return recording.getType(getMapActivity());
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.audionotes_plugin_name);
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void share(LatLon latLon, String title) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		if (recording.isPhoto()) {
			Uri screenshotUri = Uri.parse(recording.getFile().getAbsolutePath());
			sharingIntent.setType("image/*");
			sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
		} else if (recording.isAudio()) {
			Uri audioUri = Uri.parse(recording.getFile().getAbsolutePath());
			sharingIntent.setType("audio/*");
			sharingIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
		} else if (recording.isVideo()) {
			Uri videoUri = Uri.parse(recording.getFile().getAbsolutePath());
			sharingIntent.setType("video/*");
			sharingIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
		}
		WeakReference<MapContextMenuFragment> fragmentRef = getMapActivity().getContextMenu().findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().startActivity(Intent.createChooser(sharingIntent, getMapActivity().getString(R.string.share_note)));
		}
	}
}
