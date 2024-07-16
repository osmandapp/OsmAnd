package net.osmand.plus.settings.fragments.profileappearance.elements;

import static net.osmand.plus.utils.ColorUtilities.getDefaultIconColor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ProfileIconPaletteElements extends IconsPaletteElements<Integer> {

	public ProfileIconPaletteElements(@NonNull Context context, boolean nightMode) {
		super(context, nightMode);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.preference_circle_item;
	}

	@Override
	public void bindView(@NonNull View itemView, @NonNull Integer icon,
	                     int controlsColor, boolean isSelected) {
		View background = itemView.findViewById(R.id.background);
		int bgColor = isSelected ? controlsColor : getDefaultIconColor(app, nightMode);
		int bgColorWithAlpha = ColorUtilities.getColorWithAlpha(bgColor, 0.1f);
		Drawable bgDrawable = AppCompatResources.getDrawable(app, R.drawable.circle_background_light);
		AndroidUtils.setBackground(background, UiUtilities.tintDrawable(bgDrawable, bgColorWithAlpha));

		ImageView outlineCircle = itemView.findViewById(R.id.outline);
		if (isSelected) {
			GradientDrawable circleContourDrawable = (GradientDrawable)
					AppCompatResources.getDrawable(app, R.drawable.circle_contour_bg_light);
			if (circleContourDrawable != null) {
				int activeColor = ColorUtilities.getActiveColor(app, nightMode);
				circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), activeColor);
			}
			outlineCircle.setImageDrawable(circleContourDrawable);
			outlineCircle.setVisibility(View.VISIBLE);
		} else {
			outlineCircle.setVisibility(View.GONE);
		}

		ImageView checkMark = itemView.findViewById(R.id.checkMark);
		if (isSelected) {
			checkMark.setImageDrawable(getPaintedIcon(icon, controlsColor));
		} else {
			checkMark.setImageDrawable(getIcon(icon, R.color.icon_color_default_light));
		}
	}
}
