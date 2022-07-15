package net.osmand.plus.myplaces.ui;

import static net.osmand.plus.myplaces.ui.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.TAB_ID;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

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
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class EditFavoriteGroupDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = EditFavoriteGroupDialogFragment.class.getSimpleName();
	public static final String GROUP_NAME_KEY = "group_name_key";

	private OsmandApplication app;
	private FavouritesHelper helper;

	private FavoriteGroup group;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = requiredMyApplication();
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
		items.add(new TitleItem(Algorithms.isEmpty(group.getName()) ? app.getString(R.string.shared_string_favorites) : group.getName()));

		BaseBottomSheetItem editNameItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
				.setTitle(getString(R.string.edit_name))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					Activity activity = getActivity();
					if (activity != null) {
						Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
						AlertDialog.Builder b = new AlertDialog.Builder(themedContext);
						b.setTitle(R.string.favorite_category_name);
						EditText nameEditText = new EditText(themedContext);
						nameEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
						nameEditText.setText(group.getName());
						LinearLayout container = new LinearLayout(themedContext);
						int sidePadding = AndroidUtils.dpToPx(activity, 24f);
						int topPadding = AndroidUtils.dpToPx(activity, 4f);
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
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						Bundle bundle = new Bundle();
						Bundle prevParams = new Bundle();

						bundle.putString(GROUP_NAME_KEY, group.getName());
						prevParams.putInt(TAB_ID, FAV_TAB);

						MapActivity.launchMapActivityMoveToTop(activity, prevParams, null, bundle);
					}
				})
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

		if (group.getPoints().size() > 0) {
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
	}

	@Override
	public void onResume() {
		super.onResume();
		if (group == null) {
			dismiss();
		}
	}

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
		EditFavoriteGroupDialogFragment f = new EditFavoriteGroupDialogFragment();
		Bundle args = new Bundle();
		args.putString(GROUP_NAME_KEY, groupName);
		f.setArguments(args);
		f.show(fragmentManager, TAG);
	}
}
