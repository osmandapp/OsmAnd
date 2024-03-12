package net.osmand.plus.card.color.palette.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.ColorUtils;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ColorsPaletteElements {

	public static final double MINIMUM_CONTRAST_RATIO = 1.5;

	private final OsmandApplication app;

	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public ColorsPaletteElements(@NonNull Context context, boolean nightMode) {
		this.nightMode = nightMode;
		app = (OsmandApplication) context.getApplicationContext();
		themedInflater = UiUtilities.getInflater(context, nightMode);
	}

	public void updateColorItemView(@NonNull View view, int color, boolean showOutline) {
		ImageView icon = view.findViewById(R.id.icon);
		AppCompatImageView background = view.findViewById(R.id.background);
		AppCompatImageView outline = view.findViewById(R.id.outline);

		Drawable transparencyIcon = getTransparencyIcon(color);
		Drawable colorIcon = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle, color);
		Drawable layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon);
		int listBgColor = ColorUtilities.getCardAndListBackgroundColor(app, nightMode);
		double contrastRatio = ColorUtils.calculateContrast(color, listBgColor);
		if (contrastRatio < MINIMUM_CONTRAST_RATIO) {
			background.setBackgroundResource(nightMode ? R.drawable.circle_contour_bg_dark : R.drawable.circle_contour_bg_light);
		}
		background.setImageDrawable(layeredIcon);

		if (showOutline) {
			Drawable border = getPaintedIcon(R.drawable.bg_point_circle_contour, color);
			outline.setImageDrawable(border);
			outline.setVisibility(View.VISIBLE);
		} else {
			outline.setVisibility(View.INVISIBLE);
			icon.setImageDrawable(UiUtilities.tintDrawable(
					icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
		}
	}

	@NonNull
	public View createButtonAddColorView(@NonNull ViewGroup rootView) {
		View itemView = createCircleView(rootView);
		ImageView icon = itemView.findViewById(R.id.icon);
		View outline = itemView.findViewById(R.id.outline);
		ImageView background = itemView.findViewById(R.id.background);

		int bgColorId = ColorUtilities.getActivityBgColorId(nightMode);
		Drawable backgroundIcon = getIcon(R.drawable.bg_point_circle, bgColorId);
		background.setImageDrawable(backgroundIcon);

		int activeColorResId = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		icon.setImageDrawable(getIcon(R.drawable.ic_action_plus, activeColorResId));
		icon.setVisibility(View.VISIBLE);
		outline.setVisibility(View.INVISIBLE);
		return itemView;
	}

	@NonNull
	public View createCircleView(@NonNull ViewGroup rootView) {
		return themedInflater.inflate(R.layout.point_editor_button, rootView, false);
	}

	@NonNull
	private Drawable getTransparencyIcon(@ColorInt int color) {
		int colorWithoutAlpha = ColorUtilities.removeAlpha(color);
		int transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f);
		return getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor);
	}

	protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return app.getUIUtilities().getPaintedIcon(id, color);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return app.getUIUtilities().getIcon(id, colorId);
	}
}
