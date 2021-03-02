package net.osmand.plus.mapcontextmenu.editors;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;

import java.util.LinkedHashSet;
import java.util.Set;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

public class FavoritePointEditorFragmentNew extends PointEditorFragmentNew {

	@Nullable
	private FavoritePointEditor editor;
	@Nullable
	private FavouritePoint favorite;
	@Nullable
	private FavoriteGroup group;
	private int color;
	private int iconId;
	@NonNull
	private BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;

	@Nullable
	private FavouritesDbHelper helper;

	private boolean autoFill;
	private boolean saved;
	private int defaultColor;

	@Override
	public void onAttach(@NonNull Context context) {
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
			if (favorite == null && savedInstanceState != null) {
				favorite = (FavouritePoint) savedInstanceState.getSerializable(FavoriteDialogs.KEY_FAVORITE);
			}
			this.favorite = favorite;
			this.group = helper.getGroup(favorite);
			this.color = favorite.getColor();
			this.backgroundType = favorite.getBackgroundType();
			this.iconId = favorite.getIconId();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		FavoritePointEditor editor = getFavoritePointEditor();
		if (view != null) {
			View replaceButton = view.findViewById(R.id.button_replace_container);
			replaceButton.setVisibility(View.VISIBLE);
			replaceButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					replacePressed();
				}
			});
			if (editor != null && editor.isNew()) {
				ImageView toolbarAction = view.findViewById(R.id.toolbar_action);
				toolbarAction.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						replacePressed();
					}
				});
			}
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
	}

	private void replacePressed() {
		Bundle args = new Bundle();
		args.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FavoriteDialogs.createReplaceFavouriteDialog(activity, args);
		}
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

	private FavoritePointEditor getFavoritePointEditor() {
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
		Context ctx = getContext();
		if (helper != null && ctx != null) {
			FavoriteGroup group = helper.getGroup(FavoriteGroup.convertDisplayNameToGroupIdName(ctx, name));
			this.group = group;
			super.setCategory(name, group != null ? group.getColor() : 0);
		}
	}

	@Override
	protected String getLastUsedGroup() {
		String lastCategory = "";
		OsmandApplication app = getMyApplication();
		if (app != null) {
			lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
			if (!Algorithms.isEmpty(lastCategory) && !app.getFavorites().groupExists(lastCategory)) {
				lastCategory = "";
			}
		}
		return lastCategory;
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public void setBackgroundType(@NonNull BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	@Override
	public void setIcon(int iconId) {
		this.iconId = iconId;
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
			FavoritePointEditorFragmentNew fragment = new FavoritePointEditorFragmentNew();
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

		FavoritePointEditorFragmentNew fragment = new FavoritePointEditorFragmentNew();
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
		final FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
					getNameTextValue(), getCategoryTextValue(), favorite.getAltitude(), favorite.getTimestamp());
			point.setDescription(isDescriptionAvailable() ? getDescriptionTextValue() : null);
			point.setAddress(isAddressAvailable() ? getAddressTextValue() : null);
			point.setColor(color);
			point.setBackgroundType(backgroundType);
			point.setIconId(iconId);
			return isChanged(favorite, point);
		}
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		final FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
					getNameTextValue(), getCategoryTextValue(), favorite.getAltitude(), favorite.getTimestamp());
			point.setDescription(isDescriptionAvailable() ? getDescriptionTextValue() : null);
			point.setAddress(isAddressAvailable() ? getAddressTextValue() : null);
			point.setColor(color);
			point.setBackgroundType(backgroundType);
			point.setIconId(iconId);
			AlertDialog.Builder builder = FavouritesDbHelper.checkDuplicates(point, helper, getMapActivity());

			if (isChanged(favorite, point)) {

				if (needDismiss) {
					dismiss(false);
				}
				return;
			}

			if (builder != null && !autoFill) {
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(),
								point.getColor(), point.getBackgroundType(), point.getIconId(), needDismiss);
					}
				});
				builder.create().show();
			} else {
				doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(),
						point.getColor(), point.getBackgroundType(), point.getIconId(), needDismiss);
			}
			saved = true;
		}
	}

	private boolean isChanged(FavouritePoint favorite, FavouritePoint point) {
		return favorite.getColor() == point.getColor() &&
				favorite.getIconId() == point.getIconId() &&
				favorite.getName().equals(point.getName()) &&
				favorite.getCategory().equals(point.getCategory()) &&
				favorite.getBackgroundType().equals(point.getBackgroundType()) &&
				Algorithms.stringsEqual(favorite.getDescription(), point.getDescription()) &&
				Algorithms.stringsEqual(favorite.getAddress(), point.getAddress());
	}

	private void doSave(FavouritePoint favorite, String name, String category, String description, String address,
						@ColorInt int color, BackgroundType backgroundType, @DrawableRes int iconId, boolean needDismiss) {
		FavouritesDbHelper helper = getHelper();
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null && helper != null) {
			if (editor.isNew()) {
				doAddFavorite(name, category, description, address, color, backgroundType, iconId);
			} else {
				doEditFavorite(favorite, name, category, description, address, color, backgroundType, iconId, helper);
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

	private void doEditFavorite(FavouritePoint favorite, String name, String category, String description, String address,
								@ColorInt int color, BackgroundType backgroundType, @DrawableRes int iconId,
								FavouritesDbHelper helper) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
			favorite.setColor(color);
			favorite.setBackgroundType(backgroundType);
			favorite.setIconId(iconId);
			helper.editFavouriteName(favorite, name, category, description, address);
		}
	}

	private void doAddFavorite(String name, String category, String description, String address, @ColorInt int color,
							   BackgroundType backgroundType, @DrawableRes int iconId) {
		OsmandApplication app = getMyApplication();
		FavouritesDbHelper helper = getHelper();
		FavouritePoint favorite = getFavorite();
		if (app != null && favorite != null && helper != null) {
			favorite.setName(name);
			favorite.setCategory(category);
			favorite.setDescription(description);
			favorite.setAddress(address);
			favorite.setColor(color);
			favorite.setBackgroundType(backgroundType);
			favorite.setIconId(iconId);
			app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
			helper.addFavourite(favorite);
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		FragmentActivity activity = getActivity();
		final FavouritePoint favorite = getFavorite();
		if (activity != null && favorite != null) {
			final OsmandApplication app = (OsmandApplication) activity.getApplication();
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
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
	public String getAddressInitValue() {
		FavouritePoint favourite = getFavorite();
		return favourite != null ? favourite.getAddress() : "";
	}

	@Override
	public Drawable getNameIcon() {
		FavouritePoint favorite = getFavorite();
		FavouritePoint point = null;
		if (favorite != null) {
			point = new FavouritePoint(favorite);
			point.setColor(getPointColor());
			point.setBackgroundType(backgroundType);
			point.setIconId(iconId);
		}
		return PointImageDrawable.getFromFavorite(getMapActivity(), getPointColor(), false, point);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, getPointColor());
	}

	@Override
	public int getDefaultColor() {
		return defaultColor;
	}

	@Override
	public int getPointColor() {
		FavouritePoint favorite = getFavorite();
		int color = favorite != null ? getColor() : 0;
		FavoriteGroup group = getGroup();
		if (group != null && (color == 0)) {
			color = group.getColor();
		}
		if (color == 0) {
			color = defaultColor;
		}
		return color;
	}

	@Override
	@NonNull
	public BackgroundType getBackgroundType() {
		return backgroundType;
	}

	@Override
	public int getIconId() {
		return iconId;
	}

	private int getColor() {
		return color;
	}

	@Override
	public Set<String> getCategories() {
		Set<String> categories = new LinkedHashSet<>();
		Set<String> categoriesHidden = new LinkedHashSet<>();
		FavouritesDbHelper helper = getHelper();
		if (helper != null && editor != null) {
			OsmandApplication app = getMyApplication();
			FavoriteGroup lastUsedGroup = helper.getGroup(getLastUsedGroup());
			if (lastUsedGroup != null) {
				categories.add(lastUsedGroup.getDisplayName(app));
			}
			for (FavouritesDbHelper.FavoriteGroup fg : helper.getFavoriteGroups()) {
				if (!fg.equals(lastUsedGroup)) {
					if (fg.isVisible()) {
						categories.add(fg.getDisplayName(app));
					} else {
						categoriesHidden.add(fg.getDisplayName(app));
					}
				}
			}
			categories.addAll(categoriesHidden);
		}
		return categories;
	}

	@Override
	public boolean isCategoryVisible(String name) {
		if (getHelper() != null) {
			return getHelper().isGroupVisible(name);
		}
		return true;
	}

	@Override
	public int getCategoryPointsCount(String category) {
		FavouritesDbHelper helper = getHelper();
		if (helper != null) {
			for (FavouritesDbHelper.FavoriteGroup fg : getHelper().getFavoriteGroups()) {
				if (fg.getDisplayName(getMyApplication()).equals(category)) {
					return fg.getPoints().size();
				}
			}
		}
		return 0;
	}

	@Override
	@ColorInt
	public int getCategoryColor(String category) {
		FavouritesDbHelper helper = getHelper();
		if (helper != null) {
			for (FavouritesDbHelper.FavoriteGroup fg : getHelper().getFavoriteGroups()) {
				if (fg.getDisplayName(getMyApplication()).equals(category)) {
					return fg.getColor();
				}
			}
		}
		return defaultColor;
	}
}
