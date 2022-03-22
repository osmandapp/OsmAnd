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
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackupIconsView extends View {
	private final Paint paint = new Paint();
	private final int iconSize;
	private final int rowMargin;
	private final OsmandApplication app;
	private final Map<Integer, List<Integer>> iconsMap = new HashMap<>();

	public BackupIconsView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(AndroidUtils.dpToPx(context, 1));
		app = (OsmandApplication) context.getApplicationContext();
		iconSize = context.getResources().getDimensionPixelSize(R.dimen.big_icon_size);
		rowMargin = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		addRows();
	}

	private void addRows() {
		iconsMap.put(R.color.backup_restore_icons_yellow, new ArrayList<>());
		iconsMap.put(R.color.backup_restore_icons_blue, new ArrayList<>());
		iconsMap.put(R.color.backup_restore_icons_green, new ArrayList<>());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		buildRows(canvas);
	}

	private void buildRows(Canvas canvas) {
		int xOffset = 0;
		int xOffsetStep = iconSize + rowMargin;
		int row = 0;

		for (Map.Entry<Integer, List<Integer>> entry: iconsMap.entrySet()) {
			int screenSize = Resources.getSystem().getDisplayMetrics().widthPixels;
			if (row % 2 == 0) {
				xOffset = rowMargin;
			}
			if (entry.getValue().isEmpty()) {
				List<Integer> icons = new ArrayList<>();
				while (screenSize > 0) {
					icons.add(getIconId());
					screenSize -= xOffsetStep;
				}
				iconsMap.put(entry.getKey(), icons);
			}
			for (int i = 0; i < entry.getValue().size(); i++) {
				drawIcon(canvas, entry.getValue().get(i), entry.getKey(), xOffset, row);
				xOffset += xOffsetStep;
			}
			xOffset = 0;
			row++;
		}
	}

	private void drawIcon(Canvas canvas, int drawableId, int color, int xOffset, int rowNumber) {
		int rowBottomMargin = iconSize / 4;
		int center = iconSize / 2;
		Drawable icon = app.getUIUtilities().getIcon(drawableId, color);
		paint.setColor(ContextCompat.getColor(app, color));
		paint.setAlpha(51);
		int x = center + xOffset;
		int y = center;

		if (icon != null) {
			y += rowNumber * (iconSize + rowBottomMargin);
			icon.setBounds((x - icon.getIntrinsicWidth() / 2),
					y - icon.getIntrinsicHeight() / 2,
					x + icon.getIntrinsicWidth() / 2,
					y + icon.getIntrinsicHeight() / 2);
			canvas.drawCircle(center + xOffset, y, center, paint);
			icon.draw(canvas);
		}
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