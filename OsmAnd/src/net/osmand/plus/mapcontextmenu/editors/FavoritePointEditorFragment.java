package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;

public class FavoritePointEditorFragment extends PointEditorFragment {

	private FavoritePointEditor editor;
	private FavouritePoint favorite;
	private FavoriteGroup group;
	FavouritesDbHelper helper;

	private boolean saved;
	private int defaultColor;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		helper = getMyApplication().getFavorites();
		editor = getMapActivity().getContextMenu().getFavoritePointEditor();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		defaultColor = getResources().getColor(R.color.color_favorite);

		favorite = editor.getFavorite();
		group = helper.getGroup(favorite);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null && editor.isNew()) {
			Button btnReplace = (Button) view.findViewById(R.id.button_replace);
			btnReplace.setVisibility(View.VISIBLE);
			btnReplace.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putSerializable(FavoriteDialogs.KEY_FAVORITE, favorite);
					FavoriteDialogs.createReplaceFavouriteDialog(getActivity(), args);
				}
			});
		}
		return view;
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Override
	public String getToolbarTitle() {
		if (editor.isNew()) {
			return getMapActivity().getResources().getString(R.string.favourites_context_menu_add);
		} else {
			return getMapActivity().getResources().getString(R.string.favourites_context_menu_edit);
		}
	}

	@Override
	public void setCategory(String name) {
		group = helper.getGroup(name);
		super.setCategory(name);
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	public static void showInstance(final MapActivity mapActivity) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		FavoritePointEditorFragment fragment = new FavoritePointEditorFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
				getNameTextValue(), getCategoryTextValue());
		point.setDescription(getDescriptionTextValue());
		AlertDialog.Builder builder = FavouritesDbHelper.checkDuplicates(point, helper, getMapActivity());

		if (favorite.getName().equals(point.getName()) &&
				favorite.getCategory().equals(point.getCategory()) &&
				Algorithms.stringsEqual(favorite.getDescription(), point.getDescription())) {

			if (needDismiss) {
				dismiss(false);
			}
			return;
		}

		if (builder != null) {
			builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), needDismiss);
				}
			});
			builder.create().show();
		} else {
			doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), needDismiss);
		}
		saved = true;
	}

	private void doSave(FavouritePoint favorite, String name, String category, String description, boolean needDismiss) {
		if (editor.isNew()) {
			doAddFavorite(name, category, description);
		} else {
			helper.editFavouriteName(favorite, name, category, description);
		}
		getMapActivity().refreshMap();
		if (needDismiss) {
			dismiss(false);
		}

		MapContextMenu menu = getMapActivity().getContextMenu();
		LatLon latLon = new LatLon(favorite.getLatitude(), favorite.getLongitude());
		if (menu.getLatLon().equals(latLon)) {
			menu.update(latLon, favorite.getPointDescription(), favorite);
		}
	}

	private void doAddFavorite(String name, String category, String description) {
		favorite.setName(name);
		favorite.setCategory(category);
		favorite.setDescription(description);
		helper.addFavourite(favorite);
	}

	@Override
	protected void delete(final boolean needDismiss) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.favourites_remove_dialog_msg, favorite.getName()));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				helper.deleteFavourite(favorite);
				saved = true;
				if (needDismiss) {
					dismiss(true);
				} else {
					getMapActivity().refreshMap();
				}
			}
		});
		builder.create().show();
	}

	@Override
	public String getHeaderCaption() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_title);
	}

	@Override
	public String getNameInitValue() {
		return favorite.getName();
	}

	@Override
	public String getCategoryInitValue() {
		return favorite.getCategory().length() == 0 ? getDefaultCategoryName() : favorite.getCategory();
	}

	@Override
	public String getDescriptionInitValue() {
		return favorite.getDescription();
	}

	@Override
	public Drawable getNameIcon() {
		int color = defaultColor;
		if (group != null) {
			color = group.color;
		}
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), color, false);
	}

	@Override
	public Drawable getCategoryIcon() {
		int color = 0;
		if (group != null) {
			color = group.color;
		}
		if (color == 0) {
			color = defaultColor;
		}
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, color);
	}
}
