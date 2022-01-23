package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
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
import net.osmand.plus.myplaces.FavouritesDbHelper;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
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

		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_category_descr))
				.setTitle(getString(R.string.favorite_category_select))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);
		final FragmentActivity activity = requireActivity();

		View addNewCategoryView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_item_with_descr_64dp, null);
		addNewCategoryView.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));
		TextView title = addNewCategoryView.findViewById(R.id.title);
		Typeface typeface = FontCache.getRobotoMedium(getContext());
		title.setTypeface(typeface);
		AndroidUiHelper.updateVisibility(addNewCategoryView.findViewById(R.id.description), false);
		BaseBottomSheetItem addNewFolderItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.add_group))
				.setTitleColorId(ColorUtilities.getActiveColorId(nightMode))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
				.setOnClickListener(v -> showAddCategoryDialog())
				.setCustomView(addNewCategoryView)
				.create();
		items.add(addNewFolderItem);

		DividerItem dividerItem = new DividerItem(app);
		dividerItem.setMargins(0, 0, 0, 0);
		items.add(dividerItem);

		View favoriteCategoryList = UiUtilities.getInflater(app, nightMode).inflate(R.layout.favorite_categories_dialog, null);
		ScrollView scrollContainer = favoriteCategoryList.findViewById(R.id.scroll_container);
		scrollContainer.setPadding(0, 0, 0, 0);
		LinearLayout favoriteCategoryContainer = favoriteCategoryList.findViewById(R.id.list_container);

		final FavouritesDbHelper favoritesHelper = app.getFavorites();
		if (isWaypointCategories()) {
			if (gpxCategories != null) {
				Map<String, List<WptPt>> pointsCategories = gpxFile != null
						? gpxFile.getPointsByCategories()
						: new HashMap<>();
				for (Map.Entry<String, Integer> e : gpxCategories.entrySet()) {
					List<WptPt> categoryPoints = pointsCategories.get(e.getKey());
					String categoryCount = Algorithms.isEmpty(categoryPoints)
							? getString(R.string.shared_string_empty)
							: String.valueOf(categoryPoints.size());
					favoriteCategoryContainer.addView(createCategoryItem(activity, nightMode, e.getKey(), e.getValue(), categoryCount, false));
				}
			}
		} else {
			List<FavouritesDbHelper.FavoriteGroup> gs = favoritesHelper.getFavoriteGroups();
			for (final FavouritesDbHelper.FavoriteGroup category : gs) {
				String favoriteCategoryCount;
				if (Algorithms.isEmpty(category.getPoints())) {
					favoriteCategoryCount = app.getString(R.string.shared_string_empty);
				} else {
					favoriteCategoryCount = String.valueOf(category.getPoints().size());
				}
				favoriteCategoryContainer.addView(createCategoryItem(activity, nightMode, category.getDisplayName(getContext()),
						category.getColor(), favoriteCategoryCount, !category.isVisible()));
			}
		}
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(favoriteCategoryList)
				.create());
	}

	private void showAddCategoryDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			Set<String> gpxCategories = isWaypointCategories() && this.gpxCategories != null
					? this.gpxCategories.keySet()
					: null;
			boolean shown = CategoryEditorFragment.showAddCategoryDialog(fragmentManager, selectionListener,
					editorTag, gpxCategories);
			if (shown) {
				dismiss();
			}
		}
	}

	private View createCategoryItem(@NonNull final Activity activity, boolean nightMode, final String categoryName, final int categoryColor, String categoryPointCount, boolean isHidden) {
		View itemView = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		final AppCompatImageView button = (AppCompatImageView) itemView.findViewById(R.id.icon);
		final int dp8 = AndroidUtils.dpToPx(activity, 8f);
		final int dp16 = AndroidUtils.dpToPx(activity, 16f);
		button.setPadding(0, 0, dp8, 0);
		LinearLayout descriptionContainer = itemView.findViewById(R.id.descriptionContainer);
		descriptionContainer.setPadding(dp16, 0, dp16, 0);
		itemView.setPadding(0, 0, 0, 0);
		int activeColorId = ColorUtilities.getActiveColorId(nightMode);

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
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(activity,
				activeColorId), compoundButton);
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		TextView text = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		text.setText(name);
		description.setText(String.valueOf(categoryPointCount));
		itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity a = getActivity();
				if (a instanceof MapActivity) {
					PointEditor pointEditor = ((MapActivity) a).getContextMenu().getPointEditor(editorTag);
					if (pointEditor != null) {
						pointEditor.setCategory(categoryName, categoryColor);
					}
					if (selectionListener != null) {
						selectionListener.onCategorySelected(categoryName, categoryColor);
					}
				}
				dismiss();
			}
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