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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BackupIconsView extends View {
	private final Paint paint = new Paint();
	private final int iconSize = AndroidUtils.dpToPx(getContext(), 36);
	private final int rowMargin = AndroidUtils.dpToPx(getContext(), 16);
	private final OsmandApplication app;

	public BackupIconsView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(AndroidUtils.dpToPx(context, 1));
		app = (OsmandApplication) context.getApplicationContext();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		buildRow(canvas, IconColorType.YELLOW, 0);
		buildRow(canvas, IconColorType.BLUE, 1);
		buildRow(canvas, IconColorType.GREEN, 2);
	}

	private void buildRow(Canvas canvas, IconColorType colorType, int rowNumber) {
		int screenSize = Resources.getSystem().getDisplayMetrics().widthPixels;
		int xOffset = 0;
		int xOffsetStep = iconSize + rowMargin;
		if (colorType == IconColorType.YELLOW || colorType == IconColorType.GREEN) {
			xOffset = rowMargin;
		}

		while (screenSize > 0) {
			drawIcon(canvas, getIconId(), colorType.getColor(), xOffset, rowNumber);
			xOffset += xOffsetStep;
			screenSize -= xOffsetStep;
		}
	}

	private void drawIcon(Canvas canvas, int drawableId, int color, int xOffset, int rowNumber) {
		int rowBottomMargin = iconSize / 4;
		int center = iconSize / 2;
		Drawable icon = app.getUIUtilities().getIcon(drawableId);
		paint.setColor(getResources().getColor(color));
		paint.setAlpha(51);
		int x = center + xOffset;
		int y = center;

		if (icon != null) {
			icon.setTint(getResources().getColor(color));
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

	enum IconColorType {
		YELLOW(R.color.backup_restore_icons_yellow),
		BLUE(R.color.backup_restore_icons_blue),
		GREEN(R.color.backup_restore_icons_green);

		private final int color;

		public int getColor() {
			return color;
		}

		IconColorType(int color) {
			this.color = color;
		}
	}
}