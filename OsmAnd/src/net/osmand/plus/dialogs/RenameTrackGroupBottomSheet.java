package net.osmand.plus.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.EditTrackGroupDialogFragment;
import net.osmand.plus.myplaces.UpdateGpxCategoryTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

public class RenameTrackGroupBottomSheet extends EditTrackGroupBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(RenameTrackGroupBottomSheet.class);
	private static final String TAG = RenameTrackGroupBottomSheet.class.getName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleWithDescr = new BottomSheetItemWithDescription.Builder()
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.title_with_desc)
				.create();
		items.add(titleWithDescr);
		super.createMenuItems(savedInstanceState);
	}

	@Override
	protected void setupEditText(View mainView) {
		super.setupEditText(mainView);
		editText.requestFocus();
	}

	@Override
	public void onRightBottomButtonClick() {
		renameGroupName();
	}

	private void renameGroupName() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			new UpdateGpxCategoryTask(activity, group, groupName)
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		listener.onTrackGroupChanged();
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_rename;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull GpxDisplayGroup group) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			RenameTrackGroupBottomSheet fragment = new RenameTrackGroupBottomSheet();
			fragment.group = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			if (target instanceof EditTrackGroupDialogFragment) {
				fragment.listener = (OnGroupNameChangeListener) target;
			}
			fragment.show(fragmentManager, RenameTrackGroupBottomSheet.TAG);
		}
	}
}