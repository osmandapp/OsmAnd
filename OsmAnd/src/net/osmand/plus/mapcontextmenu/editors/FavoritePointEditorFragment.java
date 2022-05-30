package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.Context;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FavoritePointEditorFragment extends PointEditorFragment {

	private FavouritesHelper favouritesHelper;

	@Nullable
	private FavoritePointEditor editor;
	@Nullable
	private FavouritePoint favorite;
	@Nullable
	private FavoriteGroup group;

	private boolean saved;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		editor = requireMapActivity().getContextMenu().getFavoritePointEditor();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesHelper = app.getFavoritesHelper();

		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null) {
			FavouritePoint favorite = editor.getFavorite();
			if (favorite == null && savedInstanceState != null) {
				favorite = (FavouritePoint) savedInstanceState.getSerializable(FavoriteDialogs.KEY_FAVORITE);
			}
			this.favorite = favorite;
			this.group = favouritesHelper.getGroup(favorite);

			setColor(getInitialColor());
			setIcon(getInitialIconId());
			setBackgroundType(getInitialBackgroundType());
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		FavoritePointEditor editor = getFavoritePointEditor();
		if (view != null) {
			View replaceButton = view.findViewById(R.id.button_replace_container);
			replaceButton.setVisibility(View.VISIBLE);
			replaceButton.setOnClickListener(v -> replacePressed());
			if (editor != null && editor.isNew()) {
				ImageView toolbarAction = view.findViewById(R.id.toolbar_action);
				toolbarAction.setOnClickListener(v -> replacePressed());
			}
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
	}

	@Override
	public void setSelectedGroup(String selectedGroup) {
		super.setSelectedGroup(selectedGroup);

		FavoriteGroup group = getFavoriteGroup(selectedGroup);
		if (group != null) {
			setColor(group.getColor());
			setIconName(group.getIconName());
			setBackgroundType(group.getBackgroundType());
		}
	}

	private void replacePressed() {
		Bundle args = new Bundle();
		args.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FavoriteDialogs.createReplaceFavouriteDialog(activity, args);
		}
	}

	@Nullable
	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Nullable
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

	@NonNull
	@Override
	public String getToolbarTitle() {
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null) {
			return getString(editor.isNew ? R.string.favourites_context_menu_add : R.string.favourites_context_menu_edit);
		}
		return "";
	}

	@Override
	public void setPointsGroup(@NonNull PointsGroup pointsGroup) {
		Context ctx = getContext();
		if (ctx != null) {
			String groupIdName = FavoriteGroup.convertDisplayNameToGroupIdName(ctx, pointsGroup.name);
			group = favouritesHelper.getGroup(groupIdName);
			super.setPointsGroup(pointsGroup);
		}
	}

	@Override
	protected String getLastUsedGroup() {
		String lastCategory = "";
		OsmandApplication app = getMyApplication();
		if (app != null) {
			lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
			if (!Algorithms.isEmpty(lastCategory) && !app.getFavoritesHelper().groupExists(lastCategory)) {
				lastCategory = "";
			}
		}
		return lastCategory;
	}

	@NonNull
	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	@Override
	protected boolean wasSaved() {
		FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
					getNameTextValue(), getCategoryTextValue(), favorite.getAltitude(), favorite.getTimestamp());
			point.setDescription(getDescriptionTextValue());
			point.setAddress(getAddressTextValue());
			point.setColor(getColor());
			point.setIconId(getIconId());
			point.setBackgroundType(getBackgroundType());
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
			point.setDescription(getDescriptionTextValue());
			point.setAddress(getAddressTextValue());
			point.setColor(getColor());
			point.setIconId(getIconId());
			point.setBackgroundType(getBackgroundType());
			AlertDialog.Builder builder = FavoriteDialogs.checkDuplicates(point, requireActivity());

			if (isChanged(favorite, point)) {

				if (needDismiss) {
					dismiss(false);
				}
				return;
			}

			if (builder != null && !skipConfirmationDialog) {
				builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) ->
						doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(),
								point.getColor(), point.getBackgroundType(), point.getIconIdOrDefault(), needDismiss));
				builder.create().show();
			} else {
				doSave(favorite, point.getName(), point.getCategory(), point.getDescription(), point.getAddress(),
						point.getColor(), point.getBackgroundType(), point.getIconIdOrDefault(), needDismiss);
			}
			saved = true;
		}
	}

	private boolean isChanged(FavouritePoint favorite, FavouritePoint point) {
		return favorite.getColor() == point.getColor() &&
				favorite.getIconIdOrDefault() == point.getIconIdOrDefault() &&
				favorite.getName().equals(point.getName()) &&
				favorite.getCategory().equals(point.getCategory()) &&
				favorite.getBackgroundType().equals(point.getBackgroundType()) &&
				Algorithms.stringsEqual(favorite.getDescription(), point.getDescription()) &&
				Algorithms.stringsEqual(favorite.getAddress(), point.getAddress());
	}

	private void doSave(FavouritePoint favorite, String name, String category, String description, String address,
	                    @ColorInt int color, BackgroundType backgroundType, @DrawableRes int iconId, boolean needDismiss) {
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null) {
			if (editor.isNew()) {
				doAddFavorite(name, category, description, address, color, backgroundType, iconId);
			} else {
				doEditFavorite(favorite, name, category, description, address, color, backgroundType, iconId, favouritesHelper);
			}
			addLastUsedIcon(iconId);
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
	                            FavouritesHelper helper) {
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
		FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			favorite.setName(name);
			favorite.setCategory(category);
			favorite.setDescription(description);
			favorite.setAddress(address);
			favorite.setColor(color);
			favorite.setBackgroundType(backgroundType);
			favorite.setIconId(iconId);
			app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
			favouritesHelper.addFavourite(favorite);
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
			builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
				favouritesHelper.deleteFavourite(favorite);
				saved = true;
				if (needDismiss) {
					dismiss(true);
				} else {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						mapActivity.refreshMap();
					}
				}
			});
			builder.create().show();
		}
	}

	@Nullable
	@Override
	public String getNameInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite != null ? favorite.getName() : "";
	}

	@NonNull
	@Override
	public String getCategoryInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite == null || favorite.getCategory().length() == 0
				? getDefaultCategoryName()
				: favorite.getCategoryDisplayName(requireContext());
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
			point.setColor(getColor());
			point.setIconId(getIconId());
			point.setBackgroundType(getBackgroundType());
		}
		return PointImageDrawable.getFromFavorite(getMapActivity(), getColor(), false, point);
	}

	@ColorInt
	@Override
	public int getDefaultColor() {
		return ContextCompat.getColor(requireContext(), R.color.color_favorite);
	}

	@NonNull
	@Override
	public Map<String, PointsGroup> getPointsGroups() {
		Map<String, PointsGroup> pointsGroups = new LinkedHashMap<>();
		if (editor != null) {
			FavoriteGroup lastUsedGroup = favouritesHelper.getGroup(getLastUsedGroup());
			if (lastUsedGroup != null) {
				pointsGroups.put(lastUsedGroup.getDisplayName(app), lastUsedGroup.toPointsGroup());
			}
			Set<PointsGroup> hiddenCategories = new LinkedHashSet<>();
			for (FavoriteGroup group : favouritesHelper.getFavoriteGroups()) {
				if (!group.equals(lastUsedGroup)) {
					if (group.isVisible()) {
						pointsGroups.put(group.getDisplayName(app), group.toPointsGroup());
					} else {
						hiddenCategories.add(group.toPointsGroup());
					}
				}
			}
			for (PointsGroup group : hiddenCategories) {
				pointsGroups.put(group.name, group);
			}
		}
		return pointsGroups;
	}

	@Override
	public boolean isCategoryVisible(@NonNull String name) {
		return favouritesHelper.isGroupVisible(name);
	}

	@Override
	public int getCategoryPointsCount(String category) {
		FavoriteGroup group = getFavoriteGroup(category);
		return group != null ? group.getPoints().size() : 0;
	}

	@Override
	@ColorInt
	public int getCategoryColor(String category) {
		FavoriteGroup group = getFavoriteGroup(category);
		return group != null ? group.getColor() : getDefaultColor();
	}

	@Nullable
	public FavoriteGroup getFavoriteGroup(String category) {
		for (FavoriteGroup group : favouritesHelper.getFavoriteGroups()) {
			if (group.getDisplayName(app).equals(category)) {
				return group;
			}
		}
		return null;
	}

	@Override
	protected void showSelectCategoryDialog() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			SelectFavouriteGroupBottomSheet.showInstance(fragmentManager, getSelectedCategory(), null);
		}
	}

	@Override
	protected void showAddNewCategoryFragment() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			FavouriteGroupEditorFragment.showInstance(manager, null, null, false);
		}
	}

	@ColorInt
	public int getInitialColor() {
		FavouritePoint favorite = getFavorite();
		int color = favorite != null ? favorite.getColor() : 0;
		FavoriteGroup group = getGroup();
		if (group != null && color == 0) {
			color = group.getColor();
		}
		if (color == 0) {
			color = getDefaultColor();
		}
		return color;
	}

	@DrawableRes
	private int getInitialIconId() {
		FavouritePoint favorite = getFavorite();
		int iconId = favorite != null ? favorite.getIconId() : 0;
		FavoriteGroup group = getGroup();
		if (group != null && iconId == 0) {
			iconId = RenderingIcons.getBigIconResourceId(group.getIconName());
		}
		if (iconId == 0) {
			iconId = getDefaultIconId();
		}
		return iconId;
	}

	@NonNull
	private BackgroundType getInitialBackgroundType() {
		FavouritePoint favorite = getFavorite();
		BackgroundType backgroundType = favorite != null ? favorite.getBackgroundType() : null;
		FavoriteGroup group = getGroup();
		if (group != null && backgroundType == null) {
			backgroundType = group.getBackgroundType();
		}
		if (backgroundType == null) {
			backgroundType = DEFAULT_BACKGROUND_TYPE;
		}
		return backgroundType;
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		showAutoFillInstance(mapActivity, false);
	}

	public static void showAutoFillInstance(final MapActivity mapActivity, boolean skipConfirmationDialog) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		if (editor != null) {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			String tag = editor.getFragmentTag();
			if (fragmentManager.findFragmentByTag(tag) == null) {
				FavoritePointEditorFragment fragment = new FavoritePointEditorFragment();
				fragment.skipConfirmationDialog = skipConfirmationDialog;
				fragmentManager.beginTransaction()
						.add(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(null)
						.commitAllowingStateLoss();
			}
		}
	}
}