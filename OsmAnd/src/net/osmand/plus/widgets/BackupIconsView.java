package net.osmand.plus.widgets;

import android.content.Context;
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
	private final List<HashMap<Integer, Integer>> columnList = new ArrayList<>();
	private int xAnim = 0;
	private final int xStep;

	public BackupIconsView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		app = (OsmandApplication) context.getApplicationContext();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.map_button_stroke));
		iconSize = context.getResources().getDimensionPixelSize(R.dimen.big_icon_size);
		rowMargin = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		xStep = iconSize + rowMargin;
	}

	private void initView() {
		int screenSize = getWidth();
		if (columnList.isEmpty()) {
			while (screenSize > 0) {
				addColumn();
				screenSize -= xStep;
			}
		}
	}

	private void addColumn() {
		HashMap<Integer, Integer> map = new HashMap<>();
		map.put(R.color.backup_restore_icons_yellow, getIconId());
		map.put(R.color.backup_restore_icons_blue, getIconId());
		map.put(R.color.backup_restore_icons_green, getIconId());
		columnList.add(map);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		initView();
		int x = xAnim;
		xAnim += 10;
		for (HashMap<Integer, Integer> map : columnList) {
			buildColumn(canvas, map, x);
			x -= xStep;
		}
		if (xAnim >= xStep * columnList.size()) {
			addColumn();
		}
		invalidate();
	}

	private void buildColumn(Canvas canvas, HashMap<Integer, Integer> map, int x) {
		int row = 0;
		int xOffset = x;
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (row % 2 == 0) {
				xOffset += rowMargin;
			}
			drawIcon(canvas, entry.getValue(), entry.getKey(), xOffset, row);
			row++;
			xOffset = x;
		}
	}

	private void drawIcon(Canvas canvas, int drawableId, int color, int x, int rowNumber) {
		int rowBottomMargin = iconSize / 4;
		int center = iconSize / 2;
		Drawable icon = app.getUIUtilities().getIcon(drawableId, color);
		paint.setColor(ContextCompat.getColor(app, color));
		paint.setAlpha(51);
		x += center;
		int y = center;
		if (icon != null) {
			y += rowNumber * (iconSize + rowBottomMargin);
			icon.setBounds((x - icon.getIntrinsicWidth() / 2),
					y - icon.getIntrinsicHeight() / 2,
					x + icon.getIntrinsicWidth() / 2,
					y + icon.getIntrinsicHeight() / 2);
			canvas.drawCircle(x, y, center, paint);
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