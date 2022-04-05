package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.data.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.Map;

public class PointsCategoryEditorFragment extends EditorFragment {

	public static final String TAG = PointsCategoryEditorFragment.class.getName();

	private static final String EDITOR_TAG_KEY = "editor_tag_key";
	private static final String CATEGORY_NAME_KEY = "category_name_key";
	private static final String CATEGORY_COLOR_KEY = "category_color_key";
	private static final String CATEGORY_ICON_KEY = "category_icon_key";
	private static final String CATEGORY_BACKGROUND_TYPE_KEY = "category_background_type_key";

	private FavouritesHelper favoritesHelper;

	private String editorTag;
	private String categoryName;
	private PointsGroup pointsGroup;
	private Map<String, PointsGroup> pointsGroups;

	private CategorySelectionListener selectionListener;
	private boolean saved;

	@Override
	protected int getLayoutId() {
		return R.layout.category_editor_fragment;
	}

	@Override
	@DrawableRes
	protected int getToolbarNavigationIconId() {
		return R.drawable.ic_action_close;
	}

	@NonNull
	@Override
	protected String getToolbarTitle() {
		return getString(pointsGroup != null ? R.string.edit_category : R.string.favorite_category_add_new_title);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		favoritesHelper = app.getFavoritesHelper();

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}
	}

	private void restoreState(Bundle bundle) {
		editorTag = bundle.getString(EDITOR_TAG_KEY);
		categoryName = bundle.getString(CATEGORY_NAME_KEY, "");
		setIconName(bundle.getString(CATEGORY_ICON_KEY, DEFAULT_ICON_NAME));

		String backgroundTypeStr = bundle.getString(CATEGORY_BACKGROUND_TYPE_KEY);
		setBackgroundType(BackgroundType.getByTypeName(backgroundTypeStr, DEFAULT_BACKGROUND_TYPE));

		String colorStr = bundle.getString(CATEGORY_COLOR_KEY);
		int defaultColor = ContextCompat.getColor(app, isWaypointCategories() ? R.color.gpx_color_point : R.color.color_favorite);
		setColor(Algorithms.isEmpty(colorStr) ? defaultColor : Integer.parseInt(colorStr));
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		setupCategoryNameTextBox();
		setupCategoryNameEditText();

		return view;
	}

	private void setupCategoryNameTextBox() {
		nameCaption.setHint(getString(R.string.favorite_category_name));
		nameCaption.setStartIconTintList(ColorStateList.valueOf(getColor()));
		nameCaption.setDefaultHintTextColor(ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode)));
	}

	private void setupCategoryNameEditText() {
		if (Algorithms.isEmpty(categoryName) || isCategoryExists(categoryName)) {
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(requireActivity(), nameEdit);
		}
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dismiss_button), false);
	}

	@Nullable
	@Override
	public String getNameInitValue() {
		return categoryName;
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
		categoryName = trimmedName;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		nameCaption.setStartIconTintList(ColorStateList.valueOf(color));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState(outState);
	}

	public void saveState(Bundle bundle) {
		bundle.putString(EDITOR_TAG_KEY, editorTag);
		bundle.putString(CATEGORY_NAME_KEY, categoryName);
		bundle.putString(CATEGORY_COLOR_KEY, String.valueOf(color));
		bundle.putString(CATEGORY_ICON_KEY, iconName);
		bundle.putString(CATEGORY_BACKGROUND_TYPE_KEY, backgroundType.getTypeName());
	}

	private boolean isCategoryExists(@NonNull String name) {
		return isWaypointCategories() ? isGpxCategoryExists(name) : favoritesHelper.groupExists(name);
	}

	private boolean isGpxCategoryExists(@NonNull String name) {
		boolean res = false;
		if (pointsGroups != null) {
			String nameLC = name.toLowerCase();
			for (PointsGroup group : pointsGroups.values()) {
				if (group.getName().equalsIgnoreCase(nameLC)) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	private boolean isWaypointCategories() {
		return WptPtEditor.TAG.equals(editorTag);
	}

	@Override
	protected void save(boolean needDismiss) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PointsGroup group = pointsGroup;
			if (group == null) {
				group = new PointsGroup(categoryName, color, iconName, backgroundType.getTypeName());
			}
			if (!isWaypointCategories()) {
				if (pointsGroup == null) {
					favoritesHelper.addEmptyCategory(categoryName, color, iconName, backgroundType, true);
				} else {
//					favoritesHelper.editFavouriteGroup(categoryName, color, iconName, backgroundType, true);
				}
			}

			PointEditor editor = mapActivity.getContextMenu().getPointEditor(editorTag);
			if (editor != null) {
				editor.setPointsGroup(group);
			}

			if (selectionListener != null) {
				selectionListener.onCategorySelected(group);
			}
			saved = true;
		}
		dismiss();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Nullable
	@Override
	protected PointEditor getEditor() {
		MapActivity mapActivity = getMapActivity();
		return mapActivity != null ? mapActivity.getContextMenu().getPointEditor(editorTag) : null;
	}

	@Override
	public void onDestroy() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !mapActivity.isChangingConfigurations()) {
			mapActivity.launchPrevActivityIntent();
		}
		super.onDestroy();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull String editorTag,
	                                @Nullable PointsGroup pointsGroup,
	                                @Nullable Map<String, PointsGroup> pointsGroups,
	                                @Nullable CategorySelectionListener selectionListener) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			PointsCategoryEditorFragment fragment = new PointsCategoryEditorFragment();
			fragment.editorTag = editorTag;
			fragment.pointsGroup = pointsGroup;
			fragment.pointsGroups = pointsGroups;
			fragment.selectionListener = selectionListener;
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}