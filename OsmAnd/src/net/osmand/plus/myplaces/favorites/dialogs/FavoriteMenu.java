package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment.IMPORT_FAVOURITES_REQUEST;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavouriteGroupEditorFragment;
import net.osmand.plus.mapcontextmenu.editors.SelectFavouriteGroupBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapcontextmenu.other.SharePoiParams;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class FavoriteMenu {
	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final MyPlacesActivity activity;

	public FavoriteMenu(@NonNull OsmandApplication app, @NonNull UiUtilities uiUtilities, @NonNull MyPlacesActivity activity) {
		this.app = app;
		this.activity = activity;
		this.uiUtilities = uiUtilities;
	}

	public void showPointOptionsMenu(View view, FavouritePoint favouritePoint, boolean nightMode,
	                                 CategorySelectionListener selectionListener, FavoriteActionListener actionListener, Fragment targetFragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(favouritePoint.getDisplayName(app))
				.setTitleColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
				.setTitleSize(14)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_edit)
				.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
				.setOnClickListener(v -> {
					FavoriteGroup group = app.getFavoritesHelper().getGroup(favouritePoint);
					if (group != null) {
						FavoritePointEditor editor = new FavoritePointEditor(app);
						editor.edit(favouritePoint, activity, targetFragment);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_move)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_move))
				.setOnClickListener(v -> {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					SelectFavouriteGroupBottomSheet.showInstance(fragmentManager, favouritePoint.getCategory(), selectionListener);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> {
					LatLon latLon = new LatLon(favouritePoint.getLatitude(), favouritePoint.getLongitude());
					SharePoiParams params = new SharePoiParams(latLon);
					params.addName(favouritePoint.getName());
					ShareMenu.show(latLon, favouritePoint.getName(), favouritePoint.getAddress(), ShareMenu.buildOsmandPoiUri(params), app, activity);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
					builder.setMessage(app.getString(R.string.favourites_remove_dialog_msg, favouritePoint.getName()));
					builder.setNegativeButton(R.string.shared_string_no, null);
					builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						app.getFavoritesHelper().deleteFavourite(favouritePoint);
						actionListener.onActionFinish();
					});
					builder.create().show();
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider;
		PopUpMenu.show(displayData);
	}

	public void showFolderSelectOptionsMenu(View view, boolean nightMode) {

	}

	public void showFolderOptionsMenu(MyPlacesActivity activity, View view, boolean nightMode, CategorySelectionListener selectionListener, Fragment fragment) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					if (fragment instanceof BaseFavoriteListFragment baseFavoriteListFragment) {
						baseFavoriteListFragment.setSelectionMode(!baseFavoriteListFragment.selectionMode);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					FavouriteGroupEditorFragment.showInstance(manager, null, selectionListener, false);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importFavourites(fragment))
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	protected void importFavourites(@NonNull Fragment fragment) {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FAVOURITES_REQUEST);
	}
	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}

	public interface FavoriteActionListener{
		void onActionFinish();
	}
}

