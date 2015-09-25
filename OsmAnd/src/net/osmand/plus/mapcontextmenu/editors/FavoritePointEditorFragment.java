package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;

import java.text.MessageFormat;

public class FavoritePointEditorFragment extends PointEditorFragment {

	private FavoritePointEditor editor;
	private FavouritePoint favorite;
	FavouritesDbHelper helper;

	private boolean saved;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		helper = getMyApplication().getFavorites();
		editor = getMapActivity().getFavoritePointEditor();
		favorite = editor.getFavorite();
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

	public static void showInstance(final MapActivity mapActivity) {
		FavoritePointEditor editor = mapActivity.getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		FavoritePointEditorFragment fragment = new FavoritePointEditorFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentName())
				.addToBackStack(null).commit();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(), getName(), getCategory());
		point.setDescription(getDescription());
		AlertDialog.Builder builder = FavouritesDbHelper.checkDuplicates(point, helper, getMapActivity());

		if (favorite.getName().equals(point.getName()) &&
				favorite.getCategory().equals(point.getCategory()) &&
				favorite.getDescription().equals(point.getDescription())) {

			if (needDismiss) {
				dismiss(true);
			}
			return;
		}

		if (builder != null) {
			builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (editor.isNew()) {
						doAddFavorite(point.getName(), point.getCategory(), point.getDescription());
					} else {
						helper.editFavouriteName(favorite, point.getName(), point.getCategory(), point.getDescription());
					}
					getMapActivity().getMapView().refreshMap(true);
					if (needDismiss) {
						dismiss(true);
					}
				}
			});
			builder.create().show();
		} else {
			if (editor.isNew()) {
				doAddFavorite(point.getName(), point.getCategory(), point.getDescription());
			} else {
				helper.editFavouriteName(favorite, point.getName(), point.getCategory(), point.getDescription());
			}
			getMapActivity().getMapView().refreshMap(true);
			if (needDismiss) {
				dismiss(true);
			}
		}
		saved = true;
	}

	private void doAddFavorite(String name, String category, String description) {
		favorite.setName(name);
		favorite.setCategory(category);
		favorite.setDescription(description);
		helper.addFavourite(favorite);
	}

	@Override
	protected void delete(final boolean needDismiss) {
		final Resources resources = this.getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.favourites_remove_dialog_msg, favorite.getName()));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				helper.deleteFavourite(favorite);
				if (needDismiss) {
					dismiss(true);
				}
				getMapActivity().getMapView().refreshMap(true);
			}
		});
		builder.create().show();
	}

	@Override
	public String getHeaderText() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_title);
	}

	@Override
	public String getNameValue() {
		return favorite.getName();
	}

	@Override
	public String getCategoryValue() {
		return favorite.getCategory();
	}

	@Override
	public String getDescriptionValue() {
		return favorite.getDescription();
	}

	@Override
	public Drawable getNameIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), favorite.getColor(), getMapActivity().getMapView().getCurrentRotatedTileBox().getDensity());
	}

	@Override
	public Drawable getCategoryIcon() {
		FavouritesDbHelper helper = getMyApplication().getFavorites();
		FavouritesDbHelper.FavoriteGroup group = helper.getGroup(favorite);
		if (group != null) {
			return getIcon(R.drawable.ic_action_folder_stroke, group.color);
		} else {
			return null;
		}
	}

	public Drawable getIcon(int resId, int color) {
		OsmandApplication app = getMyApplication();
		Drawable d = app.getResources().getDrawable(resId).mutate();
		d.clearColorFilter();
		d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		return d;
	}
}
