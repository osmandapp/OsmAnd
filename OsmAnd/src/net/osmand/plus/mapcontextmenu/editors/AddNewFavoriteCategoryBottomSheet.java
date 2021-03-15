package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import net.osmand.plus.track.ColorsCard;
import net.osmand.plus.track.CustomColorBottomSheet;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddNewFavoriteCategoryBottomSheet extends MenuBottomSheetDialogFragment implements CustomColorBottomSheet.ColorPickerListener, BaseCard.CardListener {


	public static final String TAG = AddNewTrackFolderBottomSheet.class.getName();
	private static final String KEY_CTX_EDIT_CAT_EDITOR_TAG = "key_ctx_edit_cat_editor_tag";
	private static final String KEY_CTX_EDIT_GPX_FILE = "key_ctx_edit_gpx_file";
	private static final String KEY_CTX_EDIT_GPX_CATEGORIES = "key_ctx_edit_gpx_categories";
	private static final String KEY_CTX_EDIT_CAT_NEW = "key_ctx_edit_cat_new";
	private static final String KEY_CTX_EDIT_CAT_NAME = "key_ctx_edit_cat_name";
	private static final String KEY_CTX_EDIT_CAT_COLOR = "key_ctx_edit_cat_color";
	FavouritesDbHelper favoritesHelper;
	private boolean isNew = true;
	private String name = "";
	private boolean isGpx;
	private ArrayList<String> gpxCategories;
	private int selectedColor;
	private ColorsCard colorsCard;
	private TextInputEditText editText;
	private TextInputLayout nameTextBox;
	private View view;
	private String editorTag;
	private SelectFavoriteCategoryBottomSheet.CategorySelectionListener selectionListener;

	public static AddNewFavoriteCategoryBottomSheet createInstance(@NonNull String editorTag, @Nullable Set<String> gpxCategories, boolean isGpx) {
		AddNewFavoriteCategoryBottomSheet fragment = new AddNewFavoriteCategoryBottomSheet();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpx);
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, new ArrayList<>(gpxCategories));
		}
		fragment.setArguments(bundle);
		fragment.setRetainInstance(true);
		return fragment;
	}

	public void setSelectionListener(SelectFavoriteCategoryBottomSheet.CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
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

	@Override
	protected void onRightBottomButtonClick() {
		name = editText.getText().toString().trim();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			boolean exists = isGpx ? isGpxCategoryExists(name) : favoritesHelper.groupExists(name);
			if (exists) {
				AlertDialog.Builder b = new AlertDialog.Builder(activity);
				b.setMessage(getString(R.string.favorite_category_dublicate_message));
				b.setNegativeButton(R.string.shared_string_ok, null);
				b.show();
			} else {
				if (activity instanceof MapActivity) {
					if (!isGpx) {
						favoritesHelper.addEmptyCategory(name, selectedColor);
					}
					PointEditor editor = ((MapActivity) activity).getContextMenu().getPointEditor(editorTag);

					if (editor != null) {
						editor.setCategory(name, selectedColor);
					}

					if (selectionListener != null) {
						selectionListener.onCategorySelected(name, selectedColor);
					}
				}
				dismiss();
			}
		}
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.PRIMARY;
	}

	@Nullable
	public FavouritesDbHelper getHelper() {
		return favoritesHelper;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NEW, Boolean.valueOf(isNew).toString());
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, editText.getText().toString().trim());
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + selectedColor);
		bundle.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpx);
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, gpxCategories);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		favoritesHelper = app.getFavorites();

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}


		items.add(new TitleItem(getString(R.string.favorite_category_add_new_title)));
		selectedColor = getResources().getColor(R.color.color_favorite);

		view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.add_new_favorite_category, null);
		nameTextBox = view.findViewById(R.id.name_text_box);
		nameTextBox.setHint(app.getResources().getString(R.string.favorite_category_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat
				.getColor(app, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		editText = view.findViewById(R.id.name_edit_text);
		editText.setText(name);
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(getActivity(), editText);
		nameTextBox.setStartIconTintList(ColorStateList.valueOf(selectedColor));

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(editFolderName);
		MapActivity mapActivity = (MapActivity) getActivity();
		List<Integer> colors = new ArrayList<>();
		for (int color : ColorDialogs.pallette) {
			colors.add(color);
		}
		colorsCard = new ColorsCard(mapActivity, selectedColor, this, colors, app.getSettings().CUSTOM_TRACK_COLORS, null);
		colorsCard.setListener(this);
		LinearLayout selectColor = view.findViewById(R.id.select_color);
		selectColor.addView(colorsCard.build(view.getContext()));
	}

	private void updateColorSelector(int color) {
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		selectedColor = color;
		nameTextBox.setStartIconTintList(ColorStateList.valueOf(selectedColor));
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			int color = ((ColorsCard) card).getSelectedColor();
			updateColorSelector(color);
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {

	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		int color = colorsCard.getSelectedColor();
		updateColorSelector(color);
	}

	public void restoreState(Bundle bundle) {
		editorTag = bundle.getString(KEY_CTX_EDIT_CAT_EDITOR_TAG);
		String isNewStr = bundle.getString(KEY_CTX_EDIT_CAT_NEW);
		if (isNewStr != null) {
			isNew = Boolean.parseBoolean(isNewStr);
		}
		name = bundle.getString(KEY_CTX_EDIT_CAT_NAME);
		if (name == null) {
			name = "";
		}
		String colorStr = bundle.getString(KEY_CTX_EDIT_CAT_COLOR);
		if (!Algorithms.isEmpty(colorStr)) {
			selectedColor = Integer.parseInt(colorStr);
		}
		isGpx = bundle.getBoolean(KEY_CTX_EDIT_GPX_FILE, false);
		gpxCategories = bundle.getStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES);
	}
}
