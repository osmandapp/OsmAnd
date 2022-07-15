package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BackupIconsView extends View {

	private static final int CIRCLE_PAINT_STROKE_WIDTH_DP = 1;
	private static final int CIRCLE_PAINT_ALPHA = 51;

	private static final int ICON_SIZE_DP = 36;
	private static final int ICON_HORIZONTAL_MARGIN_DP = 16;

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private final Paint circlePaint = new Paint();
	private final List<IconsRow> iconsRows = new ArrayList<>();

	private final int iconSize;
	private final int horizontalMargin;
	private final int verticalMargin;
	private final int stepWidth;

	private int measuredWidth;
	private int iconsPerRow;

	private int offsetX;

	public BackupIconsView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		app = (OsmandApplication) context.getApplicationContext();
		uiUtilities = app.getUIUtilities();

		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth(dpToPx(CIRCLE_PAINT_STROKE_WIDTH_DP));

		iconSize = dpToPx(ICON_SIZE_DP);
		horizontalMargin = dpToPx(ICON_HORIZONTAL_MARGIN_DP);
		verticalMargin = iconSize / 4;
		stepWidth = iconSize + horizontalMargin;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (measuredWidth == 0) {
			measuredWidth = getMeasuredWidth();
			iconsPerRow = measuredWidth / stepWidth + 2;
			fillIconsRows();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawIconsRows(canvas);
		invalidate();
		offsetX++;
		if (offsetX >= measuredWidth) {
			offsetX = measuredWidth - iconsPerRow * stepWidth;
		}
	}

	private void drawIconsRows(@NonNull Canvas canvas) {
		for (IconsRow row : iconsRows) {
			int baseOffsetX = row.index % 2 == 0 ? horizontalMargin : 0;
			int initialOffsetX = this.offsetX + baseOffsetX;

			drawFittingIcons(canvas, row, initialOffsetX);
			drawNotFittingIcons(canvas, row, initialOffsetX);
		}
	}

	private void drawFittingIcons(@NonNull Canvas canvas, @NonNull IconsRow row, int initialOffsetX) {
		int offsetX = initialOffsetX;
		int i = 0;
		while (offsetX < measuredWidth && i < row.iconsIds.size()) {
			int iconId = row.iconsIds.get(i);
			drawIcon(canvas, iconId, row.colorId, offsetX, row.index);
			offsetX += stepWidth;
			i++;
		}
	}

	private void drawNotFittingIcons(@NonNull Canvas canvas, @NonNull IconsRow row, int initialOffsetX) {
		int offsetX = initialOffsetX - stepWidth;
		int i = row.iconsIds.size() - 1;
		int endIndex = (int) Math.ceil((measuredWidth - initialOffsetX) / (float) stepWidth);
		while (i >= endIndex && offsetX + iconSize >= 0) {
			int iconId = row.iconsIds.get(i);
			drawIcon(canvas, iconId, row.colorId, offsetX, row.index);
			offsetX -= stepWidth;
			i--;
		}
	}

	private void drawIcon(@NonNull Canvas canvas, @DrawableRes int iconId, @ColorRes int colorId, int offsetX, int row) {
		int iconSize2 = iconSize / 2;
		int centerX = mapX(offsetX + iconSize2);
		int centerY = row * (iconSize + verticalMargin) + iconSize2;

		Drawable icon = uiUtilities.getIcon(iconId, colorId);

		int leftBound = centerX - icon.getIntrinsicWidth() / 2;
		int topBound = centerY - icon.getIntrinsicHeight() / 2;
		int rightBound = centerX + icon.getIntrinsicWidth() / 2;
		int bottomBound = centerY + icon.getIntrinsicHeight() / 2;

		icon.setBounds(leftBound, topBound, rightBound, bottomBound);
		icon.draw(canvas);

		circlePaint.setColor(ContextCompat.getColor(app, colorId));
		circlePaint.setAlpha(CIRCLE_PAINT_ALPHA);
		canvas.drawCircle(centerX, centerY, iconSize2, circlePaint);
	}

	private int mapX(int x) {
		return !AndroidUtils.isLayoutRtl(getContext()) ? measuredWidth - x : x;
	}

	private void fillIconsRows() {
		List<List<Integer>> excludedIndexesForRows = getForbiddenIconsIndexesForRows();
		for (int row = 0; row < colorsIds.size(); row++) {

			int colorId = colorsIds.get(row);
			List<Integer> rowIconsIndexes = new ArrayList<>();
			List<Integer> excludedIndexesForRow = excludedIndexesForRows.get(row);

			for (int i = 0; i < iconsPerRow; i++) {
				Set<Integer> currentlyExcludedIndexes = new HashSet<>();
				currentlyExcludedIndexes.addAll(excludedIndexesForRow);
				currentlyExcludedIndexes.addAll(getDuplicatedIconsIndexesAtPosition(rowIconsIndexes, i, iconsPerRow));

				int randomIndex = randomize(iconsIds.size(), currentlyExcludedIndexes);
				rowIconsIndexes.add(randomIndex);
			}

			List<Integer> rowIconsIds = new ArrayList<>();
			for (int iconIndex : rowIconsIndexes) {
				rowIconsIds.add(iconsIds.get(iconIndex));
			}
			iconsRows.add(new IconsRow(row, colorId, rowIconsIds));
		}
	}

	/**
	 * @return list of forbidden icons indexes for each row to ensure that each icon will be used in only one row
	 */
	@NonNull
	private List<List<Integer>> getForbiddenIconsIndexesForRows() {
		int rows = colorsIds.size();
		int uniqueIconsForRow = iconsIds.size() / rows;
		int totalUniqueIcons = uniqueIconsForRow * rows;

		List<Integer> uniqueIconsIndexes = new ArrayList<>();
		for (int i = 0; i < totalUniqueIcons; i++) {
			int randomIndex = randomize(iconsIds.size(), uniqueIconsIndexes);
			uniqueIconsIndexes.add(randomIndex);
		}

		List<List<Integer>> forbiddenIconsIndexesForRows = new ArrayList<>();
		for (int row = 0; row < rows; row++) {
			int from = uniqueIconsForRow * row;
			int to = uniqueIconsForRow * (row + 1);
			List<Integer> uniqueIconsIndexesForRow = uniqueIconsIndexes.subList(from, to);

			List<Integer> forbiddenIconsIndexesForRow = new ArrayList<>();
			for (int iconIndex = 0; iconIndex < iconsIds.size(); iconIndex++) {
				boolean forbidden = !uniqueIconsIndexesForRow.contains(iconIndex);
				if (forbidden) {
					forbiddenIconsIndexesForRow.add(iconIndex);
				}
			}
			forbiddenIconsIndexesForRows.add(forbiddenIconsIndexesForRow);
		}

		return forbiddenIconsIndexesForRows;
	}

	/**
	 * @return set of icons indexes to ensure that distance between equal icons >= 2
	 */
	@NonNull
 	private Set<Integer> getDuplicatedIconsIndexesAtPosition(@NonNull List<Integer> addedRowIcons,
	                                                         int position,
	                                                         int iconsPerRow) {
		Set<Integer> iconsToExclude = new HashSet<>();
		if (position > 0) {
			iconsToExclude.add(addedRowIcons.get(position - 1));
		}
		if (position > 1) {
			iconsToExclude.add(addedRowIcons.get(position - 2));
		}
		if (position + 2 >= iconsPerRow) {
			iconsToExclude.add(addedRowIcons.get(0));
		}
		if (position + 1 == iconsPerRow) {
			iconsToExclude.add(addedRowIcons.get(1));
		}
		return iconsToExclude;
	}

	private int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	private static final List<Integer> colorsIds = Arrays.asList(
			R.color.backup_restore_icons_yellow,
			R.color.backup_restore_icons_blue,
			R.color.backup_restore_icons_green
	);

	private static final List<Integer> iconsIds = Arrays.asList(
			R.drawable.ic_action_photo,
			R.drawable.ic_action_favorite,
			R.drawable.ic_action_micro_dark,
			R.drawable.ic_notification_track,
			R.drawable.ic_type_video,
			R.drawable.ic_action_info_dark,
			R.drawable.ic_action_openstreetmap_logo,
			R.drawable.ic_action_flag,
			R.drawable.ic_map,
			R.drawable.ic_layer_top,
			R.drawable.ic_plugin_srtm,
			R.drawable.ic_action_plan_route,
			R.drawable.ic_action_map_style,
			R.drawable.ic_action_file_routing,
			R.drawable.ic_action_hillshade_dark,
			R.drawable.ic_action_gdirections_dark,
			R.drawable.ic_action_settings,
			R.drawable.ic_action_map_language,
			R.drawable.ic_action_car_dark,
			R.drawable.ic_action_pedestrian_dark,
			R.drawable.ic_action_volume_up,
			R.drawable.ic_action_sun,
			R.drawable.ic_action_ruler_unit);

	private static int randomize(int range, @NonNull Collection<Integer> excludedValues) {
		boolean ignoreExcluded = range == excludedValues.size();
		int randomValue;
		do {
			randomValue = new Random().nextInt(range);
		} while (!ignoreExcluded && excludedValues.contains(randomValue));
		return randomValue;
	}

	private static class IconsRow {

		public final int index;
		@ColorRes
		public final int colorId;
		@NonNull
		public final List<Integer> iconsIds;

		public IconsRow(int index, @ColorRes int colorId, @NonNull List<Integer> iconsIds) {
			this.index = index;
			this.colorId = colorId;
			this.iconsIds = Collections.unmodifiableList(iconsIds);
		}
	}
}