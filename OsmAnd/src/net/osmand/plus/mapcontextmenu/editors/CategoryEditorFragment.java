package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.GPXUtilities.PointsCategory;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.myplaces.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Set;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

public class CategoryEditorFragment extends EditorFragment {

	public static final String TAG = CategoryEditorFragment.class.getName();

	private static final String KEY_EDITOR_TAG = "key_editor_tag";
	private static final String KEY_EXISTING_FAVORITE_CATEGORY_NAME = "key_existing_favorite_category_name";
	private static final String KEY_GPX_CATEGORIES_LIST = "key_gpx_categories_list";
	private static final String KEY_CATEGORY_NAME = "key_category_name";
	private static final String KEY_CATEGORY_COLOR = "key_category_color";
	private static final String KEY_DEFAULT_ICON_ID = "key_default_icon_id";
	private static final String KEY_DEFAULT_SHAPE = "key_default_shape";

	private FavouritesDbHelper favoritesHelper;
	private String editorTag;
	private CategorySelectionListener selectionListener;

	private FavoriteGroup favoriteCategory;
	private ArrayList<String> gpxCategories;

	private String name;
	@ColorInt
	private int color;
	@DrawableRes
	private int iconId;
	private BackgroundType shape;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favoritesHelper = app.getFavorites();

