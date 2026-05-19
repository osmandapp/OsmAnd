package net.osmand.plus.myplaces.favorites.dialogs;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.FavoriteAppearanceFragment;
import net.osmand.plus.mapcontextmenu.editors.FavouriteGroupEditorFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.myplaces.favorites.FavoriteFolderPath;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collections;

public class FavoriteOptionsDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = FavoriteOptionsDialogFragment.class.getSimpleName();
	public static final String GROUP_NAME_KEY = "group_name_key";

	private FavouritesHelper helper;

	@Nullable
	private String folderPath;
	@Nullable
	private FavoriteFolder folder;
	@Nullable
	private FavoriteGroup group;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = app.getFavoritesHelper();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			folderPath = args.getString(GROUP_NAME_KEY);
			if (folderPath != null) {
				folder = helper.getFavoriteFolder(folderPath);
				group = folder != null ? folder.getGroup() : helper.getGroup(folderPath);
			}
		}
		if (folderPath == null || folder == null) {
			return;
		}
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View groupView = LayoutInflater.from(new ContextThemeWrapper(requireActivity(), themeRes))
				.inflate(R.layout.track_list_item, null, false);
		TextView title = groupView.findViewById(R.id.title);
		TextView description = groupView.findViewById(R.id.description);
		ImageView icon = groupView.findViewById(R.id.icon);
		View checkboxContainer = groupView.findViewById(R.id.checkbox_container);
		View menuButton = groupView.findViewById(R.id.menu_button);
		View divider = groupView.findViewById(R.id.divider);
		boolean visible = group == null || group.isVisible();

		title.setText(FavoriteFolderFormatter.getDisplayName(app, folderPath));
		title.setMaxLines(2);
		description.setText(group != null
				? GpxUiHelper.getFavoriteFolderDescription(app, group)
				: getVirtualFolderDescription(folder));
		if (visible) {
			title.setTypeface(FontCache.getNormalFont());
		} else {
			title.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		}
		int color = group == null || group.getColor() == 0 ? getColor(R.color.color_favorite) : group.getColor();
		int hiddenColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		if (group != null && group.isPinned()) {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_pin,
					group.isVisible() ? color : hiddenColor));
		} else if (visible) {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color));
		} else {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_hidden, hiddenColor));
		}
		AndroidUiHelper.updateVisibility(groupView.findViewById(R.id.direction_icon), false);
		AndroidUiHelper.updateVisibility(checkboxContainer, false);
		AndroidUiHelper.updateVisibility(menuButton, false);
		AndroidUiHelper.updateVisibility(divider, false);

		View contentContainer = groupView.findViewById(R.id.content_container);
		LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		newParams.setMargins(dpToPx(6), 0, 0, 0);
		contentContainer.setLayoutParams(newParams);

		BaseBottomSheetItem groupItem = new BaseBottomSheetItem.Builder().setCustomView(groupView).create();
		items.add(groupItem);
		items.add(new DividerItem(app));

		if (group != null) {
			BaseBottomSheetItem showOnMapItem = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(group.isVisible())
					.setIcon(getContentIcon(R.drawable.ic_map))
					.setTitle(getString(R.string.shared_string_show_on_map))
					.setLayoutId(R.layout.bottom_sheet_item_with_switch)
					.setOnClickListener(v -> {
						boolean shouldShowOnMap = !group.isVisible();
						helper.updateGroupVisibility(group, shouldShowOnMap, true);
						updateAll();
						dismiss();
					})
					.create();
			items.add(showOnMapItem);

			BaseBottomSheetItem pinItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(group.isPinned() ? R.drawable.ic_action_drawing_pin_disable : R.drawable.ic_action_drawing_pin))
					.setTitle(getString(group.isPinned() ? R.string.unpin_folder : R.string.pin_folder))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> {
						boolean shouldPin = !group.isPinned();
						helper.updateGroupPin(group, shouldPin, true);
						updateAll();
						dismiss();
					})
					.create();
			items.add(pinItem);
			items.add(new DividerHalfItem(getContext()));
		}

		BaseBottomSheetItem addFolderItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_folder_add_outlined))
				.setTitle(getString(R.string.add_new_folder))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> showAddFolderDialog())
				.create();
		items.add(addFolderItem);

		BaseBottomSheetItem editNameItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> showRenameDialog())
				.create();
		items.add(editNameItem);

		if (group != null) {
			BaseBottomSheetItem changeColorItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_appearance))
					.setTitle(getString(R.string.change_default_appearance))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> callActivity(activity -> {
						PointsGroup pointsGroup = group.toPointsGroup(app);
						FragmentManager manager = activity.getSupportFragmentManager();
						Fragment fragment = getParentFragment();
						if (fragment instanceof BaseFavoriteListFragment) {
							FavoriteAppearanceFragment.showInstance(manager, pointsGroup, fragment);
							dismiss();
						}
					}))
					.create();
			items.add(changeColorItem);
		}

		if (group != null && !group.getPoints().isEmpty()) {
			items.add(new DividerHalfItem(getContext()));

			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			FavoriteGroup favoriteGroup = group;
			MapMarkersGroup markersGr = markersHelper.getMarkersGroup(favoriteGroup);
			boolean synced = markersGr != null;

			BaseBottomSheetItem markersGroupItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(synced ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_flag))
					.setTitle(getString(synced ? R.string.remove_from_map_markers : R.string.shared_string_add_to_map_markers))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(view -> {
						if (synced) {
							markersHelper.removeMarkersGroup(markersGr);
						} else {
							markersHelper.addOrEnableGroup(favoriteGroup);
						}
						dismiss();
						MapActivity.launchMapActivityMoveToTop(requireActivity());
					})
					.create();
			items.add(markersGroupItem);

			BaseBottomSheetItem addToTrackGroupItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_add_to_track))
					.setTitle(getString(R.string.add_to_a_track))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(view -> {
						SelectTrackTabsFragment.GpxFileSelectionListener gpxFileSelectionListener = gpxFile -> {
							gpxFile.addPointsGroup(favoriteGroup.toPointsGroup(app));
							saveGpx(app, gpxFile);
							syncGpx(gpxFile);
						};
						dismiss();
						SelectTrackTabsFragment.showInstance(requireActionBarActivity().getSupportFragmentManager(), gpxFileSelectionListener);
					})
					.create();
			items.add(addToTrackGroupItem);

			Drawable shareIcon = getContentIcon(R.drawable.ic_action_gshare_dark);
			if (shareIcon != null) {
				shareIcon = AndroidUtils.getDrawableForDirection(app, shareIcon);
			}
			BaseBottomSheetItem shareItem = new SimpleBottomSheetItem.Builder()
					.setIcon(shareIcon)
					.setTitle(getString(R.string.shared_string_share))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(view -> {
						BaseFavoriteListFragment fragment = getFavoriteListFragment();
						if (fragment != null) {
							fragment.shareFavorites(Collections.singletonList(favoriteGroup));
						}
						dismiss();
					})
					.create();
			items.add(shareItem);
		}
		items.add(new DividerHalfItem(getContext()));

		String delete = getString(R.string.shared_string_delete);
		BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
				.setTitleColorId(R.color.color_osm_edit_delete)
				.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete))
				.setTitle(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), delete, delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					AlertDialog.Builder b = new AlertDialog.Builder(getThemedContext());
					b.setTitle(R.string.delete_folder);
					b.setMessage(getDeleteFolderMessage());
					b.setNeutralButton(R.string.shared_string_cancel, null);
					b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
						deleteFolder();
						dismiss();
					});
					b.show();
				})
				.create();
		items.add(deleteItem);
	}

	private void saveGpx(OsmandApplication app, GpxFile gpxFile) {
		SaveGpxHelper.saveGpx(new File(gpxFile.getPath()), gpxFile, errorMessage -> {
			if (errorMessage == null) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
			}
		});
	}

	private void syncGpx(GpxFile gpxFile) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(gpxFile);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (folder == null) {
			dismiss();
		}
	}

	@Nullable
	private BaseFavoriteListFragment getFavoriteListFragment() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof BaseFavoriteListFragment) {
			return (BaseFavoriteListFragment) fragment;
		}
		return null;
	}

	private void updateAll() {
		BaseFavoriteListFragment baseFavoriteListFragment = getFavoriteListFragment();
		if (baseFavoriteListFragment != null) {
			baseFavoriteListFragment.reloadData();
		}
	}

	@NonNull
	private String getVirtualFolderDescription(@Nullable FavoriteFolder folder) {
		int pointsCount = folder != null ? folder.getSubtreePointsCount() : 0;
		return pointsCount > 0
				? getString(R.string.gpx_selection_number_of_points, String.valueOf(pointsCount))
				: getString(R.string.shared_string_empty);
	}

	private void showAddFolderDialog() {
		callActivity(activity -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			FavouriteGroupEditorFragment.showInstance(manager, null, pointsGroup -> {
				FavoriteGroup createdGroup = helper.getGroup(pointsGroup.getName());
				if (createdGroup != null) {
					helper.saveSelectedGroupsIntoFile(Collections.singletonList(createdGroup), true);
				}
				updateAll();
			}, false, folderPath);
			dismiss();
		});
	}

	private void showRenameDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			int controlsColor = ColorUtilities.getDefaultIconColor(app, isNightMode());
			AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
					.setTitle(R.string.shared_string_rename)
					.setControlsColor(controlsColor)
					.setNegativeButton(R.string.shared_string_cancel, null);
			dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
				Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
				if (extra instanceof EditText editText) {
					String newName = editText.getText().toString().trim();
					if (renameFolder(newName)) {
						updateAll();
						dismiss();
					}
				}
			});
			String caption = activity.getString(R.string.favorite_folder_name);
			CustomAlert.showInput(dialogData, activity, FavoriteFolderFormatter.getDisplayName(app, folderPath), caption);
		}
	}

	private boolean renameFolder(@NonNull String newName) {
		if (folderPath == null || newName.isEmpty()) {
			return false;
		}
		boolean rootFolder = Algorithms.isEmpty(folderPath);
		String newSegment = rootFolder ? FavoriteGroup.convertDisplayNameToGroupIdName(app, newName) : newName;
		if (Algorithms.isEmpty(folderPath) && Algorithms.isEmpty(newSegment)) {
			return true;
		}
		if (!FavoriteFolderPath.isValidSegment(newSegment)) {
			app.showShortToastMessage(R.string.favorite_folder_invalid_name);
			return false;
		}
		String parentPath = FavoriteFolderPath.parentPath(folderPath);
		String newPath = Algorithms.isEmpty(parentPath)
				? newSegment
				: parentPath + FavoriteFolderPath.DELIMITER + newSegment;
		if (Algorithms.stringsEqual(folderPath, newPath)) {
			return true;
		}
		if (!rootFolder && helper.hasRenameFavoriteFolderSubtreeConflict(folderPath, newPath)) {
			app.showShortToastMessage(R.string.favorite_folder_rename_conflict);
			return false;
		}
		boolean renamed;
		if (rootFolder && group != null) {
			renamed = !helper.groupExists(newPath);
			if (renamed) {
				helper.updateGroupName(group, newPath, true);
			}
		} else {
			renamed = helper.renameFavoriteFolderSubtree(folderPath, newPath, true);
		}
		if (renamed) {
			app.getFavoriteSortModesHelper().clearRelatedKeys(folderPath);
			app.getFavoriteSortModesHelper().syncSettings();
			folderPath = newPath;
			folder = helper.getFavoriteFolder(newPath);
			group = folder != null ? folder.getGroup() : null;
		} else {
			app.showShortToastMessage(R.string.favorite_folder_rename_conflict);
		}
		return renamed;
	}

	@NonNull
	private String getDeleteFolderMessage() {
		if (Algorithms.isEmpty(folderPath) && group != null) {
			String groupName = getString(R.string.shared_string_favorites);
			return getString(R.string.favorite_confirm_delete_group, groupName, group.getPoints().size());
		}
		String name = FavoriteFolderFormatter.getBreadcrumb(app, folderPath);
		int pointsCount = folder != null ? folder.getSubtreePointsCount() : group != null ? group.getPoints().size() : 0;
		return getString(R.string.favorite_confirm_delete_folder, name, pointsCount);
	}

	private void deleteFolder() {
		if (folderPath == null) {
			return;
		}
		boolean deleted;
		FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
		if (Algorithms.isEmpty(folderPath) && group != null) {
			deleted = helper.deleteGroup(group, false);
			if (deleted) {
				sortModesHelper.clearRelatedKeys(group.getName());
			}
		} else {
			deleted = helper.deleteFavoriteFolderSubtree(folderPath, false);
			if (deleted) {
				sortModesHelper.clearRelatedKeys(folderPath);
			}
		}
		if (deleted) {
			sortModesHelper.syncSettings();
			helper.saveCurrentPointsIntoFile(true);
			updateAll();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable String folderPath) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			FavoriteOptionsDialogFragment fragment = new FavoriteOptionsDialogFragment();
			Bundle args = new Bundle();
			args.putString(GROUP_NAME_KEY, folderPath);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}
}
