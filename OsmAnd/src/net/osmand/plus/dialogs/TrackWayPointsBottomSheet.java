package net.osmand.plus.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.EditTrackGroupDialogFragment.UpdateGpxCategoryTask;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class TrackWayPointsBottomSheet extends RenameFileBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(TrackWayPointsBottomSheet.class);
	private static final String TAG = TrackWayPointsBottomSheet.class.getName();
	private boolean isRename;
	private GpxDisplayGroup group;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		selectedFileName = group.getName();
		View view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.title_with_desc, null);
		BaseBottomSheetItem titleWithDescr = new BottomSheetItemWithDescription.Builder()
				.setDescription(isRename ? null : getString(R.string.please_provide_group_name_message))
				.setTitle(getString(isRename ? R.string.shared_string_rename : R.string.copy_to_map_favorites))
				.setCustomView(view)
				.create();

		items.add(titleWithDescr);
		View mainView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		nameTextBox = setupTextBox(mainView);
		editText = setupEditText(mainView);

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(editFolderName);
	}

	@Override
	protected TextInputEditText setupEditText(View mainView) {
		TextInputEditText editText = mainView.findViewById(R.id.name_edit_text);
		String groupName = Algorithms.isEmpty(group.getName()) ? app.getString(R.string.shared_string_gpx_points) : group.getName();
		editText.setText(groupName);
		if (isRename) {
			editText.requestFocus();
		}
		return editText;
	}

	@Override
	public void onRightBottomButtonClick() {
		if (isRename) {
			renameGroupName();
		} else {
			copyToFavorites();
		}
	}

	private void copyToFavorites() {
		String category = editText.getText().toString();
		FavouritesDbHelper favouritesDbHelper = app.getFavorites();
		for (GpxDisplayItem item : group.getModifiableList()) {
			if (item.locationStart != null) {
				FavouritePoint fp = FavouritePoint.fromWpt(item.locationStart, app, category);
				if (!Algorithms.isEmpty(item.description)) {
					fp.setDescription(item.description);
				}
				favouritesDbHelper.addFavourite(fp, false);
			}
		}
		favouritesDbHelper.saveCurrentPointsIntoFile();
		dismiss();
	}

	private void renameGroupName() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String newName = editText.getText().toString();
			boolean nameChanged = !Algorithms.objectEquals(group.getName(), newName);
			if (nameChanged) {
				new UpdateGpxCategoryTask(activity, group, newName)
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				dismiss();
			}
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return isRename ? R.string.shared_string_rename : R.string.shared_string_copy;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull GpxDisplayGroup group,
	                                boolean isRename) {
		if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(TrackWayPointsBottomSheet.TAG) == null) {
			TrackWayPointsBottomSheet fragment = new TrackWayPointsBottomSheet();
			fragment.group = group;
			fragment.isRename = isRename;
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TrackWayPointsBottomSheet.TAG);
		}
	}
}