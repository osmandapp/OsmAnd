package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SelectFavouriteGroupBottomSheet extends SelectPointsCategoryBottomSheet {

	private final Map<String, FavoriteFolder> favoriteFolders = new LinkedHashMap<>();

	@Override
	protected int getDefaultColorId() {
		return R.color.color_favorite;
	}

	@Nullable
	@Override
	protected PointEditor getPointEditor() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getContextMenu().getFavoritePointEditor();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		populateFavoriteFolderTargets();
	}

	private void populateFavoriteFolderTargets() {
		pointsGroups.clear();
		favoriteFolders.clear();

		FavouritesHelper helper = app.getFavoritesHelper();
		for (FavoriteFolder folder : helper.getFlattenedFavoriteFolders(true)) {
			if (folder.isRoot() && folder.getGroup() == null) {
				continue;
			}
			PointsGroup pointsGroup = createPointsGroup(folder);
			pointsGroups.put(pointsGroup.getName(), pointsGroup);
			favoriteFolders.put(folder.getFullPath(), folder);
		}
	}

	@NonNull
	protected BaseBottomSheetItem createCategoriesListItem() {
		View view = inflate(R.layout.favorite_categories_dialog);
		ViewGroup container = view.findViewById(R.id.list_container);

		for (PointsGroup pointsGroup : pointsGroups.values()) {
			FavoriteFolder folder = favoriteFolders.get(pointsGroup.getName());
			FavoriteGroup favoriteGroup = folder != null ? folder.getGroup() : null;
			container.addView(createCategoryItem(pointsGroup, favoriteGroup != null && !favoriteGroup.isVisible()));
		}
		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	@NonNull
	private PointsGroup createPointsGroup(@NonNull FavoriteFolder folder) {
		FavoriteGroup group = folder.getGroup();
		return group != null ? group.toPointsGroup(app)
				: new PointsGroup(folder.getFullPath(), DEFAULT_ICON_NAME, DEFAULT_BACKGROUND_TYPE.getTypeName(), 0);
	}

	@Override
	protected int getTitleStringId() {
		return R.string.select_folder;
	}

	@Override
	protected int getDescriptionStringId() {
		return R.string.select_folder_descr;
	}

	@Override
	protected int getAddNewCategoryStringId() {
		return R.string.add_new_folder;
	}

	@NonNull
	@Override
	protected String getCategoryDisplayName(@NonNull PointsGroup pointsGroup) {
		return FavoriteFolderFormatter.getBreadcrumb(app, pointsGroup.getName());
	}

	@Override
	protected void setupCategoryTitle(@NonNull TextView title, @NonNull PointsGroup pointsGroup) {
		title.setSingleLine(true);
		title.setMaxLines(1);
		title.setEllipsize(TextUtils.TruncateAt.MIDDLE);

		String fullPath = pointsGroup.getName();
		title.post(() -> {
			int availableWidth = title.getWidth() - title.getPaddingLeft() - title.getPaddingRight();
			if (availableWidth > 0) {
				title.setText(FavoriteFolderFormatter.getMiddleTruncatedBreadcrumb(app, fullPath,
						title.getPaint(), availableWidth));
			}
		});
	}

	@NonNull
	@Override
	protected String getCategoryDescription(@NonNull PointsGroup pointsGroup) {
		FavoriteFolder folder = favoriteFolders.get(pointsGroup.getName());
		if (folder != null && folder.isVirtual()) {
			return String.valueOf(folder.getSubtreePointsCount());
		}
		return super.getCategoryDescription(pointsGroup);
	}

	@Override
	protected void showAddNewCategoryFragment(CategorySelectionListener listener) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (listener != null) {
				listener.onAddGroupOpened();
			}
			FragmentManager manager = activity.getSupportFragmentManager();
			FavouriteGroupEditorFragment.showInstance(manager, null, listener, false);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @Nullable String selectedCategory,
	                                @Nullable CategorySelectionListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectFavouriteGroupBottomSheet fragment = new SelectFavouriteGroupBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_SELECTED_CATEGORY, selectedCategory);

			fragment.setArguments(args);
			fragment.setListener(listener);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}
