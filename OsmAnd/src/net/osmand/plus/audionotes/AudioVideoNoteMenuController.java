package net.osmand.plus.audionotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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

	public AudioVideoNoteMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, final @NonNull Recording recording) {
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
							MapActivity activity = getMapActivity();
							if (activity != null) {
								mPlugin.playRecording(activity, getRecording());
							}
						}
					}
				}
			};

			rightTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						AlertDialog.Builder bld = new AlertDialog.Builder(activity);
						bld.setMessage(R.string.recording_delete_confirm);
						bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								MapActivity a = getMapActivity();
								if (mPlugin != null && a != null) {
									mPlugin.deleteRecording(getRecording(), true);
									a.getContextMenu().close();
								}
							}
						});
						bld.setNegativeButton(R.string.shared_string_no, null);
						bld.show();
					}
				}
			};
			rightTitleButtonController.caption = mapActivity.getString(R.string.shared_string_delete);
			rightTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;
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
		int iconId = AudioVideoNotesPlugin.getIconIdForRecordingFile(mRecording.getFile());
		if (iconId == -1) {
			iconId = R.drawable.ic_action_photo_dark;
		}
		return getIcon(iconId, R.color.audio_video_icon_color);
	}

	@NonNull
	@Override
	public String getNameStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (mIsFileAvailable) {
				return mRecording.getName(mapActivity, false);
			} else {
				return mapActivity.getString(R.string.data_is_not_available);
			}
		} else {
			return "";
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (mIsFileAvailable) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				return mRecording.getType(mapActivity);
			} else {
				return "";
			}
		} else {
			return super.getTypeStr();
		}
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.audionotes_plugin_name);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void updateData() {
		super.updateData();

		MapActivity mapActivity = getMapActivity();
		if (!mIsFileAvailable || mapActivity == null) {
			return;
		}
		boolean accessibilityEnabled = mapActivity.getMyApplication().accessibilityEnabled();
		rightTitleButtonController.visible = true;
		if (!mRecording.isPhoto()) {
			if (mPlugin.isPlaying(mRecording)) {
				leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_control_stop);
				leftTitleButtonController.startIconId = R.drawable.ic_action_rec_stop;
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
				leftTitleButtonController.caption = mapActivity.getString(R.string.recording_context_menu_play);
				leftTitleButtonController.startIconId = R.drawable.ic_play_dark;
				String durationStr = mRecording.getPlainDuration(accessibilityEnabled);
				leftTitleButtonController.needRightText = true;
				leftTitleButtonController.rightTextCaption = "— " + durationStr;
			}
		} else {
			leftTitleButtonController.caption = mapActivity.getString(R.string.recording_context_menu_show);
			leftTitleButtonController.startIconId = R.drawable.ic_action_view;
		}
	}

	@Override
	public void share(LatLon latLon, String title, String address) {
		MapActivity mapActivity = getMapActivity();
		if (mIsFileAvailable && mapActivity != null) {
			String path = mRecording.getFile().getAbsolutePath();
			MediaScannerConnection.scanFile(mapActivity, new String[]{path},
					null, new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) {
							MapActivity activity = getMapActivity();
							if (activity != null) {
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
								activity.startActivity(Intent.createChooser(shareIntent,
										activity.getString(R.string.share_note)));
							}
						}
					});
		} else {
			super.share(latLon, title, "");
		}
	}
}
