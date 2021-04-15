package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.itinerary.ItineraryHelper;
import net.osmand.util.Algorithms;

public class EditFavoriteGroupDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "EditFavoriteGroupDialogFragment";
	private static final String GROUP_NAME_KEY = "group_name_key";

	private FavoriteGroup group;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		FavouritesDbHelper helper = app.getFavorites();
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
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Activity activity = getActivity();
						if (activity != null) {
							Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
							AlertDialog.Builder b = new AlertDialog.Builder(themedContext);
							b.setTitle(R.string.favorite_category_name);
							final EditText nameEditText = new EditText(themedContext);
							nameEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
							nameEditText.setText(group.getName());
							LinearLayout container = new LinearLayout(themedContext);
							int sidePadding = AndroidUtils.dpToPx(activity, 24f);
							int topPadding = AndroidUtils.dpToPx(activity, 4f);
							container.setPadding(sidePadding, topPadding, sidePadding, topPadding);
							container.addView(nameEditText);
							b.setView(container);
							b.setNegativeButton(R.string.shared_string_cancel, null);
							b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String name = nameEditText.getText().toString();
									boolean nameChanged = !Algorithms.objectEquals(group.getName(), name);
									if (nameChanged) {
										app.getFavorites()
												.editFavouriteGroup(group, name, group.getColor(), group.isVisible());
										updateParentFragment();
									}
									dismiss();
								}
							});
							b.show();
						}
					}
				})
				.create();
		items.add(editNameItem);

		final View changeColorView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.change_fav_color, null, false);
		((ImageView) changeColorView.findViewById(R.id.change_color_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_appearance));
		updateColorView((ImageView) changeColorView.findViewById(R.id.colorImage));
		BaseBottomSheetItem changeColorItem = new BaseBottomSheetItem.Builder()
				.setCustomView(changeColorView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Activity activity = getActivity();
						if (activity != null) {
							final ListPopupWindow popup = new ListPopupWindow(v.getContext());
							popup.setAnchorView(v);
							popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
							popup.setModal(true);
							popup.setDropDownGravity(Gravity.END | Gravity.TOP);
							if (AndroidUiHelper.isOrientationPortrait(activity)) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									popup.setVerticalOffset(AndroidUtils.dpToPx(app, 48f));
								}
							} else {
								popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
							}
							popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
							final FavoriteColorAdapter colorAdapter = new FavoriteColorAdapter(activity);
							popup.setAdapter(colorAdapter);
							popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									Integer color = colorAdapter.getItem(position);
									if (color != null) {
										if (color != group.getColor()) {
											app.getFavorites()
													.editFavouriteGroup(group, group.getName(), color, group.isVisible());
											updateParentFragment();
										}
									}
									popup.dismiss();
									dismiss();
								}
							});
							popup.show();
						}
					}
				})
				.create();
		items.add(changeColorItem);

		BaseBottomSheetItem showOnMapItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(group.isVisible())
				.setIcon(getContentIcon(R.drawable.ic_map))
				.setTitle(getString(R.string.shared_string_show_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean visible = !group.isVisible();
						app.getFavorites()
								.editFavouriteGroup(group, group.getName(), group.getColor(), visible);
						updateParentFragment();
						dismiss();
					}
				})
				.create();
		items.add(showOnMapItem);

		if (group.getPoints().size() > 0) {
			items.add(new DividerHalfItem(getContext()));

			final ItineraryHelper itineraryHelper = app.getItineraryHelper();
			final FavoriteGroup favGroup = this.group;
			final MapMarkersGroup markersGr = app.getItineraryHelper().getMarkersGroup(this.group);
			final boolean synced = markersGr != null;

			BaseBottomSheetItem markersGroupItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(synced ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_flag))
					.setTitle(getString(synced ? R.string.remove_from_map_markers : R.string.shared_string_add_to_map_markers))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if (synced) {
								itineraryHelper.removeMarkersGroup(markersGr);
							} else {
								app.getItineraryHelper().addOrEnableGroup(favGroup);
							}
							dismiss();
							MapActivity.launchMapActivityMoveToTop(getActivity());
						}
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
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							FavoritesTreeFragment fragment = getFavoritesTreeFragment();
							if (fragment != null) {
								fragment.shareFavorites(EditFavoriteGroupDialogFragment.this.group);
							}
							dismiss();
						}
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

	private void updateColorView(ImageView colorImageView) {
		int color = group.getColor() == 0 ? getResources().getColor(R.color.color_favorite) : group.getColor();
		if (color == 0) {
			colorImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(getMyApplication().getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	public static void showInstance(FragmentManager fragmentManager, String groupName) {
		EditFavoriteGroupDialogFragment f = new EditFavoriteGroupDialogFragment();
		Bundle args = new Bundle();
		args.putString(GROUP_NAME_KEY, groupName);
		f.setArguments(args);
		f.show(fragmentManager, EditFavoriteGroupDialogFragment.TAG);
	}

	public static class FavoriteColorAdapter extends ArrayAdapter<Integer> {

		private final OsmandApplication app;

		public FavoriteColorAdapter(Context context) {
			super(context, R.layout.rendering_prop_menu_item);
			this.app = (OsmandApplication) getContext().getApplicationContext();
			init();
		}

		public void init() {
			for (int color : ColorDialogs.pallette) {
				add(color);
			}
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			Integer color = getItem(position);
			View v = convertView;
			if (v == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.rendering_prop_menu_item, parent, false);
			}
			if (color != null) {
				TextView textView = (TextView) v.findViewById(R.id.text1);
				textView.setText(app.getString(ColorDialogs.paletteColors[position]));
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color), null);
				textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(getContext(), 10f));
				v.findViewById(R.id.divider).setVisibility(View.GONE);
			}
			return v;
		}
	}
}
