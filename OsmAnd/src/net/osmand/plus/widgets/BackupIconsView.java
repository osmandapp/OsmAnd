package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackupIconsView extends View {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private final Paint circlePaint = new Paint();
	private final Map<Integer, List<Integer>> iconsMap = new HashMap<>();

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
			initIconsMap();
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

	private void initIconsMap() {
		List<Integer> colorIds = new ArrayList<>();
		colorIds.add(R.color.backup_restore_icons_yellow);
		colorIds.add(R.color.backup_restore_icons_blue);
		colorIds.add(R.color.backup_restore_icons_green);

		int xOffsetStep = iconSize + rowMargin;
		int iconsCount = (int) screenWidth / xOffsetStep;

		for (Integer colorId : colorIds) {
			List<Integer> icons = new ArrayList<>();
			for (int i = 0; i < iconsCount; i++) {
				icons.add(getIconId());
			}
			iconsMap.put(colorId, icons);
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

	private int getIconId() {
		List<Integer> icons = Arrays.asList(
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
		int randomIndex = new Random().nextInt(icons.size());
		return icons.get(randomIndex);
	}
}