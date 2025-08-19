package net.osmand.plus.track.fragments;

import static net.osmand.data.PointDescription.POINT_TYPE_WPT;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType.TRACK_POINTS;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.OptionsDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dialogs.EditTrackGroupBottomSheet.OnTrackGroupChangeListener;
import net.osmand.plus.dialogs.RenameTrackGroupBottomSheet;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapcontextmenu.editors.AddToFavoritesBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.GpxGroupEditorFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.DisplayGroupsBottomSheet.DisplayPointGroupsCallback;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditTrackGroupDialogFragment extends MenuBottomSheetDialogFragment implements OnPointsDeleteListener, OnTrackGroupChangeListener {

	private static final String TAG = EditTrackGroupDialogFragment.class.getSimpleName();

	private MapMarkersHelper mapMarkersHelper;
	private GpxSelectionHelper selectedGpxHelper;

	private GpxFile gpxFile;
	private PointsGroup pointsGroup;
	private GpxDisplayGroup displayGroup;
	private boolean groupHidden;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mapMarkersHelper = app.getMapMarkersHelper();
		selectedGpxHelper = app.getSelectedGpxHelper();

		if (displayGroup != null) {
			gpxFile = displayGroup.getGpxFile();
			pointsGroup = gpxFile.getPointsGroups().get(displayGroup.getName());
			groupHidden = pointsGroup != null && pointsGroup.isHidden();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (displayGroup == null) return;

		items.add(new TitleItem(getCategoryName(app, displayGroup.getName())));

		SelectedGpxFile selectedGpxFile;
		if (gpxFile.isShowCurrentTrack()) {
			selectedGpxFile = selectedGpxHelper.getSelectedCurrentRecordingTrack();
		} else {
			selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.getPath());
		}
		boolean trackPoints = displayGroup.getType() == TRACK_POINTS;
		if (trackPoints && selectedGpxFile != null) {
			items.add(createShowOnMapItem(selectedGpxFile));
		}
		items.add(createEditNameItem());
		if (trackPoints) {
			items.add(createChangeColorItem());
		}
		items.add(new OptionsDividerItem(app));

		boolean currentTrack = gpxFile.isShowCurrentTrack();
		if (!currentTrack && trackPoints) {
			items.add(createCopyToMarkersItem());
		}
		items.add(createAddToFavorites());
		if (!currentTrack) {
			items.add(createAddToNavigationItem());
		}
		items.add(new OptionsDividerItem(app));

		items.add(createDeleteGroupItem());
	}

	@NonNull
	private BaseBottomSheetItem createShowOnMapItem(@NonNull SelectedGpxFile selectedGpxFile) {
		Context context = requireContext();
		boolean checked = !selectedGpxFile.isGroupHidden(displayGroup.getName());
		ApplicationMode appMode = getAppMode();
		BottomSheetItemWithCompoundButton[] showOnMapItem = new BottomSheetItemWithCompoundButton[1];
		showOnMapItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColor(appMode.getProfileColor(nightMode))
				.setChecked(checked)
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setCustomView(getCustomButtonView(context, appMode, checked, nightMode))
				.setOnClickListener(v -> {
					boolean visible = !showOnMapItem[0].isChecked();
					if (pointsGroup != null) {
						pointsGroup.setHidden(!visible);
					}
					showOnMapItem[0].setChecked(visible);
					updateCustomButtonView(context, appMode, v, visible, nightMode);

					Fragment fragment = getTargetFragment();
					if (fragment instanceof DisplayPointGroupsCallback) {
						((DisplayPointGroupsCallback) fragment).onPointGroupsVisibilityChanged();
					}
				})
				.create();
		return showOnMapItem[0];
	}

	private void updateGroupVisibility() {
		callMapActivity(mapActivity -> {
			if (pointsGroup != null && groupHidden != pointsGroup.isHidden()) {
				Map<String, PointsGroup> groups = Collections.singletonMap(pointsGroup.getName(), pointsGroup);
				UpdatePointsGroupsTask task = new UpdatePointsGroupsTask(mapActivity, gpxFile, groups, null);
				OsmAndTaskManager.executeTask(task);
			}
		});
	}

	@NonNull
	private BaseBottomSheetItem createEditNameItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_name_field))
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> callActivity(activity -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					RenameTrackGroupBottomSheet.showInstance(manager, EditTrackGroupDialogFragment.this, displayGroup);
				}))
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
		SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByPath(gpxFile.getPath());
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
	private BaseBottomSheetItem createAddToFavorites() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setTitle(getString(R.string.add_to_favorites))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> callActivity(activity -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					AddToFavoritesBottomSheet.showInstance(manager, EditTrackGroupDialogFragment.this, displayGroup);
					dismiss();
				}))
				.create();
	}

	@NonNull
	private BaseBottomSheetItem createDeleteGroupItem() {
		String delete = app.getString(R.string.shared_string_delete);
		return new SimpleBottomSheetItem.Builder()
				.setTitleColorId(R.color.color_osm_edit_delete)
				.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete))
				.setTitle(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), delete, delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> callActivity(this::showDeleteConfirmationDialog))
				.create();
	}

	private void showDeleteConfirmationDialog(@NonNull FragmentActivity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext());
		builder.setTitle(getString(R.string.are_you_sure));
		builder.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> deleteGroupItems());
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void deleteGroupItems() {
		Set<GpxDisplayItem> items = new HashSet<>(displayGroup.getDisplayItems());
		OsmAndTaskManager.executeTask(new DeletePointsTask(app, gpxFile, items, this));
	}

	@NonNull
	private BaseBottomSheetItem createChangeColorItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
				.setTitle(getString(R.string.change_default_appearance))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					callActivity(activity -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						GpxGroupEditorFragment.showInstance(manager, gpxFile, pointsGroup, null);
					});
					dismiss();
				}).create();
	}

	@NonNull
	private BaseBottomSheetItem createAddToNavigationItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark))
				.setTitle(getString(R.string.add_to_navigation))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> addToNavigation()).create();
	}

	private void addToNavigation() {
		List<GpxDisplayItem> displayItems = displayGroup.getDisplayItems();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		if (!Algorithms.isEmpty(displayItems)) {
			int i = 0;
			GpxDisplayItem item = displayItems.get(i++);
			if (item.locationStart != null) {
				LatLon latLon = new LatLon(item.locationStart.getLat(), item.locationStart.getLon());
				targetPointsHelper.setStartPoint(latLon, false, new PointDescription(POINT_TYPE_WPT, item.name));
			}

			List<TargetPoint> targetPoints = new ArrayList<>();
			for (int k = i; k < displayItems.size(); k++) {
				GpxDisplayItem displayItem = displayItems.get(k);
				if (item.locationStart != null) {
					LatLon latLon = new LatLon(displayItem.locationStart.getLatitude(), displayItem.locationStart.getLongitude());
					TargetPoint point = new TargetPoint(latLon, new PointDescription(POINT_TYPE_WPT, displayItem.name));
					targetPoints.add(point);
				}
			}
			RoutingHelper routingHelper = app.getRoutingHelper();
			boolean updateRoute = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode();
			targetPointsHelper.reorderAllTargetPoints(targetPoints, updateRoute);
		} else {
			targetPointsHelper.clearStartPoint(false);
			targetPointsHelper.clearPointToNavigate(false);
		}
		dismissAll();
		app.getOsmandMap().getMapActions().doRoute();
	}

	private void dismissAll() {
		if (getTargetFragment() instanceof TrackMenuFragment fragment) {
			fragment.dismiss();
		}
		dismiss();
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
		if (getTargetFragment() instanceof TrackMenuFragment fragment) {
			fragment.updateContent();
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

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull GpxDisplayGroup group, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditTrackGroupDialogFragment fragment = new EditTrackGroupDialogFragment();
			fragment.displayGroup = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
