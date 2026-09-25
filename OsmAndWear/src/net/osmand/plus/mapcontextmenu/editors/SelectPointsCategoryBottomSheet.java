package net.osmand.plus.mapcontextmenu.editors;


import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SelectPointsCategoryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectPointsCategoryBottomSheet.class.getSimpleName();

	protected static final String KEY_SELECTED_CATEGORY = "selected_category";

	protected OsmandApplication app;

	protected String selectedCategory;
	protected Map<String, PointsGroup> pointsGroups = new LinkedHashMap<>();

	private CategorySelectionListener listener;

	public void setListener(@Nullable CategorySelectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}
	}

	private void restoreState(@NonNull Bundle bundle) {
		selectedCategory = bundle.getString(KEY_SELECTED_CATEGORY);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(createTitleItem());
		items.add(createAddNewCategoryItem());

		DividerItem dividerItem = new DividerItem(app);
		dividerItem.setMargins(0, 0, 0, 0);
		items.add(dividerItem);

		items.add(createCategoriesListItem());
	}

	@NonNull
	private BaseBottomSheetItem createTitleItem() {
		return new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_category_descr))
				.setTitle(getString(R.string.favorite_category_select))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
	}

	@NonNull
	private BaseBottomSheetItem createAddNewCategoryItem() {
		OsmandApplication app = requiredMyApplication();
		View container = UiUtilities.getInflater(app, nightMode)
				.inflate(R.layout.bottom_sheet_item_with_descr_64dp, null);
		container.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));

		TextView title = container.findViewById(R.id.title);
		title.setTypeface(FontCache.getMediumFont());

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);

		return new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.add_group))
				.setTitleColorId(ColorUtilities.getActiveColorId(nightMode))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setOnClickListener(v -> showAddNewCategoryFragment(listener))
				.setCustomView(container)
				.create();
	}

	@NonNull
	protected View createCategoryItem(PointsGroup pointsGroup, boolean isHidden) {
		Context context = requireContext();
		int dp8 = AndroidUtils.dpToPx(context, 8f);
		int dp16 = AndroidUtils.dpToPx(context, 16f);

		View itemView = UiUtilities.getInflater(context, nightMode)
				.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);

		LinearLayout descriptionContainer = itemView.findViewById(R.id.descriptionContainer);
		descriptionContainer.setPadding(dp16, 0, dp16, 0);

		AppCompatImageView button = itemView.findViewById(R.id.icon);
		button.setPadding(0, 0, dp8, 0);
		if (isHidden) {
			button.setImageDrawable(getContentIcon(R.drawable.ic_action_folder_hidden));
		} else {
			int categoryColor = pointsGroup.getColor();
			if (categoryColor != 0) {
				button.setImageDrawable(getPaintedIcon(R.drawable.ic_action_folder, categoryColor));
			} else {
				button.setImageDrawable(getIcon(R.drawable.ic_action_folder, getDefaultColorId()));
			}
		}
		String categoryName = pointsGroup.getName();
		RadioButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(Algorithms.stringsEqual(selectedCategory, categoryName));
		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		UiUtilities.setupCompoundButton(nightMode, activeColor, compoundButton);

		TextView text = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		text.setText(name);
		description.setText(String.valueOf(pointsGroup.getPoints().size()));

		itemView.setOnClickListener(v -> {
			PointEditor pointEditor = getPointEditor();
			if (pointEditor != null) {
				pointEditor.setPointsGroup(pointsGroup);
			}
			if (listener != null) {
				listener.onCategorySelected(pointsGroup);
			}
			dismiss();
		});

		return itemView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putString(KEY_SELECTED_CATEGORY, selectedCategory);
	}

	@ColorRes
	protected abstract int getDefaultColorId();

	@Nullable
	protected abstract PointEditor getPointEditor();

	@NonNull
	protected abstract BaseBottomSheetItem createCategoriesListItem();

	protected abstract void showAddNewCategoryFragment(CategorySelectionListener listener);

	public interface CategorySelectionListener {

		void onCategorySelected(PointsGroup pointsGroup);
	}
}