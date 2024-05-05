package net.osmand.plus.track.fragments;

import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.OptionsDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dialogs.CopyTrackGroupToFavoritesBottomSheet;
import net.osmand.plus.dialogs.EditTrackGroupBottomSheet.OnGroupNameChangeListener;
import net.osmand.plus.dialogs.RenameTrackGroupBottomSheet;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.editors.GpxGroupEditorFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.DisplayGroupsBottomSheet.DisplayPointGroupsCallback;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EditTrackGroupDialogFragment extends MenuBottomSheetDialogFragment implements OnPointsDeleteListener, OnGroupNameChangeListener {


	public static final String TAG = EditTrackGroupDialogFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapMarkersHelper mapMarkersHelper;
	private GpxSelectionHelper selectedGpxHelper;

	private GPXFile gpxFile;
	private PointsGroup pointsGroup;
	private GpxDisplayGroup displayGroup;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = requiredMyApplication();
		mapMarkersHelper = app.getMapMarkersHelper();
		selectedGpxHelper = app.getSelectedGpxHelper();

		if (displayGroup != null) {
			gpxFile = displayGroup.getGpxFile();
			pointsGroup = gpxFile.getPointsGroups().get(displayGroup.getName());
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (displayGroup == null) {
			return;
		}

		items.add(new TitleItem(getCategoryName(app, displayGroup.getName())));

		SelectedGpxFile selectedGpxFile;
		if (gpxFile.showCurrentTrack) {
			selectedGpxFile = selectedGpxHelper.getSelectedCurrentRecordingTrack();
		} else {
			selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
		}
		boolean trackPoints = displayGroup.getType() == GpxDisplayItemType.TRACK_POINTS;
		if (trackPoints && selectedGpxFile != null) {
			items.add(createShowOnMapItem(selectedGpxFile));
		}
		items.add(createEditNameItem());
		if (trackPoints) {
			items.add(createChangeColorItem());
		}
		items.add(new OptionsDividerItem(app));

		if (!gpxFile.showCurrentTrack) {
			items.add(createCopyToMarkersItem());
		}
		items.add(createCopyToFavoritesItem());
		items.add(new OptionsDividerItem(app));

		items.add(createDeleteGroupItem());
	}

	@NonNull
	private BaseBottomSheetItem createShowOnMapItem(@NonNull SelectedGpxFile selectedGpxFile) {
		boolean checked = !selectedGpxFile.isGroupHidden(displayGroup.getName());
		ApplicationMode mode = app.getSettings().getApplicationMode();
		BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColor(mode.getProfileColor(nightMode))
				.setChecked(checked)
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setCustomView(getCustomButtonView(app, mode, checked, nightMode))
				.setOnClickListener(v -> {
					boolean visible = !showOnMapItem[0].isChecked();
					if (pointsGroup != null) {
						pointsGroup.setHidden(!visible);
					}
					showOnMapItem[0].setChecked(visible);
					updateCustomButtonView(app, mode, v, visible, nightMode);

					Fragment fragment = getTargetFragment();
					if (fragment instanceof DisplayPointGroupsCallback) {
						((DisplayPointGroupsCallback) fragment).onPointGroupsVisibilityChanged();
					}
				})
				.create();
		return showOnMapItem[0];
	}

	private void updateGroupVisibility() {
		MapActivity activity = (MapActivity) getActivity();
		if (activity != null) {
			Map<String, PointsGroup> groups = Collections.singletonMap(pointsGroup.name, pointsGroup);
			UpdatePointsGroupsTask task = new UpdatePointsGroupsTask(activity, gpxFile, groups, null);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@NonNull
	private BaseBottomSheetItem createEditNameItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_name_field))
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						RenameTrackGroupBottomSheet.showInstance(manager, EditTrackGroupDialogFragment.this, displayGroup);
					}
				})
				.create();
	}

	@NonNull
	private BaseBottomSheetItem createCopyToMarkersItem() {
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(gpxFile);
		boolean synced = markersGroup != null && (Algorithms.isEmpty(markersGroup.getWptCategories())
				|| markersGroup.getWptCategories().contains(displayGroup.getName()));

		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(synced ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_copy))
				.setTitle(getString(synced ? R.string.remove_group_from_markers : R.string.add_group_to_markers))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					updateGroupWptCategory(synced);
					dismiss();
				})
				.create();
	}

	private void updateGroupWptCategory(boolean synced) {
		SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
		if (selectedGpxFile == null) {
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.showOnMap().selectedAutomatically().saveSelection();
			selectedGpxHelper.selectGpxFile(gpxFile, params);
		}
		boolean groupCreated = false;
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(gpxFile);
		if (markersGroup == null) {
			groupCreated = true;
			markersGroup = mapMarkersHelper.addOrEnableGroup(gpxFile);
		}
		Set<String> categories = markersGroup.getWptCategories();
		Set<String> selectedCategories = new HashSet<>();
		if (categories != null) {
			selectedCategories.addAll(categories);
		}
		if (synced) {
			selectedCategories.remove(displayGroup.getName());
		} else {
			selectedCategories.add(displayGroup.getName());
		}
		if (Algorithms.isEmpty(selectedCategories)) {
			mapMarkersHelper.removeMarkersGroup(markersGroup);
		} else {
			mapMarkersHelper.updateGroupWptCategories(markersGroup, selectedCategories);
			if (!groupCreated) {
				mapMarkersHelper.runSynchronization(markersGroup);
			}
		}
	}

	@NonNull
	private BaseBottomSheetItem createCopyToFavoritesItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setTitle(getString(R.string.copy_to_map_favorites))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						CopyTrackGroupToFavoritesBottomSheet.showInstance(manager, EditTrackGroupDialogFragment.this, displayGroup);
					}
				})
				.create();
	}

	@NonNull
	private BaseBottomSheetItem createDeleteGroupItem() {
		String delete = app.getString(R.string.shared_string_delete);
		Typeface typeface = FontCache.getRobotoMedium(app);
		return new SimpleBottomSheetItem.Builder()
				.setTitleColorId(R.color.color_osm_edit_delete)
				.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete))
				.setTitle(UiUtilities.createCustomFontSpannable(typeface, delete, delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						showDeleteConfirmationDialog(activity);
					}
				})
				.create();
	}

	private void showDeleteConfirmationDialog(@NonNull FragmentActivity activity) {
		Context themedCtx = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder b = new AlertDialog.Builder(themedCtx);
		b.setTitle(app.getString(R.string.are_you_sure));
		b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> deleteGroupItems());
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	private void deleteGroupItems() {
		Set<GpxDisplayItem> items = new HashSet<>(displayGroup.getDisplayItems());
		new DeletePointsTask(app, gpxFile, items, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	private BaseBottomSheetItem createChangeColorItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
				.setTitle(getString(R.string.change_default_appearance))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						GpxGroupEditorFragment.showInstance(manager, gpxFile, pointsGroup, null);
					}
					dismiss();
				}).create();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (displayGroup == null) {
			dismiss();
		}
	}

	@Override
	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			updateGroupVisibility();
		}
		super.onDestroy();
	}

	@Override
	public void onPointsDeleted() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TrackMenuFragment) {
			((TrackMenuFragment) fragment).updateContent();
		}
		dismiss();
	}

	@Override
	public void onTrackGroupChanged() {
		dismiss();
	}

	@NonNull
	public static String getCategoryName(@NonNull Context ctx, @Nullable String category) {
		return Algorithms.isEmpty(category) ? ctx.getString(R.string.shared_string_waypoints) : category;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull GpxDisplayGroup displayGroup, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditTrackGroupDialogFragment fragment = new EditTrackGroupDialogFragment();
			fragment.displayGroup = displayGroup;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
