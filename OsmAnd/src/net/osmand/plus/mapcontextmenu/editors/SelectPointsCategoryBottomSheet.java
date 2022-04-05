package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.GPXUtilities.GPXFile;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.PointsGroup;
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
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;

public class SelectPointsCategoryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectPointsCategoryBottomSheet.class.getSimpleName();

	private static final String KEY_EDITOR_TAG = "editor_tag";
	private static final String KEY_SELECTED_CATEGORY = "selected_category";

	private String editorTag;
	private String selectedCategory;

	private GPXFile gpxFile;
	private PointsGroup pointsGroup;
	private Map<String, PointsGroup> pointsGroups;
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
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			PointsCategoryEditorFragment.showInstance(manager, editorTag, null, pointsGroups, selectionListener);
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
		if (pointsGroups != null) {
			for (PointsGroup pointsGroup : pointsGroups.values()) {
				container.addView(createCategoryItem(pointsGroup, false));
			}
		}
	}

	private void attachFavoriteCategories(@NonNull ViewGroup container) {
		OsmandApplication app = requiredMyApplication();
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		List<FavoriteGroup> favoriteGroups = favoritesHelper.getFavoriteGroups();
		for (FavoriteGroup favoriteGroup : favoriteGroups) {
			PointsGroup pointsGroup = favoriteGroup.toPointsGroup(app);
			container.addView(createCategoryItem(pointsGroup, !favoriteGroup.isVisible()));
		}
	}

	@NonNull
	private View createCategoryItem(final PointsGroup pointsGroup, boolean isHidden) {
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
			int categoryColor = pointsGroup.getColor();
			if (categoryColor != 0) {
				button.setImageDrawable(getPaintedIcon(R.drawable.ic_action_folder, categoryColor));
			} else {
				int defaultColorId = isWaypointCategories() ? R.color.gpx_color_point : R.color.color_favorite;
				button.setImageDrawable(getIcon(R.drawable.ic_action_folder, defaultColorId));
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
		description.setText(String.valueOf(pointsGroup.points.size()));

		itemView.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				PointEditor pointEditor = mapActivity.getContextMenu().getPointEditor(editorTag);
				if (pointEditor != null) {
					pointEditor.setPointsGroup(pointsGroup);
				}
				if (selectionListener != null) {
					selectionListener.onCategorySelected(pointsGroup);
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
	                                                      @NonNull Map<String, PointsGroup> gpxGroups) {
		showInstance(fragmentManager, null, WptPtEditor.TAG, selectedCategory, gpxGroups);
	}

	private static void showInstance(@NonNull FragmentManager fragmentManager,
	                                 @Nullable CategorySelectionListener selectionListener,
	                                 @NonNull String editorTag,
	                                 @NonNull String selectedCategory,
	                                 @Nullable Map<String, PointsGroup> pointsGroups) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectPointsCategoryBottomSheet fragment = new SelectPointsCategoryBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_EDITOR_TAG, editorTag);
			args.putString(KEY_SELECTED_CATEGORY, selectedCategory);
			if (pointsGroups != null) {
				fragment.pointsGroups.putAll(pointsGroups);
			}
			fragment.setArguments(args);
			fragment.selectionListener = selectionListener;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface CategorySelectionListener {

		void onCategorySelected(PointsGroup pointsGroup);
	}
}