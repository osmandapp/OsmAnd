package net.osmand.plus.settings.fragments.profileappearance.elements;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.plus.R;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class NavigationIconPaletteElements extends IconsPaletteElements<String> {

	public NavigationIconPaletteElements(@NonNull Context context, boolean nightMode) {
		super(context, nightMode);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.preference_select_icon_button;
	}

	@Override
	public void bindView(@NonNull View itemView, @NonNull String icon,
	                     int controlsColor, boolean isSelected) {
		Drawable navDrawable = NavigationIcon.getDrawable(app, icon);
		if (navDrawable instanceof LayerDrawable) {
			LayerDrawable navigationDrawable = (LayerDrawable) navDrawable;
			Drawable topDrawable = DrawableCompat.wrap(navigationDrawable.getDrawable(1));
			DrawableCompat.setTint(topDrawable, controlsColor);
		}
		ImageView imageView = itemView.findViewById(R.id.icon);
		imageView.setImageDrawable(navDrawable);
		Matrix matrix = new Matrix();
		imageView.setScaleType(ImageView.ScaleType.MATRIX);
		float width = imageView.getDrawable().getIntrinsicWidth() / 2f;
		float height = imageView.getDrawable().getIntrinsicHeight() / 2f;
		matrix.postRotate((float) -90, width, height);
		imageView.setImageMatrix(matrix);

		ImageView coloredRect = itemView.findViewById(R.id.backgroundRect);
		Drawable coloredDrawable = UiUtilities.tintDrawable(
				AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button),
				ColorUtilities.getColor(app, R.color.icon_color_default_light, 0.1f));
		AndroidUtils.setBackground(coloredRect, coloredDrawable);
		ImageView outlineRect = itemView.findViewById(R.id.outlineRect);
		GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_icon_button_outline);
		if (rectContourDrawable != null) {
			rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), controlsColor);
		}
		outlineRect.setImageDrawable(rectContourDrawable);
		outlineRect.setVisibility(isSelected ? View.VISIBLE : View.GONE);
	}
}