		if (savedInstanceState != null) {
			restoreArgs(savedInstanceState);
			restoreCategoryParams(savedInstanceState);
		} else if (getArguments() != null) {
			restoreArgs(getArguments());
			if (favoriteCategory != null) {
				fetchParamsFromCategory(favoriteCategory);
			}
		}
		validateCategoryParams();
	}

	private void restoreArgs(@NonNull Bundle bundle) {
		editorTag = bundle.getString(KEY_EDITOR_TAG);
		if (bundle.containsKey(KEY_EXISTING_FAVORITE_CATEGORY_NAME)) {
			favoriteCategory = favoritesHelper.getGroup(bundle.getString(KEY_EXISTING_FAVORITE_CATEGORY_NAME));
		}
		if (bundle.containsKey(KEY_GPX_CATEGORIES_LIST)) {
			gpxCategories = bundle.getStringArrayList(KEY_GPX_CATEGORIES_LIST);
		}
	}

	private void restoreCategoryParams(@NonNull Bundle bundle) {
		name = bundle.getString(KEY_CATEGORY_NAME, "");
		color = bundle.getInt(KEY_CATEGORY_COLOR, 0);
		iconId = bundle.getInt(KEY_DEFAULT_ICON_ID, 0);
		shape = BackgroundType.getByTypeName(bundle.getString(KEY_DEFAULT_SHAPE), BackgroundType.CIRCLE);
	}

	private void fetchParamsFromCategory(@NonNull FavoriteGroup favoriteCategory) {
		name = favoriteCategory.getName();
		color = favoriteCategory.getColor();
		iconId = RenderingIcons.getBigIconResourceId(favoriteCategory.getIconName());
		shape = favoriteCategory.getShape();
	}

	private void validateCategoryParams() {
		name = name == null ? "" : name;
		color = color == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : color;
		iconId = RenderingIcons.getBigIconName(iconId) == null ? getDefaultIconId() : iconId;
		shape = shape == null || !shape.isSelected() ? BackgroundType.CIRCLE : shape;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.category_editor_fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		setupCategoryNameTextBox();
		setupCategoryNameEditText();

		return view;
	}

	private void setupCategoryNameTextBox() {
		nameCaption.setHint(getString(R.string.favorite_category_name));
		int colorRes = nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes));
		nameCaption.setDefaultHintTextColor(colorStateList);
		nameCaption.setStartIconTintList(ColorStateList.valueOf(getPointColor()));
	}

	private void setupCategoryNameEditText() {
		if (Algorithms.isEmpty(name) || isCategoryExists(name.trim())) {
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(getActivity(), nameEdit);
		}
	}

	@NonNull
	@Override
	protected String getToolbarTitle() {
		return favoriteCategory != null
				? getString(R.string.edit_category)
				: getString(R.string.favorite_category_add_new_title);
	}

	@Override
	protected int getToolbarNavigationIconId() {
		return R.drawable.ic_action_close;
	}

	@Nullable
	@Override
	public String getNameInitValue() {
		return name;
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dismiss_button), false);
	}

	@Override
	protected void checkEnteredName(@NonNull String name, @NonNull View saveButton) {
		String trimmedName = name.trim();
		if (trimmedName.isEmpty()) {
			nameCaption.setError(getString(R.string.empty_category_name));
			saveButton.setEnabled(false);
		} else if (isCategoryExists(trimmedName)) {
			nameCaption.setError(getString(R.string.favorite_category_dublicate_message));
			saveButton.setEnabled(false);
		} else {
			nameCaption.setError(null);
			saveButton.setEnabled(true);
		}
	}

	private boolean isCategoryExists(@NonNull String name) {
		return isGpxCategory() ? isGpxCategoryExists(name) : favoritesHelper.groupExists(name);
	}

	private boolean isGpxCategoryExists(@NonNull String name) {
		if (gpxCategories != null) {
			for (String category : gpxCategories) {
				if (name.equalsIgnoreCase(category)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_EDITOR_TAG, editorTag);
		if (favoriteCategory != null) {
			bundle.putString(KEY_EXISTING_FAVORITE_CATEGORY_NAME, favoriteCategory.getName());
		}
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_GPX_CATEGORIES_LIST, gpxCategories);
		}

		bundle.putString(KEY_CATEGORY_NAME, getNameTextValue());
		bundle.putInt(KEY_CATEGORY_COLOR, color);
		bundle.putInt(KEY_DEFAULT_ICON_ID, iconsCard.getSelectedIconId());
		bundle.putString(KEY_DEFAULT_SHAPE, shapesCard.getSelectedShape().getTypeName());
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			int color = ((ColorsCard) card).getSelectedColor();
			updateColorSelector(color);
		} else if (card instanceof IconsCard) {
			setIcon(iconsCard.getSelectedIconId());
		} else if (card instanceof ShapesCard) {
			BackgroundType selectedShape = shapesCard.getSelectedShape();
			setBackgroundType(selectedShape);
			updateSelectedShapeText();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	@Override
	protected void updateColorSelector(int color) {
		super.updateColorSelector(color);
		nameCaption.setStartIconTintList(ColorStateList.valueOf(color));
	}

	@Override
	protected void save(boolean needDismiss) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			String categoryName = getNameTextValue();
			String iconName = RenderingIcons.getBigIconName(iconId);
			PointsCategory category = new PointsCategory(categoryName, color, iconName, shape.getTypeName());

			PointEditor editor = getEditor();
			if (editor != null) {
				editor.setCategory(category, wasSaved());
			}

			if (selectionListener != null) {
				// todo category
				selectionListener.onCategorySelected(getNameTextValue(), color);
			}

			if (needDismiss) {
				dismiss();
			}
		}
	}

	@Override
	protected boolean wasSaved() {
		if (favoriteCategory == null) {
			return false;
		}

		return getNameTextValue().equals(favoriteCategory.getName())
				&& color == favoriteCategory.getColor()
				&& iconsCard.getSelectedIconName().equals(favoriteCategory.getIconName())
				&& shapesCard.getSelectedShape().equals(favoriteCategory.getShape());
	}

	@Override
	public int getPointColor() {
		return color;
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public int getIconId() {
		return iconId;
	}

	@Override
	public void setIcon(int iconId) {
		this.iconId = iconId;
	}

	@NonNull
	@Override
	public BackgroundType getBackgroundType() {
		return shape;
	}

	@Override
	public void setBackgroundType(@NonNull BackgroundType backgroundType) {
		shape = backgroundType;
	}

	@Nullable
	@Override
	protected PointEditor getEditor() {
		MapActivity mapActivity = getMapActivity();
		return mapActivity != null ? mapActivity.getContextMenu().getPointEditor(editorTag) : null;
	}

	private boolean isGpxCategory() {
		return WptPtEditor.TAG.equals(editorTag);
	}

	public static boolean showAddCategoryDialog(@NonNull FragmentManager fragmentManager,
	                                            @Nullable CategorySelectionListener selectionListener,
	                                            @NonNull String editorTag,
	                                            @Nullable Set<String> gpxCategories) {
		return showInstance(fragmentManager, selectionListener, editorTag, null, gpxCategories);
	}

	private static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                    @Nullable CategorySelectionListener categorySelectionListener,
	                                    @NonNull String editorTag,
	                                    @Nullable String favouriteGroupName,
	                                    @Nullable Set<String> gpxCategories) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			CategoryEditorFragment fragment = new CategoryEditorFragment();

			Bundle args = new Bundle();
			args.putString(KEY_EDITOR_TAG, editorTag);
			args.putString(KEY_EXISTING_FAVORITE_CATEGORY_NAME, favouriteGroupName);
			if (gpxCategories != null) {
				args.putStringArrayList(KEY_GPX_CATEGORIES_LIST, new ArrayList<>(gpxCategories));
			}
			fragment.setArguments(args);

			fragment.selectionListener = categorySelectionListener;
			fragment.setRetainInstance(true);

			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();

			return true;
		}
		return false;
	}
}