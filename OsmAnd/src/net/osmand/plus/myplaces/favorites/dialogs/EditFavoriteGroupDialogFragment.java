package net.osmand.plus.myplaces.favorites.dialogs;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapcontextmenu.editors.FavoriteAppearanceFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

public class EditFavoriteGroupDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = EditFavoriteGroupDialogFragment.class.getSimpleName();
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
		items.add(new TitleItem(Algorithms.isEmpty(group.getName()) ? getString(R.string.shared_string_favorites) : group.getName()));

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
								updateParentFragment();
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
						if (fragment instanceof FavoritesTreeFragment) {
							FavoriteAppearanceFragment.showInstance(manager, pointsGroup, fragment);
							dismiss();
						}
					}
				}))
				.create();
		items.add(changeColorItem);

		BaseBottomSheetItem showOnMapItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(group.isVisible())
				.setIcon(getContentIcon(R.drawable.ic_map))
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(v -> {
					boolean visible = !group.isVisible();
					helper.updateGroupVisibility(group, visible, true);
					updateParentFragment();
					dismiss();
				})
				.create();
		items.add(showOnMapItem);

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

			Drawable shareIcon = getContentIcon(R.drawable.ic_action_gshare_dark);
			if (shareIcon != null) {
				shareIcon = AndroidUtils.getDrawableForDirection(app, shareIcon);
			}
			BaseBottomSheetItem shareItem = new SimpleBottomSheetItem.Builder()
					.setIcon(shareIcon)
					.setTitle(getString(R.string.shared_string_share))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(view -> {
						FavoritesTreeFragment fragment = getFavoritesTreeFragment();
						if (fragment != null) {
							fragment.shareFavorites(group);
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
						updateParentFragment();
						dismiss();
					});
					b.show();
				})
				.create();
		items.add(deleteItem);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (group == null) {
			dismiss();
		}
	}

	@Nullable
	private FavoritesTreeFragment getFavoritesTreeFragment() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof FavoritesTreeFragment) {
			return (FavoritesTreeFragment) fragment;
		}
		return null;
	}

	private void updateParentFragment() {
		FavoritesTreeFragment fragment = getFavoritesTreeFragment();
		if (fragment != null) {
			fragment.reloadData();
		}
	}

	public static void showInstance(FragmentManager fragmentManager, String groupName) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			EditFavoriteGroupDialogFragment fragment = new EditFavoriteGroupDialogFragment();
			Bundle args = new Bundle();
			args.putString(GROUP_NAME_KEY, groupName);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}
}
