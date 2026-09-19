package net.osmand.plus.dialogs;

import static net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType.TRACK_POINTS;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType.TRACK_ROUTE_POINTS;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.primitives.Route;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask.UpdateGpxListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;

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
		GpxFile gpxFile = group.getGpxFile();
		GpxDisplayItemType type = group.getType();
		if (TRACK_POINTS == type) {
			PointsGroup pointsGroup = gpxFile.getPointsGroups().get(group.getName());
			if (pointsGroup != null) {
				updateGpx(gpxFile, pointsGroup);
			}
		} else if (TRACK_ROUTE_POINTS == type) {
			Route route = gpxFile.getRouteByName(group.getDescription());
			if (route != null && !Algorithms.stringsEqual(route.getName(), groupName)) {
				route.setName(groupName);
				SaveGpxHelper.saveGpx(gpxFile);
			}
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnGroupNameChangeListener) {
			((OnGroupNameChangeListener) fragment).onTrackGroupChanged();
		}
		dismiss();
	}

	private void updateGpx(@NonNull GpxFile gpxFile, @NonNull PointsGroup group) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			UpdateGpxListener listener = getUpdateGpxListener(mapActivity);
			PointsGroup newGroup = new PointsGroup(groupName, group.getIconName(), group.getBackgroundType(), group.getColor());
			Map<String, PointsGroup> groups = Collections.singletonMap(group.getName(), newGroup);

			UpdatePointsGroupsTask task = new UpdatePointsGroupsTask(mapActivity, gpxFile, groups, listener);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private UpdateGpxListener getUpdateGpxListener(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
		return exception -> {
			if (exception == null) {
				MapActivity activity = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					TrackMenuFragment fragment = activity.getFragmentsHelper().getTrackMenuFragment();
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