package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.SwitchCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class EditFavoriteGroupDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "EditFavoriteGroupDialogFragment";
	private static final String GROUP_NAME_KEY = "group_name_key";

	private OsmandApplication app;
	private FavoriteGroup group;
	private FavouritesDbHelper helper;

	@Override
	public void onStart() {
		super.onStart();

		final Window window = getDialog().getWindow();
		WindowManager.LayoutParams params = window.getAttributes();
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.gravity = Gravity.BOTTOM;
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		window.setAttributes(params);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final Activity activity = getActivity();
		app = (OsmandApplication) activity.getApplicationContext();
		helper = app.getFavorites();

		Bundle args = null;
		if (savedInstanceState != null) {
			args = savedInstanceState;
		} else if (getArguments() != null) {
			args = getArguments();
		}

		if (args != null) {
			String groupName = args.getString(GROUP_NAME_KEY);
			if (groupName != null) {
				group = helper.getGroup(groupName);
			}
		}

		final View view = inflater.inflate(R.layout.edit_fav_fragment, container,
				false);
		if (group == null) {
			return view;
		}

		IconsCache ic = app.getIconsCache();

		final TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(Algorithms.isEmpty(group.name) ? app.getString(R.string.shared_string_favorites) : group.name);
		View editNameView = view.findViewById(R.id.edit_name_view);
		((ImageView) view.findViewById(R.id.edit_name_icon))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_edit_dark));
		editNameView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder b = new AlertDialog.Builder(activity);
				b.setTitle(R.string.favorite_group_name);
				final EditText nameEditText = new EditText(activity);
				nameEditText.setText(group.name);
				int leftPadding = AndroidUtils.dpToPx(activity, 24f);
				int topPadding = AndroidUtils.dpToPx(activity, 4f);
				b.setView(nameEditText, leftPadding, topPadding, leftPadding, topPadding);
				b.setNegativeButton(R.string.shared_string_cancel, null);
				b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String name = nameEditText.getText().toString();
						boolean nameChanged = !Algorithms.objectEquals(group.name, name);
						if (nameChanged) {
							getMyApplication().getFavorites()
									.editFavouriteGroup(group, name, group.color, group.visible);
							updateParentFragment();
						}
						dismiss();
					}
				});
				b.show();
			}
		});

		final View changeColorView = view.findViewById(R.id.change_color_view);
		((ImageView) view.findViewById(R.id.change_color_icon))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_appearance));
		updateColorView(changeColorView);
		changeColorView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ListPopupWindow popup = new ListPopupWindow(getActivity());
				popup.setAnchorView(changeColorView);
				popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
				popup.setModal(true);
				popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
				popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
				popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
				final FavoriteColorAdapter colorAdapter = new FavoriteColorAdapter(getActivity());
				popup.setAdapter(colorAdapter);
				popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Integer color = colorAdapter.getItem(position);
						if (color != null) {
							if (color != group.color) {
								getMyApplication().getFavorites()
										.editFavouriteGroup(group, group.name, color, group.visible);
								updateParentFragment();
							}
						}
						popup.dismiss();
						dismiss();
					}
				});
				popup.show();
			}
		});

		View showOnMapView = view.findViewById(R.id.show_on_map_view);
		((ImageView) view.findViewById(R.id.show_on_map_icon))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_map));
		final SwitchCompat checkbox = (SwitchCompat) view.findViewById(R.id.show_on_map_switch);
		checkbox.setChecked(group.visible);
		showOnMapView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean visible = !group.visible;
				checkbox.setChecked(visible);
				getMyApplication().getFavorites()
						.editFavouriteGroup(group, group.name, group.color, visible);
				updateParentFragment();
			}
		});

		View addToMarkersView = view.findViewById(R.id.add_to_markers_view);
		if (app.getSettings().USE_MAP_MARKERS.get() && group.points.size() > 0) {
			((ImageView) view.findViewById(R.id.add_to_markers_icon))
					.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_flag_dark));
			addToMarkersView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
					List<LatLon> points = new ArrayList<>(group.points.size());
					List<PointDescription> names = new ArrayList<>(group.points.size());
					for (FavouritePoint fp : group.points) {
						points.add(new LatLon(fp.getLatitude(), fp.getLongitude()));
						names.add(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, fp.getName()));
					}
					markersHelper.addMapMarkers(points, names);
					dismiss();
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
		} else {
			addToMarkersView.setVisibility(View.GONE);
		}

		View shareView = view.findViewById(R.id.share_view);
		if (group.points.size() > 0) {
			((ImageView) view.findViewById(R.id.share_icon))
					.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_gshare_dark));
			shareView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FavoritesTreeFragment fragment = getFavoritesTreeFragment();
					if (fragment != null) {
						fragment.shareFavorites(group);
					}
					dismiss();
				}
			});
		} else {
			shareView.setVisibility(View.GONE);
		}
		if (group.points.size() == 0) {
			view.findViewById(R.id.divider).setVisibility(View.GONE);
		}

		return view;
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

	private void updateColorView(View colorView) {
		ImageView colorImageView = (ImageView) colorView.findViewById(R.id.colorImage);
		int color = group.color == 0 ? getResources().getColor(R.color.color_favorite) : group.color;
		if (color == 0) {
			colorImageView.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getIconsCache().getPaintedIcon(R.drawable.ic_action_circle, color));
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

		private OsmandApplication app;

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
				v = LayoutInflater.from(getContext()).inflate(R.layout.rendering_prop_menu_item, null);
			}
			if (color != null) {
				TextView textView = (TextView) v.findViewById(R.id.text1);
				textView.setText(app.getString(ColorDialogs.paletteColors[position]));
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getIconsCache().getPaintedIcon(R.drawable.ic_action_circle, color), null);
				textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(getContext(), 10f));
				v.findViewById(R.id.divider).setVisibility(View.GONE);
			}
			return v;
		}
	}
}
