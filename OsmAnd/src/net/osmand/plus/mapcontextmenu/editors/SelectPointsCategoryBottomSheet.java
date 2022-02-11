package net.osmand.plus.mapcontextmenu.editors;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.myplaces.FavouritesHelper.FavoriteGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;

import static net.osmand.GPXUtilities.GPXFile;
import static net.osmand.GPXUtilities.WptPt;

public class SelectPointsCategoryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectPointsCategoryBottomSheet.class.getSimpleName();

	private static final String KEY_EDITOR_TAG = "editor_tag";
	private static final String KEY_SELECTED_CATEGORY = "selected_category";
	private static final String KEY_GPX_CATEGORIES = "gpx_categories";

	private String editorTag;
	private String selectedCategory;

	private GPXFile gpxFile;
	private Map<String, Integer> gpxCategories;
	private CategorySelectionListener selectionListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}
	}

	private void restoreState(@NonNull Bundle bundle) {
		editorTag = bundle.getString(KEY_EDITOR_TAG);
		selectedCategory = bundle.getString(KEY_SELECTED_CATEGORY);
		if (WptPtEditor.TAG.equals(editorTag)) {
			WptPtEditor editor = ((MapActivity) requireActivity()).getContextMenu().getWptPtPointEditor();
			gpxFile = editor != null ? editor.getGpxFile() : null;
		}
		if (bundle.containsKey(KEY_GPX_CATEGORIES)) {
			gpxCategories = (HashMap<String, Integer>) bundle.getSerializable(KEY_GPX_CATEGORIES);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

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
		Typeface typeface = FontCache.getRobotoMedium(getContext());
		title.setTypeface(typeface);

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);

		return new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.add_group))
				.setTitleColorId(ColorUtilities.getActiveColorId(nightMode))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setOnClickListener(v -> showAddNewCategoryFragment())
				.setCustomView(container)
				.create();
	}

	private void showAddNewCategoryFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Set<String> categories = gpxCategories != null ? gpxCategories.keySet() : null;
			AddNewFavoriteCategoryBottomSheet fragment =
					AddNewFavoriteCategoryBottomSheet.createInstance(editorTag, categories, isWaypointCategories());
			fragment.setSelectionListener(selectionListener);
			fragment.show(mapActivity.getSupportFragmentManager(), AddNewFavoriteCategoryBottomSheet.TAG);
		}
		dismiss();
	}

	@NonNull
	private BaseBottomSheetItem createCategoriesListItem() {
		View view = UiUtilities.getInflater(requireContext(), nightMode)
				.inflate(R.layout.favorite_categories_dialog, null);
		ViewGroup categoriesContainer = view.findViewById(R.id.list_container);

		if (isWaypointCategories()) {
			attachWaypointCategories(categoriesContainer);
		} else {
			attachFavoriteCategories(categoriesContainer);
		}

		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	private void attachWaypointCategories(@NonNull ViewGroup container) {
		if (gpxCategories != null) {
			Map<String, List<WptPt>> pointsCategories = gpxFile != null
					? gpxFile.getPointsByCategories()
					: new HashMap<>();

			for (Map.Entry<String, Integer> category : gpxCategories.entrySet()) {
				String categoryName = category.getKey();
				int categoryColor = category.getValue();
				List<WptPt> categoryPoints = pointsCategories.get(categoryName);
				String categoryCount = Algorithms.isEmpty(categoryPoints)
						? getString(R.string.shared_string_empty)
						: String.valueOf(categoryPoints.size());
				container.addView(createCategoryItem(categoryName, categoryColor, categoryCount, false));
			}
		}
	}

	private void attachFavoriteCategories(@NonNull ViewGroup container) {
		FavouritesHelper favoritesHelper = requiredMyApplication().getFavoritesHelper();
		List<FavoriteGroup> favoriteGroups = favoritesHelper.getFavoriteGroups();
		for (FavoriteGroup category : favoriteGroups) {
			String displayName = category.getDisplayName(getContext());
			String categoryCount = Algorithms.isEmpty(category.getPoints())
					? getString(R.string.shared_string_empty)
					: String.valueOf(category.getPoints().size());
			container.addView(createCategoryItem(displayName, category.getColor(), categoryCount, !category.isVisible()));
		}
	}

	@NonNull
	private View createCategoryItem(final String categoryName, final int categoryColor, String categoryPointCount, boolean isHidden) {
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
			button.setImageResource(R.drawable.ic_action_hide);
		} else {
			if (categoryColor != 0) {
				button.setImageDrawable(getPaintedIcon(R.drawable.ic_action_folder, categoryColor));
			} else {
				int defaultColorId = isWaypointCategories() ? R.color.gpx_color_point : R.color.color_favorite;
				button.setImageDrawable(getIcon(R.drawable.ic_action_folder, defaultColorId));
			}
		}

		RadioButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(Algorithms.stringsEqual(selectedCategory, categoryName));
		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		UiUtilities.setupCompoundButton(nightMode, activeColor, compoundButton);

		TextView text = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		text.setText(name);
		description.setText(String.valueOf(categoryPointCount));

		itemView.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				PointEditor pointEditor = mapActivity.getContextMenu().getPointEditor(editorTag);
				if (pointEditor != null) {
					pointEditor.setCategory(categoryName, categoryColor);
				}
				if (selectionListener != null) {
					selectionListener.onCategorySelected(categoryName, categoryColor);
				}
			}
			dismiss();
		});

		return itemView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState(outState);
	}

	private void saveState(@NonNull Bundle bundle) {
		bundle.putString(KEY_EDITOR_TAG, editorTag);
		bundle.putString(KEY_SELECTED_CATEGORY, selectedCategory);
		if (gpxCategories != null) {
			bundle.putSerializable(KEY_GPX_CATEGORIES, new HashMap<>(gpxCategories));
		}
	}

	private boolean isWaypointCategories() {
		return WptPtEditor.TAG.equals(editorTag);
	}

	@Nullable
	private MapActivity getMapActivity() {
		return ((MapActivity) getActivity());
	}

	public void setSelectionListener(CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	public static void showSelectFavoriteCategoryFragment(@NonNull FragmentManager fragmentManager,
	                                                      @Nullable CategorySelectionListener selectionListener,
	                                                      @NonNull String selectedCategory) {
		showInstance(fragmentManager, selectionListener, FavoritePointEditor.TAG, selectedCategory, null);
	}

	public static void showSelectWaypointCategoryFragment(@NonNull FragmentManager fragmentManager,
	                                                      @NonNull String selectedCategory,
	                                                      @NonNull Map<String, Integer> gpxCategories) {
		showInstance(fragmentManager, null, WptPtEditor.TAG, selectedCategory, gpxCategories);
	}

	private static void showInstance(@NonNull FragmentManager fragmentManager,
	                                 @Nullable CategorySelectionListener selectionListener,
	                                 @NonNull String editorTag,
	                                 @NonNull String selectedCategory,
	                                 @Nullable Map<String, Integer> gpxCategories) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectPointsCategoryBottomSheet fragment = new SelectPointsCategoryBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_EDITOR_TAG, editorTag);
			args.putString(KEY_SELECTED_CATEGORY, selectedCategory);
			if (gpxCategories != null) {
				args.putSerializable(KEY_GPX_CATEGORIES, new HashMap<>(gpxCategories));
			}
			fragment.setArguments(args);
			fragment.selectionListener = selectionListener;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface CategorySelectionListener {

		void onCategorySelected(String category, int color);
	}
}