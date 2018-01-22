package net.osmand.plus.audionotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class AudioVideoNoteMenuController extends MenuController {
	private Recording mRecording;
	private AudioVideoNotesPlugin mPlugin;
	private boolean mIsFileAvailable;

	public AudioVideoNoteMenuController(MapActivity mapActivity, PointDescription pointDescription, final Recording recording) {
		super(new AudioVideoNoteMenuBuilder(mapActivity, recording), pointDescription, mapActivity);
		this.mRecording = recording;
		mPlugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		mIsFileAvailable = mRecording.getFile().exists();
		builder.setShowTitleIfTruncated(false);

		if (mIsFileAvailable) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					if (mPlugin != null) {
						if (mPlugin.isPlaying(getRecording())) {
							mPlugin.stopPlaying();
						} else {
							mPlugin.playRecording(getMapActivity(), getRecording());
						}
					}
				}
			};

			rightTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					AlertDialog.Builder bld = new AlertDialog.Builder(getMapActivity());
					bld.setMessage(R.string.recording_delete_confirm);
					bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mPlugin != null) {
								mPlugin.deleteRecording(getRecording(), true);
								getMapActivity().getContextMenu().close();
							}
						}
					});
					bld.setNegativeButton(R.string.shared_string_no, null);
					bld.show();
				}
			};
			rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
			rightTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_delete_dark, true);
		}

		updateData();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof Recording) {
			this.mRecording = (Recording) object;
			mIsFileAvailable = mRecording.getFile().exists();
		}
	}

	@Override
	protected Object getObject() {
		return mRecording;
	}

	public Recording getRecording() {
		return mRecording;
	}

	@Override
	public Drawable getRightIcon() {
		if (mRecording.isPhoto()) {
			return getIcon(R.drawable.ic_action_photo_dark, R.color.audio_video_icon_color);
		} else if (mRecording.isAudio()) {
			return getIcon(R.drawable.ic_action_micro_dark, R.color.audio_video_icon_color);
		} else {
			return getIcon(R.drawable.ic_action_video_dark, R.color.audio_video_icon_color);
		}
	}

	@Override
	public String getNameStr() {
		if (mIsFileAvailable) {
			return mRecording.getName(getMapActivity(), false);
		} else {
			return getMapActivity().getString(R.string.data_is_not_available);
		}
	}

	@Override
	public String getTypeStr() {
		if (mIsFileAvailable) {
			return mRecording.getType(getMapActivity());
		} else {
			return super.getTypeStr();
		}
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
	public void updateData() {
		super.updateData();

		if (!mIsFileAvailable) {
			return;
		}
		boolean accessibilityEnabled = getMapActivity().getMyApplication().accessibilityEnabled();
		rightTitleButtonController.visible = true;
		if (!mRecording.isPhoto()) {
			if (mPlugin.isPlaying(mRecording)) {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_control_stop);
				leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_rec_stop, true);
				int pos = mPlugin.getPlayingPosition();
				String durationStr;
				if (pos == -1) {
					durationStr = mRecording.getPlainDuration(accessibilityEnabled);
				} else {
					durationStr = Algorithms.formatDuration(pos / 1000, accessibilityEnabled);
				}
				leftTitleButtonController.needRightText = true;
				leftTitleButtonController.rightTextCaption = "— " + durationStr;
				rightTitleButtonController.visible = false;
			} else {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_play);
				leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_play_dark, true);
				String durationStr = mRecording.getPlainDuration(accessibilityEnabled);
				leftTitleButtonController.needRightText = true;
				leftTitleButtonController.rightTextCaption = "— " + durationStr;
			}
		} else {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.recording_context_menu_show);
			leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_view, true);
		}
	}

	@Override
	public void share(LatLon latLon, String title, String address) {
		if (mIsFileAvailable) {
			String path = mRecording.getFile().getAbsolutePath();
			MediaScannerConnection.scanFile(getMapActivity(), new String[]{path},
					null, new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) {
							Intent shareIntent = new Intent(
									android.content.Intent.ACTION_SEND);
							if (mRecording.isPhoto()) {
								shareIntent.setType("image/*");
							} else if (mRecording.isAudio()) {
								shareIntent.setType("audio/*");
							} else if (mRecording.isVideo()) {
								shareIntent.setType("video/*");
							}
							shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
							shareIntent
									.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							getMapActivity().startActivity(Intent.createChooser(shareIntent,
									getMapActivity().getString(R.string.share_note)));
						}
					});
		} else {
			super.share(latLon, title, "");
		}
	}
}
