package net.osmand.plus.track.helpers.folder;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_PATH_NAME_CHARACTERS;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackFolderOptionDialogs {

	public static void showRenameFolderDialog(@NonNull FragmentActivity activity, @NonNull TrackFolder folder,
	                                          @NonNull TrackFolderHelper helper, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(R.string.shared_string_rename)
				.setControlsColor(folder.getColor())
				.setNegativeButton(R.string.shared_string_cancel, null);
		dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				EditText editText = (EditText) extra;
				String newName = editText.getText().toString();

				if (Algorithms.isBlank(newName)) {
					app.showToastMessage(R.string.empty_filename);
				} else if (ILLEGAL_PATH_NAME_CHARACTERS.matcher(newName).find()) {
					app.showToastMessage(R.string.file_name_containes_illegal_char);
				} else {
					File destFolder = new File(folder.getDirFile().getParentFile(), newName);
					if (destFolder.exists()) {
						app.showToastMessage(R.string.file_with_name_already_exist);
					} else {
						helper.renameTrackFolder(folder, newName);
					}
				}
			}
		});
		String caption = activity.getString(R.string.enter_new_name);
		CustomAlert.showInput(dialogData, activity, folder.getDirName(), caption);
	}

	public static void showDeleteFolderDialog(@NonNull Context ctx, @NonNull TrackFolder trackFolder,
	                                          @NonNull TrackFolderHelper helper, boolean nightMode) {
		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(R.string.delete_folder_question)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> helper.deleteTrackFolder(trackFolder));
		String folderName = trackFolder.getName(ctx);
		String tracksCount = String.valueOf(trackFolder.getTotalTracksCount());
		String message = ctx.getString(R.string.delete_track_folder_dialog_message, folderName, tracksCount);
		CustomAlert.showSimpleMessage(dialogData, message);
	}
}
