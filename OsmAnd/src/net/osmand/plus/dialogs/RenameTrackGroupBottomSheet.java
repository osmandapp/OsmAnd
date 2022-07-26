package net.osmand.plus.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.UpdateGpxCategoryTask;
import net.osmand.plus.myplaces.UpdateGpxCategoryTask.UpdateGpxListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;

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
		GPXFile gpxFile = group.getGpxFile();
		PointsGroup pointsGroup = gpxFile.getPointsGroups().get(group.getName());
		if (pointsGroup != null) {
			updateGpx(gpxFile, pointsGroup);
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnGroupNameChangeListener) {
			((OnGroupNameChangeListener) fragment).onTrackGroupChanged();
		}
		dismiss();
	}

	private void updateGpx(@NonNull GPXFile gpxFile, @NonNull PointsGroup pointsGroup) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			UpdateGpxListener listener = getUpdateGpxListener(mapActivity);
			PointsGroup newGroup = new PointsGroup(groupName, pointsGroup.iconName,
					pointsGroup.backgroundType, pointsGroup.color);

			UpdateGpxCategoryTask task = new UpdateGpxCategoryTask(mapActivity, gpxFile, pointsGroup.name,
					newGroup, listener, false);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private UpdateGpxListener getUpdateGpxListener(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
		return exception -> {
			if (exception == null) {
				MapActivity activity = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					TrackMenuFragment fragment = activity.getTrackMenuFragment();
					if (fragment != null) {
						fragment.updateContent();
					}
				}
			} else {
				LOG.error(exception);
			}
		};
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
			fragment.show(fragmentManager, TAG);
		}
	}
}