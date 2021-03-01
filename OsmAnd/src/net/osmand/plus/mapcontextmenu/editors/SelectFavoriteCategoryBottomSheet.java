package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.List;
import java.util.Map;

public class SelectFavoriteCategoryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectFavoriteCategoryBottomSheet.class.getSimpleName();
	private OsmandApplication app;
	private GPXUtilities.GPXFile gpxFile;
	private String editorTag;
	private Map<String, Integer> gpxCategories;
	private SelectCategoryDialogFragment.CategorySelectionListener selectionListener;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			SelectFavoriteCategoryBottomSheet fragment = new SelectFavoriteCategoryBottomSheet();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	private static Drawable getIcon(final Activity activity, int iconId) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		UiUtilities iconsCache = app.getUIUtilities();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color_default_light : R.color.icon_color_default_dark);
	}

	private static Drawable getIcon(final Activity activity, int resId, int color) {
		Drawable drawable = AppCompatResources.getDrawable(activity, resId);
		if (drawable != null) {
			drawable = DrawableCompat.wrap(drawable).mutate();
			drawable.clearColorFilter();
			drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		return drawable;
	}


	public GPXUtilities.GPXFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(GPXUtilities.GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public void setGpxCategories(Map<String, Integer> gpxCategories) {
		this.gpxCategories = gpxCategories;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();

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
		Typeface face = Typeface.createFromAsset(app.getAssets(),
				"fonts/Roboto-Medium.ttf");
		title.setTypeface(face);
		AndroidUiHelper.updateVisibility(addNewCategoryView.findViewById(R.id.description), false);
		BaseBottomSheetItem addNewFolderItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.favorite_category_add_new))
				.setTitleColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						AddNewFavoriteCategoryBottomSheet.showInstance(getFragmentManager(), getTargetFragment());
						dismiss();
					}
				})
				.setCustomView(addNewCategoryView)
				.create();
		items.add(addNewFolderItem);

		DividerItem dividerItem = new DividerItem(app);
		dividerItem.setMargins(0, 0, 0, 0);
		items.add(dividerItem);

		View favoriteCategoryList = UiUtilities.getInflater(app, nightMode).inflate(R.layout.favorite_categories_dialog, null);
		LinearLayout favoriteCategoryContainer = favoriteCategoryList.findViewById(R.id.list_container);

		final FavouritesDbHelper helper2 = app.getFavorites();
		if (gpxFile != null) {
			if (gpxCategories != null) {
				for (Map.Entry<String, Integer> e : gpxCategories.entrySet()) {
					String categoryName = e.getKey();
					int favoriteCategoryCount = e.getKey().length();
					favoriteCategoryContainer.addView(createCategoryItem(activity, nightMode, categoryName, e.getValue(), favoriteCategoryCount, false, true));
				}
			}
		} else {
			List<FavouritesDbHelper.FavoriteGroup> gs = helper2.getFavoriteGroups();
			for (final FavouritesDbHelper.FavoriteGroup category : gs) {
				int favoriteCategoryCount = category.getPoints().size();
				favoriteCategoryContainer.addView(createCategoryItem(activity, nightMode, category.getDisplayName(getContext()),
						category.getColor(), favoriteCategoryCount, !category.isVisible(), true));
			}

			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(favoriteCategoryList)
					.create());
		}
	}

	private View createCategoryItem(@NonNull final Activity activity, boolean nightMode, final String categoryName, final int categoryColor, int categoryPointCount, boolean isHidden, final boolean selected) {
		View itemView = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		final AppCompatImageView button = (AppCompatImageView) itemView.findViewById(R.id.icon);
		LinearLayout test = itemView.findViewById(R.id.descriptionContainer);
		test.setPadding(0, 0, 0, 0);
		View divider = itemView.findViewById(R.id.divider_bottom);
		divider.setVisibility(View.GONE);
		itemView.setPadding(0, 0, 0, 0);
		final RadioButton compoundButton = itemView.findViewById(R.id.compound_button);
		int activeColorId = nightMode ?
				R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int disableColorId = nightMode ?
				R.color.icon_color_default_dark : R.color.icon_color_default_light;
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app,
				nightMode ? activeColorId : disableColorId), compoundButton);

		if (isHidden) {
			button.setImageResource(R.drawable.ic_action_hide);
		} else {
			if (categoryColor != 0) {
				button.setImageDrawable(getIcon(activity, R.drawable.ic_action_folder, categoryColor));
			} else {
				button.setImageDrawable(getIcon(activity, R.drawable.ic_action_folder, ContextCompat.getColor(activity,
						gpxFile != null ? R.color.gpx_color_point : R.color.color_favorite)));
			}
		}
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		TextView text = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		text.setText(name);
		description.setText(String.valueOf(categoryPointCount));
		test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity a = getActivity();
				compoundButton.setSelected(true);
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

	public interface CategorySelectionListener {

		void onCategorySelected(String category, int color);
	}
}
