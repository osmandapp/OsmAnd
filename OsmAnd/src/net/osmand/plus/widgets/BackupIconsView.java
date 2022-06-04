package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BackupIconsView extends View {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private final Paint circlePaint = new Paint();
	private final Map<Integer, List<Integer>> iconsMap = new LinkedHashMap<>();

	private final int iconSize;
	private final int rowMargin;
	private int screenWidth;

	private int iconsOffset = 0;

	public BackupIconsView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		app = (OsmandApplication) context.getApplicationContext();
		uiUtilities = app.getUIUtilities();

		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.map_button_stroke));

		Resources resources = app.getResources();
		iconSize = resources.getDimensionPixelSize(R.dimen.big_icon_size);
		rowMargin = resources.getDimensionPixelSize(R.dimen.content_padding);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (screenWidth == 0) {
			int extraSpace = 2 * (iconSize + rowMargin);
			screenWidth = getMeasuredWidth() + extraSpace;
			fillIconsMap();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		buildRows(canvas);
		invalidate();

		if (iconsOffset >= screenWidth) {
			iconsOffset = 0;
		} else {
			iconsOffset++;
		}
	}

	private void buildRows(Canvas canvas) {
		int xOffsetStep = iconSize + rowMargin;
		int row = 0;

		for (Map.Entry<Integer, List<Integer>> entry : iconsMap.entrySet()) {
			Integer color = entry.getKey();
			List<Integer> iconIds = entry.getValue();

			int xOffset = row % 2 == 0 ? rowMargin : 0;
			xOffset = xOffset + iconsOffset;

			for (Integer iconId : iconIds) {
				if (xOffset > screenWidth - xOffsetStep) {
					xOffset -= screenWidth;
				}
				drawIcon(canvas, iconId, color, xOffset, row);
				xOffset += xOffsetStep;
			}
			row++;
		}
	}

	private void drawIcon(Canvas canvas, int drawableId, int colorId, int xOffset, int rowNumber) {
		Drawable icon = uiUtilities.getIcon(drawableId, colorId);
		circlePaint.setColor(ContextCompat.getColor(app, colorId));
		circlePaint.setAlpha(51);

		int center = iconSize / 2;
		int rowBottomMargin = iconSize / 4;
		int x = center + xOffset;
		int y = center + rowNumber * (iconSize + rowBottomMargin);

		icon.setBounds((x - icon.getIntrinsicWidth() / 2),
				y - icon.getIntrinsicHeight() / 2,
				x + icon.getIntrinsicWidth() / 2,
				y + icon.getIntrinsicHeight() / 2);
		canvas.drawCircle(center + xOffset, y, center, circlePaint);
		icon.draw(canvas);
	}

	private void fillIconsMap() {
		int xStep = iconSize + rowMargin;
		int iconsPerRow = screenWidth / xStep;

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
			iconsMap.put(colorId, rowIconsIds);
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
}