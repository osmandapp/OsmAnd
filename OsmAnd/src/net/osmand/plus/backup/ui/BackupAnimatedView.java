package net.osmand.plus.backup.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class BackupAnimatedView extends View {
	private Paint paint = new Paint();
	private final int iconSize = AndroidUtils.dpToPx(getContext(), 36);
	private final int rowMargin = AndroidUtils.dpToPx(getContext(), 16);

	public BackupAnimatedView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(AndroidUtils.dpToPx(context, 1));
		paint.setAlpha(128);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		buildRow(canvas, IconColorType.YELLOW);
		buildRow(canvas, IconColorType.BLUE);
		buildRow(canvas, IconColorType.GREEN);
	}

	private void buildRow(Canvas canvas, IconColorType colorType) {
		int screenSize = Resources.getSystem().getDisplayMetrics().widthPixels;
		int index = 0;
		int xOffset = 0;
		int xOffsetStep = iconSize + rowMargin;
		if (colorType == IconColorType.YELLOW || colorType == IconColorType.GREEN) {
			xOffset = rowMargin;
		}
		List<Integer> iconsList = getIconsList(colorType);

		while (screenSize > 0) {
			drawIcon(canvas, iconsList.get(index), colorType.getColor(), xOffset);
			xOffset += xOffsetStep;
			screenSize -= xOffsetStep;
			index++;
		}
	}

	private void drawIcon(Canvas canvas, int drawable, int color, int xOffset) {
		int rowBottomMargin = iconSize / 4;
		int center = iconSize / 2;
		Drawable icon = AppCompatResources.getDrawable(getContext(), drawable);
		paint.setColor(getResources().getColor(color));
		int x = center + xOffset;
		int y = center;

		if (icon != null) {
			icon.setTint(getResources().getColor(color));
			if (color == IconColorType.BLUE.getColor()) {
				y += iconSize + rowBottomMargin;
			} else if (color == IconColorType.GREEN.getColor()) {
				y += (iconSize + rowBottomMargin) * 2;
			}
			icon.setBounds((x - icon.getIntrinsicWidth() / 2),
					y - icon.getIntrinsicHeight() / 2,
					x + icon.getIntrinsicWidth() / 2,
					y + icon.getIntrinsicHeight() / 2);
			canvas.drawCircle(center + xOffset, y, center, paint);
			icon.draw(canvas);
		}
	}

	private List<Integer> getIconsList(IconColorType colorType) {
		List<Integer> icons = new ArrayList<>();

		if (colorType == IconColorType.YELLOW) {
			icons.add(R.drawable.ic_action_photo);
			icons.add(R.drawable.ic_action_favorite);
			icons.add(R.drawable.ic_action_micro_dark);
			icons.add(R.drawable.ic_notification_track);
			icons.add(R.drawable.ic_type_video);
			icons.add(R.drawable.ic_action_info_dark);
			icons.add(R.drawable.ic_action_openstreetmap_logo);
			icons.add(R.drawable.ic_action_flag);

		} else if (colorType == IconColorType.BLUE) {
			icons.add(R.drawable.ic_map);
			icons.add(R.drawable.ic_action_settings);
			icons.add(R.drawable.ic_layer_top);
			icons.add(R.drawable.ic_plugin_srtm);
			icons.add(R.drawable.ic_action_plan_route);
			icons.add(R.drawable.ic_action_map_style);
			icons.add(R.drawable.ic_action_file_routing);
			icons.add(R.drawable.ic_action_hillshade_dark);

		} else {
			icons.add(R.drawable.ic_action_gdirections_dark);
			icons.add(R.drawable.ic_action_settings);
			icons.add(R.drawable.ic_action_map_language);
			icons.add(R.drawable.ic_action_car_dark);
			icons.add(R.drawable.ic_action_pedestrian_dark);
			icons.add(R.drawable.ic_action_volume_up);
			icons.add(R.drawable.ic_action_sun);
			icons.add(R.drawable.ic_action_ruler_unit);
		}
		return icons;
	}

	enum IconColorType {
		YELLOW(R.color.purchase_sc_toolbar_active_dark),
		BLUE(R.color.backup_restore_icons_blue),
		GREEN(R.color.purchase_save_discount);
		private int color;

		public int getColor() {
			return color;
		}

		IconColorType(int color) {
			this.color = color;
		}
	}
}