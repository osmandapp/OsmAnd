package net.osmand.plus.track.helpers.folder;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

public class TrackFolderOptionDialogs {

	public static void showRenameFolderDialog(
			@NonNull FragmentActivity activity, @NonNull TrackFolder trackFolder,
			@NonNull TrackFolderHelper helper, boolean nightMode
	) {
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(R.string.shared_string_rename)
				.setControlsColor(trackFolder.getColor())
				.setNegativeButton(R.string.shared_string_cancel, null);
		dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				EditText editText = (EditText) extra;
				String newName = editText.getText().toString();
				if (!Algorithms.isEmpty(newName)) {
					helper.renameTrackFolder(trackFolder, newName);
				} else {
					// show some toast message to indicate about wrong name
				}
			}
		});
		String caption = activity.getString(R.string.enter_new_name);
		CustomAlert.showInput(dialogData, activity, trackFolder.getOriginalName(), caption);
	}

	public static void showDeleteFolderDialog(
			@NonNull Context ctx, @NonNull TrackFolder trackFolder,
			@NonNull TrackFolderHelper helper, boolean nightMode
	) {
		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(R.string.shared_string_delete_folder_q)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					helper.deleteTrackFolder(trackFolder);
				});
		String folderName = trackFolder.getName();
		String tracksCount = String.valueOf(trackFolder.getTotalTracksCount());
		String message = ctx.getString(R.string.delete_track_folder_dialog_message, folderName, tracksCount);
		CustomAlert.showSimpleMessage(dialogData, message);
	}

}
