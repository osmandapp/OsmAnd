package net.osmand.plus.myplaces.favorites.dialogs;

import android.app.Activity;
import android.content.Context;
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
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collections;

public class FavoriteOptionsDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = FavoriteOptionsDialogFragment.class.getSimpleName();
	public static final String GROUP_NAME_KEY = "group_name_key";

	private FavouritesHelper helper;

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
			String groupName = args.getString(GROUP_NAME_KEY);
			if (groupName != null) {
				group = helper.getGroup(groupName);
			}
		}
		if (group == null) {
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
		boolean visible = group.isVisible();

		title.setText(group.getDisplayName(app));
		title.setMaxLines(2);
		description.setText(GpxUiHelper.getFavoriteFolderDescription(app, group));
		if (visible) {
			title.setTypeface(FontCache.getNormalFont());
		} else {
			title.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		}
		int color = group.getColor() == 0 ? getColor(R.color.color_favorite) : group.getColor();
		if (group.isVisible()) {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color));
		} else {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_hidden,
					ColorUtilities.getDefaultIconColor(app, nightMode)));
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

		BaseBottomSheetItem editNameItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
				.setTitle(getString(R.string.shared_string_rename))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					Activity activity = getActivity();
					if (activity != null) {
						Context themedContext = getThemedContext();
						AlertDialog.Builder b = new AlertDialog.Builder(themedContext);
						b.setTitle(R.string.favorite_category_name);
						EditText nameEditText = new EditText(themedContext);
						nameEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
						nameEditText.setText(group.getName());
						LinearLayout container = new LinearLayout(themedContext);
						int sidePadding = dpToPx(24f);
						int topPadding = dpToPx(4f);
						container.setPadding(sidePadding, topPadding, sidePadding, topPadding);
						container.addView(nameEditText);
						b.setView(container);
						b.setNegativeButton(R.string.shared_string_cancel, null);
						b.setPositiveButton(R.string.shared_string_save, (dialog, which) -> {
							String name = nameEditText.getText().toString();
							boolean nameChanged = !Algorithms.objectEquals(group.getName(), name);
							if (nameChanged) {
								helper.updateGroupName(group, name, true);
								updateAll();
							}
							dismiss();
						});
						b.show();
					}
				})
				.create();
		items.add(editNameItem);

		BaseBottomSheetItem changeColorItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
				.setTitle(getString(R.string.change_default_appearance))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> callActivity(activity -> {
					PointsGroup pointsGroup = group != null ? group.toPointsGroup(app) : null;
					FragmentManager manager = activity.getSupportFragmentManager();
					if (pointsGroup != null) {
						Fragment fragment = getParentFragment();
						if (fragment instanceof BaseFavoriteListFragment) {
							FavoriteAppearanceFragment.showInstance(manager, pointsGroup, fragment);
							dismiss();
						}
					}
				}))
				.create();
		items.add(changeColorItem);

		if (!group.getPoints().isEmpty()) {
			items.add(new DividerHalfItem(getContext()));

			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			FavoriteGroup favGroup = this.group;
			MapMarkersGroup markersGr = markersHelper.getMarkersGroup(this.group);
			boolean synced = markersGr != null;

			BaseBottomSheetItem markersGroupItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(synced ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_flag))
					.setTitle(getString(synced ? R.string.remove_from_map_markers : R.string.shared_string_add_to_map_markers))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(view -> {
						if (synced) {
							markersHelper.removeMarkersGroup(markersGr);
						} else {
							markersHelper.addOrEnableGroup(favGroup);
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
							gpxFile.addPointsGroup(group.toPointsGroup(app));
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
							fragment.shareFavorites(Collections.singletonList(group));
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
					b.setTitle(R.string.favorite_delete_group);
					String groupName = Algorithms.isEmpty(group.getName()) ? getString(R.string.shared_string_favorites) : group.getName();
					b.setMessage(getString(R.string.favorite_confirm_delete_group, groupName, group.getPoints().size()));
					b.setNeutralButton(R.string.shared_string_cancel, null);
					b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
						helper.deleteGroup(group, false);
						helper.saveCurrentPointsIntoFile(true);
						FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
						sortModesHelper.onFavoriteFolderDeleted(group);
						updateAll();
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
		if (group == null) {
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable String groupName) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			FavoriteOptionsDialogFragment fragment = new FavoriteOptionsDialogFragment();
			Bundle args = new Bundle();
			args.putString(GROUP_NAME_KEY, groupName);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}
}
