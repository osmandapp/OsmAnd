package net.osmand.plus.mapcontextmenu.editors;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;


public class FavoritePointEditorFragment extends PointEditorFragment {

	@Nullable
	private FavoritePointEditor editor;
	@Nullable
	private FavouritePoint favorite;
	@Nullable
	private FavoriteGroup group;
	@Nullable
	FavouritesDbHelper helper;

	private boolean autoFill;
	private boolean saved;
	private int defaultColor;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			helper = mapActivity.getMyApplication().getFavorites();
			editor = mapActivity.getContextMenu().getFavoritePointEditor();
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		defaultColor = getResources().getColor(R.color.color_favorite);

		FavoritePointEditor editor = getFavoritePointEditor();
		FavouritesDbHelper helper = getHelper();
		if (editor != null && helper != null) {
			FavouritePoint favorite = editor.getFavorite();
			this.favorite = favorite;
			this.group = helper.getGroup(favorite);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		FavoritePointEditor editor = getFavoritePointEditor();
		if (view != null && editor != null && editor.isNew()) {
			Button replaceButton = (Button) view.findViewById(R.id.replace_button);
			replaceButton.setTextColor(getResources().getColor(!editor.isLight() ? R.color.osmand_orange : R.color.map_widget_blue));
			replaceButton.setVisibility(View.VISIBLE);
			replaceButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FavoriteDialogs.createReplaceFavouriteDialog(activity, args);
					}
				}
			});
		}
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (autoFill) {
			save(true);
		}
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	public FavoritePointEditor getFavoritePointEditor() {
		return editor;
	}

	@Nullable
	public FavouritePoint getFavorite() {
		return favorite;
	}

	@Nullable
	public FavoriteGroup getGroup() {
		return group;
	}

	@Nullable
	public FavouritesDbHelper getHelper() {
		return helper;
	}

	@Override
	public String getToolbarTitle() {
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null) {
			if (editor.isNew()) {
				return getString(R.string.favourites_context_menu_add);
			} else {
				return getString(R.string.favourites_context_menu_edit);
			}
		}
		return "";
	}

	@Override
	public void setCategory(String name, int color) {
		FavouritesDbHelper helper = getHelper();
		if (helper != null) {
			FavoriteGroup group = helper.getGroup(FavoriteGroup.convertDisplayNameToGroupIdName(requireContext(), name));
			this.group = group;
			super.setCategory(name, group != null ? group.getColor() : 0);
		}
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		if (editor != null) {
			FavoritePointEditorFragment fragment = new FavoritePointEditorFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commitAllowingStateLoss();
		}
	}

	public static void showAutoFillInstance(final MapActivity mapActivity, boolean autoFill) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		FavoritePointEditorFragment fragment = new FavoritePointEditorFragment();
		fragment.autoFill = autoFill;

		if (editor != null) {
			mapActivity.getSupportFragmentManager().beginTransaction()
					//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		final FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
					getNameTextValue(), getCategoryTextValue(), favorite.getAltitude(), favorite.getTimestamp());
			point.setDescription(getDescriptionTextValue());
			point.setAddress(getAddressTextValue());
			AlertDialog.Builder builder = FavouritesDbHelper.checkDuplicates(point, helper, getMapActivity());

			if (favorite.getName().equals(point.getName()) &&
					favorite.getCategory().equals(point.getCategory()) &&
					Algorithms.stringsEqual(favorite.getDescription(), point.getDescription()) &&
					Algorithms.stringsEqual(favorite.getAddress(), point.getAddress())) {
				if (needDismiss) {
					dismiss(false);
				}
				return;
			}

			if (builder != null && !autoFill) {
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(), needDismiss);
					}
				});
				builder.create().show();
			} else {
				doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(), needDismiss);
			}
			saved = true;
		}
	}

	private void doSave(FavouritePoint favorite, String name, String category, String description, String address, boolean needDismiss) {
		FavouritesDbHelper helper = getHelper();
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null && helper != null) {
			if (editor.isNew()) {
				doAddFavorite(name, category, description, address);
			} else {
				helper.editFavouriteName(favorite, name, category, description, address);
			}
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		mapActivity.refreshMap();
		if (needDismiss) {
			dismiss(false);
		}

		MapContextMenu menu = mapActivity.getContextMenu();
		LatLon latLon = new LatLon(favorite.getLatitude(), favorite.getLongitude());
		if (menu.getLatLon() != null && menu.getLatLon().equals(latLon)) {
			menu.update(latLon, favorite.getPointDescription(mapActivity), favorite);
		}
	}

	private void doAddFavorite(String name, String category, String description, String address) {
		OsmandApplication app = getMyApplication();
		FavouritesDbHelper helper = getHelper();
		FavouritePoint favorite = getFavorite();
		if (app != null && favorite != null && helper != null) {
			favorite.setName(name);
			favorite.setCategory(category);
			favorite.setDescription(description);
			favorite.setAddress(address);
			app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
			helper.addFavourite(favorite);
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		FragmentActivity activity = getActivity();
		final FavouritePoint favorite = getFavorite();
		if (activity != null && favorite != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(getString(R.string.favourites_remove_dialog_msg, favorite.getName()));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FavouritesDbHelper helper = getHelper();
					if (helper != null) {
						helper.deleteFavourite(favorite);
						saved = true;
						if (needDismiss) {
							dismiss(true);
						} else {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								mapActivity.refreshMap();
							}
						}
					}
				}
			});
			builder.create().show();
		}
	}

	@Override
	public String getHeaderCaption() {
		return getString(R.string.favourites_edit_dialog_title);
	}

	@Override
	public String getNameInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite != null ? favorite.getName() : "";
	}

	@Override
	public String getCategoryInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite == null || favorite.getCategory().length() == 0 ? getDefaultCategoryName() : favorite.getCategoryDisplayName(requireContext());
	}

	@Override
	public String getDescriptionInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite != null ? favorite.getDescription() : "";
	}

	@Override
	public Drawable getNameIcon() {
		return PointImageDrawable.getFromFavorite(getMapActivity(), getPointColor(), false, getFavorite());
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, getPointColor());
	}

	@Override
	public int getPointColor() {
		int color = 0;
		FavoriteGroup group = getGroup();
		if (group != null) {
			color = group.getColor();
		}
		if (color == 0) {
			color = defaultColor;
		}
		return color;
	}
}
