package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.PlatformUtil;
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

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class AddNewFavoriteCategoryBottomSheet extends MenuBottomSheetDialogFragment implements CustomColorBottomSheet.ColorPickerListener, BaseCard.CardListener {


	public static final String TAG = AddNewTrackFolderBottomSheet.class.getName();
	private static final Log LOG = PlatformUtil.getLog(AddNewTrackFolderBottomSheet.class);
	private static final String FOLDER_NAME_KEY = "folder_name_key";
	FavouritesDbHelper favoritesHelper;
	private boolean isGpx;
	private ArrayList<String> gpxCategories;
	private int selectedColor;
	private ColorsCard colorsCard;
	private TextInputEditText editText;
	private TextInputLayout nameTextBox;
	private String folderName;
	private int defaultColor;
	private View view;
	private String editorTag;
	private SelectFavoriteCategoryBottomSheet.CategorySelectionListener selectionListener;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			AddNewFavoriteCategoryBottomSheet fragment = new AddNewFavoriteCategoryBottomSheet();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
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
		String name = editText.getText().toString().trim();
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

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		favoritesHelper = app.getFavorites();
		if (savedInstanceState != null) {
			folderName = savedInstanceState.getString(FOLDER_NAME_KEY);
		}
		items.add(new TitleItem(getString(R.string.favorite_category_add_new_title)));
		defaultColor = getResources().getColor(R.color.color_favorite);
		selectedColor = defaultColor;

		view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.add_new_favorite_category, null);
		nameTextBox = view.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light);
		nameTextBox.setHint(app.getResources().getString(R.string.favorite_category_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat
				.getColor(app, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		editText = view.findViewById(R.id.name_edit_text);
		editText.setText(folderName);
		if (editText.requestFocus()) {
			getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		nameTextBox.setStartIconTintList(ColorStateList.valueOf(selectedColor));
		nameTextBox.setBoxStrokeColorStateList(ColorStateList.valueOf(selectedColor));
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(editFolderName);
		MapActivity mapActivity = (MapActivity) getActivity();
		List<Integer> colors = new ArrayList<>();
		for (int color : ColorDialogs.pallette) {
			colors.add(color);
		}
		colorsCard = new ColorsCard(mapActivity, selectedColor, this, colors);
		colorsCard.setListener(this);
		LinearLayout selectColor = view.findViewById(R.id.select_color);
		selectColor.addView(colorsCard.build(view.getContext()));
	}

	private void updateColorSelector(int color) {
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		selectedColor = color;
		nameTextBox.setStartIconTintList(ColorStateList.valueOf(selectedColor));
		nameTextBox.setBoxStrokeColorStateList(ColorStateList.valueOf(selectedColor));
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

}
