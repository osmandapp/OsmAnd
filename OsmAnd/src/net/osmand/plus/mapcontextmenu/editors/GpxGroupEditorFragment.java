package net.osmand.plus.mapcontextmenu.editors;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask.UpdateGpxListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GpxGroupEditorFragment extends GroupEditorFragment {

	private static final Log LOG = PlatformUtil.getLog(GpxGroupEditorFragment.class);

	private GPXFile gpxFile;
	private final Map<String, PointsGroup> pointsGroups = new LinkedHashMap<>();

	@ColorInt
	@Override
	public int getDefaultColor() {
		return ContextCompat.getColor(app, R.color.gpx_color_point);
	}

	@Nullable
	@Override
	protected PointEditor getEditor() {
		return requireMapActivity().getContextMenu().getWptPtPointEditor();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pointsGroups.putAll(gpxFile.getPointsGroups());
	}

	@Override
	protected boolean isCategoryExists(@NonNull String name) {
		for (PointsGroup group : pointsGroups.values()) {
			if (group.name.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void addNewGroup() {
		pointsGroup = new PointsGroup(groupName, getIconName(), getBackgroundType().getTypeName(), getColor());
	}

	@Override
	public void editPointsGroup(boolean updatePointsAppearance) {
		MapActivity mapActivity = getMapActivity();
		if (pointsGroup != null && mapActivity != null) {
			UpdateGpxListener listener = getUpdateGpxListener(mapActivity);
			String backgroundType = getBackgroundType().getTypeName();
			PointsGroup newGroup = new PointsGroup(groupName, getIconName(), backgroundType, getColor());
			Map<String, PointsGroup> groups = Collections.singletonMap(pointsGroup.name, newGroup);

			UpdatePointsGroupsTask task = new UpdatePointsGroupsTask(mapActivity, gpxFile, groups, listener);
			task.setUpdatePointsAppearance(updatePointsAppearance);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		dismiss();
	}

	@NonNull
	private UpdateGpxListener getUpdateGpxListener(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
		return exception -> {
			saved = true;
			if (exception == null) {
				MapActivity activity = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
					TrackMenuFragment fragment = activity.getFragmentsHelper().getTrackMenuFragment();
					if (fragment != null) {
						fragment.onPointGroupsVisibilityChanged();
					}
				}
			} else {
				LOG.error(exception);
			}
		};
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @Nullable GPXFile gpxFile,
	                                @Nullable PointsGroup pointsGroup,
	                                @Nullable CategorySelectionListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			GpxGroupEditorFragment fragment = new GpxGroupEditorFragment();
			fragment.gpxFile = gpxFile;
			fragment.pointsGroup = pointsGroup;
			fragment.listener = listener;
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
