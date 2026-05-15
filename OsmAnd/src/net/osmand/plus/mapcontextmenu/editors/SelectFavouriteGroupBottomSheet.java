package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.os.Bundle;
import android.text.TextPaint;
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
import net.osmand.plus.myplaces.favorites.FavoriteFolderNode;
import net.osmand.plus.myplaces.favorites.FavoriteFolderPath;
import net.osmand.plus.myplaces.favorites.FavoriteFolderTree;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SelectFavouriteGroupBottomSheet extends SelectPointsCategoryBottomSheet {

	private static final String PATH_ELLIPSIS = "...";

	private final Map<String, FavoriteFolderNode> favoriteFolderNodes = new LinkedHashMap<>();

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
		favoriteFolderNodes.clear();

		FavouritesHelper helper = app.getFavoritesHelper();
		FavoriteFolderTree tree = helper.getFavoriteFolderTree();
		for (FavoriteFolderNode node : tree.flatten(true)) {
			if (node.isRoot() && node.getGroup() == null) {
				continue;
			}
			PointsGroup pointsGroup = createPointsGroup(node);
			pointsGroups.put(pointsGroup.getName(), pointsGroup);
			favoriteFolderNodes.put(node.getFullPath(), node);
		}
	}

	@NonNull
	protected BaseBottomSheetItem createCategoriesListItem() {
		View view = inflate(R.layout.favorite_categories_dialog);
		ViewGroup container = view.findViewById(R.id.list_container);

		for (PointsGroup pointsGroup : pointsGroups.values()) {
			FavoriteFolderNode node = favoriteFolderNodes.get(pointsGroup.getName());
			FavoriteGroup favoriteGroup = node != null ? node.getGroup() : null;
			container.addView(createCategoryItem(pointsGroup, favoriteGroup != null && !favoriteGroup.isVisible()));
		}
		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	@NonNull
	private PointsGroup createPointsGroup(@NonNull FavoriteFolderNode node) {
		FavoriteGroup group = node.getGroup();
		return group != null ? group.toPointsGroup(app)
				: new PointsGroup(node.getFullPath(), DEFAULT_ICON_NAME, DEFAULT_BACKGROUND_TYPE.getTypeName(), 0);
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
		return FavoriteFolderPath.toBreadcrumb(app, pointsGroup.getName());
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
				title.setText(getMiddleTruncatedBreadcrumb(fullPath, title.getPaint(), availableWidth));
			}
		});
	}

	@NonNull
	@Override
	protected String getCategoryDescription(@NonNull PointsGroup pointsGroup) {
		FavoriteFolderNode node = favoriteFolderNodes.get(pointsGroup.getName());
		if (node != null && node.isVirtual()) {
			return String.valueOf(node.getSubtreePointsCount());
		}
		return super.getCategoryDescription(pointsGroup);
	}

	@NonNull
	private String getMiddleTruncatedBreadcrumb(@Nullable String fullPath, @NonNull TextPaint textPaint, int availableWidth) {
		String fullBreadcrumb = FavoriteFolderPath.toBreadcrumb(app, fullPath);
		if (fits(fullBreadcrumb, textPaint, availableWidth)) {
			return fullBreadcrumb;
		}

		List<String> segments = FavoriteFolderPath.split(fullPath);
		if (segments.size() <= 2) {
			return TextUtils.ellipsize(fullBreadcrumb, textPaint, availableWidth, TextUtils.TruncateAt.MIDDLE).toString();
		}

		String best = buildMiddleTruncatedBreadcrumb(segments, 1, 1);
		for (int visibleSegments = 3; visibleSegments < segments.size(); visibleSegments++) {
			int leadingCount = (visibleSegments + 1) / 2;
			int trailingCount = visibleSegments - leadingCount;
			String candidate = buildMiddleTruncatedBreadcrumb(segments, leadingCount, trailingCount);
			if (fits(candidate, textPaint, availableWidth)) {
				best = candidate;
			} else {
				break;
			}
		}
		return fits(best, textPaint, availableWidth)
				? best
				: TextUtils.ellipsize(best, textPaint, availableWidth, TextUtils.TruncateAt.MIDDLE).toString();
	}

	private boolean fits(@NonNull String text, @NonNull TextPaint textPaint, int availableWidth) {
		return textPaint.measureText(text) <= availableWidth;
	}

	@NonNull
	private String buildMiddleTruncatedBreadcrumb(@NonNull List<String> segments, int leadingCount, int trailingCount) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < leadingCount; i++) {
			appendBreadcrumbSegment(builder, segments.get(i));
		}
		appendBreadcrumbSegment(builder, PATH_ELLIPSIS);
		for (int i = segments.size() - trailingCount; i < segments.size(); i++) {
			appendBreadcrumbSegment(builder, segments.get(i));
		}
		return builder.toString();
	}

	private void appendBreadcrumbSegment(@NonNull StringBuilder builder, @NonNull String segment) {
		if (builder.length() > 0) {
			builder.append(FavoriteFolderPath.DISPLAY_DELIMITER);
		}
		builder.append(segment);
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