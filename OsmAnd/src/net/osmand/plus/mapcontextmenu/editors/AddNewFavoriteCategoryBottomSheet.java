package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.myplaces.AddNewTrackFolderBottomSheet;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ListStringPreference;
import net.osmand.plus.track.ColorsCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.mapcontextmenu.editors.SelectFavoriteCategoryBottomSheet.*;
import static net.osmand.plus.routepreparationmenu.cards.BaseCard.*;
import static net.osmand.plus.track.CustomColorBottomSheet.*;

public class AddNewFavoriteCategoryBottomSheet extends MenuBottomSheetDialogFragment implements
		ColorPickerListener, CardListener {

	public static final String TAG = AddNewTrackFolderBottomSheet.class.getName();

	private static final String KEY_CTX_EDIT_CAT_EDITOR_TAG = "key_ctx_edit_cat_editor_tag";
	private static final String KEY_CTX_EDIT_GPX_FILE = "key_ctx_edit_gpx_file";
	private static final String KEY_CTX_EDIT_GPX_CATEGORIES = "key_ctx_edit_gpx_categories";
	private static final String KEY_CTX_EDIT_CAT_NAME = "key_ctx_edit_cat_name";
	private static final String KEY_CTX_EDIT_CAT_COLOR = "key_ctx_edit_cat_color";

	FavouritesDbHelper favoritesHelper;
	private String editorTag;

	private String categoryName = "";
	private int categoryColor;
	private boolean isGpxCategory;
	private ArrayList<String> gpxCategories;

	private View view;
	private TextInputLayout categoryNameTextBox;
	private ColorsCard colorsCard;
	private CategorySelectionListener selectionListener;

	private boolean saveButtonEnabled = false;

	public static AddNewFavoriteCategoryBottomSheet createInstance(@NonNull String editorTag,
	                                                               @Nullable Set<String> gpxCategories,
	                                                               boolean isGpxCategory) {
		AddNewFavoriteCategoryBottomSheet fragment = new AddNewFavoriteCategoryBottomSheet();
		Bundle args = new Bundle();
		args.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		args.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpxCategory);
		if (gpxCategories != null) {
			args.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, new ArrayList<>(gpxCategories));
		}
		fragment.setArguments(args);
		fragment.setRetainInstance(true);
		return fragment;
	}

	public void setSelectionListener(CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}
		favoritesHelper = requiredMyApplication().getFavorites();
	}

	private void restoreState(Bundle bundle) {
		editorTag = bundle.getString(KEY_CTX_EDIT_CAT_EDITOR_TAG);
		categoryName = bundle.getString(KEY_CTX_EDIT_CAT_NAME);
		if (categoryName == null) {
			categoryName = "";
		}
		String colorStr = bundle.getString(KEY_CTX_EDIT_CAT_COLOR);
		categoryColor = Algorithms.isEmpty(colorStr)
				? getResources().getColor(R.color.color_favorite)
				: Integer.parseInt(colorStr);
		isGpxCategory = bundle.getBoolean(KEY_CTX_EDIT_GPX_FILE, false);
		gpxCategories = bundle.getStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.favorite_category_add_new_title)));

		view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.add_new_favorite_category, null);
		BaseBottomSheetItem mainViewBottomSheet = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(mainViewBottomSheet);

		setupCategoryNameTextBox();
		setupCategoryNameEditText();
		updateCategoryColor(categoryColor);
		setupColorsCard();
	}

	private void setupCategoryNameTextBox() {
		categoryNameTextBox = view.findViewById(R.id.name_text_box);
		categoryNameTextBox.setHint(getString(R.string.favorite_category_name));
		int colorRes = nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes));
		categoryNameTextBox.setDefaultHintTextColor(colorStateList);
		categoryNameTextBox.setStartIconTintList(ColorStateList.valueOf(categoryColor));
	}

	private void setupCategoryNameEditText() {
		TextInputEditText categoryNameEditText = view.findViewById(R.id.name_edit_text);
		categoryNameEditText.setText(categoryName);
		categoryNameEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(getActivity(), categoryNameEditText);
		categoryNameEditText.addTextChangedListener(getTextWatcher());
	}

	private TextWatcher getTextWatcher() {
		return new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				onCategoryNameFromEditTextChanged(s.toString());
			}
		};
	}

	private void onCategoryNameFromEditTextChanged(String categoryName) {
		saveButtonEnabled = false;
		String trimmedCategoryName = categoryName.trim();
		if (trimmedCategoryName.isEmpty()) {
			categoryNameTextBox.setError(getString(R.string.empty_category_name));
		} else if (isCategoryExists(trimmedCategoryName)) {
			categoryNameTextBox.setError(getString(R.string.favorite_category_dublicate_message));
		} else {
			categoryNameTextBox.setError(null);
			categoryNameTextBox.setErrorEnabled(false);
			saveButtonEnabled = true;
		}
		this.categoryName = trimmedCategoryName;
		updateBottomButtons();
	}

	private boolean isCategoryExists(@NonNull String name) {
		return isGpxCategory ? isGpxCategoryExists(name) : favoritesHelper.groupExists(name);
	}

	private boolean isGpxCategoryExists(@NonNull String name) {
		boolean res = false;
		if (gpxCategories != null) {
			String nameLC = name.toLowerCase();
			for (String category : gpxCategories) {
				if (category.toLowerCase().equals(nameLC)) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	private void updateCategoryColor(int color) {
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		categoryColor = color;
		categoryNameTextBox.setStartIconTintList(ColorStateList.valueOf(categoryColor));
	}

	private void setupColorsCard() {
		MapActivity mapActivity = ((MapActivity) getActivity());
		if (mapActivity == null) {
			return;
		}

		List<Integer> colors = new ArrayList<>();
		for (int color : ColorDialogs.pallette) {
			colors.add(color);
		}
		ListStringPreference colorsListPref = requiredMyApplication().getSettings().CUSTOM_TRACK_COLORS;
		colorsCard = new ColorsCard(mapActivity, categoryColor, this, colors, colorsListPref, null);
		colorsCard.setListener(this);
		LinearLayout colorsCardContainer = view.findViewById(R.id.colors_card_container);
		colorsCardContainer.addView(colorsCard.build(view.getContext()));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.PRIMARY;
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return saveButtonEnabled;
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (activity instanceof MapActivity) {
				if (!isGpxCategory) {
					favoritesHelper.addEmptyCategory(categoryName, categoryColor);
				}
				PointEditor editor = ((MapActivity) activity).getContextMenu().getPointEditor(editorTag);

				if (editor != null) {
					editor.setCategory(categoryName, categoryColor);
				}

				if (selectionListener != null) {
					selectionListener.onCategorySelected(categoryName, categoryColor);
				}
			}
			dismiss();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, categoryName);
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + categoryColor);
		bundle.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpxCategory);
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, gpxCategories);
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			int color = ((ColorsCard) card).getSelectedColor();
			updateCategoryColor(color);
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		int color = colorsCard.getSelectedColor();
		updateCategoryColor(color);
	}
}